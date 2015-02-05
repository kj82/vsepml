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

import java.util.List;

public class NumericalMeasureJsonModel extends MeasureJsonModel {
	
	public NumericalMeasureJsonModel() {
		super();
	}
	
	public NumericalMeasureJsonModel(String bn, String bu) {
		super(bn, bu);
	}
	
	public NumericalMeasureJsonModel(String bn, long bt, String bu, List<ValueJsonModel> e) {
		super(bn, bt, bu, e);
	}
	
	public NumericalMeasureJsonModel appendMeasure(float value, long time) {
		getE().add(new NumericalValueJsonModel(value, time));
		return this;
	}
	
	public NumericalMeasureJsonModel appendMeasure(NumericalMeasure measure) {
		if (!measure.getSensor().equals(getBn())) {
			throw new IllegalArgumentException("Incompatible sensor name: " + measure.getSensor());
		} 
		getE().add(new NumericalValueJsonModel(measure.getValue(), measure.getTime()));
		return this;
	}

	@Override
	public String toString() {
		return "MEASURE MODEL/ bn: " + getBn() + " - bt: " + getBt() + " - bu: " + getBu();
	}
}
