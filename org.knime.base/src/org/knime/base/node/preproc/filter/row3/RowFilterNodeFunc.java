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
 *   14 Oct 2024 (Alexander Jauch-Walser): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.ArrayList;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion.FilterValueParametersProvider;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.missing.IsMissingFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.PatternWithCaseParameters;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberParameters;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.NodeFuncApi.Builder;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;

/**
 *
 * Base class for Row Filter NodeFuncs.
 *
 * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
 */
abstract class RowFilterNodeFunc implements NodeFunc {

    // Whether to include or exclude the rows
    static final String INCLUDE = "include";

    static final String COLUMN = "column";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var tableSpec = (DataTableSpec)inputSpecs[0];
        var criteria = getFilterCriteria(arguments, tableSpec);

        // NodeFunc will always create a row filter with exactly one filter criterion
        var rowFilterSettings = new RowFilterNodeSettings();
        rowFilterSettings.m_predicates = criteria;
        rowFilterSettings.m_outputMode = arguments.getBoolean(INCLUDE) ? FilterMode.MATCHING : FilterMode.NON_MATCHING;

        NodeParametersUtil.saveSettings(RowFilterNodeSettings.class, rowFilterSettings, settings);
    }

    @Override
    public final NodeFuncApi getApi() {
        var builder = NodeFuncApi.builder(getName());
        builder.withInputTable("table", "The table to filter.")//
            .withOutputTable("filtered_table", "The filtered table.")//
            .withBooleanArgument(INCLUDE, "Whether rows matching the filter for a specific column should be "
                + "included in the output table or not.");
        extendApi(builder);
        return builder.build();
    }

    /**
     * Each NodeFunc will produce a row filter with a specific configured criterion
     *
     * @param tableSpec
     */
    abstract FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, DataTableSpec tableSpec)
        throws InvalidSettingsException;

    @Override
    public String getNodeFactoryClassName() {
        return RowFilterNodeFactory.class.getName();
    }

    /**
     * Derived NodeFuncs can set a function description add additional parameters to the API builder here
     */
    abstract void extendApi(final NodeFuncApi.Builder builder);

    abstract String getName();

    /**
     *
     * Filters rows based on a regex on a specified column
     *
     * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("restriction")
    public static class AttributePatternRowFilterNodeFunc extends RowFilterNodeFunc {

        private static final String REGEX = "regex";

        @Override
        FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
            throws InvalidSettingsException {
            var column = arguments.getString(COLUMN);
            var regex = arguments.getString(REGEX);

            var criterion = new FilterCriterion();
            criterion.m_column = new StringOrEnum<>(column);
            criterion.m_columnType = StringCell.TYPE;
            criterion.m_operator = "REGEX";

            criterion.m_filterValueParameters = new PatternWithCaseParameters(regex);

            return new FilterCriterion[]{criterion};
        }

        @Override
        void extendApi(final Builder builder) {
            builder
                .withDescription(
                    "Creates a new table that contains only rows that match the given regex for a given column.")
                .withStringArgument(COLUMN, "The column on which to filter.")
                .withStringArgument(REGEX, "Regular expression that is used to match the RowIDs of the input table.");
        }

        @Override
        String getName() {
            return "filter_rows_by_regex";
        }

    }

    /**
     * Filters by applying a range filter to a numerical column.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class AttributeRangeRowFilterNodeFunc extends RowFilterNodeFunc {

        private static final String UPPER_BOUND = "upper_bound";

        private static final String LOWER_BOUND = "lower_bound";

        @Override
        FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
            throws InvalidSettingsException {
            var lowerBound = arguments.getString(LOWER_BOUND, null);
            var upperBound = arguments.getString(UPPER_BOUND, null);
            var columnSpec = tableSpec.getColumnSpec(arguments.getString(COLUMN));

            // TODO: this should be fixed on the framework side and not in here
            if (columnSpec.getType().isCompatible(DoubleValue.class)) {
                if (isNotPresent(lowerBound)) {
                    lowerBound = null;
                }
                if (isNotPresent(upperBound)) {
                    upperBound = null;
                }
            }

            var criteria = new ArrayList<FilterCriterion>();
            if (lowerBound != null) {
                criteria.add(createCriterion(columnSpec, LegacyFilterOperator.GTE, lowerBound));
            }
            if (upperBound != null) {
                criteria.add(createCriterion(columnSpec, LegacyFilterOperator.LTE, upperBound));
            }
            return criteria.toArray(FilterCriterion[]::new);
        }

        private static boolean isNotPresent(final String bound) {
            return bound == null || bound.equals("null") || bound.length() == 0;
        }

        private static FilterCriterion createCriterion(final DataColumnSpec columnSpec,
            final LegacyFilterOperator operator, final String value) { //
            var criterion = new FilterCriterion(columnSpec);
            criterion.m_operator = operator.name();
            var type = columnSpec.getType();
            criterion.m_columnType = type;
            criterion.m_filterValueParameters =
                FilterValueParametersProvider.createNewParameters(type, operator.name());
            criterion.m_filterValueParameters.applyStash(new DataValue[]{new StringCell(value)});
            return criterion;
        }

        @Override
        void extendApi(final Builder builder) {
            builder.withStringArgument(COLUMN, "The column which the criterion will be applied to.").withDescription("""
                    Filters rows by comparing the value of the specified column against the specified range,
                    if both upper and lower bound are given, or just against the given bound.
                    Upper and lower bound are optional so that filtering e.g. only with a lower bound
                    is possible. Typically used for numerical columns but can be used with any column type
                    for which a comparison is sensible.
                    """)//
                .withOptionalStringArgument(LOWER_BOUND, "Lower bound to include.")//
                .withOptionalStringArgument(UPPER_BOUND, "Upper bound to include.");

        }

        @Override
        String getName() {
            return "filter_rows_by_attribute_range";
        }

    }

    /**
     *
     * Filters rows with missing values from a specified column
     *
     * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("restriction")
    public static final class MissingValueRowFilterNodeFunc extends RowFilterNodeFunc {

        @Override
        FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
            throws InvalidSettingsException {
            var column = arguments.getString(COLUMN);

            var criterion = new FilterCriterion();
            criterion.m_column = new StringOrEnum<>(column);
            var columnSpec = tableSpec.getColumnSpec(column);
            criterion.m_columnType = columnSpec == null ? StringCell.TYPE : columnSpec.getType();
            criterion.m_operator = IsMissingFilterOperator.getInstance().getId();

            return new FilterCriterion[]{criterion};
        }

        @Override
        void extendApi(final Builder builder) {
            builder.withStringArgument(COLUMN, "The column on which to filter. The column can be of any type.")
                .withDescription("Matches rows whose value of the specified column are missing.");
        }

        @Override
        String getName() {
            return "filter_missing_rows";
        }

    }

    /**
     *
     * Filters rows based on the row index and a operator e.g. greater than
     *
     * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("restriction")
    public static final class RowFilterByRowNumberNodeFunc extends RowFilterNodeFunc {

        private static final String OPERATOR = "operator";

        private static final String ROW_NUMBER = "row_number";

        private static final String EQUALS = "equals";

        private static final String NEQUALS = "notEquals";

        private static final String LESSTHAN = "lessThan";

        private static final String LESSTHANEQUALS = "lessThanEquals";

        private static final String GREATERTHAN = "greaterThan";

        private static final String GREATERTHANEQUALS = "greaterThanEquals";

        private static final String FIRSTN = "firstN";

        private static final String LASTN = "lastN";

        @Override
        FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
            throws InvalidSettingsException {

            var rowNumber = arguments.getLong(ROW_NUMBER);
            if (rowNumber <= 0) {
                throw new InvalidSettingsException("Row number must be larger than 0.");
            }

            var criterion = new FilterCriterion();
            criterion.m_column = new StringOrEnum<>(RowIdentifiers.ROW_NUMBER);
            criterion.m_operator = getOperator(arguments.getString(OPERATOR)).name();
            criterion.m_filterValueParameters = new RowNumberParameters(rowNumber);

            return new FilterCriterion[]{criterion};
        }

        private static LegacyFilterOperator getOperator(final String operatorName) {
            return switch (operatorName) {
                case EQUALS -> LegacyFilterOperator.EQ;
                case NEQUALS -> LegacyFilterOperator.NEQ;
                case LESSTHAN -> LegacyFilterOperator.LT;
                case LESSTHANEQUALS -> LegacyFilterOperator.LTE;
                case GREATERTHAN -> LegacyFilterOperator.GT;
                case GREATERTHANEQUALS -> LegacyFilterOperator.GTE;
                case FIRSTN -> LegacyFilterOperator.FIRST_N_ROWS;
                case LASTN -> LegacyFilterOperator.LAST_N_ROWS;
                default -> null;
            };
        }

        @Override
        void extendApi(final Builder builder) {
            builder
                .withDescription(
                    "Creates a new table by filtering the range of rows by row number. The first row has row number 1."
                    )//
                .withStringArgument(OPERATOR,
                    String.format("""
                        The operator which will be used to filter the row numbers on:
                        %s: Returns the row number which equals the specified number.
                        %s: Returns the rows with row number not matching the specified number.
                        %s: Returns the rows with row number which are strictly smaller than specified number
                        %s: Returns the rows with row number which are smaller than or equal to specified number
                        %s: Returns the rows with row number which are strictly larger than specified number
                        %s: Returns the rows with row number which are larger than or equal than specified number
                        %s: Returns the first n rows from the start of the table.
                        %s: Returns the last n rows from the end of the table.""", EQUALS, NEQUALS, LESSTHAN,
                        LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, FIRSTN, LASTN))
                .withOptionalLongArgument(ROW_NUMBER,
                    "The row number which filters rows of the table in combination with an operator.");

        }

        @Override
        String getName() {
            return "filter_row_by_row_number";
        }
    }

    /**
     *
     * Filters Rows based on a regex on the RowID column
     *
     * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
     */
    @SuppressWarnings("restriction")
    public static final class RowIDRowFilterNodeFunc extends RowFilterNodeFunc {

        private static final String REGEX = "regex";

        @Override
        FilterCriterion[] getFilterCriteria(final NodeSettingsRO arguments, final DataTableSpec tableSpec)
            throws InvalidSettingsException {
            var regex = arguments.getString(REGEX);

            var criterion = new FilterCriterion();
            criterion.m_column = new StringOrEnum<>(RowIdentifiers.ROW_ID);
            criterion.m_operator = LegacyFilterOperator.REGEX.name();

            criterion.m_filterValueParameters = new PatternWithCaseParameters(regex);

            return new FilterCriterion[]{criterion};
        }

        @Override
        void extendApi(final Builder builder) {
            builder
                .withDescription(
                    "Creates a new table that only contains rows whose RowID matches the given regex pattern.")
                .withStringArgument(REGEX, "Regular expression that is used to match the RowIDs of the input table.");
        }

        @Override
        String getName() {
            return "filter_row_ids_by_regex";
        }

    }

}
