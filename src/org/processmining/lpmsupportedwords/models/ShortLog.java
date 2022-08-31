package org.processmining.lpmsupportedwords.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class ShortLog {
	private final short[][] shortLog;
	private final XEventClassifier classifier;
	private final Map<String, Short> eventToShort;
	private final List<XEvent> shortToEvent;

	/**
	 * Transforms a given XLog into an array of arrays of shorts based on the
	 * given classifier, and builds the necessary data structures to translate
	 * events between the ShortLog and XLog formats.
	 * 
	 * @param log
	 * @param classifier
	 */
	public ShortLog(XLog log, XEventClassifier classifier) {
		this.classifier = classifier;
		eventToShort = new HashMap<>();
		shortToEvent = new ArrayList<>();
		short key = 0;
		shortLog = new short[log.size()][];
		for (int i = 0; i < log.size(); i++) {
			XTrace trace = log.get(i);
			shortLog[i] = new short[trace.size()];
			for (int j = 0; j < trace.size(); j++) {
				String eventClassId = classifier.getClassIdentity(trace.get(j));
				if (eventToShort.containsKey(eventClassId)) {
					shortLog[i][j] = eventToShort.get(eventClassId);
				} else {
					shortLog[i][j] = key;
					shortToEvent.add(minEvent(trace.get(j), classifier));
					eventToShort.put(eventClassId, key++);
				}
			}
		}
	}

	/**
	 * Reduces an event to the bare minimum to be identified as the equal with
	 * the given classifier.
	 * 
	 * @param event
	 * @param classifier
	 * @return
	 */
	private static XEvent minEvent(XEvent event, XEventClassifier classifier) {
		XEvent newEvent = (XEvent) event.clone();
		Set<String> attributeKeys = event.getAttributes().keySet();
		for (String attributeKey : attributeKeys) {
			boolean isKeyRedundant = true;
			for (String classifierKey : classifier.getDefiningAttributeKeys()) {
				if (attributeKey.equals(classifierKey)) {
					isKeyRedundant = false;
					break;
				}
			}
			if (isKeyRedundant) {
				newEvent.getAttributes().remove(attributeKey);
			}
		}
		
		return newEvent;
	}

	/**
	 * Allows construction of a ShortLog with guaranteed the same classifier and
	 * legend (translations between ShortLog and XLog) as its twin.
	 * 
	 * @param log
	 * @param twin
	 */
	public ShortLog(XLog log, ShortLog twin) {
		this.classifier = twin.classifier;
		this.eventToShort = twin.eventToShort;
		this.shortToEvent = twin.shortToEvent;
		shortLog = new short[log.size()][];
		short key = (short) shortToEvent.size();
		for (int i = 0; i < log.size(); i++) {
			XTrace trace = log.get(i);
			shortLog[i] = new short[trace.size()];
			for (int j = 0; j < trace.size(); j++) {
				String eventClassId = classifier.getClassIdentity(trace.get(j));
				if (eventToShort.containsKey(eventClassId)) {
					shortLog[i][j] = eventToShort.get(eventClassId);
				} else {
					shortLog[i][j] = key;
					shortToEvent.add(minEvent(trace.get(j), classifier));
					eventToShort.put(eventClassId, key++);
				}
			}
		}
	}

	/**
	 * Returns an array where array[i] is the number of times i occurs in the
	 * log.
	 * 
	 * @return
	 */
	public int[] getEventCounts() {
		int[] eventCounter = new int[shortToEvent.size()];
		for (short[] trace : shortLog) {
			for (short event : trace) {
				eventCounter[event]++;
			}
		}
		return eventCounter;
	}

	/**
	 * Returns a copy of the short-based log. shortLog[tradeIndex][eventIndex]
	 * on this refers to the short that represents the event class at
	 * originalLog.get(traceIndex).get(eventIndex) in the original log.
	 * 
	 * @return
	 */
	public short[][] getLog() {
		return shortLog.clone();
	}

	/**
	 * The classifier used to build this ShortLog.
	 * 
	 * @return
	 */
	public XEventClassifier getClassifier() {
		return classifier;
	}

	/**
	 * Returns an array of XEvents that are represented in the ShortLog by the
	 * index used here.
	 * 
	 * @return
	 */
	public XEvent[] getEventArray() {
		XEvent[] array = new XEvent[0];
		return shortToEvent.toArray(array);
	}

	/**
	 * Returns the short used for a particular event in this ShortLog.
	 * 
	 * @param label
	 * @return
	 */
	public short getEventId(String label) {
		if (eventToShort.containsKey(label)) {
			return eventToShort.get(label);
		} else {
			return -1;
		}
	}
}
