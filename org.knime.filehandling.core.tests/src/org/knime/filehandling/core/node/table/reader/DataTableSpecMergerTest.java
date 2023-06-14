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
 *   Jun 14, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.StringCell;

/**
 * Unit tests for DataTableSpecMerger
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class DataTableSpecMergerTest {

    @Test
    void testTakesOnNameOfFirstSpec() throws Exception {
        var foo = new DataTableSpec("foo");
        var bar = new DataTableSpec("bar");
        assertEquals("foo", createMerger().merge(foo).merge(bar).createMergedSpec().getName(),
            "Should have name of first encountered spec.");
        assertEquals("bar", createMerger().merge(bar).merge(foo).createMergedSpec().getName(),
            "Should have name of first encountered spec.");
    }

    @Test
    void testUsesUnionOfProperties() throws Exception {
        var first = createSpecWithProperty("foo", "bar");
        var second = createSpecWithProperty("bar", "foo");
        assertEquals(Map.of("foo", "bar", "bar", "foo"),
            createMerger().merge(first).merge(second).createMergedSpec().getProperties(), "Unexpected properties.");
        assertEquals(Map.of("bar", "foo", "foo", "bar"),
            createMerger().merge(second).merge(first).createMergedSpec().getProperties(), "Unexpected properties.");
    }

    @Test
    void testUpdatesProperties() throws Exception {
        var first = createSpecWithProperty("foo", "bar");
        var second = createSpecWithProperty("foo", "baz");
        assertEquals(Map.of("foo", "baz"), createMerger().merge(first).merge(second).createMergedSpec().getProperties(),
            "Property should have been updated.");
    }

    private static DataTableSpec createSpecWithProperty(final String key, final String value) {
        var creator = new DataTableSpecCreator();
        creator.putProperty(key, value);
        return creator.createSpec();
    }

    private static DataTableSpecMerger createMerger() {
        return new DataTableSpecMerger();
    }

    @Test
    void testCreatesOrderedUnionOfColumns() throws Exception {
        var foo = new DataColumnSpecCreator("foo", StringCell.TYPE).createSpec();
        var bar = new DataColumnSpecCreator("bar", StringCell.TYPE).createSpec();
        var first = new DataTableSpec(foo);
        var second = new DataTableSpec(bar);
        assertEquals(new DataTableSpec(foo, bar), createMerger().merge(first).merge(second).createMergedSpec(),
            "Merged spec is incorrect.");
        assertEquals(new DataTableSpec(bar, foo), createMerger().merge(second).merge(first).createMergedSpec(),
            "Merged spec is incorrect.");
    }

    @Test
    void testComplainsIfMergeIsNotCalled() throws Exception {
        assertThrows(IllegalStateException.class, () -> createMerger().createMergedSpec());
    }

}
