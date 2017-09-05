package com.apposcopy.synthesis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;

public class SignatureDiff {


	static Integer getScore(Sample s, ArrayList<Integer> weights) {
		
		int w0 = weights.get(0);
		int w1 = weights.get(1);
		int w2 = weights.get(2);
		int w3 = weights.get(3);
		
		
		int score = s.getIntentFilters().size() * w0 + s.getDangerAPIs().size() * w1
				+ s.getTaintFlows().size() * w2;
		
		score += (s.getActivities().size() + s.getReceivers().size() + s.getServices().size()) * w3;
		
		
		return score;
	}
	
	public static void readFile(String file, Map<String,Integer> map){
		BufferedReader br = null;

		try {

			String sCurrentLine;

			br = new BufferedReader(new FileReader(file));

			while ((sCurrentLine = br.readLine()) != null) {
				String[] parts = sCurrentLine.split(" & ");
				map.put(parts[0], Integer.parseInt(parts[1]));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	static Integer getScoreFrequency(Sample s, HashMap<String,Integer> map){
		
		int score = 0;

		for (Pair<String, String> p : s.getIntentFilters()) {
			int weight = 10000;
			if (map.containsKey(p.val1)) {
				//weight = Math.floorDiv(10000, map.get(p.val1));
				weight = (int) Math.floor((double)10000/map.get(p.val1));
			}
			//System.out.println("filter: " + p.val1 + " weight: " + weight);
			score += weight;
		}
		
		for (Pair<String, String> p : s.getDangerAPIs()) {
			int weight = 10000;
			if (map.containsKey(p.val1)) {
				//weight = Math.floorDiv(10000, map.get(p.val1));
				weight = (int) Math.floor(10000/map.get(p.val1));
			}
			//System.out.println("danger: " + p.val1 + " weight: " + weight);
			score += weight;
		}
		
		for (Quad<String,String,String,String> p : s.getTaintFlows()){
			int weight = 10000;
			
			String q = "<" + p.val1 + ", " + p.val3 +  ">";
			
			if (map.containsKey(q)) {
				//weight = Math.floorDiv(10000, map.get(q));
				weight = (int) Math.floor((double)10000/map.get(q));
			}
			//System.out.println("taint: " + q + " weight: " + weight);
			score += weight;
		}
		
		//System.out.println("SCORE: " + score);
		return score;
		
	}
		

	static boolean isMalware(Sample s1, Sample s2, int threshold, int function, HashMap<String,Integer> map) {

		ArrayList<Integer> weights = new ArrayList<Integer>();
		for (int i = 0; i < 4; i++)
			weights.add(1);

		weights.set(2, s1.getActivities().size() + s1.getReceivers().size() + s1.getServices().size() + 1);
		weights.set(1, weights.get(2) * s1.getTaintFlows().size() + 1);
		weights.set(0, weights.get(1) * s1.getDangerAPIs().size() + 1);
		
		int sum_sample1 = 0;
		int sum_sample2 = 0;
		
		if (function == 1){
			sum_sample1 = getScoreFrequency(s1, map);
			sum_sample2 = getScoreFrequency(s2, map);
		} else {
			sum_sample1 = getScore(s1, weights);
			sum_sample2 = getScore(s2, weights);
		}

		//System.out.println("Nodes: " +  (s1.getActivities().size() + s1.getReceivers().size() + s1.getServices().size()) + " Taints: " + s1.getTaintFlows().size() + " DangerAPIs: " + s1.getDangerAPIs().size() + " IntentFilter: " + s1.getIntentFilters().size());
		//System.out.println("Nodes: " +  (s2.getActivities().size() + s2.getReceivers().size() + s2.getServices().size()) + " Taints: " + s2.getTaintFlows().size() + " DangerAPIs: " + s2.getDangerAPIs().size() + " IntentFilter: " + s2.getIntentFilters().size());

		int score = (int) Math.floor((double) sum_sample2 / sum_sample1 *10000);
		//System.out.println("SCORE: " + (double) sum_sample2 / sum_sample1);
		System.out.println("SCORE: " + score);

		if (1-(double)sum_sample2/sum_sample1 < threshold)
			return true;
		else
			return false;
	}

	public static void main(String[] args) throws FileNotFoundException {

		Gson signature = new Gson();
		JsonReader reader = new JsonReader(new FileReader(args[0]));
		Sample signature_sample = signature.fromJson(reader, Sample.class);

		Gson target = new Gson();
		reader = new JsonReader(new FileReader(args[1]));
		Sample target_sample = target.fromJson(reader, Sample.class);

		int limit = Integer.parseInt(args[2]);
		
		int function = Integer.parseInt(args[3]);
		//int weights = Integer.parseInt(args[4]);
		//int nodes = Integer.parseInt(args[5]);
		
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		String filename = args[4];
		readFile(filename,map);

		if (isMalware(signature_sample, target_sample, limit, function, map))
			System.out.println("Malware");
		else
			System.out.println("Benign");

	}

}
