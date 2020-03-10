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
 *   Oct 25, 2019 (Tobias): created
 */
package org.knime.filehandling.core.port;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;

/**
 * File handling {@link PortObjectSpec} implementation.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class FileSystemPortObjectSpec extends AbstractSimplePortObjectSpec {

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<FileSystemPortObjectSpec> {}

    private static final String CFG_FILE_SYSTEM_NAME = "fileSystemName";

    private static final String CFG_FILE_SYSTEM_ID = "fileSystemId";

    private String m_fileSystemType;

    private String m_fileSystemId;

    /**
     * Default constructor.
     */
    public FileSystemPortObjectSpec() {
        this(null, null);
    }

    /**
     * @param fileSystemType the file system type
     * @param fileSystemId unique file system id
     *
     */
    public FileSystemPortObjectSpec(final String fileSystemType, final String fileSystemId) {
        m_fileSystemType = fileSystemType;
        m_fileSystemId = fileSystemId;
    }

    /**
     * @return the fileSystemType
     */
    public String getFileSystemType() {
        return m_fileSystemType;
    }

    /**
     * @return the unique fileSystemId
     */
    public String getFileSystemId() {
        return m_fileSystemId;
    }

    /**
     * @return the {@link FSConnection} if available
     */
    public Optional<FSConnection> getFileSystemConnection() {
        return FSConnectionRegistry.getInstance().retrieve(getFileSystemId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model) {
        model.addString(CFG_FILE_SYSTEM_NAME, m_fileSystemType);
        model.addString(CFG_FILE_SYSTEM_ID, m_fileSystemId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_fileSystemType = model.getString(CFG_FILE_SYSTEM_NAME);
        m_fileSystemId = model.getString(CFG_FILE_SYSTEM_ID);
    }

    /**
     * @param inData {@link PortObjectSpec} array to search for optional {@link FileSystemPortObjectSpec} at given index
     * @param i index of optional {@link FileSystemPortObjectSpec} or -1
     * @return {@link FSConnection} if available
     */
    public static Optional<FSConnection> getFileSystemConnection(final PortObjectSpec[] inData, final int i) {
        return (inData != null && i >= 0 && inData.length > i && (inData[i] instanceof FileSystemPortObjectSpec))
                ? ((FileSystemPortObjectSpec) inData[i]).getFileSystemConnection() : Optional.empty();
    }

    /**
     * @param inData {@link PortObjectSpec} array to search for optional {@link FileSystemPortObjectSpec} at given index
     * @param i index of optional {@link FileSystemPortObjectSpec} or -1
     * @return file system type if available
     */
    public static Optional<String> getFileSystemType(final PortObjectSpec[] inData, final int i) {
        return (inData != null && i >= 0 && inData.length > i && (inData[i] instanceof FileSystemPortObjectSpec))
                ? Optional.of(((FileSystemPortObjectSpec) inData[i]).getFileSystemType()) : Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FileSystemPortObjectSpec [m_fileSystemType=");
        builder.append(m_fileSystemType);
        builder.append(", m_fileSystemId=");
        builder.append(m_fileSystemId);
        builder.append("]");
        return builder.toString();
    }


}
