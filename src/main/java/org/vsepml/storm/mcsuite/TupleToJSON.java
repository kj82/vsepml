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

import java.util.Map;

import org.json.simple.JSONObject;

import backtype.storm.tuple.Tuple;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;
import storm.kafka.bolt.selector.KafkaTopicSelector;

public abstract class TupleToJSON implements TupleToKafkaMapper<String, String>, KafkaTopicSelector {
    abstract public String getDatabase(Tuple input);
    abstract public String getId(Tuple input);
    abstract public Map<String, Object> getValues(Tuple input);

    // Generate JSON string from values and id
    public String getJSONString(Tuple input) {
        JSONObject out= new JSONObject();;
        if(getValues(input).containsKey("Document")){
            JSONParser p=new JSONParser();
            try {
                out= (JSONObject) p.parse(getValues(input).get("Document").toString());

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else {
            out.putAll(getValues(input));
        }
        out.put("_id", getId(input));
        return out.toJSONString();
    }

    // Override Kafka methods
    @Override
    public String getTopic(Tuple input) {
        return getDatabase(input);
    }
    @Override
    public String getKeyFromTuple(Tuple input) {
        return getId(input);
    }
    @Override
    public String getMessageFromTuple(Tuple input) {
        return getJSONString(input);
    }
}