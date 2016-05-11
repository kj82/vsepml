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

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ferrynico on 17/03/2016.
 */
public class SensorSerializer extends TupleToJSON {
    @Override
    public String getDatabase(Tuple input) {
        return "mc-suite";
    }

    @Override
    public String getId(Tuple input) {
        return input.getValueByField("MeasurementId").toString();
    }

    @Override
    public Map<String, Object> getValues(Tuple input) {
        Map<String, Object> out = new HashMap<String, Object>();
        for(int i=0 ; i < input.size() ; i++){
            out.put(input.getFields().get(i),input.getValue(i));
        }
        return out;
    }
}
