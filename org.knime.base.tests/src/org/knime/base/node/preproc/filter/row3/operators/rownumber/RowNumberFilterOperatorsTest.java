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
 *   20 Nov 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rownumber;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.knime.base.node.preproc.filter.row3.operators.pattern.RowNumberPatternFilterParameters;
import org.knime.base.node.preproc.filter.row3.operators.pattern.RowNumberRegexPatternFilterOperator;
import org.knime.base.node.preproc.filter.row3.operators.pattern.RowNumberWildcardPatternFilterOperator;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests for the row number filter operators exception cases.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"static-method"}) // webui
final class RowNumberFilterOperatorsTest {

    @SuppressWarnings("unused") // parameterized test source
    private static List<RowNumberFilterOperator<RowNumberParameters>> rowNumberFilterOperators =
        RowNumberOperators.getOperators();

    @ParameterizedTest
    @FieldSource("rowNumberFilterOperators")
    void testSlicingInvariant(final RowNumberFilterOperator<RowNumberParameters> operator)
        throws InvalidSettingsException {
        final var params = new RowNumberParameters(10L);
        // checks that the slicing invariant is true for all our implementations
        // supportsSlicing <=> createSliceFilter(..) != null
        final var supportsSlicing = operator.supportsSlicing();
        final var filter = operator.createSliceFilter(params);
        if (supportsSlicing) {
            assertNotNull(filter, "Expected non-null slice filter since operator claims to support slicing");
        } else {
            fail("All our current row number filter operators support slicing");
        }
    }

    @SuppressWarnings("unused") // parameterized test source
    private static List<RowNumberFilterOperator<RowNumberPatternFilterParameters>> rowNumberPatternFilterOperators =
        Arrays.asList(RowNumberWildcardPatternFilterOperator.getInstance(),
            RowNumberRegexPatternFilterOperator.getInstance());

    @ParameterizedTest
    @FieldSource("rowNumberPatternFilterOperators")
    void testPatternFilters(final RowNumberFilterOperator<RowNumberPatternFilterParameters> patternOp)
        throws InvalidSettingsException {
        final var params = new RowNumberPatternFilterParameters();
        params.m_pattern = ".*1";

        final var supportsSlicing = patternOp.supportsSlicing();
        final var filter = patternOp.createSliceFilter(params);
        if (supportsSlicing) {
            fail("None of our current row number pattern filter operators support slicing");
        } else {
            assertNull(filter, "Expected null slice filter since operator does not support slicing");
        }
    }
}