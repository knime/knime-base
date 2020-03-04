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
 *   Feb 24, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.cell;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreKey;
import org.knime.core.data.filestore.FileStoreUtil;

/**
 * Holds information shared by multiple {@link FSLocationCell}s created by the same process as the file system type and
 * specifier.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class FSLocationCellMetaData {

    private static final MemoryAlertAwareGuavaCache CACHE = new MemoryAlertAwareGuavaCache();

    private final String m_fileSystemType;

    private final String m_fileSystemSpecifier;

    FSLocationCellMetaData(final FileStoreKey key, final String fileSystemType, final String fileSystemSpecifier) {
        this(fileSystemType, fileSystemSpecifier);
        CACHE.put(key, this);
    }

    /**
     * Constructor for the use during deserialization.
     */
    private FSLocationCellMetaData(final String fileSystemType, final String fileSystemSpecifier) {
        m_fileSystemType = fileSystemType;
        m_fileSystemSpecifier = fileSystemSpecifier;
    }

    /**
     * @return the file system type
     */
    String getFileSystemType() {
        return m_fileSystemType;
    }

    /**
     * @return the file system specifier, can be {@code null}
     */
    String getFileSystemSpecifier() {
        return m_fileSystemSpecifier;
    }

    void write(final FileStore fileStore) throws IOException {
        final File file = fileStore.getFile();
        synchronized (file) {
            if (!file.exists()) {
                try (final DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
                    os.writeUTF(m_fileSystemType);
                    final boolean specifierIsNull = m_fileSystemSpecifier == null;
                    os.writeBoolean(specifierIsNull);
                    if (!specifierIsNull) {
                        os.writeUTF(m_fileSystemSpecifier);
                    }
                }
            }
        }
    }

    static FSLocationCellMetaData read(final FileStore fileStore) throws ExecutionException {
        return CACHE.get(FileStoreUtil.getFileStoreKey(fileStore), () -> readFromFileStore(fileStore));
    }

    private static FSLocationCellMetaData readFromFileStore(final FileStore fileStore) throws IOException {
        try (final DataInputStream is = new DataInputStream(new FileInputStream(fileStore.getFile()))) {
            final String fileSystemType = is.readUTF();
            final boolean specifierIsNull = is.readBoolean();
            if (specifierIsNull) {
                return new FSLocationCellMetaData(fileSystemType, null);
            }
            final String fileSystemSpecifier = is.readUTF();
            return new FSLocationCellMetaData(fileSystemType, fileSystemSpecifier);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof FSLocationCellMetaData) {
            final FSLocationCellMetaData other = (FSLocationCellMetaData)obj;
            return m_fileSystemType.equals(other.m_fileSystemType)
                && Objects.equals(m_fileSystemSpecifier, other.m_fileSystemSpecifier);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_fileSystemType, m_fileSystemSpecifier);
    }
}
