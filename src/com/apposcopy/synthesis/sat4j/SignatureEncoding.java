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
public class SignatureEncoding implements Encoding {

	/**
	 * Members of Encoding class
	 */
	protected List<Constraint> problem_constraints;
	protected List<Constraint> conflict_constraints;
	
	boolean is_connected;

	List<Sample> samples;

	protected List<Variable> list_variables;

	protected List<IntentFilterVar> list_ift_vars;

	protected List<EdgeVar> list_edge_vars;
	
	protected List<EmbedVar> list_embed_vars;

	protected List<IsEmbedVar> list_is_embed_vars;

	protected List<TypeVar> list_type_vars;

	protected List<TaintFlowVar> list_taint_vars;

	protected List<Constraint> objective_functions;

	protected List<Integer> objective_values;

	protected Map<Trio<Integer, String, Integer>, Variable> typeMap_vars;

	protected Map<Trio<Integer, String, String>, Variable> intentFilterMap_vars;

	protected Map<Trio<Integer, String, String>, Variable> edgeMap_vars;
	
	protected Map<Trio<Integer, String, String>, Variable> embedMap_vars;

	protected Map<Pair<Integer, String>, Variable> isEmbedMap_vars;

	protected Map<Pent<Integer, String, String, String, String>, Variable> taintMap_vars;

	protected int objective_id;

	// hack eq!
	protected List<EdgeVar> list_eq_fn_vars;
	protected Map<EdgeVar, EdgeVar> map_eq_fn_vars;
	protected List<Variable> model_eq;
	protected Map<EdgeVar, Integer> map_eq_fn_activity;

	protected int index_var = 0;
	
	protected List<String> components = new ArrayList<>();
	protected List<String> activities = new ArrayList<>();
	protected List<String> services = new ArrayList<>();
	protected List<String> receivers = new ArrayList<>();
	protected Set<String> allFilters = new HashSet<>();
	protected Set<String> sources = new HashSet<>();
	protected Set<String> sinks = new HashSet<>();
	
	// T maps to S where T is the type of the component and S is the set of
	// sources/sinks that T could contain. Bounded by the sample that contains
	// the minimum flows.
	protected Map<Integer, Set<String>> compFlowsMap = new HashMap<>();

	// T<i,c> maps to n where i is the index of the sample and c is the name of the
	// component. n is the type of the component.
	protected Map<Pair<Integer,String>, Integer> compTypeMap = new HashMap<>();

	// i maps to S where i is the index of the sample and S is all components in
	// sample i.
	protected Map<Integer, Set<String>> compSetMap = new HashMap<>();

	// all possible src components.
	protected Map<Integer, Set<String>> srcCompsMap = new HashMap<>();

	protected Map<Integer, Set<String>> sinkCompsMap = new HashMap<>();

	/**
	 * Constructors
	 */
	public SignatureEncoding(List<Sample> s) {
		samples = s;

		is_connected = false;
		
		typeMap_vars = new HashMap<>();
		intentFilterMap_vars = new HashMap<>();
		edgeMap_vars = new HashMap<>();
		taintMap_vars = new HashMap<>();
		embedMap_vars = new HashMap<>();
		isEmbedMap_vars = new HashMap<>();

		problem_constraints = new ArrayList<Constraint>();
		conflict_constraints = new ArrayList<Constraint>();

		list_variables = new ArrayList<Variable>();
		list_edge_vars = new ArrayList<EdgeVar>();
		list_embed_vars = new ArrayList<EmbedVar>();
		list_is_embed_vars = new ArrayList<IsEmbedVar>();
		list_ift_vars = new ArrayList<IntentFilterVar>();
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
		
		isInference = false;
	}
	
	boolean isInference;
	
	private static List<Sample> getList(Sample signature, Sample sample) {
		List<Sample> samples = new ArrayList<>();
		samples.add(signature);
		samples.add(sample);
		return samples;
	}
	
	public SignatureEncoding(Sample signature, Sample sample) {
		this(getList(signature, sample));
		isInference = true;
	}

	/**
	 * Public methods
	 */

	public void build() {
		init();
		createVariables();
		createConstraints();
		if(!isInference)
			createObjectiveFunctions();
		createInferenceConstraints();
	}
	
	public boolean isConnected(){
		return is_connected;
	}
	
	public void init() {
		for (Sample s : samples) {
			// index of each sample.
			int sIndex = samples.indexOf(s);
			// sample id starts from 1.
			sIndex++;

			Set<String> comps = new HashSet<>();
			comps.addAll(s.getActivities());
			comps.addAll(s.getServices());
			comps.addAll(s.getReceivers());
			compSetMap.put(sIndex, comps);
			
			Set<String> srcs = new HashSet<>();
			Set<String> sinks = new HashSet<>();
			for (Quad<String, String, String, String> quad : s.getTaintFlows()) {
				String srcComp = quad.val0;
				String sinkComp = quad.val2;
				srcs.add(srcComp);
				sinks.add(sinkComp);
			}
			srcCompsMap.put(sIndex, srcs);
			sinkCompsMap.put(sIndex, sinks);
		}
		
		buildCompFlowsMap();
	}
	
