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

import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings for the {@link StringFormatManagerNodeModel}
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // WebUI is not API yet / init settings with "= false" for verbosity.
public final class StringFormatManagerNodeSettings implements DefaultNodeSettings {

    // Layout

    interface DialogLayout {
        @Section(title = "Column Selection")
        interface Columns {
        }

        @Section(title = "Format")
        @After(Columns.class)
        interface Format {
            @HorizontalLayout
            interface FirstLast {
            }

            @After(FirstLast.class)
            interface TheRest {
            }
        }

        @Section(title = "Special values")
        @After(Format.class)
        interface SpecialValues {
            @HorizontalLayout
            interface ReplaceEmptyString {
            }
        }

        @Section(title = "Interaction")
        @After(SpecialValues.class)
        interface Interaction {
        }
    }

    interface Signals {
        interface ReplaceEmptyStrings {
        }
    }

    // Settings

    @Widget(title = "Columns to format", description = """
            Select the columns to attach the selected format to.
            This does not change the data, but only the way the strings are being displayed in views.
            """)
    @ChoicesWidget(choices = StringColumns.class)
    @Layout(DialogLayout.Columns.class)
    ColumnFilter m_columnsToFormat;

    @Widget(title = "Show first characters", description = """
            Select the number of characters that should be retained at the <b>start</b> of the string. \
            If a string's length is longer than the permitted length, it will be truncated. \
            This does not change the data, but only the way the strings are being displayed in views.
            """)
    @NumberInputWidget(min = 0)
    @Layout(DialogLayout.Format.FirstLast.class)
    int m_nFirstChars = 500;

    @Widget(title = "Show last characters", description = """
            Select the number of characters that should be retained at the <b>end</b> of the string. \
            If a string's length is longer than the permitted length, it will be truncated. \
            This does not change the data, but only the way the strings are being displayed in views.
            """)
    @NumberInputWidget(min = 0)
    @Layout(DialogLayout.Format.FirstLast.class)
    int m_nLastChars = 5;

    enum WrapLinesOnDemandOption {
            @Label("No")
            NO, //
            @Label("Anywhere")
            ANYWHERE, //
            @Label("Between words")
            BETWEEN_WORDS;
    }

    @Widget(title = "Wrap lines on demand", description = """
            Determine how to wrap a string when it is too long to fit the width of the view:
            <ul>
                <li><i>Anywhere</i>: The string is wrapped anywhere on demand, also in the middle of a word.</li>
                <li><i>Between words</i>: The string is wrapped only between words, e.g. at white space.</li>
                <li><i>No</i>: The string isn't wrapped and long strings will overflow the view width.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Layout(DialogLayout.Format.TheRest.class)
    WrapLinesOnDemandOption m_wrapLinesOnDemand = WrapLinesOnDemandOption.NO;

    // TODO reword: change "replace .. with" to "display .. as" ?

    @Widget(title = "Show line break and carriage return as symbols", description = """
            If checked, line break (\\n) and carriage return (\\r) are displayed as symbols. \
            Enabling this will always show a single-line string unless <i>Wrap  lines on demand</i> is enabled.
            """)
    @Layout(DialogLayout.Format.TheRest.class)
    boolean m_replaceNewlineAndCarriageReturn = false;

    @Widget(title = "Show other non-printable characters as symbols", description = """
            If checked, non-printable symbols like a tabulator or non-break space will be displayed with a placeholder \
            symbol (\ufffd). Enable this to make any non-standard control characters in your strings visible.
            """)
    @Layout(DialogLayout.Format.TheRest.class)
    boolean m_replaceNonPrintableCharacters = true;

    enum CustomStringReplacementOption {
            @Label("Blank")
            BLANK, //
            @Label("Custom string")
            CUSTOM
    }

    static class IsCustomCondition extends OneOfEnumCondition<CustomStringReplacementOption> {
        @Override
        public CustomStringReplacementOption[] oneOf() {
            return new CustomStringReplacementOption[]{CustomStringReplacementOption.CUSTOM};
        }
    }

    @Widget(title = "Show empty string as", description = """
            Determine how to display empty strings. \
            This does not change the underlying data, but just how an empty string is shown in views.
            <ul>
                <li><i>Blank</i>: The empty string will be shown as blank, this is the default.</li>
                <li><i>Custom string</i>: A custom string can be defined that will be shown instead of a blank cell. \
                This might be useful to show some placeholder text or default value.</li>
            </ul>
            """)
    @Layout(DialogLayout.SpecialValues.ReplaceEmptyString.class)
    @ValueSwitchWidget
    @Signal(id = Signals.ReplaceEmptyStrings.class, condition = IsCustomCondition.class)
    CustomStringReplacementOption m_replaceEmptyString = CustomStringReplacementOption.BLANK;

    @Widget(title = "Substitute for empty string", description = """
            This string will be shown instead of an empty string, \
            if <i>Show empty string as custom string</i> is enabled.
            """)
    @Layout(DialogLayout.SpecialValues.ReplaceEmptyString.class)
    @Effect(signals = Signals.ReplaceEmptyStrings.class, type = EffectType.SHOW)
    String m_emptyStringReplacement = "<empty>";

    @Widget(title = "Link hyperlinks and e-mail addresses", description = """
            Enabling this will display URLs and e-mail addresses as links.
            """)
    @Layout(DialogLayout.Interaction.class)
    boolean m_linkLinksAndEmails = true;

    // Constructors

    StringFormatManagerNodeSettings() {
        // required by framework for serialization/deserialization
    }

    StringFormatManagerNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_columnsToFormat = ColumnFilter.createDefault(StringColumns.class, ctx);
    }

    // Column choices

    static final class StringColumns implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(s -> s.getType().isCompatible(StringValue.class))//
                .toArray(DataColumnSpec[]::new);
        }
    }
}
