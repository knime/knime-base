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
 *   04.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Implementation of {@link SeekableByteChannel} for remote files systems that do not support seekable byte channels. In
 * this case the file is downloaded into a local temporary file from which a SeekableByteChannel is retrieved. Closing
 * the channel will upload the file if file was changed or is empty.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @param <P> Path type to use.
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class TempFileSeekableByteChannel<P extends FSPath> implements SeekableByteChannel {

    private final Path m_tempFile;

    private final SeekableByteChannel m_tempFileSeekableByteChannel;

    private boolean m_isClosed = false;

    private boolean m_isWritable;

    private boolean m_changed = false;

    private final P m_file;

    private final Set<? extends OpenOption> m_openOptions;

    /**
     * Creates a SeekableByteChannel for a remote file by copying the contents of the remote file to a local temporary
     * file and returning this seekable byte channel
     *
     * @param file the remote file
     * @param options options specifying how the file is opened
     * @throws IOException if an I/O error occurs
     */
    protected TempFileSeekableByteChannel(final P file, final Set<? extends OpenOption> options) throws IOException {
        m_file = file;
        m_openOptions = options;

        final String tmpDir = System.getProperty("java.io.tmpdir");
        m_tempFile = Paths.get(tmpDir, String.format("tempFSfile-%s-%s", UUID.randomUUID().toString().replace('-', '_'),
            m_file.getFileName().toString()));

        if (m_openOptions.contains(StandardOpenOption.APPEND) || m_openOptions.contains(StandardOpenOption.READ)) {
            try {
                copyFromRemote(m_file, m_tempFile);
            } catch (NoSuchFileException e) {
                // the file need not necessarily exist
            }
        }

        m_isWritable = options.contains(StandardOpenOption.APPEND) || options.contains(StandardOpenOption.WRITE);

        final Set<OpenOption> opts = new HashSet<>(options);
        opts.add(StandardOpenOption.CREATE);
        m_tempFileSeekableByteChannel = Files.newByteChannel(m_tempFile, opts);

        @SuppressWarnings("resource")
        FSFileSystem<? extends FSPath> fileSystem = file.getFileSystem();
        if (fileSystem instanceof BaseFileSystem) {
            ((BaseFileSystem<?>)fileSystem).registerCloseable(this);
        }
    }

    /**
     * Copies the content of the remote file to a local temporary file.
     *
     * @param remoteFile the remote file to copy from
     * @param tempFile the temporary file to copy to
     * @throws IOException if an I/O error occurs
     */
    public abstract void copyFromRemote(final P remoteFile, final Path tempFile) throws IOException;

    /**
     * Copies the content of the local remote file to the remote file.
     *
     * @param remoteFile the remote file to copy to
     * @param tempFile the temporary file to copy to
     * @throws IOException if an I/O error occurs
     */
    public abstract void copyToRemote(final P remoteFile, final Path tempFile) throws IOException;

    @Override
    public boolean isOpen() {
        return m_tempFileSeekableByteChannel.isOpen();
    }

    @SuppressWarnings("resource")
    @Override
    public void close() throws IOException {
        if (!m_isClosed) {
            final long size = m_tempFileSeekableByteChannel.size();

            m_tempFileSeekableByteChannel.close();

            // upload file if changed or is a new file
            if (m_isWritable && (m_changed || size == 0)) {
                copyToRemote(m_file, m_tempFile);
            }

            Files.delete(m_tempFile);
            m_isClosed = true;
            m_file.getFileSystem().unregisterCloseable(this);
        }
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        return m_tempFileSeekableByteChannel.read(dst);
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        final int bytes = m_tempFileSeekableByteChannel.write(src);
        m_changed = m_changed || bytes > 0;
        return bytes;
    }

    @Override
    public long position() throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        return m_tempFileSeekableByteChannel.position();
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        return m_tempFileSeekableByteChannel.position(newPosition);
    }

    @Override
    public long size() throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        return m_tempFileSeekableByteChannel.size();
    }

    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
        if (m_isClosed) {
            throw new ClosedChannelException();
        }
        m_tempFileSeekableByteChannel.truncate(size);
        m_changed = true;
        return this;
    }
}
