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
 *   02.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.attributes;

import java.nio.file.attribute.FileTime;

/**
 * Basic attributes provider for the {@link FSFileAttributes}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FSBasicAttributes {

    private final FileTime m_lastModifiedTime;

    private final FileTime m_lastAccessTime;

    private final FileTime m_creationTime;

    private final long m_size;

    private final boolean m_isSymbolicLink;

    private final boolean m_isOther;

    /**
     * Constructs a basic attribute instance.
     *
     * @param lastModifiedTime last modified time of the file
     * @param lastAccessTime last access time of the file
     * @param creationTime creation time of the file
     * @param size size of the file
     * @param isSymbolic whether file is a symbolic link
     * @param isOther whether file is other
     */
    public FSBasicAttributes(final FileTime lastModifiedTime, final FileTime lastAccessTime,
        final FileTime creationTime, final long size, final boolean isSymbolic, final boolean isOther) {
        m_lastModifiedTime = lastModifiedTime;
        m_lastAccessTime = lastAccessTime;
        m_creationTime = creationTime;
        m_size = size;
        m_isSymbolicLink = isSymbolic;
        m_isOther = isOther;
    }

    /**
     * @return lastModifiedTime
     */
    public FileTime lastModifiedTime() {
        return m_lastModifiedTime;
    }

    /**
     * @return lastAccessTime
     */
    public FileTime lastAccessTime() {
        return m_lastAccessTime;
    }

    /**
     * @return m_creationTime
     */
    public FileTime creationTime() {
        return m_creationTime;
    }

    /**
     * @return isSymbolicLink
     */
    public boolean isSymbolicLink() {
        return m_isSymbolicLink;
    }

    /**
     * @return isOther
     */
    public boolean isOther() {
        return m_isOther;
    }

    /**
     * @return size
     */
    public long size() {
        return m_size;
    }

}
