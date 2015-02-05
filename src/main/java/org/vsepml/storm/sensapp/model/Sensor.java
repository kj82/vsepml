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

public class Sensor {
	
	private String name;
	private URI uri;
	private String description;
	private String backend;
	private String template;
	private String unit;
	private boolean uploaded = false;
	
	public Sensor() {
	}
	
	public Sensor(String name, URI uri, String description, String backend, String template, String unit, boolean uploaded) {
		this.name = name;
		this.uri = uri;
		this.description = description;
		this.backend = backend;
		this.template = template;
		this.unit = unit;
		this.uploaded = uploaded;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getBackend() {
		return backend;
	}
	
	public void setBackend(String backend) {
		this.backend = backend;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public void setTemplate(String template) {
		this.template = template;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public boolean isUploaded() {
		return uploaded;
	}
	
	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
	}

}
