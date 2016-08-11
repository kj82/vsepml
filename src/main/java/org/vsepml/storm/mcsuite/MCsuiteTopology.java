/**
 * Copyright 2015 Nicolas Ferry <${email}>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vsepml.storm.mcsuite;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import org.vsepml.storm.twitter.StormKafkaBolt;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 09/03/2016.
 */
public class MCsuiteTopology {
    static final String TOPOLOGY_NAME = "mc-suite topology2";
    private static final Logger journal = Logger.getLogger(MCsuiteTopology.class.getName());

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        Config config = new Config();
        config.setNumWorkers(4);

        Properties props = new Properties();
        props.put("metadata.broker.list", args[0]);
        props.put("request.required.acks", "1");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        config.put("kafka.broker.properties", props);

        TopologyBuilder b = new TopologyBuilder();


        b.setSpout("sensorStream", new SensorSimulatorSpout(args[2], 8000),1);
        b.setBolt("Splitter", new CSVLineSplitterBolt(';'),1).shuffleGrouping("sensorStream"); //split per line and add numbering

        //b.setBolt("test", new GrafterBolt(),1).shuffleGrouping("sensorStream");

        // CouchDB configuration
        config.put(CouchBolt.COUCHDB_URL, "http://"+args[1]+":5984");
        config.put(CouchBolt.COUCHDB_USER, "couchdb");
        config.put(CouchBolt.COUCHDB_PASSWORD, "couchdb");
        b.setBolt("couch", new CouchBolt(new SensorSerializer()).withBatching(1, 20),5).shuffleGrouping("Splitter");//store into couchdb



        b.setBolt("Kafka", new StormKafkaBolt<String,String>("Sensors"),2).shuffleGrouping("Splitter");//publish on kafka

        b.setBolt("movingAverage", new NumericalWindowAverageBolt(10), 5) // Windowed average
                .fieldsGrouping("Splitter", new Fields("SensorId"));
        b.setBolt("spikes", new SpikeDetectionBolt(0.10f), 2) // Detect spike with respect to average
                .shuffleGrouping("movingAverage");

        b.setBolt("Kafka2", new StormKafkaBolt<String, Double>("Averages"),2).shuffleGrouping("movingAverage");
        b.setBolt("Kafka3", new StormKafkaBolt<String, Double>("Spikes"),1).shuffleGrouping("spikes");
        //b.setBolt("Kafka4", new StormKafkaBolt<String, Double>("Grafter"),2).shuffleGrouping("test");

        try {
            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }

    }

}
