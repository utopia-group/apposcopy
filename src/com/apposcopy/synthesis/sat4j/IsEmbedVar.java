package com.apposcopy.synthesis.sat4j;

/**
 * Each IsEmbedVar denote a isEmbed(i,x): x is the component of the 
 * signature.
 * i is the index of the sample.
 * 
 * @author obastani
 *
 */
public class IsEmbedVar extends Variable {
	private String mapSig;

	public String getMapSig() {
		return mapSig;
	}

	public void setMapSig(String mapSig) {
		this.mapSig = mapSig;
	}

	public IsEmbedVar(int id, int i, String sig) {
		super(id, i);
		mapSig = sig;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(index).append(" val:").append(mapSig)
				.append(" solverId:" + (solverId + 1));
		return sb.toString();
	}

}
