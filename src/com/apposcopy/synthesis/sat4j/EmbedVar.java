/*
 * Copyright (C) 2017 The Apposcopy and Astroid Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apposcopy.synthesis.sat4j;

/**
 * Each EmbedVar denote a embed(i,x,y): x is the component of the 
 * target signature and y is the component of the sample. 
 * i is the index of the sample.
 * 
 * @author yufeng
 *
 */
public class EmbedVar extends Variable {

	private String mapSample;

	private String mapSig;

	public String getMapSample() {
		return mapSample;
	}

	public void setMapSample(String mapSample) {
		this.mapSample = mapSample;
	}

	public String getMapSig() {
		return mapSig;
	}

	public void setMapSig(String mapSig) {
		this.mapSig = mapSig;
	}

	public EmbedVar(int id, int i, String sig, String sample) {
		super(id, i);
		mapSample = sample;
		mapSig = sig;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(mapSample).append(" tgt:").append(mapSig)
				.append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
