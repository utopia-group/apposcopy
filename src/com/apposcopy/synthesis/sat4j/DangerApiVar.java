package com.apposcopy.synthesis.sat4j;


/**
 * Each IntentFilterVar denote a filter(i,s,t,f): s is the source component and t is
 * the target component. i is the index. f is the filter type.
 * 
 * @author yufeng
 *
 */
public class DangerApiVar extends Variable {

	public String getCompSrc() {
		return compSrc;
	}

	public void setCompSrc(String compSrc) {
		this.compSrc = compSrc;
	}

	public String getApi() {
		return api;
	}

	public void setApi(String f) {
		this.api = f;
	}

	// compSrc and compTgt will always be the same!
	private String compSrc;

	private String api;

	public DangerApiVar(int id, int i, String s, String f) {
		super(id, i);
		compSrc = s;
		api = f;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(compSrc).append(" API:").append(api)
				.append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
