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
 *   Nov 6, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row.nodefunc;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RangeRowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.func.NodeFuncApi.Builder;

/**
 * Filters based on a range defined by a lower and an upper bound.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class AttributeRangeRowFilterNodeFunc extends AbstractAttributeRowFilterNodeFunc {

    private static final String UPPER_BOUND = "upper_bound";

    private static final String LOWER_BOUND = "lower_bound";

    @Override
    String getName() {
        return "filter_rows_by_attribute_range";
    }

    @Override
    void extendApi(final Builder builder) {
        super.extendApi(builder);
        builder
            .withDescription(
                """
                Filters rows by comparing the value of the specified column against the specified range.
                Typically used for numerical columns but can be used with any column type for which a comparison is sensible.
                """)//
            .withOptionalStringArgument(LOWER_BOUND, "Lower bound to include.")//
            .withOptionalStringArgument(UPPER_BOUND, "Upper bound to include.");
    }

    @Override
    IRowFilter createFilter(final NodeSettingsRO arguments, final DataColumnSpec columnSpec, final boolean include)
        throws InvalidSettingsException {
        var lowerBound = arguments.getString(LOWER_BOUND, null);
        var upperBound = arguments.getString(UPPER_BOUND, null);
        var type = columnSpec.getType();
        var lowerBoundCell = createCell(type, lowerBound);
        var upperBoundCell = createCell(type, upperBound);
        return new RangeRowFilter(columnSpec.getName(), include, lowerBoundCell, upperBoundCell);
    }

    private static DataCell createCell(final DataType type, final String value) {
        if (value == null) {
            return null;
        }
        var dataCellFactory = new DataCellFactory();
        return dataCellFactory.createDataCellOfType(type, value);
    }

}
