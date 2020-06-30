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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SpecMergeMode}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class SpecMergeModeTest {

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy;

    @Mock
    private TypeResolver<String, String> m_typeResolver;

    /**
     * Sets up stubbings used by all methods.
     */
    @Before
    public void init() {
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
    }

    /**
     * Tests if {@link SpecMergeMode#FAIL_ON_DIFFERING_SPECS} returns the correct spec if the specs are the same.
     */
    @Test
    public void testFailOnDifferingSpecs() {
        TypedReaderTableSpec<String> spec =
            TypedReaderTableSpec.create(asList("berta", "frieda"), asList("foo", "bar"),
                asList(Boolean.TRUE, Boolean.TRUE));
        final List<TypedReaderTableSpec<String>> specs = asList(spec, spec);
        when(m_typeResolver.getMostSpecificType()).thenReturn("foo", "bar", "foo", "bar");
        TypedReaderTableSpec<String> actual = SpecMergeMode.FAIL_ON_DIFFERING_SPECS.mergeSpecs(specs, m_typeHierarchy);
        assertEquals(spec, actual);
    }

    /**
     * Tests if {@link SpecMergeMode#FAIL_ON_DIFFERING_SPECS} fails on specs with different names.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFailOnDifferingSpecsFailsOnDifferentNames() {
        TypedReaderTableSpec<String> spec1 = TypedReaderTableSpec.create(asList("berta", "frieda"),
            asList("foo", "bar"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> spec2 = TypedReaderTableSpec.create(asList("berta", "gerta"), asList("foo", "bar"),
            asList(Boolean.TRUE, Boolean.TRUE));
        SpecMergeMode.FAIL_ON_DIFFERING_SPECS.mergeSpecs(asList(spec1, spec2), m_typeHierarchy);
    }

    /**
     * Tests if {@link SpecMergeMode#FAIL_ON_DIFFERING_SPECS} fails on specs of different size.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFailOnDifferingSpecsFailsOnDifferentSizes() {
        TypedReaderTableSpec<String> spec1 = TypedReaderTableSpec.create(asList("berta", "frieda"),
            asList("foo", "bar"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> spec2 =
            TypedReaderTableSpec.create(asList("berta"), asList("foo"), asList(Boolean.TRUE));
        SpecMergeMode.FAIL_ON_DIFFERING_SPECS.mergeSpecs(asList(spec1, spec2), m_typeHierarchy);
    }

    /**
     * Tests if {@link SpecMergeMode#FAIL_ON_DIFFERING_SPECS} fails if no specs are provided.
     */
    @Test(expected = NoSuchElementException.class)
    public void testFailOnDifferingSpecsFailsOnEmptyCollection() {
        SpecMergeMode.FAIL_ON_DIFFERING_SPECS.mergeSpecs(Collections.emptyList(), m_typeHierarchy);
    }

    /**
     * Tests the {@link SpecMergeMode#INTERSECTION} implementation.
     */
    @Test
    public void testIntersection() {
        TypedReaderTableSpec<String> spec1 = TypedReaderTableSpec.create(asList("berta", "frieda"),
            asList("foo", "bar"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> spec2 = TypedReaderTableSpec.create(asList("hans", "frieda"), asList("foo", "bla"),
            asList(Boolean.TRUE, Boolean.TRUE));
        when(m_typeResolver.getMostSpecificType()).thenReturn("bar");
        TypedReaderTableSpec<String> expected =
            TypedReaderTableSpec.create(asList("frieda"), asList("bar"), asList(Boolean.TRUE));
        TypedReaderTableSpec<String> actual =
            SpecMergeMode.INTERSECTION.mergeSpecs(asList(spec1, spec2), m_typeHierarchy);
        assertEquals(expected, actual);
    }

    /**
     * Tests if {@link SpecMergeMode#INTERSECTION} fails if the intersection of all specs is empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIntersectionFailsOnEmptyIntersection() {
        TypedReaderTableSpec<String> spec1 = TypedReaderTableSpec.create(asList("berta", "frieda"),
            asList("foo", "bar"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> spec2 = TypedReaderTableSpec.create(asList("irmgard", "franz"),
            asList("foo", "bla"), asList(Boolean.TRUE, Boolean.TRUE));
        SpecMergeMode.INTERSECTION.mergeSpecs(asList(spec1, spec2), m_typeHierarchy);
    }

    /**
     * Tests if {@link SpecMergeMode#INTERSECTION} fails if no specs are provided.
     */
    @Test(expected = NoSuchElementException.class)
    public void testIntersectionFailsOnEmptyCollection() {
        SpecMergeMode.INTERSECTION.mergeSpecs(Collections.emptyList(), m_typeHierarchy);
    }

    /**
     * Tests the {@link SpecMergeMode#UNION} implementation.
     */
    @Test
    public void testUnion() {
        TypedReaderTableSpec<String> spec1 = TypedReaderTableSpec.create(asList("berta", "frieda"),
            asList("foo", "bar"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> spec2 = TypedReaderTableSpec.create(asList("hans", "frieda"), asList("foo", "bla"),
            asList(Boolean.TRUE, Boolean.TRUE));
        when(m_typeResolver.getMostSpecificType()).thenReturn("foo", "bar", "foo");
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList("berta", "frieda", "hans"),
            asList("foo", "bar", "foo"), asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> actual = SpecMergeMode.UNION.mergeSpecs(asList(spec1, spec2), m_typeHierarchy);
        assertEquals(expected, actual);
    }

    /**
     * Tests if {@link SpecMergeMode#UNION} fails if no specs are provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUnionFailsOnEmptyCollection() {
        SpecMergeMode.UNION.mergeSpecs(Collections.emptyList(), m_typeHierarchy);
    }

}
