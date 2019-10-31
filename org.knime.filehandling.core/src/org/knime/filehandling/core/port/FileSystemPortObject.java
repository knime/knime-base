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

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;

/**
 * File handling {@link PortObject} implementation.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class FileSystemPortObject extends AbstractSimplePortObject {

    /**Standard type.*/
    @SuppressWarnings("hiding")
    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(FileSystemPortObject.class);
    /**Optional type.*/
    @SuppressWarnings("hiding")
    public static final PortType TYPE_OPTIONAL =
            PortTypeRegistry.getInstance().getPortType(FileSystemPortObject.class, true);

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<FileSystemPortObject> {}

    private FileSystemPortObjectSpec m_spec;

    /**
     *
     */
    public FileSystemPortObject() {
        this(null);
    }

    /**
     * @param spec {@link FileSystemPortObjectSpec}
     *
     */
    public FileSystemPortObject(final FileSystemPortObjectSpec spec) {
        m_spec = spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        //TODO: Create nicer summary
        return m_spec.getFileSystemType() + ": " + m_spec.getFileSystemId() + "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        //TODO: Create nicer view with more information about the file system
        return super.getViews();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemPortObjectSpec getSpec() {
        return m_spec;
    }

    /**
     * @return the {@link FSConnection} if available
     */
    public Optional<FSConnection> getFileSystemConnection() {
        return FSConnectionRegistry.getInstance().retrieve(getSpec().getFileSystemId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws InvalidSettingsException, CanceledExecutionException {
        if (spec instanceof FileSystemPortObjectSpec) {
            m_spec = (FileSystemPortObjectSpec) spec;
        }
    }

    /**
     * @param inData {@link PortObject} array to search for optional {@link FileSystemPortObject} at given index
     * @param i index of optional {@link FileSystemPortObject}
     * @return {@link FSConnection} if available
     */
    public static Optional<FSConnection> getFileSystemConnection(final PortObject[] inData, final int i) {
        return (inData != null && inData.length > i && (inData[i] instanceof FileSystemPortObject))
                ? ((FileSystemPortObject) inData[i]).getFileSystemConnection() : Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FileSystemPortObject [m_fileSystemType=");
        builder.append(getSpec().getFileSystemType());
        builder.append(", m_fileSystemId=");
        builder.append(getSpec().getFileSystemId());
        builder.append("]");
        return builder.toString();
    }
}
