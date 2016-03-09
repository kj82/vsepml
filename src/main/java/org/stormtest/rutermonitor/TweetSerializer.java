package org.stormtest.rutermonitor;

import java.util.HashMap;
import java.util.Map;

import org.stormtest.couchbolt.TupleToJson;

import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class TweetSerializer extends TupleToJson {

	@Override
	public String getDatabase() {
		return "stormtest_rutermonitor_tweets";
	}

	@Override
	public String getId(Tuple input) {
		return input.getLong(0).toString();
	}

	@Override
	public Map<String, Object> getValues(Tuple input) {
		Map<String, Object> out = new HashMap<String, Object>();
		out.put("created", input.getString(1));
		out.put("user", input.getString(2));
		out.put("text", input.getString(3));
		return out;
	}
}
