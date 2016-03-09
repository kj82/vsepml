package org.stormtest.rutermonitor;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class TweetWordFilterBolt implements IRichBolt {
	private static final Logger LOG = LoggerFactory.getLogger(TweetWordFilterBolt.class);
	private OutputCollector collector;
	private String[] filterWords;

	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.filterWords = (String[])conf.getOrDefault("rutermonitor.twitter.words", new String[] {});
	}

	@Override
	public void execute(Tuple input) {
		String text = input.getString(3);
		for (String word : filterWords) {
			if (text.contains(word)) {
				this.collector.emit(input, input.getValues());
				break;
			}
		}
		this.collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(
			"id",
			"created",
			"user",
			"text"
		));
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
	
	@Override
	public void cleanup() {}
}
