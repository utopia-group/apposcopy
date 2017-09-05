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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import shord.project.ClassicProject;
import shord.project.analyses.JavaAnalysis;
import shord.project.analyses.ProgramRel;
import soot.SootMethod;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;

/**
 * Synthesizing signature from example.
 * 
 * @author yufeng
 *
 */

@Chord(name = "synth-java")
public class SynthSignature extends JavaAnalysis {
	static final String[] validSrcSinks = { "$getDeviceId", "$getSubscriberId",
			"$ENC/DEC", "$MODEL", "$BRAND", "$SDK", "MANUFACTURER", "$PRODUCT",
			"$getLine1Number", "$content://sms", "$getSimSerialNumber",
			"$File", "$InstalledPackages", "!INTERNET", "!ENC/DEC", "!FILE",
			"!WebView", "!PROCESS.OutputStream" };

	static final String[] specMethods = {
			"<android.content.BroadcastReceiver: void abortBroadcast()>",
			"<java.lang.Runtime: java.lang.Process exec(java.lang.String)>",
			"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String",
			"<android.telephony.SmsManager: void sendTextMessage(java.lang.String,java.lang.String,"
					+ "java.lang.String,android.app.PendingIntent,android.app.PendingIntent)>" };

	static final int PRIORITYCOST = 25;
	static final int ICCGCOST = 2;
	static final int FLOWCOST = 5;
	static final int SPECMETHCOST = 10;
	static final int IFTCOST = 20;
	
	Map<String, String> compTypeMap = new HashMap<String, String>();

	public void buildCompTypeMap() {
		ProgramRel relReceiver = (ProgramRel) ClassicProject.g().getTrgt(
				"Receiver");
		relReceiver.load();
		RelView viewReceiver = relReceiver.getView();
		Iterable<String> resReceiver = viewReceiver.getAry1ValTuples();
		for (String comp : resReceiver)
			compTypeMap.put(comp, "Receiver");

		ProgramRel relService = (ProgramRel) ClassicProject.g().getTrgt(
				"Service");
		relService.load();
		RelView viewService = relService.getView();
		Iterable<String> resService = viewService.getAry1ValTuples();
		for (String comp : resService)
			compTypeMap.put(comp, "Service");

		ProgramRel relAct = (ProgramRel) ClassicProject.g().getTrgt("Activity");
		relAct.load();
		RelView viewAct = relAct.getView();
		Iterable<String> resAct = viewAct.getAry1ValTuples();
		for (String comp : resAct)
			compTypeMap.put(comp, "Activity");
	}
	
