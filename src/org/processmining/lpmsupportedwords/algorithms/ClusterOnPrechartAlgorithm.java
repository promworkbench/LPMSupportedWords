package org.processmining.lpmsupportedwords.algorithms;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;
import org.processmining.lpmsupportedwords.parameters.ClusterOnPrechartParameters;

public class ClusterOnPrechartAlgorithm {

	/**
	 * Sorts a given log into sublogs that each contain all traces with the same
	 * prechart of length specified in the parameters.
	 * 
	 * @param context
	 * @param log
	 * @param parameters
	 * @return
	 */
	public static EventLogArray clusterOnPrechart(PluginContext context, XLog log,
			ClusterOnPrechartParameters parameters) {
		String logName = XConceptExtension.instance().extractName(log);
		if (logName == null) {
			logName = "Event Log";
		}
		logName += " - Sublog ";

		EventLogArray logs = EventLogArrayFactory.createEventLogArray();
		XFactory factory = XFactoryRegistry.instance().currentDefault();

		for (XTrace trace : log) {
			if (trace.size() < parameters.getPrechartLength()) {
				continue;
			}
			boolean added = false;
			for (int i = 0; i < logs.getSize(); i++) {
				XLog sublog = logs.getLog(i);
				boolean match = true;
				for (int j = 0; j < parameters.getPrechartLength(); j++) {
					if (!sublog.get(0).get(j).equals(trace.get(j))) {
						match = false;
						break;
					}
				}
				if (match) {
					sublog.add(trace);
					added = true;
					break;
				}
			}
			if (!added) {
				XLog newLog = factory.createLog();
				newLog.getClassifiers().addAll(log.getClassifiers());
				XConceptExtension.instance().assignName(newLog, logName + (logs.getSize()));
				newLog.add(trace);
				logs.addLog(newLog);
			}
		}

		return logs;
	}
}
