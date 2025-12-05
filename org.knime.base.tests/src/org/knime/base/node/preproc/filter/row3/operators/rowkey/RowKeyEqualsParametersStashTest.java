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
 *   27 Nov 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rowkey;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.filter.row3.operators.defaults.CaseSensitivity;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;

/**
 * Tests for the applyStash method of RowKeyEqualsParameters, testing stashing from various cell types.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class RowKeyEqualsParametersStashTest {

    private RowKeyEqualsParameters m_rowKeyParams;

    @BeforeEach
    void setUp() {
        m_rowKeyParams = new RowKeyEqualsParameters();
    }

    @Test
    void testApplyStashWithEmptyArray() {
        m_rowKeyParams.m_value = "original";
        m_rowKeyParams.applyStash(new DataValue[0]);
        assertEquals("original", m_rowKeyParams.m_value, "Value should remain unchanged with empty stash array");
    }

    @Test
    void testApplyStashWithNullValue() {
        m_rowKeyParams.m_value = "original";
        m_rowKeyParams.applyStash(new DataValue[]{null});
        assertEquals("original", m_rowKeyParams.m_value, "Should ignore null value and keep original");
    }

    @Test
    void testApplyStashWithStringCell() {
        final var stringCell = new StringCell("Row42");
        m_rowKeyParams.applyStash(new DataValue[]{stringCell});
        assertEquals("Row42", m_rowKeyParams.m_value, "Should apply StringCell value");
    }

    @Test
    void testApplyStashWithEmptyString() {
        final var emptyStringCell = new StringCell("");
        m_rowKeyParams.applyStash(new DataValue[]{emptyStringCell});
        assertEquals("", m_rowKeyParams.m_value, "Should handle empty string");
    }

    @Test
    void testCaseSensitivityPreservedAfterStash() {
        m_rowKeyParams.m_caseSensitivity = CaseSensitivity.CASE_INSENSITIVE;
        final var stringCell = new StringCell("TestRowKey");
        m_rowKeyParams.applyStash(new DataValue[]{stringCell});
        assertEquals("TestRowKey", m_rowKeyParams.m_value, "Should stash string value");
        assertEquals(CaseSensitivity.CASE_INSENSITIVE, m_rowKeyParams.m_caseSensitivity,
            "Case sensitivity setting should be preserved after stash");
    }

    @Test
    void testStashingBetweenRowKeyEqualsParameters() {
        final var sourceParams = new RowKeyEqualsParameters("SourceRowKey");
        sourceParams.m_caseSensitivity = CaseSensitivity.CASE_INSENSITIVE;
        m_rowKeyParams.applyStash(sourceParams.stash());
        assertEquals("SourceRowKey", m_rowKeyParams.m_value,
            "Should stash row key value between RowKeyEqualsParameters");
        assertEquals(CaseSensitivity.CASE_SENSITIVE, m_rowKeyParams.m_caseSensitivity,
            "Case sensitivity should remain at default when stashing between parameters");
    }

}
