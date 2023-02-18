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
 *   Aug 16, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.knime.base.node.preproc.sorter.dialog.DynamicSorterPanel;
import org.knime.base.node.util.SortKeyItem;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

/**
 * Node model for the Top K Selector node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TopKSelectorNodeModel extends NodeModel {

    static final int IN_DATA = 0;

    /**
     * The key for the IncludeList in the NodeSettings.
     */
    public static final String INCLUDELIST_KEY = "columns";

    /**
     * The key for the Sort Order Array in the NodeSettings.
     */
    public static final String SORTORDER_KEY = "order";

    /**
     * The key for the Alphanumeric Comparison in the node settings.
     * @since 4.7
     */
    static final String ALPHANUMCOMP_KEY = "alphaNumStringComp";

    private final TopKSelectorSettings m_settings = new TopKSelectorSettings();

    TopKSelectorNodeModel() {
        super(1, 1);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final var sk = m_settings.getSortKey();
        CheckUtils.checkSetting(sk != null && !sk.isEmpty(),
            "No columns have been specified to select the top rows of. Set them in the dialog.");
        final DataTableSpec dts = inSpecs[IN_DATA];
        final List<String> missing = SortKeyItem.getMissing(sk, dts, TopKSelectorNodeModel::isRowKey);
        CheckUtils.checkSetting(missing.isEmpty(), String.format("The columns %s are configured but no longer exist.",
            ConvenienceMethods.getShortStringFrom(missing, 3)));
        return inSpecs.clone();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final var sortKey = m_settings.getSortKey();
        CheckUtils.checkSetting(sortKey != null && !sortKey.isEmpty(),
            "No columns have been specified to select the top rows of. Set them in the dialog.");

        final BufferedDataTable table = inData[IN_DATA];
        if (table.size() < m_settings.getK()) {
            setWarningMessage(String.format(
                "The input table has fewer rows (%s) than the specified k. Make sure the input has at least %s rows.",
                table.size(), m_settings.getK()));
        }
        final boolean missingsLast = m_settings.isMissingToEnd();
        final var dts = table.getDataTableSpec();
        final var rc = SortKeyItem.toRowComparator(dts, sortKey, missingsLast, TopKSelectorNodeModel::isRowKey);
        final TopKSelector elementSelector = createElementSelector(rc);
        final var outputOrder = m_settings.getOutputOrder();
        final OrderPreprocessor preprocessor = outputOrder.getPreprocessor();
        final BufferedDataTable execTable = preprocessor.preprocessSelectionTable(table,
            exec.createSubExecutionContext(preprocessor.getProgressRequired()));
        fillElementSelector(exec.createSubExecutionContext(0.9 - preprocessor.getProgressRequired()), execTable,
            elementSelector);
        final Collection<DataRow> topK =
            outputOrder.getPostprocessor().postprocessSelection(elementSelector.getTopK(), rc);
        final BufferedDataTable outputTable = createOutputTable(topK, dts, exec.createSubExecutionContext(0.1));
        return new BufferedDataTable[]{outputTable};
    }

    private static boolean isRowKey(final String colName) {
        return DynamicSorterPanel.ROWKEY.getName().equals(colName);
    }

    private static BufferedDataTable createOutputTable(final Collection<DataRow> topK, final DataTableSpec spec,
        final ExecutionContext exec) throws CanceledExecutionException {
        final BufferedDataContainer container = exec.createDataContainer(spec);
        final double nrElements = topK.size();
        final Iterator<DataRow> iterator = topK.iterator();
        for (long i = 1; iterator.hasNext(); i++) {
            exec.checkCanceled();
            final DataRow row = iterator.next();
            final long iFinal = i;
            exec.setProgress(i / nrElements,
                () -> String.format("Writing row %s to output (%s of %s)", row.getKey(), iFinal, (long)nrElements));
            container.addRowToTable(row);
        }
        container.close();
        return container.getTable();
    }

    private static void fillElementSelector(final ExecutionContext exec, final BufferedDataTable table,
        final TopKSelector elementSelector) throws CanceledExecutionException {
        final double nrRows = table.size();
        try (CloseableRowIterator iterator = table.iterator()) {
            for (long i = 1; iterator.hasNext(); i++) {
                exec.checkCanceled();
                final DataRow row = iterator.next();
                final long iFinal = i;
                exec.setProgress(i / nrRows,
                    () -> String.format("Consuming row %s (%s of %s)", row.getKey(), iFinal, (long)nrRows));
                elementSelector.consume(row);
            }
        }
    }

    private TopKSelector createElementSelector(final Comparator<DataRow> comparator) {
        final Comparator<DataRow> inverted = (i, j) -> -comparator.compare(i, j);
        final TopKMode topKMode = TopKMode.getTopKModeByText(m_settings.getTopKMode());
        if (m_settings.getK() == 1 && topKMode == TopKMode.TOP_K_ROWS) {
            return new TopSelector(inverted);
        } else if (topKMode == TopKMode.TOP_K_ALL_ROWS_W_UNIQUE) {
            return new HeapTopKUniqueRowsSelector(inverted, m_settings.getK());
        } else {
            return new HeapTopKSelector(inverted, m_settings.getK());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

}
