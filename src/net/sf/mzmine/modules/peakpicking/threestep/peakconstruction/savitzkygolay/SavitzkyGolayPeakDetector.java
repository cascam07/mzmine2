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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.savitzkygolay;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleMzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.ConnectedPeak;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder;
import net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.peakfillingmodels.PeakFillingModel;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ConnectedMzPeak;
import net.sf.mzmine.util.MathUtils;

/**
 * This class implements a peak builder using a match score to link MzPeaks in
 * the axis of retention time. Also uses Savitzky-Golay coefficients to
 * calculate the first and second derivative (smoothed) of raw data points
 * (intensity) that conforms each peak. The first derivative is used to
 * determine the peak's range, and the second derivative to determine the
 * intensity of the peak.
 * 
 */
public class SavitzkyGolayPeakDetector implements PeakBuilder {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private float minimumPeakHeight, minimumPeakDuration, 
			derivativeThresholdLevel;
	private boolean fillingPeaks;
	private PeakFillingModel peakModel;

	private static final float[][] SGCoefficientsSecondDerivative = {
			{ 0.0f },
			{ -1.0f, 0.5f },
			{ -0.143f, -0.071f, 0.143f },
			{ -0.048f, -0.036f, 0.0f, 0.060f },
			{ -0.022f, -0.018f, -0.009f, 0.008f, 0.030f },
			{ -0.012f, -0.010f, -0.007f, -0.001f, 0.007f, 0.017f },
			{ -0.007f, -0.006f, -0.005f, -0.002f, 0.001f, 0.005f, 0.011f },
			{ -0.005f, -0.004f, -0.004f, -0.002f, -0.001f, 0.002f, 0.004f,
					0.007f },
			{ -0.003f, -0.003f, -0.003f, -0.002f, -0.001f, 0.000f, 0.002f,
					0.003f, 0.005f },
			{ -0.002f, -0.002f, -0.002f, -0.002f, -0.001f, 0.000f, 0.000f,
					0.001f, 0.003f, 0.004f },
			{ -0.002f, -0.002f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.001f, 0.001f, 0.002f, 0.003f },
			{ -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.000f, 0.001f, 0.001f, 0.002f, 0.002f },
			{ -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, -0.001f, 0.000f,
					0.000f, 0.000f, 0.001f, 0.001f, 0.001f, 0.002f } };

	private float maxValueDerivative = 0.0f;

	/**
	 * Constructor of Savitzky-Golay Peak Builder
	 * 
	 * @param parameters
	 */
	public SavitzkyGolayPeakDetector(

	SavitzkyGolayPeakDetectorParameters parameters) {
		minimumPeakDuration = (Float) parameters
				.getParameterValue(SavitzkyGolayPeakDetectorParameters.minimumPeakDuration);
		minimumPeakHeight = (Float) parameters
				.getParameterValue(SavitzkyGolayPeakDetectorParameters.minimumPeakHeight);
		derivativeThresholdLevel = (Float) parameters
				.getParameterValue(SavitzkyGolayPeakDetectorParameters.derivativeThresholdLevel);

		fillingPeaks = (Boolean) parameters
				.getParameterValue(SavitzkyGolayPeakDetectorParameters.fillingPeaks);
		
		String peakModelname = (String) parameters
				.getParameterValue(SavitzkyGolayPeakDetectorParameters.peakModel);

		// Create an instance of selected model class
		try {

			String peakModelClassName = null;

			for (int modelIndex = 0; modelIndex < SavitzkyGolayPeakDetectorParameters.peakModelNames.length; modelIndex++) {
				if (SavitzkyGolayPeakDetectorParameters.peakModelNames[modelIndex]
						.equals(peakModelname))
					peakModelClassName = SavitzkyGolayPeakDetectorParameters.peakModelClasses[modelIndex];
				;
			}

			if (peakModelClassName == null)
				throw new ClassNotFoundException();

			Class peakModelClass = Class.forName(peakModelClassName);

			peakModel = (PeakFillingModel) peakModelClass.newInstance();

		} catch (Exception e) {
			logger.warning("Error trying to make an instance of peak model "
					+ peakModelname);
		}

	}

