package org.stormtest.rutermonitor;

import java.util.HashMap;
import java.util.Map;

import org.stormtest.couchbolt.TupleToJson;

import backtype.storm.tuple.Tuple;

public class RuterStopvisitSerializer extends TupleToJson {
	@Override
	public String getDatabase() {
		return "stormtest_rutermonitor_stopvisits";
	}

	@Override
	public String getId(Tuple input) {
		return input.getString(0);
	}

	@Override
	public Map<String, Object> getValues(Tuple input) {
		Map<String, Object> out = new HashMap<String, Object>();
		out.put("visitid", input.getString(1));
		out.put("recorded", input.getString(2));
		out.put("delay", input.getLong(3));
		out.put("line", input.getInteger(4));
		out.put("origin", input.getString(5));
		out.put("stop", input.getString(6));
		out.put("destination", input.getString(7));
		out.put("vehicle", input.getString(8));
		out.put("posX", input.getInteger(9));
		out.put("posY", input.getInteger(10));
		out.put("vehicleroute", input.getString(11));
		out.put("vehiclejourney", input.getString(12));
		return out;
	}
}
