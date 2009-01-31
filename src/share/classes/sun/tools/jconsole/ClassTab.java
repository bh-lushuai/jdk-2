/*
 * Copyright 2004-2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package sun.tools.jconsole;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.management.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import sun.awt.*;

import static sun.tools.jconsole.Formatter.*;
import static sun.tools.jconsole.Resources.*;
import static sun.tools.jconsole.Utilities.*;


@SuppressWarnings("serial")
class ClassTab extends Tab implements ActionListener {
    PlotterPanel loadedClassesMeter;
    TimeComboBox timeComboBox;
    private JCheckBox verboseCheckBox;
    private HTMLPane details;
    private ClassOverviewPanel overviewPanel;
    private boolean plotterListening = false;

    private static final String loadedPlotterKey        = "loaded";
    private static final String totalLoadedPlotterKey   = "totalLoaded";
    private static final String loadedPlotterName       = Resources.getText("Loaded");
    private static final String totalLoadedPlotterName  = Resources.getText("Total Loaded");
    private static final Color  loadedPlotterColor      = Plotter.defaultColor;
    private static final Color  totalLoadedPlotterColor = Color.red;

    private static final String infoLabelFormat = "ClassTab.infoLabelFormat";

    /*
      Hierarchy of panels and layouts for this tab:

	ClassTab (BorderLayout)

	    North:  topPanel (BorderLayout)

			Center: controlPanel (FlowLayout)
				    timeComboBox

			East:   topRightPanel (FlowLayout)
				    verboseCheckBox

	    Center: plotterPanel (BorderLayout)

			Center: plotter

	    South:  bottomPanel (BorderLayout)

			Center: details
    */

    public static String getTabName() {
	return Resources.getText("Classes");
    }

    public ClassTab(VMPanel vmPanel) {
	super(vmPanel, getTabName());

	setLayout(new BorderLayout(0, 0));
	setBorder(new EmptyBorder(4, 4, 3, 4));

	JPanel topPanel     = new JPanel(new BorderLayout());
	JPanel plotterPanel = new JPanel(new BorderLayout());
	JPanel bottomPanel  = new JPanel(new BorderLayout());

	add(topPanel,     BorderLayout.NORTH);
	add(plotterPanel, BorderLayout.CENTER);
	add(bottomPanel,  BorderLayout.SOUTH);

	JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
	topPanel.add(controlPanel, BorderLayout.CENTER);

	verboseCheckBox = new JCheckBox(Resources.getText("Verbose Output"));
	verboseCheckBox.addActionListener(this);
	verboseCheckBox.setToolTipText(getText("Verbose Output.toolTip"));
	JPanel topRightPanel = new JPanel();
	topRightPanel.setBorder(new EmptyBorder(0, 65-8, 0, 70));
	topRightPanel.add(verboseCheckBox);
	topPanel.add(topRightPanel, BorderLayout.AFTER_LINE_ENDS);

	loadedClassesMeter = new PlotterPanel(Resources.getText("Number of Loaded Classes"),
					      Plotter.Unit.NONE, false);
	loadedClassesMeter.plotter.createSequence(loadedPlotterKey,
						  loadedPlotterName,
						  loadedPlotterColor,
						  true);
	loadedClassesMeter.plotter.createSequence(totalLoadedPlotterKey,
						  totalLoadedPlotterName,
						  totalLoadedPlotterColor,
						  true);
	setAccessibleName(loadedClassesMeter.plotter,
			  getText("ClassTab.loadedClassesPlotter.accessibleName"));
	plotterPanel.add(loadedClassesMeter);

	timeComboBox = new TimeComboBox(loadedClassesMeter.plotter);
	controlPanel.add(new LabeledComponent(Resources.getText("Time Range:"),
					      getMnemonicInt("Time Range:"),
					      timeComboBox));

	LabeledComponent.layout(plotterPanel);

	bottomPanel.setBorder(new CompoundBorder(new TitledBorder(Resources.getText("Details")),
						  new EmptyBorder(10, 10, 10, 10)));

	details = new HTMLPane();
	setAccessibleName(details, getText("Details"));
	JScrollPane scrollPane = new JScrollPane(details);
	scrollPane.setPreferredSize(new Dimension(0, 150));
	bottomPanel.add(scrollPane, BorderLayout.SOUTH);
	
    }

    public void actionPerformed(ActionEvent ev) {
	final boolean b = verboseCheckBox.isSelected();
	workerAdd(new Runnable() {
	    public void run() {
		ProxyClient proxyClient = vmPanel.getProxyClient();
		try {
		    proxyClient.getClassLoadingMXBean().setVerbose(b);
		} catch (UndeclaredThrowableException e) {
		    proxyClient.markAsDead();
		} catch (IOException ex) {
		    // Ignore
		}
	    }
	});
    }


    public SwingWorker<?, ?> newSwingWorker() {
        final ProxyClient proxyClient = vmPanel.getProxyClient();

	if (!plotterListening) {
	    proxyClient.addWeakPropertyChangeListener(loadedClassesMeter.plotter);
	    plotterListening = true;
	}

	return new SwingWorker<Boolean, Object>() {
	    private long clCount, cuCount, ctCount;
	    private boolean isVerbose;
	    private String detailsStr;
	    private long timeStamp;

	    public Boolean doInBackground() {
		try {
		    ClassLoadingMXBean classLoadingMBean = proxyClient.getClassLoadingMXBean();

		    clCount = classLoadingMBean.getLoadedClassCount();
		    cuCount = classLoadingMBean.getUnloadedClassCount();
		    ctCount = classLoadingMBean.getTotalLoadedClassCount();
		    isVerbose = classLoadingMBean.isVerbose();
		    detailsStr = formatDetails();
		    timeStamp = System.currentTimeMillis();

		    return true;
		} catch (UndeclaredThrowableException e) {	
		    proxyClient.markAsDead();
		    return false;
		} catch (IOException e) {	
		    return false;
		}
	    }

	    protected void done() {
		try {
		    if (get()) {
			loadedClassesMeter.plotter.addValues(timeStamp, clCount, ctCount);

			if (overviewPanel != null) {
			    overviewPanel.updateClassInfo(ctCount, clCount);
			    overviewPanel.getPlotter().addValues(timeStamp, clCount);
			}

			loadedClassesMeter.setValueLabel(clCount + "");
			verboseCheckBox.setSelected(isVerbose);
			details.setText(detailsStr);
		    }
		} catch (InterruptedException ex) {
		} catch (ExecutionException ex) {
		    if (JConsole.isDebug()) {
			ex.printStackTrace();
		    }
		}
	    }

	    private String formatDetails() {
		String text = "<table cellspacing=0 cellpadding=0>";

		long time = System.currentTimeMillis();
		String timeStamp = formatDateTime(time);
		text += newRow(Resources.getText("Time"), timeStamp);
		text += newRow(Resources.getText("Current classes loaded"), justify(clCount, 5));
		text += newRow(Resources.getText("Total classes loaded"),   justify(ctCount, 5));
		text += newRow(Resources.getText("Total classes unloaded"), justify(cuCount, 5));

		return text;
	    }
	};
    }


    OverviewPanel[] getOverviewPanels() {
	if (overviewPanel == null) {
	    overviewPanel = new ClassOverviewPanel();
	}
	return new OverviewPanel[] { overviewPanel };
    }

    private static class ClassOverviewPanel extends OverviewPanel {
	ClassOverviewPanel() {
	    super(getText("Classes"), loadedPlotterKey, loadedPlotterName, null);
	}

	private void updateClassInfo(long total, long loaded) {
	    long unloaded = (total - loaded);
	    getInfoLabel().setText(getText(infoLabelFormat, loaded, unloaded, total));
	}
    }
}
