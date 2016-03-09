package org.stormtest.rutermonitor;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.stormtest.couchbolt.CouchBolt;

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import storm.kafka.bolt.KafkaBolt;

public class MonitorTopology {
	public static void main(String[] args) {
		// Configuration
		Config config = new Config();
		config.put("rutermonitor.reisapi.requestwaitseconds", 20); 		// Time between requests to the Ruter api
		config.put("rutermonitor.reisapi.departurewaitseconds", 60);	// Time since last update when a vehicle is considered as departed
		
		// Stops to monitor
		List<String> monitoredStops = Arrays.asList(
			// Nationaltheatret (subway+tram/bus)
			"3010031",
			"3010032",
			// Solli
			"3010110",
			// Ruseløkka
			"3010126",
			// Observatoriegata
			"3010125",
			
			// Follow subway from Jernbanetorget to Gaustad+Forskningsparken
			"3010011",
			"3010020",
			"3010200",
			"3012305",
			"3012310",
			"3012315",
			"3012320",
			"3010360",
			"3010370",
			
			// Trams from Forsknigsparken to Gaustad
			"3010371",
			"3012323"
		);
		
		// Kafka configuration
		Properties kafkaProps = new Properties();
		kafkaProps.put("metadata.broker.list", "kafka:9092");
		kafkaProps.put("request.required.acks", "1");
		kafkaProps.put("serializer.class", "kafka.serializer.StringEncoder");
		config.put(KafkaBolt.KAFKA_BROKER_PROPERTIES, kafkaProps);
		
		// CouchDB configuration
		config.put(CouchBolt.COUCHDB_URL, "http://couchdb:5984");
		config.put(CouchBolt.COUCHDB_USER, "couchdb");
		config.put(CouchBolt.COUCHDB_PASSWORD, "couchdb");
		
		/* --- Build Storm topology --- */
		TopologyBuilder builder = new TopologyBuilder();
		
		// Wait for departure bolt
		BoltDeclarer waitBolt = builder.setBolt("ruter-wait-departure", new WaitForDepartureBolt());
		
		// Add all stop spouts
		for (String stop : monitoredStops) {
			builder.setSpout("ruter-spout-"+stop, new RuterSpout())
				.addConfiguration("rutermonitor.reisapi.stopref", stop);
			waitBolt.fieldsGrouping("ruter-spout-"+stop, new Fields("id"));
		}
		
		// Enrich data with names of stops
		builder.setBolt("ruter-add-stopdata", new AddRuterStopDataBolt())
			.shuffleGrouping("ruter-wait-departure");
		
		// Write all stop visits to Kafka and CouchDB
		RuterStopvisitSerializer stopvisitSer = new RuterStopvisitSerializer();
		
		builder.setBolt("ruter-kafka-stopvisit",
				new KafkaBolt<String,String>()
					.withTopicSelector(stopvisitSer)
					.withTupleToKafkaMapper(stopvisitSer)
				)
			.shuffleGrouping("ruter-add-stopdata");
		
		builder.setBolt("ruter-couchdb-stopvisit",
				new CouchBolt(stopvisitSer)
					.withBatching(5, 20)
				)
			.shuffleGrouping("ruter-add-stopdata");

		
		
		
		// Stream tweets from twitter
		builder.setSpout("twitter-spout", new TwitterSpout());
		
		// Convert them to tuple
		builder.setBolt("tweet-converter", new TweetConverterBolt())
			.shuffleGrouping("twitter-spout");
		
		// Filter based on some words
		builder.setBolt("tweet-filter", new TweetWordFilterBolt())
			.addConfiguration("rutermonitor.twitter.words", new String[] { "ruter","nsb","trikk","tog","buss","tbane" })
			.shuffleGrouping("tweet-converter");
		
		// Write all tweets to Kafka and CouchDB
		TweetSerializer tweetSer = new TweetSerializer();
		
		builder.setBolt("tweet-kafka",
				new KafkaBolt<String,String>()
					.withTopicSelector(tweetSer)
					.withTupleToKafkaMapper(tweetSer)
				)
			.shuffleGrouping("tweet-converter");
		
		builder.setBolt("tweet-couchdb",
				new CouchBolt(tweetSer)
					.withBatching(5, 20)
				)
			.shuffleGrouping("tweet-converter");
			

		/* --- Submit topology to Storm infrastructure --- */
		try {
			StormSubmitter.submitTopology("ruter-monitoring", config, builder.createTopology());
		} catch (AlreadyAliveException | InvalidTopologyException | AuthorizationException e) {
			e.printStackTrace();
		}
	}
}
