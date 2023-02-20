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
 *   20 Feb 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.valuelookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Tests for the {@code ValueLookupNodeModel}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@ExtendWith({ExecutionContextExtension.class})
class ValueLookupNodeModelTest {

    private static final ValueLookupNodeFactory FACTORY = new ValueLookupNodeFactory();
    private ValueLookupNodeModel m_model;
    private ValueLookupNodeSettings m_settings;


    @BeforeEach
    void setUp() {
        m_model = FACTORY.createNodeModel();
        m_settings = new ValueLookupNodeSettings();
    }

    @AfterEach
    void tearDown() {
        m_model = null;
        m_settings = null;
    }

    /**
     * Tests that we do not throw an exception when we do not configure any output columns from the dictionary table
     * and only append a match column (boolean column if a match was found). Previously, this had a problem when the
     * {@code m_dictValueCols} field was {@code null} (instead of an empty array). See AP-13269.
     *
     * @param ctx execution context
     * @throws Exception while executing node
     */
    @Test
    void testOutputOnlyMatchColumn(final ExecutionContext ctx) throws Exception {
        // input data table
        final var dts = new DataTableSpec(
            new DataColumnSpecCreator("StringCol", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("LongCol", LongCell.TYPE).createSpec()
        );
        final var container = ctx.createDataContainer(dts);
        Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), new StringCell("A"), new LongCell(0)),
            new DefaultRow(RowKey.createRowKey(1L), new StringCell("B"), new LongCell(1)),
            new DefaultRow(RowKey.createRowKey(2L), new StringCell("B"), new LongCell(2))
        ).forEach(container::addRowToTable);
        container.close();
        // dictionary data table
        final var dictDts = new DataTableSpec(
            new DataColumnSpecCreator("LongCol", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("StringCol", StringCell.TYPE).createSpec()
        );
        final var dictContainer = ctx.createDataContainer(dictDts);
        Stream.of(
            new DefaultRow(RowKey.createRowKey(0L), new LongCell(0), new StringCell("0")),
            new DefaultRow(RowKey.createRowKey(1L), new LongCell(1), new StringCell("1")),
            new DefaultRow(RowKey.createRowKey(2L), new LongCell(2), new StringCell("2"))
        ).forEach(dictContainer ::addRowToTable);
        dictContainer.close();
        m_settings.m_createFoundCol = true;
        m_settings.m_dictKeyCol = "LongCol";
        m_settings.m_lookupCol = "LongCol";
        final var res = m_model.execute(
            new BufferedDataTable[] { container.getTable(), dictContainer.getTable() }, ctx, m_settings)[0];
        assertEquals(3, res.size(), "Unexpected number of output rows");
        try (final var c = res.iterator()) {
            for (int i = 0; i < 3; i++) {
                assertTrue(c.hasNext(), "Table should have a next value");
                final var r = c.next();
                // "StringCol", "LongCol", "Match Found"
                final var found = ((BooleanCell)r.getCell(2)).getBooleanValue();
                assertTrue(found, "Expected to find a match");
            }
        }
    }
}
