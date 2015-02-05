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
package org.vsepml.storm.sensapp;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import org.codehaus.jackson.map.ObjectMapper;
import org.vsepml.storm.sensapp.model.*;
import twitter4j.Status;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 04/02/15.
 */
public class SensAppBolt extends BaseRichBolt {

    private static final Logger journal = Logger.getLogger(SensAppBolt.class.getName());
    private OutputCollector collector;
    private String endPoint;
    private String name="";
    private static ObjectMapper mapper = new ObjectMapper();

    public SensAppBolt(String url) {
        this.endPoint=url;
        Random r=new Random();
        name="twitter"+r.nextInt();
        Sensor s = null;
        try {
            s = new Sensor(name,new URI(url),"","raw","String","", false);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String js=JsonPrinter.sensorToJson(s);
        if(!RestRequest.isSensorRegistred(s))
            RestRequest.postSensor(s);
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        Status tweet = (Status) tuple.getValueByField("tweet");
        LinkedList<ValueJsonModel> l=new LinkedList<ValueJsonModel>();
        l.add(new StringValueJsonModel(tweet.getText(), tweet.getCreatedAt().getTime()));
        StringMeasureJsonModel m=new StringMeasureJsonModel(name,tweet.getCreatedAt().getTime(),"m",l);
        try {
            String jsonString = mapper.writeValueAsString(m);
            RestRequest.putData(new URI(endPoint),jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
