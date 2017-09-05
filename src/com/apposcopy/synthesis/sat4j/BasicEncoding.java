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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.StringTokenizer;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;

import com.apposcopy.model.Sample;
import com.apposcopy.synthesis.sat4j.Constraint.ConstraintType;
import com.apposcopy.util.SampleComparator;
import com.apposcopy.util.Util;

/**
 * Basic SAT-based encoding for malware signature -- the number of variables may
 * be too large for real experiments.
 * 
 * @author osbert
 * @author yufeng
 * @author Ruben
 */
public class BasicEncoding implements Encoding {

	/**
	 * Members of Encoding class
	 */
	protected List<Constraint> problem_constraints;
	protected List<Constraint> conflict_constraints;

	List<Sample> samples;
	protected int id_reference_sample;

	protected List<Variable> list_variables;
	protected List<EdgeVar> node_variables;

	protected List<Variable> node_activities;
	protected List<Variable> node_receivers;
	protected List<Variable> node_services;
	protected List<EdgeVar> edge_variables;

	protected Map<Pair<String, String>, Variable> map_activities;
	protected Map<Pair<String, String>, Variable> map_services;
	protected Map<Pair<String, String>, Variable> map_receivers;

	protected Map<Pair<Integer, String>, Set<String>> intent_filters;
	protected Map<Variable, Set<String>> number_intent_filters;

	protected Map<Pair<Integer, String>, Set<String>> danger_apis;
	protected Map<Variable, Set<String>> number_danger_apis;

	protected Map<Pair<Integer, String>, Set<String>> tainted_flow;
	protected Map<Variable, Set<String>> number_tainted_flow;

	protected List<Variable> blocked_variables;

	protected int index_var = 0;

	protected boolean is_connected = false;

	protected List<Constraint> objective_functions;

	protected Sample sample;
	
	protected HashMap<String,Integer> frequency;

	/**
	 * Constructors
	 * @param map 
	 */
	public BasicEncoding(List<Sample> s, HashMap<String, Integer> map) {
		samples = s;

		Collections.sort(samples, new SampleComparator());
		id_reference_sample = 0;

		problem_constraints = new ArrayList<Constraint>();
		conflict_constraints = new ArrayList<Constraint>();

		node_variables = new ArrayList<EdgeVar>();
		node_activities = new ArrayList<Variable>();
		node_services = new ArrayList<Variable>();
		node_receivers = new ArrayList<Variable>();

		edge_variables = new ArrayList<EdgeVar>();
		list_variables = new ArrayList<Variable>();

		blocked_variables = new ArrayList<Variable>();

		intent_filters = new HashMap<>();
		number_intent_filters = new HashMap<>();

		danger_apis = new HashMap<>();
		number_danger_apis = new HashMap<>();

		tainted_flow = new HashMap<>();
		number_tainted_flow = new HashMap<>();

		map_activities = new HashMap<>();
		map_services = new HashMap<>();
		map_receivers = new HashMap<>();
		index_var = 0;

		objective_functions = new ArrayList<>();

		sample = new Sample();
		
		frequency = map;

	}

	/**
	 * Public methods
	 */

	public void build() {
		createEncoding();
		createObjectiveFunctions();
	}

	public enum Type {
		ACTIVITIES, SERVICES, RECEIVERS, UNDEFINED;
	}

	public Type getType(String name, Sample s) {
		if (s.getActivities().contains(name))
			return Type.ACTIVITIES;
		else if (s.getServices().contains(name))
			return Type.SERVICES;
		else if (s.getReceivers().contains(name))
			return Type.RECEIVERS;

		return Type.UNDEFINED;
	}

	public Set<String> getIntersection(Set<String> a, Set<String> b) {

		Set<String> intersection = new HashSet<String>(a);
		intersection.retainAll(b);

		return intersection;
	}

	public void createEncoding() {

		// Get the information for intent filters
		// Assumes that the names for activities, services and receivers are
		// unique
		int pos = 0;
		for (Sample s : samples) {
			for (Pair<String, String> pair : s.getIntentFilters()) {
				Pair<Integer, String> p = new Pair<Integer, String>(pos, pair.val0);
				if (intent_filters.containsKey(p)) {
					intent_filters.get(p).add(pair.val1);
				} else {
					Set<String> list = new HashSet<String>();
					list.add(pair.val1);
					intent_filters.put(p, list);
				}
			}
			pos++;
		}

		pos = 0;
		for (Sample s : samples) {
			for (Pair<String, String> pair : s.getDangerAPIs()) {
				Pair<Integer, String> p = new Pair<Integer, String>(pos, pair.val0);
				if (danger_apis.containsKey(p)) {
					danger_apis.get(p).add(pair.val1);
				} else {
					Set<String> list = new HashSet<String>();
					list.add(pair.val1);
					danger_apis.put(p, list);
				}
			}
			pos++;
		}

		pos = 0;
		for (Sample s : samples) {
			for (Quad<String, String, String, String> quad : s.getTaintFlows()) {

				if (!quad.val0.equals(quad.val2))
					continue;

				Pair<Integer, String> p = new Pair<Integer, String>(pos, quad.val0);
				String smash = quad.val1;
				smash = smash.concat(quad.val3);
				if (tainted_flow.containsKey(p)) {
					tainted_flow.get(p).add(smash);
				} else {
					Set<String> list = new HashSet<String>();
					list.add(smash);
					tainted_flow.put(p, list);
				}

				//
				//
				// Pair<Integer, String> p = new Pair<Integer, String>(pos,
				// quad.val0);
				// if (tainted_flow.containsKey(p)) {
				// tainted_flow.get(p).add(quad.val1);
				// } else {
				// Set<String> list = new HashSet<String>();
				// list.add(quad.val1);
				// tainted_flow.put(p, list);
				// }
				//
				// Pair<Integer, String> p2 = new Pair<Integer, String>(pos,
				// quad.val2);
				// if (tainted_flow.containsKey(p2)) {
				// tainted_flow.get(p2).add(quad.val3);
				// } else {
				// Set<String> list = new HashSet<String>();
				// list.add(quad.val3);
				// tainted_flow.put(p2, list);
				// }
			}
			pos++;
		}

		// create variables for node matching
		System.out.println("Reference graph: " + id_reference_sample);

		List<List<Variable>> empty_mappings = new ArrayList<List<Variable>>();
		for (int i = 0; i < samples.get(id_reference_sample).getIccg().size(); i++)
			empty_mappings.add(new ArrayList<Variable>());

		List<List<Variable>> empty_mappings_activities = new ArrayList<List<Variable>>();
		for (int i = 0; i < samples.get(id_reference_sample).getActivities().size(); i++)
			empty_mappings_activities.add(new ArrayList<Variable>());

		List<List<Variable>> empty_mappings_services = new ArrayList<List<Variable>>();
		for (int i = 0; i < samples.get(id_reference_sample).getServices().size(); i++)
			empty_mappings_services.add(new ArrayList<Variable>());

		List<List<Variable>> empty_mappings_receivers = new ArrayList<List<Variable>>();
		for (int i = 0; i < samples.get(id_reference_sample).getReceivers().size(); i++)
			empty_mappings_receivers.add(new ArrayList<Variable>());

		Map<Pair<Integer, String>, List<Variable>> amo_from_sample_activities = new HashMap<>();
		Map<Pair<Integer, String>, List<Variable>> amo_from_sample_services = new HashMap<>();
		Map<Pair<Integer, String>, List<Variable>> amo_from_sample_receivers = new HashMap<>();

		for (int i = 0; i < samples.size(); i++) {

			if (i == id_reference_sample)
				continue;

			pos = 0;
			// match node activities
			for (String src : samples.get(id_reference_sample).getActivities()) {
				List<Variable> constraint = new ArrayList<>();
				for (String tgt : samples.get(i).getActivities()) {
					EdgeVar edgeVar = new EdgeVar(index_var++, i, src, tgt);
					map_activities.put(new Pair<String, String>(src, tgt), edgeVar);
					node_variables.add(edgeVar);
					node_activities.add(edgeVar);
					list_variables.add(edgeVar);
					constraint.add(edgeVar);

					Pair<Integer, String> p = new Pair<Integer, String>(i, tgt);
					if (amo_from_sample_activities.containsKey(p)) {
						amo_from_sample_activities.get(p).add(edgeVar);
					} else {
						amo_from_sample_activities.put(p, new ArrayList<Variable>());
						amo_from_sample_activities.get(p).add(edgeVar);
					}

					// intent filter information
					Pair<Integer, String> p1 = new Pair<Integer, String>(i, tgt);
					Pair<Integer, String> p2 = new Pair<Integer, String>(id_reference_sample, src);

					if (!intent_filters.containsKey(p1) || !intent_filters.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_intent_filters.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(intent_filters.get(p1), intent_filters.get(p2));
						number_intent_filters.put(edgeVar, intersect);
					}

					if (!danger_apis.containsKey(p1) || !danger_apis.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_danger_apis.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(danger_apis.get(p1), danger_apis.get(p2));
						number_danger_apis.put(edgeVar, intersect);
					}

					if (!tainted_flow.containsKey(p1) || !tainted_flow.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_tainted_flow.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(tainted_flow.get(p1), tainted_flow.get(p2));
						number_tainted_flow.put(edgeVar, intersect);
					}
				}
				// empty matching
				EdgeVar edgeVar = new EdgeVar(index_var++, id_reference_sample, src, "Empty");
				node_variables.add(edgeVar);
				list_variables.add(edgeVar);
				constraint.add(edgeVar);

				// add all empty mappings for a given signature edge
				empty_mappings_activities.get(pos).add(edgeVar);

				// one-to-one mapping between nodes
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1));

				pos++;
			}

