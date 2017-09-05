package com.apposcopy.synthesis.sat4j;

/**
 * Each EdgeVar denote a edge(i,s,t): s is the source and t is the target. i is
 * the index.
 * 
 * @author yufeng
 *
 */
public class EdgeVar extends Variable {

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getTgt() {
		return tgt;
	}

	public void setTgt(String tgt) {
		this.tgt = tgt;
	}

	private String src;

	private String tgt;
	
	public EdgeVar(int id, int i, String s, String t) {
		super(id, i);
		src = s;
		tgt = t;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(src).append(" tgt:").append(tgt).append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
