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
 *   Jan 24, 2024 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.schema.Parameter;

@SuppressWarnings("restriction")
final class GroupByAliasSettings {

    @Parameter(title = "Group columns", description = "Specifies the columns to group by. "
        + "If no groups are specified, the entire table is aggregated.")
    ColumnFilter m_groupColumns;

    Aggregation[] m_aggregations;

    static final class Aggregation {
        @Parameter(title = "Column", description = "The column to aggregate.")
        ColumnSelection m_column;

        @Parameter(title = "Aggregation method", description = "The aggregation method to use.")
        AggregationType m_aggregationMethod;
    }

    /**
     * The supported aggregation methods.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum AggregationType {
            Mean("Mean_V4.6"), //
            Count("Count"), //
            First("First"), //
            Last("Last"), //
            List("List"), //
            Median("Median_V3.4"), //
            Sum("Sum_V2.5.2");

        private final String m_methodId;

        AggregationType(final String methodId) {
            m_methodId = methodId;
        }

        public static String valuesString() {
            return Stream.of(values())//
                .map(Enum::name)//
                .collect(Collectors.joining(", ", "[", "]"));
        }

        public ColumnAggregator createOperator(final DataColumnSpec column) throws InvalidSettingsException {
            return new ColumnAggregator(column, getMethod());
        }

        public AggregationMethod getMethod() {
            return AggregationMethods.getMethod4Id(m_methodId);
        }

        public static AggregationType forName(final String name) throws InvalidSettingsException {
            try {
                return AggregationType.valueOf(name);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException("The aggregation '%s' does not exist.".formatted(name));
            }
        }

    }

}