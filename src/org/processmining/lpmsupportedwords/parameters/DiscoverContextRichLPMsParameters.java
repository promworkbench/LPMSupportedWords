package org.processmining.lpmsupportedwords.parameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.log.parameters.ClassifierParameter;

public class DiscoverContextRichLPMsParameters implements ClassifierParameter {
	private final XLog log;

	private XEventClassifier classifier;
	private int supportMax;
	private int supportThreshold;
	private int supportDelta;
	private int prechartMax;
	private int prechartLength;

	public DiscoverContextRichLPMsParameters(XLog log) {
		this.log = log;
		setClassifier(getDefaultClassifier(log));
	}

	public static XEventClassifier getDefaultClassifier(XLog log) {
		if (log.getClassifiers().size() > 0) {
			return log.getClassifiers().get(0);
		} else {
			System.out.println("DiscoverContextRichLPMsParameters found no classifier in the log."
					+ " Using default Name+Lifecycle.");
			return new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier());
		}
	}

	public XLog getLog() {
		return log;
	}

	public XEventClassifier getClassifier() {
		return classifier;
	}

	public void setClassifier(XEventClassifier classifier) throws IllegalArgumentException {
		if (classifier == null) {
			throw new IllegalArgumentException(this.getClass().getName() + ".setClassifier was given a null argument.");
		}
		this.classifier = classifier;
		updateSupportMax();
		updatePrechartMax();
	}

	public int getSupportMax() {
		return supportMax;
	}

	public void updateSupportMax() {
		XLogInfo info = XLogInfoFactory.createLogInfo(log, classifier);
		Map<XEventClass, Integer> counter = new HashMap<>();
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				counter.merge(info.getEventClasses().getClassOf(event), 1, (a, b) -> a + b);
			}
		}
		supportMax = Collections.max(counter.values());
		setSupportThreshold(supportMax / 10);
	}

	public int getSupportThreshold() {
		return supportThreshold;
	}

	public void setSupportThreshold(int supportThreshold) {
		this.supportThreshold = supportThreshold;
		setSupportDelta(supportThreshold);
	}

	public int getSupportDelta() {
		return supportDelta;
	}

	public void setSupportDelta(int supportDelta) {
		this.supportDelta = supportDelta;
	}
	
	public int getPrechartMax() {
		return prechartMax;
	}
	
	public void updatePrechartMax() {
		prechartMax = log.stream().map(XTrace::size).max(Integer::compareTo).get();
		setPrechartLength(1);
	}

	public int getPrechartLength() {
		return prechartLength;
	}

	public void setPrechartLength(int minPrechartLength) {
		this.prechartLength = minPrechartLength;
	}
}
