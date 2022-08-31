package org.processmining.lpmsupportedwords.algorithms;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;
import org.processmining.lpmsupportedwords.models.PNRepResultArray;
import org.processmining.lpmsupportedwords.models.ShortLog;
import org.processmining.lpmsupportedwords.parameters.GenerateHTMLCoverageReportParameters;
import org.processmining.lpmsupportedwords.plugins.HTMLExportPlugin;
import org.processmining.modelrepair.plugins.align.PNLogReplayer;
import org.processmining.modelrepair.plugins.align.PNLogReplayer.ReplayParams;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.precision.algorithms.EscapingEdgesPrecisionAlgorithm;
import org.processmining.precision.models.EscapingEdgesPrecisionResult;
import org.processmining.precision.parameters.EscapingEdgesPrecisionParameters;

public class GenerateHTMLCoverageReportAlgorithm {
	public static final SplitLogOnLPMAlgorithmInterface ALG = new SplitLogOnLPMAlgorithmNaive();

	public static Object[] evaluate(UIPluginContext context, AcceptingPetriNetArray models, XLog log,
			GenerateHTMLCoverageReportParameters parameters) {

		context.getProgress().setMaximum(1 + models.getSize() * 2);

		System.out.println("Calculating trace variants...");

		// create hashmap (str -> lst)
		// for trace in log
		// key: hash of trace
		// value: add trace id to (if required: new empty) list
		Map<String, List<Integer>> traceVariants = new HashMap<>();
		ShortLog sLog = new ShortLog(log, parameters.getClassifier());
		short[][] shortLog = sLog.getLog();
		for (int i = 0; i < shortLog.length; i++) {
			String t = Arrays.toString(shortLog[i]);
			if (!traceVariants.containsKey(t)) {
				traceVariants.put(t, new ArrayList<Integer>());
			}
			traceVariants.get(t).add(new Integer(i));
		}

		// create new map (int -> int)
		// for entry in hashmap
		// key: trace id of some trace of this variant
		// value: multiplicity of this variant
		Map<Integer, Integer> useVariants = new HashMap<>();
		for (List<Integer> list : traceVariants.values()) {
			useVariants.put(list.get(0), list.size());
		}
		// trace id -> do I need to output this (true iff in keyset), if so, what is the multiplicity (value of key)

		context.getProgress().inc();
		System.out.println("Trace variants calculated, splitting logs...");

		// prepare matching split logs for each model
		EventLogArray splitLogs = EventLogArrayFactory.createEventLogArray();
		for (int i = 0; i < models.getSize(); i++) {
			splitLogs.addLog(ALG.split(models.getNet(i), log, parameters.getClassifier()));
			context.getProgress().inc();
		}

		System.out.println("Split logs done, calculating alignments...");

		// generate alignments
		
		PNRepResultArray alignments = new PNRepResultArray();
		for (int i = 0; i < models.getSize(); i++) {
			Petrinet net = models.getNet(i).getNet();
			// construct a default final marking (final places of the net marked) for a given net
			// and define a connection between net and final marking
			PNLogReplayer.constructFinalMarking(context, net);

			// retrieve initial marking (defined by the discover algorithm) and final markings of
			// a model via their connections
			Marking initMarking = models.getNet(i).getInitialMarking();
			Marking finalMarking = models.getNet(i).getFinalMarkings().iterator().next();

			// align a log to a net for a given initial and final marking and given event classifier
			XEventClassifier classifier = parameters.getClassifier();
			XLog splitLog = splitLogs.getLog(i);
			if (splitLog.isEmpty()) {
				System.out.println("log " + i + " is empty; check if your transition labels match your event classes");
				context.getFutureResult(0).cancel(true);
				return null;
			}
			TransEvClassMapping map = PNLogReplayer.getEventClassMapping(context, net, splitLog, classifier);
			ReplayParams params = getReplayerParameters(context, net, initMarking, finalMarking, splitLog, classifier);
			Set<Marking> finalMarkings = models.getNet(i).getFinalMarkings();
			params.parameters.setFinalMarkings(finalMarkings.toArray(new Marking[finalMarking.size()]));
			PNRepResult alignment = PNLogReplayer.callReplayer(context, net, splitLog, map, params);
			alignments.add(alignment);

			System.out.println("alignment " + i + " done");
			context.getProgress().inc();
		}

		System.out.println("Alignments calculated, applying measures...");

		// apply measures
		boolean[][][] coverage = new boolean[models.getSize()][][];
		double[] fitness = new double[models.getSize()];
		double[] precision = new double[models.getSize()];
		int[] support = new int[models.getSize()];
		double[] confidence = new double[models.getSize()];
		double[] determinism = new double[models.getSize()];

		for (int i = 0; i < models.getSize(); i++) {
			coverage[i] = getCoverage(alignments.get(i), splitLogs.getLog(i), log);

			System.out.println("coverage " + i + " calculated");
			context.getProgress().inc();

			fitness[i] = (double) alignments.get(i).getInfo().get(PNRepResult.TRACEFITNESS);

			precision[i] = precision(context, alignments.get(i), models.getNet(i));

			support[i] = splitLogs.getLog(i).size();

			confidence[i] = confidence(coverage[i], sLog, models.getNet(i));

			determinism[i] = determinism(models.getNet(i));
		}

		System.out.println("Measures applied, generating HTML...");

		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\r\n");
		html.append("<html>\r\n");
		html.append("	<head>\r\n");
		html.append("		<meta charset=\"utf-8\"/>\r\n");
		html.append("		<title>Evaluation results</title>\r\n");
		html.append("		<style>\r\n");
		//include style.css
		appendResourceToStringBuilder("/style.css", html);
		html.append("		</style>\r\n");
		html.append("		<script type=\"application/javascript\">\r\n");

		html.append("var stats = {\r\n");
		html.append("	'double_fitness':" + Arrays.toString(fitness) + ",\r\n");
		html.append("	'double_precision':" + Arrays.toString(precision) + ",\r\n");
		html.append("	'int_support':" + Arrays.toString(support) + ",\r\n");
		html.append("	'double_confidence':" + Arrays.toString(confidence) + ",\r\n");
		html.append("	'double_determinism':" + Arrays.toString(determinism) + "\r\n");
		html.append("}\r\n\r\n");
		html.append("function coverageData() {\r\n");
		html.append("	return [\r\n");

		while (!useVariants.isEmpty()) {
			int key = -1, value = -1;
			for (Map.Entry<Integer, Integer> entry : useVariants.entrySet()) {
				if (entry.getValue() > value) {
					key = entry.getKey();
					value = entry.getValue();
				}
			}
			useVariants.remove(key);

			html.append("		{'multiplicity':" + value + ",\r\n");
			html.append("			'coverage':[\r\n");
			for (int i = 0; i < models.getSize(); i++) {
				boolean[] trace = coverage[i][key];
				html.append("				");
				html.append(Arrays.toString(trace).replace("true", "1").replace("false", "0"));
				if (i + 1 < models.getSize()) {
					html.append(",\r\n");
				} else {
					html.append("\r\n");
				}
			}
			html.append("			]\r\n");
			if (useVariants.isEmpty()) {
				html.append("		}\r\n");
			} else {
				html.append("		},\r\n");
			}
		}

		html.append("	];\r\n");
		html.append("}\r\n\r\n");

		//include script.js
		appendResourceToStringBuilder("/script.js", html);
		html.append("		</script>\r\n");
		html.append("	</head>\r\n");
		html.append("	<body onload=\"load();\">\r\n");
		html.append("		<table id=\"coverage\">\r\n");
		html.append("			<tr>\r\n");
		html.append("			</tr>\r\n");
		html.append("		</table>\r\n");
		html.append("		<div id=\"canvases\">\r\n");
		html.append("		</div>\r\n");
		html.append("	</body>\r\n");
		html.append("</html>\r\n");

		try {
			File dir = new File(System.getProperty("java.io.tmpdir"));
			File file = File.createTempFile("evaluationReport", ".html", dir);
			new HTMLExportPlugin().export(context, html.toString(), file);
			Desktop.getDesktop().open(file);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new Object[] { alignments, html.toString(), splitLogs };
	}

	public static ReplayParams getReplayerParameters(PluginContext context, PetrinetGraph net, Marking m_initial, Marking m_final, XLog log, XEventClassifier classifier) {
		
		ReplayParams rParams = new ReplayParams();
		
		rParams.selectedAlg = new PetrinetReplayerWithoutILP();
		List<XEventClass> eventClasses = PNLogReplayer.getEventClasses(log, classifier);
		rParams.mapEvClass2Cost = new HashMap<XEventClass, Integer>();
		for (XEventClass cl : eventClasses) {
			rParams.mapEvClass2Cost.put(cl, 1);
		}

		rParams.mapTrans2Cost = new HashMap<Transition, Integer>();
		for (Transition t : net.getTransitions()) {
			if (t.isInvisible()) rParams.mapTrans2Cost.put(t, 0);
			else rParams.mapTrans2Cost.put(t, 1);
		}
		
		rParams.parameters = new CostBasedCompleteParam(rParams.mapEvClass2Cost, rParams.mapTrans2Cost);
		rParams.parameters.setMaxNumOfStates(200000);
		rParams.parameters.setInitialMarking(m_initial);
		rParams.parameters.setFinalMarkings(m_final);
		rParams.parameters.setCreateConn(true);
		
		return rParams;
	}
	
	private static double determinism(AcceptingPetriNet net) {
		Collection<Place> places = net.getNet().getPlaces();
		int numplaces = 0;
		double sum = 0.0;
		for (Place p : places) {
			if (net.getNet().getOutEdges(p).size() > 0) {
				sum += 1.0 / net.getNet().getOutEdges(p).size();
				numplaces++;
			}
		}
		if (numplaces < 1) {
			return -1.0;
		}
		return sum / numplaces;
	}

	protected static double precision(UIPluginContext context, PNRepResult pnRepResult, AcceptingPetriNet net) {
		EscapingEdgesPrecisionAlgorithm alg = new EscapingEdgesPrecisionAlgorithm();
		EscapingEdgesPrecisionParameters precisionParams = new EscapingEdgesPrecisionParameters(pnRepResult, net);
		try {
			EscapingEdgesPrecisionResult result = alg.apply(context, pnRepResult, net, precisionParams);
			return result.getPrecision();
		} catch (IllegalTransitionException e) {
			e.printStackTrace();
			return -1.0;
		}
	}

	private static double confidence(boolean[][] coverage, ShortLog sLog, AcceptingPetriNet apn) {
		short[][] log = sLog.getLog();
		int[] counts = sLog.getEventCounts();

		int[] coveredCounts = new int[counts.length];
		for (int i = 0; i < coverage.length; i++) {
			for (int j = 0; j < coverage[i].length; j++) {
				if (coverage[i][j]) {
					coveredCounts[log[i][j]]++;
				}
			}
		}

		double sum = 0.0;
		int count = 0;
		Iterator<Transition> it = apn.getNet().getTransitions().iterator();
		while (it.hasNext()) {
			short id = sLog.getEventId(it.next().getLabel());
			if (0 <= id && id < counts.length) {
				if (counts[id] == 0) {
					System.out.println("problem?");
				} else if (coveredCounts[id] == 0) {
					System.out.println("problem.");
				} else {
					sum += (double) counts[id] / (double) coveredCounts[id];
				}
				count++;
			}
		}

		if (sum == 0) {
			sum = -1;
		}
		return count / sum;
	}

	/**
	 * Adds the contents of the given resource (some filename) to the given
	 * StringBuilder.
	 * 
	 * @param resource
	 * @param sb
	 */
	public static void appendResourceToStringBuilder(String resource, StringBuilder sb) {
		try (InputStream is = GenerateHTMLCoverageReportAlgorithm.class.getResourceAsStream(resource);
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);) {
			while (br.ready()) {
				sb.append(br.readLine() + "\r\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts a replay result based on a split log into an array of boolean
	 * arrays indicating coverage of the whole log.
	 * 
	 * @param replayResult
	 * @param splitLog
	 * @param log
	 * @return boolean[trace][event] = (trace, event) in log is covered by the
	 *         model used in replayResult
	 */
	public static boolean[][] getCoverage(PNRepResult replayResult, XLog splitLog, XLog log) {

		// prepare boolean projection of originalLog to record coverage
		boolean[][] coverage = new boolean[log.size()][];
		for (int i = 0; i < log.size(); i++) {
			coverage[i] = new boolean[log.get(i).size()];
		}

		// prepare index lookup to find trace index from trace name in originalLog
		Map<String, Integer> traceNameToIndex = new HashMap<>();
		for (int i = 0; i < log.size(); i++) {
			String name = XConceptExtension.instance().extractName(log.get(i));
			traceNameToIndex.put(name, i);
		}

		for (SyncReplayResult result : replayResult) {
			boolean[] resultCoverage = projectCoverageOnLog(result);
			for (int index : result.getTraceIndex()) {
				String name = XConceptExtension.instance().extractName(splitLog.get(index));
				// System.out.println(name);
				String[] position = name.split(ALG.SPLIT_TRACENAME);
				int traceIndex = traceNameToIndex.get(position[0]);
				String[] target = position[1].substring(1, position[1].length()-1).split(", ");
				for (int i = 0; i < resultCoverage.length; i++) {
					coverage[traceIndex][Integer.parseInt(target[i])] = resultCoverage[i];
//					int from = Integer.parseInt(position[1].substring(1, position[1].length()-1).split(",")[0]);
//					System.arraycopy(resultCoverage, 0, coverage[traceIndex], from, resultCoverage.length);
				}
			}
		}

		return coverage;
	}

	/**
	 * Turns a given replay result into a boolean array.
	 * 
	 * @param result
	 * @return
	 */
	private static boolean[] projectCoverageOnLog(SyncReplayResult result) {
		List<Boolean> list = new ArrayList<>();
		for (StepTypes stepType : result.getStepTypes()) {
			if (stepType.equals(StepTypes.LMGOOD)) {
				list.add(new Boolean(true));
			} else if (stepType.equals(StepTypes.L)) {
				list.add(new Boolean(false));
			}
		}
		boolean[] out = new boolean[list.size()];
		for (int i = 0; i < list.size(); i++) {
			out[i] = list.get(i);
		}
		return out;
	}


}
