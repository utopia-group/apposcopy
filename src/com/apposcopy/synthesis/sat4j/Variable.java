package com.apposcopy.synthesis.sat4j;

public abstract class Variable {

	protected int solverId;

	protected int index;

	public Variable(int id, int i) {
		solverId = id;
		index = i;
	}

	public int getSolverId() {
		return solverId;
	}

	public void setSolverId(int solverId) {
		this.solverId = solverId;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int i) {
		this.index = i;
	}
}
