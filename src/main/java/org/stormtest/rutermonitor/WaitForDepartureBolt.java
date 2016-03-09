package org.stormtest.rutermonitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.Config;
import backtype.storm.Constants;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.TupleUtils;

public class WaitForDepartureBolt implements IRichBolt {
	private static final Logger LOG = LoggerFactory.getLogger(WaitForDepartureBolt.class);
	private OutputCollector collector;
	// Cached stop data for all stops
	private Map<String, RuterStopVisitData> buffer;
	private Long waitToSendMillis;
	
	
	public void prepare(Map config, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.buffer = new HashMap<String, RuterStopVisitData>();
		
		this.waitToSendMillis = ((Long)config.get("rutermonitor.reisapi.departurewaitseconds"))*1000;
	}
	

	public void execute(Tuple input) {
		Long now = System.currentTimeMillis();
		
		if (!TupleUtils.isTick(input)) {
			// Add/update stop visit in buffer
			String visitId = input.getString(1);
			if (this.buffer.containsKey(visitId)) {
				RuterStopVisitData buffered = this.buffer.get(visitId);
				this.collector.ack(buffered.input);
				buffered.input = input;
				buffered.lastWriteMillis = now;
				this.buffer.put(visitId, buffered);
			} else {
				RuterStopVisitData buffnew = new RuterStopVisitData(input, now);
				this.buffer.put(visitId, buffnew);
			}
			
		} else {			
			// Send all old data (assume to be departed)
			for (Iterator<Map.Entry<String, RuterStopVisitData>> it = this.buffer.entrySet().iterator(); it.hasNext();) {
				RuterStopVisitData buffered = ((Map.Entry<String, RuterStopVisitData>)it.next()).getValue();
				
				if (now - buffered.lastWriteMillis > this.waitToSendMillis) {
					
					this.collector.emit(buffered.input.getValues());
					this.collector.ack(buffered.input);
					
					it.remove();
				}
			}		
		}
	}


	public Map<String, Object> getComponentConfiguration() {
		Config conf = new Config();
		conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 1);
		return conf;
	}
	

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(
			"id",
			"visitid",
			"recorded",
			"delay",
			"line",
			"origin",
			"stop",
			"destination",
			"vehicle",
			"vehicleroute",
			"vehiclejourney"
		));
	}
	
	
	
	public void cleanup() {}
}
