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
package com.apposcopy.synthesis;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import com.apposcopy.model.Sample;
import com.apposcopy.util.Util;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import chord.util.tuple.object.Pair;

public class ReachablePaths {
	
	public static List<String> reachableNodes(String node, Sample s){
		
		List<String> nodes = new ArrayList<String>();
		HashMap<String, Boolean> seen = new HashMap<>();
		
		// FIXME: improve performance
		Queue<String> working = new ArrayDeque<String>();
		working.add(node);
		seen.put(node, true);
		
		while (!working.isEmpty()) {
			String current = working.poll();

			for (Pair<String, String> p : s.getIccg()) {
				if (p.val0.equals(current)) {
					if (!seen.containsKey(p.val1)){
						working.add(p.val1);
						seen.put(p.val1, true);
						nodes.add(p.val1);
					}
				}
			}
		}
		
		return nodes;
	}
	
	
	public static void main(String[] args) throws FileNotFoundException {

		Gson signature = new Gson();
		JsonReader reader = new JsonReader(new FileReader(args[0]));
		Sample signature_sample = signature.fromJson(reader, Sample.class);
		
		List<String> nodes = new ArrayList<>();
		for (String s : signature_sample.getActivities())
			nodes.add(s);
		
		for (String s : signature_sample.getReceivers())
			nodes.add(s);
		
		for (String s : signature_sample.getServices())
			nodes.add(s);
		
		for (String s : nodes){
			List<String> reachable = reachableNodes(s, signature_sample);
			for (String r : reachable){
				signature_sample.getIccg().add(new Pair<String,String>(s,r));
			}
		}
	
		Gson output = new Gson();
		String sigJson = output.toJson(signature_sample);
		Util.persistJson(sigJson, signature_sample.getFamily());
		
	}

}
