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
package org.vsepml.storm.twitter;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Created by ferrynico on 10/01/15.
 * Inspired by https://github.com/davidkiss/storm-twitter-word-count
 */
public class StormTwitterStreamSpout extends BaseRichSpout {

    private static final Logger journal = Logger.getLogger(StormTwitterStreamSpout.class.getName());

    private static SpoutOutputCollector collector;
    private TwitterStream twitterStream;
    private LinkedBlockingQueue<Status> queue;

    private String accessToken;
    private String accessTokenSecret;
    private String consumerKey;
    private String consumerSecret;

    public StormTwitterStreamSpout(String accessToken, String accessTokenSecret, String consumerKey, String consumerSecret){
        super();
        this.accessToken=accessToken;
        this.accessTokenSecret=accessTokenSecret;
        this.consumerKey=consumerKey;
        this.consumerSecret=consumerSecret;
    }

    @Override
    public final void close() {
        this.twitterStream.cleanUp();
        this.twitterStream.shutdown();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("tweet"));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        this.collector = spoutOutputCollector;
        queue = new LinkedBlockingQueue<Status>(1000);

        StatusListener listener = new StatusListener(){
            public void onStatus(Status status) {
                queue.offer(status);
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}

            @Override
            public void onScrubGeo(long l, long l1) {}

            @Override
            public void onStallWarning(StallWarning stallWarning) {}

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };


        ConfigurationBuilder twitterConf = new ConfigurationBuilder();
        twitterConf.setIncludeEntitiesEnabled(true);
        twitterConf.setUseSSL(true);
        twitterConf.setUserStreamRepliesAllEnabled(true);
        twitterConf.setOAuthAccessToken(accessToken);
        twitterConf.setOAuthAccessTokenSecret(accessTokenSecret);
        twitterConf.setOAuthConsumerKey(consumerKey);
        twitterConf.setOAuthConsumerSecret(consumerSecret);


        twitterStream = new TwitterStreamFactory(twitterConf.build()).getInstance();
        twitterStream.addListener(listener);
        // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
        twitterStream.sample();
    }

    @Override
    public void nextTuple() {
        Status st = queue.poll();
        if (st == null) {
            Utils.sleep(100);
        } else {
            collector.emit(new Values(st));
        }
    }
}
