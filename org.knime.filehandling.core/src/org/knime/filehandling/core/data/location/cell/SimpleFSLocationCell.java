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
 *   Feb 19, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.cell;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.DataCellFactoryMethod;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.FSLocationValue;

/**
 * Implementation of {@link FSLocationValue} that stores the path within a {@link FSLocation} object.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.3.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class SimpleFSLocationCell extends DataCell implements FSLocationValue {

    private static final long serialVersionUID = 1L;

    static final DataType TYPE = DataType.getType(SimpleFSLocationCell.class);

    private final FSLocation m_fsLocation;

    SimpleFSLocationCell(final FSLocation fsLocation) {
        m_fsLocation = fsLocation;
    }

    /**
     * Only used for type mapping use the other factory classes.
     * @see MultiSimpleFSLocationCellFactory
     * @see SimpleFSLocationCellFactory
     */
    public static class Factory implements DataCellFactory {

        /**
         * Factory method used in the type mapping framework to create a new {@link SimpleFSLocationCell}.
         *
         * @param location {@link FSLocation} of the new cell
         * @return {@link SimpleFSLocationCell} with the given {@link FSLocation}
         */
        @DataCellFactoryMethod(name = "FSLocation")
        public SimpleFSLocationCell createCell(final FSLocation location) {
            return new SimpleFSLocationCell(location);
        }

        @Override
        public DataType getDataType() {
            return TYPE;
        }

    }

    @Override
    public FSLocation getFSLocation() {
        return m_fsLocation;
    }

    @Override
    public String toString() {
        return m_fsLocation.getPath();
    }

    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        SimpleFSLocationCell other = (SimpleFSLocationCell)dc;
        return m_fsLocation.equals(other.m_fsLocation);
    }

    @Override
    protected boolean equalContent(final DataValue otherValue) {
        if (otherValue instanceof FSLocationValue) {
            return m_fsLocation.equals(((FSLocationValue)otherValue).getFSLocation());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {//NOSONAR equals is implemented abstractly in the super class
        return m_fsLocation.hashCode();
    }

    /**
     * Serializer for {@link SimpleFSLocationCell SimpleFSlocationCells}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @noreference This class is not intended to be referenced by clients
     */
    public static final class SimpleFSLocationCellSerializer implements DataCellSerializer<SimpleFSLocationCell> {

        @Override
        public void serialize(final SimpleFSLocationCell cell, final DataCellDataOutput output) throws IOException {
            final FSLocation fsLocation = cell.getFSLocation();
            output.writeUTF(fsLocation.getFileSystemCategory());
            final Optional<String> specifier = fsLocation.getFileSystemSpecifier();
            output.writeBoolean(specifier.isPresent());
            if (specifier.isPresent()) {
                output.writeUTF(specifier.get());
            }
            output.writeUTF(fsLocation.getPath());
        }

        @Override
        public SimpleFSLocationCell deserialize(final DataCellDataInput input) throws IOException {
            final String category = input.readUTF();
            final String specifier = input.readBoolean() ? input.readUTF() : null;
            final String path = input.readUTF();
            final FSLocation fsLocation = new FSLocation(category, specifier, path);
            return new SimpleFSLocationCell(fsLocation);
        }

    }

}
