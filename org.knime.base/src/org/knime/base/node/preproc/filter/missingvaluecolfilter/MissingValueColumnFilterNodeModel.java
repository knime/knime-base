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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.missingvaluecolfilter;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The model for the missing value column filter which removes all columns with more missing values than a certain
 * percentage or absolute number.
 *
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class MissingValueColumnFilterNodeModel extends WebUINodeModel<MissingValueColumnFilterNodeSettings> {

    MissingValueColumnFilterNodeModel(final WebUINodeConfiguration config) {
        super(config, MissingValueColumnFilterNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final MissingValueColumnFilterNodeSettings modelSettings) throws CanceledExecutionException {
        final var inputTable = inData[0];
        final var dataTableSpec = inputTable.getDataTableSpec();

        // consider only columns currently present in the table spec (same as before, where
        // "NameFilterConfiguration.FilterResult#getIncludes()" was used and "orphaned" column names were not retrieved)
        final var selected =
            modelSettings.m_columnFilter.getNonMissingSelected(dataTableSpec.getColumnNames(), dataTableSpec);

        if (selected.length == 0) {
            return new BufferedDataTable[]{inputTable};
        }
        final var selectedIndices = inputTable.getSpec().columnsToIndices(selected);

        // determine possible early stopping criterion
        final long rowCount = inputTable.size();
        final var mode = modelSettings.m_removeColumnsBy;
        final long minimum = switch (mode) {
            case SOME -> 1;
            // can stop when we have seen at least the configured number
            case NUMBER -> modelSettings.m_number;
            // these two cannot stop early
            case ONLY, PERCENTAGE -> rowCount;
        };

        // in case we stop early, this number will be the lower bound for the number of missing values
        final var missingCount = new long[selectedIndices.length];
        long processedRows = 0;

        for (final DataRow row : inputTable) {
            for (var i = 0; i < selectedIndices.length; i++) {
                final var columnIdx = selectedIndices[i];
                if (missingCount[i] <= minimum && row.getCell(columnIdx).isMissing()) {
                    missingCount[i]++;
                }
            }
            processedRows++;
            exec.setProgress(1.0 * processedRows / rowCount);
            if (Arrays.stream(missingCount).allMatch(count -> count >= minimum)) {
                exec.setProgress(1.0);
                break;
            }
        }

        final var r = new ColumnRearranger(dataTableSpec);
        r.remove(switch (mode) {
            case SOME, ONLY, NUMBER -> IntStream.range(0, selectedIndices.length) //
                .filter(idx -> missingCount[idx] >= minimum) //
                .map(idx -> selectedIndices[idx]).toArray();
            case PERCENTAGE -> filterByPercentage(selectedIndices, missingCount, rowCount, modelSettings.m_percentage);
        });

        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inputTable, r, exec)};
    }

    /**
     * Threshold filter the selected indices whether they exceed the given percentage or not.
     *
     * @param selectedIndices indices to filter
     * @param missingCount number of missing values (lower bound) in each column
     * @param rowCount total row count
     * @param percentage threshold
     * @return filtered array with columns exceeding or meeting the given threshold of missing values
     */
    private static int[] filterByPercentage(final int[] selectedIndices, final long[] missingCount, final long rowCount,
        final double percentage) {
        // map counts to percentage values
        final var percentages =
            Arrays.stream(missingCount).mapToDouble(count -> 1.0 * count / rowCount * 100).toArray();
        // identify column indices from selected indices which meet threshold
        final var threshold = percentage;
        return IntStream.range(0, selectedIndices.length).filter(idx -> percentages[idx] >= threshold)
            .map(idx -> selectedIndices[idx]).toArray();
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final MissingValueColumnFilterNodeSettings modelSettings) throws InvalidSettingsException {
        // output spec can only be determined after we had a look at the data
        return null;
    }

    @Override
    protected void validateSettings(final MissingValueColumnFilterNodeSettings settings)
        throws InvalidSettingsException {
        if (settings.m_percentage < 0) {
            throw new InvalidSettingsException("Percentage must not be negative.");
        }
        if (settings.m_percentage > 100) {
            throw new InvalidSettingsException("Percentage must not exceed 100.");
        }
    }
}
