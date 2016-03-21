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
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.vsepml.storm.mcsuite.SensorSerializer;
import org.vsepml.storm.mcsuite.TupleToJSON;

import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 16/01/15.
 */
public class StormKafkaBolt<K,V> extends BaseRichBolt{

    private static final Logger journal = Logger.getLogger(StormTwitterStreamSpout.class.getName());

    private Producer<K, V> producer;
    private OutputCollector collector;
    private String topic;

    private String key;

    public StormKafkaBolt(String key, String topic){
        super();
        this.key=key;
        this.topic = topic;
    }

    public StormKafkaBolt(String topic){
        super();
        this.topic = topic;
    }

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        Map configMap = (Map)map.get("kafka.broker.properties");
        Properties properties = new Properties();
        properties.putAll(configMap);
        ProducerConfig config = new ProducerConfig(properties);
        this.producer = new Producer(config);
        this.collector = outputCollector;
    }

    @Override
    public void execute(Tuple tuple) {
        Object key = null;
        if(this.key != null) {
            if (tuple.contains(this.key)) {
                key = tuple.getValueByField(this.key);
            }
        }else{
            Random rnd = new Random();
            key=String.valueOf(rnd.nextInt());
        }

        TupleToJSON serializer=new SensorSerializer();
        String message=serializer.getJSONString(tuple);

        try {
            this.producer.send(new KeyedMessage(this.topic, key, message));
        } catch (Exception var8) {
            journal.log(Level.INFO,"Could not send message with key \'" + key + "\' and value \'" + message + "\'", var8);
        } finally {
            this.collector.ack(tuple);
        }

    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {

    }
}
