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
 *   25 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import java.util.List;
import java.util.function.Predicate;

import org.knime.base.node.preproc.filter.row3.operators.RowKeyFilterOperator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKeyValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.NotEqualsNorMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.NotEqualsOperator;

/**
 * Default equality operators for data types that don't have their own registered operators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultEqualityOperators {

    private DefaultEqualityOperators() {
    }

    public static List<FilterOperator<? extends FilterValueParameters>> getOperators(final DataType dataType) {
        return List.of( //
            new EqualDefault(), //
            new NotEqualDefault(), //
            new NotEqualNorMissingDefault());
    }

    public static List<RowKeyFilterOperator<? extends FilterValueParameters>> getRowKeyOperators() {
        return List.of( //
            new RowKeyEqualDefault(), //
            new RowKeyNotEqualDefault() //
        );
    }

    private static final class EqualDefault implements FilterOperator<SingleStringParameters>, EqualsOperator {

        @Override
        public Class<SingleStringParameters> getNodeParametersClass() {
            return SingleStringParameters.class;
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureDataType, final SingleStringParameters filterParameters)
            throws InvalidSettingsException {
            final var type = runtimeColumnSpec.getType();
            final var cell = filterParameters.createCellAs(type);
            return v -> cell.equals(v.materializeDataCell());
        }

    }

    private abstract static class NotEqualBase implements FilterOperator<SingleStringParameters> {

        @Override
        public Class<SingleStringParameters> getNodeParametersClass() {
            return SingleStringParameters.class;
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureDataType, final SingleStringParameters filterParameters)
            throws InvalidSettingsException {
            final var type = runtimeColumnSpec.getType();
            final var cell = filterParameters.createCellAs(type);
            return v -> !cell.equals(v.materializeDataCell());
        }

    }

    private static final class NotEqualDefault extends NotEqualBase implements NotEqualsOperator {

        @Override
        public boolean returnTrueForMissingCells() {
            return true;
        }
    }

    private static final class NotEqualNorMissingDefault extends NotEqualBase implements NotEqualsNorMissingOperator {
    }

    private static final class RowKeyEqualDefault
        implements RowKeyFilterOperator<StringEqualsParameters>, EqualsOperator {

        @Override
        public Class<StringEqualsParameters> getNodeParametersClass() {
            return StringEqualsParameters.class;
        }

        @Override
        public Predicate<RowKeyValue> createPredicate(final StringEqualsParameters params)
            throws InvalidSettingsException {
            final var stringPredicate = params.toStringPredicate();
            return rowKey -> stringPredicate.test(rowKey.getString());
        }

    }

    private static class RowKeyNotEqualDefault
        implements RowKeyFilterOperator<StringEqualsParameters>, NotEqualsOperator {

        @Override
        public Class<StringEqualsParameters> getNodeParametersClass() {
            return StringEqualsParameters.class;
        }

        @Override
        public Predicate<RowKeyValue> createPredicate(final StringEqualsParameters params)
            throws InvalidSettingsException {
            final var stringPredicate = params.toStringPredicate();
            return rowKey -> !stringPredicate.test(rowKey.getString());
        }

    }

}