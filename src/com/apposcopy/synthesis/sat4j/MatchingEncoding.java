package com.apposcopy.synthesis.sat4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apposcopy.model.Sample;
import com.apposcopy.synthesis.sat4j.Constraint.ConstraintType;
import com.apposcopy.synthesis.sat4j.Constraint.EncodingType;
import com.apposcopy.util.Util;

import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * SAT-based encoding for malware signature. Initial idea from Osbert
 * 
 * @author osbert
 * @author yufeng
 * @author Ruben
 */
public class MatchingEncoding implements Encoding {

	/**
	 * Members of Encoding class
	 */
	protected List<Constraint> problem_constraints;
	protected List<Constraint> conflict_constraints;
	
	boolean is_connected;

	List<Sample> samples;

	protected List<Variable> list_variables;

	protected List<IntentFilterVar> list_ift_vars;
	
	protected List<DangerApiVar> list_api_vars;

	protected List<EdgeVar> list_edge_vars;
	
	protected List<EmbedVar> list_embed_vars;

	protected List<IsEmbedVar> list_is_embed_vars;

	protected List<TypeVar> list_type_vars;

	protected List<TaintFlowVar> list_taint_vars;

	protected List<Constraint> objective_functions;

	protected List<Integer> objective_values;

	protected Map<Trio<Integer, String, Integer>, Variable> typeMap_vars;

	protected Map<Trio<Integer, String, String>, Variable> intentFilterMap_vars;
	
	protected Map<Trio<Integer, String, String>, Variable> dangerApiMap_vars;

	protected Map<Trio<Integer, String, String>, Variable> edgeMap_vars;
	
	protected Map<Trio<Integer, String, String>, Variable> embedMap_vars;

	protected Map<Pair<Integer, String>, Variable> isEmbedMap_vars;

	protected Map<Pent<Integer, String, String, String, String>, Variable> taintMap_vars;

	protected int objective_id;
	
	protected Sample signature;

	protected Sample sample;
	
	protected List<String> sigComps = new ArrayList<>();
	protected List<String> sampleComps = new ArrayList<>();

	// hack eq!
	protected List<EdgeVar> list_eq_fn_vars;
	protected Map<EdgeVar, EdgeVar> map_eq_fn_vars;
	protected List<Variable> model_eq;
	protected Map<EdgeVar, Integer> map_eq_fn_activity;

	protected int index_var = 0;
	
	protected List<String> activities = new ArrayList<>();
	protected List<String> services = new ArrayList<>();
	protected List<String> receivers = new ArrayList<>();
	protected Set<String> allFilters = new HashSet<>();
	protected Set<String> sigApis = new HashSet<>();
	protected Set<String> sources = new HashSet<>();
	protected Set<String> sinks = new HashSet<>();
	
	protected Map<Integer, Set<String>> compSetMap = new HashMap<>();
	
	// all possible src components.
	protected Map<Integer, Set<String>> srcCompsMap = new HashMap<>();

	protected Map<Integer, Set<String>> sinkCompsMap = new HashMap<>();
	
	/**
	 * Constructors
	 */
	public MatchingEncoding(Sample sig, Sample s) {
		signature = sig;
		sample = s;

		is_connected = false;
		
		typeMap_vars = new HashMap<>();
		intentFilterMap_vars = new HashMap<>();
		edgeMap_vars = new HashMap<>();
		taintMap_vars = new HashMap<>();
		embedMap_vars = new HashMap<>();
		isEmbedMap_vars = new HashMap<>();
		dangerApiMap_vars = new HashMap<>();
		
		problem_constraints = new ArrayList<Constraint>();
		conflict_constraints = new ArrayList<Constraint>();

		list_variables = new ArrayList<Variable>();
		list_edge_vars = new ArrayList<EdgeVar>();
		list_embed_vars = new ArrayList<EmbedVar>();
		list_is_embed_vars = new ArrayList<IsEmbedVar>();
		list_ift_vars = new ArrayList<IntentFilterVar>();
		list_api_vars = new ArrayList<>();
		list_type_vars = new ArrayList<TypeVar>();
		list_taint_vars = new ArrayList<TaintFlowVar>();
		objective_functions = new ArrayList<>();
		objective_values = new ArrayList<Integer>();

		list_eq_fn_vars = new ArrayList<EdgeVar>();
		map_eq_fn_vars = new HashMap<>();
		map_eq_fn_activity = new HashMap<>();
		model_eq = new ArrayList<>();

		objective_id = 0;
		index_var = 0;
	}
	
