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
 *   Dec 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Default implementation of a {@link SourceGroup}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of source items (must implement {@link Object#equals(Object)} and {@link Object#hashCode()})
 * @noinstantiate non-public API
 * @noreference non-public API
 */
public final class DefaultSourceGroup<I> implements SourceGroup<I> {

    private final List<I> m_items;

    private final String m_id;

    /**
     * Constructor.
     *
     * @param id used to identify this source group
     * @param items the source items
     */
    public DefaultSourceGroup(final String id, final Collection<I> items) {
        m_items = new ArrayList<>(items);
        m_id = id;
    }

    @Override
    public Iterator<I> iterator() {
        return m_items.iterator();
    }

    @Override
    public String getID() {
        return m_id;
    }

    @Override
    public Stream<I> stream() {
        return m_items.stream();
    }

    @Override
    public int size() {
        return m_items.size();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultSourceGroup) {
            @SuppressWarnings("unchecked") // we check that obj is of type DefaultSourceGroup
            final DefaultSourceGroup<I> other = (DefaultSourceGroup<I>)obj;
            return this.m_id.equals(other.m_id) && m_items.equals(other.m_items);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()//
            .append(m_id)//
            .append(m_items)//
            .toHashCode();
    }

}