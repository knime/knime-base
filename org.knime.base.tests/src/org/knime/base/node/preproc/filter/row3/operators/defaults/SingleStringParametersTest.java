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

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests some exception cases for {@link SingleStringParameters}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class SingleStringParametersTest {

    @SuppressWarnings("static-method")
    @Test
    void testCreateCell() {
        final var params = new SingleStringParameters("test");
        final var cell = params.createCell();
        assertEquals("test", cell.getStringValue(), "Expected cell to contain test string");
    }

    @SuppressWarnings("static-method")
    @Test
    void testCreateCellAsString() throws InvalidSettingsException {
        final var params = new SingleStringParameters("test");
        assertEquals(new StringCell("test"), params.createCellAs(params.getSpecificType()), "Expected string cell");
        assertEquals(params.createCell(), params.createCellAs(StringCell.TYPE), "Expected string cell");
    }

    @SuppressWarnings("static-method")
    @Test
    void testCreateCellAsInvalidType() {
        final var params = new SingleStringParameters("test");
        assertThatThrownBy(() -> params.createCellAs(IntCell.TYPE)).isInstanceOf(InvalidSettingsException.class)
            .hasMessageMatching("The string \".+\" does not represent a valid \".+Integer.+\"");
    }

    @SuppressWarnings("static-method")
    @Test
    void testCreateCellAsInvalidTypeDate() {
        final var params = new SingleStringParameters("");
        assertThatThrownBy(() -> params.createCellAs(DataType.getType(LocalDateCell.class)))
            .isInstanceOf(InvalidSettingsException.class)
            .hasMessageMatching("An empty string does not represent a valid \".*Date.*\"");

        final var missingDay = new SingleStringParameters("2025-12");
        assertThatThrownBy(() -> missingDay.createCellAs(DataType.getType(LocalDateCell.class)))
            .isInstanceOf(InvalidSettingsException.class)
            .hasMessageMatching("The string \".+\" does not represent a valid \".*Date.*\"");
    }

    @Test
    void testLoadFrom() {
        final var params = new SingleStringParameters("initial");
        params.loadFrom(new SingleStringParameters("loaded").createCell());
        assertEquals("loaded", params.m_value, "Expected value to be loaded correctly from other parameters");
    }

}
