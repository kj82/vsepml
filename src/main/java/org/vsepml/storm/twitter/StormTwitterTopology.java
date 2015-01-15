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

        TopologyBuilder b = new TopologyBuilder();

        Properties prop = new Properties();
        InputStream input = null;
        try {

            input = new FileInputStream("twitter.properties");

            // load a properties file
            prop.load(input);

            b.setSpout("TwitterStreamSpout", new StormTwitterStreamSpout(prop.getProperty("accessToken"),
                    prop.getProperty("accessTokenSecret"),prop.getProperty("consumerKey"),prop.getProperty("consumerSecret")),1);
            b.setBolt("Splitter", new StormTwitterHashtagSplitter(),2)
                    .shuffleGrouping("TwitterStreamSpout");
            ArrayList<String> identifiers= new ArrayList<String>();
            identifiers.add("oslo");
            b.setBolt("Identifier", new StormTwitterHashTagIdentifier(identifiers),1)
                    .shuffleGrouping("Splitter");

            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());


        } catch (IOException ex) {
            journal.log(Level.INFO, ">> monitoring.properties not found status monitor not in use");
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
