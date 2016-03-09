package org.stormtest.rutermonitor;

import backtype.storm.tuple.Tuple;

public class RuterStopVisitData {	
	public Tuple input;
	public Long lastWriteMillis;
	
	public RuterStopVisitData(Tuple input, Long lastWriteMillis) {
		this.input = input;
		this.lastWriteMillis = lastWriteMillis;
	}
}