	public void buildCompFlowsMap() {
		int min = 10000;
		Sample minSample = samples.get(0);
		for(Sample s : samples) {
			if(s.getTaintFlows().size() < min) {
				min = s.getTaintFlows().size();
				minSample = s;
			}
		}
		
		Map<String, Integer> compTypeMap = new HashMap<>();
		for(String t : minSample.getActivities()) {
			compTypeMap.put(t, 1);
		}
		
		for(String t : minSample.getServices()) {
			compTypeMap.put(t, 2);
		}
		
		for(String t : minSample.getReceivers()) {
			compTypeMap.put(t, 3);
		}
		//FIXME
//		assert minSample.getTaintFlows().size() > 0 : samples.get(0).getIccg();
		for (Quad<String, String, String, String> quad : minSample
				.getTaintFlows()) {
			String srcComp = quad.val0;
			int srcType = compTypeMap.get(srcComp);
			String src = quad.val1;
			String sinkComp = quad.val2;
			int sinkType = compTypeMap.get(sinkComp);
			String sink = quad.val3;
			Set<String> srcSet = new HashSet<>();
			Set<String> sinkSet = new HashSet<>();

			if (compFlowsMap.containsKey(srcType)) {
				srcSet = compFlowsMap.get(srcType);
			}
			srcSet.add(src);
			compFlowsMap.put(srcType, srcSet);

			if (compFlowsMap.containsKey(sinkType)) {
				sinkSet = compFlowsMap.get(sinkType);
			}
			sinkSet.add(sink);
			compFlowsMap.put(sinkType, sinkSet);
			sources.add(src);
			sinks.add(sink);
		}
	}

