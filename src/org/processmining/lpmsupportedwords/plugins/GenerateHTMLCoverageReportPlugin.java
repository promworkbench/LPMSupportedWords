package org.processmining.lpmsupportedwords.plugins;

import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNetArray;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.log.models.EventLogArray;
import org.processmining.lpmsupportedwords.algorithms.GenerateHTMLCoverageReportAlgorithm;
import org.processmining.lpmsupportedwords.help.GenerateHTMLCoverageReportHelp;
import org.processmining.lpmsupportedwords.help.Me;
import org.processmining.lpmsupportedwords.models.PNRepResultArray;
import org.processmining.lpmsupportedwords.parameters.GenerateHTMLCoverageReportParameters;

@Plugin(name = "Generate HTML coverage report",
		parameterLabels = { "Models", "Event Log", "Parameters" },
		returnLabels = { "PNRepResultArray", "HTML", "Event Log Array" },
		returnTypes = { PNRepResultArray.class, String.class, EventLogArray.class },
		help = GenerateHTMLCoverageReportHelp.TEXT)
public class GenerateHTMLCoverageReportPlugin extends GenerateHTMLCoverageReportAlgorithm {

	@UITopiaVariant(affiliation = Me.affiliation, author = Me.name, email = Me.email)
	@PluginVariant(variantLabel = "Generate HTML coverage report, default", requiredParameterLabels = { 0, 1 })
	public Object[] runDefault(UIPluginContext context, AcceptingPetriNetArray models, XLog log) {
		return run(context, models, log, new GenerateHTMLCoverageReportParameters(log));
	}

	public Object[] run(UIPluginContext context, AcceptingPetriNetArray models, XLog log,
			GenerateHTMLCoverageReportParameters parameters) {
		return evaluate(context, models, log, parameters);
	}

}
