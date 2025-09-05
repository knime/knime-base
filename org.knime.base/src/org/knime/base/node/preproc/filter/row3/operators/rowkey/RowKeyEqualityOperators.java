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
package org.knime.base.node.preproc.filter.row3.operators.rowkey;

import java.util.List;
import java.util.function.Predicate;

import org.knime.core.data.RowKeyValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.EqualsOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.builtin.NotEqualsOperator;

/**
 * Provides equality operators for row keys.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class RowKeyEqualityOperators {

    private RowKeyEqualityOperators() {
        // utility class
    }

    public static List<RowKeyFilterOperator<? extends FilterValueParameters>> getOperators() {
        return List.of(new RowKeyEquals(), //
            new RowKeyNotEquals() //
        );
    }

    private static final class RowKeyEquals implements RowKeyFilterOperator<RowKeyEqualsParameters>, EqualsOperator {

        @Override
        public Class<RowKeyEqualsParameters> getNodeParametersClass() {
            return RowKeyEqualsParameters.class;
        }

        @Override
        public Predicate<RowKeyValue> createPredicate(final RowKeyEqualsParameters params)
            throws InvalidSettingsException {
            return params.toPredicate();
        }

    }

    private static final class RowKeyNotEquals
        implements RowKeyFilterOperator<RowKeyEqualsParameters>, NotEqualsOperator {

        @Override
        public String getLabel() {
            return "Not equal"; // otherwise, "nor missing" would be appended, which does not make sense for row keys
        }

        @Override
        public Class<RowKeyEqualsParameters> getNodeParametersClass() {
            return RowKeyEqualsParameters.class;
        }

        @Override
        public Predicate<RowKeyValue> createPredicate(final RowKeyEqualsParameters params)
            throws InvalidSettingsException {
            return Predicate.not(params.toPredicate());
        }

    }

}