	public void createVariables() {
		// signature id is 0.
		createSignatureVars();
		// outer loop: iterate each sample.
		for (Sample s : samples) {
			// index of each sample.
			int sIndex = samples.indexOf(s);
			// sample id starts from 1.
			sIndex++;
			assert compSetMap.containsKey(sIndex);
			Set<String> comps = compSetMap.get(sIndex);

			// Component typeVar. 1: activity; 2: service; 3: receiver
			for (String comp : comps) {
				for (int type = 1; type <= 3; type++) {
					TypeVar tv3 = new TypeVar(index_var++, sIndex, comp, type);
					list_type_vars.add(tv3);
					list_variables.add(tv3);
					Trio<Integer, String, Integer> trio = new Trio<>(sIndex, comp, type);
					typeMap_vars.put(trio, tv3);
				}
			}

			// IntentFilters
			for (String comp : comps) {
				for (String filter : allFilters) {
					IntentFilterVar ift = new IntentFilterVar(index_var++, sIndex, comp, filter);
					list_variables.add(ift);
					list_ift_vars.add(ift);
					Trio<Integer, String, String> trio = new Trio<>(sIndex, comp, filter);
					intentFilterMap_vars.put(trio, ift);
				}
			}

			// edges
			for (String comp0 : comps) {
				for (String comp1 : comps) {
					EdgeVar edgeVar = new EdgeVar(index_var++, sIndex, comp0, comp1);
					list_variables.add(edgeVar);
					list_edge_vars.add(edgeVar);
					Trio<Integer, String, String> trio = new Trio<>(sIndex, comp0, comp1);
					edgeMap_vars.put(trio, edgeVar);
				}
			}

			// Taint flows
			Map<String, Integer> compTypeMap = new HashMap<>();
			for(String t : s.getActivities()) {
				compTypeMap.put(t, 1);
			}
			
			for(String t : s.getServices()) {
				compTypeMap.put(t, 2);
			}
			
			for(String t : s.getReceivers()) {
				compTypeMap.put(t, 3);
			}
			
			Set<String> srcComps = srcCompsMap.get(sIndex);
			Set<String> sinkComps = sinkCompsMap.get(sIndex);

			for (String comp0 : srcComps) {
				int srcType = compTypeMap.get(comp0);
				for (String comp1 : sinkComps) {
					int sinkType = compTypeMap.get(comp1);
					for (String src : sources) {
						for (String sink : sinks) {
							if (!compFlowsMap.containsKey(srcType)
									|| !compFlowsMap.containsKey(sinkType)
									|| !compFlowsMap.get(srcType).contains(src)
									|| !compFlowsMap.get(sinkType).contains(
											sink))
								continue;

							TaintFlowVar flowVar = new TaintFlowVar(
									index_var++, sIndex, comp0, src, comp1,
									sink);
							list_variables.add(flowVar);
							list_taint_vars.add(flowVar);

							Pent<Integer, String, String, String, String> pent = new Pent<>(
									sIndex, comp0, src, comp1, sink);
							taintMap_vars.put(pent, flowVar);
						}
					}
				}
			}

		}

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
	
	Map<String, Integer> sigTypeMap = new HashMap<>();

	public void createSignatureVars() {
		// Determine the maximum number of components in target signature S.
		int maxAct = -1;
		int maxServ = -1;
		int maxRecv = -1;
		for (Sample s : samples) {
			if (maxAct == -1 || s.getActivities().size() < maxAct)
				maxAct = s.getActivities().size();

			if (maxServ == -1 || s.getServices().size() < maxServ)
				maxServ = s.getServices().size();

			if (maxRecv == -1 || s.getReceivers().size() < maxRecv)
				maxRecv = s.getReceivers().size();
		}
		
		assert maxAct >= 0 && maxServ >= 0 && maxRecv >= 0 : "Not handling case where component type is empty!";

		// Here, I use 0 to denote the index of target signature S
		// and 1, 2,..., n to denote the index of each sample.
		// Init variables for target signature.
		for (Sample s : samples) {
//			for (Quad<String, String, String, String> quad : s.getTaintFlows()) {
//				String source = quad.val1;
//				sources.add(source);
//				String sink = quad.val3;
//				sinks.add(sink);
//			}
			for (Pair<String, String> pair : s.getIntentFilters())
				allFilters.add(pair.val1);
		}

		// embedVars
		for (int i = 0; i < maxAct; i++) {
			String newAct = "activity_" + i;
			components.add(newAct);
			activities.add(newAct);
			sigTypeMap.put(newAct, 1);
		}

		for (int i = 0; i < maxServ; i++) {
			String newServ = "service_" + i;
			components.add(newServ);
			services.add(newServ);
			sigTypeMap.put(newServ, 2);
		}

		for (int i = 0; i < maxRecv; i++) {
			String newRecv = "receiver_" + i;
			components.add(newRecv);
			receivers.add(newRecv);
			sigTypeMap.put(newRecv, 3);
		}
		
		for (String sig : components) {
			IsEmbedVar iev = new IsEmbedVar(index_var++, 0, sig);
			list_variables.add(iev);
			list_is_embed_vars.add(iev);
			Pair<Integer, String> pair = new Pair<>(0, sig);
			isEmbedMap_vars.put(pair, iev);
		}

		for (Sample s : samples) {
			// all components in s.
			int idx = samples.indexOf(s) + 1;
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);
			for (String sig : components) {
				for (String comp : comps) {
					EmbedVar ev = new EmbedVar(index_var++, idx, sig, comp);
					list_variables.add(ev);
					list_embed_vars.add(ev);
					Trio<Integer, String, String> trio = new Trio<>(idx, sig, comp);
					embedMap_vars.put(trio, ev);
				}
			}
		}

		// types
		for (String comp : components) {
			for (int type = 1; type <= 3; type++) {
				TypeVar tv3 = new TypeVar(index_var++, 0, comp, type);
				list_type_vars.add(tv3);
				list_variables.add(tv3);
				Trio<Integer, String, Integer> trio = new Trio<>(0, comp, type);
				typeMap_vars.put(trio, tv3);
			}
		}

		// edges
		for (String comp0 : components) {
			for (String comp1 : components) {
				EdgeVar edgeVar = new EdgeVar(index_var++, 0, comp0, comp1);
				list_variables.add(edgeVar);
				list_edge_vars.add(edgeVar);
				Trio<Integer, String, String> trio = new Trio<>(0, comp0, comp1);
				edgeMap_vars.put(trio, edgeVar);
			}
		}

		// IntentFilters
		for (String comp : components) {
			for (String filter : allFilters) {
				IntentFilterVar ift = new IntentFilterVar(index_var++, 0, comp, filter);
				list_variables.add(ift);
				list_ift_vars.add(ift);
				Trio<Integer, String, String> trio = new Trio<>(0, comp, filter);
				intentFilterMap_vars.put(trio, ift);
			}
		}

		// Taint flows
		for (String comp0 : components) {
			int srcType = sigTypeMap.get(comp0);
			for (String comp1 : components) {
				int sinkType = sigTypeMap.get(comp1);
				for (String src : sources) {
					if (!compFlowsMap.containsKey(srcType)
							|| !compFlowsMap.get(srcType).contains(src))
						continue;
					
					for (String sink : sinks) {
						if (!compFlowsMap.containsKey(sinkType)
								|| !compFlowsMap.get(sinkType).contains(sink))
							continue;
						
						TaintFlowVar flowVar = new TaintFlowVar(index_var++, 0, comp0, src, comp1, sink);
						list_variables.add(flowVar);
						list_taint_vars.add(flowVar);

						Pent<Integer, String, String, String, String> pent = new Pent<>(0, comp0, src, comp1, sink);
						taintMap_vars.put(pent, flowVar);
					}
				}
			}
		}
	}

