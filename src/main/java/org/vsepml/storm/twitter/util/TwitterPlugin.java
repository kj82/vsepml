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
package org.vsepml.storm.twitter.util;

import backtype.storm.task.OutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import twitter4j.GeoLocation;

import java.util.ArrayList;

/**
 * Created by ferrynico on 09/10/2015.
 */
public class TwitterPlugin implements Plugin{

    private OutputCollector collector;
    private ArrayList<String> identifiers = new ArrayList<String>();

    @Override
    public void execute(Tuple tuple) {
        String hashtag = (String) tuple.getValueByField("hashtag");
        if(filterHashTag(hashtag.toLowerCase())){
            if(tuple.getValueByField("geolocation") != null) {
                GeoLocation l = (GeoLocation) tuple.getValueByField("geolocation");
                collector.emit(new Values(l.getLatitude() + "," + l.getLongitude()));
            }
        }
    }

    public void setCollector(OutputCollector collector) {
        this.collector = collector;
    }

    private Boolean filterHashTag(String hashTag){
        for(String identifier: identifiers){
            if (hashTag.toLowerCase().contains(identifier)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("latlong"));
    }
}
