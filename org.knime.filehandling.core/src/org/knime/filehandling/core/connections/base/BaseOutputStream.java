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
 *   15.01.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper for {@link OutputStream} that is closed when the file system is closed.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public class BaseOutputStream extends OutputStream {

    private final OutputStream m_outputStream;

    private final BaseFileSystem<?> m_fileSystem;

    /**
     * Wraps the given outputStream and registers it at the file system.
     *
     * @param outputStream outputStreamt to wrap
     * @param fileSystem the handling file system
     */
    public BaseOutputStream(final OutputStream outputStream, final BaseFileSystem<?> fileSystem) {
        m_outputStream = outputStream;
        m_fileSystem = fileSystem;
        m_fileSystem.addCloseable(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int b) throws IOException {
        m_outputStream.write(b);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte b[]) throws IOException {
        m_outputStream.write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte b[], final int off, final int len) throws IOException {
        m_outputStream.write(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        m_outputStream.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        try {
            m_outputStream.close();
        } finally {
            m_fileSystem.notifyClosed(this);
        }
    }

}