			pos = 0;
			// match node services
			for (String src : samples.get(id_reference_sample).getServices()) {
				List<Variable> constraint = new ArrayList<>();
				for (String tgt : samples.get(i).getServices()) {
					EdgeVar edgeVar = new EdgeVar(index_var++, i, src, tgt);
					map_services.put(new Pair<String, String>(src, tgt), edgeVar);
					node_variables.add(edgeVar);
					node_services.add(edgeVar);
					list_variables.add(edgeVar);
					constraint.add(edgeVar);

					Pair<Integer, String> p = new Pair<Integer, String>(i, tgt);
					if (amo_from_sample_services.containsKey(p)) {
						amo_from_sample_services.get(p).add(edgeVar);
					} else {
						amo_from_sample_services.put(p, new ArrayList<Variable>());
						amo_from_sample_services.get(p).add(edgeVar);
					}

					// intent filter information
					Pair<Integer, String> p1 = new Pair<Integer, String>(i, tgt);
					Pair<Integer, String> p2 = new Pair<Integer, String>(id_reference_sample, src);

					if (!intent_filters.containsKey(p1) || !intent_filters.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_intent_filters.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(intent_filters.get(p1), intent_filters.get(p2));
						number_intent_filters.put(edgeVar, intersect);
					}

					if (!danger_apis.containsKey(p1) || !danger_apis.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_danger_apis.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(danger_apis.get(p1), danger_apis.get(p2));
						number_danger_apis.put(edgeVar, intersect);
					}

					if (!tainted_flow.containsKey(p1) || !tainted_flow.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_tainted_flow.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(tainted_flow.get(p1), tainted_flow.get(p2));
						number_tainted_flow.put(edgeVar, intersect);
					}
				}
				// empty matching
				EdgeVar edgeVar = new EdgeVar(index_var++, id_reference_sample, src, "Empty");
				node_variables.add(edgeVar);
				list_variables.add(edgeVar);
				constraint.add(edgeVar);

				// add all empty mappings for a given signature edge
				empty_mappings_services.get(pos).add(edgeVar);

				// one-to-one mapping between nodes
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1));
				pos++;
			}

			pos = 0;
			// match node receivers
			for (String src : samples.get(id_reference_sample).getReceivers()) {
				List<Variable> constraint = new ArrayList<>();
				for (String tgt : samples.get(i).getReceivers()) {
					EdgeVar edgeVar = new EdgeVar(index_var++, i, src, tgt);
					map_receivers.put(new Pair<String, String>(src, tgt), edgeVar);
					node_variables.add(edgeVar);
					node_receivers.add(edgeVar);
					list_variables.add(edgeVar);
					constraint.add(edgeVar);

					Pair<Integer, String> p = new Pair<Integer, String>(i, tgt);
					if (amo_from_sample_receivers.containsKey(p)) {
						amo_from_sample_receivers.get(p).add(edgeVar);
					} else {
						amo_from_sample_receivers.put(p, new ArrayList<Variable>());
						amo_from_sample_receivers.get(p).add(edgeVar);
					}

					// intent filter information
					Pair<Integer, String> p1 = new Pair<Integer, String>(i, tgt);
					Pair<Integer, String> p2 = new Pair<Integer, String>(id_reference_sample, src);

					if (!intent_filters.containsKey(p1) || !intent_filters.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_intent_filters.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(intent_filters.get(p1), intent_filters.get(p2));
						number_intent_filters.put(edgeVar, intersect);
					}

					if (!danger_apis.containsKey(p1) || !danger_apis.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_danger_apis.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(danger_apis.get(p1), danger_apis.get(p2));
						number_danger_apis.put(edgeVar, intersect);
					}

					if (!tainted_flow.containsKey(p1) || !tainted_flow.containsKey(p2)) {
						Set<String> intersect = new HashSet<String>();
						number_tainted_flow.put(edgeVar, intersect);
					} else {
						Set<String> intersect = getIntersection(tainted_flow.get(p1), tainted_flow.get(p2));
						number_tainted_flow.put(edgeVar, intersect);
					}
				}
				// empty matching
				EdgeVar edgeVar = new EdgeVar(index_var++, id_reference_sample, src, "Empty");
				node_variables.add(edgeVar);
				list_variables.add(edgeVar);
				constraint.add(edgeVar);

				// add all empty mappings for a given signature edge
				empty_mappings_receivers.get(pos).add(edgeVar);

				// one-to-one mapping between nodes
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1));
				pos++;
			}

			pos = 0;
			// match edges
			for (Pair<String, String> src : samples.get(id_reference_sample).getIccg()) {

				List<Variable> constraint = new ArrayList<>();
				assert (i != id_reference_sample);

				for (Pair<String, String> tgt : samples.get(i).getIccg()) {

					// check if the start and end have the same type
					Type val0a = getType(src.val0, samples.get(id_reference_sample));
					Type val0b = getType(tgt.val0, samples.get(i));
					Type val1a = getType(src.val1, samples.get(id_reference_sample));
					Type val1b = getType(tgt.val1, samples.get(i));

					if (val0a == val0b && val1a == val1b && val0a != Type.UNDEFINED && val1a != Type.UNDEFINED) {
						EdgeVar edgeVar = new EdgeVar(index_var++, id_reference_sample, src.val0, src.val1);
						edge_variables.add(edgeVar);
						list_variables.add(edgeVar);
						constraint.add(edgeVar);

						// If an edge its mapped so it is its nodes
						Variable v1 = null;
						Variable v2 = null;

						if (getType(src.val0, samples.get(id_reference_sample)) == Type.ACTIVITIES) {
							Pair<String, String> p = new Pair<String, String>(src.val0, tgt.val0);
							assert (map_activities.containsKey(p));
							v1 = map_activities.get(p);
						} else if (getType(src.val0, samples.get(id_reference_sample)) == Type.RECEIVERS) {
							Pair<String, String> p = new Pair<String, String>(src.val0, tgt.val0);
							assert (map_receivers.containsKey(p));
							v1 = map_receivers.get(p);
						} else if (getType(src.val0, samples.get(id_reference_sample)) == Type.SERVICES) {
							Pair<String, String> p = new Pair<String, String>(src.val0, tgt.val0);
							assert (map_services.containsKey(p));
							v1 = map_services.get(p);
						}

						assert (v1 != null);

						if (getType(src.val1, samples.get(id_reference_sample)) == Type.ACTIVITIES) {
							Pair<String, String> p = new Pair<String, String>(src.val1, tgt.val1);
							assert (map_activities.containsKey(p));
							v2 = map_activities.get(p);
						} else if (getType(src.val1, samples.get(id_reference_sample)) == Type.RECEIVERS) {
							Pair<String, String> p = new Pair<String, String>(src.val1, tgt.val1);
							assert (map_receivers.containsKey(p));
							v2 = map_receivers.get(p);
						} else if (getType(src.val1, samples.get(id_reference_sample)) == Type.SERVICES) {
							Pair<String, String> p = new Pair<String, String>(src.val1, tgt.val1);
							assert (map_services.containsKey(p));
							v2 = map_services.get(p);
						}

						assert (v2 != null);

						Constraint edge_v1 = new Constraint();
						edge_v1.addLiteral(edgeVar, -1);
						edge_v1.addLiteral(v1, 1);
						edge_v1.setType(Constraint.ConstraintType.GEQ);
						edge_v1.setRhs(0);

						Constraint edge_v2 = new Constraint();
						edge_v2.addLiteral(edgeVar, -1);
						edge_v2.addLiteral(v2, 1);
						edge_v2.setType(Constraint.ConstraintType.GEQ);
						edge_v2.setRhs(0);

						problem_constraints.add(edge_v1);
						problem_constraints.add(edge_v2);

					}
				}
				// empty matching
				EdgeVar edgeVar = new EdgeVar(index_var++, id_reference_sample, src.val0, "Empty");
				edge_variables.add(edgeVar);
				list_variables.add(edgeVar);
				constraint.add(edgeVar);

				// add all empty mappings for a given signature edge
				empty_mappings.get(pos).add(edgeVar);

				// one-to-one mapping between edges
				problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1));

				pos++;
			}

		}

		// if a node in the signature has an empty mapping to a graph G, then
		// it has to have an empty mapping to all graphs
		for (List<Variable> empty_constraint : empty_mappings_receivers) {
			for (int i = 0; i < empty_constraint.size(); i++) {
				for (int j = i + 1; j < empty_constraint.size(); j++) {
					Constraint empty_v1 = new Constraint();
					empty_v1.addLiteral(empty_constraint.get(i), -1);
					empty_v1.addLiteral(empty_constraint.get(j), 1);
					empty_v1.setType(Constraint.ConstraintType.GEQ);
					empty_v1.setRhs(0);
					// empty_v1.print();
					problem_constraints.add(empty_v1);

					Constraint empty_v2 = new Constraint();
					empty_v2.addLiteral(empty_constraint.get(i), 1);
					empty_v2.addLiteral(empty_constraint.get(j), -1);
					empty_v2.setType(Constraint.ConstraintType.GEQ);
					empty_v2.setRhs(0);
					// empty_v2.print();
					problem_constraints.add(empty_v2);
				}
			}
		}

		// if a node in the signature has an empty mapping to a graph G, then
		// it has to have an empty mapping to all graphs
		for (List<Variable> empty_constraint : empty_mappings_services) {
			for (int i = 0; i < empty_constraint.size(); i++) {
				for (int j = i + 1; j < empty_constraint.size(); j++) {
					Constraint empty_v1 = new Constraint();
					empty_v1.addLiteral(empty_constraint.get(i), -1);
					empty_v1.addLiteral(empty_constraint.get(j), 1);
					empty_v1.setType(Constraint.ConstraintType.GEQ);
					empty_v1.setRhs(0);
					// empty_v1.print();
					problem_constraints.add(empty_v1);

					Constraint empty_v2 = new Constraint();
					empty_v2.addLiteral(empty_constraint.get(i), 1);
					empty_v2.addLiteral(empty_constraint.get(j), -1);
					empty_v2.setType(Constraint.ConstraintType.GEQ);
					empty_v2.setRhs(0);
					// empty_v2.print();
					problem_constraints.add(empty_v2);
				}
			}
		}

		// if a node in the signature has an empty mapping to a graph G, then
		// it has to have an empty mapping to all graphs
		for (List<Variable> empty_constraint : empty_mappings_activities) {
			for (int i = 0; i < empty_constraint.size(); i++) {
				for (int j = i + 1; j < empty_constraint.size(); j++) {
					Constraint empty_v1 = new Constraint();
					empty_v1.addLiteral(empty_constraint.get(i), -1);
					empty_v1.addLiteral(empty_constraint.get(j), 1);
					empty_v1.setType(Constraint.ConstraintType.GEQ);
					empty_v1.setRhs(0);
					// empty_v1.print();
					problem_constraints.add(empty_v1);

					Constraint empty_v2 = new Constraint();
					empty_v2.addLiteral(empty_constraint.get(i), 1);
					empty_v2.addLiteral(empty_constraint.get(j), -1);
					empty_v2.setType(Constraint.ConstraintType.GEQ);
					empty_v2.setRhs(0);
					// empty_v2.print();
					problem_constraints.add(empty_v2);
				}
			}
		}

		// if an edge in the signature has an empty mapping to a graph G, then
		// it has to have an empty mapping to all graphs
		for (List<Variable> empty_constraint : empty_mappings) {
			for (int i = 0; i < empty_constraint.size(); i++) {
				for (int j = i + 1; j < empty_constraint.size(); j++) {
					Constraint empty_v1 = new Constraint();
					empty_v1.addLiteral(empty_constraint.get(i), -1);
					empty_v1.addLiteral(empty_constraint.get(j), 1);
					empty_v1.setType(Constraint.ConstraintType.GEQ);
					empty_v1.setRhs(0);
					// empty_v1.print();
					problem_constraints.add(empty_v1);

					Constraint empty_v2 = new Constraint();
					empty_v2.addLiteral(empty_constraint.get(i), 1);
					empty_v2.addLiteral(empty_constraint.get(j), -1);
					empty_v2.setType(Constraint.ConstraintType.GEQ);
					empty_v2.setRhs(0);
					// empty_v2.print();
					problem_constraints.add(empty_v2);
				}
			}
		}

		for (List<Variable> lv : amo_from_sample_activities.values()) {
			if (lv.size() <= 1)
				continue;
			Constraint c = new Constraint(lv, Constraint.ConstraintType.LEQ, 1);
			// c.print();
			problem_constraints.add(c);
		}

		for (List<Variable> lv : amo_from_sample_receivers.values()) {
			if (lv.size() <= 1)
				continue;
			Constraint c = new Constraint(lv, Constraint.ConstraintType.LEQ, 1);
			// c.print();
			problem_constraints.add(c);
		}

		for (List<Variable> lv : amo_from_sample_services.values()) {
			if (lv.size() <= 1)
				continue;
			Constraint c = new Constraint(lv, Constraint.ConstraintType.LEQ, 1);
			// c.print();
			problem_constraints.add(c);
		}

		System.out.println("total variables: " + this.list_variables.size());
		System.out.println("total constraints: " + this.problem_constraints.size());
		// print();
	}

	public void createObjectiveFunctions() {
		
		//System.out.println("frequency: " + frequency.size());
		
		// Goal: maximize intent filters
		List<Variable> maxIntentLits = new ArrayList<>();
		List<Integer> maxIntentCoeffs = new ArrayList<>();

		for (Variable ev : node_variables) {
			if (number_intent_filters.containsKey(ev)) {
				Set<String> s = number_intent_filters.get(ev);
				// FIXME: instead of duplicating the variables we could change
				// the weight
				for (String t : s){
					//System.out.println("filters: " + t);
					int weight = 10000;
					if (frequency.containsKey(t)){
						//System.out.println("frequency: " + frequency.get(t));
						//weight = Math.floorDiv(10000, frequency.get(t));
						weight = (int) Math.floor((double)10000/frequency.get(t));
					}
					maxIntentLits.add(ev);
					maxIntentCoeffs.add(weight);
					//System.out.println("weight: " + weight);
				}
			}
		}

		Constraint objectiveA = new Constraint(maxIntentLits, maxIntentCoeffs, ConstraintType.GEQ, 0);
		objective_functions.add(objectiveA);

		// Goal: maximize danger apis
		List<Variable> maxDangerLits = new ArrayList<>();
		List<Integer> maxDangerCoeffs = new ArrayList<>();

		for (Variable ev : node_variables) {
			if (number_danger_apis.containsKey(ev)) {
				Set<String> s = number_danger_apis.get(ev);
				// FIXME: instead of duplicating the variables we could change
				// the weight
				for (String t : s){
					//System.out.println("apis: " + t);
					int weight = 10000;
					if (frequency.containsKey(t)){
						//System.out.println("frequency: " + frequency.get(t));
						//weight = Math.floorDiv(10000, frequency.get(t));
						weight = (int) Math.floor((double)10000/frequency.get(t));
					}
					
					maxDangerLits.add(ev);
					maxDangerCoeffs.add(weight);
					//System.out.println("weight: " + weight);
				}
			}
		}

		Constraint objectiveB = new Constraint(maxDangerLits, maxDangerCoeffs, ConstraintType.GEQ, 0);
		objective_functions.add(objectiveB);

		// Goal: maximize tainted flow
		List<Variable> maxTaintLits = new ArrayList<>();
		List<Integer> maxTaintCoeffs = new ArrayList<>();

		for (Variable ev : node_variables) {
			if (number_tainted_flow.containsKey(ev)) {
				Set<String> s = number_tainted_flow.get(ev);
				// FIXME: instead of duplicating the variables we could change
				// the weight
				for (String t : s){
					//System.out.println("taints: " + t);
					String[] parts = t.split("!");
					String match = "<"+parts[0]+", !"+parts[1]+">";
					//System.out.println("match: " + match);
					int weight = 10000;
					if (frequency.containsKey(match)){
						//System.out.println("frequency: " + frequency.get(match));
						//weight = Math.floorDiv(10000, frequency.get(match));
						weight = (int) Math.floor((double)10000/frequency.get(match));
					}
					maxTaintLits.add(ev);
					maxTaintCoeffs.add(weight);
					//System.out.println("weight: " + weight);
				}
			}
		}

		Constraint objectiveC = new Constraint(maxTaintLits, maxTaintCoeffs, ConstraintType.GEQ, 0);
		objective_functions.add(objectiveC);
		
		
//		List<Variable> frequencyLits = new ArrayList<>();
//		List<Integer> frequencyCoeffs = new ArrayList<>();
//		
//		for (Variable v : maxIntentLits)
//			frequencyLits.add(v);
//		
//		for (Variable v : maxDangerLits)
//			frequencyLits.add(v);
//		
//		for (Variable v : maxTaintLits)
//			frequencyLits.add(v);
//		
//		for (Integer r : maxIntentCoeffs)
//			frequencyCoeffs.add(r);
//		
//		for (Integer r : maxDangerCoeffs)
//			frequencyCoeffs.add(r);
//		
//		for (Integer r : maxTaintCoeffs)
//			frequencyCoeffs.add(r);
//		
//		Constraint objectiveFrequency = new Constraint(frequencyLits, frequencyCoeffs,ConstraintType.GEQ, 0);
//		objective_functions.add(objectiveFrequency);

		
		// Goal: maximize the number of edges
		List<Variable> maxEdgesLits = new ArrayList<>();
		List<Integer> maxEdgesCoeffs = new ArrayList<>();

		for (EdgeVar ev : edge_variables) {
			// FIXME: it may be better to minimize the number of empty mappings
			if (ev.getTgt().contains("Empty")) {
				continue;
			}

			maxEdgesLits.add(ev);
			maxEdgesCoeffs.add(1);
		}

		Constraint objectiveD = new Constraint(maxEdgesLits, maxEdgesCoeffs, ConstraintType.GEQ, 0);
		objective_functions.add(objectiveD);
		
	}

	public Integer nVars() {
		return list_variables.size();
	}

	public List<Variable> getVariables() {
		return list_variables;
	}

	public Integer nConstraints() {
		return problem_constraints.size();
	}

	public List<Constraint> getConstraints() {
		return problem_constraints;
	}

	public void print() {

		System.out.println("#Variables: " + nVars());
		System.out.println("#Constraints: " + nConstraints());

		for (Constraint c : problem_constraints) {
			c.print();
		}
	}
	
	public Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> getSuspicious(
			Set<Pair<String, String>> edges, ArrayList<Boolean> model, Map<String, String> map_name,
			HashMap<String, List<EdgeVar>> nodes_signature_mapping, Map<String,String> reverse) {
		
		List<Pair<String,String>> intentFilters = new ArrayList<>();
		List<Pair<String,String>> dangerAPIs = new ArrayList<>();
		List<Quad<String,String,String,String>> taintFlows = new ArrayList<>();
	
		// number of nodes
		Set<String> nodes = new HashSet<>();
		for (Pair<String,String> e : edges){
			nodes.add(e.val0);
			nodes.add(e.val1);
		}
		
		// number of flows
//		for (EdgeVar v : flow_variables){
//			if (!model.get(v.getSolverId()))
//				continue;
//			
//			
//			if (nodes.contains(map_name.get(v.getSrc())) && nodes.contains(map_name.get(v.getTgt()))){
//				Pair<String,String> p = map_inter_tainted_flow.get(v);
//				Quad<String,String,String,String> q = new Quad<String,String,String,String>(map_name.get(v.getSrc()),p.val0,map_name.get(v.getTgt()),p.val1);
//				
//				if (!taintFlows.contains(q))
//					taintFlows.add(q);
//			}
//		}
		
		// number of danger apis and number of intents
		HashMap<String, Set<String>> intent_output = new HashMap<>();
		HashMap<String, Set<String>> danger_output = new HashMap<>();
		HashMap<String, Set<String>> taint_output = new HashMap<>();

		
		for (String nn : nodes){
			if (!reverse.containsKey(nn))
				continue;
			String node_name = reverse.get(nn);
			assert (nodes_signature_mapping.containsKey(node_name));
			assert(nodes_signature_mapping.get(node_name).size() == samples.size()-1);
			
			boolean init = false;
			Set<String> ifilters = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)){
				
				if (!nodes.contains(map_name.get(node_name)))
					continue;
				
				if(number_intent_filters.containsKey(v)){
					if (!init){
						ifilters.addAll(number_intent_filters.get(v));
						init = true;
					} else {
						ifilters = getIntersection(ifilters, number_intent_filters.get(v));
					}
				}
			}
			
			init = false;
			Set<String> idanger = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)){
				
				if (!nodes.contains(map_name.get(node_name)))
					continue;
				
				if(number_danger_apis.containsKey(v)){
					if (!init){
						idanger.addAll(number_danger_apis.get(v));
						init = true;
					} else {
						idanger = getIntersection(idanger, number_danger_apis.get(v));
					}
				}
			}
			
			init = false;
			Set<String> itaint = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)){
				
				if (!nodes.contains(map_name.get(node_name)))
					continue;
				
				if(number_tainted_flow.containsKey(v)){
					if (!init){
						itaint.addAll(number_tainted_flow.get(v));
						init = true;
					} else {
						itaint = getIntersection(itaint, number_tainted_flow.get(v));
					}
				}
			}
			
			
			for (String inner : ifilters){
				if (!map_name.containsKey(node_name))
					continue;
				
				if (!intent_output.containsKey(map_name.get(node_name))){
					intent_output.put(map_name.get(node_name), new HashSet<String>());
					intent_output.get(map_name.get(node_name)).add(inner);
					//System.out.println(map_name.get(node_name) + "[intentFilter]" + inner);
					intentFilters.add(new Pair<String,String>(map_name.get(node_name), inner));
				} else {
					if (!intent_output.get(map_name.get(node_name)).contains(inner)){
						intent_output.get(map_name.get(node_name)).add(inner);
						//System.out.println(map_name.get(node_name) + "[intentFilter]" + inner);
						intentFilters.add(new Pair<String,String>(map_name.get(node_name), inner));
					}
				}
				
			}
			
			for (String inner : idanger){
				if (!map_name.containsKey(node_name))
					continue;
				
				if (!danger_output.containsKey(map_name.get(node_name))){
					danger_output.put(map_name.get(node_name), new HashSet<String>());
					danger_output.get(map_name.get(node_name)).add(inner);
					//System.out.println(map_name.get(node_name) + "[dangerAPIs]" + inner);
					dangerAPIs.add(new Pair<String,String>(map_name.get(node_name), inner));
				} else {
					if (!danger_output.get(map_name.get(node_name)).contains(inner)){
						danger_output.get(map_name.get(node_name)).add(inner);
						//System.out.println(map_name.get(node_name) + "[dangerAPIs]" + inner);
						dangerAPIs.add(new Pair<String,String>(map_name.get(node_name), inner));
					}
				}
				
			}
			
			//System.out.println("itaint: " + itaint.size() );
			for (String inner : itaint){
				if (!map_name.containsKey(node_name))
					continue;
				
				if (!taint_output.containsKey(map_name.get(node_name))){
					taint_output.put(map_name.get(node_name), new HashSet<String>());
					taint_output.get(map_name.get(node_name)).add(inner);
					//System.out.println(map_name.get(node_name) + "[taintFlow]" + inner);
					int index = inner.indexOf("!");
					String s2 = inner.substring(index);
					String s1 = inner.substring(0, index);
					//System.out.println(map_name.get(node_name) + "[src]" + s1 + "[taintFlow]" +map_name.get(node_name) + "[sink]" + s2);
					taintFlows.add(new Quad<String,String,String,String>(map_name.get(node_name),s1,map_name.get(node_name),s2));
				} else {
					if (!taint_output.get(map_name.get(node_name)).contains(inner)){
						taint_output.get(map_name.get(node_name)).add(inner);
						//System.out.println(map_name.get(node_name) + "[taintFlow]" + inner);
						int index = inner.indexOf("!");
						String s2 = inner.substring(index);
						String s1 = inner.substring(0, index);
						//System.out.println(map_name.get(node_name) + "[src]" + s1 + "[taintFlow]" +map_name.get(node_name) + "[sink]" + s2);
						taintFlows.add(new Quad<String,String,String,String>(map_name.get(node_name),s1,map_name.get(node_name),s2));
					}
				}
				
				//System.out.println(map_name.get(p.val0) + "[src]" + p.val1 + "[taintFlow]" + map_name.get(p2.val0) + "[sink]" + p2.val1);
				//taintFlows.add(new Quad<String,String,String,String>(map_name.get(p.val0),p.val1,map_name.get(p2.val0),p2.val1));
				
			}
			
		}
		
		//System.out.println("flows:" + taintFlows.size());
		
		Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> suspicious = 
				new Quad<List<Pair<String,String>>, List<Pair<String,String>>, List<Quad<String,String,String,String>>, Set<String>>(intentFilters, dangerAPIs, taintFlows, nodes);
		
		return suspicious;
		
	}
	
	protected boolean isLarger(Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> q1, Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> q2){
		
		if (q1.val0.size() > q2.val0.size())
			return true;
		else if (q1.val0.size() == q2.val0.size()) {

			if (q1.val1.size() > q2.val1.size())
				return true;
			else if (q1.val1.size() == q2.val1.size()) {

				if (q1.val2.size() > q2.val2.size())
					return true;
				else if (q1.val2.size() == q2.val2.size()) {

					if (q1.val3.size() > q2.val3.size())
						return true;
				}
			}
		}		
	
		return false;
	}

	public List<String> saveModel(Solver solver, String solution) {

		ArrayList<Boolean> model = new ArrayList<>();
		for (int i = 0; i < nVars(); i++)
			model.add(false);
		if (solution.length() > 0) {
			// We are using the solution from open-wbo
			// Parse the solution
			StringTokenizer st = new StringTokenizer(solution);
			while (st.hasMoreTokens()) {
				String s = st.nextToken();
				if (s.startsWith("x")) {
					model.set(Integer.parseInt(s.substring(1)) - 1, true);
				} else if (s.startsWith("-x")) {
					model.set(Integer.parseInt(s.substring(2)) - 1, false);
				}
			}
		} else {
			for (int i = 0; i < nVars(); i++)
				model.set(i, solver.getModel().get(i));
		}

		Map<Pair<String, String>, Variable> map_edges = new HashMap<>();
		Map<String, String> map_name = new HashMap<>();
		int a_counter = 0;
		int s_counter = 0;
		int r_counter = 0;

		List<String> res = new ArrayList<>();

		// save the sample
		sample = new Sample();
		List<Pair<String, String>> iccg = new ArrayList<>();
		List<String> activities = new ArrayList<>();
		List<String> services = new ArrayList<>();
		List<String> receivers = new ArrayList<>();
		
		Map<String,String> reverseMapping = new HashMap<>();

		// Find the most suspicious subgraph
		Set<Pair<String, String>> edges = new HashSet<>();
		Set<String> nodes = new HashSet<>();
		List<Set<Pair<String, String>>> subgraphs = new ArrayList<>();
		
		// 1. Recover all nodes and create a name for them
		for (EdgeVar v : node_variables) {
			if (!model.get(v.getSolverId()))
				continue;
						
			if (v.getTgt().contains("Empty"))
				continue;

			Type t_in = getType(v.getSrc(), samples.get(id_reference_sample));
			if (!map_name.containsKey(v.getSrc())) {
				if (t_in == Type.ACTIVITIES)
					map_name.put(v.getSrc(), new String("activity_" + a_counter++));
				else if (t_in == Type.RECEIVERS)
					map_name.put(v.getSrc(), new String("receiver_" + r_counter++));
				else if (t_in == Type.SERVICES)
					map_name.put(v.getSrc(), new String("service_" + s_counter++));
				
				nodes.add(map_name.get(v.getSrc()));
				reverseMapping.put(map_name.get(v.getSrc()),v.getSrc());
			}
		}
		
		// 2. Recover the connections
		for (EdgeVar v : edge_variables) {

			if (!model.get(v.getSolverId()))
				continue;

			if (v.getTgt().contains("Empty"))
				continue;

			Pair<String, String> p = new Pair<String, String>(v.getSrc(), v.getTgt());

			if (!map_edges.containsKey(p)) {
				Type t_in = getType(v.getSrc(), samples.get(id_reference_sample));
				if (!map_name.containsKey(v.getSrc())) {
					if (t_in == Type.ACTIVITIES)
						map_name.put(v.getSrc(), new String("activity_" + a_counter++));
					else if (t_in == Type.RECEIVERS)
						map_name.put(v.getSrc(), new String("receiver_" + r_counter++));
					else if (t_in == Type.SERVICES)
						map_name.put(v.getSrc(), new String("service_" + s_counter++));
				}

				Type t_out = getType(v.getTgt(), samples.get(id_reference_sample));
				if (!map_name.containsKey(v.getTgt())) {
					if (t_out == Type.ACTIVITIES)
						map_name.put(v.getTgt(), new String("activity_" + a_counter++));
					else if (t_out == Type.RECEIVERS)
						map_name.put(v.getTgt(), new String("receiver_" + r_counter++));
					else if (t_out == Type.SERVICES)
						map_name.put(v.getTgt(), new String("service_" + s_counter++));
				}

				if (t_in != Type.UNDEFINED && t_out != Type.UNDEFINED) {
					edges.add(new Pair<>(map_name.get(v.getSrc()), map_name.get(v.getTgt())));
					map_edges.put(p, v);
					nodes.add(map_name.get(v.getSrc()));
					nodes.add(map_name.get(v.getTgt()));
					reverseMapping.put(map_name.get(v.getSrc()),v.getSrc());
					reverseMapping.put(map_name.get(v.getTgt()),v.getTgt());
				}
			}
		}
		
		/*
		List<Pair<String, String>> intentFilters = new ArrayList<>();
		List<Pair<String, String>> dangerAPIs = new ArrayList<>();

		// Set<Pair<String,String>> flowPairs = new HashSet<>();
		List<Quad<String, String, String, String>> taintFlows = new ArrayList<>();

		// Checking if the graph is connected.
		Set<Pair<String, String>> edges = new HashSet<>();
		Set<String> nodes = new HashSet<>();

		// check if the graph only contains activities
		boolean only_activities = true;
		for (EdgeVar v : edge_variables) {

			if (!model.get(v.getSolverId()))
				continue;

			if (v.getTgt().contains("Empty"))
				continue;

			Type t_in = getType(v.getSrc(), samples.get(id_reference_sample));
			if (!map_name.containsKey(v.getSrc())) {
				if (t_in == Type.ACTIVITIES)
					map_name.put(v.getSrc(), new String("activity_" + a_counter++));
				else if (t_in == Type.RECEIVERS)
					map_name.put(v.getSrc(), new String("receiver_" + r_counter++));
				else if (t_in == Type.SERVICES)
					map_name.put(v.getSrc(), new String("service_" + s_counter++));
			}

			if (t_in == Type.RECEIVERS || t_in == Type.SERVICES)
				only_activities = false;
		}
		System.out.println("only_activities: " + only_activities);

		if (!only_activities) {
			for (EdgeVar v : edge_variables) {
				// if (!solver.getModel().get(v.getSolverId()))
				// continue;

				if (!model.get(v.getSolverId()))
					continue;

				if (v.getTgt().contains("Empty"))
					continue;

				blocked_variables.add(v);

				Pair<String, String> p = new Pair<String, String>(v.getSrc(), v.getTgt());

				if (!map_edges.containsKey(p)) {
					map_edges.put(p, v);

					Type t_in = getType(v.getSrc(), samples.get(id_reference_sample));
					if (!map_name.containsKey(v.getSrc())) {
						if (t_in == Type.ACTIVITIES)
							map_name.put(v.getSrc(), new String("activity_" + a_counter++));
						else if (t_in == Type.RECEIVERS)
							map_name.put(v.getSrc(), new String("receiver_" + r_counter++));
						else if (t_in == Type.SERVICES)
							map_name.put(v.getSrc(), new String("service_" + s_counter++));
					}

					Type t_out = getType(v.getTgt(), samples.get(id_reference_sample));
					if (!map_name.containsKey(v.getTgt())) {
						if (t_out == Type.ACTIVITIES)
							map_name.put(v.getTgt(), new String("activity_" + a_counter++));
						else if (t_out == Type.RECEIVERS)
							map_name.put(v.getTgt(), new String("receiver_" + r_counter++));
						else if (t_out == Type.SERVICES)
							map_name.put(v.getTgt(), new String("service_" + s_counter++));
					}

					if (t_in != Type.UNDEFINED && t_out != Type.UNDEFINED) {
						// res.add(map_name.get(v.getSrc()) + "[edgeTo]" +
						// map_name.get(v.getTgt()));
						// iccg.add(new Pair<>(map_name.get(v.getSrc()),
						// map_name.get(v.getTgt())));
						edges.add(new Pair<>(map_name.get(v.getSrc()), map_name.get(v.getTgt())));
						nodes.add(map_name.get(v.getSrc()));
						nodes.add(map_name.get(v.getTgt()));
						// System.out.println(v.getSrc() + "->" + v.getTgt());
					}

				}
			}

		} else {
			for (EdgeVar v : node_variables) {
				if (!model.get(v.getSolverId()))
					continue;

				if (v.getTgt().contains("Empty"))
					continue;

				Type t_in = getType(v.getSrc(), samples.get(id_reference_sample));
				if (!map_name.containsKey(v.getSrc())) {
					if (t_in == Type.ACTIVITIES)
						map_name.put(v.getSrc(), new String("activity_" + a_counter++));
					else if (t_in == Type.RECEIVERS)
						map_name.put(v.getSrc(), new String("receiver_" + r_counter++));
					else if (t_in == Type.SERVICES)
						map_name.put(v.getSrc(), new String("service_" + s_counter++));
				}

				if (t_in == Type.RECEIVERS) {
					nodes.add(map_name.get(v.getSrc()));
				}

				// System.out.println("type: " + t_in + " node: " + v);

			}
		}

		// FIXME: check if there are not connected nodes
		is_connected = Util.isConnected(edges, nodes);
		if (!is_connected) {
			// Remove activities and try again?
			// We should fix this loop asap
			List<String> removeAct = new ArrayList<>();
			for (String s : nodes) {

				if (s.startsWith("activity_"))
					removeAct.add(s);
			}
			nodes.removeAll(removeAct);

			List<Pair<String, String>> removeEdg = new ArrayList<>();
			for (Pair<String, String> s : edges) {

				if (s.val0.startsWith("activity_") || s.val1.startsWith("activity_"))
					removeEdg.add(s);
			}
			edges.removeAll(removeEdg);
			is_connected = Util.isConnected(edges, nodes);
		}
		if (!only_activities)
			System.out.println("is connected: " + is_connected);
		else
			System.out.println("is connected: " + (nodes.size() == 1));

		HashMap<String, Set<String>> intent_output = new HashMap<>();
		HashMap<String, Set<String>> danger_output = new HashMap<>();
		HashMap<String, Set<String>> taint_output = new HashMap<>();

		Set<String> nodes_signature = new HashSet<String>();
		HashMap<String, List<EdgeVar>> nodes_signature_mapping = new HashMap<>();

		for (EdgeVar v : node_variables) {
			if (!model.get(v.getSolverId()))
				continue;

			if (v.getTgt().contains("Empty"))
				continue;

			if (!nodes.contains(map_name.get(v.getSrc())))
				continue;

			blocked_variables.add(v);

			nodes_signature.add(v.getSrc());
			if (nodes_signature_mapping.containsKey(v.getSrc())) {
				nodes_signature_mapping.get(v.getSrc()).add(v);
			} else {
				nodes_signature_mapping.put(v.getSrc(), new ArrayList<EdgeVar>());
				nodes_signature_mapping.get(v.getSrc()).add(v);
			}
		}

		for (String node_name : nodes_signature) {
			// System.out.println("nodes: " + node_name + " | size: " +
			// nodes_signature_mapping.get(node_name).size());
			assert (nodes_signature_mapping.get(node_name).size() == samples.size() - 1);

			boolean init = false;
			Set<String> ifilters = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)) {

				if (!nodes.contains(map_name.get(node_name)))
					continue;

				if (number_intent_filters.containsKey(v)) {
					if (!init) {
						ifilters.addAll(number_intent_filters.get(v));
						init = true;
					} else {
						ifilters = getIntersection(ifilters, number_intent_filters.get(v));
					}
				}
			}

			init = false;
			Set<String> idanger = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)) {

				if (!nodes.contains(map_name.get(node_name)))
					continue;

				if (number_danger_apis.containsKey(v)) {
					if (!init) {
						idanger.addAll(number_danger_apis.get(v));
						init = true;
					} else {
						idanger = getIntersection(idanger, number_danger_apis.get(v));
					}
				}
			}

			init = false;
			Set<String> itaint = new HashSet<>();
			for (EdgeVar v : nodes_signature_mapping.get(node_name)) {

				if (!nodes.contains(map_name.get(node_name)))
					continue;

				if (number_tainted_flow.containsKey(v)) {
					if (!init) {
						itaint.addAll(number_tainted_flow.get(v));
						init = true;
					} else {
						itaint = getIntersection(itaint, number_tainted_flow.get(v));
					}
				}
			}

			for (String inner : ifilters) {
				if (!map_name.containsKey(node_name))
					continue;

				if (!intent_output.containsKey(map_name.get(node_name))) {
					intent_output.put(map_name.get(node_name), new HashSet<String>());
					intent_output.get(map_name.get(node_name)).add(inner);
					System.out.println(map_name.get(node_name) + "[intentFilter]" + inner);
					intentFilters.add(new Pair<String, String>(map_name.get(node_name), inner));
				} else {
					if (!intent_output.get(map_name.get(node_name)).contains(inner)) {
						intent_output.get(map_name.get(node_name)).add(inner);
						System.out.println(map_name.get(node_name) + "[intentFilter]" + inner);
						intentFilters.add(new Pair<String, String>(map_name.get(node_name), inner));
					}
				}

			}

			for (String inner : idanger) {
				if (!map_name.containsKey(node_name))
					continue;

				if (!danger_output.containsKey(map_name.get(node_name))) {
					danger_output.put(map_name.get(node_name), new HashSet<String>());
					danger_output.get(map_name.get(node_name)).add(inner);
					System.out.println(map_name.get(node_name) + "[dangerAPIs]" + inner);
					dangerAPIs.add(new Pair<String, String>(map_name.get(node_name), inner));
				} else {
					if (!danger_output.get(map_name.get(node_name)).contains(inner)) {
						danger_output.get(map_name.get(node_name)).add(inner);
						System.out.println(map_name.get(node_name) + "[dangerAPIs]" + inner);
						dangerAPIs.add(new Pair<String, String>(map_name.get(node_name), inner));
					}
				}

			}

			for (String inner : itaint) {
				if (!map_name.containsKey(node_name))
					continue;

				if (!taint_output.containsKey(map_name.get(node_name))) {
					taint_output.put(map_name.get(node_name), new HashSet<String>());
					taint_output.get(map_name.get(node_name)).add(inner);
					// System.out.println(map_name.get(node_name) +
					// "[taintFlow]" + inner);
					int index = inner.indexOf("!");
					String s2 = inner.substring(index);
					String s1 = inner.substring(0, index);
					System.out.println(map_name.get(node_name) + "[src]" + s1 + "[taintFlow]" + map_name.get(node_name)
							+ "[sink]" + s2);
					taintFlows.add(new Quad<String, String, String, String>(map_name.get(node_name), s1,
							map_name.get(node_name), s2));
				} else {
					if (!taint_output.get(map_name.get(node_name)).contains(inner)) {
						taint_output.get(map_name.get(node_name)).add(inner);
						// System.out.println(map_name.get(node_name) +
						// "[taintFlow]" + inner);
						int index = inner.indexOf("!");
						String s2 = inner.substring(index);
						String s1 = inner.substring(0, index);
						System.out.println(map_name.get(node_name) + "[src]" + s1 + "[taintFlow]"
								+ map_name.get(node_name) + "[sink]" + s2);
						taintFlows.add(new Quad<String, String, String, String>(map_name.get(node_name), s1,
								map_name.get(node_name), s2));
					}
				}

				// System.out.println(map_name.get(p.val0) + "[src]" + p.val1 +
				// "[taintFlow]" + map_name.get(p2.val0) + "[sink]" + p2.val1);
				// taintFlows.add(new
				// Quad<String,String,String,String>(map_name.get(p.val0),p.val1,map_name.get(p2.val0),p2.val1));

			}

		}
*/
		/*
		for (String s : map_name.values()) {
			if (s.contains("activity_")) {
				if (nodes.contains(s))
					activities.add(s);
			} else if (s.contains("receiver_")) {
				if (nodes.contains(s))
					receivers.add(s);
			} else if (s.contains("service_")) {
				if (nodes.contains(s))
					services.add(s);
			}
		}

		for (Pair<String, String> e : edges) {
			res.add(e.val0 + "[edgeTo]" + e.val1);
			iccg.add(new Pair<String, String>(e.val0, e.val1));
		}

		sample.setIccg(iccg);
		sample.setActivities(activities);
		sample.setServices(services);
		sample.setReceivers(receivers);
		sample.setIntentFilters(intentFilters);
		sample.setDangerAPIs(dangerAPIs);
		sample.setTaintFlows(taintFlows);

		return res;
		*/
		subgraphs = Util.getSubGraphs(edges, nodes);
		System.out.println("Subgraphs: " + subgraphs.size());
		
		if (subgraphs.size() > 0){
		
		Set<String> nodes_signature = new HashSet<String>();
		HashMap<String, List<EdgeVar>> nodes_signature_mapping = new HashMap<>();
		
		for (EdgeVar v : node_variables){
			if (!model.get(v.getSolverId()))
				continue;
			
			if (v.getTgt().contains("Empty"))
				continue;
			
			if (!nodes.contains(map_name.get(v.getSrc())))
				continue;
			
			nodes_signature.add(v.getSrc());
			if (nodes_signature_mapping.containsKey(v.getSrc())){
				nodes_signature_mapping.get(v.getSrc()).add(v);
			} else {
				nodes_signature_mapping.put(v.getSrc(), new ArrayList<EdgeVar>());
				nodes_signature_mapping.get(v.getSrc()).add(v);
			}
		}
		
		Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> signature_info = null;
		
		for (Set<Pair<String, String>> sp : subgraphs){
//			System.out.println("GRAPH!");
//			for (Pair<String,String> e : sp){
//				System.out.println(e.val0 + "[edge]" + e.val1);
//			}
			if (signature_info == null){
				signature_info = getSuspicious(sp, model, map_name, nodes_signature_mapping, reverseMapping);
			} else {
				Quad<List<Pair<String, String>>, List<Pair<String, String>>, List<Quad<String, String, String, String>>, Set<String>> q = getSuspicious(sp, model, map_name, nodes_signature_mapping, reverseMapping);
				if (isLarger(q, signature_info))
					signature_info = q;
				//System.out.println("q0: " + q.val0.size() + " q1: " + q.val1.size() +" q2: " + q.val2.size() +" q3: " + q.val3.size());
			}
			
		}
		
		//System.out.println("q0: " + signature_info.val0.size() + " q1: " + signature_info.val1.size() +" q2: " + signature_info.val2.size() +" q3: " + signature_info.val3.size());
		
		for (Quad<String,String,String,String> p : signature_info.val2){
			System.out.println(p.val0 + "[src]" + p.val1 + "[taintFlow]" + p.val2 + "[sink]" + p.val3);
		}
		
		for (Pair<String,String> p : signature_info.val1){
			System.out.println(p.val0 + "[dangerAPIs]" + p.val1);
		}
		
		for (Pair<String,String> p : signature_info.val0){
			System.out.println(p.val0 + "[intentFilter]" + p.val1);
		}
		
		for (String s : signature_info.val3){
			if (s.contains("activity_")){
				if (nodes.contains(s))
					activities.add(s);
			}
			else if (s.contains("receiver_")){
				if (nodes.contains(s))
					receivers.add(s);
			}
			else if (s.contains("service_")){
				if (nodes.contains(s))
					services.add(s);
			}
		}
		
		for (EdgeVar v : edge_variables) {

			if (!model.get(v.getSolverId()))
				continue;

			if (v.getTgt().contains("Empty"))
				continue;

			Pair<String, String> p = new Pair<String, String>(map_name.get(v.getSrc()), map_name.get(v.getTgt()));
			if (signature_info.val3.contains(map_name.get(v.getSrc())) && signature_info.val3.contains(map_name.get(v.getTgt()))
					&& !iccg.contains(p)) {
				res.add(p.val0 + "[edgeTo]" + p.val1);
				System.out.println(p.val0 + "[edgeTo]" + p.val1);
				iccg.add(p);
			}
		}
		
		sample.setIccg(iccg);
		sample.setActivities(activities);
		sample.setServices(services);
		sample.setReceivers(receivers);
		sample.setIntentFilters(signature_info.val0);
		sample.setDangerAPIs(signature_info.val1);
		sample.setTaintFlows(signature_info.val2);
		
		}
		
		return res;
	}

	public List<Variable> getBlockVars() {

		List<Variable> lv = new ArrayList<>();
		for (EdgeVar v : edge_variables) {
			lv.add(v);
			// System.out.println("edge_variables: " + v.getSolverId() + " src:
			// " + v.getSrc() + " tgt: " + v.getTgt());
		}

		return lv;
	}

	public void printFormula() {

		try {
			PrintWriter writer = new PrintWriter("./opb/malware.opb", "UTF-8");
			writer.println("* #variable= " + nVars() + " #constraint= " + nConstraints());

			String obj = "min: ";

			ArrayList<Integer> obj_values = new ArrayList<Integer>();
			for (int i = 0; i < getObjectiveFunctions().size(); i++)
				obj_values.add(1);

			int sum = 0;
			for (int i = getObjectiveFunctions().size() - 2; i >= 0; i--) {
				obj_values.set(i, obj_values.get(i + 1) * getObjectiveFunctions().get(i + 1).getSize() + 1 + sum);
				sum += obj_values.get(i + 1) * getObjectiveFunctions().get(i + 1).getSize();
			}

			// print objective functions
			for (int i = 0; i < getObjectiveFunctions().size(); i++) {
				int weight = obj_values.get(i);
				for (int j = 0; j < getObjectiveFunctions().get(i).getCoefficients().size(); j++) {
					obj = obj.concat(-weight + " x"
							+ (getObjectiveFunctions().get(i).getLiterals().get(j).getSolverId() + 1) + " ");
				}
			}

			obj = obj.concat(";");

			writer.println(obj);

			for (Constraint c : problem_constraints) {
				writer.println(c.printOPB());
			}

			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Sample getSignature(Solver solver) {
		return sample;
	}

	public boolean isConnected() {
		return is_connected;
	}

	public List<Variable> getBlockedGraph() {
		return blocked_variables;
	}

	public List<Constraint> getObjectiveFunctions() {
		return objective_functions;
	}

	public void printVariables() {

	}
}
