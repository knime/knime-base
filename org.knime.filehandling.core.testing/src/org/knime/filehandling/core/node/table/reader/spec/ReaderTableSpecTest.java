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
 *   Mar 25, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Contains unit tests for {@link ReaderTableSpec}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class ReaderTableSpecTest {

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(Collection)} fails on a {@code null} collection.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnListConstructorFailsOnNull() {
        new ReaderTableSpec<String>((Collection<ReaderColumnSpec<String>>)null);
    }

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(Collection)} fails on an empty collection.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnListConstructorFailsOnEmpty() {
        new ReaderTableSpec<String>(Collections.emptySet());
    }

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(ReaderColumnSpec...)} fails on {@code null} array.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnArrayConstructorFailsOnNull() {
        new ReaderTableSpec<String>((ReaderColumnSpec<String>[])null);
    }

    /**
     * Tests if {@link ReaderTableSpec#ReaderTableSpec(ReaderColumnSpec...)} fails on an empty array.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnArrayConstructorFailsOnEmpty() {
        new ReaderTableSpec<String>();
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if the names argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNullNames() {
        ReaderTableSpec.create(null, asList("foo"));
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if the types argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNullTypes() {
        ReaderTableSpec.create(asList("foo"), null);
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if individual types are {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnIndividualNullType() {
        ReaderTableSpec.create(asList("foo", "bar"), asList("foo", null));
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if the arguments differ in size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnTypesShorter() {
        ReaderTableSpec.create(asList("foo", "bar"), asList("foo"));
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if the arguments differ in size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNamesShorter() {
        ReaderTableSpec.create(asList("foo"), asList("foo", "bar"));
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection, Collection)} fails if the provided arguments are empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnEmpty() {
        ReaderTableSpec.create(Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Object...)} fails if the provided array is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeArrayFailsOnNull() {
        ReaderTableSpec.create((String[])null);
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Object...)} fails if no types are provided (the array is empty).
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeArrayFailsOnEmpty() {
        ReaderTableSpec.create();
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection)} fails if the argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnNull() {
        ReaderTableSpec.create((Collection<String>)null);
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection)} fails if individual types are {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnContainedNull() {
        ReaderTableSpec.create(asList("foo", null));
    }

    /**
     * Tests if {@link ReaderTableSpec#create(Collection)} fails if the provided collection is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnEmpty() {
        ReaderTableSpec.create(Collections.emptySet());
    }

    /**
     * Tests the creation if only types are provided.
     */
    @Test
    public void testCreateOnlyTypes() {
        final ReaderTableSpec<String> constructorArray =
            new ReaderTableSpec<>(ReaderColumnSpec.create("frieda"), ReaderColumnSpec.create("berta"));
        final ReaderTableSpec<String> constructorList =
            new ReaderTableSpec<>(asList(ReaderColumnSpec.create("frieda"), ReaderColumnSpec.create("berta")));
        final ReaderTableSpec<String> createArray = ReaderTableSpec.create("frieda", "berta");
        final ReaderTableSpec<String> createType = ReaderTableSpec.create(Arrays.asList("frieda", "berta"));
        assertEquals(ReaderColumnSpec.create("frieda"), constructorArray.getColumnSpec(0));
        assertEquals(ReaderColumnSpec.create("berta"), constructorArray.getColumnSpec(1));
        assertEquals(constructorArray, constructorList);
        assertEquals(constructorArray, createArray);
        assertEquals(constructorArray, createType);
    }

    /**
     * Tests the creation if both names and types are provided.
     */
    @Test
    public void testCreateWithName() {
        final ReaderTableSpec<String> constructorArray = new ReaderTableSpec<>(
            ReaderColumnSpec.createWithName("hubert", "frieda"), ReaderColumnSpec.createWithName("alfons", "berta"));
        final ReaderTableSpec<String> constructorList =
            new ReaderTableSpec<>(asList(ReaderColumnSpec.createWithName("hubert", "frieda"),
                ReaderColumnSpec.createWithName("alfons", "berta")));
        final ReaderTableSpec<String> createList =
            ReaderTableSpec.create(asList("hubert", "alfons"), Arrays.asList("frieda", "berta"));
        assertEquals(ReaderColumnSpec.createWithName("hubert", "frieda"), constructorArray.getColumnSpec(0));
        assertEquals(ReaderColumnSpec.createWithName("alfons", "berta"), constructorArray.getColumnSpec(1));
        assertEquals(constructorArray, constructorList);
        assertEquals(constructorArray, createList);
    }

    /**
     * Tests the {@link ReaderTableSpec#hashCode()} implementation.
     */
    @Test
    public void testHashCode() {
        final ReaderTableSpec<String> spec1 = ReaderTableSpec.create("frieda", "gerta");
        final ReaderTableSpec<String> spec2 = ReaderTableSpec.create("frieda", "gerta");
        assertEquals(spec1.hashCode(), spec2.hashCode());
        final ReaderTableSpec<String> spec3 = ReaderTableSpec.create(asList("hans"), asList("franz"));
        assertNotEquals(spec1, spec3);
        assertNotEquals(spec1.hashCode(), spec3.hashCode());
    }

    /**
     * Tests the {@link ReaderTableSpec#equals(Object)} implementation.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        final ReaderTableSpec<String> spec1 = ReaderTableSpec.create("frieda", "gerta");
        assertFalse(spec1.equals(null));
        assertTrue(spec1.equals(spec1));
        final ReaderTableSpec<String> spec2 = ReaderTableSpec.create("frieda", "gerta");
        assertTrue(spec1.equals(spec2));
        assertTrue(spec2.equals(spec1));
        final ReaderTableSpec<String> spec3 = ReaderTableSpec.create(asList("hans"), asList("franz"));
        assertFalse(spec1.equals(spec3));
        assertFalse(spec3.equals(spec1));
        assertFalse(spec1.equals("foo"));
    }

    /**
     * Tests the {@link ReaderTableSpec#iterator()} method.
     */
    @Test
    public void testIterator() {
        final ReaderTableSpec<String> spec = ReaderTableSpec.create("frieda", "berta");
        final Iterator<ReaderColumnSpec<String>> iterator = spec.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(ReaderColumnSpec.create("frieda"), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(ReaderColumnSpec.create("berta"), iterator.next());
        assertFalse(iterator.hasNext());
    }

    /**
     * Tests the {@link ReaderTableSpec#stream()} method.
     */
    @Test
    public void testStream() {
        List<ReaderColumnSpec<String>> cols =
            asList(ReaderColumnSpec.create("frieda"), ReaderColumnSpec.create("berta"));
        ReaderTableSpec<String> spec = new ReaderTableSpec<>(cols);
        assertEquals(cols, spec.stream().collect(toList()));

    }

    /**
     * Tests the {@link ReaderTableSpec#toString()} method.
     */
    @Test
    public void testToString() {
        List<ReaderColumnSpec<String>> cols =
            asList(ReaderColumnSpec.create("frieda"), ReaderColumnSpec.create("berta"));
        ReaderTableSpec<String> spec = new ReaderTableSpec<>(cols);
        assertEquals(cols.toString(), spec.toString());
    }

}
