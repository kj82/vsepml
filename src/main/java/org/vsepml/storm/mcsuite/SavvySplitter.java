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
import backtype.storm.tuple.Values;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 20/01/2017.
 */
public class SavvySplitter extends BaseRichBolt {
    private AtomicLong a = new AtomicLong(0);
    private OutputCollector collector;

    private static final Logger journal = Logger.getLogger(SavvySplitter.class.getName());

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        String s= tuple.getStringByField("measurement");
        System.out.println(">>>>>" + s);
        if(!s.equals("0")) { //the API send O if no data
            //data sample: {"machine":"E15L17_VCVFQY_1","group":"QE1KWH","data":{"timestamp":{"$date":"2017-01-20T10:22:14.286Z"},"B3JCPQ":"10","A5TBSV":"0"}}
            org.json.simple.parser.JSONParser p = new JSONParser();
            journal.log(Level.INFO, s);
            try {
                org.json.simple.JSONObject obj = (org.json.simple.JSONObject) p.parse(s);
                org.json.simple.JSONObject data = (org.json.simple.JSONObject) obj.get("data");
                journal.log(Level.INFO, "---------------->"+data.toJSONString());
                org.json.simple.JSONObject timestamp = (org.json.simple.JSONObject) data.get("timestamp");
                Long tmp = a.incrementAndGet();
                journal.log(Level.INFO, "---------------->"+timestamp.toJSONString());
                Instant fromIso8601 = Instant.parse((String) timestamp.get("$date"));
                long epoch = fromIso8601.toEpochMilli();//convert from iso8601 to unix epoch
                for (Object st : data.keySet()) {
                    if (!(data.get(st) instanceof org.json.simple.JSONObject)) {
                        Header h = new Header((String) st);
                        collector.emit(new Values(h.getName(), (String) data.get(st), tmp, epoch, h.getUnit(), h.getType(), h.getCoeff()));
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("SensorId","MeasurementsAtTimeT", "MeasurementId", "Timestamp", "unit", "type", "coeff"));
    }
}
