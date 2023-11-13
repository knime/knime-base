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
 *   Nov 13, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.valuelookup;

import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.LookupColumnNoMatchReplacement;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.LookupColumnOutput;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.MatchBehaviour;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.SearchDirection;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.StringMatching;
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
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ValueLookupNodeFunc implements SimpleNodeFunc {

    private static final String LOOKUP_COLUMN_OUTPUT = "lookup_column_output";

    private static final String LOOKUP_COLUMN_REPLACEMENT_COLUMN = "lookup_column_replacement_column";

    private static final String LOOKUP_COLUMN_NO_MATCH_REPLACEMENT = "lookup_column_no_match_replacement";

    private static final String DICTIONARY_TABLE = "dictionary_table";

    private static final String DATA_TABLE = "data_table";

    private static final String APPEND_FOUND_COLUMN = "append_found_column";

    private static final String APPENDED_COLUMNS = "appended_columns";

    private static final String CASE_SENSITIVE = "case_sensitive";

    private static final String STRING_MATCHING = "string_matching";

    private static final String NO_MATCH_BEHAVIOR = "no_match_behavior";

    private static final String USE_FIRST_MATCH = "use_first_match";

    private static final String KEY_COLUMN = "key_column";

    private static final String LOOKUP_COLUMN = "lookup_column";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var dataSpec = (DataTableSpec)inputSpecs[0];
        var dictSpec = (DataTableSpec)inputSpecs[1];
        var lookupColumn = arguments.getString(LOOKUP_COLUMN);
        checkContained(lookupColumn, LOOKUP_COLUMN, dataSpec, DATA_TABLE);
        var keyColumn = arguments.getString(KEY_COLUMN);
        checkContained(keyColumn, KEY_COLUMN, dictSpec, DICTIONARY_TABLE);
        var valueLookupSettings = new ValueLookupNodeSettings();
        valueLookupSettings.m_lookupCol = lookupColumn;
        valueLookupSettings.m_dictKeyCol = keyColumn;
        valueLookupSettings.m_lookupColumnOutput =
                LookupColumnOutput.valueOf(arguments.getString(LOOKUP_COLUMN_OUTPUT));
        valueLookupSettings.m_lookupReplacementCol = arguments.getString(LOOKUP_COLUMN_REPLACEMENT_COLUMN);
        valueLookupSettings.m_columnNoMatchReplacement = LookupColumnNoMatchReplacement.valueOf(
            arguments.getString(LOOKUP_COLUMN_NO_MATCH_REPLACEMENT));
        valueLookupSettings.m_createFoundCol = arguments.getBoolean(APPEND_FOUND_COLUMN);
        valueLookupSettings.m_matchBehaviour =
            EnumArgumentType.getConstant(MatchBehaviour.class, arguments.getString(NO_MATCH_BEHAVIOR));
        valueLookupSettings.m_searchDirection =
            arguments.getBoolean(USE_FIRST_MATCH) ? SearchDirection.FORWARD : SearchDirection.BACKWARD;
        valueLookupSettings.m_stringMatchBehaviour =
            EnumArgumentType.getConstant(StringMatching.class, arguments.getString(STRING_MATCHING));
        var appendedColumns = arguments.getStringArray(APPENDED_COLUMNS);
        for (var col : appendedColumns) {
            checkContained(col, APPENDED_COLUMNS, dictSpec, DICTIONARY_TABLE);
        }
        valueLookupSettings.m_dictValueCols = new ColumnFilter(appendedColumns);
        DefaultNodeSettings.saveSettings(ValueLookupNodeSettings.class, valueLookupSettings, settings);
    }

    private static void checkContained(final String columnName, final String columnPurpose,
        final DataTableSpec tableSpec, final String tablePurpose) throws InvalidSettingsException {
        if (tableSpec != null) {
            CheckUtils.checkSetting(tableSpec.containsName(columnName),
                "The specified %s '%s' does not exist in the %s.", columnPurpose, columnName, tablePurpose);
        }
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("value_lookup")//
            .withInputTable(DATA_TABLE, "The data table has a column that contains lookup values")
            .withInputTable(DICTIONARY_TABLE,
                "The dictionary table has a key column and value columns that will be inserted into the data table.")//
            .withOutputTable("output_table", "The data_table with additional columns.")//
            .withDescription("""
                    Looks up values from the data_table in the dictionary_table and appends the cells from matching
                    rows to the rows in the output table.
                    Missing values are treated as ordinary values, i.e. they are valid as lookup and replacement value.
                    The key column of the dictionary can also be a collection type.
                    Then, the values in the collection act as alternative lookup values for the associated row.
                    """)
            .withArgument(LOOKUP_COLUMN,
                "The column in the data_table that will be used to look up cells in the dictionary.",
                PrimitiveArgumentType.STRING)
            .withArgument(KEY_COLUMN, "The column in the dictionary_table that contains the search key / criterion.",
                PrimitiveArgumentType.STRING)
            .withArgument(USE_FIRST_MATCH,
                "If there are multiple matches in the dictionary, controls whether the first or last match is used.",
                PrimitiveArgumentType.BOOLEAN)
            .withArgument(NO_MATCH_BEHAVIOR, """
                    Controls what happens if no match can be found.
                    %s: Insert missing values if no match can be found.
                    %s: If no exact match exist, match the next lower value.
                    %s: If no exact match exist, match the next higher value.
                    """.formatted(MatchBehaviour.EQUAL, MatchBehaviour.EQUALORSMALLER, MatchBehaviour.EQUALORLARGER),
                EnumArgumentType.create(MatchBehaviour.class))
            .withArgument(STRING_MATCHING, """
                    The matching behavior when matching strings:
                    %s: Matches a lookup string only if it exactly matches a search string.
                    %s: Matches a lookup string if the key in the dictionary is a substring of it.
                    %s: Match a lookup string if a pattern in the dictionary matches it.
                    """.formatted(StringMatching.FULLSTRING, StringMatching.SUBSTRING, StringMatching.REGEX),
                EnumArgumentType.create(StringMatching.class))
            .withArgument(CASE_SENSITIVE, "Whether string matching should be case sensitive.",
                PrimitiveArgumentType.BOOLEAN)
            .withArgument(APPENDED_COLUMNS,
                "The columns from the dictionary_table to append that should be appended to the output.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))
            .withArgument(APPEND_FOUND_COLUMN,
                "If true, a boolean column '%s' that indicates whether a match was found is appended to the table."
                    .formatted(ValueLookupNodeModel.COLUMN_NAME_MATCHFOUND),
                PrimitiveArgumentType.BOOLEAN)
            .withArgument(LOOKUP_COLUMN_OUTPUT, """
                    Controls the content of the lookup column in the output table.
                    %s: The lookup column is kept unchanged in the output table.
                    %s: The lookup column content is replaced by the content of the replacement column.
                    %s: The lookup column is removed from the output table.
                    """.formatted(LookupColumnOutput.RETAIN, LookupColumnOutput.REPLACE, LookupColumnOutput.REMOVE),
                EnumArgumentType.create(LookupColumnOutput.class))
            .withArgument(LOOKUP_COLUMN_REPLACEMENT_COLUMN, """
                    The column in the dictionary table that contains the values for the lookup column.
                    Only relevant if the lookup column output is set to %s.
                    """.formatted(LookupColumnOutput.REPLACE), PrimitiveArgumentType.STRING)
            .withArgument(LOOKUP_COLUMN_NO_MATCH_REPLACEMENT, """
                    Controls the content of the lookup column in the output table if no match was found.
                    %s: Use the original lookup value. This might result in a mixed type column.
                    %s: Use a missing value.
                    Only relevant if the lookup column output is set to %s.
                    """.formatted(LookupColumnNoMatchReplacement.RETAIN, LookupColumnNoMatchReplacement.INSERT_MISSING),
                EnumArgumentType.create(LookupColumnNoMatchReplacement.class))
            .build();
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return ValueLookupNodeFactory.class;
    }

}
