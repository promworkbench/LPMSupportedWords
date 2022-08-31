package org.processmining.lpmsupportedwords.parameters;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;

public class GenerateHTMLCoverageReportParameters {
	private XEventClassifier classifier;

	public GenerateHTMLCoverageReportParameters(XLog log) {
		if (log.getClassifiers().size() > 0) {
			setClassifier(log.getClassifiers().get(0));
		} else {
			System.out.println(this.getClass().getName()
					+ " found no classifier upon construction. Using default Name+Lifecycle.");
			setClassifier(new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier()));
		}
	}
	
	public GenerateHTMLCoverageReportParameters(XEventClassifier classifier) {
		setClassifier(classifier);
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
		if (object instanceof GenerateHTMLCoverageReportParameters) {
			GenerateHTMLCoverageReportParameters parameters = (GenerateHTMLCoverageReportParameters) object;
			return (classifier.equals(parameters.classifier));
		}
		return false;
	}

	public int hashCode() {
		return classifier.hashCode();
	}
}
