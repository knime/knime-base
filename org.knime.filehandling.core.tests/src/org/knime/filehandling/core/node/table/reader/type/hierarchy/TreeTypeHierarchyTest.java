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
 *   Jan 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.type.hierarchy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy.TreeTypeHierarchyBuilder;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;

/**
 * Contains unit tests for {@link TreeTypeHierarchy}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TreeTypeHierarchyTest {

    private static final String HANS = "Hans";

    private static final String RUDIGER = "Rudiger";

    private static final String PAUL = "Paul";

    private static final String EMMA = "Emma";

    private static final String ELFRIEDE = "Elfriede";

    /*-
     *        Elfriede(0)
     *         /     \
     *      Emma(1) Hans(2)
     *      /   \
     *  Paul(3) Rudiger(4)
     */
    private static TreeTypeHierarchyBuilder<String, Integer> createBuilder() {
        return TreeTypeHierarchy.builder(new TestingTypeTester(ELFRIEDE, 0))
            .addType(ELFRIEDE, new TestingTypeTester(EMMA, 1)).addType(EMMA, new TestingTypeTester(PAUL, 3))
            .addType(EMMA, new TestingTypeTester(RUDIGER, 4)).addType(ELFRIEDE, new TestingTypeTester(HANS, 2));
    }

    private static class TestingTypeTester implements TypeTester<String, Integer> {

        private final int m_number;

        private final String m_type;

        TestingTypeTester(final String type, final int number) {
            m_number = number;
            m_type = type;
        }

        @Override
        public boolean test(final Integer t) {
            return isInSubtree(t, m_number);
        }

        private static boolean isInSubtree(final int number, final int subtreeRoot) {
            if (number <= subtreeRoot) {
                return number == subtreeRoot;
            }
            return isInSubtree(number, subtreeRoot * 2 + 1) || isInSubtree(number, subtreeRoot * 2 + 2);
        }

        @Override
        public String getType() {
            return m_type;
        }

    }

    /**
     * Tests if {@link TreeTypeHierarchyBuilder#addType(Object, TypeTester)} fails on duplicate types.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTreeTypeHierarchyBuilderRejectsDuplicateTypes() {
        createBuilder().addType(HANS, new TestingTypeTester(PAUL, 5));
    }

    /**
     * Tests if {@link TreeTypeHierarchyBuilder#addType(Object, TypeTester)} fails if the provided parent type is
     * unknown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testTreeTypeHierarchyBuilderRejectsUnknownParentType() {
        createBuilder().addType("Wolfgang", new TestingTypeTester("Franz", 7));
    }

    /**
     * Tests the behavior of the {@link TypeResolver} returned by {@link TreeTypeHierarchy#createResolver()}.
     */
    @Test
    public void testTypeResolver() {
        // see the comment on createBuilder for the hierarchy
        final TreeTypeHierarchy<String, Integer> hierarchy = createBuilder().build();
        TypeResolver<String, Integer> resolver = hierarchy.createResolver();
        assertEquals("When the resolver hasn't seen any instances, it should return the most general type.", ELFRIEDE,
            resolver.getMostSpecificType());
        assertFalse(resolver.reachedTop());
        resolver.accept(4);
        assertEquals(RUDIGER, resolver.getMostSpecificType());
        assertFalse(resolver.reachedTop());
        resolver.accept(1);
        assertEquals(EMMA, resolver.getMostSpecificType());
        assertFalse(resolver.reachedTop());
        resolver.accept(3);
        assertEquals(EMMA, resolver.getMostSpecificType());
        resolver.accept(4);
        assertEquals(EMMA, resolver.getMostSpecificType());
        resolver.accept(1);
        assertEquals(EMMA, resolver.getMostSpecificType());
        resolver.accept(2);
        assertEquals(ELFRIEDE, resolver.getMostSpecificType());
        assertTrue(resolver.reachedTop());
        resolver = hierarchy.createResolver();
        resolver.accept(2);
        assertEquals(HANS, resolver.getMostSpecificType());
        assertFalse(resolver.reachedTop());
        resolver.accept(4);
        assertEquals(ELFRIEDE, resolver.getMostSpecificType());
        assertTrue(resolver.reachedTop());
    }

    /**
     * Tests the implementation of {@link TreeTypeHierarchy#createTypeFocusedHierarchy()}.
     */
    @Test
    public void testCreateTypeFocussedHierarchy() {
        final TreeTypeHierarchy<String, Integer> hierarchy = createBuilder().build();
        final TreeTypeHierarchy<String, String> typeFocussedHierarchy = hierarchy.createTypeFocusedHierarchy();
        TypeResolver<String, String> resolver = typeFocussedHierarchy.createResolver();
        assertEquals(ELFRIEDE, resolver.getMostSpecificType());
        resolver.accept(HANS);
        assertEquals(HANS, resolver.getMostSpecificType());
        resolver.accept(EMMA);
        assertEquals(ELFRIEDE, resolver.getMostSpecificType());
        resolver = typeFocussedHierarchy.createResolver();
        resolver.accept(PAUL);
        assertEquals(PAUL, resolver.getMostSpecificType());
        resolver.accept(EMMA);
        assertEquals(EMMA, resolver.getMostSpecificType());
        resolver.accept(ELFRIEDE);
        assertEquals(ELFRIEDE, resolver.getMostSpecificType());
        assertTrue(resolver.reachedTop());
    }

    /**
     * Tests the {@link Object#equals(Object)} implementation of the tree nodes used in {@link TreeTypeHierarchy}.
     */
    @Test
    public void testTreeNodeEquals() {
        TreeTypeHierarchy.TreeNode<String, Integer> elfriede =
            new TreeTypeHierarchy.TreeNode<>(null, new TestingTypeTester(ELFRIEDE, 0));
        assertTrue(elfriede.equals(elfriede));
        assertFalse(elfriede.equals(null));
        assertFalse(elfriede.equals("some object of a different type"));//NOSONAR
        TreeTypeHierarchy.TreeNode<String, Integer> emma =
            new TreeTypeHierarchy.TreeNode<>(elfriede, new TestingTypeTester(EMMA, 1));
        TreeTypeHierarchy.TreeNode<String, Integer> hans =
            new TreeTypeHierarchy.TreeNode<>(elfriede, new TestingTypeTester(HANS, 2));
        assertFalse(elfriede.equals(hans));
        assertFalse(elfriede.equals(emma));
        assertFalse(emma.equals(hans));
    }

}
