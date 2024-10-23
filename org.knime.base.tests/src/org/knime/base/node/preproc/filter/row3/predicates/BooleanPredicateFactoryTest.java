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
 *   22 Jan 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.predicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.OptionalInt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the boolean predicate factory.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class BooleanPredicateFactoryTest {

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testBoolean(final boolean matchTrue) {
        final var factory = BooleanPredicateFactory.create(BooleanCell.TYPE, matchTrue).orElseThrow();
        final var idx = OptionalInt.of(EqualityPredicateFactoryTest.SPEC.findColumnIndex("Bool1"));
        assertThatCode(() -> factory.createPredicate(idx, null)) //
            .as("Booleans can be matched TRUE or FALSE") //
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testBooleanException(final boolean matchTrue) {
        final var factory = BooleanPredicateFactory.create(BooleanCell.TYPE, matchTrue).orElseThrow();
        final var idx = OptionalInt.empty();
        assertThatCode(() -> factory.createPredicate(idx, null)) //
            .as("Boolean predicate needs a column index") //
            .isInstanceOf(IllegalStateException.class) //
            .hasMessageContaining("Boolean predicate operates on column but did not get a column index");

        assertThat(BooleanPredicateFactory.create(StringCell.TYPE, matchTrue))
            .as("Boolean predicate operates only on BooleanCells") //
            .isEmpty();
    }
}
