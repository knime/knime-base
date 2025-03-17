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
 *   15 Aug 2023 (jasper): created
 */
package org.knime.base.node.viz.format.string;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.viz.format.string.StringFormatManagerNodeSettings.CustomStringReplacementOption;
import org.knime.base.node.viz.format.string.StringFormatManagerNodeSettings.WrapLinesOnDemandOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Model for the String Formatter node that attaches a {@linkplain StringFormatter} to selected data columns
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class StringFormatManagerNodeModel extends WebUINodeModel<StringFormatManagerNodeSettings> {

    private ValueFormatHandler m_handler;

    StringFormatManagerNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, StringFormatManagerNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final StringFormatManagerNodeSettings modelSettings) throws InvalidSettingsException {
        // We can already instantiate the value format handler because it only depends on the settings, not on the data
        // That will also take care of settings validation
        m_handler = new ValueFormatHandler(createFormatter(modelSettings));
        final var inSpec = inSpecs[0];
        final var targetCols = modelSettings.m_columnsToFormat.filter(stringColumns(inSpec));
        final var result = createOutputSpec(inSpec, targetCols, m_handler);
        return new DataTableSpec[]{result};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final StringFormatManagerNodeSettings modelSettings) throws Exception {
        final var in = inData[0];
        final var inSpec = in.getDataTableSpec();
        final var targetCols = modelSettings.m_columnsToFormat.filter(stringColumns(inSpec));
        final var resultSpec = createOutputSpec(inSpec, targetCols, m_handler);
        final var result = resultSpec == inSpec ? in : exec.createSpecReplacerTable(in, resultSpec);
        return new BufferedDataTable[]{result};
    }

    /**
     * Attaches the new format handler to the column spec of the selected columns
     *
     * @param in the input table spec
     * @param targetColumns the selected columns
     * @param handler the {@linkplain ValueFormatHandler} that should be attached to the columns
     * @return the modified table spec
     */
    private static DataTableSpec createOutputSpec(final DataTableSpec in, final String[] targetColumns,
        final ValueFormatHandler handler) {
        final var tableSpecCreator = new DataTableSpecCreator(in);
        for (var columnName : targetColumns) {
            final var columnSpec = in.getColumnSpec(columnName);
            if (columnSpec == null || !isStringCell(columnSpec)) {
                continue; // skip columns that do not exist anymore
            }
            final var columnSpecCreator = new DataColumnSpecCreator(columnSpec);
            columnSpecCreator.setValueFormatHandler(handler);
            final var outputColumnSpec = columnSpecCreator.createSpec();
            tableSpecCreator.replaceColumn(in.findColumnIndex(columnName), outputColumnSpec);
        }
        return tableSpecCreator.createSpec();
    }

    /**
     * Return all string-cell columns from the input spec
     *
     * @param inSpec
     * @return array of string column names
     */
    private static List<DataColumnSpec> stringColumns(final DataTableSpec inSpec) {
        return inSpec.stream()//
            .filter(StringFormatManagerNodeModel::isStringCell)// only support string cells
            .toList();
    }

    static boolean isStringCell(final DataColumnSpec spec) {
        return StringCell.TYPE.equals(spec.getType());
    }

    /**
     * Create a {@linkplain StringFormatter} instance from the modelSettings
     *
     * @param modelSettings
     * @return a string formatter instance
     * @throws InvalidSettingsException if the settings are not valid (e.g. max chars < last chars)
     */
    private static StringFormatter createFormatter(final StringFormatManagerNodeSettings modelSettings)
        throws InvalidSettingsException {
        final var settings = new StringFormatter.Settings(//
            modelSettings.m_nFirstChars, //
            modelSettings.m_nLastChars, //
            modelSettings.m_wrapLinesOnDemand != WrapLinesOnDemandOption.NO, //
            modelSettings.m_wrapLinesOnDemand == WrapLinesOnDemandOption.ANYWHERE, //
            modelSettings.m_alignmentSuggestion, modelSettings.m_replaceNewlineAndCarriageReturn, //
            modelSettings.m_replaceNonPrintableCharacters, //
            modelSettings.m_replaceEmptyString == CustomStringReplacementOption.CUSTOM
                ? Optional.of(modelSettings.m_emptyStringReplacement) : Optional.empty(),
            modelSettings.m_linkLinksAndEmails);
        return StringFormatter.fromSettings(settings);
    }

}
