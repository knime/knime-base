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
 * ---------------------------------------------------------------------
 *
 * History
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.RowFlushable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This model is the end node of a for loop.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndNodeModel} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopEndDynamicNodeModel extends NodeModel implements LoopEndNode, RowFlushable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoopEndDynamicNodeModel.class);

    private long m_startTime;

    //overall row count for each table
    private long[] m_counts;

    //current iteration
    private int m_iteration = 0;

    /* Helper factories to collect the intermediate tables and create
     * the final concatenated table. */
    private ConcatenateTableFactory[] m_tableFactories;

    private final LoopEndDynamicNodeSettings m_settings;

    /**
     * Creates a new model.
     *
     * @param inputPorts the input ports
     * @param outputPorts the output ports
     */
    LoopEndDynamicNodeModel(final PortType[] inputPorts, final PortType[] outputPorts) {
        super(inputPorts, outputPorts);
        m_settings = new LoopEndDynamicNodeSettings(inputPorts.length);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return IntStream.range(0, inSpecs.length)//
                .mapToObj(i -> getConfiguredSpecs(i, inSpecs[i]))//
                .toArray(DataTableSpec[]::new);
    }

    private DataTableSpec getConfiguredSpecs(final int index, final DataTableSpec spec) {
        if (m_settings.ignoreEmptyTables(index) || m_settings.tolerateColumnTypes(index)
            || m_settings.tolerateChangingTableSpecs(index)) {
            return null;
        } else {
            return ConcatenateTableFactory.createSpec(spec, m_settings.addIterationColumn(), false);
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        CheckUtils.checkState(this.getLoopStartNode() instanceof LoopStartNodeTerminator,
            "Loop End is not connected to matching/corresponding Loop Start node. "
            + "You are trying to create an infinite loop!");

        if (m_tableFactories == null) {
            //first time we get here: create table factory
            m_counts = new long[inData.length]; // initialized to 0
            m_tableFactories = IntStream.range(0, inData.length)//
                .mapToObj(i -> new ConcatenateTableFactory(m_settings.ignoreEmptyTables(i),
                    m_settings.tolerateColumnTypes(i), m_settings.addIterationColumn(),
                    m_settings.tolerateChangingTableSpecs(i), getRowKeyFunction(i)))//
                .toArray(ConcatenateTableFactory[]::new);

            m_startTime = System.currentTimeMillis();
        }

        for (var i = 0; i < inData.length; i++) {
            m_tableFactories[i].addTable(inData[i], exec);
        }

        final var terminateLoop = ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        final var result = new BufferedDataTable[inData.length];
        if (terminateLoop) {
            LOGGER.debugWithFormat("Total loop execution time: %dms", System.currentTimeMillis() - m_startTime);
            m_startTime = 0;
            m_iteration = 0;
            m_counts = null;
            for (var i = 0; i < inData.length; i++) {
                result[i] = m_tableFactories[i].createTable(exec);
            }
        } else {
            m_iteration++;
            continueLoop();
        }

        return result;
    }

    @Override
    public boolean shouldPropagateModifiedVariables() {
        return m_settings.propagateLoopVariables();
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    @Override
    protected void reset() {
        m_startTime = 0;
        if (m_tableFactories != null) {
            Arrays.stream(m_tableFactories).forEach(f -> f.clear(true));
        }
        m_tableFactories = null;
        m_counts = null;
        m_iteration = 0;
    }

    @Override
    protected void onDispose() {
        reset();
        super.onDispose();
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // empty
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        AbstractLoopEndNodeSettings s = new LoopEndDynamicNodeSettings(getNrInPorts());
        s.loadSettings(settings);
    }

    private Optional<Function<RowKey, RowKey>> getRowKeyFunction(final int tableNumber) {
        switch (m_settings.rowKeyPolicy()) {
            case APPEND_SUFFIX:
                 return Optional.of(k -> new RowKey(k.toString() + "#" + m_iteration));
            case GENERATE_NEW:
                    return Optional.of(k -> {
                        final var key = RowKey.createRowKey(m_counts[tableNumber]);
                        m_counts[tableNumber]++;
                        return key;
                    });
            case UNMODIFIED:
            default:
                return Optional.empty();
        }
    }

    @Override
    public void flushRows() {
        if (m_tableFactories != null) {
            Stream.of(m_tableFactories).forEach(RowFlushable::flushRows);
        }
    }
}
