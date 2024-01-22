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
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.chunk;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Loop start node that outputs a set of rows at a time. Used to implement
 * a streaming (or chunking approach) where only a set of rows is processed at
 * a time
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class LoopStartChunkNodeModel extends WebUINodeModel<LoopStartChunkNodeSettings>
    implements LoopStartNodeTerminator {

    /**
     * @param configuration
     */
    LoopStartChunkNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, LoopStartChunkNodeSettings.class);
    }

    // loop invariants
    private BufferedDataTable m_table;
    private CloseableRowIterator m_iterator;

    // loop variants
    private long m_iteration;

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final LoopStartChunkNodeSettings modelSettings)
        throws InvalidSettingsException {
        pushFlowVariableInt("currentIteration", 0);
        pushFlowVariableInt("maxIterations", 0);
        return inSpecs;
    }

    private static void checkIntegerCasts(final long nrRowsPerChunk, final long nrChunks, final long nrRows)
        throws InvalidSettingsException {
        try {
            Math.toIntExact(nrRowsPerChunk);
            Math.toIntExact(nrChunks);
        } catch (ArithmeticException e) {
            throw Message.builder().withSummary("Input data too large (too many rows)") //
                .addTextIssue(String.format( //
                    "Input data has %d rows. Resulting chunk size (%d) or chunk count (%d) would exceed %d)", //
                    nrRows, nrRowsPerChunk, nrChunks, Integer.MAX_VALUE)) //
                .addResolutions("Use smaller data", "Increase chunk size") //
                .build().orElseThrow().toInvalidSettingsException(e);
        }
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final LoopStartChunkNodeSettings settings) throws Exception {
        BufferedDataTable table = inData[0];
        long rowCount = table.size();

        final long totalChunkCount;
        final long nrRowsPerIteration;
        switch (settings.m_mode) {
            case NrOfChunks:
                totalChunkCount = Math.min(settings.m_nrOfChunks, rowCount);
                nrRowsPerIteration = (rowCount + totalChunkCount - 1) / totalChunkCount; // == Math.ceil
                break;
            case RowsPerChunk:
                nrRowsPerIteration = settings.m_nrRowsPerChunk;
                totalChunkCount = (rowCount + nrRowsPerIteration - 1) / nrRowsPerIteration; // == Math.ceil
                break;
            default:
                throw new KNIMEException("Unsupported mode: " + settings.m_mode);
        }

        if (m_iteration == 0) {
            assert getLoopEndNode() == null : "1st iteration but end node set";
            m_table = table;
            m_iterator = table.iterator();
            checkIntegerCasts(nrRowsPerIteration, totalChunkCount, rowCount);
        } else {
            assert getLoopEndNode() != null : "No end node set";
            assert table == m_table : "Input tables differ between iterations";
        }

        BufferedDataContainer cont = exec.createDataContainer(table.getSpec());
        for (int i = 0; i < nrRowsPerIteration && m_iterator.hasNext(); i++) {
            cont.addRowToTable(m_iterator.next());
        }
        cont.close();
        pushFlowVariableInt("currentIteration", (int)m_iteration);
        pushFlowVariableInt("maxIterations", (int)totalChunkCount);
        m_iteration++;
        return new BufferedDataTable[] {cont.getTable()};
    }

    @Override
    protected void onReset() {
        m_iteration = 0;
        if (m_iterator != null) {
            m_iterator.close();
        }
        m_iterator = null;
        m_table = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean terminateLoop() {
        boolean continueLoop = m_iterator == null || m_iterator.hasNext();
        return !continueLoop;
    }
}
