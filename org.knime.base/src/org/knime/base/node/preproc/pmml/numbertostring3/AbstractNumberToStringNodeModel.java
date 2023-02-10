/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------
 *
 * History
 *   Jan 28, 2019 (Johannes Schweig): created
 */
package org.knime.base.node.preproc.pmml.numbertostring3;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.util.spec.TableSpecUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;

/**
 * The NodeModel for the Number to String Node that converts numbers to StringValues.
 *
 * @author Johannes Schweig
 * @param <T> SettingsModel for a ColumnFilter component
 * @since 4.0
 */

public abstract class AbstractNumberToStringNodeModel<T extends SettingsModel> extends NodeModel {

    /** The filter value class. */
    private static final Class<DoubleValue> VALUE_CLASS = DoubleValue.class;

    /**
     * Key for the included columns in the NodeSettings.
     */
    public static final String CFG_INCLUDED_COLUMNS = "include";

    /** The included columns. */
    private final T m_inclCols;

    /**
     * Constructor with one data inport, one data outport and an optional PMML inport and outport.
     *
     * @param pmmlInEnabled true if there should be an optional input port
     * @param inclCols SettingsModel for a ColumnFilter component
     * @since 3.0
     */
    public AbstractNumberToStringNodeModel(final boolean pmmlInEnabled, final T inclCols) {
        super(pmmlInEnabled ? new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE_OPTIONAL}
            : new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE, PMMLPortObject.TYPE});
        m_inclCols = inclCols;
    }

    /**
     * Constructor with one data inport, one data outport and an optional PMML inport and outport.
     *
     * @param inclCols SettingsModel for a ColumnFilter component
     */
    public AbstractNumberToStringNodeModel(final T inclCols) {
        super(1, 1);
        m_inclCols = inclCols;
    }

    /**
     * Returns the indices of columns that the transformation should be applied to
     *
     * @param spec the current DataTableSpec
     * @return an integer array with the column indices
     * @throws InvalidSettingsException
     */
    protected int[] findColumnIndices(final DataTableSpec spec) throws InvalidSettingsException {
        final String[] inclCols = getInclCols(spec);
        final StringBuilder warnings = new StringBuilder();
        if (inclCols.length == 0) {
            warnings.append("No columns selected");
        }
        final int[] indices;
        if (isKeepAllSelected()) {
            indices = TableSpecUtils.findAllCompatibleColumns(spec, VALUE_CLASS);
        } else {
            indices = TableSpecUtils.findCompatibleColumns(spec, inclCols, VALUE_CLASS, warnings::append);
        }
        if (warnings.length() > 0) {
            setWarningMessage(warnings.toString());
        }
        return indices;
    }

    /**
     * Returns all present includes from a DataTableSpec.
     *
     * @param inSpec the current DataTableSpec
     * @return a String array with the included columns
     * @since 4.5
     */
    protected abstract String[] getInclCols(final DataTableSpec inSpec);

    /**
     * @return returns true if the keep all selected checkbox is checked, false if it is not checked or not present
     */
    protected abstract boolean isKeepAllSelected();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_inclCols.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inclCols.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * @return the included columns
     */
    protected T getInclCols() {
        return m_inclCols;
    }

    /**
     * The CellFactory to produce the new converted cells.
     *
     * @author cebron, University of Konstanz
     */
    public static class ConverterFactory implements CellFactory {

        /*
         * Column indices to use.
         */
        private final int[] m_colindices;

        /*
         * Original DataTableSpec.
         */
        private final DataTableSpec m_spec;

        /**
         *
         * @param colindices the column indices to use.
         * @param spec the original DataTableSpec.
         */
        public ConverterFactory(final int[] colindices, final DataTableSpec spec) {
            m_colindices = colindices;
            m_spec = spec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            DataCell[] newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                DataCell dc = row.getCell(m_colindices[i]);
                // handle integers separately to avoid decimal places
                if (dc instanceof IntValue) {
                    int iVal = ((IntValue)dc).getIntValue();
                    newcells[i] = new StringCell(Integer.toString(iVal));
                } else if (dc instanceof LongValue) {
                    long l = ((LongValue)dc).getLongValue();
                    newcells[i] = new StringCell(Long.toString(l));
                } else if (dc instanceof DoubleValue) {
                    double d = ((DoubleValue)dc).getDoubleValue();
                    newcells[i] = new StringCell(Double.toString(d));
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataColumnSpec[] getColumnSpecs() {
            DataColumnSpec[] newcolspecs = new DataColumnSpec[m_colindices.length];
            for (int i = 0; i < newcolspecs.length; i++) {
                DataColumnSpec colspec = m_spec.getColumnSpec(m_colindices[i]);
                DataColumnSpecCreator colspeccreator = null;
                // change DataType to StringCell
                colspeccreator = new DataColumnSpecCreator(colspec.getName(), StringCell.TYPE);
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        /**
         * {@inheritDoc}
         *
         * @deprecated
         */
        @Deprecated
        @Override
        public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Converting");
        }

        /**
         * This method was intended to return error messages, but never did, hence it is deprecated now.
         *
         * @return Returns an empty string, because that is what it has returned in the past, in case somebody is still
         *         checking for the length of the returned message.
         *
         * @deprecated since 5.0
         */
        @Deprecated
        public String getErrorMessage() {
            return "";
        }
    } // end ConverterFactory

}
