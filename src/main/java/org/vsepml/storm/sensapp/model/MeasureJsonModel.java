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

import java.util.ArrayList;
import java.util.List;


abstract public class MeasureJsonModel {

	private String bn;
	private long bt;
	private String bu;
	private List<ValueJsonModel> e;
	
	public MeasureJsonModel() {
		e = new ArrayList<ValueJsonModel>();
	}
	
	public MeasureJsonModel(String bn, String bu) {
		this.bn = bn;
		this.bu = bu;
		e = new ArrayList<ValueJsonModel>();
	}
	
	public MeasureJsonModel(String bn, long bt, String bu, List<ValueJsonModel> e) {
		this.bn = bn;
		this.bt = bt;
		this.bu = bu;
		this.e = e; 
	}
	
	public String getBn() {
		return bn;
	}
	public void setBn(String bn) {
		this.bn = bn;
	}
	public long getBt() {
		return bt;
	}
	public void setBt(long bt) {
		this.bt = bt;
	}
	public String getBu() {
		return bu;
	}
	public void setBu(String bu) {
		this.bu = bu;
	}
	public List<ValueJsonModel> getE() {
		return e;
	}
	public void setE(List<ValueJsonModel> e) {
		this.e = e;
	}

	public void clearValues() {
		e.clear();
	}
	
	@Override
	public String toString() {
		return "MEASURE MODEL/ bn: " + bn + " - bt: " + bt + " - bu: " + bu;
	}
}
