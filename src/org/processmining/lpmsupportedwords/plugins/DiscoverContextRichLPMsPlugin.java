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
import org.processmining.lpmsupportedwords.dialogs.DiscoverContextRichLPMsDialog;
import org.processmining.lpmsupportedwords.help.DiscoverContextRichLPMsHelp;
import org.processmining.lpmsupportedwords.help.Me;
import org.processmining.lpmsupportedwords.parameters.DiscoverContextRichLPMsParameters;

@Plugin(name = "Discover Context-Rich LPMs", parameterLabels = { "Event Log", "Parameters" }, returnLabels = {
		"Accepting Petri Net Array" }, returnTypes = {
				AcceptingPetriNetArray.class }, help = DiscoverContextRichLPMsHelp.TEXT)
public class DiscoverContextRichLPMsPlugin {

	@UITopiaVariant(affiliation = Me.affiliation, author = Me.name, email = Me.email)
	@PluginVariant(variantLabel = "Discover Context-Rich LPMs, dialog", requiredParameterLabels = { 0 })
	public AcceptingPetriNetArray mineDialog(UIPluginContext context, XLog log) {
		DiscoverContextRichLPMsParameters parameters = new DiscoverContextRichLPMsParameters(log);
		
		DiscoverContextRichLPMsDialog dialog = new DiscoverContextRichLPMsDialog(parameters);
		
		InteractionResult result = context.showWizard("Discover Context-Rich LPMs Parameters", true, true, dialog);
		if (result != InteractionResult.FINISHED) {
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		XLog words = MineSupportedWordsAlgorithm.mineSupportedWords(context, log, parameters);

		EventLogArray array = ClusterOnPrechartAlgorithm.clusterOnPrechart(context, words, parameters);

		EventLogArray logs = EventLogArrayFactory.createEventLogArray();
		for (int i = 0; i < array.getSize(); i++) {
			words = array.getLog(i);
			logs.addLog(WordFilteringAlgorithm.filterWords(words, parameters));
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
