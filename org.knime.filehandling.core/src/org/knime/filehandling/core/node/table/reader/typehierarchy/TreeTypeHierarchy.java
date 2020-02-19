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
package org.knime.filehandling.core.node.table.reader.typehierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.util.CheckUtils;

/**
 * A {@link TypeHierarchy} that can be viewed as a tree where the leafs represent the most specific types and the root
 * the most general type.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify data types
 * @param <V> the type of values
 */
public final class TreeTypeHierarchy<T, V> implements TypeFocusableTypeHierarchy<T, V> {

    private final Collection<TreeNode<T, V>> m_leaves;

    private final TreeNode<T, V> m_root;

    private TreeTypeHierarchy(final TreeNode<T, V> root, final Collection<TreeNode<T, V>> leafs) {
        m_root = root;
        m_leaves = leafs;
    }

    /**
     * Creates a {@link TreeTypeHierarchyBuilder} for building instances of {@link TreeTypeHierarchy}.
     *
     * @param root the {@link TypeTester} that tests for the most general type (must match all possible input values)
     * @return a {@link TreeTypeHierarchyBuilder builder} for building {@link TreeTypeHierarchy hierarchies}
     */
    public static <T, V> TreeTypeHierarchyBuilder<T, V> builder(final TypeTester<T, V> root) {
        return new TreeTypeHierarchyBuilder<>(root);
    }

    @Override
    public TypeResolver<T, V> createResolver() {
        return new TreeTypeResolver();
    }

    @Override
    public TreeTypeHierarchy<T, T> createTypeFocusedHierarchy() {
        final Map<TreeNode<T, ?>, Set<T>> childTypes = collectChildTypes();
        final TreeNode<T, T> root =
            new TreeNode<>(null, TypeTester.createTypeTester(m_root.getType(), t -> true, false));
        final List<TreeNode<T, T>> leaves = createNodesAndCollectLeaves(childTypes, root);
        return new TreeTypeHierarchy<>(root, leaves);
    }

    private List<TreeNode<T, T>> createNodesAndCollectLeaves(final Map<TreeNode<T, ?>, Set<T>> childTypes,
        final TreeNode<T, T> root) {
        final List<TreeNode<T, T>> leaves = new LinkedList<>();
        for (TreeNode<T, ?> child : m_root.m_children) {
            createTypeFocusedNode(root, child, childTypes, leaves);
        }
        return leaves;
    }

    private Map<TreeNode<T, ?>, Set<T>> collectChildTypes() {
        final Map<TreeNode<T, ?>, Set<T>> childTypes = new HashMap<>();
        collectChildTypes(m_root, childTypes);
        return childTypes;
    }

    private void createTypeFocusedNode(final TreeNode<T, T> parent, final TreeNode<T, ?> correspondingNode,
        final Map<TreeNode<T, ?>, Set<T>> typeMap, final List<TreeNode<T, T>> leaves) {
        final TreeNode<T, T> node = new TreeNode<>(parent,
            createTypeFocussedTypeTester(correspondingNode.getType(), typeMap.get(correspondingNode)));
        if (correspondingNode.m_children.isEmpty()) {
            leaves.add(node);
        } else {
            for (TreeNode<T, ?> child : correspondingNode.m_children) {
                createTypeFocusedNode(node, child, typeMap, leaves);
            }
        }
    }

    private void collectChildTypes(final TreeNode<T, ?> node, final Map<TreeNode<T, ?>, Set<T>> childTypes) {
        if (node.m_children.isEmpty()) {
            // we reached a leaf so the recursion stops
            childTypes.put(node, Collections.singleton(node.getType()));
        } else {
            // recursively collect types of children
            for (TreeNode<T, ?> child : node.m_children) {
                collectChildTypes(child, childTypes);
            }
            final Set<T> types = new HashSet<>();
            types.add(node.getType());
            // collect types of children
            for (TreeNode<T, ?> child : node.m_children) {
                types.addAll(childTypes.get(child));
            }
            childTypes.put(node, types);
        }
    }

    private static <T> TypeTester<T, T> createTypeFocussedTypeTester(final T type, final Set<T> subTypes) {
        final Set<T> supportedTypes = new HashSet<>(subTypes);
        supportedTypes.add(type);
        return TypeTester.createTypeTester(type, supportedTypes::contains, false);
    }

    private final class TreeTypeResolver implements TypeResolver<T, V> {

        private TreeWalker<T, V> m_walker = null;

        @Override
        public T getMostSpecificType() {
            if (m_walker == null) {
                // we haven't seen any value yet, hence we return the most general type
                return m_root.getType();
            } else {
                return m_walker.m_current.getType();
            }
        }

        @Override
        public void accept(final V value) {
            if (m_walker == null) {
                m_walker = initializeIterator(value);
            } else {
                m_walker.advanceUntilMatch(value);
            }
        }

        @Override
        public boolean reachedTop() {
            if (m_walker == null) {
                return false;
            }
            return m_walker.reachedTop();
        }

        private TreeWalker<T, V> initializeIterator(final V value) {
            // for each leaf we have to search its path to root and
            // the deepest node we find that matches is our entry point
            return m_leaves.stream()//
                .map(TreeWalker::new)//
                .map(w -> w.advanceUntilMatch(value))//
                .max((l, r) -> Integer.compare(l.getCurrent().getDepth(), r.getCurrent().getDepth()))//
                .orElseThrow(() -> new IllegalStateException(String.format("No match found for %s. This is"
                    + " illegal because the top most type in a hierarchy must match everything.", value)));
        }

    }

