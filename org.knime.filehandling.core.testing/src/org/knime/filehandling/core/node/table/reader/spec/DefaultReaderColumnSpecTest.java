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
 *   May 28, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

/**
 * Contains unit tests for {@link DefaultReaderColumnSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 */
@SuppressWarnings("javadoc")
public class DefaultReaderColumnSpecTest {

    private static final String FOO = "foo";

    private static final String FRIEDA = "frieda";

    /**
     * Tests the {@link DefaultReaderColumnSpec#DefaultReaderColumnSpec(String)} constructor.
     */
    @Test
    public void testCreation() {
        DefaultReaderColumnSpec noName = new DefaultReaderColumnSpec(FRIEDA);
        assertTrue(noName.getName().isPresent());
        assertTrue(noName.getName().get().equals(FRIEDA));
        assertFalse(noName.getName().get().equals(FOO));

        noName = new DefaultReaderColumnSpec(null);
        assertFalse(noName.getName().isPresent());
        assertEquals(Optional.empty(), noName.getName());
    }

    /**
     * Tests the equals implementation.
     */
    @Test
    public void testEquals() {
        final DefaultReaderColumnSpec spec = new DefaultReaderColumnSpec(FRIEDA);
        assertEquals(spec, spec);
        assertNotEquals(spec, null);
        assertNotEquals(spec, FOO);

        assertNotEquals(spec, TypedReaderTableSpec.create(Arrays.asList(FRIEDA), Arrays.asList(FOO)));
        final DefaultReaderColumnSpec same = new DefaultReaderColumnSpec(FRIEDA);
        assertEquals(spec, same);
        assertEquals(same, spec);

        final DefaultReaderColumnSpec different = new DefaultReaderColumnSpec(FOO);
        assertNotEquals(spec, different);
        assertNotEquals(different, spec);

    }

    /**
     * Tests the hashcode implementation.
     */
    @Test
    public void testHashCode() {
        final DefaultReaderColumnSpec spec = new DefaultReaderColumnSpec(FRIEDA);
        assertEquals(spec.hashCode(), spec.hashCode());
        assertNotEquals(spec, TypedReaderTableSpec.create(Arrays.asList(FRIEDA), Arrays.asList(FOO)));
        final DefaultReaderColumnSpec same = new DefaultReaderColumnSpec(FRIEDA);
        assertEquals(spec.hashCode(), same.hashCode());

        final DefaultReaderColumnSpec different = new DefaultReaderColumnSpec(FOO);
        assertNotEquals(spec.hashCode(), different.hashCode());
    }

    /**
     * Tests the toString implementation.
     */
    @Test
    public void testToString() {
        DefaultReaderColumnSpec spec = new DefaultReaderColumnSpec("hans");
        assertEquals("hans", spec.toString());
        spec = new DefaultReaderColumnSpec(null);
        assertEquals("<no name>", spec.toString());
    }

}
