package org.processmining.lpmsupportedwords.plugins;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetArrayFactory;
import org.processmining.acceptingpetrinetminer.algorithms.DiscoverAcceptingPetriNetArrayFromEventLogArrayAlgorithm;
import org.processmining.acceptingpetrinetminer.parameters.DiscoverAcceptingPetriNetArrayFromEventLogArrayParameters;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.hub.ProMResourceManager;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.models.EventLogArray;
import org.processmining.log.models.impl.EventLogArrayFactory;
import org.processmining.lpmsupportedwords.algorithms.ClusterOnPrechartAlgorithm;
import org.processmining.lpmsupportedwords.algorithms.MineSupportedWordsAlgorithm;
import org.processmining.lpmsupportedwords.algorithms.WordFilteringAlgorithm;
import org.processmining.lpmsupportedwords.dialogs.MineSupportedWordsDialog;
import org.processmining.lpmsupportedwords.help.GenerateContextRichLPMsHelp;
import org.processmining.lpmsupportedwords.help.Me;
import org.processmining.lpmsupportedwords.parameters.ClusterOnPrechartParameters;
import org.processmining.lpmsupportedwords.parameters.MineSupportedWordsParameters;
import org.processmining.lpmsupportedwords.parameters.WordFilteringParameters;

@Plugin(name = "Generate Context-Rich LPMs", parameterLabels = { "Event Log", "Parameters" }, returnLabels = {
		"Accepting Petri Net Array" }, returnTypes = {
				AcceptingPetriNetArray.class }, help = GenerateContextRichLPMsHelp.TEXT)
public class GenerateContextRichLPMsPlugin {

	@UITopiaVariant(affiliation = Me.affiliation, author = Me.name, email = Me.email)
	@PluginVariant(variantLabel = "Generate Context-Rich LPMs, dialog", requiredParameterLabels = { 0 })
	public AcceptingPetriNetArray mineDialog(UIPluginContext context, XLog log) {
		MineSupportedWordsParameters parameters = new MineSupportedWordsParameters(log);
		MineSupportedWordsDialog dialog = new MineSupportedWordsDialog(log, parameters);
		InteractionResult result = context.showWizard("MineSupportedWordsDialog", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		XLog words = MineSupportedWordsAlgorithm.mineSupportedWords(context, log, parameters);

		ClusterOnPrechartParameters cpparameters = new ClusterOnPrechartParameters(1);
		EventLogArray array = ClusterOnPrechartAlgorithm.clusterOnPrechart(context, words, cpparameters);

		WordFilteringParameters wfparameters = new WordFilteringParameters(parameters.getSupportThreshold(),
				parameters.getClassifier());
		EventLogArray logs = EventLogArrayFactory.createEventLogArray();
		for (int i = 0; i < array.getSize(); i++) {
			words = array.getLog(i);
			logs.addLog(WordFilteringAlgorithm.filterWords(words, wfparameters));
		}

		DiscoverAcceptingPetriNetArrayFromEventLogArrayParameters params = new DiscoverAcceptingPetriNetArrayFromEventLogArrayParameters(
				logs);
		params.setClassifier(parameters.getClassifier());
		params.setMiner("Inductive Miner (Perfect Fitness)");
		AcceptingPetriNetArray nets = new DiscoverAcceptingPetriNetArrayFromEventLogArrayAlgorithm().apply(context,
				logs, params);

		AcceptingPetriNetArray out = AcceptingPetriNetArrayFactory.createAcceptingPetriNetArray();
		for (int i = 0; i < nets.getSize(); i++) {
			if (!nets.getNet(i).getNet().getTransitions().isEmpty()) {
				out.addNet(nets.getNet(i));
			}
		}

		context.getProvidedObjectManager().createProvidedObject("Logs", logs, EventLogArray.class, context);
		if (context != null) {
			ProMResourceManager.initialize(null).getResourceForInstance(logs).setFavorite(true);
		}

		return out;
	}

}
