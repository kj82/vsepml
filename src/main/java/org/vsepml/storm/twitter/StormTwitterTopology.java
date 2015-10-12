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
package org.vsepml.storm.twitter;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import storm.kafka.bolt.KafkaBolt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 10/01/15.
 */
public class StormTwitterTopology {

    static final String TOPOLOGY_NAME = "storm-twitter";
    private static final Logger journal = Logger.getLogger(StormTwitterTopology.class.getName());

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        Config config = new Config();
        config.setNumWorkers(4);

        Properties props = new Properties();
        props.put("metadata.broker.list", args[0]);
        props.put("request.required.acks", "1");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        config.put("kafka.broker.properties", props);

        TopologyBuilder b = new TopologyBuilder();

        Properties propTwitter = new Properties();
        InputStream input = null;
        try {

            input = new FileInputStream("twitter.properties");

            // load a properties file
            propTwitter.load(input);

            b.setSpout("TwitterStreamSpout",
                    new StormTwitterStreamSpout(
                            propTwitter.getProperty("accessToken"),
                            propTwitter.getProperty("accessTokenSecret"),
                            propTwitter.getProperty("consumerKey"),
                            propTwitter.getProperty("consumerSecret")), 1);

            b.setBolt("Splitter", new StormTwitterHashtagSplitter(),2)
                    .shuffleGrouping("TwitterStreamSpout");

            ArrayList<String> identifiers= new ArrayList<String>();
            identifiers.add("oslo");
            //b.setBolt("Identifier", new StormTwitterHashTagIdentifier(identifiers),1).shuffleGrouping("Splitter");

            b.setBolt("ReflexiveContainer", new ReflexiveContainerBolt(8080, "http://ferrynico.com/TwitterPlugin.class")).shuffleGrouping("splitter");

            //StormKafkaBolt<String,String> kafka= new StormKafkaBolt<String,String>("latlong","test");

            //b.setBolt("kafkaBolt",kafka,2).shuffleGrouping("Identifier");

            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());


        } catch (IOException ex) {
            journal.log(Level.INFO, ">> twitter.properties not found");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
