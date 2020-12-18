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
 *   Dec 14, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.io.IOException;
import java.io.Reader;

/**
 * A reader that replaces the line break characters from different file systems by a '\n'.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class OSIndependentNewLineReader extends Reader {

    /** The line break used to substitute all other line breaks. */
    public static final String LINE_BREAK = "\n";

    /** The underlying reader. */
    final Reader m_reader;

    /** If the next character is a line feed, skip it */
    private boolean m_skipLF = false;

    /**
     * Constructor.
     *
     * @param reader the underlying {@link Reader}
     */
    public OSIndependentNewLineReader(final Reader reader) {
        m_reader = reader;
    }

    static boolean isLineBreak(final String rowDelimiter) {
        return "\n".equals(rowDelimiter) || "\r\n".equals(rowDelimiter) || "\r".equals(rowDelimiter);
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        int bytesRead = m_reader.read(cbuf, off, len);
        bytesRead -= replaceLineBreaks(cbuf, off, off + bytesRead);
        if (bytesRead == 0) {
            return read(cbuf, off, 1);
        }
        return bytesRead;
    }

    private int replaceLineBreaks(final char[] cbuf, final int off, final int endIdx) {
        int moveIdx = 0;
        for (int i = off; i < endIdx - moveIdx; i++) {
            if (moveIdx > 0) {
                cbuf[i] = cbuf[i + moveIdx];
            }
            if (m_skipLF) {
                m_skipLF = false;
                if (cbuf[i] == '\n') {
                    ++moveIdx;
                    if ((i + moveIdx) == cbuf.length) { // NOSONAR rather increases the complexity than reducing it
                        return moveIdx;
                    }
                    cbuf[i] = cbuf[i + moveIdx];
                }
            }
            if (cbuf[i] == '\r') {
                cbuf[i] = '\n';
                m_skipLF = true;
            }
        }
        return moveIdx;
    }

    @Override
    public void close() throws IOException {
        m_reader.close();
    }

}