    /**
     * Builder for {@link TreeTypeHierarchy} objects.</br>
     * Assuming we have instances of {@code TypeTester<Class<?>, String>} {@code stringTester},
     * {@code doubleTester}, {@code integerTester}, {@code booleanTester} and want to create the hierarchy:
     *
     * <pre>
     *        String
     *        /    \
     *    Double  Boolean
     *     /
     * Integer
     * </pre>
     * The necessary code to create this hierarchy is:
     * <pre>
     * TreeTypeHierarchy<Class, String> hierarchy = TreeTypeHierarchy.builder(stringTester)
     *                                                .addType(String.class, doubleTester)
     *                                                .addType(Double.class, integerTester)
     *                                                .addType(String.class, booleanTester)
     * </pre>
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> the type representing individual data types
     * @param <V> the type representing values that belong to a particular data type
     */
    public static final class TreeTypeHierarchyBuilder<T, V> {

        private final Map<T, TreeNode<T, V>> m_nodes = new HashMap<>();

        private final Set<TreeNode<T, V>> m_leaves = new LinkedHashSet<>();

        private final TreeNode<T, V> m_root;

        private TreeTypeHierarchyBuilder(final TypeTester<T, V> root) {
            final TreeNode<T, V> rootNode = new TreeNode<>(null, root);
            m_nodes.put(root.getType(), rootNode);
            m_leaves.add(rootNode);
            m_root = rootNode;
        }

        /**
         * Adds the type identified by {@link TypeTester tester} to the hierarchy.</br>
         * The returned object is this instance in order to allow method chaining.
         *
         * @param parentType the type of the parent node (must already be known to the builder)
         * @param tester {@link TypeTester} identifying the type to add
         * @return this builder
         * @throws IllegalArgumentException if any of the arguments are {@code null} or if <b>parentType</b> is unknown
         *             to the builder
         */
        public TreeTypeHierarchyBuilder<T, V> addType(final T parentType, final TypeTester<T, V> tester) {
            CheckUtils.checkArgumentNotNull(tester, "The tester must not be null.");
            CheckUtils.checkArgumentNotNull(parentType, "The parentType must not be null.");
            CheckUtils.checkArgument(!m_nodes.containsKey(tester.getType()),
                "There is already a tester registered for type %s.", tester.getType());
            final TreeNode<T, V> parentNode = m_nodes.get(parentType);
            CheckUtils.checkArgument(parentNode != null, "Unknown parent type %s.", parentType);
            final TreeNode<T, V> node = new TreeNode<>(parentNode, tester);
            m_nodes.put(tester.getType(), node);
            // if the parent was a leaf before it definitely isn't one now
            m_leaves.remove(parentNode);
            // the newly added node doesn't have any children and is therefore a leaf node
            m_leaves.add(node);
            return this;
        }

        /**
         * Builds the hierarchy based on the nodes added so far.
         *
         * @return the {@link TreeTypeHierarchy} corresponding to the nodes added so far
         */
        public TreeTypeHierarchy<T, V> build() {
            return new TreeTypeHierarchy<>(m_root, new ArrayList<>(m_leaves));
        }

    }

    private static final class TreeWalker<T, V> {

        private TreeNode<T, V> m_current;

        TreeWalker(final TreeNode<T, V> start) {
            m_current = start;
        }

        public boolean reachedTop() {
            return !m_current.hasParent();
        }

        public TreeNode<T, V> advanceOneNode() {
            if (!m_current.hasParent()) {
                throw new NoSuchElementException();
            }
            m_current = m_current.getParent();
            return m_current;
        }

        private TreeNode<T, V> getCurrent() {
            return m_current;
        }

        private TreeWalker<T, V> advanceUntilMatch(final V value) {
            while (!m_current.test(value)) {
                CheckUtils.checkState(!reachedTop(),
                    "No match found for %s. "
                        + "This is illegal because the top most type in a hierarchy (%s) must match everything.",
                    value, m_current);
                advanceOneNode();
            }
            return this;
        }

    }

    /**
     * Represents an individual type in the type hierarchy.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static class TreeNode<T, V> implements TypeTester<T, V> {

        private final TreeNode<T, V> m_parent;

        private final TypeTester<T, V> m_tester;

        private final List<TreeNode<T, V>> m_children = new LinkedList<>();

        private final int m_depth;

        private final int m_hashCode;

        /**
         * Constructor. Also registers this node as a child of {@code parent} (provided it's not the root node).
         *
         * @param parent the direct parent node ({@code null} in case of the root)
         * @param tester that identifies the type represented by this node
         */
        TreeNode(final TreeNode<T, V> parent, final TypeTester<T, V> tester) {
            m_parent = parent;
            m_tester = tester;
            m_depth = parent == null ? 0 : m_parent.m_depth + 1;
            m_hashCode = new HashCodeBuilder().append(m_depth).append(m_tester).append(m_parent).toHashCode();
            if (m_parent != null) {
                m_parent.registerChild(this);
            }
        }

        @Override
        public String toString() {
            return m_tester.getType().toString();
        }

        @Override
        public boolean test(final V t) {
            return m_tester.test(t);
        }

        @Override
        public T getType() {
            return m_tester.getType();
        }

        private boolean hasParent() {
            return m_parent != null;
        }

        private TreeNode<T, V> getParent() {
            return m_parent;
        }

        private int getDepth() {
            return m_depth;
        }

        private void registerChild(final TreeNode<T, V> child) {
            m_children.add(child);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj == this) {
                return true;
            }
            if (obj instanceof TreeNode) {
                // The equals checks below ensure type safety
                @SuppressWarnings("rawtypes")
                TreeNode other = (TreeNode)obj;
                // The last check can be costly as it might check the whole path to root...
                return m_depth == other.m_depth && m_tester.equals(other.m_tester) && m_parent.equals(other.m_parent);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return m_hashCode;
        }

    }

}
