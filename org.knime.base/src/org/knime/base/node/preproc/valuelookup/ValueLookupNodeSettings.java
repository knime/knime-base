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
package org.knime.base.node.preproc.valuelookup;

import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.ColumnFilter;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * Node Settings for the Value Lookup Node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // New Node UI is not yet API
public final class ValueLookupNodeSettings implements DefaultNodeSettings {

    /** How Strings in the target column / dictionary lookup shall be handled */
    enum StringMatching {
            /** Only match exact correspondence */
            @Schema(title = "Full string")
            FULLSTRING,
            /** Match if dictionary lookup is substring of target column */
            @Schema(title = "Substring")
            SUBSTRING,
            /** Allow Wildcards in dictionary lookup column */
            @Schema(title = "Wildcard")
            WILDCARD,
            /** Allow Regex in dictionary lookup column */
            @Schema(title = "RegEx")
            REGEX;
    }

    /** Whether only exact matches are acceptable or the next-lower or next-higher match is also of interest */
    enum MatchBehaviour {
            /** Only match if the number is one of the dict values */
            @Schema(title = "Insert missing values")
            EQUAL,
            /** Match to the queried number or, if not available, the next lower number */
            @Schema(title = "Match next smaller")
            EQUALORSMALLER,
            /** Match to the queried number or, if not available, the next higher number */
            @Schema(title = "Match next larger")
            EQUALORLARGER;
    }

    /** In what direction to search (determines which match is selected, can speed up things) */
    enum SearchDirection {
            /** Search forwards through input table, select first match */
            @Schema(title = "Use first")
            FORWARD,
            /** Search backwards through input table, select last match */
            @Schema(title = "Use last")
            BACKWARD;
    }

    /** Provides the columns names of the table at input port 0 */
    static final class DataTableChoices implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            final var spec = context.getDataTableSpecs()[0];
            return spec == null ? new String[0] : spec.getColumnNames();
        }
    }

    /** Provides the columns names of the table at input port 1 */
    static final class DictionaryTableChoices implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            return choices(context.getDataTableSpecs()[1]);
        }

        /**
         * Returns possible column choices from the given data table spec or an empty list if the spec is {@code null}.
         * @param spec data table spec to choose column names from
         * @return choices or empty list if spec is {@code null}
         */
        static String[] choices(final DataTableSpec spec) {
            return spec == null ? new String[0] : spec.getColumnNames();
        }
    }

    /** The name of the lookup column in the data table */
    @Schema(title = "Lookup column (data table)", //
        description = "The column in the data table that will be used to look up cells in the dictionary", //
        choices = DataTableChoices.class)
    String m_lookupCol;

    /** Whether to delete the lookup column in the output table */
    @Schema(title = "Delete lookup column", //
        description = "When selected, the lookup column will be deleted from the data table") //
    boolean m_deleteLookupCol = false; //NOSONAR: more verbosity

    /** The name of the key column in the dictionary table */
    @Schema(title = "Key column (dictionary table)", //
        description = "The column in the dictionary table that contains the search key / criterion", //
        choices = DictionaryTableChoices.class)
    String m_dictKeyCol;

    /** The names of the columns from the dictionary table that shall be added to the output table */
    @Schema(title = "New columns from dictionary table", //
        description = "The columns in the dictionary table that contain the values added to the data table", //
        choices = DictionaryTableChoices.class, //
        withTypes = false) // TODO: Types can be enabled once a bug in `ChoicesAndEnumDefinitionProvider.java:188` has
                           // been addressed, that prohibits twinlists from having type for any but the first input port
    ColumnFilter m_dictValueCols;

    /** The selected string match behaviour */
    @Schema(title = "String matching", //
        description = "The matching behavior when matching strings: "
            + "Full string matching matches a lookup string only if it exactly matches a search string. "
            + "Substring matching matches a lookup string if the key in the dictionary is a substring of it. "
            + "Wildcard and RegEx matching match a lookup string if a pattern in the dictionary matches to it.")
    StringMatching m_stringMatchBehaviour = StringMatching.FULLSTRING;

    /** Whether the string match shall be case sensitive */
    @Schema(title = "Match strings case-sensitive", //
        description = "When enabled, the string matching will be case-sensitive, otherwise case-insensitive")
    boolean m_caseSensitive = true;

    /** The matching behaviour (only exact, exact or next lower, exact or next higher) */
    @Schema(title = "If no row matches", //
        description = "Defines what happens when a lookup key is not present in the dictionary: "
            + "If \"Insert missing values\" is selected, missing values are inserted. "
            + "If \"Match next smaller\" (\"- larger\") is selected, the next smaller (larger) value from the "
            + "dictionary is matched, based on the value of the lookup key. "
            + "If no such element can be found, a missing value is inserted.")
    MatchBehaviour m_matchBehaviour = MatchBehaviour.EQUAL;

    /** The search direction (forwards / backwards / binSearch) */
    @Schema(title = "If multiple rows match", //
        description = "Specifies the direction in which to perform the search. "
            + "This defines the behavior in case there are multiple matching keys in the dictionary table.")
    SearchDirection m_searchDirection = SearchDirection.FORWARD;

    /** Whether to create a column that indicates whether a match has been found */
    @Schema(title = "Append a column indicating whether a match was found", //
        description = "When checked, a new column \"" + ValueLookupNodeModel.COLUMN_NAME_MATCHFOUND
            + "\" is appended to the output that contains a boolean indicating whether a match was found.")
    boolean m_createFoundCol = false; //NOSONAR: more verbosity


    /**
     * Constructor for de/serialization.
     */
    ValueLookupNodeSettings() {
        // required by interface
    }

    ValueLookupNodeSettings(final SettingsCreationContext ctx) {
        m_dictValueCols = ColumnFilter.createDefault(DictionaryTableChoices.class, ctx);
    }

}
