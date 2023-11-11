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
 *   Nov 11, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.duplicates;

import org.knime.base.node.preproc.duplicates.DuplicateRowFilterDialogSettings.RowSelection;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.EnumArgumentType;
import org.knime.core.node.func.ListArgumentType;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.SimpleNodeFunc;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;

/**
 * NodeFunc that makes the Duplicate Row Filter available to K-AI's build mode.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class DuplicateRowFilterNodeFunc implements SimpleNodeFunc {

    private static final String DECISION_COLUMN = "decision_column";

    private static final String ROW_SELECTION = "row_selection_method";

    private static final String DETECTION_COLUMNS = "detection_columns";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var tableSpec = (DataTableSpec)inputSpecs[0];
        var detectionColumns = arguments.getStringArray(DETECTION_COLUMNS);
        checkAllContained(tableSpec, detectionColumns);
        var dupRowFilterSettings = new DuplicateRowFilterDialogSettings();
        dupRowFilterSettings.m_consideredColumns = new ColumnFilter(detectionColumns);
        var rowSelection = getRowSelection(arguments);
        dupRowFilterSettings.m_rowSelectionType = rowSelection;
        if (requiresDecisionColumn(rowSelection)) {
            var decisionColumn = arguments.getString(DECISION_COLUMN, null);
            validateDecisionColumn(tableSpec, rowSelection, decisionColumn);
            dupRowFilterSettings.m_selectedColumn = decisionColumn;
        }
        DefaultNodeSettings.saveSettings(DuplicateRowFilterDialogSettings.class, dupRowFilterSettings, settings);
    }

    private static void validateDecisionColumn(final DataTableSpec tableSpec, final RowSelection rowSelection,
        final String decisionColumn) throws InvalidSettingsException {
        CheckUtils.checkSetting(decisionColumn != null, "The %s '%s' requires a %s to be selected.", ROW_SELECTION,
            rowSelection.name(), DECISION_COLUMN);
        if (tableSpec != null) {
            CheckUtils.checkSetting(tableSpec.containsName(DECISION_COLUMN),
                "The %s '%s' does not exist in the input table.", DECISION_COLUMN, decisionColumn);
        }
    }

    private static boolean requiresDecisionColumn(final RowSelection rowSelection) {
        return rowSelection == RowSelection.MINIMUM || rowSelection == RowSelection.MAXIMUM;
    }

    private static RowSelection getRowSelection(final NodeSettingsRO arguments) throws InvalidSettingsException {
        var rowSelection = arguments.getString(ROW_SELECTION);
        try {
            return RowSelection.valueOf(rowSelection.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(
                "The %s method '%s' is not supported.".formatted(ROW_SELECTION, rowSelection));
        }
    }

    private static void checkAllContained(final DataTableSpec tableSpec, final String[] detectionColumns)
        throws InvalidSettingsException {
        if (tableSpec != null) {
            for (var col : detectionColumns) {
                CheckUtils.checkSetting(tableSpec.containsName(col),
                    "The specified detection column '%s' does not exist in the input table.", col);
            }
        }
    }

    @Override
    public NodeFuncApi getApi() {

        return NodeFuncApi.builder("filter_duplicate_rows")
            .withDescription("Allows to filter out duplicate rows from the input table.")//
            .withInputTable("table", "The table to rid of duplicates.")//
            .withOutputTable("filtered_table", "The table without duplicate rows.")//
            .withArgument(DETECTION_COLUMNS, "The columns used to detect duplicates.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))//
            .withArgument(ROW_SELECTION,
                """
                        Which row to keep in case of a match.
                        Valid values are %s.
                        In case of %s and %s, the row with the min or max value in the decision_column is kept.
                            """.formatted(EnumArgumentType.create(RowSelection.class), RowSelection.MINIMUM,
                    RowSelection.MAXIMUM),
                EnumArgumentType.create(RowSelection.class))//
            .withOptionalStringArgument(DECISION_COLUMN, """
                    The column that is used to decide which row to keep in case %s is set to %s or %s.
                    Can be set to None for the other row selection methods.
                        """.formatted(ROW_SELECTION, RowSelection.MINIMUM, RowSelection.MAXIMUM)).build();
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return DuplicateRowFilterNodeFactory.class;
    }

}
