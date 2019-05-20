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
 *   May 2, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.base.node.mine.transformation.settings.TransformationComputeSettings;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public abstract class AbstractPCA2NodeModel extends NodeModel {

    /**
     * The data in-port index.
     */
    public static final int DATA_IN_PORT = 0;

    /**
     * The compute settings.
     */
    protected final TransformationComputeSettings m_computeSettings = new TransformationComputeSettings();

    private String[] m_columnNames;

    /**
     * Constructs a body for an LDA Node with a BufferedDataTable as the inPort.
     *
     * @param outPortTypes
     */
    protected AbstractPCA2NodeModel(final PortType[] outPortTypes) {
        super(new PortType[]{BufferedDataTable.TYPE}, outPortTypes);
    }

    /**
     * Constructs a body for an LDA Node.
     *
     * @param inPortTypes the in-port types
     * @param outPortTypes the out-port types
     */
    protected AbstractPCA2NodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
        super(inPortTypes, outPortTypes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // clear members
    }

    /**
     * Validates and returns the input data.
     *
     * @param inData
     * @param exec
     * @return the input as a BufferedDataTable
     * @throws IllegalArgumentException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     */
    @Override
    protected final PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException {
        if (!(inData[DATA_IN_PORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[DATA_IN_PORT];
        if (inTable.size() == 0) {
            throw new InvalidSettingsException("Cannot produce a PCA model for an empty table.");
        }

        return doExecute(inTable, exec);
    }

    /**
     * Will be called after execute, which prepared the data. Calculates the resulting LDA.
     *
     * @param inTable
     * @param exec
     * @return The created PortObject[].
     * @throws IllegalArgumentException
     * @throws InvalidSettingsException
     * @throws CanceledExecutionException
     *
     */
    protected abstract PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws IllegalArgumentException, InvalidSettingsException, CanceledExecutionException;

    /**
     * Tries to find a valid class column and one column that can be projected down from. Also finds the indices of the
     * used Columns, w/o the class column index if it should be contained.
     *
     * @param inSpecs
     * @return the inSpec that can further be used
     * @throws InvalidSettingsException
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (!(inSpecs[DATA_IN_PORT] instanceof DataTableSpec)) {
            throw new IllegalArgumentException("Datatable as input expected");
        }
        final DataTableSpec inSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];
        // check the included columns
        m_columnNames = m_computeSettings.getUsedColsModel().applyTo(inSpec).getIncludes();
        CheckUtils.checkSetting(m_columnNames != null && m_columnNames.length > 0,
            "Please selected at least one column");

        return doConfigure(inSpec);
    }

    /**
     * Additional configuration that will be done after the call of configure(), i.e. after auto-configuration of the
     * class column and one column to project down from. Should then serve as
     * {@link NodeModel#configure(DataTableSpec[])}.
     *
     * @param inSpec
     * @return The portobjectspec as the output of this node will produce
     * @throws InvalidSettingsException
     */
    protected abstract PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException;

    /**
     * Returns the used column names.
     *
     * @return the column names
     */
    protected String[] getColumnNames() {
        return m_columnNames;
    }

    /**
     * Create a column rearranger that applies the LDA, if given
     *
     * @param inSpec the inspec of the table
     * @param lda the transformation or null if called from configure
     * @param k number of dimensions to reduce to (number of rows in w)
     * @param removeUsedCols whether to remove the input data
     * @return the column re-arranger
     *
     *         TODO: update doc
     */
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final TransformationMatrix transMatrix, final int k, final boolean removeUsedCols) {
        return TransformationUtils.createColumnRearranger(inSpec, transMatrix, k, removeUsedCols, m_columnNames,
            TransformationType.PCA);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) {
        m_computeSettings.saveSettingsTo(settings);
        saveAdditionalSettingsTo(settings);
    }

    /**
     * Save additional settings, called after {@link #saveSettingsTo(NodeSettingsWO)}.
     *
     * @param settings
     */
    protected abstract void saveAdditionalSettingsTo(final NodeSettingsWO settings);

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.loadValidatedSettingsFrom(settings);
        loadAdditionalValidatedSettingsFrom(settings);
    }

    /**
     * Load additional settings, called after {@link #loadValidatedSettingsFrom(NodeSettingsRO)}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected abstract void loadAdditionalValidatedSettingsFrom(final NodeSettingsRO settings)
        throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_computeSettings.validateSettings(settings);
        validateAdditionalSettings(settings);
    }

    /**
     * Validate additional settings, called after {@link #validateSettings(NodeSettingsRO)}.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    protected abstract void validateAdditionalSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

}
