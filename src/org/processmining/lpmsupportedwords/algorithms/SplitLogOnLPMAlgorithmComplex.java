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

public class SplitLogOnLPMAlgorithmComplex implements SplitLogOnLPMAlgorithmInterface {
	/**
	 * Splits a log into traces that start with the first event (or combination
	 * if starting with an invisible parallel split) of the given Petri net and
	 * end with the final event(s).
	 * 
	 * @param apn
	 * @param log
	 * @param classifier
	 * @return
	 */
	public XLog split(AcceptingPetriNet apn, XLog log, XEventClassifier classifier) {
		Set<String> start = new HashSet<>();
		for (Place place : apn.getInitialMarking()) {
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : apn.getNet().getOutEdges(place)) {
				if (edge.getTarget().getLabel().substring(0, 3).equals("tau")) {
					// tau split, follow out edges to find places, then follow their out edges to find transitions
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge2 : apn.getNet()
							.getOutEdges(edge.getTarget())) {
						for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge3 : apn.getNet()
								.getOutEdges(edge2.getTarget())) {
							start.add(edge3.getTarget().getLabel());
						}
					}
				} else {
					start.add(edge.getTarget().getLabel());
				}
			}
		}

		Set<String> stop = new HashSet<>();
		for (Marking m : apn.getFinalMarkings()) {
			for (Place place : m) {
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : apn.getNet()
						.getInEdges(place)) {
					stop.add(edge.getSource().getLabel());
				}
			}
		}

		Set<String> trans = new HashSet<>();
		for (Transition t : apn.getNet().getTransitions()) {
			trans.add(t.getLabel());
		}

		XFactory factory = XFactoryRegistry.instance().currentDefault();
		XLog newLog = factory.createLog();
		XConceptExtension.instance().assignName(newLog,
				"Trace fragments from " + start.toString() + " to " + stop.toString());

		for (XTrace trace : log) {
			String traceName = XConceptExtension.instance().extractName(trace);
			if (traceName == null) {
				traceName = "";
			}
			traceName += SPLIT_TRACENAME;

			int i = 0;
			while (i < trace.size()) {
				// find match for first activity
				while (i < trace.size() && !start.contains(classifier.getClassIdentity(trace.get(i)))) {
					i++;
				}
				// trace.get(i) is of type startName or i is out of bounds
				if (i >= trace.size()) {
					break;
				}
				//found a match, start building new trace
				XTrace newTrace = factory.createTrace();
				newTrace.add(trace.get(i));
				List<Integer> indices = new ArrayList<>();
				indices.add(i);
				i++;
				while (i < trace.size()) {
					String event = classifier.getClassIdentity(trace.get(i));
					if (start.contains(event)
							&& (event.equals(classifier.getClassIdentity(newTrace.get(0))) || indices.size() > 1)) {
						break; //don't add this event, we need to start a new trace.
					}
					if (trans.contains(event)) { //only add the event if it occurs in the model
						newTrace.add(trace.get(i));
						indices.add(i);
					}
					i++;
					if (stop.contains(event)) {
						break; //trace is complete, search for new start
					}
				}
				XConceptExtension.instance().assignName(newTrace, traceName + indices.toString());
				newLog.add(newTrace);
			}

		}
		return newLog;
	}
}
