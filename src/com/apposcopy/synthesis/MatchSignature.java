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

import java.util.ArrayList;
import java.util.List;

import org.sat4j.specs.ContradictionException;

import com.apposcopy.model.Sample;
import com.apposcopy.synthesis.sat4j.Encoding;
import com.apposcopy.synthesis.sat4j.MatchingEncoding;
import com.apposcopy.synthesis.sat4j.Solver;

public class MatchSignature {
	protected MatchingEncoding encoding;
	//protected SignatureEncoding encoding;
	//protected BasicEncoding basic_encoding;
	protected Solver solver;
	protected boolean OK;
	protected boolean isRun = false;
	protected boolean res;
	protected boolean basic = false;

	protected int solver_limit = 5;
	
	public MatchSignature(Sample signature, Sample sample) {
		solver = new Solver();
		
		encoding = new MatchingEncoding(signature, sample);
		encoding.build();
		
		try {
			solver.buildMatch(encoding);
			OK = true;
		} catch (ContradictionException e) {
			OK = false;
			e.printStackTrace();
//			assert false;
		}
	}

	public Encoding getEncoding() {
		return encoding;
	}
	
	public Solver getSolver() {
		return solver;
	}

	public List<String> get() {
		if(!isRun) {
			isRun = true;
			if (!OK)
				return new ArrayList<String>();
	
			res = solver.solve();
			if (!res) OK = false;
		}
		String solution = new String();
		return res ? encoding.saveModel(solver, solution) : new ArrayList<String>();
	}
	
	public void blockModel() {
		//if(!isConnected()) {
			solver.blockGraph(encoding.getBlockedGraph());
		//} 
		
//		solver.blockModel(encoding.getBlockVars());
	}
	
	public boolean isOK() {
		return OK;
	}
}
