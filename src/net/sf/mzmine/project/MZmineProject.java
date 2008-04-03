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

package net.sf.mzmine.project;

import java.io.File;
import java.util.Vector;

import javax.swing.DefaultListModel;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;

/**
 * 
 * MZmineProject collects all items user has opened or created during an MZmine
 * session. This includes
 * <ul>
 * <li> Experimental parameters and their values for each RawDataFile.
 * Experimental parameters are available for defining any properties of the
 * sample, for instance concentration or a class label.
 * <li> Opened RawDataFiles
 * <li> PeakLists of each RawDataFile. A peak list represents results of peak
 * detection on a single RawDataFile or a processed version of a preceding
 * PeakList.
 * <li> PeakLists of multiple aligned PeakLists. An aligned peak list represent
 * results of aligning multiple PeakLists of individual runs or a processed
 * version of a preceding aligned PeakList.
 * </ul>
 * 
 * @see Parameter
 * @see ParameterValue
 * @see RawDataFile
 * @see PeakList
 * 
 */
public interface MZmineProject {
	/**
	 * set file system location of this project ;
	 * 
	 * @param parameter
	 */
	public void setLocation(File path);

	/**
	 * Return file system location of this project ;
	 * 
	 * @param parameter
	 */
	public File getLocation();

	/**
	 * Adds a new experimental parameter to the project
	 * 
	 * @param parameter
	 */
	public void addParameter(Parameter parameter);

	/**
	 * Removes an experimental parameter from the project
	 * 
	 * @param parameter
	 */
	public void removeParameter(Parameter parameter);

	/**
	 * Returns true if project contains the experimental parameter
	 */
	public boolean hasParameter(Parameter parameter);

	/**
	 * Returns all experimental parameter of the project
	 */
	public Parameter[] getParameters();

	/**
	 * Sets experimental parameter's value corresponding to a RawDataFile.
	 * <p>
	 * If the parameter does not exists in the project, it is added to the
	 * project. If parameter already has a value corresponding the given file,
	 * previous value is replaced.
	 * 
	 */
	public void setParameterValue(Parameter parameter, RawDataFile rawDataFile,
			Object value);

	/**
	 * Returns experimental parameter's value corresponding to a RawDataFile.
	 * 
	 */
	public Object getParameterValue(Parameter parameter, RawDataFile rawDataFile);

	/**
	 * Adds a new RawDataFile to the project.
	 * 
	 */
	public void addFile(RawDataFile newFile);

	/**
	 * Removes a RawDataFile from the project.
	 * 
	 */
	public void removeFile(RawDataFile file);

	/**
	 * Returns all RawDataFiles of the project.
	 * 
	 */
	public RawDataFile[] getDataFiles();

	/**
	 * Returns RawDataFile with specified file name.
	 * 
	 */
	public RawDataFile getDataFile(String fileName);

	/**
	 * Adds a peak list to the project
	 * 
	 */
	public void addPeakList(PeakList peaklist);

	/**
	 * Removes a peak list from the project
	 * 
	 */
	public void removePeakList(PeakList peaklist);

	/**
	 * Returns all peak lists of the project
	 */
	public PeakList[] getPeakLists();

	/**
	 * Returns all peak lists which contain given data file
	 */
	public PeakList[] getPeakLists(RawDataFile file);

	/**
	 * Adds a listener which is notified when there is a change of the project
	 */
	public void addProjectListener(ProjectListener listener);

	/**
	 * Remove a listener which is notified when there is a change of the project
	 */
	public void removeProjectListener(ProjectListener listener);

	/**
	 * List listeners
	 */
	public Vector<ProjectListener> getProjectListeners();

	/**
	 * check project is in temporal space (Not yet saved)
	 */

	public boolean getIsTemporal();

	/**
	 * set project is in temporal space or not
	 */

	public void setIsTemporal(boolean isTemoral);

	/**
	 * Returns PeakListModel for GUI
	 */
	public DefaultListModel getPeakListsListModel();

	/**
	 * Returns DataFileListModel for GUI
	 */
	public DefaultListModel getRawDataListModel();

	/**
	 * Returns PeakListModel for GUI
	 */
	public void setPeakListsListModel(DefaultListModel listModel);

	/**
	 * Returns DataFileListModel for GUI
	 */
	public void setRawDataListModel(DefaultListModel listModel);
}