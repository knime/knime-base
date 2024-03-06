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
 *   21 Dec 2022 (ivan): created
 */
package org.knime.base.node.viz.format.number;

import java.util.stream.Stream;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class NumberFormatManagerNodeSettings implements DefaultNodeSettings {

    // Types

    enum GroupSeparator {
            @Label("Thin space")
            THIN_SPACE("\u2009"), //
            @Label("Dot")
            DOT("."), //
            @Label("Comma")
            COMMA(","), //
            @Label("Apostrophe")
            APOSTROPHE("'"), //
            @Label("Underscore")
            UNDERSCORE("_"), //
            @Label("None")
            NONE("");

        private final String m_separator;

        GroupSeparator(final String separator) {
            m_separator = separator;
        }

        String getSeparator() {
            return m_separator;
        }
    }

    enum DecimalSeparator {
            @Label("Dot")
            DOT("."), @Label("Comma")
            COMMA(",");

        private final String m_separator;

        DecimalSeparator(final String separator) {
            m_separator = separator;
        }

        String getSeparator() {
            return m_separator;
        }
    }

    // Layout

    interface DialogLayout {

        @Section(title = "Columns")
        interface ColumnSelection {
        }

        @Section(title = "Digits")
        @After(DialogLayout.ColumnSelection.class)
        interface Digits {
            @HorizontalLayout
            interface MinMax {
            }
        }

        @Section(title = "Separators")
        @After(DialogLayout.Digits.class)
        interface Separators {
            @HorizontalLayout
            interface MinMaxDigitsGroup {
            }
        }

        @Section(title = "Display")
        @After(DialogLayout.Separators.class)
        interface Display {
        }

    }

    // Settings

    @Widget(title = "Columns to format", description = """
            Select the columns to attach the selected format to.
            This does not change the data, only the way the values in the column are displayed in views.
            """)
    @ChoicesWidget(choices = NumberColumns.class)
    @Layout(DialogLayout.ColumnSelection.class)
    ColumnFilter m_columnsToFormat;

    @Widget(title = "Minimum number of decimals", description = """
            The formatted number will show trailing zeros until the requested number of decimals are reached.
            For instance, <tt>1.2</tt> with 2 decimals is shown as <tt>1.20</tt>.
            """)
    @NumberInputWidget(min = 0)
    @Layout(DialogLayout.Digits.MinMax.class)
    int m_minimumDecimals;

    @Widget(title = "Maximum number of decimals", description = """
            The formatted number is rounded to the given number of decimals.
            For instance, <tt>1.2599</tt> with 2 decimals is shown as <tt>1.26</tt>.
            """)
    @NumberInputWidget(min = 0)
    @Layout(DialogLayout.Digits.MinMax.class)
    int m_maximumDecimals = 3;

    /** Integer group separator. */
    @Widget(title = "Group separator", description = """
            The separator between groups of integer digits.
            For instance, <tt>1,000,000</tt> uses a comma and <tt>1'000'000</tt>
             uses an apostrophe as group separator.
            """)
    @Layout(DialogLayout.Separators.class)
    GroupSeparator m_groupSeparator = GroupSeparator.THIN_SPACE;

    @Widget(title = "Decimal separator", description = "The decimal separator to use.")
    @Layout(DialogLayout.Separators.class)
    DecimalSeparator m_decimalSeparator = DecimalSeparator.DOT;

    @Widget(title = "Always show decimal separator", description = """
            Whether to always show the decimal separator, even if no fractional digits are present.
            If enabled, output like <tt>1.</tt> can be generated.
            If disabled, the same input would be shown as <tt>1</tt>.
            """)
    @Layout(DialogLayout.Separators.class)
    boolean m_alwaysShowDecimalSeparator;

    @Widget(title = "Alignment suggestion", description = "Specify how to align the number.")
    @ValueSwitchWidget
    @Persist(optional = true)
    @Layout(DialogLayout.Display.class)
    AlignmentSuggestionOption m_alignmentSuggestion = AlignmentSuggestionOption.RIGHT;

    // Utility

    NumberFormatManagerNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_columnsToFormat = ColumnFilter.createDefault(NumberColumns.class, ctx);
    }

    NumberFormatManagerNodeSettings() {
        // required by framework for serialization/deserialization
    }

    static final class NumberColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(NumberFormatManagerNodeModel::isTargetColumn)//
                .toArray(DataColumnSpec[]::new);
        }

    }

}
