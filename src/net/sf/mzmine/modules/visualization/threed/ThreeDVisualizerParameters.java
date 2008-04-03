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

package net.sf.mzmine.modules.visualization.threed;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

/**
 * 3D visualizer parameter set
 */
public class ThreeDVisualizerParameters extends SimpleParameterSet {

    public static final Integer[] msLevels = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    public static final Parameter msLevel = new SimpleParameter(
            ParameterType.INTEGER, "MS level", "MS level of plotted scans", 1,
            msLevels);

    public static final Parameter retentionTimeRange = new SimpleParameter(
            ParameterType.RANGE, "Retention time",
            "Retention time (X axis) range", null, new Range(0, 600),
            new Float(0), null, MZmineCore.getRTFormat());

    public static final Parameter rtResolution = new SimpleParameter(
            ParameterType.INTEGER, "Retention time resolution",
            "Number of data points on retention time axis", "data points", 100,
            100, null);

    public static final Parameter mzRange = new SimpleParameter(
            ParameterType.RANGE, "m/z range", "m/z (Y axis) range", "m/z",
            new Range(0, 1000), new Float(0), null, MZmineCore.getMZFormat());

    public static final Parameter mzResolution = new SimpleParameter(
            ParameterType.INTEGER, "m/z resolution",
            "Number of data points on m/z axis", "data points", 100, 100, null);

    public ThreeDVisualizerParameters() {
        super(new Parameter[] { msLevel, retentionTimeRange, rtResolution,
                mzRange, mzResolution });
    }

}