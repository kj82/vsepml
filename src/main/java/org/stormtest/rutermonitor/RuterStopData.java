package org.stormtest.rutermonitor;

public class RuterStopData {
	public String name;
	public String utmZone;
	public Integer utmX;
	public Integer utmY;
	
	public RuterStopData(String name, String utmZone, Integer utmX, Integer utmY) {
		this.name = name;
		this.utmZone = utmZone;
		this.utmX = utmX;
		this.utmY = utmY;
	}
}
