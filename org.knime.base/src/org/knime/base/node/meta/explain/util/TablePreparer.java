/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.03.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * Ensures that the specs and tables passed on to the estimator have the correct structure.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TablePreparer {

    private ColumnSetManager m_colManager;

    private final String m_purpose;

    /**
     * @param filter column filter identifying the columns handled by this instance
     * @param purpose purpose of this instance (e.g. feature or prediction)
     */
    public TablePreparer(final DataColumnSpecFilterConfiguration filter, final String purpose) {
        m_colManager = new ColumnSetManager(filter);
        m_purpose = purpose;
    }

    /**
     * Constructs a TablePreparer that handles the columns in <b>spec</b>.
     *
     * @param spec the spec handled by the created instance
     * @param purpose of this instance (e.g. feature or prediction)
     */
    public TablePreparer(final DataTableSpec spec, final String purpose) {
        m_colManager = new ColumnSetManager(spec);
        m_purpose = purpose;
    }

    /**
     * Creates the output spec for the loop start node.
     *
     * @return output spec of the loop start node
     */
    public DataTableSpec getTableSpec() {
        return m_colManager.getTableSpec();
    }

    /**
     * Update with a new filter and table spec.
     *
     * @param inSpec
     * @param filter
     */
    public void updateSpecs(final DataTableSpec inSpec, final DataColumnSpecFilterConfiguration filter) {
        m_colManager = new ColumnSetManager(filter);
        m_colManager.updateColumnSet(inSpec);
    }

    /**
     * Checks if {@link DataTableSpec samplingSpec} contains the required columns.
     *
     * @param samplingSpec spec to check
     * @throws InvalidSettingsException if a column is missing
     */
    public void checkSpec(final DataTableSpec samplingSpec) throws InvalidSettingsException {
        try {
            m_colManager.checkColumnsContained(samplingSpec);
        } catch (MissingColumnException e) {
            throw new InvalidSettingsException(
                "The sampling table misses the " + m_purpose + " column " + e.getMissingColumn() + ".", e);
        }
    }

    private static void ensureColumnsAreContained(final DataTableSpec inSpec, final ColumnSetManager mgr,
        final String mgrPurpose) throws InvalidSettingsException {
        try {
            mgr.checkColumnsContained(inSpec);
        } catch (MissingColumnException e) {
            throw new InvalidSettingsException("The input table does not contain all " + mgrPurpose
                + " columns. Missing column " + e.getMissingColumn() + ".");
        }
    }

    /**
     * @param inputTable table to transform
     * @param exec {@link ExecutionContext} for table creation
     * @return the input table containing only the columns managed by this TablePreparer in the exact same order
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws MissingColumnException
     */
    public BufferedDataTable createTable(final BufferedDataTable inputTable, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        final ColumnSetManager manager = m_colManager;
        final DataTableSpec tableSpec = inputTable.getDataTableSpec();
        ensureColumnsAreContained(tableSpec, manager, m_purpose);
        final ColumnRearranger rearranger = manager.createRearranger(tableSpec);
        return exec.createColumnRearrangeTable(inputTable, rearranger, exec);
    }

    /**
     * Creates a {@link CloseableRowIterator} that only contains the columns managed by this TablePreparer.
     *
     * @param inputTable must contain all managed columns and may also contain additional columns
     * @param exec {@link ExecutionContext} for creation of the filter table
     * @return a {@link CloseableRowIterator} over a filtered version of <b>inputTable</b>
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     * @throws MissingColumnException
     */
    public CloseableRowIterator createIterator(final BufferedDataTable inputTable, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        return createTable(inputTable, exec).iterator();
    }

    /**
     * @return the number of columns this table preparer manages
     */
    public int getNumColumns() {
        return m_colManager.getNumColumns();
    }

}
