package org.stormtest.couchbolt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stormtest.rutermonitor.RuterSpout;

import com.esotericsoftware.minlog.Log;

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;
import backtype.storm.utils.TupleUtils;

public class CouchBolt implements IRichBolt {
	private static final Logger LOG = LoggerFactory.getLogger(CouchBolt.class);
	
	public static String COUCHDB_URL = "stormtest.couchdb.url";
	public static String COUCHDB_USER = "stormtest.couchdb.user";
	public static String COUCHDB_PASSWORD = "stormtest.couchdb.passwd";
	public static String COUCHDB_RETRIES = "stormtest.couchdb.retries";
	
	private OutputCollector collector;
	private TupleToJson serializer;

	private URL url;
	private String encodedAuth = null;
	
	private int flushIntervalSecs = 0;
	private int batchSize = 0;
	
	private LinkedList<Tuple> buffer;
	private long lastRequestTime;
	private int retries;
	private int currentTry;
	
	
	
	public CouchBolt(TupleToJson serializer) {
		this.serializer = serializer;
	}
	
	public CouchBolt withBatching(int flushIntervalSecs, int batchSize) {
		this.flushIntervalSecs = flushIntervalSecs;
		this.batchSize = batchSize;
		return this;
	}

	@Override
	public void prepare(Map config, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.buffer = new LinkedList<>();
		
		this.retries = (int)config.getOrDefault(COUCHDB_RETRIES, 3);
		this.currentTry = 0;
		this.lastRequestTime = System.currentTimeMillis();
		
		String surl = (String)config.get(COUCHDB_URL) + "/" + serializer.getDatabase();
		
		try {
			URL dbUrl = new URL(surl);
			
			if (config.containsKey(COUCHDB_USER) && config.containsKey(COUCHDB_PASSWORD)) {
				String userpass = config.get(COUCHDB_USER) + ":" + config.get(COUCHDB_PASSWORD);
				this.encodedAuth = Base64.getEncoder().encodeToString(userpass.getBytes());
			}
			
			// Connect to CouchDB and make sure that the database exists, or create it
			HttpURLConnection con = (HttpURLConnection)dbUrl.openConnection();
			con.setRequestMethod("PUT");
			con.setRequestProperty("Accept", "application/json");
			if (this.encodedAuth != null) con.setRequestProperty("Authorization", "Basic " + this.encodedAuth);
			
			switch (con.getResponseCode()) {
				case HttpURLConnection.HTTP_CREATED:
				case HttpURLConnection.HTTP_PRECON_FAILED:
					// Created or exits
					this.url = new URL(surl + "/_bulk_docs");
					break;
				case HttpURLConnection.HTTP_BAD_REQUEST:
					throw new RuntimeException("Error creating database: " + serializer.getDatabase());
				case HttpURLConnection.HTTP_UNAUTHORIZED:
					throw new RuntimeException("Bad username/password for CouchDB: " + surl);
				default:
					throw new RuntimeException("Error connecting to CouchDB url: " + surl);
			}

		} catch (IOException e) {
			throw new RuntimeException("Error connecting to CouchDB url: " + surl);
		}
	}

	@Override
	public void execute(Tuple input) {
		boolean flush = false;
		
		// Add new incoming tuple and decide if it is time to flush
		if (TupleUtils.isTick(input)) {
			flush = true;
		} else {
			this.buffer.add(input);
			if (this.buffer.size() >= this.batchSize) flush = true;
		}
		
		if (flush) {
			// If this is a flush retry, wait for flushtime before we try again
			if (this.currentTry > 0 && (System.currentTimeMillis()-this.lastRequestTime) < this.flushIntervalSecs*1000) return;
			if (this.buffer.size() < 1) return;
			
			this.lastRequestTime = System.currentTimeMillis();
			boolean success = false;
			try {
				// Time to write some documents
				HttpURLConnection con = (HttpURLConnection)this.url.openConnection();
				con.setDoOutput(true);
				con.setRequestMethod("POST");
				con.setRequestProperty("Accept", "application/json");
				con.setRequestProperty("Content-Type", "application/json");
				if (this.encodedAuth != null) con.setRequestProperty("Authorization", "Basic " + this.encodedAuth);
				
				// Write all documents in buffer (up to batch size) to the request
				Writer request = new OutputStreamWriter(con.getOutputStream(), "UTF8");
				// Documents start
				request.write("{ \"docs\": [");
				int counter = 0;
				for (Iterator it = buffer.iterator(); it.hasNext();) {
					request.write(this.serializer.getJSONString((Tuple)it.next()));
					if (counter++ >= this.batchSize || !it.hasNext()) break;
					request.write(",");
				}
				// Documents end
				request.write("] }");
				request.close();
				
				// Read response from server
				Reader response = new InputStreamReader(con.getInputStream(), "UTF8");
				Object jsonReturn = JSONValue.parseWithException(response);
				
				if (con.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
					// Note all successful inserts
					Set<String> insertedIds = new HashSet<>();
					JSONArray docReturns = (JSONArray)jsonReturn;
					for (Object obj : docReturns) {
						JSONObject doc = (JSONObject)obj;
						if (doc.containsKey("ok")) insertedIds.add((String)doc.get("id"));
					}
					
					// Check that requested documents in buffer succeeded
					success = true;
					for (Iterator it = buffer.iterator(); it.hasNext();) {
						Tuple buffered = (Tuple)it.next();
						if (insertedIds.contains(serializer.getId(buffered))) it.remove();
						else success = false;
						if (--counter < 1) break;
					}
					if (!success) LOG.error("CoucDB response did not match request");
				} else {
					LOG.error("CouchDB HTTP code: " + con.getResponseCode());
				}
			} catch (IOException | ParseException e) {
				LOG.error("CouchDB: " + e.toString());
			}
			
			// If something failed, we need to try again
			if (success) this.currentTry = 0;
			else this.currentTry++;
		}
		
		// Crash if we tried too many times
		if (this.currentTry > this.retries)
			throw new RuntimeException("Could not write documents to CouchDB: " + this.url.toString());
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {}
	
	@Override
	public void cleanup() {}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		if (flushIntervalSecs > 0) {
			Config conf = new Config();
			conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, flushIntervalSecs);
			return conf;
		} else {
			return null;
		}
	}
}
