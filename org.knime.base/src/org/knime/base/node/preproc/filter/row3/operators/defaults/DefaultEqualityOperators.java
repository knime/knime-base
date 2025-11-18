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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsNorMissingOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.valuefilter.ValueFilterValidationUtil;

/**
 * Default equality operators for data types that don't have their own registered operators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultEqualityOperators {

    private DefaultEqualityOperators() {
    }

    /**
     * Gets the default equality operators for the given data type. These are used for data types that don't register
     * equality operators themselves.
     *
     * @param dataType the data type to get the operators for
     * @return default implementations for testing equality
     */
    @SuppressWarnings({
        "restriction", // webui
        "java:S1452" // we don't have one concrete params type here for all of the returned operators
    })
    public static List<FilterOperator<? extends FilterValueParameters>> getOperators(final DataType dataType) {
        return List.of( //
            new EqualDefault(), //
            new NotEqualDefault(), //
            new NotEqualNorMissingDefault());
    }

    @SuppressWarnings("restriction")
    private abstract static class EqualBase implements FilterOperator<SingleStringParameters> {

        private final boolean m_matchTrue;

        EqualBase(final boolean matchTrue) {
            m_matchTrue = matchTrue;
        }

        @Override
        public Class<SingleStringParameters> getNodeParametersClass() {
            return SingleStringParameters.class;
        }

        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureDataType, final SingleStringParameters filterParameters)
            throws InvalidSettingsException {
            final var type = runtimeColumnSpec.getType();
            // the default implementation works only on the _same_ data type
            ValueFilterValidationUtil.checkSameType(this, type, configureDataType);
            final var cell = filterParameters.createCellAs(type);
            return v -> m_matchTrue == cell.equals(v.materializeDataCell());
        }
    }

    /**
     * Checks whether the default equality operators are applicable for the given data type.
     *
     * @param dataType the data type to check
     * @return {@code true} if the default equality operators can be used for the given data type, {@code false}
     *         otherwise
     */
    public static boolean isApplicable(final DataType dataType) {
        // we handle Boolean with IS_TRUE/IS_FALSE operators
        return !BooleanCell.TYPE.equals(dataType) && TypeMappingUtils.supportsDataType(dataType);
    }

    @SuppressWarnings("restriction")
    private static final class EqualDefault extends EqualBase implements EqualsOperator {

        private EqualDefault() {
            super(true);
        }

    }

    @SuppressWarnings("restriction")
    private static final class NotEqualDefault extends EqualBase implements NotEqualsOperator {

        private NotEqualDefault() {
            super(false);
        }

        @Override
        public boolean returnTrueForMissingCells() {
            return true;
        }
    }

    @SuppressWarnings("restriction")
    private static final class NotEqualNorMissingDefault extends EqualBase implements NotEqualsNorMissingOperator {

        private NotEqualNorMissingDefault() {
            super(false);
        }

        // returns false for missing cells
    }
}
