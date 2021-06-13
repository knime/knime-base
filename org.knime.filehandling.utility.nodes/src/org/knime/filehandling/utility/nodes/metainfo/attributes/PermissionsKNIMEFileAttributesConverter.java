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
 *   Mar 3, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo.attributes;

import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;

/**
 * Enum supporting the creation of {@link DataCell}s with respect to file permissions based on
 * {@link KNIMEFileAttributes}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public enum PermissionsKNIMEFileAttributesConverter implements KNIMEFileAttributesConverter {

        /**
         * Indicates whether or not the file/folder is readable.
         */
        READABLE("Readable", BooleanCell.TYPE, KNIMEFileAttributes::isReadable),

        /**
         * Indicates whether or not the file/folder is writable.
         */
        WRITABLE("Writable", BooleanCell.TYPE, KNIMEFileAttributes::isWritable),

        /**
         * Indicates whether or not the file/folder is executable.
         */
        EXECUTABLE("Executable", BooleanCell.TYPE, KNIMEFileAttributes::isExecutable);

    static final DataCell MISSING_CELL =
        new MissingCell("The specified file system does not support permission lookups.");

    private final String m_name;

    private final DataType m_type;

    private final Function<KNIMEFileAttributes, Optional<Boolean>> m_valueAccessor;

    /**
     * Constructor.
     *
     * @param name name of this attribute
     * @param type cell type created by this attribute
     * @param valueAccessor function to access the permissions flag
     */
    private PermissionsKNIMEFileAttributesConverter(final String name, final DataType type,
        final Function<KNIMEFileAttributes, Optional<Boolean>> valueAccessor) {
        m_name = name;
        m_type = type;
        m_valueAccessor = valueAccessor;
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
        return m_valueAccessor.apply(attributes)//
            .map(BooleanCellFactory::create)//
            .orElse(MISSING_CELL);
    }
}
