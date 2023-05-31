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

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Range Filter node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstany, Germany
 */
@SuppressWarnings("restriction")
final class TableCropperSettings implements DefaultNodeSettings {

    /**
     * Constructor for auto-configure.
     *
     * @param context of the creation
     */
    TableCropperSettings(final SettingsCreationContext context) {
        var spec = context.getDataTableSpecs()[0];
        if (spec != null && spec.getNumColumns() > 0) {
            m_startColumnName = spec.getColumnSpec(0).getName();
            m_endColumnName = spec.getColumnSpec(spec.getNumColumns() - 1).getName();
            m_endColumnNumber = spec.getNumColumns(); // set end column to last column in table.
        }
    }

    /**
     * Constructor for deserialization.
     */
    TableCropperSettings() {
    }

    @Section(title = "Columns")
    interface ColumnsSection {
    }

    @Widget(title = "Column range mode",
        description = "Specify the range of columns included in the output by defining the start and end column. "
            + "The options for specifiying the start and end columns are:" //
            + "<ul>" //
            + "<li><b>By name</b>: Specify the name of the start and end columns to include.</li>"//
            + "<li><b>By number</b>: Specify the start and end column by their number in the table. "//
            + "The first column has number 1.</li>"//
            + "</ul>")
    @ValueSwitchWidget
    @Signal(condition = ColumnRangeMode.IsByName.class)
    @Layout(ColumnsSection.class)
    ColumnRangeMode m_columnRangeMode = ColumnRangeMode.BY_NAME;

    // TODO: UIEXT-1007 migrate String to ColumnSelection

    @Widget(title = "Start column", description = "Select the first column to include.")
    @ChoicesWidget(choices = AllColumns.class)
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.SHOW)
    @Layout(ColumnsSection.class)
    String m_startColumnName;

    @Widget(title = "End column (inclusive)", description = "Select the last column to include.")
    @ChoicesWidget(choices = AllColumns.class)
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.SHOW)
    @Layout(ColumnsSection.class)
    String m_endColumnName;

    @Widget(title = "Start column number",
        description = "Select the first column to include (the first column of the table has number 1).")
    @NumberInputWidget(min = 1)
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.HIDE)
    @Layout(ColumnsSection.class)
    int m_startColumnNumber = 1;

    @Widget(title = "Start counting columns from the end of the table",
        description = "If selected, the start column will be counted from the end of the table.")
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.HIDE)
    @Layout(ColumnsSection.class)
    boolean m_startColumnCountFromEnd;

    @Widget(title = "End column number (inclusive)", description = "Select the last column to include.")
    @NumberInputWidget(min = 1)
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.HIDE)
    @Layout(ColumnsSection.class)
    int m_endColumnNumber = 1;

    @Widget(title = "Start counting columns from the end of the table",
        description = "If selected, the end column will be counted from the end of the table.")
    @Effect(signals = ColumnRangeMode.IsByName.class, type = EffectType.HIDE)
    @Layout(ColumnsSection.class)
    boolean m_endColumnCountFromEnd;

    @Section(title = "Rows")
    @After(ColumnsSection.class)
    @Before(OutputSection.class)
    interface RowsSection {
    }

    @Widget(title = "Start row number",
        description = "Select the first row to include (the first row of the table has number 1).")
    @NumberInputWidget(min = 1)
    @Layout(RowsSection.class)
    long m_startRowNumber = 1;

    @Widget(title = "Start counting rows from the end of the table",
        description = "If selected, the start row will be counted from the end of the table.")
    @Layout(RowsSection.class)
    boolean m_startRowCountFromEnd;

    @Widget(title = "End row number (inclusive)", description = "Select the last row to include.")
    @NumberInputWidget(min = 1)
    @Layout(RowsSection.class)
    long m_endRowNumber = 1;

    @Widget(title = "Start counting rows from the end of the table",
        description = "If selected, the end row will be counted from the end of the table.")
    @Layout(RowsSection.class)
    boolean m_endRowCountFromEnd;

    @Section(title = "Output", advanced = true)
    @After(RowsSection.class)
    interface OutputSection {
    }

    @Widget( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table." //
    )
    @Persist(optional = true)
    @Layout(OutputSection.class)
    boolean m_updateDomains;

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec != null ? spec.getColumnNames() : new String[0];
        }

    }

    enum ColumnRangeMode {
            @Label("By name") //
            BY_NAME, //
            @Label("By number") //
            BY_NUMBER;

        static class IsByName extends OneOfEnumCondition<ColumnRangeMode> {

            @Override
            public ColumnRangeMode[] oneOf() {
                return new ColumnRangeMode[]{BY_NAME};
            }

        }
    }

}