	/**
	 * Public methods
	 */

	public void build() {
		init();
		createVariables();
		createConstraints();
		createObjectiveFunctions();
	}
	
	public boolean isConnected(){
		return is_connected;
	}
	
	public void init() {
		// intent filter.
		for (Pair<String, String> pair : signature.getIntentFilters()) {
			allFilters.add(pair.val1);
		}
		for (Pair<String, String> pair : sample.getIntentFilters()) {
			allFilters.add(pair.val1);
		}
		// dangerous API
		for (Pair<String, String> pair : signature.getDangerAPIs()) {
			sigApis.add(pair.val1);
		}
		
		//taint flows
		buildCompFlowsMap();
	}
	
	public void buildCompFlowsMap() {
		for (Quad<String, String, String, String> quad : signature
				.getTaintFlows()) {
			String src = quad.val1;
			String sink = quad.val3;
			sources.add(src);
			sinks.add(sink);
		}
	}
	
	public void createVariables() {
		//Create variables for signature 0.
		genVar(signature, 0);
		//Create variables for sample 1.
		genVar(sample, 1);
		
		//Generate embedVar.
		genEmbedVar();
		
		if (Util.verbose) {
			System.out.println("=========================================");
			System.out.println("Total vars: " + this.list_variables.size());
			System.out.println("Edge vars:" + this.list_edge_vars.size());
			System.out.println("Flow vars:" + this.list_taint_vars.size());
			System.out.println("Intent filter vars:" + this.list_ift_vars.size());
			System.out.println("Comp Type vars:" + this.list_type_vars.size());
			System.out.println("Embed vars:" + this.list_embed_vars.size());
			System.out.println("=========================================");
		}
	}
	
	public void genEmbedVar() {
		Set<String> sigComps = compSetMap.get(0);
		Set<String> comps = compSetMap.get(1);
		int idx = 1;

		for (String sig : sigComps) {
			for (String comp : comps) {
				EmbedVar ev = new EmbedVar(index_var++, idx, sig, comp);
				list_variables.add(ev);
				list_embed_vars.add(ev);
				Trio<Integer, String, String> trio = new Trio<>(idx, sig, comp);
				embedMap_vars.put(trio, ev);
			}
		}
	}

	public void genVar(Sample s, int idx) {
		Set<String> comps = new HashSet<>();

		comps.addAll(s.getActivities());
		comps.addAll(s.getServices());
		comps.addAll(s.getReceivers());
		compSetMap.put(idx, comps);

		// Component typeVar. 1: activity; 2: service; 3: receiver
		for (String comp : comps) {
			for (int type = 1; type <= 3; type++) {
				TypeVar tv3 = new TypeVar(index_var++, idx, comp, type);
				list_type_vars.add(tv3);
				list_variables.add(tv3);
				Trio<Integer, String, Integer> trio = new Trio<>(idx, comp,
						type);
				typeMap_vars.put(trio, tv3);
			}
		}

		// IntentFilters
		for (String comp : comps) {
			for (String filter : allFilters) {
				IntentFilterVar ift = new IntentFilterVar(index_var++, idx,
						comp, filter);
				list_variables.add(ift);
				list_ift_vars.add(ift);
				Trio<Integer, String, String> trio = new Trio<>(idx, comp,
						filter);
				intentFilterMap_vars.put(trio, ift);
			}
		}
		
		// Dangerous API
		for (String comp : comps) {
			for (String api : sigApis) {
				DangerApiVar apiVar = new DangerApiVar(index_var++, idx,
						comp, api);
				list_variables.add(apiVar);
				list_api_vars.add(apiVar);
				Trio<Integer, String, String> trio = new Trio<>(idx, comp,
						api);
				dangerApiMap_vars.put(trio, apiVar);
			}
		}

		// edges
		for (String comp0 : comps) {
			for (String comp1 : comps) {
				EdgeVar edgeVar = new EdgeVar(index_var++, idx, comp0, comp1);
				list_variables.add(edgeVar);
				list_edge_vars.add(edgeVar);
				Trio<Integer, String, String> trio = new Trio<>(idx, comp0,
						comp1);
				edgeMap_vars.put(trio, edgeVar);
			}
		}
		
		Set<String> srcComps = new HashSet<>();
		Set<String> sinkComps = new HashSet<>();
		for (Quad<String, String, String, String> quad : s.getTaintFlows()) {
			String srcComp = quad.val0;
			String sinkComp = quad.val2;
			srcComps.add(srcComp);
			sinkComps.add(sinkComp);
		}
		srcCompsMap.put(idx, srcComps);
		sinkCompsMap.put(idx, sinkComps);

		// Taint flows
		for (String comp0 : srcComps) {
			for (String comp1 : sinkComps) {
				for (String src : sources) {
					for (String sink : sinks) {
						TaintFlowVar flowVar = new TaintFlowVar(index_var++,
								idx, comp0, src, comp1, sink);
						list_variables.add(flowVar);
						list_taint_vars.add(flowVar);

						Pent<Integer, String, String, String, String> pent = new Pent<>(
								idx, comp0, src, comp1, sink);
						taintMap_vars.put(pent, flowVar);
					}
				}
			}
		}
	}
	
