package com.apposcopy.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class FreqAnalysis {

	// calculate per app or per each component.
	private static boolean perApp = true;

	public static void main(String[] args) throws JsonSyntaxException,
			JsonIOException, FileNotFoundException {
		String loc = args[0];

		Map<String, Integer> iftMap = new HashMap<>();
		Map<Pair<String, String>, Integer> flowMap = new HashMap<>();
		Map<String, Integer> apiMap = new HashMap<>();

		File folder = new File(loc);
		Gson gson = new Gson();
		int counter = 0;
		
		for (File file : folder.listFiles()) {
			counter++;
			System.out.println(file);
			Sample sample = gson.fromJson(new FileReader(file), Sample.class);
			Set<Pair<String, String>> visitedFlow = new HashSet<>();
			Set<String> visitedApi = new HashSet<>();

			for (Pair<String, String> pair : sample.getIntentFilters()) {
				String ift = pair.val1;
				if (iftMap.containsKey(ift)) {
					int val = iftMap.get(ift);
					val++;
					iftMap.put(ift, val);
				} else {
					iftMap.put(ift, 1);
				}
			}

			for (Pair<String, String> pair : sample.getDangerAPIs()) {
				String api = pair.val1;
				if (perApp && visitedApi.contains(api))
					continue;
				else
					visitedApi.add(api);

				if (apiMap.containsKey(api)) {
					int val = apiMap.get(api);
					val++;
					apiMap.put(api, val);
				} else {
					apiMap.put(api, 1);
				}
			}

			for (Quad<String, String, String, String> quad : sample
					.getTaintFlows()) {
				String src = quad.val1;
				String sink = quad.val3;
				Pair<String, String> flow = new Pair<>(src, sink);
				if (perApp && visitedFlow.contains(flow))
					continue;
				else
					visitedFlow.add(flow);

				if (flowMap.containsKey(flow)) {
					int val = flowMap.get(flow);
					val++;
					flowMap.put(flow, val);
				} else {
					flowMap.put(flow, 1);
				}
			}
		}

		
		System.out.println("Total applications: " + counter);
		System.out.println("Intent Filter----------------");
		for (String key : iftMap.keySet()) {
			System.out.println(key + ": " + iftMap.get(key));
		}
		System.out.println("Danger API----------------");
		for (String key : apiMap.keySet()) {
			System.out.println(key + ": " + apiMap.get(key));
		}
		System.out.println("Taint Flow----------------");
		for (Pair<String, String> key : flowMap.keySet()) {
			System.out.println(key + ": " + flowMap.get(key));
		}
	}
}
