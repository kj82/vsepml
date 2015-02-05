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
package org.vsepml.storm.sensapp.model;

import java.net.URI;

import java.util.ArrayList;
import java.util.List;

public class Composite {
	
	private String name;
	private String description;
	private URI uri;
	private List<URI> sensors;
	
	public Composite() {
		sensors = new ArrayList<URI>();
	}
	
	public Composite(String name, String description, URI uri) {
		this.name = name;
		this.description = description;
		this.uri = uri;
	}
	
	public Composite(String name, String description, URI uri, List<URI> sensors) {
		this.name = name;
		this.description = description;
		this.uri = uri;
		this.sensors = sensors;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public URI getUri() {
		return uri;
	}
	
	public void setUri(URI uri) {
		this.uri = uri;
	}
	
	public List<URI> getSensors() {
		return sensors;
	}
	
	public void setSensors(List<URI> sensors) {
		this.sensors = sensors;
	}
}
