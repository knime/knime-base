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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.util.cache;

import java.io.IOException;
import java.util.function.LongSupplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.mutable.MutableLong;
import org.knime.base.node.util.cache.CacheNodeSettings.ColumnDomains;
import org.knime.base.node.util.cache.CacheNodeSettings.CopyImplementation;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.v2.RowContainer;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowWriteCursor;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.workflow.WorkflowTableBackendSettings;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;


/**
 * Creates a model that caches the entire input data.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("restriction")
final class CacheNodeModel extends WebUINodeModel<CacheNodeSettings> {

    CacheNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, CacheNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data, final ExecutionContext exec,
        final CacheNodeSettings modelSettings) throws Exception {
        final DataContainerSettings dcSettings = DataContainerSettings.builder() //
                .withCheckDuplicateRowKeys(false) //
                .withInitializedDomain(modelSettings.m_domains == ColumnDomains.RETAIN) //
                .withDomainUpdate(modelSettings.m_domains == ColumnDomains.COMPUTE) //
                .build();
        final boolean isRowBackend = WorkflowTableBackendSettings.getTableBackendForCurrentContext().getClass()
            .equals(BufferedTableBackend.class);
        final CopyImplementation impl;
        if (modelSettings.m_implementation == CopyImplementation.AUTO) {
            impl = isRowBackend ? CopyImplementation.ROW_BASED_BY_ROW : CopyImplementation.COLUMNAR_BY_ROW;
        } else {
            impl = modelSettings.m_implementation;
        }
        final BufferedDataTable copy = switch (impl) {
            case ROW_BASED_BY_ROW -> rowBackendFullRowCopy(data[0], exec, dcSettings);
            case COLUMNAR_BY_ROW -> colBackendFullRowCopy(data[0], exec, dcSettings);
            case COLUMNAR_BY_CELL -> colBackendCellByCellCopy(data[0], exec, dcSettings);
            default -> throw new IllegalStateException("Unexpected value: " + modelSettings.m_implementation);
        };
        return new BufferedDataTable[] {copy};
    }

    private static BufferedDataTable colBackendFullRowCopy(final BufferedDataTable data, final ExecutionContext exec,
        final DataContainerSettings dcSettings) throws CanceledExecutionException, IOException {
        final long totalCount = data.size();
        final MutableLong row = new MutableLong(1);
        final UnaryOperator<StringBuilder> progressFractionBuilder =
            progressFractionBuilder(newProgressNumberFormat(), row::longValue, totalCount);
        try (RowContainer con = exec.createRowContainer(data.getDataTableSpec(), dcSettings);
                RowCursor readCursor = data.cursor();
                RowWriteCursor writeCursor = con.createCursor()) {
            for (; readCursor.canForward(); row.increment()) {
                exec.setProgress(row.longValue() / (double)totalCount,
                    () -> progressFractionBuilder.apply(new StringBuilder("Caching row ")).toString());
                exec.checkCanceled();
                writeCursor.commit(readCursor.forward());
            }
            return con.finish();
        }
    }

    // TODO (TP): This is identical to colBackendFull --> Remove
    private static BufferedDataTable colBackendCellByCellCopy(final BufferedDataTable data, final ExecutionContext exec,
        final DataContainerSettings dcSettings) throws CanceledExecutionException, IOException {
        try (RowContainer con = exec.createRowContainer(data.getDataTableSpec(), dcSettings);
                RowCursor readCursor = data.cursor();
                RowWriteCursor writeCursor = con.createCursor()) {
            final long totalCount = data.size();
            final MutableLong row = new MutableLong(1);
            final UnaryOperator<StringBuilder> progressFractionBuilder =
                progressFractionBuilder(newProgressNumberFormat(), row::longValue, totalCount);
            for (; readCursor.canForward(); row.increment()) {
                exec.setProgress(row.longValue() / (double)totalCount,
                    () -> progressFractionBuilder.apply(new StringBuilder("Caching row ")).toString());
                exec.checkCanceled();
                writeCursor.commit(readCursor.forward());
            }
            return con.finish();
        }
    }

    // TODO (TP): The special case could be handled in RowWrite.commit()? --> Remove?
    private static BufferedDataTable rowBackendFullRowCopy(final BufferedDataTable data,
        final ExecutionContext exec, final DataContainerSettings dcSettings) throws CanceledExecutionException {
        // it writes only the cells that are "visible" in the input table
        // think of one of the wrappers, e.g. the column filter that
        // hides 90% of the columns. Any iterator will nevertheless instantiate
        // also the cells in the hidden columns and thus make the iteration
        // slow.
        BufferedDataContainer con = exec.createDataContainer(data.getDataTableSpec(), dcSettings);
        final long totalCount = data.size();
        final MutableLong row = new MutableLong(1);
        final UnaryOperator<StringBuilder> progressFractionBuilder =
            progressFractionBuilder(newProgressNumberFormat(), row::longValue, totalCount);
        try (CloseableRowIterator it = data.iterator()) {
            for (; it.hasNext(); row.increment()) {
                final DataRow next = it.next();
                exec.setProgress(row.longValue() / (double)totalCount,
                    () -> progressFractionBuilder.apply(new StringBuilder("Caching row ")).toString());
                exec.checkCanceled();
                con.addRowToTable(next);
            }
        } finally {
            con.close();
        }
        return con.getTable();
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final CacheNodeSettings modelSettings)
        throws InvalidSettingsException {
        return inSpecs;
    }

    private static NumberFormatter newProgressNumberFormat() {
        try {
            return NumberFormatter.builder().setGroupSeparator(",").build();
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Pattern matching a single numeric digit. */
    private static final Pattern ANY_DIGIT = Pattern.compile("\\d");

    /** Space character that's exactly as wide as a single digit. */
    private static final String FIGURE_SPACE = "\u2007";

    /**
     * Creates a function that adds a nicely formatted, padded fraction of the form {@code " 173/2065"} to a given
     * {@link StringBuilder} that reflects the current value of the given supplier {@code currentValue}. The padding
     * with space characters tries to minimize jumping in the UI.
     *
     * @param numFormat number format for the numerator and denominator
     * @param currentValue supplier for the current numerator
     * @param total fixed denominator
     * @return function that modified the given {@link StringBuilder} and returns it for convenience
     */
    private static UnaryOperator<StringBuilder> progressFractionBuilder(final NumberFormatter numFormat,
            final LongSupplier currentValue, final long total) {
        // only computed once
        final var totalStr = numFormat.format(total);
        final var paddingStr = ANY_DIGIT.matcher(totalStr).replaceAll(FIGURE_SPACE).replace(',', ' ');

        return sb -> {
            // computed every time a progress message is requested
            final var currentStr = numFormat.format(currentValue.getAsLong());
            final var padding = paddingStr.substring(0, Math.max(totalStr.length() - currentStr.length(), 0));
            return sb.append(padding).append(currentStr).append("/").append(totalStr);
        };
    }

}
