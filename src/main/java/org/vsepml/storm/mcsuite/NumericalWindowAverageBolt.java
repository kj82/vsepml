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
import org.apache.commons.collections.buffer.CircularFifoBuffer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ferrynico on 11/03/2016.
 */
public class NumericalWindowAverageBolt extends BaseRichBolt {
    private OutputCollector collector;
    private double avgValue;
    private int sizeFifo;
    Map<String, CircularFifoBuffer> fifos = new HashMap<String, CircularFifoBuffer>();


    public NumericalWindowAverageBolt(int sizeFifo){
        this.sizeFifo = sizeFifo;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        String key= tuple.getValueByField("SensorId").toString();
        if(fifos.get(key) == null)
            fifos.put(key,new CircularFifoBuffer(sizeFifo));
        CircularFifoBuffer fifo=fifos.get(key);
        fifo.add(tuple);
        if(!tuple.getStringByField("MeasurementsAtTimeT").equals("")) {
            if (!tuple.getStringByField("MeasurementsAtTimeT").toLowerCase().matches(".*[a-z].*")) {
                updateAvgValue(fifo);
                collector.emit(new Values(tuple.getValueByField("MeasurementsAtTimeT"), avgValue, tuple.getValueByField("SensorId"), tuple.getValueByField("MeasurementId")));
                collector.ack(tuple);
            }
        }

    }


    private void updateAvgValue(CircularFifoBuffer fifo) {
        double sum = 0;
        int n = 0;

        Iterator i = fifo.iterator();

        while(i.hasNext()){
            Tuple tuple = (Tuple) i.next();
            if((tuple.getStringByField("MeasurementsAtTimeT") != null) && (!tuple.getStringByField("MeasurementsAtTimeT").equals(""))) {
                sum += Double.parseDouble(tuple.getStringByField("MeasurementsAtTimeT"));
                n++;
            }
        }
        avgValue = 0;
        if (n != 0)
            avgValue = sum / n;

    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("MeasurementsAtTimeT","Average","SensorId","MeasurementId"));
    }
}
