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
 *   Nov 9, 2021 (Alexander Bondaletov): created
 */
package org.knime.filehandling.utility.nodes.metainfo.attributes;

import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;

/**
 * Enum supporting the creation of {@link DataCell}s with respect to {@link PosixFileAttributes} based on
 * {@link KNIMEFileAttributes}.
 *
 * @author Alexander Bondaletov
 */
public enum PosixKNIMEFileAttributesConverter implements KNIMEFileAttributesConverter {
        /**
         * The owner of the file
         */
        OWNER("Owner", a -> StringCellFactory.create(a.owner().getName())),
        /**
         * The file group owner
         */
        GROUP("Group", a -> StringCellFactory.create(a.group().getName())),
        /**
         * Read permission, owner.
         */
        OWNER_READ("Owner Readable", PosixFilePermission.OWNER_READ),
        /**
         * Write permission, owner.
         */
        OWNER_WRITE("Owner Writable", PosixFilePermission.OWNER_WRITE),
        /**
         * Execute/search permission, owner.
         */
        OWNER_EXECUTE("Owner Executable", PosixFilePermission.OWNER_EXECUTE),
        /**
         * Read permission, group.
         */
        GROUP_READ("Group Readable", PosixFilePermission.GROUP_READ),
        /**
         * Write permission, group.
         */
        GROUP_WRITE("Group Writable", PosixFilePermission.GROUP_WRITE),
        /**
         * Execute/search permission, group.
         */
        GROUP_EXECUTE("Group Executable", PosixFilePermission.GROUP_EXECUTE),
        /**
         * Read permission, others.
         */
        OTHERS_READ("Others Readable", PosixFilePermission.OTHERS_READ),
        /**
         * Write permission, others.
         */
        OTHERS_WRITE("Others Writable", PosixFilePermission.OTHERS_WRITE),
        /**
         * Execute/search permission, others.
         */
        OTHERS_EXECUTE("Others Executable", PosixFilePermission.OTHERS_EXECUTE);

    private final String m_name;

    private final DataType m_type;

    private final Function<PosixFileAttributes, DataCell> m_cellCreator;

    /**
     * Constructor.
     *
     * @param name name of this attribute
     * @param cellCreator function to create the data cell
     */
    private PosixKNIMEFileAttributesConverter(final String name,
        final Function<PosixFileAttributes, DataCell> cellCreator) {
        m_name = name;
        m_type = StringCell.TYPE;
        m_cellCreator = cellCreator;
    }

    private PosixKNIMEFileAttributesConverter(final String name, final PosixFilePermission permission) {
        m_name = name;
        m_type = BooleanCell.TYPE;
        m_cellCreator = a -> BooleanCellFactory.create(a.permissions().contains(permission));
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public DataType getType() {
        return m_type;
    }

    @Override
    public DataCell createCell(final KNIMEFileAttributes attributes) {
        try {
            return m_cellCreator.apply(attributes.getPosixAttributes());
        } catch (UnsupportedOperationException e) {
            return new MissingCell(e.getMessage());
        }
    }

}
