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
 * Each TypeVar denotes a type(i, c, t): i is the index of the sample, c is the id of 
 * of the component, and t is the type of the component (activity, service or receiver).
 * 
 * @author yufeng
 *
 */
public class TypeVar extends Variable {

	private String name;
	
	// 1: activity; 2: service; 3: receiver
	private int type;

	public TypeVar(int id, int index, String n, int t) {
		super(id, index);
		name = n;
		type = t;
	}

	public int getType() {
		return type;
	}

	public void setType(int t) {
		this.type = t;
	}

	public String getName() {
		return name;
	}

	public void setName(String n) {
		this.name = n;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" index:").append(index).append(" type:").append(type)
				.append(" solverId:" + (solverId+1));
		return sb.toString();
	}

}
