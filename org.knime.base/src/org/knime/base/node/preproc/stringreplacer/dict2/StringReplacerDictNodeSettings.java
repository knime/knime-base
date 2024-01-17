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
 *   21 Dec 2022 (jasper): created
 */
package org.knime.base.node.preproc.stringreplacer.dict2;

import org.knime.base.node.preproc.common.settings.CaseMatching;
import org.knime.base.node.preproc.stringreplacer.PatternType;
import org.knime.base.node.preproc.stringreplacer.ReplacementStrategy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Node Settings for the String Replacer (Dictionary)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class StringReplacerDictNodeSettings implements DefaultNodeSettings {

    // Enums

    /** What to do when multiple patterns match **/
    enum MultipleMatchHandling {
            @Label("Apply first matching")
            REPLACEFIRST, //
            @Label("Apply all sequentially")
            REPLACEALL;

        static final String OPTION_DESCRIPTION =
            """
            Select the strategy to use if multiple patterns match.
            <ul>
                <li>
                    <i>Apply first matching</i> only applies the first replacement that has a matching pattern.
                </li>
                <li>
                    <i>Apply all sequentially</i> applies all replacements with matching patterns from the dictionary
                    table sequentially. This means that later patterns can also match the output of another
                    replacement: For example, when the input is "A" and there are the replacements A -> B and B -> C,
                    the resulting string is "C".
                </li>
            </ul>
            """;
    }

    // Rules

    /** Indicates that the "Wildcard" pattern type is selected */
    interface IsWildcard {
        class Condition extends OneOfEnumCondition<PatternType> {
            @Override
            public PatternType[] oneOf() {
                return new PatternType[]{PatternType.WILDCARD};
            }
        }
    }

    /** Indicates that the option "Append column" is enabled **/
    interface IsAppendColumns {}


    // Helper methods

    /** Filter a table spec for the string-compatible columns */
    static DataColumnSpec[] getStringCompatibleColumns(final DataTableSpec tableSpec) {
        return tableSpec.stream().filter(spec -> spec.getType().isCompatible(StringValue.class))
            .toArray(DataColumnSpec[]::new);
    }

    /** Provides the string column choices of the table at input port 0 */
    static final class TargetColumnChoices implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)// data table
                .map(s -> getStringCompatibleColumns(s))//
                .orElse(new DataColumnSpec[]{});
        }
    }

    /** Provides the string column choices of the table at input port 1, including collections */
    static final class PatternAndReplacementColumnChoices implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(1)// dictionary table
                .map(s -> getStringCompatibleColumns(s))//
                .orElse(new DataColumnSpec[]{});
        }
    }

    // Layout

    interface DialogSections {
        @Section(title = "Column Selection")
        interface ColumnSelection {}

        @Section(title = "Find & Replace")
        interface FindAndReplace {}

        @Section(title = "Output")
        interface Output {}
    }


    // Settings

    @Layout(DialogSections.ColumnSelection.class)
    @Widget(title = "Target columns", description = "Select the columns in which the strings should be replaced.")
    @ChoicesWidget(choices = TargetColumnChoices.class)
    ColumnFilter m_targetColumns;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = PatternType.OPTION_NAME, description = PatternType.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @Signal(id = IsWildcard.class, condition = IsWildcard.Condition.class)
    PatternType m_patternType = PatternType.DEFAULT;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = "Use backslash as escape character", description = """
            If checked, the backslash character can be used to escape special characters. For instance, <tt>\\?</tt>
            will match the literal character <tt>?</tt> instead of an arbitrary character. In order to match a
            backslash you need to escape the backslash, too (<tt>\\</tt>).
            """)
    @Effect(signals = IsWildcard.class, type = EffectType.SHOW)
    boolean m_enableEscaping;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = CaseMatching.CaseSensitivityPersistor.class, configKey = "caseSensitive")
    @Widget(title = CaseMatching.OPTION_NAME, description = CaseMatching.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    CaseMatching m_caseMatching = CaseMatching.DEFAULT;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = "Pattern column", description = """
            The column containing literal strings, wildcard patterns or regular expressions, depending on the pattern
            type selected above.
            """)
    @ChoicesWidget(choices = PatternAndReplacementColumnChoices.class)
    String m_patternColumn;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = "Replacement column", description = """
            The column containing text that replaces the previous value in the cell if the pattern matched it. If you
            are using regular expressions, you may also use backreferences (e.g. <tt>$1</tt> to refer to the first
            capture group, named capture groups can also be used with <tt>(?&lt;group&gt;)</tt> and <tt>${group}</tt>
            to refer to them).
            """)
    @ChoicesWidget(choices = PatternAndReplacementColumnChoices.class)
    String m_replacementColumn;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = ReplacementStrategy.OPTION_NAME, description = ReplacementStrategy.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.DEFAULT;

    @Layout(DialogSections.FindAndReplace.class)
    @Widget(title = "If multiple patterns match", description = MultipleMatchHandling.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    MultipleMatchHandling m_multipleMatchHandling = MultipleMatchHandling.REPLACEFIRST;

    @Layout(DialogSections.Output.class)
    @Widget(title = "Append new columns", description = """
            If enabled, the strings will not be replaced in-place but new columns are appended that contains the
            original string with the replacement applied.
            """)
    @Signal(id = IsAppendColumns.class, condition = TrueCondition.class)
    boolean m_appendColumns;

    @Layout(DialogSections.Output.class)
    @Widget(title = "Suffix for new columns",
        description = "The suffix that is appended to the newly created columns with strings")
    @Effect(signals = IsAppendColumns.class, type = EffectType.SHOW)
    String m_columnSuffix = "_replaced";

    /**
     * Constructor for de/serialisation.
     */
    StringReplacerDictNodeSettings() {
        // required by interface
    }

    StringReplacerDictNodeSettings(final DefaultNodeSettingsContext ctx) {
        m_targetColumns = ColumnFilter.createDefault(TargetColumnChoices.class, ctx);
    }

}
