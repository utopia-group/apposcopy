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
 * Each IntentFilterVar denote a filter(i,s,t,f): s is the source component and t is
 * the target component. i is the index. f is the filter type.
 * 
 * @author yufeng
 *
 */
public class IntentFilterVar extends Variable {

	public String getCompSrc() {
		return compSrc;
	}

	public void setCompSrc(String compSrc) {
		this.compSrc = compSrc;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String f) {
		this.filter = f;
	}

	// compSrc and compTgt will always be the same!
	private String compSrc;

	private String filter;

	public IntentFilterVar(int id, int i, String s, String f) {
		super(id, i);
		compSrc = s;
		filter = f;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(compSrc).append(" filter:")
				.append(filter).append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
