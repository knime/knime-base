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
package org.knime.filehandling.core.data.path.cell;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.path.FSPathValue;

/**
 * Implementation of {@link FSPathValue} that stores the path within a {@link FSLocation} object.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class FSPathCell extends FileStoreCell implements FSPathValue {

    static final DataType TYPE = DataType.getType(FSPathCell.class);

    private static final long serialVersionUID = 1L;

    private FSPathCellMetaData m_metaData;

    private FSLocation m_location;

    private final String m_path;

    FSPathCell(final FSPathCellMetaData metaData, final FileStore fileStore, final FSLocation location) {
        super(fileStore);
        m_metaData = metaData;
        m_path = location.getPath();
        m_location = location;
    }

    private FSPathCell(final String path) {
        m_path = path;
    }

    @Override
    protected void flushToFileStore() throws IOException {
        m_metaData.write(getFileStores()[0]);
    }

    @Override
    public String toString() {
        return m_location.getPath();
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        final FSPathCell other = (FSPathCell)dc;
        return m_metaData.equals(other.m_metaData) && m_location.equals(other.m_location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_metaData, m_location);
    }

    @Override
    protected void postConstruct() throws IOException {
        try {
            m_metaData = FSPathCellMetaData.read(getFileStores()[0]);
            m_location = new FSLocation(m_metaData.getFileSystemType(), m_metaData.getFileSystemSpecifier(), m_path);
        } catch (ExecutionException ex) {
            throw new IOException("The meta data cannot be read.", ex);
        }
    }

    @Override
    public FSLocation getFSLocation() {
        return m_location;
    }

    /**
     * Serializer for {@link FSPathCell}s.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class PathSerializer implements DataCellSerializer<FSPathCell> {

        @Override
        public void serialize(final FSPathCell cell, final DataCellDataOutput output) throws IOException {
            output.writeUTF(cell.m_location.getPath());
        }

        @Override
        public FSPathCell deserialize(final DataCellDataInput input) throws IOException {
            final String path = input.readUTF();
            return new FSPathCell(path);
        }

    }
}
