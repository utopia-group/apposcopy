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
