package com.apposcopy.synthesis.sat4j;


/**
 * Each TypeVar denotes a type(i, c, t): i is the index of the sample, c is the id of 
 * of the component, and t is the type of the component (activity, service or receiver).
 * 
 * @author yufeng
 *
 */
public class TypeVar extends Variable {

	private String name;
	
	// 1: activity; 2: service; 3: receiver
	private int type;

	public TypeVar(int id, int index, String n, int t) {
		super(id, index);
		name = n;
		type = t;
	}

	public int getType() {
		return type;
	}

	public void setType(int t) {
		this.type = t;
	}

	public String getName() {
		return name;
	}

	public void setName(String n) {
		this.name = n;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append(" index:").append(index).append(" type:").append(type)
				.append(" solverId:" + (solverId+1));
		return sb.toString();
	}

}
