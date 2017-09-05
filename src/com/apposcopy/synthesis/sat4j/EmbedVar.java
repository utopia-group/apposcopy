package com.apposcopy.synthesis.sat4j;

/**
 * Each EmbedVar denote a embed(i,x,y): x is the component of the 
 * target signature and y is the component of the sample. 
 * i is the index of the sample.
 * 
 * @author yufeng
 *
 */
public class EmbedVar extends Variable {

	private String mapSample;

	private String mapSig;

	public String getMapSample() {
		return mapSample;
	}

	public void setMapSample(String mapSample) {
		this.mapSample = mapSample;
	}

	public String getMapSig() {
		return mapSig;
	}

	public void setMapSig(String mapSig) {
		this.mapSig = mapSig;
	}

	public EmbedVar(int id, int i, String sig, String sample) {
		super(id, i);
		mapSample = sample;
		mapSig = sig;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" src:").append(mapSample).append(" tgt:").append(mapSig)
				.append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
