/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A container object to represent a hierarchical relationships between groups and conditions.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class Node {

    private final AbstractElement m_element;

    private Node m_parent;

    private final List<Node> m_children = new ArrayList<>();

    /**
     * Constructs a {@link Node}.
     *
     * @param element the group or condition element
     */
    public Node(final AbstractElement element) {
        m_element = Objects.requireNonNull(element, "element");
    }

    /**
     * Gets the wrapped condition or group.
     *
     * @return the wrapped element.
     */
    public AbstractElement getElement() {
        return m_element;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent {@link Node}
     */
    public Node getParent() {
        return m_parent;
    }

    /**
     * Sets the parent node.
     *
     * @param parent the parent {@link Node}
     */
    public void setParent(final Node parent) {
        m_parent = parent;
    }

    /**
     * Gets a list of children nodes.
     *
     * @return the {@link List} of children {@link Node}s
     */
    public List<Node> getChildren() {
        return Collections.unmodifiableList(m_children);
    }

    /**
     * Adds the child node.
     *
     * @param node the child {@link Node} to add
     */
    public void addChild(final Node node) {
        node.setParent(this);
        m_children.add(node);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Node)) {
            return false;
        }
        final Node castOther = (Node)other;
        return Objects.equals(m_element, castOther.m_element) && Objects.equals(m_children, castOther.m_children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_element, m_children);
    }

    @Override
    public String toString() {
        return "Node [m_element=" + m_element + ", m_children=" + m_children + "]";
    }

}
