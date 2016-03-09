package org.stormtest.couchbolt;

import java.util.Map;

import org.json.simple.JSONObject;

import backtype.storm.tuple.Tuple;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;
import storm.kafka.bolt.selector.KafkaTopicSelector;

public abstract class TupleToJson implements TupleToKafkaMapper<String, String>, KafkaTopicSelector {
	abstract public String getDatabase();
	abstract public String getId(Tuple input);
	abstract public Map<String, Object> getValues(Tuple input);
	
	// Generate JSON string from values and id
	public String getJSONString(Tuple input) {
		JSONObject out = new JSONObject();
		out.put("_id", getId(input));
		out.putAll(getValues(input));
		return out.toJSONString();
	}
	
	// Override Kafka methods
	@Override
	public String getTopic(Tuple input) {
		return getDatabase();
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
