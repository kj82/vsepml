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

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.vsepml.storm.twitter.util.CustomClassLoader;
import org.vsepml.storm.twitter.util.Plugin;
import org.vsepml.storm.twitter.util.WSServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 26/09/15.
 */
public class ReflexiveContainerBolt extends BaseRichBolt {

    private static final Logger journal = Logger.getLogger(ReflexiveContainerBolt.class.getName());

    private OutputCollector collector;
    private Plugin plugin;
    private CustomClassLoader classLoader;
    private int port;
    private WSServer serverSocket;
    private String URL;

    public ReflexiveContainerBolt(int port){
        this.port=port;
        serverSocket=new WSServer(port,this);
    }

    public ReflexiveContainerBolt(int port, String url){
        this(port);
        loadNewBoltBehavior(url);
    }

    public void loadNewBoltBehavior(String url){
        ClassLoader parentClassLoader = ReflexiveContainerBolt.class.getClassLoader();
        classLoader = new CustomClassLoader(parentClassLoader);
        try {
            Class p = classLoader.loadClass(url,true);
            plugin = (Plugin) p.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        if(plugin != null)
            plugin.execute(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        if(plugin != null)
            plugin.declareOutputFields(outputFieldsDeclarer);
    }
}
