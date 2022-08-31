package org.processmining.lpmsupportedwords.parameters;

import org.deckfour.xes.model.XLog;

public class ClusterOnPrechartParameters {

	private int prechartLength;

	public ClusterOnPrechartParameters(XLog log) {
		setPrechartLength(1);
	}
	
	public ClusterOnPrechartParameters(int prechartLength) {
		setPrechartLength(prechartLength);
	}

	public int getPrechartLength() {
		return prechartLength;
	}

	public void setPrechartLength(int minPrechartLength) {
		this.prechartLength = minPrechartLength;
	}

	public boolean equals(Object object) {
		if (object instanceof ClusterOnPrechartParameters) {
			ClusterOnPrechartParameters parameters = (ClusterOnPrechartParameters) object;
			return (prechartLength == parameters.prechartLength);
		}
		return false;
	}

	public int hashCode() {
		return prechartLength;
	}
}
