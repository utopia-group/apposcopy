package com.apposcopy.synthesis.sat4j;

import java.util.List;

import com.apposcopy.model.Sample;

public interface Encoding {
		
	abstract public void build();
	abstract public boolean isConnected();
	abstract List<String> saveModel(Solver solver, String solution);
	abstract Sample getSignature(Solver solver);
	abstract List<Variable> getBlockedGraph();
	abstract void printFormula();
		
}
