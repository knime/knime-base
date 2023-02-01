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
 *   Feb 1, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.table.utils;

import java.util.OptionalLong;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Can be used to represent a position in an index-based structure. Allows accessing elements from the back of the
 * structure.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
public final class Position {

    private final long m_position;

    private final boolean m_countFromEnd;

    /**
     * @param pos The position index to access
     * @param cFromEnd True, if negative indexing should be used
     * @param subject the subject of the position (e.g. row)
     * @throws InvalidSettingsException If <code>pos</code> is not greater than 0
     */
    public Position(final long pos, final boolean cFromEnd, final String subject) throws InvalidSettingsException {
        CheckUtils.checkSetting(pos > 0, "The %s position (%s) must be greater than 0.", subject, pos);
        m_position = pos;
        m_countFromEnd = cFromEnd;
    }

    /**
     * Resolves the position against an actual size.
     *
     * @param size to resolve the position against
     * @return the resolved position or {@link OptionalLong#empty()} if the position exceeds the size
     */
    public OptionalLong resolvePosition(final long size) {
        if (m_position > size) {
            return OptionalLong.empty();
        }

        if (m_countFromEnd) {
            return OptionalLong.of(size - m_position + 1);
        }
        return OptionalLong.of(m_position);

    }

    /**
     * @return The position
     */
    public long getPosition() {
        return m_position;
    }

    /**
     * @return True, if the position is counted from the end.
     */
    public boolean isCountedFromEnd() {
        return m_countFromEnd;
    }

}
