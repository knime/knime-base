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
 *   Sep 11, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo;

import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;

/**
 * Enum supporting the creation of {@link DataCell}s based on {@link KNIMEFileAttributes}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
enum KNIMEFileAttributesConverter {

        /**
         * Indicates whether or not the path is a file or folder/directory?
         */
        DIRECTORY(0, "Directory", BooleanCell.TYPE, k -> BooleanCellFactory.create(k.isDirectory())),

        /**
         * Size of the file/folder in bytes.
         */
        SIZE(1, "Size", LongCell.TYPE, k -> LongCellFactory.create(k.size())),

        /**
         * Size of the file/folder in bytes, in human readable form.
         */
        HUMANSIZE(2, "Size (human readable)", StringCell.TYPE,
            k -> StringCellFactory.create(FileUtils.byteCountToDisplaySize(k.size()))),

        /**
         * Last time the file/folder was modified.
         */
        LAST_MODIFIED_DATE(3, "Last modified date", LocalDateTimeCellFactory.TYPE, k -> {
            final Instant lastModified = k.lastModifiedTime().toInstant();
            if (lastModified.equals(Instant.EPOCH)) {
                return new MissingCell("No last modified date available");
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.ofInstant(lastModified, ZoneId.systemDefault()));
            }
        }),

        /**
         * The creation date of the file/folder.
         */
        CREATION_DATE(4, "Creation date", LocalDateTimeCellFactory.TYPE, k -> {
            final Instant created = k.creationTime().toInstant();
            if (created.equals(Instant.EPOCH)) {
                return new MissingCell("No creation date available");
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.ofInstant(created, ZoneId.systemDefault()));
            }
        }),

        /**
         * Indicates whether or not the file/folder exists. Always creates a {@code true} entry, as otherwise there
         * would not be {@link BasicFileAttributes}.
         */
        EXISTS(5, "Exists", BooleanCell.TYPE, k -> BooleanCellFactory.create(true));

    private final int m_position;

    private final String m_name;

    private final DataType m_type;

    private final Function<KNIMEFileAttributes, DataCell> m_cellCreator;

    /**
     * @param position position of this attribute
     * @param name name of this attribute
     * @param type cell type created by this attribute
     * @param cellCreator function to create the data cell
     */
    KNIMEFileAttributesConverter(final int position, final String name, final DataType type,
        final Function<KNIMEFileAttributes, DataCell> cellCreator) {
        m_position = position;
        m_name = name;
        m_type = type;
        m_cellCreator = cellCreator;
    }

    int getPosition() {
        return m_position;
    }

    String getName() {
        return m_name;
    }

    DataType getType() {
        return m_type;
    }

    DataCell createCell(final KNIMEFileAttributes attributes) {
        return m_cellCreator.apply(attributes);
    }
}
