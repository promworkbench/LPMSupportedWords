package org.processmining.lpmsupportedwords.plugins;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.lpmsupportedwords.models.PNRepResultArray;
import org.processmining.plugins.pnalignanalysis.visualization.projection.PNLogReplayProjectedVis;

@Plugin(name = "@0 Visualize PNRepResult Array", returnLabels = { "Visualized PNRepResult Array" }, returnTypes = {
		JComponent.class }, parameterLabels = { "PNRepResult Array" }, userAccessible = true)
@Visualizer
public class VisualizePNRepResultArrayPlugin {
	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualize(UIPluginContext context, PNRepResultArray nets) {

		PNLogReplayProjectedVis visualizer = new PNLogReplayProjectedVis();

		JTabbedPane tabbedPane = new JTabbedPane();
		
		System.out.println("Apologies for the list of IllegalComponentStateExceptions...");
		for (int index = 0; index < nets.size(); index++) {
			String label = "Net " + (index + 1);
			try {
				tabbedPane.add(label, visualizer.visualize(context, nets.get(index)));
			} catch (ConnectionCannotBeObtained e) {
				e.printStackTrace();
				tabbedPane.add(label, new Component() {

					/**
					 * 
					 */
					private static final long serialVersionUID = 4439145656155474550L;
				});
			}
		}
		return tabbedPane;
	}

}
