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

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.StringToStringWithRowIDChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.FilteredInputTableColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Settings of the Table Splitter node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class TableSplitterNodeSettings implements DefaultNodeSettings {

    @Section(title = "Find splitting row")
    interface FindSplittingRowSection {
    }

    @Widget( //
        title = "Find splitting row by", //
        description = "Select if the table should be split at the first matching row or at the last matching row." //
    )
    @Layout(FindSplittingRowSection.class)
    @ValueSwitchWidget
    FindSplittingRowMode m_findSplittingRowMode = FindSplittingRowMode.FIRST_MATCH;

    @Widget( //
        title = "Lookup column",
        description = "Select the column that should be used to evaluate the matching criteria. "
            + "Only columns of type String, Number (integer), or Number (long) can be selected.")
    @ChoicesProvider(ColumnChoices.class)
    @Layout(FindSplittingRowSection.class)
    @Migration(LookupColumnMigration.class)
    @Persist(configKey = "lookupColumnV2")
    StringOrEnum<RowIDChoice> m_lookupColumn = new StringOrEnum<>(ROW_ID);

    static final class LookupColumnMigration extends StringToStringWithRowIDChoiceMigration {
        LookupColumnMigration() {
            super("lookupColumn");
        }
    }

    interface MatchingCriteriaRef extends Reference<MatchingCriteria> {
    }

    static final class MatchingCriteriaIsEquals implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(MatchingCriteriaRef.class).isOneOf(MatchingCriteria.EQUALS);
        }
    }

    @Widget( //
        title = "Matching criteria", //
        description = "Select criteria for matching the row:" + "<ul>"
            + "<li><b>Equals:</b> compares the value of the cell to a given search pattern.</li>"
            + "<li><b>Missing:</b> only matches rows that have a missing value at the selected column.</li>"
            + "<li><b>Empty:</b> matches rows that have an empty or missing value at the selected column. "
            + "Strings and RowIDs containing only whitespace characters will also match.</li>" + "</ul>"//
    )
    @Layout(FindSplittingRowSection.class)
    @ValueReference(MatchingCriteriaRef.class)
    MatchingCriteria m_matchingCriteria = MatchingCriteria.EQUALS;

    @Widget( //
        title = "Search pattern", //
        description = "Select a search pattern to compare the value of the selected column. "
            + "If a number column is selected the search pattern must be a parsable number." //
    )
    @Layout(FindSplittingRowSection.class)
    @Effect(predicate = MatchingCriteriaIsEquals.class, type = EffectType.SHOW)
    String m_searchPattern = "";

    @Section(title = "Output")
    @After(FindSplittingRowSection.class)
    interface OutputSection {
    }

    @Widget( //
        title = "Include matching row in top output table", //
        description = "Select this option to include the row that split the table in the top output table." //
    )
    @Layout(OutputSection.class)
    boolean m_includeMatchingRowInTopTable = true;

    @Widget( //
        title = "Include matching row in bottom output table", //
        description = "Select this option to include the row that split the table in the bottom output table." //
    )
    @Layout(OutputSection.class)
    boolean m_includeMatchingRowInBottomTable;

    @Widget( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table.", //
        advanced = true)
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(OutputSection.class)
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
    private static final class ColumnChoices implements FilteredInputTableColumnsProvider {

        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return TableSplitterNodeModel.isCompatible(col);
        }

    }
}
