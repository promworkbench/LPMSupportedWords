package org.processmining.lpmsupportedwords.dialogs;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.lpmsupportedwords.parameters.MineSupportedWordsParameters;

import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class MineSupportedWordsDialog extends JPanel {

	/**
	 * Eclipse complained.
	 */
	private static final long serialVersionUID = -2392771140197721327L;

	/**
	 * Provides the user with a slider to select the support threshold.
	 * 
	 * @param log
	 * @param parameters
	 */
	public MineSupportedWordsDialog(XLog log, final MineSupportedWordsParameters parameters) {
		// TODO Add classifier selection

		// Create table layout of two rows
		double size[][] = { { TableLayoutConstants.FILL }, { 30, TableLayoutConstants.FILL } };
		setLayout(new TableLayout(size));

		// Add title to the top row
		add(SlickerFactory.instance().createLabel("<html><h2>Select support treshold</h2>"), "0, 0");

		// And create a nice slider
		XLogInfo info = XLogInfoFactory.createLogInfo(log, parameters.getClassifier());
		Map<XEventClass, Integer> counter = new HashMap<>();
		for (XEventClass eventClass : info.getEventClasses().getClasses()) {
			counter.put(eventClass, 0);
		}
		for (XTrace trace : log) {
			for (XEvent event : trace) {
				XEventClass key = info.getEventClasses().getClassOf(event);
				counter.put(key, counter.get(key) + 1);
			}
		}

		int max = 1;
		for (Integer fre : counter.values()) {
			if (fre > max) {
				max = fre;
			}
		}

		final NiceSlider slider = SlickerFactory.instance().createNiceIntegerSlider("Select support threshold", 1, max,
				Math.max(1, max / 10), Orientation.HORIZONTAL);
		// Update the parameters when a user clicks on things
		ChangeListener listener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				parameters.setSupportThreshold((slider.getSlider().getValue()));
			}
		};
		slider.addChangeListener(listener);
		listener.stateChanged(null);
		add(slider, "0, 1");
	}
}
