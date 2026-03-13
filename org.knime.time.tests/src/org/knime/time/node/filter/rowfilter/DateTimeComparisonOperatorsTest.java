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
 *   11 Mar 2026 (Copilot): created
 */
package org.knime.time.node.filter.rowfilter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;

@SuppressWarnings({"restriction", "static-method"})
final class DateTimeComparisonOperatorsTest {

    @Test
    void testIsBefore() throws InvalidSettingsException {
        final var params = createParams(LocalDate.of(2022, 6, 1));
        final var spec = new DataColumnSpecCreator("col", DataType.getType(LocalDateCell.class)).createSpec();
        final var before = getOperator("LT");
        final var predicate = before.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 5, 31))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 1))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 2))));
    }

    @Test
    void testIsBeforeOrAt() throws InvalidSettingsException {
        final var params = createParams(LocalDate.of(2022, 6, 1));
        final var spec = new DataColumnSpecCreator("col", DataType.getType(LocalDateCell.class)).createSpec();
        final var beforeOrAt = getOperator("LTE");
        final var predicate = beforeOrAt.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 5, 31))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 1))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 2))));
    }

    @Test
    void testIsAfter() throws InvalidSettingsException {
        final var params = createParams(LocalDate.of(2022, 6, 1));
        final var spec = new DataColumnSpecCreator("col", DataType.getType(LocalDateCell.class)).createSpec();
        final var after = getOperator("GT");
        final var predicate = after.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 5, 31))));
        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 2))));
    }

    @Test
    void testIsAfterOrAt() throws InvalidSettingsException {
        final var params = createParams(LocalDate.of(2022, 6, 1));
        final var spec = new DataColumnSpecCreator("col", DataType.getType(LocalDateCell.class)).createSpec();
        final var afterOrAt = getOperator("GTE");
        final var predicate = afterOrAt.createPredicate(spec, DataType.getType(LocalDateCell.class), params);

        assertFalse(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 5, 31))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 1))));
        assertTrue(predicate.test(LocalDateCellFactory.create(LocalDate.of(2022, 6, 2))));
    }

    @Test
    void testTypeMismatchThrows() {
        final var params = createParams(LocalDate.of(2022, 6, 1));
        final var operators = createOperators();
        final var incompatibleSpec =
            new DataColumnSpecCreator("col", DataType.getType(LocalTimeCell.class)).createSpec();
        final var before = operators.getOperators().get(0);

        assertThrows(InvalidSettingsException.class,
            () -> before.createPredicate(incompatibleSpec, DataType.getType(LocalDateCell.class), params));
    }

    private static LocalDateCellFilterParameters createParams(final LocalDate date) {
        final var params = new LocalDateCellFilterParameters();
        params.m_date = date;
        params.m_useExecutionTime = false;
        return params;
    }

    private static DateTimeComparisonOperators<LocalDateCell, LocalDateCellFilterParameters> createOperators() {
        return new DateTimeComparisonOperators<>(DataType.getType(LocalDateCell.class),
            LocalDateCellFilterParameters.class);
    }

    private static FilterOperator<LocalDateCellFilterParameters> getOperator(final String id) {
        final var operators = createOperators();
        return operators.getOperators().stream().filter(op -> id.equals(op.getId())).findFirst().orElseThrow();
    }

}
