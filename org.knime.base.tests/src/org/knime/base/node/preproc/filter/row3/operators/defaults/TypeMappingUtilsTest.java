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
 *   1 Dec 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.operators.defaults.TypeMappingUtils.ConverterException;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the utility methods in {@link TypeMappingUtils}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class TypeMappingUtilsTest {

    @Test
    void testFromString() throws ConverterException {
        assertEquals("42",
            ((StringCell)TypeMappingUtils.readDataCellFromString(StringCell.TYPE, "42")).getStringValue(),
            "Expected string cell with value '42'");

        assertEquals(DataType.getMissingCell(), TypeMappingUtils.readDataCellFromString(StringCell.TYPE, null),
            "Expected missing cell for null input");
    }

    @Test
    void testFromStringExceptional() {
        final var type = ListCell.getCollectionType(StringCell.TYPE);
        assertFalse(TypeMappingUtils.supportsDataType(type), "Collection types are not yet supported");

        assertThatThrownBy(() -> TypeMappingUtils.readDataCellFromString(type, "not-a-list"))
            .isInstanceOf(IllegalArgumentException.class) //
            .hasMessageMatching("Collection types are not supported.+");

        assertThatThrownBy(() -> TypeMappingUtils.readDataCellFromString(IntCell.TYPE, "not-int"))
            .isInstanceOf(ConverterException.class) //
            .hasMessageMatching("For input string.+");
    }

    @Test
    void testRoundtrip() throws ConverterException {
        final var inputString = "42";
        final var cell = TypeMappingUtils.readDataCellFromString(IntCell.TYPE, inputString);
        assertEquals(new IntCell(42), cell, "Expected integer cell with value 42");
        final var string = TypeMappingUtils.getStringFromDataCell(cell);
        assertEquals(inputString, string, "Expected roundtrip conversion to preserve string value");
    }
}
