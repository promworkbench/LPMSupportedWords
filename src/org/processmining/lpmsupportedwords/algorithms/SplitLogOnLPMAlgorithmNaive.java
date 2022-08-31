package org.processmining.lpmsupportedwords.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class SplitLogOnLPMAlgorithmNaive implements SplitLogOnLPMAlgorithmInterface {
	/**
	 * Splits a log into traces that start with the first event of the given
	 * Petri net and end with the final event(s).
	 * 
	 * Expects all initial and final markings to consist of a single place each.
	 * Expects only a single visible transition from the initial marking.
	 * Expects only a single visible transition to each final marking.
	 * 
	 * @param apn
	 * @param log
	 * @param classifier
	 * @return
	 */
	public XLog split(AcceptingPetriNet apn, XLog log, XEventClassifier classifier) {
		// find initial transition
		Place initialPlace = apn.getInitialMarking().iterator().next();
		PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> initialEdge = apn.getNet()
				.getOutEdges(initialPlace).iterator().next();
		String start = initialEdge.getTarget().getLabel();

		// find final transition(s)
		Set<String> stop = new HashSet<>();
		for (Marking finalMarking : apn.getFinalMarkings()) {
			Place finalPlace = finalMarking.iterator().next();
			PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> finalEdge = apn.getNet().getInEdges(finalPlace)
					.iterator().next();
			stop.add(finalEdge.getSource().getLabel());
		}

		// get all transitions
		Set<String> trans = new HashSet<>();
		for (Transition t : apn.getNet().getTransitions()) {
			trans.add(t.getLabel());
		}

		// initialize output log
		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog newLog = factory.createLog();
		XConceptExtension.instance().assignName(newLog, "Trace fragments from " + start + " to " + stop.toString());

		for (XTrace trace : log) {
			String traceName = XConceptExtension.instance().extractName(trace);
			if (traceName == null) {
				traceName = "";
			}
			traceName += SPLIT_TRACENAME;

			boolean recording = false;
			XTrace newTrace = null;
			List<Integer> indices = null;
			for (int i = 0; i < trace.size(); i++) {
				String event = classifier.getClassIdentity(trace.get(i));
				if (start.equals(event)) {
					recording = true;
					newTrace = factory.createTrace();
					indices = new ArrayList<>();
					newLog.add(newTrace);
				}
				if (recording && trans.contains(event)) {
					newTrace.add(trace.get(i));
					indices.add(i);
					XConceptExtension.instance().assignName(newTrace, traceName + indices.toString());
				}
				if (recording && stop.contains(event)) {
					recording = false;
				}
			}
		}
		return newLog;
	}
}