	/**
	 * @see net.sf.mzmine.modules.peakpicking.threestep.peakconstruction.PeakBuilder#addChromatogram(net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram,
	 *      net.sf.mzmine.data.RawDataFile)
	 */
	public ChromatographicPeak[] addChromatogram(Chromatogram chromatogram,
			RawDataFile dataFile) {

		maxValueDerivative = 0.0f;
		float maxIntensity = 0;

		Vector<ChromatographicPeak> detectedPeaks = new Vector<ChromatographicPeak>();

		int[] scanNumbers = dataFile.getScanNumbers(1);
		float[] chromatoIntensities = new float[scanNumbers.length];
		float avgChromatoIntensities = 0;
		Arrays.sort(scanNumbers);

		for (int i = 0; i < scanNumbers.length; i++) {
			ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[i]);
			if (mzValue != null) {
				chromatoIntensities[i] = mzValue.getMzPeak().getIntensity();
			} else {
				chromatoIntensities[i] = 0;
			}

			if (chromatoIntensities[i] > maxIntensity)
				maxIntensity = chromatoIntensities[i];
			avgChromatoIntensities += chromatoIntensities[i];
		}

		avgChromatoIntensities /= scanNumbers.length;

		// If the current chromatogram has characteristics of background
		// return an empty array.
		if ((avgChromatoIntensities) > (maxIntensity * 0.5f))
			return detectedPeaks.toArray(new ChromatographicPeak[0]);

		float[] chromato2ndDerivative = calculate2ndDerivative(chromatoIntensities);
		float noiseThreshold = calcDerivativeThreshold(chromato2ndDerivative);

		ChromatographicPeak[] chromatographicPeaks = SGPeaksSearch(dataFile,
				chromatogram, scanNumbers, chromato2ndDerivative,
				noiseThreshold);

		if (chromatographicPeaks.length != 0) {
			for (ChromatographicPeak p : chromatographicPeaks) {
				float pLength = p.getRawDataPointsRTRange().getSize();
				float pHeight = p.getHeight();
				if ((pLength >= minimumPeakDuration)
						&& (pHeight >= minimumPeakHeight)) {

					// Apply peak filling method
					if (fillingPeaks) {
						ChromatographicPeak shapeFilledPeak = peakModel.fillingPeak(p);
						detectedPeaks.add(shapeFilledPeak);
					} else
						detectedPeaks.add(p);
				}
			}
		}

