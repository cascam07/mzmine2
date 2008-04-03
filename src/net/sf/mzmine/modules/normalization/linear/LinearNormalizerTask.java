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

package net.sf.mzmine.modules.normalization.linear;

import java.util.Hashtable;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;

class LinearNormalizerTask implements Task {

    public static final float maximumOverallPeakHeightAfterNormalization = 100000.0f;

    private PeakList originalPeakList;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    private int processedDataFiles, totalDataFiles;

    private String suffix, normalizationType, peakMeasurementType;
    private boolean removeOriginal;

    public LinearNormalizerTask(PeakList peakList,
            LinearNormalizerParameters parameters) {

        this.originalPeakList = peakList;

        totalDataFiles = originalPeakList.getNumberOfRawDataFiles();

        suffix = (String) parameters.getParameterValue(LinearNormalizerParameters.suffix);
        normalizationType = (String) parameters.getParameterValue(LinearNormalizerParameters.normalizationType);
        peakMeasurementType = (String) parameters.getParameterValue(LinearNormalizerParameters.peakMeasurementType);
        removeOriginal = (Boolean) parameters.getParameterValue(LinearNormalizerParameters.autoRemove);

    }

    public void cancel() {
        status = TaskStatus.CANCELED;

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        return (float) processedDataFiles / (float) totalDataFiles;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Linear normalization of " + originalPeakList + " by "
                + normalizationType;
    }

    public void run() {

        status = TaskStatus.PROCESSING;

        // This hashtable maps rows from original alignment result to rows of
        // the normalized alignment
        Hashtable<PeakListRow, SimplePeakListRow> rowMap = new Hashtable<PeakListRow, SimplePeakListRow>();

        // Create new peak list
        SimplePeakList normalizedPeakList = new SimplePeakList(originalPeakList
                + " " + suffix, originalPeakList.getRawDataFiles());

        // Loop through all raw data files, and find the peak with biggest
        // height
        float maxOriginalHeight = 0.0f;
        for (RawDataFile file : originalPeakList.getRawDataFiles()) {
            for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
                Peak p = originalpeakListRow.getPeak(file);
                if (p != null)
                    if (maxOriginalHeight <= p.getHeight())
                        maxOriginalHeight = p.getHeight();
            }
        }

        // Loop through all raw data files, and normalize peak values
        for (RawDataFile file : originalPeakList.getRawDataFiles()) {

            // Cancel?
            if (status == TaskStatus.CANCELED) {
                return;
            }

            // Determine normalization type and calculate normalization factor
            // accfileingly
            float normalizationFactor = 1.0f;

            // - normalization by average squared peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeAverageIntensity) {
                float intensitySum = 0.0f;
                int intensityCount = 0;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    Peak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            intensitySum += p.getHeight();
                        } else {
                            intensitySum += p.getArea();
                        }
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (float) intensityCount;
            }

            // - normalization by average squared peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeAverageSquaredIntensity) {
                float intensitySum = 0.0f;
                int intensityCount = 0;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    Peak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            intensitySum += (p.getHeight() * p.getHeight());
                        } else {
                            intensitySum += (p.getArea() * p.getArea());
                        }
                        intensityCount++;
                    }
                }
                normalizationFactor = intensitySum / (float) intensityCount;
            }

            // - normalization by maximum peak intensity
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeMaximumPeakHeight) {
                float maximumIntensity = 0.0f;
                for (PeakListRow peakListRow : originalPeakList.getRows()) {
                    Peak p = peakListRow.getPeak(file);
                    if (p != null) {
                        if (peakMeasurementType == LinearNormalizerParameters.PeakMeasurementTypeHeight) {
                            if (maximumIntensity < p.getHeight())
                                maximumIntensity = p.getHeight();
                        } else {
                            if (maximumIntensity < p.getArea())
                                maximumIntensity = p.getArea();
                        }

                    }
                }
                normalizationFactor = maximumIntensity;
            }

            // - normalization by total raw signal
            if (normalizationType == LinearNormalizerParameters.NormalizationTypeTotalRawSignal) {
                normalizationFactor = 0;
                for (int scanNumber : file.getScanNumbers(1)) {
                    Scan scan = file.getScan(scanNumber);
                    normalizationFactor += scan.getTIC();
                }
            }

            // Find peak with maximum height and calculate scaling the brings
            // height of this peak to

            // Readjust normalization factor so that maximum height will be
            // equal to maximumOverallPeakHeightAfterNormalization after
            // normalization
            float maxNormalizedHeight = maxOriginalHeight / normalizationFactor;
            normalizationFactor = normalizationFactor * maxNormalizedHeight
                    / maximumOverallPeakHeightAfterNormalization;

            // Normalize all peak intenisities using the normalization factor
            for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {

                // Cancel?
                if (status == TaskStatus.CANCELED) {
                    return;
                }

                Peak originalPeak = originalpeakListRow.getPeak(file);
                if (originalPeak != null) {
                    SimplePeak normalizedPeak = new SimplePeak(originalPeak);
                    float normalizedHeight = originalPeak.getHeight()
                            / normalizationFactor;
                    float normalizedArea = originalPeak.getArea()
                            / normalizationFactor;
                    normalizedPeak.setHeight(normalizedHeight);
                    normalizedPeak.setArea(normalizedArea);

                    SimplePeakListRow normalizedRow = rowMap.get(originalpeakListRow);
                    if (normalizedRow == null) {
                        normalizedRow = new SimplePeakListRow(
                                originalpeakListRow.getID());
                        normalizedRow.setComment(originalpeakListRow.getComment());
                        for (CompoundIdentity ident : originalpeakListRow.getCompoundIdentities())
                            normalizedRow.addCompoundIdentity(ident);
                        rowMap.put(originalpeakListRow, normalizedRow);
                    }

                    normalizedRow.addPeak(file, originalPeak, normalizedPeak);

                }

            }

            // Progress
            processedDataFiles++;

        }

        // Finally add all normalized rows to normalized alignment result
        for (PeakListRow originalpeakListRow : originalPeakList.getRows()) {
            SimplePeakListRow normalizedRow = rowMap.get(originalpeakListRow);
            normalizedPeakList.addRow(normalizedRow);
        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(normalizedPeakList);

        // Remove the original peaklist if requested
        if (removeOriginal)
            currentProject.removePeakList(originalPeakList);

        status = TaskStatus.FINISHED;
    }

}