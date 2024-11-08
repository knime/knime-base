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
 *   Nov 8, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;
import org.knime.core.webui.node.dialog.defaultdialog.widget.dynamic.DynamicValuesInput;

/**
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public class RowFilterNodeSettingsUtil {

    public record StringColumnFilter(boolean isRowKeys, String columnName, String pattern) {

        public static StringColumnFilter searchingRowKeys(final String searchTerm) {
            return new StringColumnFilter(true, null, addingWildcardMatchesBeforeAndAfter(Pattern.quote(searchTerm)));
        }

        public static StringColumnFilter filteringColumn(final String columnName, final String[] searchTerms) {
            return new StringColumnFilter(false, columnName,
                addingWildcardMatchesBeforeAndAfter(toRegexPattern(searchTerms)));
        }

        private static String toRegexPattern(final String[] filterValues) {
            return Arrays.stream(filterValues).map(Pattern::quote).reduce((a, b) -> a + "|" + b).orElse("");
        }

        private static String addingWildcardMatchesBeforeAndAfter(final String pattern) {
            return ".*(" + pattern + ").*";
        }

    }

    public static NodeSettings createModelSettings(final StringColumnFilter[] stringColumnFilters) {
        final var modelSettings = new NodeSettings("model");

        return modelSettings;

    }

    public static NodeSettings writeFiltersToRowFilterNodeSettings(final StringColumnFilter[] stringColumnFilters,
        final NodeSettings settings) {
        final var newSettings = new NodeSettings("adjusted rowFilter");
        settings.copyTo(newSettings);
        final var newModelSettings = new NodeSettings("model");
        DefaultNodeSettings.saveSettings(RowFilterNodeSettings.class, createSettings(stringColumnFilters),
            newModelSettings);
        newSettings.addNodeSettings(newModelSettings);
        return newSettings;

    }

    private static RowFilterNodeSettings createSettings(final StringColumnFilter[] stringColumnFilters) {

        final var settings = new RowFilterNodeSettings();
        settings.m_predicates = Arrays.stream(stringColumnFilters).map(RowFilterNodeSettingsUtil::createFilterCriterion)
            .toArray(FilterCriterion[]::new);

        return settings;
    }

    private static FilterCriterion createFilterCriterion(final StringColumnFilter stringColumnFilter) {
        final var filterCriterion = new AbstractRowFilterNodeSettings.FilterCriterion();
        filterCriterion.m_predicateValues = toDynamicValuesInput(stringColumnFilter);
        filterCriterion.m_operator = FilterOperator.REGEX;
        filterCriterion.m_column = getColumnSelection(stringColumnFilter);
        return filterCriterion;
    }

    private static ColumnSelection getColumnSelection(final StringColumnFilter stringColumnFilter) {
        if (stringColumnFilter.isRowKeys()) {
            return SpecialColumns.ROWID.toColumnSelection();
        }
        return new ColumnSelection(stringColumnFilter.columnName(), StringCell.TYPE);
    }

    private static DynamicValuesInput toDynamicValuesInput(final StringColumnFilter stringColumnFilter) {
        final var pattern = stringColumnFilter.pattern();
        return new DynamicValuesInput(StringCell.TYPE, StringCellFactory.create(pattern), true);
    }

}
