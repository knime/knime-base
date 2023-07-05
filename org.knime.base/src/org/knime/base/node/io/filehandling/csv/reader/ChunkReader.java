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
 *   May 31, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * A reader for reading chunks of a file.
 * This is done by reading up to a specified number of bytes and then terminating after a specified end sequence.
 * Does not take care of Byte-Order-Marks, these have to be filtered out beforehand.
 *
 * The implementation is inspired by {@code sun.nio.cs.StreamDecoder} but is heavily refactored to fit our specific
 * use-case.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ChunkReader extends Reader {

    private final long m_chunkSize;

    private final ReadableByteChannel m_channel;

    private final ByteBuffer m_buffer;

    private final CharsetDecoder m_decoder;

    private final CharBuffer m_endSequence;

    private final int m_maxBytesPerChar;

    private long m_bytesDecoded;

    private boolean m_reachedEnd;

    /**
     * Constructor.
     *
     * @param channel to read the bytes from
     * @param charset used for decoding
     * @param chunkSize the number of bytes the reader reads before detecting the end sequence
     * @param endSequence that the reader terminates after when it read more than chunkSize bytes
     */
    public ChunkReader(final ReadableByteChannel channel, final Charset charset, final long chunkSize,
        final String endSequence) {
        this(channel, charset, chunkSize, endSequence, 8192);
    }

    /**
     * Constructor for testing.
     *
     * @param channel to read the bytes from
     * @param charset used for decoding
     * @param chunkSize the number of bytes the reader reads before detecting the end sequence
     * @param endSequence that the reader terminates after when it read more than chunkSize bytes
     * @param bufferSize the size of the internal ByteBuffer
     */
    ChunkReader(final ReadableByteChannel channel, final Charset charset, final long chunkSize,
        final String endSequence, final int bufferSize) {
        m_chunkSize = chunkSize;
        // same behavior as sun.nio.cs.StreamDecoder
        m_decoder = charset.newDecoder()//
            .onMalformedInput(CodingErrorAction.REPLACE)//
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
        m_channel = channel;
        var encoder = charset.newEncoder();
        m_maxBytesPerChar = (int)Math.ceil(encoder.maxBytesPerChar());
        m_endSequence = CharBuffer.wrap(endSequence);
        m_buffer = ByteBuffer.allocate(bufferSize);
        // ensure that buffer is empty
        m_buffer.flip();
    }

    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        } else if (m_reachedEnd) {
            return -1;
        }

        var charBuffer = CharBuffer.wrap(cbuf, off, len).slice();

        if (len * m_maxBytesPerChar < getNumBytesLeftInChunk()) {
            // all chars are guaranteed to be inside of the chunk
            return readIntoCharBuffer(charBuffer, len);
        }

        for (//
                var bytesLeftInChunk = getNumBytesLeftInChunk(); //
                charBuffer.hasRemaining() && bytesLeftInChunk > 0; //
                bytesLeftInChunk = getNumBytesLeftInChunk()) {
            int guaranteedCharsLeftInChunk = (int)Math.min(Integer.MAX_VALUE, bytesLeftInChunk / m_maxBytesPerChar);
            int charsLeftStartingInChunk = Math.max(1, guaranteedCharsLeftInChunk);
            int numCharsToRead = Math.min(charsLeftStartingInChunk, charBuffer.remaining());
            // read the characters that are guaranteed to start inside of the chunk
            int numCharsRead = readIntoCharBuffer(charBuffer, numCharsToRead);
            if (numCharsRead < numCharsToRead) {
                // reached the end of the input
                var numTotalRead = charBuffer.position();
                return numTotalRead == 0 ? -1 : numTotalRead;
            }
        }

        if (charBuffer.hasRemaining()) {
            readOutsideOfChunk(charBuffer, len);
        }
        if (charBuffer.position() == 0) {
            return -1;
        }
        return charBuffer.position();
    }

    private void readOutsideOfChunk(final CharBuffer charBuffer, final int len) throws IOException {

        var outsideChunkBuffer = CharBuffer.allocate(len - charBuffer.position());
        var charsReadOutsideOfChunk = readIntoCharBuffer(outsideChunkBuffer, outsideChunkBuffer.capacity());

        if (charsReadOutsideOfChunk < 0) {
            return;
        }
        outsideChunkBuffer.flip();
        truncateAfterEndSequence(outsideChunkBuffer);
        charBuffer.put(outsideChunkBuffer);
    }

    private void truncateAfterEndSequence(final CharBuffer outsideChunkBuffer) {
        while (outsideChunkBuffer.hasRemaining()) {
            if (outsideChunkBuffer.mismatch(m_endSequence) == m_endSequence.limit()) {
                // the end sequence is part of the output
                outsideChunkBuffer.position(outsideChunkBuffer.position() + m_endSequence.limit());
                m_reachedEnd = true;
                break;
            } else {
                // move forward
                outsideChunkBuffer.get();
            }
        }
        outsideChunkBuffer.flip();
    }

    private long getNumBytesLeftInChunk() {
        return m_chunkSize - m_bytesDecoded;
    }

    private int readIntoCharBuffer(final CharBuffer charBuffer, final int len) throws IOException {
        var oldLimit = charBuffer.limit();
        var oldPosition = charBuffer.position();
        try {
            charBuffer.limit(oldPosition + len);
            var endOfFile = false;
            for (;;) {//NOSONAR
                int posBeforeDecode = m_buffer.position();
                var result = m_decoder.decode(m_buffer, charBuffer, endOfFile);
                m_bytesDecoded += m_buffer.position() - posBeforeDecode;
                var action = reactToCoderResult(result, endOfFile, charBuffer, oldPosition);
                endOfFile = endOfFile || action.endOfFile();
                if (action.shouldBreak()) {
                    break;
                }
            }

            int numCharsRead = charBuffer.position() - oldPosition;
            if (endOfFile) {
                // flush the decoder
                m_decoder.reset();
                if (numCharsRead == 0) {
                    // we didn't read anything
                    return -1;
                }
            }
            return numCharsRead;
        } finally {
            // restore limit to the original limit
            charBuffer.limit(oldLimit);
        }
    }

    private enum Action {
        BREAK(true, false),
        CONTINUE_EOF(false, true),
        BREAK_EOF(true, true),
        CONTINUE(false, false);

        private final boolean m_break;

        private final boolean m_eof;

        Action(final boolean shouldBreak, final boolean eof) {
            m_break = shouldBreak;
            m_eof = eof;
        }

        boolean shouldBreak() {
            return m_break;
        }

        boolean endOfFile() {
            return m_eof;
        }
    }

    private Action reactToCoderResult(final CoderResult result, final boolean endOfFile, final CharBuffer charBuffer,
        final int positionBeforeDecoding) throws IOException {
        if (result.isUnderflow()) {
            return reactToUnderflow(endOfFile, charBuffer, positionBeforeDecoding);
        } else if (result.isOverflow()) {
            // reached the end of the CharBuffer
            return Action.BREAK;
        } else {
            result.throwException();
            throw new IllegalStateException("The CoderResult#throwException() did not throw an exception.");
        }
    }

    private Action reactToUnderflow(final boolean endOfFile, final CharBuffer charBuffer,
        final int positionBeforeDecoding) throws IOException {
        // there were not enough bytes in m_buffer
        if (endOfFile || !charBuffer.hasRemaining()) {
            // we already tried to read more bytes and failed
            // or there is no more space in the charBuffer anyway
            return Action.BREAK;
        }
        var numBytesRead = fillByteBuffer();
        if (numBytesRead < 0) {
            if ((charBuffer.position() - positionBeforeDecoding == 0) && (!m_buffer.hasRemaining())) {
                // nothing has been read and also nothing can be read
                return Action.BREAK_EOF;
            } else {
                m_decoder.reset();
                return Action.CONTINUE_EOF;
            }
        } else {
            return Action.CONTINUE;
        }
    }

    /**
     * Fills m_buffer by reading from m_channel via {@link ReadableByteChannel#read(ByteBuffer)}.
     *
     * @return the number of bytes read from the channel or -1 if the channel is closed or is already EOF.
     * @throws IOException if reading from the channel fails
     */
    private int fillByteBuffer() throws IOException {
        m_buffer.compact();
        var numRead = m_channel.isOpen() ? m_channel.read(m_buffer) : -1;
        m_buffer.flip();
        if (numRead < 0) {
            // reached the end
            return numRead;
        }
        return m_buffer.remaining();
    }

    @Override
    public void close() throws IOException {
        m_channel.close();
    }

    /**
     * @return the number of bytes that have been decoded so far
     */
    public long getByteCount() {
        return m_bytesDecoded;
    }

}
