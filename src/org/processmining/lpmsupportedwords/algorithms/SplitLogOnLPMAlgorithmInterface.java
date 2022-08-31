package org.processmining.lpmsupportedwords.algorithms;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;

public interface SplitLogOnLPMAlgorithmInterface {
	public static final String SPLIT_TRACENAME = " - Subtrace from ";

	public XLog split(AcceptingPetriNet apn, XLog log, XEventClassifier classifier);
}
