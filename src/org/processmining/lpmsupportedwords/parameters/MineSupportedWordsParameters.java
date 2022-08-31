package org.processmining.lpmsupportedwords.parameters;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;

public class MineSupportedWordsParameters {
	private int supportThreshold;
	private XEventClassifier classifier;

	public MineSupportedWordsParameters(XLog log) {
		setSupportThreshold(10);
		if (log.getClassifiers().size() > 0) {
			setClassifier(log.getClassifiers().get(0));
		} else {
			System.out.println(this.getClass().getName()
					+ " found no classifier upon construction. Using default Name+Lifecycle.");
			setClassifier(new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier()));
		}
	}
	
	public MineSupportedWordsParameters(int supportThreshold, XEventClassifier classifier) {
		setSupportThreshold(supportThreshold);
		setClassifier(classifier);
	}

	public int getSupportThreshold() {
		return supportThreshold;
	}

	public void setSupportThreshold(int supportThreshold) {
		this.supportThreshold = supportThreshold;
	}

	public XEventClassifier getClassifier() {
		return classifier;
	}

	public void setClassifier(XEventClassifier classifier) throws IllegalArgumentException {
		if (classifier == null) {
			throw new IllegalArgumentException(this.getClass().getName() + ".setClassifier was given a null argument.");
		}
		this.classifier = classifier;
	}

	public boolean equals(Object object) {
		if (object instanceof MineSupportedWordsParameters) {
			MineSupportedWordsParameters parameters = (MineSupportedWordsParameters) object;
			return (supportThreshold == parameters.supportThreshold && classifier.equals(parameters.classifier));
		}
		return false;
	}

	public int hashCode() {
		return supportThreshold + classifier.hashCode();
	}
}
