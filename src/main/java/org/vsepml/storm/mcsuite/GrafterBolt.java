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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import backtype.storm.tuple.Values;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;
import com.opencsv.CSVReader;
import org.vsepml.storm.twitter.TwitterSensAppTopology;


/**
 * Created by ferrynico on 04/05/2016.
 */
public class GrafterBolt extends BaseRichBolt {
    private static final Logger journal = Logger.getLogger(GrafterBolt.class.getName());
    private OutputCollector collector;
    private CSVReader reader;
    private String headers;


    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        reader = new CSVReader(new StringReader(tuple.getStringByField("measurement")), ',');
        String[] nextLine;
        try {
            nextLine = reader.readNext();
            if (nextLine != null){
                if (!nextLine[0].equals("Date")) {
                    if(!tuple.getStringByField("measurement").equals("")) {
                        IFn require = Clojure.var("clojure.core", "require");
                        require.invoke(Clojure.read("grafterizer.transformation"));

                        IFn require2 = Clojure.var("clojure.core", "require");
                        require2.invoke(Clojure.read("grafter.tabular"));

                        IFn require3 = Clojure.var("clojure.core", "require");
                        require3.invoke(Clojure.read("jarfter.core"));

                        //IFn mygraft = Clojure.var("grafterizer.transformation" , "my-transformation");

                        IFn mygraft = Clojure.var("jarfter.core", "-main");

                        StringBuilder sb = new StringBuilder();
                        sb.append(headers);
                        sb.append("\n");
                        sb.append(tuple.getStringByField("measurement"));
                        byte[] b = String.valueOf(sb).getBytes(StandardCharsets.UTF_8);

                        journal.log(Level.INFO, ":::::>>>>>>>>>" + sb.toString());
                        ByteArrayInputStream in = new ByteArrayInputStream(b);
                        mygraft.invoke(in, "/home/ubuntu/temp.nt");
                    }
                } else {
                    headers = tuple.getStringByField("measurement");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }




    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("Output"));
    }
}
