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
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * The output settings, output as columns, rows, collection, what to do if the pattern does not match, etc.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.3
 */
@SuppressWarnings({"restriction", "squid:S3052"}) // Pending API, initialise with defaults
final class OutputSettings implements NodeParameters {

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

    }

    static final class OutputModeRef implements ParameterReference<OutputMode> {

    }

    static final class OutputModeIsColumns implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.COLUMNS);
        }

    }

    static final class OutputModeIsRows implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.ROWS);
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
    @Migrate(loadDefaultIfAbsent = true)
    @ValueReference(OutputModeRef.class)
    OutputMode m_mode = OutputMode.COLUMNS;

    //  -------------------  Output columns prefix: only for output as columns  -------------------

    enum ColumnPrefixMode {
            @Label("Input column name")
            INPUT_COL_NAME, //
            @Label("Custom")
            CUSTOM, //
            @Label("None")
            NONE;
    }

    static final class ColumnPrefixModeRef implements ParameterReference<ColumnPrefixMode> {

    }

    static final class UseCustomColumnPrefix implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnPrefixModeRef.class).isOneOf(ColumnPrefixMode.CUSTOM);
        }

    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output column prefix", description = """
            Define what prefix should be used for the output column names:
            <ul>
                <li><i>Input column name</i>: The name of the column containing the string to split is used as a prefix.
                    </li>
                <li><i>Custom</i>: Define a custom string that shall be used as a prefix.</li>
                <li><i>None</i>: No prefix is added.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @ValueReference(ColumnPrefixModeRef.class)
    @Effect(predicate = OutputModeIsColumns.class, type = EffectType.SHOW)
    ColumnPrefixMode m_columnPrefixMode = ColumnPrefixMode.INPUT_COL_NAME;

    //  -------------------  Custom column prefix: only for output as columns with custom prefix -------------------

    static final class CustomColumnPrefix implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(OutputModeIsColumns.class).and(i.getPredicate(UseCustomColumnPrefix.class));
        }

    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Custom prefix", description = "Define a custom column prefix.")
    @Effect(predicate = CustomColumnPrefix.class, type = EffectType.SHOW)
    String m_columnPrefix = "Split ";

    //  -------------------  Append column/replace input column: only for output as rows or collection  ----------------

    enum SingleOutputColumnMode {
            @Label("Append")
            APPEND, //
            @Label("Replace")
            REPLACE;
    }

    static final class SingleOutputColumnModeRef implements ParameterReference<SingleOutputColumnMode> {

    }

    static final class ReplaceSingleOutputColumn implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SingleOutputColumnModeRef.class).isOneOf(SingleOutputColumnMode.REPLACE);
        }

    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output column",
        description = "Choose whether to append the output column or replace the input column.")
    @ValueSwitchWidget
    @Effect(predicate = OutputModeIsColumns.class, type = EffectType.HIDE)
    @ValueReference(SingleOutputColumnModeRef.class)
    @Migrate(loadDefaultIfAbsent = true)
    SingleOutputColumnMode m_singleOutputColumnMode = SingleOutputColumnMode.APPEND;

    //  -------------------  Output column name: only for append column  -------------------

    static final class OutputModeIsColumnsOrSingleOutpuModeIsReplace implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(ReplaceSingleOutputColumn.class).or(i.getPredicate(OutputModeIsColumns.class));
        }

    }

    @Layout(DialogSections.Output.class)
    @Widget(title = "Output column name", description = "Choose a name for the output column")
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    @Effect(predicate = OutputModeIsColumnsOrSingleOutpuModeIsReplace.class, type = EffectType.HIDE)
    @Migrate(loadDefaultIfAbsent = true)
    String m_columnName = "Split";

    //  -------------------  Group labels in output  -------------------

    enum OutputGroupLabelMode {
            @Label("Capture group names or indices")
            CAPTURE_GROUP_NAMES, //
            @Label("Split input column name")
            SPLIT_INPUT_COL_NAME;

        interface Ref extends ParameterReference<OutputGroupLabelMode> {
        }

        static final class IsCaptureGroupNames implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(Ref.class).isOneOf(OutputGroupLabelMode.CAPTURE_GROUP_NAMES);
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
    @ValueReference(OutputGroupLabelMode.Ref.class)
    @Migrate(loadDefaultIfAbsent = true)
    OutputGroupLabelMode m_groupLabels = OutputGroupLabelMode.CAPTURE_GROUP_NAMES;

    // -------  Remove input column: only for output as columns, for the others we have the append|replace option  --

    @Layout(DialogSections.Output.class)
    @Widget(title = "Remove input column", description = "Remove the input column from the output table.")
    @Effect(predicate = OutputModeIsColumns.class, type = EffectType.SHOW)
    @Migrate(loadDefaultIfAbsent = true)
    boolean m_removeInputColumn = false;

}
