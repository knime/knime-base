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
 *   Dec 16, 2022 (benjamin): created
 */
package org.knime.base.node.preproc.table.splitter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Table Splitter node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class TableSplitterNodeSettings implements DefaultNodeSettings {

    @Widget( //
        title = "Find splitting row by", //
        description = "Select if the table should be split at the first matching row or at the last matching row." //
    )
    FindSplittingRowMode m_findSplittingRowMode = FindSplittingRowMode.FIRST_MATCH;

    @Widget( //
        title = "Lookup column",
        description = "Select the column that should be used to evaluate the matching criteria. "
            + "Only columns of type String, Number (integer), or Number (long) can be selected.")
    @ChoicesWidget(choices = ColumnChoices.class //
    )
    String m_lookupColumn = TableSplitterNodeModel.ROWID_PLACEHOLDER;

    @Widget( //
        title = "Matching criteria", //
        description = "Select criteria for matching the row:" + "<ul>"
            + "<li><b>Equals:</b> compares the value of the cell to a given search pattern.</li>"
            + "<li><b>Missing:</b> only matches rows that have a missing value at the selected column.</li>"
            + "<li><b>Empty:</b> matches rows that have an empty or missing value at the selected column. "
            + "Strings and Row IDs containing only whitespace characters will also match.</li>" + "</ul>"//
    )
    MatchingCriteria m_matchingCriteria = MatchingCriteria.EQUALS;

    @Widget( //
        title = "Search pattern", //
        description = "Select a search pattern to compare the value of the selected column. "
            + "If a number column is selected the search pattern must be a parsable number." //
    )
    String m_searchPattern = "";

    @Widget( //
        title = "Include matching row in top output table", //
        description = "Select this option to include the row that split the table in the top output table." //
    )
    boolean m_includeMatchingRowInTopTable = true;

    @Widget( //
        title = "Include matching row in bottom output table", //
        description = "Select this option to include the row that split the table in the bottom output table." //
    )
    boolean m_includeMatchingRowInBottomTable;

    @Widget( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table." //
    )
    @Persist(optional = true)
    boolean m_updateDomains;

    /** Modes for finding the matching row. "First match" or "Last match". */
    enum FindSplittingRowMode {
            @Label("First match") //
            FIRST_MATCH, //
            @Label("Last match") //
            LAST_MATCH;
    }

    /** Matching criteria. "Equals", "Missing", or "Empty". */
    enum MatchingCriteria {
            @Label("Equals") //
            EQUALS, //
            @Label("Missing") //
            MISSING, //
            @Label("Empty") //
            EMPTY;
    }

    /**
     * A column provider that gives the names of the columns that are compatible according to
     * {@link TableSplitterNodeModel#isCompatible}
     */
    private static final class ColumnChoices implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            final DataTableSpec specs = context.getDataTableSpecs()[0];
            if (specs == null) {
                return new String[0];
            } else {
                return specs.stream() //
                    .filter(TableSplitterNodeModel::isCompatible) //
                    .map(DataColumnSpec::getName) //
                    .toArray(String[]::new);
            }
        }
    }
}
