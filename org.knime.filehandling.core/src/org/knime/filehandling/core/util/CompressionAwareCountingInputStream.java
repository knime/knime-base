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
 *   26 Nov 2020 (lars.schweikardt): created
 */
package org.knime.filehandling.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipException;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;

import com.google.common.io.CountingInputStream;

/**
 * Class which holds an {@link InputStream} and a {@link CountingInputStream} to be used in reader nodes which can read
 * .gz files.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public final class CompressionAwareCountingInputStream extends InputStream {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CompressionAwareCountingInputStream.class);

    private CountingInputStream m_countingStream;

    private InputStream m_inputStream;

    /**
     * Constructor.
     *
     * @param path {@link Path} of the file to be read
     * @throws IOException
     */
    public CompressionAwareCountingInputStream(final Path path) throws IOException {
        createStreams(path);
    }

    /**
     * This constructor will solely decorate the {@link InputStream} with a {@link CountingInputStream}.
     *
     * @param inputStream the {@link InputStream}
     */
    public CompressionAwareCountingInputStream(final InputStream inputStream) {
        m_inputStream = new CountingInputStream(inputStream);
    }

    /**
     * Returns the read bytes of the {@link CountingInputStream}.
     *
     * @return the already read bytes by the {@link CountingInputStream}
     */
    public long getCount() {
        return m_countingStream.getCount();
    }

    /**
     * Creates a {@link CountingInputStream} and an {@link InputStream} based on the file extension of
     *
     * @throws IOException
     */
    private void createStreams(final Path path) throws IOException {
        m_countingStream = new CountingInputStream(FSFiles.newInputStream(path));

        if (FileCompressionUtils.mightBeCompressed(path)) {
            try {
                m_inputStream = FileCompressionUtils.createDecompressedStream(m_countingStream);
            } catch (ZipException e) {
                LOGGER.debug("A ZIPException occurred while creating the the InputStream.", e);
                m_countingStream.close();
                m_countingStream = new CountingInputStream(Files.newInputStream(path));
                m_inputStream = m_countingStream;
            }
        } else {
            m_inputStream = m_countingStream;
        }
    }

    @Override
    public void close() throws IOException {
        m_inputStream.close();
    }

    @Override
    public int read() throws IOException {
        return m_inputStream.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return m_inputStream.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return m_inputStream.read(b, off, len);
    }
}
