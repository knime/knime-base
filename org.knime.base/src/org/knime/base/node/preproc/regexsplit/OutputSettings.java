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
 *   4 Jan 2024 (carlwitt): created
 */
package org.knime.base.node.preproc.regexsplit;

import org.knime.base.node.preproc.regexsplit.RegexSplitNodeSettings.DialogSections;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.And;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Not;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Or;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The output settings, output as columns, rows, collection, what to do if the pattern does not match, etc.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.3
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // Pending API, initialise with defaults
final class OutputSettings implements DefaultNodeSettings, WidgetGroup {

    static final class LegacyProvider implements DefaultProvider<OutputSettings> {
        @Override
        public OutputSettings getDefault() {
            final var settings = new OutputSettings();
            settings.m_columnPrefixMode = ColumnPrefixMode.CUSTOM;
            settings.m_columnPrefix = "split_";
            return settings;
        }
    }

    //  -------------------  Output as column, row, or collection  -------------------

    enum OutputMode {
            @Label("Columns")
            COLUMNS, //
            @Label("Rows")
            ROWS, //
            @Label("List")
            LIST, //
            @Label("Set (remove duplicates)")
            SET;

        boolean isCollection() {
            return this == LIST || this == SET;
        }

        interface IsColumns {
            class Condition extends OneOfEnumCondition<OutputMode> {
                @Override
                public OutputMode[] oneOf() {
                    return new OutputMode[]{OutputMode.COLUMNS};
                }
            }
        }

        interface IsRows {
            class Condition extends OneOfEnumCondition<OutputMode> {
                @Override
                public OutputMode[] oneOf() {
                    return new OutputMode[]{OutputMode.ROWS};
                }
            }
        }
    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output matched groups as", description = """
            Define how to output the results:
            <ul>
                <li><i>Columns</i>: Each capture group in the defined pattern creates a new column in the output table.
                The column names correspond to the names of the named capture groups.</li>
                <li><i>Rows</i>: Each input row is duplicated by the number of capture groups, and every capture is
                added to one of those copies.</li>
                <li><i>List</i>: The captures are appended to the input as a list of strings.</li>
                <li><i>Set (remove duplicates)</i>: The captures are appended to the input as a set of strings. Note
                that duplicates are removed and the order of captures is not preserved.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Signal(id = OutputMode.IsColumns.class, condition = OutputMode.IsColumns.Condition.class)
    @Signal(id = OutputMode.IsRows.class, condition = OutputMode.IsRows.Condition.class)
    @Persist(optional = true)
    OutputMode m_mode = OutputMode.COLUMNS;

    //  -------------------  Output columns prefix: only for output as columns  -------------------

    enum ColumnPrefixMode {
            @Label("Input column name")
            INPUT_COL_NAME, //
            @Label("Custom")
            CUSTOM, //
            @Label("None")
            NONE;

        interface IsCustom {
            class Condition extends OneOfEnumCondition<ColumnPrefixMode> {
                @Override
                public ColumnPrefixMode[] oneOf() {
                    return new ColumnPrefixMode[]{ColumnPrefixMode.CUSTOM};
                }
            }
        }
    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output columns prefix", description = """
            Define what prefix should be used for the output column names:
            <ul>
                <li><i>Input column name</i>: The name of the column containing the string to split is used as a prefix.
                    </li>
                <li><i>Custom</i>: Define a custom string that shall be used as a prefix.</li>
                <li><i>None</i>: No prefix is added.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Signal(id = ColumnPrefixMode.IsCustom.class, condition = ColumnPrefixMode.IsCustom.Condition.class)
    @Effect(signals = OutputMode.IsColumns.class, type = EffectType.SHOW)
    ColumnPrefixMode m_columnPrefixMode = ColumnPrefixMode.INPUT_COL_NAME;

    //  -------------------  Custom column prefix: only for output as columns with custom prefix -------------------

    @Layout(DialogSections.Output.class)
    @Widget(title = "Custom prefix", description = "Define a custom column prefix.")
    @Effect(signals = {OutputMode.IsColumns.class, ColumnPrefixMode.IsCustom.class}, operation = And.class,
        type = EffectType.SHOW)
    String m_columnPrefix = "Split ";

    // -------  Remove input column: only for output as columns, for the others we have the append|replace option  --

