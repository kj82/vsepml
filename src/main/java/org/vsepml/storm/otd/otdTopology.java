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
package org.vsepml.storm.otd;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import org.vsepml.storm.mcsuite.*;
import org.vsepml.storm.twitter.StormKafkaBolt;

import java.util.Properties;
import java.util.logging.Logger;


/**
 * Created by ferrynico on 08/09/2016.
 */
public class otdTopology {

    static final String TOPOLOGY_NAME = "OTDtopology";
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


        b.setSpout("sensorStream", new SensorSimulatorSpout(args[1], 8000),1);

        b.setBolt("test", new GrafterBolt(),1).shuffleGrouping("sensorStream");

        b.setBolt("Kafka4", new StormKafkaBolt<String, Double>("Grafter"),2).shuffleGrouping("test");

        try {
            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }

    }

}
