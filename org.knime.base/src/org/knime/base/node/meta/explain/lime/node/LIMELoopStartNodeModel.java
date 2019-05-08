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
 *   14.03.2016 (adrian): created
 */
package org.knime.base.node.meta.explain.lime.node;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.meta.explain.lime.LimeExplainer;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Node Model of the start of a Shapley Values loop.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class LIMELoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {

    /**
     * Port index of the table containing the rows to explain
     */
    private static final int ROI_PORT_IDX = 0;

    /**
     * Port index of the table containing the rows for sampling
     */
    private static final int SAMPLING_DATA_PORT_IDX = 1;

    private LimeExplainer m_explainer;

    private LIMESettings m_settings = new LIMESettings();

    /**
     * @return the estimator used to create rows in the loop start node
     */
    LimeExplainer getEstimator() {
        if (m_explainer == null) {
            m_explainer = new LimeExplainer(m_settings);
        }
        return m_explainer;
    }

    /**
     * The first port contains the rows for which to estimate the Shapley Values. The second port provides the dataset
     * that is used to transform rows from the first table.
     */
    private static final int NUM_INPORTS = 2;

    /**
     *
     */
    public LIMELoopStartNodeModel() {
        super(NUM_INPORTS, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec roiSpec = inSpecs[ROI_PORT_IDX];
        final DataTableSpec samplingSpec = inSpecs[SAMPLING_DATA_PORT_IDX];
        checkCompatibility(roiSpec, samplingSpec);
        if (m_explainer == null) {
            m_explainer = new LimeExplainer(m_settings);
        }
        return m_explainer.configure(roiSpec, samplingSpec, m_settings);
    }

    private static void checkCompatibility(final DataTableSpec roiSpec, final DataTableSpec samplingSpec)
        throws InvalidSettingsException {
        CheckUtils.checkNotNull(roiSpec);
        CheckUtils.checkNotNull(samplingSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable roiTable = inData[ROI_PORT_IDX];
        final BufferedDataTable samplingTable = inData[SAMPLING_DATA_PORT_IDX];
        final BufferedDataTable[] result = m_explainer.getNextTables(roiTable, samplingTable, exec);
        pushFlowVariableInt("currentIteration", m_explainer.getCurrentIteration());
        pushFlowVariableInt("maxIterations", m_explainer.getMaxIterations());
        pushFlowVariableString("weightColumnName", m_explainer.getWeightColumnName());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return !m_explainer.hasNextIteration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final LIMESettings cfg = new LIMESettings();
        cfg.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings = new LIMESettings();
        m_settings.loadSettingsModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (m_explainer != null) {
            m_explainer.reset();
        }
        m_explainer = null;
    }

}