    @Layout(DialogSections.Output.class)
    @Widget(title = "Remove input column", description = "Remove the input column from the output table.")
    @Effect(signals = OutputMode.IsColumns.class, type = EffectType.SHOW)
    @Persist(optional = true)
    boolean m_removeInputColumn = false;


    //  -------------------  Append column/replace input column: only for output as rows or collection  ----------------

    enum SingleOutputColumnMode {
            @Label("Append")
            APPEND, //
            @Label("Replace")
            REPLACE;

        interface IsReplace {
            class Condition extends OneOfEnumCondition<SingleOutputColumnMode> {
                @Override
                public SingleOutputColumnMode[] oneOf() {
                    return new SingleOutputColumnMode[]{SingleOutputColumnMode.REPLACE};
                }
            }
        }
    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output column",
        description = "Choose whether to append the output column or replace the input column.")
    @ValueSwitchWidget
    @Effect(signals = OutputMode.IsColumns.class, operation = Not.class, type = EffectType.SHOW)
    @Signal(id = SingleOutputColumnMode.IsReplace.class, condition = SingleOutputColumnMode.IsReplace.Condition.class)
    @Persist(optional = true)
    SingleOutputColumnMode m_singleOutputColumnMode = SingleOutputColumnMode.APPEND;

    //  -------------------  Output column name: only for append column  -------------------

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output column name", description = "Choose a name for the output column")
    @TextInputWidget(minLength = 1)
    @Effect(signals = {SingleOutputColumnMode.IsReplace.class, OutputMode.IsColumns.class}, operation = Or.class,
        type = EffectType.HIDE)
    @Persist(optional = true)
    String m_columnName = "Split";

    //  -------------------  Group labels in output  -------------------

    enum OutputGroupLabelMode {
            @Label("Capture group names or indices")
            CAPTURE_GROUP_NAMES, //
            @Label("Split input column name")
            SPLIT_INPUT_COL_NAME;

        interface IsCaptureGroupNames {
            class Condition extends OneOfEnumCondition<OutputGroupLabelMode> {
                @Override
                public OutputGroupLabelMode[] oneOf() {
                    return new OutputGroupLabelMode[]{OutputGroupLabelMode.CAPTURE_GROUP_NAMES};
                }
            }
        }
    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Group labels in output", advanced = true, description = """
            Define the naming of the output groups:
            <ul>
                <li><i>Capture group names or indices</i>: Use the names of the capture groups. For unnamed capture
                    groups, their index is used as their label.</li>
                <li><i>Split input column name</i>: Apply the provided pattern to the name of the input column and
                    use the captures as labels.</li>
            </ul>

            The impact of this setting depends on the selected <i>Output mode</i>:
            <ul>
                <li><i>Columns</i>: The labels will be used as the suffix of the column names.</li>
                <li><i>Rows</i>: The labels will be used as the suffix of the row IDs.</li>
                <li><i>List</i> and <i>Set</i>: The labels will be used as element names in the collection cell
                    specification.</li>
            </ul>
            """)
    @RadioButtonsWidget
    @Signal(id = OutputGroupLabelMode.IsCaptureGroupNames.class,
        condition = OutputGroupLabelMode.IsCaptureGroupNames.Condition.class)
    @Persist(optional = true)
    OutputGroupLabelMode m_groupLabels = OutputGroupLabelMode.CAPTURE_GROUP_NAMES;

    enum NoMatchBehaviour {
            @Label("Insert missing value")
            INSERT_MISSING, //
            @Label("Insert empty string")
            INSERT_EMPTY, //
            @Label("Fail")
            FAIL;
    }

    //  -------------------  No match behaviour  -------------------

    @Layout(DialogSections.Output.class)
    @Widget(title = "If pattern does not match", description = """
            Define what to do if a pattern can't be matched to the input string:
            <ul>
                <li><i>Insert missing value</i> puts missing cell(s) in place of the output column(s).
                    The node will emit a warning when an input string doesn't match.</li>
                <li><i>Insert empty string</i> puts empty string(s) in place of the output column(s).
                    The node will emit a warning when an input string doesn't match.</li>
                <li><i>Fail</i> causes the node to fail if one of the inputs can not be matched against the pattern.
                </li>
            </ul>
            """)
    @Persist(optional = true)
    @ValueSwitchWidget
    NoMatchBehaviour m_noMatchBehaviour = NoMatchBehaviour.INSERT_MISSING;
}