	public void run() {
		//step 0: preparation.
		ProgramRel relFlowComp = (ProgramRel) ClassicProject.g().getTrgt(
				"SrcSinkComp");
		relFlowComp.load();
		RelView viewFlowComp = relFlowComp.getView();
		Iterable<Quad<String, String, String, String>> resFlowComp = viewFlowComp
				.getAry4ValTuples();

		ProgramRel relBackEdge = (ProgramRel) ClassicProject.g().getTrgt(
				"BackEdge");
		relBackEdge.load();
		RelView viewBackEdge = relBackEdge.getView();
		Iterable<Pair<String, String>> resBackEdge = viewBackEdge
				.getAry2ValTuples();

		ProgramRel relCIA = (ProgramRel) ClassicProject.g().getTrgt(
				"CompIntentAction");
		relCIA.load();
		RelView viewCIA = relCIA.getView();
		Iterable<Pair<String, String>> resCIA = viewCIA.getAry2ValTuples();

		ProgramRel relSpecCall = (ProgramRel) ClassicProject.g().getTrgt(
				"SpecCallerComp");
		relSpecCall.load();
		RelView viewSpecCall = relSpecCall.getView();
		Iterable<Pair<SootMethod, String>> resSpecCall = viewSpecCall
				.getAry2ValTuples();

		ProgramRel relPriority = (ProgramRel) ClassicProject.g().getTrgt(
				"Priority");
		relPriority.load();
		RelView viewPriority = relPriority.getView();
		Iterable<Pair<String, Integer>> resPriority = viewPriority
				.getAry2ValTuples();
		ProgramRel relInstallAPK = (ProgramRel) ClassicProject.g().getTrgt(
				"HasAPK");
		relInstallAPK.load();
		RelView viewInstAPK = relInstallAPK.getView();
		ProgramRel relICCG = (ProgramRel) ClassicProject.g().getTrgt(
				"ICCG");
		relICCG.load();
		RelView viewICCG = relICCG.getView();
		Iterable<Pair<String, String>> resICCG = viewICCG
				.getAry2ValTuples();
		
		buildCompTypeMap();
		
		List<String> validList = Arrays.asList(validSrcSinks);
		List<String> specList = Arrays.asList(specMethods);

		// step 1: Group taint flow based on sink components.
		HashMap<String, Set<Pair<String, String>>> compGroup = new HashMap<String, Set<Pair<String, String>>>();
		for (Quad<String, String, String, String> quad : resFlowComp) {
			String sinkComp = quad.val2;
			String src = quad.val1;
			String sink = quad.val3;
			if (!validList.contains(src) || !validList.contains(sink))
				continue;

			Set<Pair<String, String>> set = new HashSet<Pair<String, String>>();
			if (compGroup.containsKey(sinkComp)) {
				set = compGroup.get(sinkComp);
			}

			Pair<String, String> p = new Pair<String, String>(src, sink);
			set.add(p);
			compGroup.put(sinkComp, set);
		}

		// step 2: For each group, compute the sub-graph in ICCG.
		int highest = 0;
		Set<String> hotComps = new HashSet<String>();
		for (String comp : compGroup.keySet()) {
			int cost = 0;
			Set<String> comps = new HashSet<String>();
			System.out.println("-----------------");
			System.out.println("comp:" + comp);
			System.out.println("flows:" + compGroup.get(comp));
			comps.add(comp);
			cost += compGroup.get(comp).size() * FLOWCOST;
			for (Pair<String, String> backPair : resBackEdge) {
				String tgt = backPair.val0;
				String src = backPair.val1;
				if (tgt.equals(comp)) {
					comps.add(src);
					// step 3.1: Including all related filters in corresponding
					// receiver
					System.out.println("subgraph:" + src);
					for (Pair<String, String> iftPair : resCIA) {
						String recev = iftPair.val0;
						String act = iftPair.val1;
						if (recev.equals(src)) {
							System.out.println("######intent filter:" + act);
							cost += IFTCOST;
						}
					}
					// step 3.2: Including all special methods in corresponding
					// receiver
					for (Pair<SootMethod, String> specPair : resSpecCall) {
						SootMethod specM = specPair.val0;
						String specComp = specPair.val1;
						if (specComp.equals(src)
								&& specList.contains(specM.getSignature())) {
							System.out.println("######specMethod:" + specM);
							cost += SPECMETHCOST;
						}
					}

					for (Pair<String, Integer> priorityPair : resPriority) {
						String priComp = priorityPair.val0;
						if (priComp.equals(src)) {
							System.out.println("######high priority");
							cost += PRIORITYCOST;
						}
					}
					// step 5: Including all related ICCG edges
					int ratio = compTypeMap.get(src).equals("Receiver") ? 10
							: 1;
					cost += ICCGCOST * ratio;
				}
			}
			// step 6: Computing the cost for each choice and picking the
			// highest one.
			System.out.println("total cost:" + cost);
			if(cost > highest) {
				highest = cost;
				hotComps = comps;
			}
		}

		// step 7: Including all other misc features: installAPK
		boolean instAPK = viewInstAPK.size() > 0 ? true : false;

		// finally, dump the solution.
		System.out.println("Generating signature--------------");
		if (instAPK)
			System.out.println("InstallAPK(_)");
		
		for (String comp : hotComps) {
			System.out.println("#Component:" + comp);
			if (compGroup.containsKey(comp)) {
				for (Pair<String, String> flow : compGroup.get(comp))
					System.out.println("#FLOW:" + flow);
			}

			for (Pair<String, String> iftPair : resCIA) {
				String recev = iftPair.val0;
				String act = iftPair.val1;
				if (recev.equals(comp)) {
					System.out.println("######intent filter:" + act);
				}
			}
			for (Pair<SootMethod, String> specPair : resSpecCall) {
				SootMethod specM = specPair.val0;
				String specComp = specPair.val1;
				if (specComp.equals(comp)
						&& specList.contains(specM.getSignature())) {
					System.out.println("######specMethod:" + specM);
				}
			}

			for (Pair<String, Integer> priorityPair : resPriority) {
				String priComp = priorityPair.val0;
				if (priComp.equals(comp)) {
					System.out.println("######high priority");
				}
			}
		}
		
		for (Pair<String, String> iccg : resICCG) {
			String src = iccg.val0;
			String tgt = iccg.val1;
			if (hotComps.contains(src) && hotComps.contains(tgt)) {
				System.out.println("###ICCG:" + src + "-->" + tgt);
				System.out.println("###ICCG(type):" + compTypeMap.get(src)
						+ "==>" + compTypeMap.get(tgt));
			}
		}
		
	}
}
