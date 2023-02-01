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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Can be used to represent a range in an index-based structure. It allows initializing the start or end of the range
 * from the back of the structure.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
public final class PositionRange {

    private final Position m_startPosition;

    private final Position m_endPosition;

    /**
     * 'row' or 'column' whether it is a row or column range for more concise error messages
     */
    private final String m_rangeSubject;

    /**
     * @param start The start position to access
     * @param end The end position to access
     * @param subject 'row' or 'column' whether it is a row or column range for more concise error messages
     * @throws InvalidSettingsException If the positions are not valid
     */
    public PositionRange(final Position start, final Position end, final String subject)
        throws InvalidSettingsException {

        m_startPosition = start;
        m_endPosition = end;
        m_rangeSubject = subject;
        validatePositions();
    }

    /**
     * @param start The start index to access
     * @param startCountFromEnd True, if negative indexing should be used for the start position
     * @param end The end index to access
     * @param endCountFromEnd True, if negative indexing should be used for the end position
     * @param subject 'row' or 'column' whether it is a row or column range for more concise error messages
     * @throws InvalidSettingsException If the positions are not valid
     */
    public PositionRange(final long start, final boolean startCountFromEnd, final long end,
        final boolean endCountFromEnd, final String subject) throws InvalidSettingsException {

        m_startPosition = new Position(start, startCountFromEnd, subject);
        m_endPosition = new Position(end, endCountFromEnd, subject);
        m_rangeSubject = subject;
        validatePositions();
    }

    /**
     * Resolves this range against the provided size. Positions that lie outside of the range are cropped to the
     * boundaries.
     *
     * @param size to resolve against
     * @return the resolved position range
     * @throws InvalidSettingsException if the resolved start position is greater than the resolved end position
     */
    public ResolvedPositionRange resolve(final long size) throws InvalidSettingsException {
        return new ResolvedPositionRange(resolvePosition(m_startPosition, size, "start"),
            resolvePosition(m_endPosition, size, "end"));
    }

    private long resolvePosition(final Position position, final long size, final String purpose)
        throws InvalidSettingsException {
        return position.resolvePosition(size).orElseThrow(
            () -> new InvalidSettingsException(String.format("The %s %s (%s) exceeds the actual size (%s).", purpose,
                m_rangeSubject, position.getPosition(), size)));
    }

    private void validatePositions() throws InvalidSettingsException {
        if (m_startPosition.isCountedFromEnd() == m_endPosition.isCountedFromEnd()) {
            if (m_startPosition.isCountedFromEnd()) {
                CheckUtils.checkSetting(m_startPosition.getPosition() >= m_endPosition.getPosition(),
                    "When counting from the end, the start %s (%s) has to be greater or equal to the end %s (%s).",
                    m_rangeSubject, m_startPosition.getPosition(), m_rangeSubject, m_endPosition.getPosition());
            } else {
                CheckUtils.checkSetting(m_startPosition.getPosition() <= m_endPosition.getPosition(),
                    "The start %s (%s) must be smaller than or equal to the end %s (%s).", m_rangeSubject,
                    m_startPosition.getPosition(), m_rangeSubject, m_endPosition.getPosition());
            }

        }
    }

    /**
     * A range in an index-based structure that is resolved against the actual size of the structure.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public final class ResolvedPositionRange {

        private long m_start;

        private long m_end;

        private ResolvedPositionRange(final long start, final long end) throws InvalidSettingsException {
            CheckUtils.checkSetting(start <= end,
                "The resolved start %s (%s) must be smaller than or equal to the resolved end %s (%s).", m_rangeSubject,
                start, m_rangeSubject, end);
            m_start = start;
            m_end = end;
        }

        /**
         * @return the start point of the range
         */
        public long getStart() {
            return m_start;
        }

        /**
         * @return the end point of the range (inclusive)
         */
        public long getEnd() {
            return m_end;
        }

    }

}
