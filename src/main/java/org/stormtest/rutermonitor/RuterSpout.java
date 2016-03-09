package org.stormtest.rutermonitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class RuterSpout implements IRichSpout {
	private static final Logger LOG = LoggerFactory.getLogger(RuterSpout.class);
	private SpoutOutputCollector collector;
	// Configuration
	private String stopRef;
	private Long requestFreqMillis;
	// Request reader objects
	private URL url;
	private DocumentBuilderFactory docBuildFact;
	private DocumentBuilder docBuild;
	private Long lastRequestTime;
	

	public void open(Map config, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
		
		this.stopRef = (String)config.get("rutermonitor.reisapi.stopref");
		this.requestFreqMillis = ((Long)config.get("rutermonitor.reisapi.requestwaitseconds"))*1000;
		this.lastRequestTime = System.currentTimeMillis()-this.requestFreqMillis;
		
		try {
			url = new URL("http://reisapi.ruter.no/StopVisit/GetDepartures/" + stopRef);
			docBuildFact = DocumentBuilderFactory.newInstance();
			docBuild = docBuildFact.newDocumentBuilder();
			
			LOG.info("Monitoring Ruter stop id: "+this.stopRef);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Error parsing URL: " + "http://reisapi.ruter.no/StopVisit/GetDepartures/" + stopRef);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Parser configuration exception: " + e.toString());
		}
	}
	
	
	public void close() {
	}


	public void nextTuple() {
		if (System.currentTimeMillis() - this.lastRequestTime < this.requestFreqMillis) {
			// Sleep a little bit, then do nothing, until we waited long enough
			Utils.sleep(1);
			return;
		} else {
			try {
				// Get XML document
				URLConnection con = this.url.openConnection();
				con.setRequestProperty("Accept", "application/xml");
				Document doc = docBuild.parse(con.getInputStream());
				
				// Get all StopVisits
				NodeList visits = doc.getElementsByTagName("MonitoredStopVisit");
				for (int i = 0; i < visits.getLength(); i++) {
					try {
						Element visit = (Element)visits.item(i);
						String delayStr = visit.getElementsByTagName("Delay").item(0).getTextContent();
						if (!delayStr.isEmpty()) {
							// Discard if no delay is set, this is a planned stop way in the future
							String originRef = visit.getElementsByTagName("OriginRef").item(0).getTextContent();
							String vehicleRef = visit.getElementsByTagName("VehicleRef").item(0).getTextContent();
							
							ZonedDateTime recorded = ZonedDateTime.parse(visit.getElementsByTagName("RecordedAtTime").item(0).getTextContent());
							Duration delay = Duration.parse(delayStr);
							Integer line = Integer.parseInt(visit.getElementsByTagName("LineRef").item(0).getTextContent());
							Integer originId = Integer.parseInt(visit.getElementsByTagName("OriginRef").item(0).getTextContent());
							Integer stopId = Integer.parseInt(stopRef);
							Integer destinationId = Integer.parseInt(visit.getElementsByTagName("DestinationRef").item(0).getTextContent());
							String vehicle = visit.getElementsByTagName("VehicleMode").item(0).getTextContent(); 
							
							String vehicleRoute = originId+"-"+vehicleRef+"-"+destinationId;
							String vehicleJourney = visit.getElementsByTagName("VehicleJourneyName").item(0).getTextContent(); 
	
							// Send out of spout
							UUID id = UUID.randomUUID();
							String visitId = originRef + "-" + vehicleRef + "-" + this.stopRef;
							this.collector.emit(new Values(
								id.toString(),
								visitId,
								recorded.toString(),
								delay.getSeconds(),
								line,
								originId,
								stopId,
								destinationId,
								vehicle,
								vehicleRoute,
								vehicleJourney
							), id);
						}
					} catch (Exception e) {}
				}
			} catch (Exception e) {
				throw new RuntimeException("Error reading from web: " + e.toString());
			}
		}
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
	

	public Map<String, Object> getComponentConfiguration() {
		return null;
	}
	
	public void activate() {}
	public void deactivate() {}

	// TODO: Make handling of ack and fail to guarantee processing
	public void ack(Object id) {}
	public void fail(Object id) {}
}
