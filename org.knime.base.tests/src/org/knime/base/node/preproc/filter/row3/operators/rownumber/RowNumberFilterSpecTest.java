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
 *   24 Nov 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.preproc.filter.row3.operators.legacy.LegacyFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterSpec.Operator;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests for the {@link RowNumberFilterOperator} (legacy handling and exception cases)
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("deprecation") // explicitly testing legacy handling
final class RowNumberFilterSpecTest {

    @SuppressWarnings("static-method") // parameter source
    static List<Arguments> getLegacySource() {
        return List.of(//
            Arguments.of(LegacyFilterOperator.EQ, Operator.EQ), //
            Arguments.of(LegacyFilterOperator.NEQ, Operator.NEQ), //
            Arguments.of(LegacyFilterOperator.LT, Operator.LT), //
            Arguments.of(LegacyFilterOperator.LTE, Operator.LTE), //
            Arguments.of(LegacyFilterOperator.GT, Operator.GT), //
            Arguments.of(LegacyFilterOperator.GTE, Operator.GTE), //
            Arguments.of(LegacyFilterOperator.FIRST_N_ROWS, Operator.FIRST_N_ROWS), //
            Arguments.of(LegacyFilterOperator.LAST_N_ROWS, Operator.LAST_N_ROWS));
    }

    @ParameterizedTest
    @MethodSource("getLegacySource")
    @SuppressWarnings("static-method") // junit test
    void testLegacyTranslation(final LegacyFilterOperator legacyOp, final Operator expected)
        throws InvalidSettingsException {
        final var translated = new RowNumberFilterSpec(legacyOp, 1);
        assertEquals(expected, translated.m_operator, "Unexpected legacy operator translation");
    }

    @SuppressWarnings("static-method") // parameter source
    static List<Arguments> getLegacyUnsupportedSource() {
        return List.of(//
            Arguments.of(LegacyFilterOperator.IS_FALSE), //
            Arguments.of(LegacyFilterOperator.IS_TRUE), //
            Arguments.of(LegacyFilterOperator.IS_MISSING), //
            Arguments.of(LegacyFilterOperator.IS_NOT_MISSING), //
            Arguments.of(LegacyFilterOperator.REGEX), //
            Arguments.of(LegacyFilterOperator.WILDCARD)); //
    }

    @ParameterizedTest
    @MethodSource("getLegacyUnsupportedSource")
    @SuppressWarnings("static-method") // junit test
    void testLegacyTranslationUnsupported(final LegacyFilterOperator legacyOp) {
        assertThatThrownBy(() -> new RowNumberFilterSpec(legacyOp, 1)).isInstanceOf(InvalidSettingsException.class)
            .hasMessageContaining("Cannot use operator \"%s\" to filter by row number", legacyOp);
    }

}
