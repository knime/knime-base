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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.explain.shapley.node;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.meta.explain.shapley.KnimeShapleyValuesEstimator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
public class ShapleyValuesLoopEndNodeModel extends NodeModel implements LoopEndNode {

    private static final String LOOP_NAME = "Shapley Values Loop";

    /**
     * Constructor
     */
    public ShapleyValuesLoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final KnimeShapleyValuesEstimator estimator = getEstimator();
        return new DataTableSpec[]{estimator.configureLoopEnd(inSpecs[0])};
    }

    private ShapleyValuesLoopStartNodeModel getLoopStart() throws InvalidSettingsException {
        final LoopStartNode loopStart = getLoopStartNode();
        if (loopStart instanceof ShapleyValuesLoopStartNodeModel) {
            return (ShapleyValuesLoopStartNodeModel)loopStart;
        } else {
            throw new InvalidSettingsException(
                "The " + LOOP_NAME + " End node can only be used with the " + LOOP_NAME + " Start node.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable data = inData[0];
        final KnimeShapleyValuesEstimator estimator = getEstimator();
        estimator.consumePredictions(data, exec);
        if (estimator.hasNextIteration()) {
            continueLoop();
            return new BufferedDataTable[1];
        }
        final BufferedDataTable result = estimator.getLoopEndTable();
        return new BufferedDataTable[]{result};
    }

    private KnimeShapleyValuesEstimator getEstimator() throws InvalidSettingsException {
        return getLoopStart().getEstimator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // currently receives its settings from the loop start node
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // currently receives its settings from the loop start node
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // currently receives its settings from the loop start node
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // currently nothing to reset
    }

}
