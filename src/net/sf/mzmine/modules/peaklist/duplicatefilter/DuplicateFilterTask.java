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

package net.sf.mzmine.modules.peaklist.duplicatefilter;

import java.util.Arrays;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakListRowSorter.SortingDirection;
import net.sf.mzmine.util.PeakListRowSorter.SortingProperty;

/**
 * 
 */
class DuplicateFilterTask implements Task {

    private PeakList peaklist;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // peaks counter
    private int processedRows, totalRows;

    // parameter values
    private String suffix;
    private float mzDifferenceMax, rtDifferenceMax;
    private boolean requireSameIdentification, removeOriginal;

    /**
     * @param rawDataFile
     * @param parameters
     */
    DuplicateFilterTask(PeakList peaklist, DuplicateFilterParameters parameters) {

        this.peaklist = peaklist;

        // Get parameter values for easier use
        suffix = (String) parameters.getParameterValue(DuplicateFilterParameters.suffix);
        mzDifferenceMax = (Float) parameters.getParameterValue(DuplicateFilterParameters.mzDifferenceMax);
        rtDifferenceMax = (Float) parameters.getParameterValue(DuplicateFilterParameters.rtDifferenceMax);
        requireSameIdentification = (Boolean) parameters.getParameterValue(DuplicateFilterParameters.requireSameIdentification);
        removeOriginal = (Boolean) parameters.getParameterValue(DuplicateFilterParameters.autoRemove);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Duplicate peak filter on " + peaklist;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0f;
        return (float) processedRows / (float) totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getStatus()
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getErrorMessage()
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public PeakList getPeakList() {
        return peaklist;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#cancel()
     */
    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        status = TaskStatus.PROCESSING;

        SimplePeakList filteredPeakList = new SimplePeakList(peaklist + " "
                + suffix, peaklist.getRawDataFiles());
       
        PeakListRow[] peaklistRows = peaklist.getRows();
        
        totalRows = peaklistRows.length;
        
        Arrays.sort(peaklistRows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Loop through all peak list rows
        for (int firstRowIndex = 0; firstRowIndex < peaklistRows.length; firstRowIndex++) {
        	        	       
        	if (peaklistRows[firstRowIndex]==null) continue;
        	        	
        	// Search for duplicate rows with smaller peak area
        	for (int secondRowIndex = (firstRowIndex + 1); secondRowIndex < peaklistRows.length; secondRowIndex++) {

        		if (status == TaskStatus.CANCELED) return;
        		
        		if (peaklistRows[secondRowIndex]==null) continue;
        		
        		// Compare identifications
        		boolean sameID = true;
        		if (requireSameIdentification) {
        			// TODO
        			// Use preferred identifications if available instead of all identifications
        			// Use CompoundIDs if available instead of CompoundNames
        			
       				CompoundIdentity[] firstIdentities = peaklistRows[firstRowIndex].getCompoundIdentities();
       				CompoundIdentity[] secondIdentities = peaklistRows[secondRowIndex].getCompoundIdentities();
       				
       				if ( (firstIdentities.length==0) && (secondIdentities.length==0) )
       					sameID = true;
       				
       				for (CompoundIdentity firstIdentity : firstIdentities) {
       					sameID = false;
       					for (CompoundIdentity secondIdentity : secondIdentities) {
       						if (firstIdentity.getCompoundName().equals(secondIdentity.getCompoundName())) {
       							sameID = true;
       							break;
       						}
       					}
       					if (!sameID)
       						break;
       				}
       				
        		}
      				
        		// Compare m/z
        		boolean sameMZ = false;
        		float firstMZ = peaklistRows[firstRowIndex].getAverageMZ();
        		float secondMZ = peaklistRows[secondRowIndex].getAverageMZ();
        		if (Math.abs(firstMZ-secondMZ) < mzDifferenceMax) {
        			sameMZ = true;
        		}
        		
        		// Compare rt
        		boolean sameRT = false;
        		float firstRT = peaklistRows[firstRowIndex].getAverageRT();
        		float secondRT = peaklistRows[secondRowIndex].getAverageRT();
        		if (Math.abs(firstRT-secondRT) < rtDifferenceMax) {
        			sameRT = true;
        		}
        		
        		if (sameID && sameMZ && sameRT) {
        			//System.out.println("Duplicate for ID " + peaklistRows[firstRowIndex].getID() + " is ID " + peaklistRows[secondRowIndex].getID());
        			peaklistRows[secondRowIndex] = null;
        		}

        		
        	}
        	
        	processedRows++;
        	
        }
        
        // Add all remaining rows to a new peak list
        for (PeakListRow peaklistRow : peaklistRows) {
        	if (peaklistRow != null) 
        		filteredPeakList.addRow(peaklistRow);
        }


        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(filteredPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(peaklist);

        status = TaskStatus.FINISHED;

    }

}