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
 *   May 15, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.knime.filehandling.core.util.BomEncodingUtils;

import com.google.common.io.CountingInputStream;

/**
 * A reader that after reading a specified number of bytes looks for a specified termination sequence of chars after
 * which it terminates even if there is more data to read.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class LimittedReader extends Reader {

    private final CountingInputStream m_countingStream;

    private final InputStreamReader m_streamReader;

    private final int m_maxBytesPerChar;

    private final long m_bytesToRead;

    private final char[] m_endSequence;

    private int m_endSequenceIndex;

    private boolean m_reachedEnd;

    /**
     * Constructor.
     *
     * @param stream to read from
     * @param charset for decoding the input stream
     * @param endSequence the sequence that designates the end of the reader
     * @param bytesToRead the number of bytes to read before the end sequence detection starts
     */
    public LimittedReader(final InputStream stream, final Charset charset, final String endSequence,
        final long bytesToRead) {
        m_bytesToRead = bytesToRead;
        m_countingStream = new CountingInputStream(stream);
        m_endSequence = endSequence.toCharArray();
        m_streamReader = BomEncodingUtils.createReader(m_countingStream, charset);
        m_maxBytesPerChar = (int)Math.ceil(charset.newDecoder().maxCharsPerByte());
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        } else if (m_reachedEnd) {
            return -1;
        }

        var numBytesLeft = m_bytesToRead - m_countingStream.getCount();
        if (len * m_maxBytesPerChar < numBytesLeft) {
            // all chars can be read without exceeding the limit
            return m_streamReader.read(cbuf, off, len);
        } else if (m_maxBytesPerChar < numBytesLeft) {
            // we can read at least one character safely
            return readSafeAndTryAgain(cbuf, off, len, numBytesLeft);
        } else {
            // read char by char to detect end sequence
            return readCharByChar(cbuf, off, len);
        }
    }

    private int readSafeAndTryAgain(final char[] cbuf, final int off, final int len, final long numBytesLeft)
        throws IOException {
        // casting to int is safe, otherwise we would have already returned above
        int numSafeChars = (int)(numBytesLeft / m_maxBytesPerChar);
        int numCharsRead = m_streamReader.read(cbuf, off, numSafeChars);
        if (numCharsRead < numSafeChars) {
            // reached the end of m_streamReader
            return numCharsRead;
        }
        // try reading again
        var numReadInRetry = read(cbuf, off + numCharsRead, len - numCharsRead);
        return numSafeChars + (numReadInRetry == -1 ? 0 : numReadInRetry);
    }

    private int readCharByChar(final char[] cbuf, final int off, final int len) throws IOException {
        for (int i = 0; i < len; i++) {//NOSONAR
            var character = read();
            if (character == -1) {
                // i == 0 means that m_streamReader is EOF
                return i == 0 ? -1 : i;
            }
            cbuf[off + i] = (char)character;
        }
        return len;
    }

    @Override
    public int read() throws IOException {
        if (m_reachedEnd) {
            return -1;
        }
        var character = m_streamReader.read();
        if (character == -1) {
            return -1;
        }
        if (lookingForEndSequence() && character == m_endSequence[m_endSequenceIndex]) {
            m_endSequenceIndex++;
            if (m_endSequenceIndex == m_endSequence.length) {
                m_reachedEnd = true;
            }
        } else {
            m_endSequenceIndex = 0;
        }
        return character;
    }

    private boolean lookingForEndSequence() {
        return m_countingStream.getCount() > m_bytesToRead;
    }

    @Override
    public void close() throws IOException {
        m_streamReader.close();
        m_countingStream.close();
    }

    /**
     * @return the number of bytes read from the underlying stream
     */
    public long getByteCount() {
        return m_countingStream.getCount();
    }

}
