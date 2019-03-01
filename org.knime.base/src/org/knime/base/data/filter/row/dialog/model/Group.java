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

import java.util.Objects;

/**
 * A group object.
 *
 * @author Viktor Buria
 * @since 3.8
 */
public class Group extends AbstractElement {

    private GroupType m_type;

    /**
     * Creates a {@link Group}.
     *
     * @param id the ID
     */
    public Group(final int id) {
        super(id);
    }

    /**
     * Creates a {@link Group} with ID and group type.
     *
     * @param id the ID
     * @param type the {@link GroupType}
     */
    public Group(final int id, final GroupType type) {
        super(id);
        setType(type);
    }

    /**
     * Gets a group type.
     *
     * @return the {@link GroupType}
     */
    public GroupType getType() {
        return m_type;
    }

    /**
     * Sets a group type.
     *
     * @param type the {@link GroupType}
     */
    public void setType(final GroupType type) {
        m_type = type;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Group)) {
            return false;
        }
        final Group castOther = (Group)other;
        return Objects.equals(getId(), castOther.getId()) && Objects.equals(m_type, castOther.m_type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), m_type);
    }

    @Override
    public String toString() {
        return "Group [m_id=" + getId() + ", m_type=" + m_type + "]";
    }

}
