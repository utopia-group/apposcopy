package com.apposcopy.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.apposcopy.model.Sample;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;

public class Obfuscator {

	public static void main(String[] args) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		int limit = 5;

		assert args.length > 0;
		String loc = args[0];
		String tgtLoc = args[1];
		Gson gson = new Gson();
		List<String> comps = new ArrayList<>();

		File locFile = new File(loc);
		String name = locFile.getName();
		String family = locFile.getParentFile().getName();
		System.out.println("family:" + family);
		String tgtName = stripExtension(name) + "_obs.json";
		System.out.println("Obfuscating " + loc + " tgt loc: " + tgtLoc);
		tgtLoc = tgtLoc + family;
		if (!new File(tgtLoc).exists()) {
			System.out.println("folder not exist: " + tgtLoc);
			new File(tgtLoc).mkdir();
		}
		tgtLoc = tgtLoc + "/" + tgtName;

		Sample sample = gson.fromJson(new FileReader(loc), Sample.class);
		System.out.println("Original ICCG------------------------");
		dumpICCG(sample);

		comps.addAll(sample.getActivities());
		comps.addAll(sample.getReceivers());
		comps.addAll(sample.getServices());

		// step1: Randomly removing one iccg edge
		// Pair<String, String> edge = null;
		// for (Pair<String, String> e : sample.getIccg()) {
		// String src = e.val0;
		// String tgt = e.val1;
		//
		// if (sample.getActivities().contains(src)
		// && (!sample.getReceivers().contains(tgt))) {
		// edge = e;
		// break;
		// }
		// }
		//
		// if (edge != null) {
		// System.out.println("Remove edge--------------" + edge);
		// LinkedList<Pair<String, String>> tgtIccg = new LinkedList<>(
		// sample.getIccg());
		// tgtIccg.remove(edge);
		// sample.setIccg(tgtIccg);
		// }

		// step2: Randomly drop/add one taint flow
		List<Quad<String, String, String, String>> orgFlows = sample.getTaintFlows();
		for (int i = 0; i < limit; i++) {
			if (!orgFlows.isEmpty()) {
				List<Quad<String, String, String, String>> delFlows = pickNRandom(orgFlows, 1);
				Quad<String, String, String, String> delFlow = delFlows.get(0);
				LinkedList<Quad<String, String, String, String>> tgtFlows = new LinkedList<>(orgFlows);
				tgtFlows.remove(delFlow);
				sample.setTaintFlows(tgtFlows);
			}
		}

		// step3: Randomly add one dummy service and iccg edges
		assert comps.size() > 1 : comps.size();
		for (int i = 0; i < limit; i++) {
			if (!sample.getIccg().isEmpty()) {

				List<Pair<String, String>> ranLst = pickNRandom(sample.getIccg(), 1);
				Pair<String, String> ranEdge = ranLst.get(0);
				// ensure that there is an edge from comp1 to comp2.
				String comp1 = ranEdge.val0;
				String comp2 = ranEdge.val1;
				String dummyService = "dummyService";
				sample.getServices().add(dummyService);
				sample.getIccg().add(new Pair<>(comp1, dummyService));
				sample.getIccg().add(new Pair<>(dummyService, comp2));
				if (ranEdge != null) {
					System.out.println("Remove edge because of dummy node--------------" + ranEdge);
					LinkedList<Pair<String, String>> tgtIccg = new LinkedList<>(sample.getIccg());
					tgtIccg.remove(ranEdge);
					sample.setIccg(tgtIccg);
				}
			}
		}

		System.out.println("updated ICCG-------------------------------" + tgtName);
		dumpICCG(sample);
		String json = gson.toJson(sample);
		saveToFile(tgtLoc, json);
	}

	public static <T> List<T> pickNRandom(List<T> lst, int n) {
		List<T> copy = new LinkedList<T>(lst);
		Collections.shuffle(copy);
		return copy.subList(0, n);
	}

	public static void dumpICCG(Sample sample) {
		System.out.println("#nodes: "
				+ (sample.getActivities().size() + sample.getReceivers().size() + sample.getServices().size()));
		System.out.println("#edges: " + sample.getIccg().size());
		System.out.println("#flows: " + sample.getTaintFlows().size());
	}

	public static String stripExtension(String str) {
		// Handle null case specially.
		if (str == null)
			return null;

		// Get position of last '.'.
		int pos = str.lastIndexOf(".");

		// If there wasn't any '.' just return the string as is.
		if (pos == -1)
			return str;

		// Otherwise return the string, up to the dot.
		return str.substring(0, pos);
	}

	public static void saveToFile(String path, String content) {
		System.out.println("saving path:" + path);
		try {
			File file = new File(path);
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(content);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
