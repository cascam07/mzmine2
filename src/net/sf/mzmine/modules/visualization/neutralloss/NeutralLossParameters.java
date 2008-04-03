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

package net.sf.mzmine.modules.visualization.neutralloss;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

public class NeutralLossParameters extends SimpleParameterSet {

    public static final String xAxisPrecursor = "Precursor mass";
    public static final String xAxisRT = "Retention time";

    public static final String[] xAxisTypes = { xAxisPrecursor, xAxisRT };

    public static final Parameter xAxisType = new SimpleParameter(
            ParameterType.STRING, "X axis", "X axis type", xAxisPrecursor,
            xAxisTypes);

    public static final Parameter retentionTimeRange = new SimpleParameter(
            ParameterType.RANGE, "Retention time",
            "Retention time (X axis) range", null, new Range(0, 600),
            new Float(0), null, MZmineCore.getRTFormat());

    public static final Parameter mzRange = new SimpleParameter(
            ParameterType.RANGE, "Precursor m/z",
            "Range of precursor m/z values", "m/z", new Range(0, 1000),
            new Float(0), null, MZmineCore.getMZFormat());

    public static final Parameter numOfFragments = new SimpleParameter(
            ParameterType.INTEGER, "Fragments",
            "Number of most intense fragments", null, new Integer(5),
            new Integer(0), null);

    public NeutralLossParameters() {
        super(new Parameter[] { xAxisType, retentionTimeRange, mzRange,
                numOfFragments });
    }

}