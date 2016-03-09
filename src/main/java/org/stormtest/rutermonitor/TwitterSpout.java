package org.stormtest.rutermonitor;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterSpout implements IRichSpout {
	private static final Logger LOG = LoggerFactory.getLogger(TwitterSpout.class);
	private SpoutOutputCollector collector;
	// Twitter objects
	private FilterQuery tweetQuery;
	private StatusListener tweetListener;
	private TwitterStream tweetStream;
	private LinkedBlockingQueue<Status> tweetQueue;
	

	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
		this.tweetQueue = new LinkedBlockingQueue<>();
		
		/* Twitter configuration */
		ConfigurationBuilder twitterConf = new ConfigurationBuilder();
		twitterConf.setIncludeEntitiesEnabled(true);
        twitterConf.setUseSSL(true);
        twitterConf.setOAuthAccessToken("245517904-zc4hDdkzNtE7BtZZeJKwfJwYxEerAmEG6zQFFxrr");
        twitterConf.setOAuthAccessTokenSecret("dpiMvHiOEDDOOFQYA2XscjdYvQQBFi2zZ3ltqC2rDVhcL");
        twitterConf.setOAuthConsumerKey("Q72roNaucCqUbCuF1gsKQBoKE");
        twitterConf.setOAuthConsumerSecret("INxlME7kJdwv8RZtIqd4dzkYachqc7tERQNLL92iuER9PMbxKd");
		
        /* Filter what tweets we listen to */
		this.tweetQuery = new FilterQuery()
			.follow(new long[] { 22011813, 28533825 })													// Follow Ruter & NSB, 
			.track(new String[] { "ruter","nsb","oslo","trikk","tbane","tog","buss","norge" })			// some hashtags
			.locations(new double[][] { new double[] { 10.1, 59.6 }, new double[] { 11.2, 60.1 }});		// and all tweets from Oslo/Akershus
			//.locations(new double[][] { new double[] { 3.6, 57.8 }, new double[] { 32.8, 71.3 }});	// and all tweets from Norway++
		
		/* What to do with tweets */
		this.tweetListener = new StatusListener() {
			@Override
			public void onStatus(Status tweet) {
				tweetQueue.offer(tweet);
			}
			
			@Override
			public void onException(Exception e) {
				LOG.error("StatusListener: " + e.toString());
			}
			
			@Override
			public void onTrackLimitationNotice(int missed) {
				LOG.warn("StatusListener: Missed tweets " + missed);
			}

			@Override
			public void onStallWarning(StallWarning w) {
				LOG.warn("StatusListener: " + w.toString());
			}
			
			@Override
			public void onScrubGeo(long arg0, long arg1) {}
			
			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {}
		};
		
		LOG.info("TweetQuery: " + this.tweetQuery.toString());
		
		/* Initiate tweet stream */
		this.tweetStream = new TwitterStreamFactory(twitterConf.build()).getInstance();
		this.tweetStream.addListener(this.tweetListener);
		this.tweetStream.filter(this.tweetQuery);
		//this.tweetStream.sample();
	}

	@Override
	public void nextTuple() {
		while (!this.tweetQueue.isEmpty()) {
			Status tweet = this.tweetQueue.poll();
			if (tweet != null) collector.emit(new Values(tweet), tweet.getId());
		}
		Utils.sleep(1);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(
			"tweet"
		));
	}

	@Override
	public void close() {
		this.tweetStream.cleanUp();
		this.tweetStream.shutdown();
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
	
	@Override
	public void activate() {}
	@Override
	public void deactivate() {}

	@Override
	public void ack(Object msgId) {}
	@Override
	public void fail(Object msgId) {}
}
