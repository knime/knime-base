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
 *   Nov 4, 2022 (Adrian): created
 */
package org.knime.base.node.preproc.table.cropper;

import java.util.stream.IntStream;

import org.knime.base.node.preproc.table.utils.PositionRange;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;

/**
 * NodeModel of the Table Cropper node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class TableCropperNodeModel extends WebUINodeModel<TableCropperSettings> {

    TableCropperNodeModel(final WebUINodeConfiguration config) {
        super(config, TableCropperSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final TableCropperSettings settings)
        throws InvalidSettingsException {
        var spec = inSpecs[0];

        return new DataTableSpec[]{createOutputSpec(spec, settings)};
    }

    private static DataTableSpec createOutputSpec(final DataTableSpec spec, final TableCropperSettings settings)
        throws InvalidSettingsException {
        var rearranger = new ColumnRearranger(spec);
        var colsToKeep = getColumnIndicesToKeep(spec, settings);
        rearranger.keepOnly(colsToKeep);
        return rearranger.createSpec();
    }

    private static int[] getColumnIndicesToKeep(final DataTableSpec spec, final TableCropperSettings settings)
        throws InvalidSettingsException {
        switch (settings.m_columnRangeMode) {
            case BY_NUMBER:
                return getColumnIndicesFromNumberRange(settings, spec.getNumColumns());
            case BY_NAME:
                return getColumnIndicesFromNameRange(spec, settings);
            default:
                throw new InvalidSettingsException(
                    String.format("The column selection mode '%s' is unknown.", settings.m_columnRangeMode));
        }
    }

    private static int[] getColumnIndicesFromNumberRange(final TableCropperSettings settings, final int numColumns)
        throws InvalidSettingsException {
        var positionRange = new PositionRange(settings.m_startColumnNumber,
            settings.m_startColumnCountFromEnd, settings.m_endColumnNumber, settings.m_endColumnCountFromEnd, "column")
                .resolve(numColumns);
        return IntStream.range((int)positionRange.getStart() - 1, (int)positionRange.getEnd()).toArray();
    }

    private static int[] getColumnIndicesFromNameRange(final DataTableSpec spec, final TableCropperSettings settings)
        throws InvalidSettingsException {
        int startColumnIndex = spec.findColumnIndex(settings.m_startColumnName);
        CheckUtils.checkSetting(startColumnIndex >= 0, "The provided table does not contain the start column ('%s').",
            settings.m_startColumnName);
        int endColumnIndex = spec.findColumnIndex(settings.m_endColumnName);
        CheckUtils.checkSetting(endColumnIndex >= 0, "The provided table does not contain the end column ('%s').",
            settings.m_endColumnName);
        CheckUtils.checkSetting(startColumnIndex <= endColumnIndex,
            "The start column must be positioned before the end column in the table.");
        return IntStream.range(startColumnIndex, endColumnIndex + 1).toArray();
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final TableCropperSettings settings) throws Exception {
        var table = inData[0];
        var colIndices = getColumnIndicesToKeep(table.getDataTableSpec(), settings);
        var slice = defineSlice(colIndices, table.size(), settings);
        exec.setMessage("Cropping table");
        var slicedTable =
            InternalTableAPI.slice(exec.createSubExecutionContext(settings.m_updateDomains ? 0.5 : 1.0), table, slice);
        if (settings.m_updateDomains) {
            var specWithNewDomain = recalculateDomain(slicedTable, exec.createSubProgress(0.5));
            slicedTable = exec.createSpecReplacerTable(slicedTable, specWithNewDomain);
        }
        return new BufferedDataTable[]{slicedTable};
    }

    private static Selection defineSlice(final int[] colIndices, final long numRows,
        final TableCropperSettings settings) throws InvalidSettingsException {
        var positionRange = new PositionRange(settings.m_startRowNumber, settings.m_startRowCountFromEnd,
            settings.m_endRowNumber, settings.m_endRowCountFromEnd, "row").resolve(numRows);
        return Selection.all()//
            .retainColumns(colIndices)//
            .retainRows(positionRange.getStart() - 1, positionRange.getEnd());
    }

    private static DataTableSpec recalculateDomain(final BufferedDataTable table, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        var domainCalculator = new DataTableDomainCreator(table.getDataTableSpec(), false);
        domainCalculator.updateDomain(table, exec);
        return domainCalculator.createSpec();
    }

    @Override
    protected void validateSettings(final TableCropperSettings settings) throws InvalidSettingsException {
        // create position ranges as they automatically validate the settings
        @SuppressWarnings("unused")
        PositionRange rowValidationPositionRange = new PositionRange(settings.m_startRowNumber,
            settings.m_startRowCountFromEnd, settings.m_endRowNumber, settings.m_endRowCountFromEnd, "row");

        @SuppressWarnings("unused")
        PositionRange columnValidationPositionRange = new PositionRange(settings.m_startColumnNumber,
            settings.m_startColumnCountFromEnd, settings.m_endColumnNumber, settings.m_endColumnCountFromEnd, "column");

    }

}
