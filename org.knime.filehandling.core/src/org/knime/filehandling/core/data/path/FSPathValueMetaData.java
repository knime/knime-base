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
package org.knime.filehandling.core.data.path;

import org.knime.core.data.meta.DataColumnMetaData;
import org.knime.core.data.meta.DataColumnMetaDataSerializer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Holds the information about the file system type and specifier in form of Strings.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class FSPathValueMetaData implements DataColumnMetaData {

    static final String CFG_FS_TYPE = "fs_type";

    static final String CFG_FS_SPECIFIER = "fs_specifier";

    private final String m_fileSystemType;

    private final String m_fileSystemSpecifier;

    /**
     * Creates a {@link FSPathValueMetaData} instance.
     *
     * @param fileSystemType the file system type
     * @param fileSystemSpecifier the file system specifier, can be {@code null}
     */
    public FSPathValueMetaData(final String fileSystemType, final String fileSystemSpecifier) {
        m_fileSystemType = fileSystemType;
        m_fileSystemSpecifier = fileSystemSpecifier;
    }

    /**
     * Returns the file system type.
     *
     * @return the file system type
     */
    public String getFileSystemType() {
        return m_fileSystemType;
    }

    /**
     * Returns the file system specifier.
     *
     * @return the file system specifier, can be {@code null}
     */
    public String getFileSystemSpecifier() {
        return m_fileSystemSpecifier;
    }

    /**
     * Serializer for {@link FSPathValueMetaData} objects.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    public static final class PathValueMetaDataSerializer implements DataColumnMetaDataSerializer<FSPathValueMetaData> {

        @Override
        public void save(final FSPathValueMetaData metaData, final ConfigWO config) {
            CheckUtils.checkNotNull(metaData, "The meta data provided to the serializer was null.");
            config.addString(CFG_FS_TYPE, metaData.getFileSystemType());
            config.addString(CFG_FS_SPECIFIER, metaData.getFileSystemSpecifier());
        }

        @Override
        public FSPathValueMetaData load(final ConfigRO config) throws InvalidSettingsException {
            final String fileSystemType = config.getString(CFG_FS_TYPE);
            final String fileSystemSpecifier = config.getString(CFG_FS_SPECIFIER);
            return new FSPathValueMetaData(fileSystemType, fileSystemSpecifier);
        }

        @Override
        public Class<FSPathValueMetaData> getMetaDataClass() {
            return FSPathValueMetaData.class;
        }

    }
}
