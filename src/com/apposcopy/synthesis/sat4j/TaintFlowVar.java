package com.apposcopy.synthesis.sat4j;

/**
 * Each EmbedVar denote a taint(i,s,sType,sink, sinkType): s is the source
 * component and t is the target component. i is the index. sType and sinkType
 * are the flow types respectively.
 * 
 * @author yufeng
 *
 */
public class TaintFlowVar extends Variable {

	private String compSrc;

	private String compTgt;

	private String srcType;

	private String sinkType;

	public String getCompSrc() {
		return compSrc;
	}

	public void setCompSrc(String compSrc) {
		this.compSrc = compSrc;
	}

	public String getCompTgt() {
		return compTgt;
	}

	public void setCompTgt(String compTgt) {
		this.compTgt = compTgt;
	}

	public String getSrcType() {
		return srcType;
	}

	public void setSrcType(String srcType) {
		this.srcType = srcType;
	}

	public String getSinkType() {
		return sinkType;
	}

	public void setSinkType(String sinkType) {
		this.sinkType = sinkType;
	}

	public TaintFlowVar(int id, int i, String s, String src, String t, String sink) {
		super(id, i);
		compSrc = s;
		compTgt = t;
		srcType = src;
		sinkType = sink;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(compSrc).append(" source:").append(srcType).append(" tgt:")
				.append(compTgt).append(" sink:").append(sinkType).append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
