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
 *   21.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Amazon S3 implementation of {@link DirectoryStream}
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public abstract class FSDirectoryStream implements DirectoryStream<Path> {

    private final Path m_path;

    private final Filter<? super Path> m_filter;

    private Iterator<Path> m_iterator;

    private volatile boolean m_isClosed = false;

    /**
     * Constructs a new instance of a {@link FSDirectoryStream} for the given path
     *
     * @param path the path to iterate over
     * @param filter the filter to apply to the output
     */
    public FSDirectoryStream(final Path path, final Filter<? super Path> filter) {
        m_path = path;
        m_filter = filter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_isClosed = true;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        if (m_isClosed) {
            throw new IllegalStateException("Directory stream is already closed.");
        }

        if (m_iterator != null) {
            throw new IllegalStateException("DirectoryStream supports only a single Iterator.");
        }

        m_iterator = getIterator(m_path, m_filter);
        return m_iterator;

    }

    /**
     * Returns a Iterator over the files in the folder.
     *
     * @param path the path of the folder
     * @param filter the filter to apply to the content
     * @return Returns a Iterator over the files in the folder.
     */
    protected abstract Iterator<Path> getIterator(Path path, Filter<? super Path> filter);

}
