/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklist.deisotoper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.MZmineMenu;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchStep;
import net.sf.mzmine.modules.batchmode.BatchStepCategory;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskGroup;
import net.sf.mzmine.taskcontrol.TaskGroupListener;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This class implements a simple isotopic peaks grouper method based on
 * searhing for neighbouring peaks from expected locations.
 * 
 */

public class IsotopeGrouper implements BatchStep, TaskListener, ActionListener {

    private IsotopeGrouperParameters parameters;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private Desktop desktop;

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

        this.desktop = MZmineCore.getDesktop();

        parameters = new IsotopeGrouperParameters();

        desktop.addMenuItem(MZmineMenu.PEAKLISTPROCESSING,
                "Isotopic peaks grouper", "TODO write description",
                KeyEvent.VK_I, this, null);

    }

    public void setParameters(ParameterSet parameters) {
        this.parameters = (IsotopeGrouperParameters) parameters;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

        PeakList[] peaklists = desktop.getSelectedPeakLists();

        if (peaklists.length == 0) {
            desktop.displayErrorMessage("Please select peak lists to deisotope");
            return;
        }

        for (PeakList peaklist : peaklists) {
            if (peaklist.getNumberOfRawDataFiles() > 1) {
                desktop.displayErrorMessage("Peak list "
                        + peaklist
                        + " cannot be deisotoped, because it contains more than one data file");
                return;
            }
        }

        ExitCode exitCode = setupParameters(parameters);
        if (exitCode != ExitCode.OK)
            return;

        runModule(null, peaklists, parameters.clone(), null);
    }

    public void taskStarted(Task task) {
        IsotopeGrouperTask igTask = (IsotopeGrouperTask) task;
        logger.info("Running isotopic peak grouper on " + igTask.getPeakList());
    }

    public void taskFinished(Task task) {

        IsotopeGrouperTask igTask = (IsotopeGrouperTask) task;

        if (task.getStatus() == Task.TaskStatus.FINISHED) {
            logger.info("Finished isotopic peak grouper on "
                    + igTask.getPeakList());
        }

        if (task.getStatus() == Task.TaskStatus.ERROR) {
            String msg = "Error while deisotoping peaklist "
                    + igTask.getPeakList() + ": " + task.getErrorMessage();
            logger.severe(msg);
            desktop.displayErrorMessage(msg);
        }

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Isotopic peaks grouper";
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#setupParameters(net.sf.mzmine.data.ParameterSet)
     */
    public ExitCode setupParameters(ParameterSet currentParameters) {
        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for " + toString(),
                (SimpleParameterSet) currentParameters);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }

    /**
     * @see net.sf.mzmine.main.MZmineModule#getParameterSet()
     */
    public ParameterSet getParameterSet() {
        return parameters;
    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.io.RawDataFile[],
     *      net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
     *      net.sf.mzmine.taskcontrol.TaskGroupListener)
     */
    public TaskGroup runModule(RawDataFile[] dataFiles, PeakList[] peakLists,
            ParameterSet parameters, TaskGroupListener taskGroupListener) {

        // check peak lists
        if ((peakLists == null) || (peakLists.length == 0)) {
            desktop.displayErrorMessage("Please select peak lists for deisotoping");
            return null;
        }

        // prepare a new group of tasks
        Task tasks[] = new IsotopeGrouperTask[peakLists.length];
        for (int i = 0; i < peakLists.length; i++) {
            tasks[i] = new IsotopeGrouperTask(peakLists[i],
                    (IsotopeGrouperParameters) parameters);
        }

        TaskGroup newGroup = new TaskGroup(tasks, this, taskGroupListener);

        // start the group
        newGroup.start();

        return newGroup;

    }

    public BatchStepCategory getBatchStepCategory() {
        return BatchStepCategory.PEAKLISTPROCESSING;
    }

}