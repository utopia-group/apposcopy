package com.apposcopy.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import chord.util.tuple.object.Pair;

public class Util {
	
	public static final boolean verbose = false;
	
	// Return all connected subgraphs
	public static List<Set<Pair<String, String>>> getSubGraphs(Set<Pair<String, String>> edges, Set<String> nodes){
		
		List<Set<Pair<String, String>>> subgraphs = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		
		for (String node : nodes){
			
			if (seen.contains(node))
				continue;
			
			Set<Pair<String, String>> graph = new HashSet<>();
			Queue<String> working = new LinkedList<>();
			Set<String> graph_node = new HashSet<>();
			
			working.add(node);
			graph_node.add(node);
			
			while (!working.isEmpty()){
				seen.add(working.peek());
				for (Pair<String,String> edge : edges){
					
					if (graph_node.contains(edge.val0)){
						graph.add(edge);
						if (!working.contains(edge.val1) && !seen.contains(edge.val1)){
							working.add(edge.val1);
							graph_node.add(edge.val1);
						}
							
					}
					
					// consider undirected graph
					if (graph_node.contains(edge.val1)){
						graph.add(edge);
						if (!working.contains(edge.val0) && !seen.contains(edge.val0)){
							working.add(edge.val0);
							graph_node.add(edge.val0);
						}
					}
				}
				working.poll();
			}
			
			if (graph.isEmpty())
				graph.add(new Pair<String,String>(node,"Empty"));
			
			subgraphs.add(graph);
			graph_node.clear();
		}
		
		return subgraphs;
	}

	// Given a list of edges of a graph, check if it's connected.
	public static boolean isConnected(Set<Pair<String, String>> edges, Set<String> nodes) {
		if(edges.size() == 0) return false;
		
		Map<String, Set<String>> edgeMap = new HashMap<>();
		Set<String> comps = new HashSet<>();
		for (Pair<String, String> e : edges) {
			String src = e.val0;
			String tgt = e.val1;
			
			comps.add(src);
			comps.add(tgt);
			Set<String> adjSet = new HashSet<>();
			if (edgeMap.containsKey(src)) {
				adjSet = edgeMap.get(src);
				adjSet.add(tgt);
			} else {
				adjSet.add(tgt);
				edgeMap.put(src, adjSet);
			}
			
			Set<String> adjSet2 = new HashSet<>();
			if (edgeMap.containsKey(tgt)) {
				adjSet2 = edgeMap.get(tgt);
				adjSet2.add(src);
			} else {
				adjSet2.add(src);
				edgeMap.put(tgt, adjSet2);
			}
		}

		int compSize = edgeMap.keySet().size();
		assert compSize > 0;
		
		//return false if there is only one component.
		if(compSize == 1 || (compSize != nodes.size())) return false;
		
		String init = edgeMap.keySet().iterator().next();
		LinkedList<String> worklist = new LinkedList<>();
		Set<String> visited = new HashSet<>();

		worklist.add(init);
		while(!worklist.isEmpty()) {
			String workder = worklist.poll();
			if(visited.contains(workder))
				continue;
			
			visited.add(workder);
			worklist.addAll(edgeMap.get(workder));
		}
		

		return (visited.size() == comps.size());
	}
	
	public static void persistJson(String json, String family) {
		try {
			int stamp = Calendar.getInstance().get(Calendar.MILLISECOND);
			String fileName = "./" + family + "_" + stamp + ".json";

			// write converted json data to a file named "file.json"
			FileWriter writer = new FileWriter(fileName);
			writer.write(json);
			writer.close();
			System.out.println("Signature saved to " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