		return detectedPeaks.toArray(new ChromatographicPeak[0]);
	}

	/**
	 * 
	 * 
	 * @param SimpleChromatogram
	 *            ucPeak
	 * @return Peak[]
	 */
	private ChromatographicPeak[] SGPeaksSearch(RawDataFile dataFile,
			Chromatogram chromatogram, int[] scanNumbers,
			float[] derivativeOfIntensities, float noiseThreshold) {

		boolean activeFirstPeak = false, activeSecondPeak = false, passThreshold = false;
		int crossZero = 0;

		Vector<ConnectedPeak> newPeaks = new Vector<ConnectedPeak>();
		Vector<ConnectedMzPeak> newMzPeaks = new Vector<ConnectedMzPeak>();
		Vector<ConnectedMzPeak> newOverlappedMzPeaks = new Vector<ConnectedMzPeak>();
		float maxDerivativeIntensity = 0;
		int indexMaxPoint = 0;

		for (int i = 1; i < derivativeOfIntensities.length; i++) {
			
			if ((derivativeOfIntensities[i] < maxDerivativeIntensity) && (crossZero == 2)) {
			maxDerivativeIntensity = derivativeOfIntensities[i];
			indexMaxPoint = i;
			}

			if (((derivativeOfIntensities[i - 1] < 0.0f) && (derivativeOfIntensities[i] > 0.0f))
					|| ((derivativeOfIntensities[i - 1] > 0.0f) && (derivativeOfIntensities[i] < 0.0f))) {

				if ((derivativeOfIntensities[i - 1] < 0.0f)
						&& (derivativeOfIntensities[i] > 0.0f)) {
					if (crossZero == 2) {
						if (passThreshold) {
							activeSecondPeak = true;
						} else {
							newMzPeaks.clear();
							crossZero = 0;
							activeFirstPeak = false;
						}
					}

				}

				if (crossZero == 3) {
					activeFirstPeak = false;
				}

				// Always increment
				passThreshold = false;
				if ((activeFirstPeak) || (activeSecondPeak)) {
					crossZero++;
				}

			}

			if (Math.abs(derivativeOfIntensities[i]) > noiseThreshold) {
				passThreshold = true;
				if ((crossZero == 0) && (derivativeOfIntensities[i] > 0)) {
					// logger.finest("Prende primero " + crossZero);
					activeFirstPeak = true;
					crossZero++;
				}
			}

			// if (true){
			if ((activeFirstPeak)) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newMzPeaks.add(mzValue);
					
					/*  ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
					  .getScan(), new SimpleMzPeak(new SimpleDataPoint(mzValue
					  .getMzPeak().getMZ(), derivativeOfIntensities[i]* 100)));
					  newMzPeaks.add(temp);*/
					 
				} else if (newMzPeaks.size() > 0) {
					activeFirstPeak = false;
					crossZero = 0;
				}

			}

			if (activeSecondPeak) {
				ConnectedMzPeak mzValue = chromatogram
						.getConnectedMzPeak(scanNumbers[i]);
				if (mzValue != null) {
					newOverlappedMzPeaks.add(mzValue);
					
					/* ConnectedMzPeak temp = new ConnectedMzPeak(mzValue
					  .getScan(), new SimpleMzPeak(new SimpleDataPoint(mzValue
					  .getMzPeak().getMZ(), derivativeOfIntensities[i]* 100)));
					  newOverlappedMzPeaks.add(temp);*/
					 
				}
			}

			if ((newMzPeaks.size() > 0) && (!activeFirstPeak)) {
				ConnectedPeak SGPeak = new ConnectedPeak(dataFile, newMzPeaks
						.elementAt(0));
				for (int j = 1; j < newMzPeaks.size(); j++) {
					SGPeak.addMzPeak(newMzPeaks.elementAt(j));
				}
				
				if (fillingPeaks) {
					
					ConnectedMzPeak mzValue = chromatogram
					.getConnectedMzPeak(scanNumbers[indexMaxPoint]);
					
					if (mzValue != null) {
						float height = mzValue.getMzPeak()
								.getIntensity();
						SGPeak.setHeight(height);

						float rt = mzValue.getScan()
								.getRetentionTime();
						SGPeak.setRT(rt);
					}

				indexMaxPoint = 0;
				maxDerivativeIntensity = 0;
				}
				
				
				
				newMzPeaks.clear();
				newPeaks.add(SGPeak);

				if ((newOverlappedMzPeaks.size() > 0) && (activeSecondPeak)) {
					for (ConnectedMzPeak p : newOverlappedMzPeaks)
						newMzPeaks.add(p);
					activeSecondPeak = false;
					activeFirstPeak = true;
					crossZero = 2;
					newOverlappedMzPeaks.clear();
					passThreshold = false;

				}
			}

		}

		if (newMzPeaks.size() > 0) {
			ConnectedPeak SGPeak = new ConnectedPeak(dataFile, newMzPeaks
					.elementAt(0));
			for (int j = 1; j < newMzPeaks.size(); j++) {
				SGPeak.addMzPeak(newMzPeaks.elementAt(j));
			}
			newMzPeaks.clear();
			newPeaks.add(SGPeak);
		}

		return newPeaks.toArray(new ConnectedPeak[0]);
	}

	/**
	 * This method returns the second smoothed derivative values of an array.
	 * 
	 * @param chromatoIntensities
	 * @return
	 */
	public float[] calculate2ndDerivative(float[] chromatoIntensities) {

		float[] derivative = new float[chromatoIntensities.length];
		int M = 0;

		for (int k = 0; k < derivative.length; k++) {

			// Determine boundaries
			if (k < 13)
				M = k;
			if (k + M > derivative.length - 1)
				M = derivative.length - (k + 1);

			// Perform derivative using Savitzky Golay coefficients
			for (int i = -M; i <= M; i++) {
				derivative[k] += chromatoIntensities[k + i]
						* getSGCoefficient(M, i);
			}
			if ((Math.abs(derivative[k])) > maxValueDerivative)
				maxValueDerivative = Math.abs(derivative[k]);

		}

		return derivative;
	}

	/**
	 * This method return the Savitzky-Golay 2nd smoothed derivative coefficient
	 * from an array
	 * 
	 * @param M
	 * @param signedC
	 * @return
	 */
	private Float getSGCoefficient(int M, int signedC) {

		int C = Math.abs(signedC);
		return SGCoefficientsSecondDerivative[M][C];

	}

	/**
	 * 
	 * @param chromatoIntensities
	 * @return noiseThresholdLevel
	 */
	private float calcDerivativeThreshold(float[] derivativeIntensities) {

		float[] intensities = new float[derivativeIntensities.length];
		for (int i = 0; i < derivativeIntensities.length; i++) {
			intensities[i] = (float) Math.abs(derivativeIntensities[i]);
		}

		return MathUtils.calcQuantile(intensities, derivativeThresholdLevel);
	}

}