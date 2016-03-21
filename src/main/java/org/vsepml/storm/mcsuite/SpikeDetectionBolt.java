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

import java.util.Map;

/**
 * Created by ferrynico on 11/03/2016.
 */
public class SpikeDetectionBolt extends BaseRichBolt {
    private OutputCollector collector;
    private double threshold;
    private double average=0;

    public SpikeDetectionBolt(double threshold){
        this.threshold=threshold;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        if(tuple.getFields().contains("average")){
            average=tuple.getDoubleByField("average");
        }
        if(tuple.getFields().contains("MeasurementsAtTimeT")){
            double measurement=Double.parseDouble(tuple.getStringByField("MeasurementsAtTimeT"));
            if(average > 0){
                if (Math.abs(measurement - average) > threshold * average) {
                    collector.emit(new Values(true,measurement,(String)tuple.getValueByField("SensorId")));
                    collector.ack(tuple);
                }
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("Alarm","MeasurementsAtTimeT","SensorId"));
    }
}
