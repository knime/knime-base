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
 *   Jan 12, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.sorter;

import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 * Settings for the Sorter node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class SorterNodeSettings implements DefaultNodeSettings {

    private static final String ROWID_DIALOG = "<row-keys>";

    @Persist(customPersistor = SortCriteriaPersistor.class)
    @Schema(title = "Sort by", description = "Allows to refine the sorting by adding sorting criteria.")
    SortCriterion[] m_sortCriteria = new SortCriterion[]{new SortCriterion()};

    @Persist(configKey = SorterNodeModel.SORTINMEMORY_KEY)
    @Schema(title = "Sort in memory",
        description = "If selected the table is sorted in memory which requires more memory, but is faster. "
            + "In case the input table is large and memory is scarce it is recommended not to check this option.")
    boolean m_sortInMemory;

    @Schema(title = "Sort missing cells to end of the table",
        description = "If selected missingvalues are always placed at the end of the sorted output. "
            + "This is independent of the sort order, i.e. if sorted ascendingly they are considered to be larger than "
            + "a non-missing value and if sorted descendinglythey are smaller than any non-missing value.")
    boolean m_missingToEnd;

    static final class SortCriterion {

        /**
         * Serialization constructor.
         */
        SortCriterion() {
        }

        SortCriterion(final String column, final SortOrder ascendingOrder, final StringComparison stringComparison) {
            m_column = column;
            m_sortOrder = ascendingOrder;
            m_stringComparison = stringComparison;
        }

        @Schema(title = "Column", description = "The column to sort by including the RowIDs.",
            choices = AllColumns.class)
        public String m_column = ROWID_DIALOG;

        @Schema(title = "Order", description = "Controls the order of the rows in the output:"//
                + "<ul>"//
                + "<li><b>▲</b>: From small to large.</li>"//
                + "<li><b>▼</b>: From large to small.</li>"//
                + "</ul>"//
                +" Only used for string-compatible columns.")
        public SortOrder m_sortOrder = SortOrder.ASCENDING;

        @Schema(title = "String comparison",
            description = "Controls how strings are compared (only applies to string-compatible columns):"//
            + "<ul>"//
            + "<li><b>Alphanumeric</b>: 'Row10' is considered to be smaller than 'Row2'.</li>"//
            + "<li><b>Lexicographic</b>: 'Row2' is considered to be smaller than 'Row10'.</li>"//
            + "</ul>"
                )
        public StringComparison m_stringComparison = StringComparison.ALPHANUMERIC;

    }

    enum SortOrder {
            @Schema(title = "▲")
            ASCENDING, //
            @Schema(title = "▼")
            DESCENDING;

        static SortOrder fromAscending(final boolean ascending) {
            return ascending ? ASCENDING : DESCENDING;
        }

    }

    enum StringComparison {
            @Schema(title = "Alphanumeric")
            ALPHANUMERIC, //
            @Schema(title = "Lexicographic")
            LEXICOGRAPHIC;
    }

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            if (spec != null) {
                return spec.getColumnNames();
            } else {
                return new String[0];
            }
        }

    }

    private static final class SortCriteriaPersistor implements NodeSettingsPersistor<SortCriterion[]> {

        private static final String ROWID_MODEL = "-ROWKEY -";

        @Override
        public SortCriterion[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (!settings.containsKey(SorterNodeModel.INCLUDELIST_KEY)) {
                return new SortCriterion[]{
                    new SortCriterion(ROWID_DIALOG, SortOrder.ASCENDING, StringComparison.ALPHANUMERIC)};
            }
            var columns = settings.getStringArray(SorterNodeModel.INCLUDELIST_KEY);
            var orders = settings.getBooleanArray(SorterNodeModel.SORTORDER_KEY);
            var alphaNumComp = settings.getBooleanArray(SorterNodeModel.ALPHANUMCOMP_KEY, new boolean[columns.length]);
            return IntStream.range(0, columns.length).mapToObj(i -> new SortCriterion(//
                parseColumn(columns[i]), //
                orders[i] ? SortOrder.ASCENDING : SortOrder.DESCENDING, //
                alphaNumComp[i] ? StringComparison.ALPHANUMERIC : StringComparison.LEXICOGRAPHIC)//
            ).toArray(SortCriterion[]::new);
        }

        private static String parseColumn(final String column) {
            if (ROWID_MODEL.equals(column)) {
                return ROWID_DIALOG;
            } else {
                return column;
            }
        }

        @Override
        public void save(final SortCriterion[] criteria, final NodeSettingsWO settings) {
            var columns = new String[criteria.length];
            var orders = new boolean[criteria.length];
            var alphaNumComp = new boolean[criteria.length];
            for (int i = 0; i < criteria.length; i++) {
                var criterion = criteria[i];
                var column = criterion.m_column;
                columns[i] = ROWID_DIALOG.equals(column) ? ROWID_MODEL : column;
                orders[i] = criterion.m_sortOrder == SortOrder.ASCENDING;
                alphaNumComp[i] = criterion.m_stringComparison == StringComparison.ALPHANUMERIC;
            }
            settings.addStringArray(SorterNodeModel.INCLUDELIST_KEY, columns);
            settings.addBooleanArray(SorterNodeModel.SORTORDER_KEY, orders);
            settings.addBooleanArray(SorterNodeModel.ALPHANUMCOMP_KEY, alphaNumComp);
        }

    }
}
