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
 *   17 Sep 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.filter.rowref;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;

/**
 * A class representing an array of bits written to and read from disk. Bits can be set, unset, skipped, and read
 * sequentially. The pointer that runs over the array can also be reset to the beginning of the array. While random
 * access in constant time is also possible, there is no API for it implemented yet.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class DiskBackedBitArray implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DiskBackedBitArray.class);

    // the date format for formatting the names of temporary files
    private final SimpleDateFormat m_dateFormat = new SimpleDateFormat("yyyyMMdd");

    // buffer size in byte
    static final int BUFFER_SIZE = 8192;

    // the length / size of the array, i.e., the amount of bits it holds
    private final long m_length;

    // a flag that is set to true once the array has been (lazily) initialized
    private boolean m_isInitialized;

    // the buffer to / from which bits are being written / read
    private byte[] m_buffer;

    // the file to which the buffer is being flushed to / read from
    private File m_file;

    // the class that makes it possible to read and write at any location to the underlying file
    private RandomAccessFile m_raf;

    // a flag that is set to true iff the end of the file has been reached and no more bits can be read / written
    private boolean m_eof;

    // offset (in byte) of current buffer
    private long m_offset;

    // bit position in current buffer byte
    private int m_pos;

    // a flag that is set to true once the array has been closed (and the underlying file deleted)
    private boolean m_isClosed;

    /**
     * Creates a new disk-backed bit array of a certain length that is initialized with zeros.
     *
     * @param length the length of the array, i.e., the amount of bits it holds
     */
    DiskBackedBitArray(final long length) {
        m_length = length;
        m_isInitialized = false;
        m_isClosed = false;
    }

    private void lazyInit() throws IOException {
        m_buffer = new byte[BUFFER_SIZE];
        final String fileName = "bit_array_" + m_dateFormat.format(new Date());
        m_file = FileUtil.createTempFile(fileName, ".bin");
        m_file.deleteOnExit();

        m_raf = new RandomAccessFile(m_file, "rw");
        for (long l = 0; l <= (m_length - 1) / (BUFFER_SIZE * 8); l++) {
            m_raf.write(m_buffer);
        }

        setPositionInternal(0);

        m_isInitialized = true;
    }

    private void flush() throws IOException {
        m_raf.seek(m_offset);
        m_raf.write(m_buffer);
    }

    private void setPositionInternal(final long pos) throws IOException {
        m_offset = pos / 8;
        m_pos = (int)pos % 8;
        m_eof = pos >= m_length;
        if (!m_eof) {
            m_raf.seek(m_offset);
            m_raf.read(m_buffer);
        }
    }

    /**
     * Resets the pointer to the beginning of the array.
     *
     * @throws IOException if anything goes wrong with disk I/O
     * @throws IllegalArgumentException if the position argument is greater than the size of the array or lesser than 0
     * @throws IllegalStateException if the array has already been closed
     */
    void setPosition(final long pos) throws IOException {
        if (pos >= m_length || pos < 0) {
            throw new IllegalArgumentException(
                String.format("Position %d out of bounds of array of length %d", m_offset * 8 + m_pos, m_length));
        }
        checkClosed();
        if (m_isInitialized) {
            flush();
        } else {
            lazyInit();
        }
        setPositionInternal(pos);
    }

    private void checkStateBeforeAccess() throws IOException {
        checkClosed();
        if (m_eof) {
            throw new ArrayIndexOutOfBoundsException(
                String.format("Position %d out of bounds of array of length %d", m_offset * 8 + m_pos, m_length));
        }
        if (!m_isInitialized) {
            lazyInit();
        }
    }

    private void checkClosed() {
        if (m_isClosed) {
            throw new IllegalStateException(
                "File backing the disk-based bit array has already been closed and deleted.");
        }
    }

    private void next() throws IOException {
        m_pos++;
        if (m_pos == BUFFER_SIZE * 8) {
            flush();
            m_offset += BUFFER_SIZE;
            m_raf.read(m_buffer);
            m_pos = 0;
        }
        if (m_offset * 8 + m_pos >= m_length) {
            flush();
            m_eof = true;
        }
    }

    /**
     * Skips the next bit, advancing the pointer by one position.
     *
     * @throws IOException if anything goes wrong with disk I/O
     * @throws ArrayIndexOutOfBoundsException if we have reached the end of the array
     * @throws IllegalStateException if the array has already been closed
     */
    void skipBit() throws IOException {
        checkStateBeforeAccess();
        next();
    }

    /**
     * Sets the next bit to 1, advancing the pointer by one position.
     *
     * @throws IOException if anything goes wrong with disk I/O
     * @throws ArrayIndexOutOfBoundsException if we have reached the end of the array
     * @throws IllegalStateException if the array has already been closed
     */
    void setBit() throws IOException {
        checkStateBeforeAccess();
        final int bytePos = m_pos / 8;
        final int bitPos = m_pos % 8;
        m_buffer[bytePos] = (byte)(m_buffer[bytePos] | (1 << bitPos));
        next();
    }

    /**
     * Returns the next bit, advancing the pointer by one position.
     *
     * @return the next bit in the array
     * @throws IOException if anything goes wrong with disk I/O
     * @throws ArrayIndexOutOfBoundsException if we have reached the end of the array
     * @throws IllegalStateException if the array has already been closed
     */
    boolean getBit() throws IOException {
        checkStateBeforeAccess();
        final int bytePos = m_pos / 8;
        final int bitPos = m_pos % 8;
        boolean result = (m_buffer[bytePos] & (1 << bitPos)) != 0; // NOSONAR rule does not apply
        next();
        return result;
    }

    /**
     * Returns the size / length of this array.
     *
     * @return the size of this array
     */
    public long size() {
        return m_length;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException if anything goes wrong with disk I/O
     */
    @Override
    public void close() throws IOException {
        m_isClosed = true;
        if (m_isInitialized) {
            m_raf.close();
            m_file.delete();
        }
    }

    @Override
    public String toString() {
        try {
            final long pos = m_offset * 8 + m_pos;
            final StringBuilder sb = new StringBuilder();
            setPosition(0);
            for (long l = 0; l < m_length; l++) {
                sb.append(getBit());
            }
            setPosition(pos);
            return sb.toString();
        } catch (IOException e) {
            LOGGER.debug("Unable to create String represenation.", e);
            return (e.getMessage());
        }
    }

}
