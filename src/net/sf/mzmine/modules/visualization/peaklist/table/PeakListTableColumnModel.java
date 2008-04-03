/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.peaklist.table;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableParameters;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.components.ColumnGroup;
import net.sf.mzmine.util.components.GroupableTableHeader;

/**
 * 
 */
public class PeakListTableColumnModel extends DefaultTableColumnModel implements
        MouseListener {

    private static final Font editFont = new Font("SansSerif", Font.PLAIN, 10);

    private FormattedCellRenderer mzRenderer, rtRenderer, intensityRenderer;
    private TableCellRenderer peakShapeRenderer, identityRenderer,
            peakStatusRenderer;
    private DefaultTableCellRenderer defaultRenderer;

    private PeakListTableVisualizer visualizer;

    private PeakListTableParameters parameters;
    private PeakList peakList;
    private GroupableTableHeader header;

    private TableColumn allColumns[];

    private TableColumn columnBeingResized;

    /**
     * 
     */
    PeakListTableColumnModel(PeakListTableVisualizer visualizer,
            GroupableTableHeader header, PeakListTableModel tableModel,
            PeakListTableParameters parameters, PeakList peakList) {

        this.visualizer = visualizer;

        this.parameters = parameters;
        this.peakList = peakList;

        allColumns = new TableColumn[tableModel.getColumnCount()];

        this.header = header;

        header.addMouseListener(this);

        // prepare formatters
        NumberFormatter mzFormat = MZmineCore.getMZFormat();
        NumberFormatter rtFormat = MZmineCore.getRTFormat();
        NumberFormatter intensityFormat = MZmineCore.getIntensityFormat();

        // prepare cell renderers
        mzRenderer = new FormattedCellRenderer(mzFormat);
        rtRenderer = new FormattedCellRenderer(rtFormat);
        intensityRenderer = new FormattedCellRenderer(intensityFormat);
        peakShapeRenderer = new PeakShapeCellRenderer(peakList, parameters);
        identityRenderer = new CompoundIdentityCellRenderer();
        peakStatusRenderer = new PeakStatusCellRenderer();
        defaultRenderer = new DefaultTableCellRenderer();
        defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        JTextField editorField = new JTextField();
        editorField.setFont(editFont);
        DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);

        int modelIndex = 0;

        for (CommonColumnType commonColumn : CommonColumnType.values()) {

            TableColumn newColumn = new TableColumn(modelIndex);
            newColumn.setHeaderValue(commonColumn.getColumnName());
            newColumn.setIdentifier(commonColumn);
            
            switch (commonColumn) {
            case AVERAGEMZ:
                newColumn.setCellRenderer(mzRenderer);
                break;
            case AVERAGERT:
                newColumn.setCellRenderer(rtRenderer);
                break;
            case IDENTITY:
                newColumn.setCellRenderer(identityRenderer);
                break;
            case COMMENT:
                newColumn.setCellRenderer(defaultRenderer);
                newColumn.setCellEditor(defaultEditor);
                break;
            default:
                newColumn.setCellRenderer(defaultRenderer);
            }

            allColumns[modelIndex] = newColumn;

            modelIndex++;

        }

        for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {

            for (DataFileColumnType dataFileColumn : DataFileColumnType.values()) {

                TableColumn newColumn = new TableColumn(modelIndex);
                newColumn.setHeaderValue(dataFileColumn.getColumnName());
                newColumn.setIdentifier(dataFileColumn);

                switch (dataFileColumn) {
                case MZ:
                    newColumn.setCellRenderer(mzRenderer);
                    break;
                case PEAKSHAPE:
                    newColumn.setCellRenderer(peakShapeRenderer);
                    break;
                case STATUS:
                    newColumn.setCellRenderer(peakStatusRenderer);
                    break;
                case RT:
                    newColumn.setCellRenderer(rtRenderer);
                    break;
                case DURATION:
                    newColumn.setCellRenderer(rtRenderer);
                    break;
                case HEIGHT:
                    newColumn.setCellRenderer(intensityRenderer);
                    break;
                case AREA:
                    newColumn.setCellRenderer(intensityRenderer);
                    break;
                }

                allColumns[modelIndex] = newColumn;

                modelIndex++;
            }
        }

    }

    public void createColumns() {

        // clear column groups
        ColumnGroup groups[] = header.getColumnGroups();
        if (groups != null) {
            for (ColumnGroup group : groups) {
                header.removeColumnGroup(group);
            }
        }

        // clear the column model
        while (getColumnCount() > 0) {
            TableColumn col = getColumn(0);
            removeColumn(col);
        }

        int modelIndex = 0;

        ColumnGroup averageGroup = new ColumnGroup("Average");
        header.addColumnGroup(averageGroup);

        for (CommonColumnType commonColumn : CommonColumnType.values()) {

            if (parameters.isColumnVisible(commonColumn)) {
                this.addColumn(allColumns[modelIndex]);
                allColumns[modelIndex].setPreferredWidth(parameters.getColumnWidth(commonColumn));
                if ((commonColumn == CommonColumnType.AVERAGEMZ)
                        || (commonColumn == CommonColumnType.AVERAGERT)) {
                    averageGroup.add(allColumns[modelIndex]);
                }
            }

            modelIndex++;

        }

        for (RawDataFile dataFile : peakList.getRawDataFiles()) {

            ColumnGroup fileGroup = new ColumnGroup(dataFile.toString());
            header.addColumnGroup(fileGroup);

            for (DataFileColumnType dataFileColumn : DataFileColumnType.values()) {

                if (parameters.isColumnVisible(dataFileColumn)) {
                    this.addColumn(allColumns[modelIndex]);
                    fileGroup.add(allColumns[modelIndex]);
                    allColumns[modelIndex].setPreferredWidth(parameters.getColumnWidth(dataFileColumn));
                }

                modelIndex++;
            }
        }

    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
        // ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
        // ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
        // ignore
    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        columnBeingResized = header.getResizingColumn();
    }

    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {

        if (columnBeingResized != null) {

            final int modelIndex = columnBeingResized.getModelIndex();
            final int newWidth = columnBeingResized.getPreferredWidth();

            final int numOfCommonColumns = CommonColumnType.values().length;
            final int numOfDataFileColumns = DataFileColumnType.values().length;
            final CommonColumnType commonColumns[] = CommonColumnType.values();
            final DataFileColumnType dataFileColumns[] = DataFileColumnType.values();

            if (modelIndex < numOfCommonColumns) {
                parameters.setColumnWidth(commonColumns[modelIndex], newWidth);
            } else {
                int dataFileColumnIndex = (modelIndex - numOfCommonColumns)
                        % numOfDataFileColumns;
                parameters.setColumnWidth(dataFileColumns[dataFileColumnIndex],
                        newWidth);

                // set same width to other data file columns of this type
                for (int dataFileIndex = peakList.getNumberOfRawDataFiles() - 1; dataFileIndex >= 0; dataFileIndex--) {
                    int columnIndex = numOfCommonColumns
                            + (dataFileIndex * numOfDataFileColumns)
                            + dataFileColumnIndex;
                    int currentWidth = allColumns[columnIndex].getPreferredWidth();

                    if (currentWidth != newWidth) {
                        allColumns[columnIndex].setPreferredWidth(newWidth);
                    }
                }

            }

            // store current widths as default
            visualizer.setParameters(parameters.clone());

        }

    }

}