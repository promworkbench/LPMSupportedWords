package org.processmining.lpmsupportedwords.dialogs;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.log.dialogs.ClassifierPanel;
import org.processmining.log.parameters.UpdateParameter;
import org.processmining.lpmsupportedwords.parameters.DiscoverContextRichLPMsParameters;

import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class DiscoverContextRichLPMsDialog extends JPanel implements UpdateParameter {
	final NiceSlider thresholdSlider;
	final NiceSlider deltaSlider;
	final NiceSlider prechartSlider;
	final DiscoverContextRichLPMsParameters parameters;

	/**
	 * Eclipse complained.
	 */
	private static final long serialVersionUID = -2392771140197721327L;

	/**
	 * Provides the user with options to select the parameters for Discover
	 * Context-Rich LPMs.
	 * 
	 * @param parameters
	 */
	public DiscoverContextRichLPMsDialog(final DiscoverContextRichLPMsParameters parameters) {
		this.parameters = parameters;

		// Create table layout
		double size[][] = { { 200, TableLayoutConstants.FILL }, { TableLayoutConstants.FILL, 30, 30, 30 } };
		setLayout(new TableLayout(size));

		// Add classifier selector
		ClassifierPanel cPanel = new ClassifierPanel(parameters.getLog().getClassifiers(), parameters, this);
		add(cPanel, "1, 0");

		// And create nice sliders
		add(SlickerFactory.instance().createLabel("Select support threshold"), "0, 1");
		add(SlickerFactory.instance().createLabel("Select support delta"), "0, 2");
		add(SlickerFactory.instance().createLabel("Select prechart length"), "0, 3");
		
		thresholdSlider = SlickerFactory.instance().createNiceIntegerSlider("", 1, parameters.getSupportMax(),
				parameters.getSupportThreshold(), Orientation.HORIZONTAL);
		add(thresholdSlider, "1, 1");

		deltaSlider = SlickerFactory.instance().createNiceIntegerSlider("", 1, parameters.getSupportMax(),
				parameters.getSupportDelta(), Orientation.HORIZONTAL);
		add(deltaSlider, "1, 2");

		prechartSlider = SlickerFactory.instance().createNiceIntegerSlider("", 1, parameters.getPrechartMax(),
				parameters.getPrechartLength(), Orientation.HORIZONTAL);
		add(prechartSlider, "1, 3");
		
		// Update the parameters when a user clicks on things
		thresholdSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				parameters.setSupportThreshold(thresholdSlider.getSlider().getValue());
				deltaSlider.getSlider().setValue(parameters.getSupportDelta());
			}
		});

		deltaSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				parameters.setSupportDelta(deltaSlider.getSlider().getValue());
			}
		});

		prechartSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				parameters.setPrechartLength(prechartSlider.getSlider().getValue());
			}
		});
	}

	public void update() {
		int t = parameters.getSupportThreshold();
		int d = parameters.getSupportDelta();
		int n = parameters.getPrechartLength();
		thresholdSlider.getSlider().setMaximum(parameters.getSupportMax());
		deltaSlider.getSlider().setMaximum(parameters.getSupportMax());
		prechartSlider.getSlider().setMaximum(parameters.getPrechartMax());
		thresholdSlider.getSlider().setValue(t);
		deltaSlider.getSlider().setValue(d);
		prechartSlider.getSlider().setValue(n);
	}
}
