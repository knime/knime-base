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
 *   Aug 16, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location;

import org.knime.core.data.DataCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait;
import org.knime.core.table.schema.traits.DataTrait.DictEncodingTrait.KeyType;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultStructDataTraits;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;

/**
 * {@link ValueFactory} for {@link FSLocationReadValue} and {@link FSLocationWriteValue} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FSLocationValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    @Override
    public FSLocationReadValue createReadValue(final StructReadAccess access) {
        return new FSLocationReadValue(access);
    }

    @Override
    public FSLocationWriteValue createWriteValue(final StructWriteAccess access) {
        return new FSLocationWriteValue(access);
    }

    @Override
    public DataSpec getSpec() {
        return new StructDataSpec(DataSpec.stringSpec(), DataSpec.stringSpec(), DataSpec.stringSpec());
    }

    @Override
    public DataTraits getTraits() {
        return DefaultStructDataTraits.builder()//
            .addInnerTraits(new DictEncodingTrait(KeyType.INT_KEY))//
            .addInnerTraits(new DictEncodingTrait(KeyType.LONG_KEY))//
            .addInnerTraits(new DictEncodingTrait(KeyType.LONG_KEY))//
            .build();
    }

    /**
     * {@link ReadValue} for {@link FSLocation FSLocations}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class FSLocationReadValue implements ReadValue, FSLocationValue {

        private final StringReadAccess m_fsCategory;

        private final StringReadAccess m_fsSpecifier;

        private final StringReadAccess m_path;

        private final SimpleFSLocationCell.Factory m_cellFactory = new SimpleFSLocationCell.Factory();

        private FSLocationReadValue(final StructReadAccess structAccess) {
            m_fsCategory = structAccess.getAccess(0);
            m_fsSpecifier = structAccess.getAccess(1);
            m_path = structAccess.getAccess(2);
        }

        @Override
        public FSLocation getFSLocation() {
            final var fsCategory = m_fsCategory.getStringValue();
            final var fsSpecifier = m_fsSpecifier.isMissing() ? null : m_fsSpecifier.getStringValue();
            final var path = m_path.getStringValue();
            return new FSLocation(fsCategory, fsSpecifier, path);
        }

        @Override
        public DataCell getDataCell() {
            return m_cellFactory.createCell(getFSLocation());
        }

    }

    /**
     * {@link WriteValue} for {@link FSLocation FSLocations}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class FSLocationWriteValue implements WriteValue<FSLocationValue> {

        private final StringWriteAccess m_fsCategory;

        private final StringWriteAccess m_fsSpecifier;

        private final StringWriteAccess m_path;

        private FSLocationWriteValue(final StructWriteAccess structAccess) {
            m_fsCategory = structAccess.getWriteAccess(0);
            m_fsSpecifier = structAccess.getWriteAccess(1);
            m_path = structAccess.getWriteAccess(2);
        }

        @Override
        public void setValue(final FSLocationValue value) {
            final var location = value.getFSLocation();
            setLocation(location);
        }

        /**
         * @param location to set
         */
        public void setLocation(final FSLocation location) {
            m_fsCategory.setStringValue(location.getFileSystemCategory());
            location.getFileSystemSpecifier().ifPresentOrElse(m_fsSpecifier::setStringValue, m_fsSpecifier::setMissing);
            m_path.setStringValue(location.getPath());
        }

    }

}
