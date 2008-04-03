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

package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.util.Range;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan implements Scan {

    private int scanNumber;
    private int msLevel;
    private int parentScan;
    private int fragmentScans[];
    private DataPoint dataPoints[];
    private float precursorMZ;
    private int precursorCharge;
    private float retentionTime;
    private Range mzRange;
    private DataPoint basePeak;
    private float totalIonCurrent;
    private boolean centroided;

    /**
     * Clone constructor
     */
    public SimpleScan(Scan sc) {
        this(sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
                sc.getParentScanNumber(), sc.getPrecursorMZ(),
                sc.getFragmentScanNumbers(), sc.getDataPoints(),
                sc.isCentroided());
    }

    /**
     * Constructor for creating scan with given data
     */
    public SimpleScan(int scanNumber, int msLevel, float retentionTime,
            int parentScan, float precursorMZ, int fragmentScans[],
            DataPoint[] dataPoints, boolean centroided) {

        // check assumptions about proper scan data
        assert (msLevel == 1) || (parentScan > 0);

        // save scan data
        this.scanNumber = scanNumber;
        this.msLevel = msLevel;
        this.retentionTime = retentionTime;
        this.parentScan = parentScan;
        this.precursorMZ = precursorMZ;
        this.fragmentScans = fragmentScans;
        this.centroided = centroided;

        setDataPoints(dataPoints);
    }

    /**
     * @return Returns scan datapoints
     */
    public DataPoint[] getDataPoints() {
        return dataPoints;
    }

    public DataPoint[] getDataPoints(Range mzRange) {

        int startIndex, endIndex;
        for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
            if (dataPoints[startIndex].getMZ() >= mzRange.getMin())
                break;
        }

        for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
            if (dataPoints[endIndex].getMZ() > mzRange.getMax())
                break;
        }

        DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

        // Copy the relevant points
        System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex
                - startIndex);

        return pointsWithinRange;
    }

    /**
     * @param mzValues m/z values to set
     * @param intensityValues Intensity values to set
     */
    public void setDataPoints(DataPoint[] dataPoints) {

        this.dataPoints = dataPoints;

        // find m/z range and base peak
        if (dataPoints.length > 0) {

            basePeak = dataPoints[0];
            mzRange = new Range(dataPoints[0].getMZ(),
                    dataPoints[0].getMZ());

            for (DataPoint dp : dataPoints) {

                if (dp.getIntensity() > basePeak.getIntensity())
                    basePeak = dp;

                mzRange.extendRange(dp.getMZ());

                totalIonCurrent += dp.getIntensity();

            }

        } else {
            // Empty scan, so no m/z range or base peak
            mzRange = new Range(0, 0);
            basePeak = null;
            totalIonCurrent = 0;
        }

    }

    /**
     * @see net.sf.mzmine.data.Scan#getNumberOfDataPoints()
     */
    public int getNumberOfDataPoints() {
        return dataPoints.length;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @param scanNumber The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
        this.scanNumber = scanNumber;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }

    /**
     * @param msLevel The msLevel to set.
     */
    public void setMSLevel(int msLevel) {
        this.msLevel = msLevel;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
     */
    public float getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @param precursorMZ The precursorMZ to set.
     */
    public void setPrecursorMZ(float precursorMZ) {
        this.precursorMZ = precursorMZ;
    }

    /**
     * @return Returns the precursorCharge.
     */
    public int getPrecursorCharge() {
        return precursorCharge;
    }

    /**
     * @param precursorCharge The precursorCharge to set.
     */
    public void setPrecursorCharge(int precursorCharge) {
        this.precursorCharge = precursorCharge;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
     */
    public float getRetentionTime() {
        return retentionTime;
    }

    /**
     * @param retentionTime The retentionTime to set.
     */
    public void setRetentionTime(float retentionTime) {
        this.retentionTime = retentionTime;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMZRangeMax()
     */
    public Range getMZRange() {
        return mzRange;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getBasePeakMZ()
     */
    public DataPoint getBasePeak() {
        return basePeak;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getParentScanNumber()
     */
    public int getParentScanNumber() {
        return parentScan;
    }

    /**
     * @param parentScan The parentScan to set.
     */
    public void setParentScanNumber(int parentScan) {
        this.parentScan = parentScan;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getFragmentScanNumbers()
     */
    public int[] getFragmentScanNumbers() {
        return fragmentScans;
    }

    /**
     * @param fragmentScans The fragmentScans to set.
     */
    public void setFragmentScanNumbers(int[] fragmentScans) {
        this.fragmentScans = fragmentScans;
    }

    /**
     * @see net.sf.mzmine.data.Scan#isCentroided()
     */
    public boolean isCentroided() {
        return centroided;
    }

    /**
     * @param centroided The centroided to set.
     */
    public void setCentroided(boolean centroided) {
        this.centroided = centroided;
    }

    public float getTIC() {
        return totalIonCurrent;
    }

}