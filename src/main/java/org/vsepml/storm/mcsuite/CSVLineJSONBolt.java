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
import com.opencsv.CSVReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 06/02/2017.
 */
public class CSVLineJSONBolt extends BaseRichBolt {
    private final char separator;
    private OutputCollector collector;
    private CSVReader reader;
    private ArrayList<Header> al=new ArrayList<Header>();
    private ArrayList<String> ls;


    private static final Logger journal = Logger.getLogger(CSVLineSplitterBolt.class.getName());

    public CSVLineJSONBolt(char separator, ArrayList<String> ls) {
        this.separator = separator;
        this.ls=ls;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        reader = new CSVReader(new StringReader(tuple.getStringByField("measurement")), separator);
        String[] nextLine;
        try {
            nextLine = reader.readNext();
            if (nextLine != null){
                if ((!nextLine[0].equals("Date")) && (!nextLine[0].equals("Fecha"))) {
                    long epoch=0;
                    if(al.get(0).getName().equals("Date")) {
                        Instant fromIso8601 = Instant.parse(nextLine[0]);
                        epoch=fromIso8601.toEpochMilli();//convert from iso8601 to unix epoch
                    }else {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        epoch=parser.parse(nextLine[0]).toInstant().toEpochMilli();
                    }

                    //Document d=new Document(epoch);
                    JSONObject o=new JSONObject();
                    JSONArray list = new JSONArray();
                    for (int i = 1; i < nextLine.length; i++) {
                        Header h=al.get(i-1);
                        if(ls.contains(h.getFullName())) {
                            JSONObject tmp=new JSONObject();
                            tmp.put("SensorID",h.getName());
                            if(h.getName().equals("Cnc_Program_Name")){
                                String[] splitted=nextLine[i].split("/");
                                tmp.put("Measurement", splitted[splitted.length - 1]);
                            }else {
                                tmp.put("Measurement",nextLine[i]);
                            }
                            //tmp.put("Measurement",nextLine[i]);
                            tmp.put("Type", h.getType());
                            tmp.put("Unit",h.getUnit());
                            tmp.put("Coeff",h.getCoeff());
                            list.add(tmp);
                        }
                    }
                    o.put("Measurements", list);
                    collector.emit(new Values(epoch,o.toJSONString()));
                } else {
                    for (int i = 1; i < nextLine.length; i++) {
                        al.add(new Header(nextLine[i]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("Timestamp", "Document"));
    }
}