	public void createConstraints() {
		// FIXME: dummy constraints
		for(Variable v : this.list_variables) {
			List<Variable> dummyList = new ArrayList<>();
			dummyList.add(v);
			problem_constraints.add(new Constraint(dummyList, ConstraintType.GEQ, 0, EncodingType.INIT));
		}
		
		/// 1. Basic hard constraints from inputs.
		for (Sample s : samples) {
			// all components in s.
			int idx = samples.indexOf(s) + 1;
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);

			/// 1.1 type constraints
			// Component typeVar. 1: activity; 2: service; 3: receiver
			for (String comp : comps) {
				for (int type = 1; type <= 3; type++) {
					// activity
					Trio<Integer, String, Integer> trio = new Trio<>(idx, comp, type);
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
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
					}else
						problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.INIT));

				}
			}

			/// 1.2 edge constraints
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

			/// 1.3 Intent filter constraints
			Map<String, List<String>> compFilters = new HashMap<>();
			for (Pair<String, String> actFilter : s.getIntentFilters()) {
				String comp = actFilter.val0;
				String filter = actFilter.val1;
				List<String> filters = new ArrayList<>();
				if(compFilters.containsKey(comp)) {
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
					assert intentFilterMap_vars.containsKey(trio);
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

			/// 1.4 taint flow constraints
			Set<String> srcComps = srcCompsMap.get(idx);
			Set<String> sinkComps = sinkCompsMap.get(idx);
			for (String comp0 : srcComps) {
				for (String comp1 : sinkComps) {
					for (String src : sources) {
						for (String sink : sinks) {
							Pent<Integer, String, String, String, String> pent = new Pent<>(idx, comp0, src, comp1,
									sink);
							// exist in the current taint flow?
							boolean flag = false;
							for (Quad<String, String, String, String> quad : s.getTaintFlows()) {
								if (quad.val0.equals(comp0) && quad.val1.equals(src) && quad.val2.equals(comp1)
										&& quad.val3.equals(sink)) {
									flag = true;
									break;
								}
							}
							if(!taintMap_vars.containsKey(pent))
								continue;
							
							assert taintMap_vars.containsKey(pent);
							Variable taintVar = taintMap_vars.get(pent);

							List<Variable> constraint = new ArrayList<>();
							constraint.add(taintVar);

							if (flag)
								problem_constraints
										.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
							else
								problem_constraints
										.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.INIT));
						}
					}
				}
			}
		}
		
		// type constraints for signature
		for (String comp : components) {
			for (int type = 1; type <= 3; type++) {
				// activity
				Trio<Integer, String, Integer> trio = new Trio<>(0, comp, type);
				assert typeMap_vars.containsKey(trio);
				Variable tv = typeMap_vars.get(trio);
				List<Variable> constraint = new ArrayList<>();
				constraint.add(tv);
				boolean flag = false;
				if ((activities.contains(comp) && (type == 1))
						|| (services.contains(comp) && (type == 2))
						|| (receivers.contains(comp) && (type == 3)))
					flag = true;

				if (flag)
					problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 1, EncodingType.INIT));
				else
					problem_constraints.add(new Constraint(constraint, ConstraintType.EQ, 0, EncodingType.INIT));

			}
		}

		/// 2. One to one mapping.
		for (Sample s : samples) {
			// Sample ID
			int idx = samples.indexOf(s) + 1;
			
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);
			
			// Construct the constraints
			//
			//  embedVar[x, w] + embedVar[x, w'] \le 1
			//
			// for all x \in Signature, w != w' \in Sample[i], i \ge 0
			//
			for (String sig : components) {
				for (String comp0 : comps) {
					for (String comp1 : comps) {
						if (comp0.equals(comp1)) {
							continue;
						}
						
						// embedVar[x, w]
						Trio<Integer, String, String> trio0 = new Trio<>(idx, sig, comp0);
						EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);
						
						// embedVar[x, w']
						Trio<Integer, String, String> trio1 = new Trio<>(idx, sig, comp1);
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
			
			// Construct the constraints
			//
			//  embedVar[x, w] + embedVar[x', w] \le 1
			//
			// for all x != x' \in Signature, w \in Sample[i], i \ge 0
			//
			for (String sig0 : components) {
				for (String sig1 : components) {
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
		}

		/// 3. Type constraints
		
		for (Sample s : samples) {
			// Sample ID
			int idx = samples.indexOf(s) + 1;
			
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);
			
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
			for (String sig : components) {
				for (String comp : comps) {
					for (int type = 1; type <= 3; type++) {
						// embedVar[x, w]
						Trio<Integer, String, String> trioEmbed = new Trio<>(idx, sig, comp);
						EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);							
						
						// typeVar[x, t]
						Trio<Integer, String, Integer> trioTypeSig = new Trio<>(0, sig, type);
						TypeVar tvSig = (TypeVar) typeMap_vars.get(trioTypeSig);
						
						// typeVar[w, t]
						Trio<Integer, String, Integer> trioType = new Trio<>(idx, comp, type);
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
							problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 1));
						}
					}
				}
			}
		}

		/// 4. Hard constraints for edges: if there is an edge in the signature, then there must be such isomorphic edge in all samples.		
		for (Sample s : samples) {
			// Sample ID
			int idx = samples.indexOf(s) + 1;

			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);

			// Construct the constraints
			//
			// -embedVar[x, w] - embedVar[x', w'] - edgeVar[x, x'] + edgeVar[w, w'] \ge -2
			for (String sig0 : components) {
				for (String comp0 : comps) {
					for (String sig1 : components) {
						for (String comp1 : comps) {
							// embedVar[x, w]
							Trio<Integer, String, String> trio0 = new Trio<>(idx, sig0, comp0);
							EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);

							// embedVar[x', w']
							Trio<Integer, String, String> trio1 = new Trio<>(idx, sig1, comp1);
							EmbedVar ev1 = (EmbedVar) embedMap_vars.get(trio1);

							// edgeVar[x, x']
							Trio<Integer, String, String> trioSig = new Trio<>(0, sig0, sig1);
							EdgeVar edgeVarSig = (EdgeVar) edgeMap_vars.get(trioSig);

							// edgeVar[w, w']
							Trio<Integer, String, String> trio = new Trio<>(idx, comp0, comp1);
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

							problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.GEQ, -2));
						}
					}
				}
			}
		}
		
		/// 5. Node should be embedded or not
		for (Sample s : samples) {
			// Sample id
			int idx = samples.indexOf(s) + 1;
			
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);
			
			// Construct the constraints
			//
			//  isEmbedVar[x] = \sum_w embedVar[x, w]
			//
			// for all x \in Signature, w \in Sample[i], i \ge 0
			//
			
			for (String sig : components) {
				List<Variable> literals = new ArrayList<>();
				List<Integer> coefficients = new ArrayList<>();
				
				// isEmbedVar[x]
				Pair<Integer, String> pair = new Pair<>(0, sig);
				IsEmbedVar iev = (IsEmbedVar) isEmbedMap_vars.get(pair);
				
				literals.add(iev);
				coefficients.add(1);
				
				for (String comp : comps) {
					// embedVar[x, w]
					Trio<Integer, String, String> trio = new Trio<>(idx, sig, comp);
					EmbedVar ev = (EmbedVar) embedMap_vars.get(trio);
					
					literals.add(ev);
					coefficients.add(-1);
				}
				
				problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.EQ, 0));
			}
		}
		
		/// 6. Non-embeded signature nodes should not have edges
		
		for (String sig0 : components) {
			for (String sig1 : components) {
				// Construct the constraints
				//
				//  edgeVar[x, x'] \leq isEmbedVar[x]
				//  edgeVar[x, x'] \leq isEmbedVar[x']
				//
				
				// edgeVar[x, x']
				Trio<Integer, String, String> trioSig = new Trio<>(0, sig0, sig1);
				EdgeVar edgeVarSig = (EdgeVar) edgeMap_vars.get(trioSig);

				// isEmbedVar[x]
				Pair<Integer, String> pair0 = new Pair<>(0, sig0);
				IsEmbedVar iev0 = (IsEmbedVar) isEmbedMap_vars.get(pair0);

				// isEmbedVar[x']
				Pair<Integer, String> pair1 = new Pair<>(0, sig1);
				IsEmbedVar iev1 = (IsEmbedVar) isEmbedMap_vars.get(pair1);
				
				for (int i=0; i<=1; i++) {
					// Build constraints
					List<Variable> literals = new ArrayList<>();
					literals.add(edgeVarSig);
					literals.add(i == 0 ? iev0 : iev1);
					
					List<Integer> coefficients = new ArrayList<>();
					coefficients.add(1);
					coefficients.add(-1);
				
					problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 0));
				}
			}
		}

		/// 7. Intent filters
		
		for (Sample s : samples) {
			int idx = samples.indexOf(s) + 1;
			
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);

			// Construct the constraints
			//
			//  embedVar[x, w] + filter[x, f] \le 1 + filter[w, f]
			//
			// for every x \in Signature, w \in Sample[i], i \ge 0
			//
			for (String sig : components) {
				for (String comp : comps) {
					for (String filter : allFilters) {
						// embedVar[x, w]
						Trio<Integer, String, String> trioEmbed = new Trio<>(idx, sig, comp);
						EmbedVar ev = (EmbedVar) embedMap_vars.get(trioEmbed);
						
						// filter[x, f]
						Trio<Integer, String, String> trioSig = new Trio<>(0, sig, filter);
						Variable iftSig = intentFilterMap_vars.get(trioSig);
						
						// filter[w, f]
						Trio<Integer, String, String> trioSample = new Trio<>(idx, comp, filter);
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
						
						problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 1));
					}
				}
			}
		}
		
		// Construct the constraints
		//
		//  filter[x, f] \le isEmbedVar[x]
		//
		// for every x \in Signature
		//
		for (String sig : components) {
			for (String filter : allFilters) {
				// filter[x, f]
				Trio<Integer, String, String> trioSig = new Trio<>(0, sig, filter);
				Variable iftSig = intentFilterMap_vars.get(trioSig);
				
				// isEmbedVar[x]
				Pair<Integer, String> pair = new Pair<>(0, sig);
				IsEmbedVar ev = (IsEmbedVar) isEmbedMap_vars.get(pair);
				
				// Build constraints
				List<Variable> literals = new ArrayList<>();
				literals.add(iftSig);
				literals.add(ev);
				
				List<Integer> coefficients = new ArrayList<>();
				coefficients.add(1);
				coefficients.add(-1);
				
				problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 0));
			}
		}
		
		/// 8. Taint flows
		
		for (Sample s : samples) {
			int idx = samples.indexOf(s) + 1;
			
			assert compSetMap.containsKey(idx);
			Set<String> comps = compSetMap.get(idx);
			
			// Construct the constraints
			//
			//  flow[x, x', embedVar[x, w] + embedVar[x', w'] + flow[x, x', src, sink] \le 2 + flow[w, w']
			//
			// for every x, x' \in Signature, w, w' \in Sample[i], i \ge 0
			//
			
			Map<String, Integer> compTypeMap = new HashMap<>();
			for(String t : s.getActivities()) {
				compTypeMap.put(t, 1);
			}
			
			for(String t : s.getServices()) {
				compTypeMap.put(t, 2);
			}
			
			for(String t : s.getReceivers()) {
				compTypeMap.put(t, 3);
			}
			
			Set<String> srcComps = srcCompsMap.get(idx);
			Set<String> sinkComps = sinkCompsMap.get(idx);
			for (String sig0 : components) {
				for (String sig1 : components) {
					for (String comp0 : srcComps) {
						for (String comp1 : sinkComps) {
							for (String src : sources) {
								for (String sink : sinks) {
									// embedVar[x, w]
									Trio<Integer, String, String> trio0 = new Trio<>(idx, sig0, comp0);
									EmbedVar ev0 = (EmbedVar) embedMap_vars.get(trio0);
									
									// embedVar[x', w']
									Trio<Integer, String, String> trio1 = new Trio<>(idx, sig1, comp1);
									EmbedVar ev1 = (EmbedVar) embedMap_vars.get(trio1);
									
									// flow[x, x', src, sink]
									//exist or not?
									int srcSigType = sigTypeMap.get(sig0);
									int sinkSigType = sigTypeMap.get(sig1);
									if (!compFlowsMap.containsKey(srcSigType)
											|| !compFlowsMap.containsKey(sinkSigType)
											|| !compFlowsMap.get(srcSigType)
													.contains(src)
											|| !compFlowsMap.get(sinkSigType)
													.contains(sink))
										continue;
									
									Pent<Integer, String, String, String, String> pentSig = new Pent<>(0, sig0, src, sig1, sink);
									Variable taintVarSig = taintMap_vars.get(pentSig);
									
									// flow[w, w', src, sink]
									//exist or not?
									int srcType = compTypeMap.get(comp0);
									int sinkType = compTypeMap.get(comp1);
									if (!compFlowsMap.containsKey(srcType)
											|| !compFlowsMap.containsKey(sinkType)
											|| !compFlowsMap.get(srcType)
													.contains(src)
											|| !compFlowsMap.get(sinkType)
													.contains(sink))
										continue;
									
									Pent<Integer, String, String, String, String> pent = new Pent<>(idx, comp0, src, comp1,sink);
									Variable taintVar = taintMap_vars.get(pent);

									// Build constraints
									List<Variable> literals = new ArrayList<>();
									literals.add(ev0);
									literals.add(ev1);
									literals.add(taintVarSig);
									literals.add(taintVar);
									
									List<Integer> coefficients = new ArrayList<>();
									coefficients.add(1);
									coefficients.add(1);
									coefficients.add(1);
									coefficients.add(-1);
									
									problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 2));
								}
							}
						}
					}
				}
			}
		}

		// Construct the constraints
		//
		//  flow[x, x', src, sink] \le isEmbedVar[x]
		//  flow[x, x', src, sink] \le isEmbedVar[x']
		//
		// for every x, x' \in Signature
		//
		for (String sig0 : components) {
			for (String sig1 : components) {
				for (String src : sources) {
					for (String sink : sinks) {
						// isEmbedVar[x, w]
						Pair<Integer, String> pair0 = new Pair<>(0, sig0);
						IsEmbedVar iev0 = (IsEmbedVar) isEmbedMap_vars.get(pair0);
						
						// isEmbedVar[x', w']
						Pair<Integer, String> pair1 = new Pair<>(0, sig1);
						IsEmbedVar iev1 = (IsEmbedVar) isEmbedMap_vars.get(pair1);
						
						// flow[x, x', src, sink]
						int srcSigType = sigTypeMap.get(sig0);
						int sinkSigType = sigTypeMap.get(sig1);
						if (!compFlowsMap.containsKey(srcSigType)
								|| !compFlowsMap.containsKey(sinkSigType)
								|| !compFlowsMap.get(srcSigType)
										.contains(src)
								|| !compFlowsMap.get(sinkSigType)
										.contains(sink))
							continue;
						Pent<Integer, String, String, String, String> pentSig = new Pent<>(0, sig0, src, sig1, sink);
						Variable taintVarSig = taintMap_vars.get(pentSig);
						
						// Build constraints
						for (int i = 0; i < 2; i++) {
							List<Variable> literals = new ArrayList<>();
							literals.add(taintVarSig);
							literals.add(i == 0 ? iev0 : iev1);
							
							List<Integer> coefficients = new ArrayList<>();
							coefficients.add(1);
							coefficients.add(-1);
							
							problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.LEQ, 0));	
						}
					}
				}
			}
		}
	}

	public void createObjectiveFunctions() {
		// For now, add a constraint
		//
		//  \sum_x isEmbedVar[x] \ge 4
		//
//		{
//			List<Variable> literals = new ArrayList<>();
//			List<Integer> coefficients = new ArrayList<>();
//			for (String sig : components) {
//				// isEmbedVar[x]
//				Pair<Integer, String> pair = new Pair<>(0, sig);
//				IsEmbedVar iev = (IsEmbedVar) isEmbedMap_vars.get(pair);
//				
//				// build the constraint
//				literals.add(iev);
//				coefficients.add(1);
//			}
//			problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.GEQ, 4));
//		}
		// The objective is
		//
		//  \max \sum_{v,v'} \sum_d metadata[v, v', d]
		//
		{
			List<Variable> literals = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();

			for (String sig : components) {
				for (String filter : allFilters) {
					// filter[x, f]
					Trio<Integer, String, String> trioSig = new Trio<>(0, sig, filter);
					Variable iftSig = intentFilterMap_vars.get(trioSig);
					literals.add(iftSig);
					coefficients.add(-1);
				}
			}
			Constraint objective_function = new Constraint(literals, coefficients, ConstraintType.GEQ, 0);
			//put the most important objective function at first!
			objective_functions.add(objective_function);
			
			
			List<Variable> literalsFlow = new ArrayList<>();
			List<Integer> coefficientsFlow = new ArrayList<>();
			for (String sig0 : components) {
				for (String sig1 : components) {
					for (String src : sources) {
						for (String sink : sinks) {
							// flow[x, x', src, sink]
							int srcSigType = sigTypeMap.get(sig0);
							int sinkSigType = sigTypeMap.get(sig1);
							if (!compFlowsMap.containsKey(srcSigType)
									|| !compFlowsMap.containsKey(sinkSigType)
									|| !compFlowsMap.get(srcSigType)
											.contains(src)
									|| !compFlowsMap.get(sinkSigType)
											.contains(sink))
								continue;
							Pent<Integer, String, String, String, String> pentSig = new Pent<>(0, sig0, src, sig1,
									sink);
							Variable taintVarSig = taintMap_vars.get(pentSig);
							literalsFlow.add(taintVarSig);
							coefficientsFlow.add(-1);
						}
					}
				}
			}
			Constraint objective_function_flow = new Constraint(literalsFlow, coefficientsFlow, ConstraintType.GEQ, 0);
			objective_functions.add(objective_function_flow);
			
			/// Turning Osbert's original Edge isomorphic as soft constraint.
			List<Variable> literalsEdges = new ArrayList<>();
			List<Integer> coefficientsEdges = new ArrayList<>();

			for(Variable var : edgeMap_vars.values() ) {
				EdgeVar ev = (EdgeVar) var;
				if(ev.getIndex() == 0) {
					literalsEdges.add(ev);
					coefficientsEdges.add(-1);
				}
			}
			Constraint objective_functionEdges = new Constraint(literalsEdges, coefficientsEdges, ConstraintType.GEQ, 0);
			objective_functions.add(objective_functionEdges);
		}
	}
	
	public void createInferenceConstraints() {
		if(!isInference) {
			return;
		}
		
		// 1. Embedding constraints
		{
			// num embedded vertices
			int numEmbed = compSetMap.get(1).size();
			
			// constraint
			List<Variable> literals = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();
			for (String sig : components) {
				// isEmbedVar[x]
				Pair<Integer, String> pair = new Pair<>(0, sig);
				IsEmbedVar iev = (IsEmbedVar) isEmbedMap_vars.get(pair);
				
				// build the constraint
				literals.add(iev);
				coefficients.add(1);
			}
			problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.EQ, numEmbed));
		}
		
		// 2. Taint flow constraints
		{
			// num taint flow
			int numTaintFlows = samples.get(0).getTaintFlows().size();
			
			// constraint
			List<Variable> literals = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();
			
			for (String sig0 : components) {
				for (String sig1 : components) {
					for (String src : sources) {
						for (String sink : sinks) {
							
							// flow[x, x', src, sink]
							int srcSigType = sigTypeMap.get(sig0);
							int sinkSigType = sigTypeMap.get(sig1);
							
							if (!compFlowsMap.containsKey(srcSigType)
									|| !compFlowsMap.containsKey(sinkSigType)
									|| !compFlowsMap.get(srcSigType).contains(src)
									|| !compFlowsMap.get(sinkSigType).contains(sink)) {
								continue;
							}
							
							Pent<Integer, String, String, String, String> pentSig = new Pent<>(0, sig0, src, sig1, sink);
							Variable taintVarSig = taintMap_vars.get(pentSig);
							
							literals.add(taintVarSig);
							coefficients.add(1);
						}
					}
				}
			}
			if(numTaintFlows > literals.size()) numTaintFlows = literals.size();
			
			problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.EQ, numTaintFlows));
		}
		
		// 3. Intent filter constraints
		{
			// num intent filters
			int numIntentFilters = samples.get(0).getIntentFilters().size();
			
			// constraint
			List<Variable> literals = new ArrayList<>();
			List<Integer> coefficients = new ArrayList<>();
			
			for (String sig : components) {
				for (String filter : allFilters) {
					
					// filter[x, f]
					Trio<Integer, String, String> trioSig = new Trio<>(0, sig, filter);
					Variable iftSig = intentFilterMap_vars.get(trioSig);
					
					literals.add(iftSig);
					coefficients.add(1);
				}
			}

			problem_constraints.add(new Constraint(literals, coefficients, ConstraintType.EQ, numIntentFilters));
		}
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
			blockedGraph.add(var);
			System.out.println("blocked var: " + var);
		}
		
		//Intent filter.
		for(int i = 0; i < list_ift_vars.size(); i++) {
			IntentFilterVar var = list_ift_vars.get(i);
			
			if(var.getIndex() != 0)
				continue;
			
			if (!solver.getModel().get(var.getSolverId()))
				continue;
			
			res.add(var.getCompSrc() + " [intentFilter] " + var.getFilter());
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
	
	public Sample getSignature(Solver solver) {
		// 1. Sample representing signature
		Sample sample = new Sample();
		
		List<String> activities = new ArrayList<>();
		List<String> services = new ArrayList<>();
		List<String> receivers = new ArrayList<>();
		for(IsEmbedVar var : list_is_embed_vars) {
			if(var.getIndex() != 0) {
				continue;
			}
			if(!solver.getModel().get(var.getSolverId())) {
				continue;
			}
			String vertex = var.getMapSig();
			if(this.activities.contains(vertex)) {
				activities.add(vertex);
			} else if(this.services.contains(vertex)) {
				services.add(vertex);
			} else if(this.receivers.contains(vertex)) {
				receivers.add(vertex);
			} else {
				throw new RuntimeException();
			}
		}
		sample.setActivities(activities);
		sample.setServices(services);
		sample.setReceivers(receivers);
		
		// 3. Set ICCG
		List<Pair<String,String>> iccg = new ArrayList<>();
		for(EdgeVar var : list_edge_vars) {
			if(var.getIndex() != 0) {
				continue;
			}
			if(!solver.getModel().get(var.getSolverId())) {
				continue;
			}
			iccg.add(new Pair<>(var.getSrc(), var.getTgt()));
		}
		sample.setIccg(iccg);
		
		// 4. Set taint flows
		List<Quad<String,String,String,String>> taintFlows = new ArrayList<>();
		for(Map.Entry<Pent<Integer,String,String,String,String>,Variable> entry : taintMap_vars.entrySet()) {
			TaintFlowVar var = (TaintFlowVar)entry.getValue();
			if(var.getIndex() != 0) {
				continue;
			}
			if(!solver.getModel().get(var.getSolverId())) {
				continue;
			}
			Pent<Integer,String,String,String,String> pent = entry.getKey();
			taintFlows.add(new Quad<String,String,String,String>(pent.val1, pent.val2, pent.val3, pent.val4));
		}
		sample.setTaintFlows(taintFlows);
		
		// 5. Set intent filters
		List<Pair<String,String>> intentFilters = new ArrayList<>();
		for(Map.Entry<Trio<Integer,String,String>,Variable> entry : intentFilterMap_vars.entrySet()) {
			IntentFilterVar var = (IntentFilterVar)entry.getValue();
			if(var.getIndex() != 0) {
				continue;
			}
			if(!solver.getModel().get(var.getSolverId())) {
				continue;
			}
			Trio<Integer,String,String> trio = entry.getKey();
			intentFilters.add(new Pair<String,String>(trio.val1, trio.val2));
		}
		sample.setIntentFilters(intentFilters);
		
		return sample;
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

		// FIXME!!!
		// System.out.println("list_fn_vars: " + list_fn_vars.size());
		//
		// for (EdgeVar fv : list_fn_vars) {
		// System.out.println(fv.toString());
		// }

		System.out.println("list_place_vars: " + list_type_vars.size());

		for (TypeVar pv : list_type_vars) {
			System.out.println(pv.toString());
		}

	}
	
	public void printFormula(){
		
	}

	public void reset() {
		problem_constraints.clear();
		conflict_constraints.clear();
		list_variables.clear();

		objective_values.clear();
		objective_id = 0;
	}
}