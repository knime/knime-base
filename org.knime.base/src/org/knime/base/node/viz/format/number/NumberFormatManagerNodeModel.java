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
package org.knime.base.node.viz.format.number;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.data.property.ValueFormatModelNumber;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class NumberFormatManagerNodeModel extends WebUINodeModel<NumberFormatManagerNodeSettings> {

    /**
     * @param configuration
     * @param modelSettingsClass
     */
    NumberFormatManagerNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, NumberFormatManagerNodeSettings.class);
    }

    /**
     *
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final NumberFormatManagerNodeSettings modelSettings) throws InvalidSettingsException {
        var valueFormatHandler = handlerFor(modelSettings);
        var inSpec = inSpecs[0];
        String[] numberColumnNames = numberColumns(inSpec);
        var result = createOutputSpec(inSpec, modelSettings.m_columnsToFormat.getSelected(numberColumnNames, inSpec),
            valueFormatHandler);
        return new DataTableSpec[]{result};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final NumberFormatManagerNodeSettings modelSettings) throws Exception {
        final var in = inData[0];
        final var inSpec = in.getDataTableSpec();
        var valueFormatHandler = handlerFor(modelSettings);
        String[] numberColumnNames = numberColumns(inSpec);
        final var outputTableSpec = createOutputSpec(inSpec,
            modelSettings.m_columnsToFormat.getSelected(numberColumnNames, inSpec), valueFormatHandler);
        final var outputTable = outputTableSpec == inSpec ? in : exec.createSpecReplacerTable(in, outputTableSpec);
        return new BufferedDataTable[]{outputTable};
    }

    /**
     * @param inSpec to filter
     * @return the names of the columns that can be selected
     */
    private static String[] numberColumns(final DataTableSpec inSpec) {
        return inSpec.stream()//
                .filter(NumberFormatManagerNodeModel::isTargetColumn)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new);
    }

    /**
     * Translate from node settings to number formatter settings.
     * @param settings node settings
     * @return number formatter
     * @throws InvalidSettingsException
     */
    private static ValueFormatHandler handlerFor(final NumberFormatManagerNodeSettings settings)
        throws InvalidSettingsException {
        var formatter = NumberFormatter.builder()//
                .setMinimumDecimals(settings.m_minimumDecimals)//
                .setMaximumDecimals(settings.m_maximumDecimals)//
                .setGroupSeparator(settings.m_groupSeparator.getSeparator())//
                .setDecimalSeparator(settings.m_decimalSeparator.getSeparator())//
                .setAlwaysShowDecimalSeparator(settings.m_alwaysShowDecimalSeparator)
                .build();
        return new ValueFormatHandler(new ValueFormatModelNumber(formatter));
    }

    /**
     * @param in original spec. Non-null.
     * @param targetColumns names of the columns to attach format handler to. Non-null. Non-empty.
     * @param handler to attach. Non-null.
     * @return a new data table spec with the specified format manager attached to the given columns
     */
    private static DataTableSpec createOutputSpec(final DataTableSpec in, final String[] targetColumns,
        final ValueFormatHandler handler) {
        final var tableSpecCreator = new DataTableSpecCreator(in);
        for (String columnName : targetColumns) {
            final var columnSpecCreator = new DataColumnSpecCreator(in.getColumnSpec(columnName));
            columnSpecCreator.setValueFormatHandler(handler);
            final var outputColumnSpec = columnSpecCreator.createSpec();
            tableSpecCreator.replaceColumn(in.findColumnIndex(columnName), outputColumnSpec);
        }
        return tableSpecCreator.createSpec();

    }

    @Override
    protected void validateSettings(final NumberFormatManagerNodeSettings settings) throws InvalidSettingsException {
        handlerFor(settings);
    }

    /**
     * Determine whether the given column is an aggregatable column, i.e. a column compatible with {@link DoubleValue},
     * {@link IntValue}, or {@link LongValue}.
     *
     * @param column data column spec
     * @return {@code true} if the column can be aggregated, {@code false} otherwise
     */
    static boolean isTargetColumn(final DataColumnSpec column) {
        final var t = column.getType();
        return t.isCompatible(DoubleValue.class);
    }
}
