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

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.vsepml.storm.twitter.StormTwitterStreamSpout;
import twitter4j.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 09/03/2016.
 */
public class SensorSimulatorSpout extends BaseRichSpout {
    private String ip;
    private int port;
    private static SpoutOutputCollector collector;
    private final String USER_AGENT = "Mozilla/5.0";
    private HttpURLConnection con;
    private LinkedBlockingQueue<String> queue;
    private BufferedReader in = null;

    private static final Logger journal = Logger.getLogger(SensorSimulatorSpout.class.getName());

    public SensorSimulatorSpout(String ip, int port){
        super();
        this.port=port;
        this.ip=ip;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("measurement"));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
        queue = new LinkedBlockingQueue<String>(1000000);
        System.out.print("start queue");

        new Thread(new Runnable() {
            @Override
            public void run() {
                URL obj = null;
                try {
                    obj = new URL("HTTP://"+ip+":"+port);

                    con = (HttpURLConnection) obj.openConnection();

                    // optional default is GET
                    con.setRequestMethod("GET");

                    //add request header
                    con.setRequestProperty("User-Agent", USER_AGENT);

                    int responseCode = con.getResponseCode();
                    System.out.println("\nSending 'GET' request to URL : ");
                    System.out.println("Response Code : " + responseCode);

                    in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine="";

                    while((inputLine = in.readLine()) != null) {
                        Boolean b=queue.offer(inputLine);
                        //System.out.println("::"+b);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void nextTuple() {
        /*String inputLine="";
        try {
            if((inputLine = in.readLine()) != null){
                collector.emit(new Values(inputLine));

            }else{
                Utils.sleep(20);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        String st = queue.poll();
        if (st == null) {
            Utils.sleep(5);
        } else {
            collector.emit(new Values(st));
        }

    }
}
