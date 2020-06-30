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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Contains unit tests for {@link TypedReaderTableSpec}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TypedReaderTableSpecTest {

    private static final String BAR = "bar";

    private static final String FOO = "foo";

    private static final String FRANZ = "franz";

    private static final String HANS = "hans";

    private static final String GERTA = "gerta";

    private static final String ALFONS = "alfons";

    private static final String HUBERT = "hubert";

    private static final String BERTA = "berta";

    private static final String FRIEDA = "frieda";

    /**
     * Tests if {@link TypedReaderTableSpec#TypedReaderTableSpec(Collection)} fails on a {@code null} collection.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnListConstructorFailsOnNull() {
        new TypedReaderTableSpec<String>((Collection<TypedReaderColumnSpec<String>>)null);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#TypedReaderTableSpec(TypedReaderColumnSpec...)} fails on {@code null} array.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testColumnArrayConstructorFailsOnNull() {
        new TypedReaderTableSpec<String>((TypedReaderColumnSpec<String>[])null);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if the names argument is
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNullNames() {
        TypedReaderTableSpec.create(null, asList(FOO), asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the types argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNullTypes() {
        TypedReaderTableSpec.create(asList(FOO), null, asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if the hasTypes argument
     * is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNullHasTypes() {
        TypedReaderTableSpec.create(asList(FOO), asList(FOO), null);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if individual types are
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnIndividualNullType() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(FOO, null), asList(Boolean.TRUE, Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if individual hasTypes are
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnIndividualNullHasType() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(FOO, BAR), asList(Boolean.TRUE, null));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if the arguments differ in
     * size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnTypesShorter() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(FOO), asList(Boolean.TRUE, Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if the arguments differ in
     * size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnHasTypesShorter() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(FOO, BAR), asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection, Collection)} fails if the arguments differ in
     * size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromNameTypeFailsOnNamesShorter() {
        TypedReaderTableSpec.create(asList(FOO), asList(FOO, BAR), asList(Boolean.TRUE, Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the types argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithoutNameFailsOnNullTypes() {
        TypedReaderTableSpec.create(null, asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the hasTypes argument is
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithoutNameFailsOnNullHasTypes() {
        TypedReaderTableSpec.create(asList(FOO), null);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the types and hasTypes do not have
     * the same size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithoutNameFailsOnTypesShorter() {
        TypedReaderTableSpec.create(asList(FOO), asList(Boolean.TRUE, Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the types and hasTypes do not have
     * the same size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithoutNameFailsOnHasTypesShorter() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} returns a spec with zero columns provided
     * with empty array.
     */
    @Test
    public void testCreateWithoutNameEmpty() {
        final TypedReaderTableSpec<String> noColTableSpec =
            TypedReaderTableSpec.create(Collections.emptySet(), Collections.emptySet());
        assertEquals(noColTableSpec.size(), 0);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the types argument is {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnNullTypes() {
        TypedReaderTableSpec.create((Collection<String>)null, asList(Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if the hasTypes argument is
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnNullHasTypes() {
        TypedReaderTableSpec.create(asList(FOO), null);
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if individual types are {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnContainedNullTypes() {
        TypedReaderTableSpec.create(asList(FOO, null), asList(Boolean.TRUE, Boolean.TRUE));
    }

    /**
     * Tests if {@link TypedReaderTableSpec#create(Collection, Collection)} fails if individual hasTypes are
     * {@code null}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateFromTypeCollectionFailsOnContainedNullHasTypes() {
        TypedReaderTableSpec.create(asList(FOO, BAR), asList(Boolean.TRUE, null));
    }

    /**
     * Tests the creation if only types are provided.
     */
    @Test
    public void testCreateOnlyTypes() {
        final TypedReaderTableSpec<String> constructorArray = new TypedReaderTableSpec<>(
            TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true));
        final TypedReaderTableSpec<String> constructorList = new TypedReaderTableSpec<>(
            asList(TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true)));
        final TypedReaderTableSpec<String> createArray =
            TypedReaderTableSpec.create(asList(FRIEDA, BERTA), asList(Boolean.TRUE, Boolean.TRUE));
        final TypedReaderTableSpec<String> createType =
            TypedReaderTableSpec.create(asList(FRIEDA, BERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertEquals(TypedReaderColumnSpec.create(FRIEDA, true), constructorArray.getColumnSpec(0));
        assertEquals(TypedReaderColumnSpec.create(BERTA, true), constructorArray.getColumnSpec(1));
        assertEquals(constructorArray, constructorList);
        assertEquals(constructorArray, createArray);
        assertEquals(constructorArray, createType);
    }

    /**
     * Tests the creation if both names and types are provided.
     */
    @Test
    public void testCreateWithName() {
        final TypedReaderTableSpec<String> constructorArray =
            new TypedReaderTableSpec<>(TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true),
                TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true));
        final TypedReaderTableSpec<String> constructorList =
            new TypedReaderTableSpec<>(asList(TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true),
                TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true)));
        final TypedReaderTableSpec<String> createList = TypedReaderTableSpec.create(asList(HUBERT, ALFONS),
            asList(FRIEDA, BERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertEquals(TypedReaderColumnSpec.createWithName(HUBERT, FRIEDA, true), constructorArray.getColumnSpec(0));
        assertEquals(TypedReaderColumnSpec.createWithName(ALFONS, BERTA, true), constructorArray.getColumnSpec(1));
        assertEquals(constructorArray, constructorList);
        assertEquals(constructorArray, createList);
    }

    /**
     * Tests the creation of table spec with no columns.
     */
    @Test
    public void testCreateEmptyTableSpec() {
        //Array based constructor
        TypedReaderTableSpec<String> noColTableSpecArray = new TypedReaderTableSpec<>();
        assertEquals(noColTableSpecArray.size(), 0);

        // Collections based constructor
        final TypedReaderTableSpec<String> noColTableSpecCollection =
            new TypedReaderTableSpec<>(Collections.emptySet());
        assertEquals(noColTableSpecCollection.size(), 0);
    }

    /**
     * Tests the {@link TypedReaderTableSpec#hashCode()} implementation.
     */
    @Test
    public void testHashCode() {
        final TypedReaderTableSpec<String> spec1 =
            TypedReaderTableSpec.create(asList(FRIEDA, GERTA), asList(Boolean.TRUE, Boolean.TRUE));
        final TypedReaderTableSpec<String> spec2 =
            TypedReaderTableSpec.create(asList(FRIEDA, GERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertEquals(spec1.hashCode(), spec2.hashCode());
        final TypedReaderTableSpec<String> spec3 =
            TypedReaderTableSpec.create(asList(HANS), asList(FRANZ), asList(Boolean.TRUE));
        assertNotEquals(spec1, spec3);
        assertNotEquals(spec1.hashCode(), spec3.hashCode());
    }

    /**
     * Tests the {@link TypedReaderTableSpec#equals(Object)} implementation.
     */
    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEquals() {
        final TypedReaderTableSpec<String> spec1 =
            TypedReaderTableSpec.create(asList(FRIEDA, GERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertTrue(spec1 != null);
        assertTrue(spec1.equals(spec1));
        final TypedReaderTableSpec<String> spec2 =
            TypedReaderTableSpec.create(asList(FRIEDA, GERTA), asList(Boolean.TRUE, Boolean.TRUE));
        assertTrue(spec1.equals(spec2));
        assertTrue(spec2.equals(spec1));
        final TypedReaderTableSpec<String> spec3 =
            TypedReaderTableSpec.create(asList(HANS), asList(FRANZ), asList(Boolean.TRUE));
        assertFalse(spec1.equals(spec3));
        assertFalse(spec3.equals(spec1));
        assertFalse(spec1.equals(FOO));
    }

    /**
     * Tests the {@link TypedReaderTableSpec#iterator()} method.
     */
    @Test
    public void testIterator() {
        final TypedReaderTableSpec<String> spec =
            TypedReaderTableSpec.create(asList(FRIEDA, BERTA), asList(Boolean.TRUE, Boolean.TRUE));
        final Iterator<TypedReaderColumnSpec<String>> iterator = spec.iterator();
        assertTrue(iterator.hasNext());
        assertEquals(TypedReaderColumnSpec.create(FRIEDA, true), iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(TypedReaderColumnSpec.create(BERTA, true), iterator.next());
        assertFalse(iterator.hasNext());
    }

    /**
     * Tests the {@link TypedReaderTableSpec#stream()} method.
     */
    @Test
    public void testStream() {
        List<TypedReaderColumnSpec<String>> cols =
            asList(TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true));
        TypedReaderTableSpec<String> spec = new TypedReaderTableSpec<>(cols);
        assertEquals(cols, spec.stream().collect(toList()));

    }

    /**
     * Tests the {@link TypedReaderTableSpec#toString()} method.
     */
    @Test
    public void testToString() {
        List<TypedReaderColumnSpec<String>> cols =
            asList(TypedReaderColumnSpec.create(FRIEDA, true), TypedReaderColumnSpec.create(BERTA, true));
        TypedReaderTableSpec<String> spec = new TypedReaderTableSpec<>(cols);
        assertEquals(cols.toString(), spec.toString());
    }

}
