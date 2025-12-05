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
 *   26 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators;

import java.util.List;
import java.util.function.Predicate;

import org.knime.base.node.preproc.filter.row3.operators.defaults.CaseSensitivity;
import org.knime.base.node.preproc.filter.row3.operators.defaults.StringWithCaseParameters;
import org.knime.base.node.preproc.filter.row3.operators.pattern.PatternFilterUtils;
import org.knime.base.node.preproc.filter.row3.operators.pattern.PatternOperatorFamily;
import org.knime.base.node.preproc.filter.row3.operators.pattern.PatternWithCaseParameters;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperators;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperatorFamily;

/**
 * Operators for the String data type.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class StringFilterOperators implements FilterOperators {

    @Override
    public DataType getDataType() {
        return StringCell.TYPE;
    }

    @Override
    public List<FilterOperatorFamily<? extends FilterValueParameters>> getOperatorFamilies() {
        return List.of( //
            new EqualsOperatorFamily<StringCell, StringWithCaseParameters>(getDataType(),
                StringWithCaseParameters.class) {

                @Override
                protected Predicate<DataValue> getEquality(final DataColumnSpec runtimeColumnSpec,
                    final FilterOperator<StringWithCaseParameters> operator, final StringWithCaseParameters params)
                    throws InvalidSettingsException {
                    final var runtimeColType = runtimeColumnSpec.getType();
                    if (!runtimeColType.isCompatible(StringValue.class)) {
                        throw FilterValidationUtil
                            .createInvalidSettingsException(builder -> builder.withSummary(FilterValidationUtil
                                .getUnsupportedOperatorSummary(super.getDataType(), operator, runtimeColumnSpec))
                                .addResolutions(
                                    // change input
                                    "Convert the input column to \"%s\" type"
                                        .formatted(StringCell.TYPE.toPrettyString()),
                                    // reconfigure
                                    FilterValidationUtil.resolutionSelectDifferentOperator(runtimeColType)));
                    }
                    final var predicate = params.toStringPredicate();
                    // safe cast due to compatibility check above
                    return dv -> predicate.test(((StringValue)dv).getStringValue());
                }
            }, new StringPatternOperatorFamily());
    }

    private static final class StringPatternOperatorFamily
        extends PatternOperatorFamily<StringValue, PatternWithCaseParameters> {

        private StringPatternOperatorFamily() {
            super(StringValue.class, PatternWithCaseParameters.class);
        }

        @Override
        protected Predicate<StringValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final boolean isRegex, final PatternWithCaseParameters params)
            throws InvalidSettingsException {
            return PatternFilterUtils.createPredicate(params.m_pattern, isRegex,
                params.m_caseSensitivity == CaseSensitivity.CASE_SENSITIVE, s -> s.getStringValue());
        }

    }

}