	public void genBasicCst(Sample s, int idx) {
		Set<String> comps = compSetMap.get(idx);
		// / 1.1 type constraints
		// Component typeVar. 1: activity; 2: service; 3: receiver
		for (String comp : comps) {
			for (int type = 1; type <= 3; type++) {
				// activity
				Trio<Integer, String, Integer> trio = new Trio<>(idx, comp,
						type);
				assert typeMap_vars.containsKey(trio);
				Variable tv = typeMap_vars.get(trio);
				List<Variable> constraint = new ArrayList<>();
				constraint.add(tv);
				boolean flag = false;
				if ((s.getActivities().contains(comp) && (type == 1))
						|| (s.getServices().contains(comp) && (type == 2))
						|| (s.getReceivers().contains(comp) && (type == 3)))
					flag = true;

				if (flag) {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 1, EncodingType.INIT));
				} else
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 0, EncodingType.INIT));

			}
		}

		// / 1.2 edge constraints
		Map<String, List<String>> compEdges = new HashMap<>();
		for (Pair<String, String> e : s.getIccg()) {
			String src = e.val0;
			String tgt = e.val1;
			List<String> tgts = new ArrayList<>();
			if (compEdges.containsKey(src)) {
				tgts = compEdges.get(src);
				tgts.add(tgt);
			} else {
				tgts.add(tgt);
				compEdges.put(src, tgts);
			}
		}
		for (String comp0 : comps) {
			for (String comp1 : comps) {
				Trio<Integer, String, String> trio = new Trio<>(idx, comp0,
						comp1);
				assert edgeMap_vars.containsKey(trio);
				Variable edgeVar = edgeMap_vars.get(trio);
				List<Variable> constraint = new ArrayList<>();
				constraint.add(edgeVar);
				if (!compEdges.containsKey(comp0)
						|| !compEdges.get(comp0).contains(comp1)) {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 0, EncodingType.INIT));
				} else {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 1, EncodingType.INIT));
				}
			}
		}

		// / 1.3 Intent filter constraints
		Map<String, List<String>> compFilters = new HashMap<>();
		for (Pair<String, String> actFilter : s.getIntentFilters()) {
			String comp = actFilter.val0;
			String filter = actFilter.val1;
			List<String> filters = new ArrayList<>();
			if (compFilters.containsKey(comp)) {
				filters = compFilters.get(comp);
				filters.add(filter);
			} else {
				filters.add(filter);
				compFilters.put(comp, filters);
			}
		}
		
		for (String comp : comps) {
			for (String filter : allFilters) {
				Trio<Integer, String, String> trio = new Trio<>(idx, comp,
						filter);
				assert intentFilterMap_vars.containsKey(trio) : trio;
				Variable iftVar = intentFilterMap_vars.get(trio);
				List<Variable> constraint = new ArrayList<>();
				constraint.add(iftVar);
				if (!compFilters.containsKey(comp)
						|| !compFilters.get(comp).contains(filter)) {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 0, EncodingType.INIT));
				} else {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 1, EncodingType.INIT));

				}
			}
		}

		// / 1.4 taint flow constraints
		Set<String> srcComps = srcCompsMap.get(idx);
		Set<String> sinkComps = sinkCompsMap.get(idx);
		for (String comp0 : srcComps) {
			for (String comp1 : sinkComps) {
				for (String src : sources) {
					for (String sink : sinks) {
						Pent<Integer, String, String, String, String> pent = new Pent<>(
								idx, comp0, src, comp1, sink);
						// exist in the current taint flow?
						boolean flag = false;
						for (Quad<String, String, String, String> quad : s
								.getTaintFlows()) {
							if (quad.val0.equals(comp0)
									&& quad.val1.equals(src)
									&& quad.val2.equals(comp1)
									&& quad.val3.equals(sink)) {
								flag = true;
								break;
							}
						}
						if (!taintMap_vars.containsKey(pent))
							continue;

						assert taintMap_vars.containsKey(pent);
						Variable taintVar = taintMap_vars.get(pent);

						List<Variable> constraint = new ArrayList<>();
						constraint.add(taintVar);

						if (flag)
							problem_constraints.add(new Constraint(constraint,
									ConstraintType.EQ, 1, EncodingType.INIT));
						else
							problem_constraints.add(new Constraint(constraint,
									ConstraintType.EQ, 0, EncodingType.INIT));
					}
				}
			}
		}
		
		// / 1.5 Dangerous APIs
		Map<String, List<String>> compApis = new HashMap<>();
		for (Pair<String, String> api : s.getDangerAPIs()) {
			String comp = api.val0;
			String val = api.val1;
			List<String> apis = new ArrayList<>();
			if (compApis.containsKey(comp)) {
				apis = compApis.get(comp);
				apis.add(val);
			} else {
				apis.add(val);
				compApis.put(comp, apis);
			}
		}
		
		for (String comp : comps) {
			for (String api : sigApis) {
				Trio<Integer, String, String> trio = new Trio<>(idx, comp,
						api);
				assert dangerApiMap_vars.containsKey(trio) : trio;
				Variable apiVar = dangerApiMap_vars.get(trio);
				List<Variable> constraint = new ArrayList<>();
				constraint.add(apiVar);
				if (!compApis.containsKey(comp)
						|| !compApis.get(comp).contains(api)) {
					problem_constraints.add(new Constraint(constraint,
							ConstraintType.EQ, 0, EncodingType.INIT));
				} else {
					Constraint cst = new Constraint(constraint,
							ConstraintType.EQ, 1, EncodingType.INIT);
					problem_constraints.add(cst);

				}
			}
		}

	}
	
	public void createConstraints() {
		Set<String> sigComps = compSetMap.get(0);
		Set<String> comps = compSetMap.get(1);
		int idx = 1;
		
		// FIXME: dummy constraints
		for(Variable v : this.list_variables) {
			List<Variable> dummyList = new ArrayList<>();
			dummyList.add(v);
			problem_constraints.add(new Constraint(dummyList, ConstraintType.GEQ, 0, EncodingType.INIT));
		}
		
		/// 1. Basic hard constraints from inputs.
		genBasicCst(signature, 0);
		genBasicCst(sample, 1);

		/// 2. One to one mapping.
		// Construct the constraints
		//
		// embedVar[x, w] + embedVar[x, w'] \le 1
		//
		// for all x \in Signature, w != w' \in Sample[i], i \ge 0
		//
		for (String sig : sigComps) {
			for (String comp0 : comps) {
				for (String comp1 : comps) {
					if (comp0.equals(comp1)) {
						continue;
					}

					// embedVar[x, w]
					Trio<Integer, String, String> trio0 = new Trio<>(idx, sig,
							comp0);
					EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);

					// embedVar[x, w']
					Trio<Integer, String, String> trio1 = new Trio<>(idx, sig,
							comp1);
					EmbedVar ev1 = (EmbedVar) embedMap_vars.get(trio1);

					// build the constraint
					List<Variable> literals = new ArrayList<>();
					literals.add(ev0);
					literals.add(ev1);

					List<Integer> coefficients = new ArrayList<>();
					coefficients.add(1);
					coefficients.add(1);

					problem_constraints.add(new Constraint(literals,
							coefficients, ConstraintType.LEQ, 1));
				}
			}
		}
			
			// Construct the constraints
			//
			//  embedVar[x, w] + embedVar[x', w] \le 1
			//
			// for all x != x' \in Signature, w \in Sample[i], i \ge 0
			//
			for (String sig0 : sigComps) {
				for (String sig1 : sigComps) {
					if (sig0.equals(sig1)) {
						continue;
					}
					
					for (String comp : comps) {
						// embedVar[x, w]
						Trio<Integer, String, String> trio0 = new Trio<>(idx, sig0, comp);
						EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);
						
						// embedVar[x', w]
						Trio<Integer, String, String> trio1 = new Trio<>(idx, sig1, comp);
						EmbedVar ev1 = (EmbedVar) embedMap_vars.get(trio1);

						// build the constraint
						List<Variable> literals = new ArrayList<>();
						literals.add(ev0);
						literals.add(ev1);
						
						List<Integer> coefficients = new ArrayList<>();
						coefficients.add(1);
						coefficients.add(1);
						
						problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 1));
					}					
				}
			}

		/// 3. Type constraints
			// Construct the constraints
			//
			//  embedVar[x, w] \le 1 + Bi
			//
			// where
			//
			//  B1 = + typeVar[x, t] - typeVar[w, t]
			//  B2 = - typeVar[x, t] + typeVar[w, t]
			//
			// for every x \in Signature, w \in Sample[i], i \ge 1
			//
		for (String sig : sigComps) {
			for (String comp : comps) {
				for (int type = 1; type <= 3; type++) {
					// embedVar[x, w]
					Trio<Integer, String, String> trioEmbed = new Trio<>(idx,
							sig, comp);
					EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);

					// typeVar[x, t]
					Trio<Integer, String, Integer> trioTypeSig = new Trio<>(0,
							sig, type);
					TypeVar tvSig = (TypeVar) typeMap_vars.get(trioTypeSig);

					// typeVar[w, t]
					Trio<Integer, String, Integer> trioType = new Trio<>(idx,
							comp, type);
					TypeVar tv = (TypeVar) typeMap_vars.get(trioType);

					// build the constraints Bi
					for (int n = -1; n <= 1; n += 2) {
						List<Variable> literals = new ArrayList<>();
						literals.add(ev);
						literals.add(tvSig);
						literals.add(tv);

						List<Integer> coefficients = new ArrayList<>();
						coefficients.add(1);
						coefficients.add(n);
						coefficients.add(-n);
						problem_constraints.add(new Constraint(literals,
								coefficients, ConstraintType.LEQ, 1));
					}
				}
			}
		}

		/// 4. Hard constraints for edges: if there is an edge in the signature, then there must be such isomorphic edge in all samples.		
			// -embedVar[x, w] - embedVar[x', w'] - edgeVar[x, x'] + edgeVar[w, w'] \ge -2
		for (String sig0 : sigComps) {
			for (String comp0 : comps) {
				for (String sig1 : sigComps) {
					for (String comp1 : comps) {
						// embedVar[x, w]
						Trio<Integer, String, String> trio0 = new Trio<>(idx,
								sig0, comp0);
						EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);

						// embedVar[x', w']
						Trio<Integer, String, String> trio1 = new Trio<>(idx,
								sig1, comp1);
						EmbedVar ev1 = (EmbedVar) embedMap_vars.get(trio1);

						// edgeVar[x, x']
						Trio<Integer, String, String> trioSig = new Trio<>(0,
								sig0, sig1);
						EdgeVar edgeVarSig = (EdgeVar) edgeMap_vars
								.get(trioSig);

						// edgeVar[w, w']
						Trio<Integer, String, String> trio = new Trio<>(idx,
								comp0, comp1);
						EdgeVar edgeVar = (EdgeVar) edgeMap_vars.get(trio);

						List<Variable> literals = new ArrayList<>();
						literals.add(ev0);
						literals.add(ev1);
						literals.add(edgeVarSig);
						literals.add(edgeVar);

						List<Integer> coefficients = new ArrayList<>();
						coefficients.add(-1);
						coefficients.add(-1);
						coefficients.add(-1);
						coefficients.add(1);

						problem_constraints.add(new Constraint(literals,
								coefficients, ConstraintType.GEQ, -2));
					}
				}
			}
		}
		
		//5. Each node of the signature has one exact embedding
		for (String sig : sigComps) {
			List<Variable> literals = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();
			for (String comp : comps) {
				// embedVar[x, w]
				Trio<Integer, String, String> trioEmbed = new Trio<>(idx, sig,
						comp);
				EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);
				literals.add(ev);
				coefficients.add(1);
			}
			problem_constraints.add(new Constraint(literals, coefficients,
					ConstraintType.EQ, 1, EncodingType.EMBED));
		}
		
		/// 7. Intent filters
			// Construct the constraints
			//
			//  embedVar[x, w] + filter[x, f] \le 1 + filter[w, f]
			//
			// for every x \in Signature, w \in Sample[i], i \ge 0
			//
		for (String sig : sigComps) {
			for (String comp : comps) {
				for (String filter : allFilters) {
					// embedVar[x, w]
					assert idx == 1;
					Trio<Integer, String, String> trioEmbed = new Trio<>(idx,
							sig, comp);
					EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);

					// filter[x, f]
					Trio<Integer, String, String> trioSig = new Trio<>(0, sig,
							filter);
					Variable iftSig = intentFilterMap_vars.get(trioSig);

					// filter[w, f]
					Trio<Integer, String, String> trioSample = new Trio<>(idx,
							comp, filter);
					Variable ift = intentFilterMap_vars.get(trioSample);

					// Build constraints
					List<Variable> literals = new ArrayList<>();
					literals.add(ev);
					literals.add(iftSig);
					literals.add(ift);

					List<Integer> coefficients = new ArrayList<>();
					coefficients.add(1);
					coefficients.add(1);
					coefficients.add(-1);

					problem_constraints.add(new Constraint(literals,
							coefficients, ConstraintType.LEQ, 1));
				}
			}
		}
		
		/// 8. Taint flows
			// Construct the constraints
			//
			//  flow[x, x', embedVar[x, w] + embedVar[x', w'] + flow[x, x', src, sink] \le 2 + flow[w, w']
			//
			// for every x, x' \in Signature, w, w' \in Sample[i], i \ge 0
			//
		for (String sig0 : sigComps) {
			for (String sig1 : sigComps) {
				for (String comp0 : comps) {
					for (String comp1 : comps) {
						for (String src : sources) {
							for (String sink : sinks) {
								// embedVar[x, w]
								Trio<Integer, String, String> trio0 = new Trio<>(
										idx, sig0, comp0);
								EmbedVar ev0 = (EmbedVar) embedMap_vars
										.get(trio0);

								// embedVar[x', w']
								Trio<Integer, String, String> trio1 = new Trio<>(
										idx, sig1, comp1);
								EmbedVar ev1 = (EmbedVar) embedMap_vars
										.get(trio1);

								// flow[x, x', src, sink]
								Pent<Integer, String, String, String, String> pentSig = new Pent<>(
										0, sig0, src, sig1, sink);
								Variable taintVarSig = taintMap_vars
										.get(pentSig);

								// flow[w, w', src, sink]
								Pent<Integer, String, String, String, String> pent = new Pent<>(
										idx, comp0, src, comp1, sink);
								Variable taintVar = taintMap_vars.get(pent);

								// Build constraints
								List<Variable> literals = new ArrayList<>();
								literals.add(ev0);
								literals.add(ev1);
								literals.add(taintVarSig);
								literals.add(taintVar);
								if(taintVar == null || taintVarSig == null) continue;
								
								List<Integer> coefficients = new ArrayList<>();
								coefficients.add(1);
								coefficients.add(1);
								coefficients.add(1);
								coefficients.add(-1);

								problem_constraints.add(new Constraint(
										literals, coefficients,
										ConstraintType.LEQ, 2));
							}
						}
					}
				}
			}
		}
		
		/// 9. dangerous apis
		// Construct the constraints
		//
		//  embedVar[x, w] + api[x, f] \le 1 + api[w, f]
		//
		// for every x \in Signature, w \in Sample[i], i \ge 0
		//
	for (String sig : sigComps) {
		for (String comp : comps) {
			for (String api : sigApis) {
				// embedVar[x, w]
				assert idx == 1;
				Trio<Integer, String, String> trioEmbed = new Trio<>(idx,
						sig, comp);
				EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);

				// api[x, f]
				Trio<Integer, String, String> trioSig = new Trio<>(0, sig,
						api);
				Variable iftSig = dangerApiMap_vars.get(trioSig);

				// api[w, f]
				Trio<Integer, String, String> trioSample = new Trio<>(idx,
						comp, api);
				Variable ift = dangerApiMap_vars.get(trioSample);

				// Build constraints
				List<Variable> literals = new ArrayList<>();
				literals.add(ev);
				literals.add(iftSig);
				literals.add(ift);

				List<Integer> coefficients = new ArrayList<>();
				coefficients.add(1);
				coefficients.add(1);
				coefficients.add(-1);

				Constraint cst = new Constraint(literals,
						coefficients, ConstraintType.LEQ, 1);
				
				problem_constraints.add(cst);
			}
		}
	}
	}

	public void createObjectiveFunctions() {
	
	}
	
	public List<Integer> getObjectiveValues() {
		return objective_values;
	}

	public void setObjectiveId(int objective_id) {
		this.objective_id = objective_id;
	}

	public List<Constraint> getConflictConstraints() {
		return conflict_constraints;
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

	public List<Constraint> getObjectiveFunctions() {
		return objective_functions;
	}

	public Integer getObjectiveId() {
		return objective_id;
	}
	
	List<Variable> blockedGraph = new ArrayList<>();
	
	public List<Variable> getBlockedGraph() {
		return blockedGraph;
	}

	public List<String> saveModel(Solver solver, String solution) {
		List<Variable> list = new ArrayList<>();
		List<String> res = new ArrayList<>();

		blockedGraph.clear();
		
		if (Util.verbose) {
			System.out.println("nVars: " + solver.getSolver().nVars());
			System.out.println("nCsts: " + solver.getSolver().nConstraints());
			System.out.println("models: " + solver.getSolver().model().length);
		}
		//Checking if the graph is connected.
		Set<Pair<String,String>> edges = new HashSet<>();
		Set<String> nodes = new HashSet<>();

		for (int i = 0; i < list_edge_vars.size(); i++) {
			EdgeVar var = list_edge_vars.get(i);
			
			if(var.getIndex() != 0)
				continue;
			
			list.add(var);
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;

			edges.add(new Pair<>(var.getSrc(),var.getTgt()));
			res.add(var.getSrc() + "[edgeTo]" + var.getTgt());
		}
		
		//Intent filter.
		for(int i = 0; i < list_ift_vars.size(); i++) {
			IntentFilterVar var = list_ift_vars.get(i);
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;
			
			res.add(var.getCompSrc() + " [intentFilter] " + var.getFilter());
		}
		
		//Dangerous API.
		for(int i = 0; i < list_api_vars.size(); i++) {
			DangerApiVar var = list_api_vars.get(i);
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;
			
			res.add(var.getCompSrc() + " [DangerAPI] " + var.getApi());
		}
		
		for (int i = 0; i < list_taint_vars.size(); i++) {
			TaintFlowVar var = list_taint_vars.get(i);

			if (var.getIndex() != 0)
				continue;
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;


			res.add(var.getCompSrc() + " [src] " + var.getSrcType() + " [TaintFlow] " + var.getCompTgt() + "[sink]"
					+ var.getSinkType());
		}
		
		for (int i = 0; i < list_embed_vars.size(); i++) {
			EmbedVar var = list_embed_vars.get(i);
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;

			res.add(var.getMapSig() + " [embedTo] " + var.getMapSample()
					+ " @sample" + var.getIndex());
			nodes.add(var.getMapSig());
		}
		
		is_connected = Util.isConnected(edges, nodes);
		
		if(Util.verbose)
			System.out.println("Is Connected graph? " + is_connected);
		
		for (int i = 0; i < list_type_vars.size(); i++) {
			TypeVar var = list_type_vars.get(i);
			
			if(var.getIndex() != 0)
				continue;
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;

			res.add(var.getName() + " [typeOf] " + var.getType());
		}
		
		model_eq = list;
		return res;
	}
	
	public List<Variable> getBlockVars() {
		return model_eq;
	}
	
	public void print() {

		System.out.println("#Variables: " + nVars());
		System.out.println("#Constraints: " + nConstraints());

		for (Constraint c : problem_constraints) {
			c.print();
		}
	}

	public void printVariables() {
		System.out.println("list_place_vars: " + list_type_vars.size());

		for (TypeVar pv : list_type_vars) {
			System.out.println(pv.toString());
		}

	}

	public void reset() {
		problem_constraints.clear();
		conflict_constraints.clear();
		list_variables.clear();

		objective_values.clear();
		objective_id = 0;
	}

	@Override
	public Sample getSignature(Solver solver) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void printFormula(){
		
	}
}