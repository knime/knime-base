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
 *   19.03.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Wrapper class for an {@link SeekableByteChannel} which registers itself at a {@link BaseFileSystem} in order to be
 * closed when the file system gets closed.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public class BaseSeekableByteChannel implements SeekableByteChannel {

    private final SeekableByteChannel m_seekableByteChannel;

    private final BaseFileSystem<?> m_fileSystem;

    /**
     * @param seekableByteChannel
     */
    public BaseSeekableByteChannel(final SeekableByteChannel seekableByteChannel, final BaseFileSystem<?> fileSystem) {
        m_seekableByteChannel = seekableByteChannel;
        m_fileSystem = fileSystem;
        m_fileSystem.registerCloseable(this);
    }

    @Override
    public boolean isOpen() {
        return m_seekableByteChannel.isOpen();
    }

    @Override
    public void close() throws IOException {
        try {
            m_seekableByteChannel.close();
        } finally {
            m_fileSystem.unregisterCloseable(this);
        }
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        return m_seekableByteChannel.read(dst);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int write(final ByteBuffer src) throws IOException {
        return m_seekableByteChannel.write(src);
    }

    @Override
    public long position() throws IOException {
        return m_seekableByteChannel.position();
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
        return m_seekableByteChannel.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        return m_seekableByteChannel.size();
    }

    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
        return m_seekableByteChannel.truncate(size);
    }
}
