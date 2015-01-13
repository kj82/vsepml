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

import java.util.ArrayList;

/**
 * Created by ferrynico on 10/01/15.
 */
public class StormTwitterTopology {

    static final String TOPOLOGY_NAME = "storm-twitter";

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException {
        Config config = new Config();
        config.setNumWorkers(4);

        TopologyBuilder b = new TopologyBuilder();

        b.setSpout("TwitterStreamSpout", new StormTwitterStreamSpout(args[0],args[1],args[2],args[3]),1);
        b.setBolt("Splitter", new StormTwitterHashtagSplitter(),2)
                .shuffleGrouping("TwitterStreamSpout");
        ArrayList<String> identifiers= new ArrayList<String>();
        identifiers.add("oslo");
        b.setBolt("Identifier", new StormTwitterHashTagIdentifier(identifiers),1)
                .shuffleGrouping("Splitter");

        StormSubmitter.submitTopology(TOPOLOGY_NAME, config, b.createTopology());


    }

}
