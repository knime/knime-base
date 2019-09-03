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
 *   Aug 29, 2019 (bjoern): created
 */
package org.knime.filehandling.core.connections.base.attributes;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Simple standard implementation of {@link BasicFileAttributes}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class BasicFileAttributesImpl implements BasicFileAttributes {

    private final FileTime m_lastModifiedTime;

    private final FileTime m_lastAccessTime;

    private final FileTime m_creationTime;

    private final boolean m_isRegularFile;

    private final boolean m_isDirectory;

    private final boolean m_isSymbolicLink;

    private final boolean m_isOther;

    private final long m_size;

    private final Object m_fileKey;

    public BasicFileAttributesImpl(final FileTime lastModifiedTime,
        final FileTime lastAccessTime,
        final FileTime creationTime,
        final boolean isRegularFile,
        final boolean isDirectory,
        final boolean isSymbolicLink,
        final boolean isOther,
        final long size,
        final Object fileKey) {

        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastAccessTime;
        m_creationTime = creationTime;
        m_isRegularFile = isRegularFile;
        m_isDirectory = isDirectory;
        m_isSymbolicLink = isSymbolicLink;
        m_isOther = isOther;
        m_size = size;
        m_fileKey = fileKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastModifiedTime() {
        return m_lastModifiedTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastAccessTime() {
        return m_lastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime creationTime() {
        return m_creationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
        return m_isRegularFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return m_isDirectory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink() {
        return m_isSymbolicLink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOther() {
        return m_isOther;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return m_size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fileKey() {
        return m_fileKey;
    }

}
