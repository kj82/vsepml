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
import backtype.storm.tuple.Values;
import twitter4j.HashtagEntity;
import twitter4j.Status;

public class TweetConverterBolt implements IRichBolt {
	private static final Logger LOG = LoggerFactory.getLogger(TweetConverterBolt.class);
	private OutputCollector collector;

	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		Status tweet = (Status)input.getValue(0);
		
		this.collector.emit(input, new Values(
			tweet.getId(),
			tweet.getCreatedAt().toString(),
			tweet.getUser().getScreenName(),
			tweet.getText()
		));
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
	public void cleanup() {}
	
	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}

}
