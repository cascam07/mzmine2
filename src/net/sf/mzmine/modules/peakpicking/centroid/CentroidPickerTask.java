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

package net.sf.mzmine.modules.peakpicking.centroid;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

/**
 * 
 */
class CentroidPickerTask implements Task {

    private RawDataFile dataFile;

    private TaskStatus status = TaskStatus.WAITING;
    private String errorMessage;

    // scan counter
    private int processedScans, totalScans;

    // parameter values
    private String suffix;
    private float binSize, chromatographicThresholdLevel, intTolerance;
    private float noiseLevel, minimumPeakHeight, mzTolerance;
    private float minimumPeakDuration;

    // peak id counter
    private int newPeakID = 1;

    /**
     * @param rawDataFile
     * @param parameters
     */
    CentroidPickerTask(RawDataFile dataFile, CentroidPickerParameters parameters) {

        this.dataFile = dataFile;

        // Get parameter values for easier use
        suffix = (String) parameters.getParameterValue(CentroidPickerParameters.suffix);
        binSize = (Float) parameters.getParameterValue(CentroidPickerParameters.binSize);
        chromatographicThresholdLevel = (Float) parameters.getParameterValue(CentroidPickerParameters.chromatographicThresholdLevel);
        intTolerance = (Float) parameters.getParameterValue(CentroidPickerParameters.intTolerance);
        minimumPeakDuration = (Float) parameters.getParameterValue(CentroidPickerParameters.minimumPeakDuration);
        minimumPeakHeight = (Float) parameters.getParameterValue(CentroidPickerParameters.minimumPeakHeight);
        mzTolerance = (Float) parameters.getParameterValue(CentroidPickerParameters.mzTolerance);
        noiseLevel = (Float) parameters.getParameterValue(CentroidPickerParameters.noiseLevel);

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Centroid peak detection on " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / (float) (2 * totalScans);
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

    public RawDataFile getDataFile() {
        return dataFile;
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

        // Create new peak list
        SimplePeakList newPeakList = new SimplePeakList(
                dataFile + " " + suffix, dataFile);

        // Get all scans of MS level 1
        int[] scanNumbers = dataFile.getScanNumbers(1);
        totalScans = scanNumbers.length;

        /*
         * Calculate M/Z binning
         */

        // Get minimum and maximum m/z values
        float startMZ = dataFile.getDataMZRange(1).getMin();
        float endMZ = dataFile.getDataMZRange(1).getMax();
        int numOfBins = (int) (Math.ceil((endMZ - startMZ) / binSize));

        float[] chromatographicThresholds = new float[numOfBins];

        if (chromatographicThresholdLevel > 0) {

            float[][] binInts = new float[numOfBins][totalScans];

            // Loop through scans and calculate binned maximum intensities
            for (int i = 0; i < totalScans; i++) {

                if (status == TaskStatus.CANCELED)
                    return;

                Scan sc = dataFile.getScan(scanNumbers[i]);

                DataPoint dataPoints[] = sc.getDataPoints();
                float[] mzValues = new float[dataPoints.length];
                float[] intensityValues = new float[dataPoints.length];
                for (int dp = 0; dp < dataPoints.length; dp++) {
                    mzValues[dp] = dataPoints[dp].getMZ();
                    intensityValues[dp] = dataPoints[dp].getIntensity();
                }
                
                float[] tmpInts = ScanUtils.binValues(mzValues,
                        intensityValues, new Range(startMZ, endMZ), numOfBins, true,
                        ScanUtils.BinningType.MAX);
                for (int bini = 0; bini < numOfBins; bini++) {
                    binInts[bini][i] = tmpInts[bini];
                }

                processedScans++;

            }

            // Calculate filtering threshold from each RIC
            float initialThreshold = Float.MAX_VALUE;

            for (int bini = 0; bini < numOfBins; bini++) {

                chromatographicThresholds[bini] = MathUtils.calcQuantile(
                        binInts[bini], chromatographicThresholdLevel);
                if (chromatographicThresholds[bini] < initialThreshold) {
                    initialThreshold = chromatographicThresholds[bini];
                }
            }

            binInts = null;

        } else {
            processedScans += totalScans;
            for (int bini = 0; bini < numOfBins; bini++)
                chromatographicThresholds[bini] = 0;

        }

        Vector<CentroidPeak> underConstructionPeaks = new Vector<CentroidPeak>();
        Vector<OneDimPeak> oneDimPeaks = new Vector<OneDimPeak>();

        for (int i = 0; i < totalScans; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan

            Scan sc = dataFile.getScan(scanNumbers[i]);

            DataPoint dataPoints[] = sc.getDataPoints();
            float[] mzValues = new float[dataPoints.length];
            float[] intensityValues = new float[dataPoints.length];
            for (int dp = 0; dp < dataPoints.length; dp++) {
                mzValues[dp] = dataPoints[dp].getMZ();
                intensityValues[dp] = dataPoints[dp].getIntensity();
            }

            // Find 1D-peaks

            for (int j = 0; j < intensityValues.length; j++) {

                // Is intensity above the noise level?
                if (intensityValues[j] >= noiseLevel) {

                    // Determine correct bin
                    int bin = (int) Math.floor((mzValues[j] - startMZ) / binSize);
                    if (bin < 0) {
                        bin = 0;
                    }
                    if (bin >= numOfBins) {
                        bin = numOfBins - 1;
                    }

                    // Is intensity above the chromatographic threshold level
                    // for this bin?
                    if (intensityValues[j] >= chromatographicThresholds[bin]) {

                        // Yes, then mark this index as 1D-peak
                        oneDimPeaks.add(new OneDimPeak(i, j, mzValues[j],
                                intensityValues[j]));
                    }

                }

            }

            // Calculate scores between under-construction scores and 1d-peaks

            TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

            for (CentroidPeak ucPeak : underConstructionPeaks) {

                for (OneDimPeak oneDimPeak : oneDimPeaks) {
                    MatchScore score = new MatchScore(ucPeak, oneDimPeak,
                            mzTolerance, intTolerance);
                    if (score.getScore() < Float.MAX_VALUE) {
                        scores.add(score);
                    }
                }

            }

            // Connect the best scoring pairs of under-construction and 1d peaks

            Iterator<MatchScore> scoreIterator = scores.iterator();

            while (scoreIterator.hasNext()) {
                MatchScore score = scoreIterator.next();

                // If 1d peak is already connected, then move to next score
                OneDimPeak oneDimPeak = score.getOneDimPeak();
                if (oneDimPeak.isConnected()) {
                    continue;
                }

                // If uc peak is already connected, then move on to next score
                CentroidPeak ucPeak = score.getPeak();
                if (ucPeak.isGrowing()) {
                    continue;
                }

                // Connect 1d to uc
                ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz,
                        sc.getRetentionTime(), oneDimPeak.intensity);
                oneDimPeak.setConnected();

            }

            // Check if there are any under-construction peaks that were not
            // connected
            for (CentroidPeak ucPeak : underConstructionPeaks) {

                // If nothing was added,
                if (!ucPeak.isGrowing()) {

                	// Finalize peak
                	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
                	
                    // Check length
                    float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
                    float ucHeight = ucPeak.getHeight();
                    if ((ucLength >= minimumPeakDuration)
                            && (ucHeight >= minimumPeakHeight)) {

                        // Good peak, add it to the peak list
                        SimplePeakListRow newRow = new SimplePeakListRow(
                                newPeakID);
                        newPeakID++;
                        newRow.addPeak(dataFile, ucPeak, ucPeak);
                        newPeakList.addRow(newRow);

                    }

                    // Remove the peak from under construction peaks
                    int ucInd = underConstructionPeaks.indexOf(ucPeak);
                    underConstructionPeaks.set(ucInd, null);

                }

            }

            // Clean-up empty slots under-construction peaks collection and
            // reset growing statuses for remaining under construction peaks
            for (int ucInd = 0; ucInd < underConstructionPeaks.size(); ucInd++) {
                CentroidPeak ucPeak = underConstructionPeaks.get(ucInd);
                if (ucPeak == null) {
                    underConstructionPeaks.remove(ucInd);
                    ucInd--;
                } else {
                    ucPeak.resetGrowingState();
                }
            }

            // If there are some unconnected 1d-peaks, then start a new
            // under-construction peak for each of them
            for (OneDimPeak oneDimPeak : oneDimPeaks) {

                if (!oneDimPeak.isConnected()) {

                    CentroidPeak ucPeak = new CentroidPeak(dataFile);
                    ucPeak.addDatapoint(sc.getScanNumber(), oneDimPeak.mz,
                            sc.getRetentionTime(), oneDimPeak.intensity);
                    underConstructionPeaks.add(ucPeak);

                }

            }

            oneDimPeaks.clear();

            processedScans++;

        } // End of scan loop

        // Finally process all remaining under-construction peaks

        for (CentroidPeak ucPeak : underConstructionPeaks) {
        	
        	// Finalize peak
        	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
        	
            // Check length & height
            float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
            float ucHeight = ucPeak.getHeight();
            if ((ucLength >= minimumPeakDuration)
                    && (ucHeight >= minimumPeakHeight)) {

                // Good peak, add it to the peak list
                SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
                newPeakID++;
                newRow.addPeak(dataFile, ucPeak, ucPeak);
                newPeakList.addRow(newRow);

            }

        }

        // Add new peaklist to the project
        MZmineProject currentProject = MZmineCore.getCurrentProject();
        currentProject.addPeakList(newPeakList);

        status = TaskStatus.FINISHED;

    }

}