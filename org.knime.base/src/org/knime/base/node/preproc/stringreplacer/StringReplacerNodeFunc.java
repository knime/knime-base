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
package org.knime.base.node.preproc.stringreplacer;

import org.knime.base.node.util.regex.CaseMatching;
import org.knime.base.node.util.regex.PatternType;
import org.knime.base.node.util.regex.ReplacementStrategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.EnumArgumentType;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.SimpleNodeFunc;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
@SuppressWarnings("restriction")
public final class StringReplacerNodeFunc implements SimpleNodeFunc {

    private static final String APPEND_NEW_COLUMN = "append_new_column";

    private static final String NEW_COLUMN_NAME = "new_column_name";

    private static final String REPLACEMENT_STRATEGY = "replacement_strategy";

    private static final String REPLACEMENT = "replacement";

    private static final String PATTERN = "pattern";

    private static final String CASE_SENSITIVE = "case_sensitive";

    private static final String PATTERN_TYPE = "pattern_type";

    private static final String COLUMN_TO_REPLACE = "column_to_replace";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var replacerSettings = new StringReplacerNodeSettings();
        var tableSpec = (DataTableSpec)inputSpecs[0];
        var columnToReplace = arguments.getString(COLUMN_TO_REPLACE);
        if (tableSpec != null) {
            validateCol(tableSpec, columnToReplace);
        }
        replacerSettings.m_colName = columnToReplace;
        replacerSettings.m_patternType = getConstant(PatternType.class, arguments.getString(PATTERN_TYPE));
        replacerSettings.m_pattern = arguments.getString(PATTERN);
        replacerSettings.m_caseMatching =
            arguments.getBoolean(CASE_SENSITIVE) ? CaseMatching.CASESENSITIVE : CaseMatching.CASEINSENSITIVE;
        replacerSettings.m_replacement = arguments.getString(REPLACEMENT);
        replacerSettings.m_replacementStrategy =
            getConstant(ReplacementStrategy.class, arguments.getString(REPLACEMENT_STRATEGY));
        var newColName = arguments.getString(NEW_COLUMN_NAME, null);
        replacerSettings.m_createNewCol = arguments.getBoolean(APPEND_NEW_COLUMN);
        replacerSettings.m_newColName = newColName;
        NodeParametersUtil.saveSettings(StringReplacerNodeSettings.class, replacerSettings, settings);
    }

    @SuppressWarnings("null")
    private static void validateCol(final DataTableSpec tableSpec, final String columnToReplace)
        throws InvalidSettingsException {
        var spec = tableSpec.getColumnSpec(columnToReplace);
        CheckUtils.checkSetting(spec != null, "The specified column '%s' does not exist in the input table.",
            columnToReplace);
        CheckUtils.checkSetting(spec.getType().isCompatible(StringValue.class),
            "The selected column '%s' is not a String column.", columnToReplace);
    }

    private static <E extends Enum<E>> E getConstant(final Class<E> enumClass, final String name)
        throws InvalidSettingsException {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException ex) {
            throw new InvalidSettingsException(ex.getMessage(), ex);
        }
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("replace_strings")//
            .withInputTable("table", "The input table.")//
            .withOutputTable("output_table", "The output table.")//
            .withDescription("Replaces strings in a selected target column.")//
            .withArgument(COLUMN_TO_REPLACE, "The column in which to replace the strings.",
                PrimitiveArgumentType.STRING)//
            .withArgument(PATTERN_TYPE,
                "The type of pattern to use. One of %s."
                    .formatted(EnumArgumentType.createValuesString(PatternType.class)),
                EnumArgumentType.create(PatternType.class))
            .withArgument(CASE_SENSITIVE,
                "Specifies whether matching will distinguish between upper and lower case letters.",
                PrimitiveArgumentType.BOOLEAN)
            .withArgument(PATTERN, "The pattern to match.", PrimitiveArgumentType.STRING)//
            .withArgument(REPLACEMENT, "The replacement string with which matched strings are replaced.",
                PrimitiveArgumentType.STRING)//
            .withArgument(REPLACEMENT_STRATEGY,
                """
                        Select what to replace in case a string matches a pattern.
                        %s: Replaces the entire string with the replacement string, requiring an exact match of the whole string.
                        %s: Replaces all occurrences of the pattern with the replacement string.
                        Note that when e.g. matching on the RegEx-pattern .*, an empty string at the end of the input is also matched and replaced.
                        To avoid that, use e.g. the pattern ^.* to indicate that the match has to start at the beginning of the string.
                        """
                    .formatted(ReplacementStrategy.WHOLE_STRING, ReplacementStrategy.ALL_OCCURRENCES),
                EnumArgumentType.create(ReplacementStrategy.class))
            .withArgument(APPEND_NEW_COLUMN,
                "Whether to append a new column to the table or replace the %s.".formatted(COLUMN_TO_REPLACE),
                PrimitiveArgumentType.BOOLEAN)//
            .withOptionalArgument(NEW_COLUMN_NAME, """
                    The name for the new column.
                    Can be None if %s is false.
                    Must be a column name that does not exist yet.
                    """.formatted(APPEND_NEW_COLUMN), PrimitiveArgumentType.STRING)//
            .build();
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return StringReplacerNodeFactory.class;
    }

}
