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

package net.sf.mzmine.io.impl;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.util.Range;

/**
 * Simple implementation of the Scan interface.
 */
public class StorableScan implements Scan {

    private transient Logger logger = Logger.getLogger(this.getClass().getName());

    private int scanNumber, msLevel, parentScan, fragmentScans[];
    private float precursorMZ;
    private int precursorCharge;
    private float retentionTime;
    private Range mzRange;
    private DataPoint basePeak;
    private float totalIonCurrent;
    private boolean centroided;

    private long storageFileOffset;
    private int storageArrayByteLength;
    private int numberOfDataPoints;
    private RawDataFile rawDataFile;

    /**
     * Clone constructor
     */
    public StorableScan(Scan sc, RawDataFile rawDataFile) {
        this(sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
                sc.getParentScanNumber(), sc.getPrecursorMZ(),
                sc.getFragmentScanNumbers(), sc.getDataPoints(),
                sc.isCentroided(), rawDataFile);
    }

    /**
     * Constructor for creating scan with given data
     */
    public StorableScan(int scanNumber, int msLevel, float retentionTime,
            int parentScan, float precursorMZ, int fragmentScans[],
            DataPoint[] dataPoints, boolean centroided, RawDataFile rawDataFile) {

        // check assumptions about proper scan data
        assert (msLevel == 1) || (parentScan > 0);

        // save rawDataFile file reference
        this.rawDataFile = rawDataFile;

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
     * @return Scan's datapoints from temporary file.
     */
    public DataPoint[] getDataPoints() {

        ByteBuffer buffer = ByteBuffer.allocate(storageArrayByteLength);
        RandomAccessFile storageFile = rawDataFile.getScanDataFile();
        synchronized (storageFile) {
            try {
                storageFile.seek(storageFileOffset);
                storageFile.read(buffer.array(), 0, storageArrayByteLength);
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Could not read data from temporary file", e);
                return new DataPoint[0];
            }
        }

        FloatBuffer floatBuffer = buffer.asFloatBuffer();

        DataPoint dataPoints[] = new DataPoint[numberOfDataPoints];

        for (int i = 0; i < numberOfDataPoints; i++) {
            float mz = floatBuffer.get();
            float intensity = floatBuffer.get();
            dataPoints[i] = new SimpleDataPoint(mz, intensity);
        }

        return dataPoints;

    }

    public DataPoint[] getDataPoints(Range mzRange) {

        DataPoint dataPoints[] = getDataPoints();

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
     * @param dataPoints New datapoints
     */
    void setDataPoints(DataPoint[] dataPoints) {

        numberOfDataPoints = dataPoints.length;

        // every float needs 4 bytes, we need one float for m/z and one float
        // for intensity
        storageArrayByteLength = numberOfDataPoints * 4 * 2;

        ByteBuffer buffer = ByteBuffer.allocate(storageArrayByteLength);
        FloatBuffer floatBuffer = buffer.asFloatBuffer();
        RandomAccessFile storageFile = rawDataFile.getWritingScanDataFile();
        synchronized (storageFile) {
            try {

                storageFileOffset = storageFile.length();
                storageFile.seek(storageFileOffset);

                for (DataPoint dp : dataPoints) {
                    floatBuffer.put(dp.getMZ());
                    floatBuffer.put(dp.getIntensity());
                }

                storageFile.write(buffer.array());

            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Could not write data to temporary file", e);
            }
        }

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
        return numberOfDataPoints;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanNumber()
     */
    public int getScanNumber() {
        return scanNumber;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getMSLevel()
     */
    public int getMSLevel() {
        return msLevel;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getPrecursorMZ()
     */
    public float getPrecursorMZ() {
        return precursorMZ;
    }

    /**
     * @return Returns the precursorCharge.
     */
    public int getPrecursorCharge() {
        return precursorCharge;
    }

    /**
     * @see net.sf.mzmine.data.Scan#getScanAcquisitionTime()
     */
    public float getRetentionTime() {
        return retentionTime;
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
    void setFragmentScanNumbers(int[] fragmentScans) {
        this.fragmentScans = fragmentScans;
    }

    /**
     * @see net.sf.mzmine.data.Scan#isCentroided()
     */
    public boolean isCentroided() {
        return centroided;
    }

    public float getTIC() {
        return totalIonCurrent;
    }

    private Object readResolve() {

        logger = Logger.getLogger(this.getClass().getName());
        return this;
    }

}