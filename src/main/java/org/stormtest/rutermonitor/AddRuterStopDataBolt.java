package org.stormtest.rutermonitor;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class AddRuterStopDataBolt implements IRichBolt {
	private OutputCollector collector;
	// Cached stop data for all stops
	private Map<Integer, RuterStopData> stopProperties; 
	
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.stopProperties = new HashMap<Integer, RuterStopData>(); 
		
		try {
			URL url = new URL("http://reisapi.ruter.no/Place/GetStopsRuter/");
			URLConnection con = url.openConnection();
			con.setRequestProperty("Accept", "application/xml");
			
			DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
			DocumentBuilder b = f.newDocumentBuilder();
			Document doc = b.parse(con.getInputStream());
			
			NodeList stops = doc.getElementsByTagName("Stop");
			for (int i = 0; i < stops.getLength(); i++) {
				try {
					Element stop = (Element)stops.item(i);
					
					// Get properties
					Integer id = Integer.parseInt(stop.getElementsByTagName("ID").item(0).getTextContent());
					String name = stop.getElementsByTagName("Name").item(0).getTextContent();
					String utmZone = "32V";
					Integer utmX = Integer.parseInt(stop.getElementsByTagName("X").item(0).getTextContent());
					Integer utmY = Integer.parseInt(stop.getElementsByTagName("Y").item(0).getTextContent());
					
					// Add to cache
					this.stopProperties.put(id, new RuterStopData(name, utmZone, utmX, utmY));
					
				} catch (Exception e) {}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error downloading stop data: " + e.toString());
		}

	}
	
	
	public void execute(Tuple input) {
		Integer originId = input.getInteger(5);
		Integer stopId = input.getInteger(6);
		Integer destinationId = input.getInteger(7);
		
		if (this.stopProperties.containsKey(originId) && this.stopProperties.containsKey(stopId) && this.stopProperties.containsKey(destinationId)) {
			RuterStopData origin = this.stopProperties.get(originId);
			RuterStopData stop = this.stopProperties.get(stopId);
			RuterStopData destination = this.stopProperties.get(destinationId);
			
			// Emit new tuple with stop id's replaced with names, and current stop position added
			this.collector.emit(input, new Values(
				input.getString(0),
				input.getString(1),
				input.getString(2),
				input.getLong(3),
				input.getInteger(4),
				origin.name,
				stop.name,
				destination.name,
				input.getString(8),
				stop.utmX,
				stop.utmY,
				input.getString(9),
				input.getString(10)
			));
			
			this.collector.ack(input);
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
			"posX",
			"posY",
			"vehicleroute",
			"vehiclejourney"
		));
	}


	public void cleanup() {}
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}

}
