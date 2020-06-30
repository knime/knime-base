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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Contains unit tests for {@link ReaderTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 *
 */
public class ReaderTableSpecTest {

    private static final String FRANZ = "franz";

    private static final String HANS = "hans";

    private static final String GERTA = "gerta";

    private static final String ALFONS = "alfons";

    private static final String HUBERT = "hubert";

    private static final String BERTA = "berta";

    private static final String FRIEDA = "frieda";

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(Collection)} fails on a {@code null} collection.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnListConstructorFailsOnNull() {
        new ReaderTableSpec<ReaderColumnSpec>((Collection<ReaderColumnSpec>)null);
    }

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(ReaderColumnSpec...)} fails on {@code null} array.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnArrayConstructorFailsOnNull() {
        new ReaderTableSpec<ReaderColumnSpec>((ReaderColumnSpec[])null);
    }

    /**
     * Tests the creation if both names and types are provided.
     */
    @Test
    public void testCreateWithName() {
        final ReaderTableSpec<ReaderColumnSpec> constructorArray = new ReaderTableSpec<>(
            TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true), TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true));
        final ReaderTableSpec<ReaderColumnSpec> constructorList =
            new ReaderTableSpec<>(asList(TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true),
                TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true)));
        final ReaderTableSpec<TypedReaderColumnSpec<String>> createList = TypedReaderTableSpec
            .create(asList(HUBERT, ALFONS), asList(FRIEDA, BERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertEquals(TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true), constructorArray.getColumnSpec(0));
        assertEquals(TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true), constructorArray.getColumnSpec(1));
        assertEquals(constructorArray, constructorList);
        assertNotEquals(constructorArray, createList);
        assertNotEquals(constructorList, createList);
    }

    /**
     * Tests the creation of table spec with no columns.
     */
    @Test
    public void testCreateEmptyTableSpec() {
        //Array based constructor
        ReaderTableSpec<ReaderColumnSpec> noColTableSpecArray = new ReaderTableSpec<>();
        assertEquals(noColTableSpecArray.size(), 0);

        // Collections based constructor
        final ReaderTableSpec<ReaderColumnSpec> noColTableSpecCollection =
            new ReaderTableSpec<>(Collections.emptySet());
        assertEquals(noColTableSpecCollection.size(), 0);
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection)} returns a spec with zero columns provided with empty
     * collection.
     */
    @Test
    public void testCreateFromTypeEmptyCollection() {
        final ReaderTableSpec<ReaderColumnSpec> noColTableSpec = ReaderTableSpec.createReaderTableSpec(Collections.emptySet());
        assertEquals(noColTableSpec.size(), 0);
    }

    /**
     * Tests the {@link TypedReaderTableSpec#hashCode()} implementation.
     */
    @Test
    public void testHashCode() {
        final ReaderTableSpec<ReaderColumnSpec> spec1 = ReaderTableSpec.createReaderTableSpec(asList(FRIEDA, GERTA));
        final ReaderTableSpec<ReaderColumnSpec> spec2 = ReaderTableSpec.createReaderTableSpec(asList(FRIEDA, GERTA));
        assertEquals(spec1.hashCode(), spec2.hashCode());
        final TypedReaderTableSpec<String> spec3 =
            TypedReaderTableSpec.create(asList(FRIEDA, HANS), asList(ALFONS, FRANZ), asList(Boolean.TRUE, Boolean.TRUE));
        assertNotEquals(spec1, spec3);
        assertNotEquals(spec1.hashCode(), spec3.hashCode());
    }

    /**
     * Tests the {@link ReaderTableSpec#equals(Object)} implementation.
     */
    @Test
    public void testEquals() {
        final ReaderTableSpec<ReaderColumnSpec> spec1 = ReaderTableSpec.createReaderTableSpec(asList(FRIEDA, GERTA));
        assertTrue(spec1 != null);
        assertTrue(spec1.equals(spec1));

        final ReaderTableSpec<ReaderColumnSpec> spec2 = ReaderTableSpec.createReaderTableSpec(asList(FRIEDA, GERTA));
        assertTrue(spec1.equals(spec2));
        assertTrue(spec2.equals(spec1));

        final TypedReaderTableSpec<String> spec3 =
            TypedReaderTableSpec.create(asList(FRIEDA, GERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertFalse(spec1.equals(spec3));
        assertFalse(spec3.equals(spec1));
    }

    /**
     * Tests the {@link ReaderTableSpec#iterator()} method.
     */
    @Test
    public void testIterator() {
        final ReaderTableSpec<ReaderColumnSpec> spec = ReaderTableSpec.createReaderTableSpec(asList(FRIEDA, BERTA));
        final Iterator<ReaderColumnSpec> iterator = spec.iterator();
        assertTrue(iterator.hasNext());
        ReaderColumnSpec next = iterator.next();
        assertTrue(next.getName().isPresent() && next.getName().get().equals(FRIEDA));
        assertTrue(iterator.hasNext());
        next = iterator.next();
        assertTrue(next.getName().isPresent() && next.getName().get().equals(BERTA));
        assertFalse(iterator.hasNext());
    }

    /**
     * Tests the {@link ReaderTableSpec#stream()} method.
     */
    @Test
    public void testStream() {
        List<TypedReaderColumnSpec<String>> cols =
            asList(TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true));
        TypedReaderTableSpec<String> spec = new TypedReaderTableSpec<>(cols);
        assertEquals(cols, spec.stream().collect(toList()));

    }

    /**
     * Tests the {@link ReaderTableSpec#toString()} method.
     */
    @Test
    public void testToString() {
        List<TypedReaderColumnSpec<String>> cols =
            asList(TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true));
        TypedReaderTableSpec<String> spec = new TypedReaderTableSpec<>(cols);
        assertEquals(cols.toString(), spec.toString());
    }

}
