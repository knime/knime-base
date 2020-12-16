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
package org.knime.filehandling.core.data.location;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.meta.DataColumnMetaDataCreator;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;

/**
 * {@link DataColumnMetaDataCreator} for {@link FSLocationValueMetaData}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FSLocationValueMetaDataCreator implements DataColumnMetaDataCreator<FSLocationValueMetaData> {

    private final Set<DefaultFSLocationSpec> m_specs;

    FSLocationValueMetaDataCreator() {
        this(new HashSet<>());
    }

    private FSLocationValueMetaDataCreator(final Set<DefaultFSLocationSpec> fsLocationSpecs) {
        m_specs = new HashSet<>(fsLocationSpecs);
    }

    @Override
    public void update(final DataCell cell) {
        if (cell.isMissing() || !(cell instanceof FSLocationValue)) {
            return;
        }
        final FSLocationValue value = (FSLocationValue)cell;
        final Set<DefaultFSLocationSpec> set = new HashSet<>();
        set.add(new DefaultFSLocationSpec(value.getFSLocation().getFileSystemCategory(),
            value.getFSLocation().getFileSystemSpecifier().orElse(null)));
        m_specs.addAll(set);
    }

    @Override
    public FSLocationValueMetaData create() {
        return new FSLocationValueMetaData(m_specs);
    }

    @Override
    public FSLocationValueMetaDataCreator copy() {
        return new FSLocationValueMetaDataCreator(m_specs);
    }

    @Override
    public FSLocationValueMetaDataCreator merge(final DataColumnMetaDataCreator<FSLocationValueMetaData> other) {
        CheckUtils.checkArgument(other instanceof FSLocationValueMetaDataCreator,
            "Can only merge with FSLocationValueMetaDataCreator but received object of type %s.",
            other.getClass().getName());
        final FSLocationValueMetaDataCreator otherCreator = (FSLocationValueMetaDataCreator)other;
        m_specs.addAll(otherCreator.m_specs);
        return this;
    }

    @Override
    public FSLocationValueMetaDataCreator merge(final FSLocationValueMetaData other) {
        m_specs.addAll(other.getFSLocationSpecs());
        return this;
    }

    @Override
    public Class<FSLocationValueMetaData> getMetaDataClass() {
        return FSLocationValueMetaData.class;
    }

}
