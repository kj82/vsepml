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
import org.vsepml.storm.twitter.TwitterSensAppTopology;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 11/05/2016.
 */
public class GrafterTopology {
    static final String TOPOLOGY_NAME = "mc-suite topology";
    private static final Logger journal = Logger.getLogger(TwitterSensAppTopology.class.getName());

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        Config config = new Config();
        config.setNumWorkers(2);

        TopologyBuilder b = new TopologyBuilder();

        Properties props = new Properties();

        b.setSpout("sensorStream", new SensorSimulatorSpout(args[0], 8000), 1);

        b.setBolt("Grafter", new GrafterBolt(), 1).shuffleGrouping("sensorStream");

        //b.setBolt("Fuseki", new FusekiBolt(args[0]), 1).shuffleGrouping("Grafter");

        try {
            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());
        } catch (AuthorizationException e) {
            e.printStackTrace();
        }

    }
}
