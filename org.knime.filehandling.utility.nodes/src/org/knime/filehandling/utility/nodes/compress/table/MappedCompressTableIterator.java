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
 *   Apr 13, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.table;

import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressIterator;
import org.knime.filehandling.utility.nodes.truncator.PathToStringTruncator;
import org.knime.filehandling.utility.nodes.utils.iterators.ClosableIterator;

/**
 * Implementation of a {@link CompressIterator} that processed an input table and obtains the archive entry names from
 * another String or Path column.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class MappedCompressTableIterator extends AbstractCompressTableIterator {

    private final EntryNameIterator m_entryNameIter;

    /**
     * Constructor.
     *
     * @param table the input table
     * @param pathColIdx the column containing the paths that need to be compressed
     * @param connection the {@link FSConnection}
     * @param includeEmptyFolders flag indicating whether or not empty folder should be compressed
     * @param entyNameColIdx the index of the column storing the archive entry names
     */
    MappedCompressTableIterator(final BufferedDataTable table, final int pathColIdx, final FSConnection connection,
        final boolean includeEmptyFolders, final int entryNameColIdx) {
        super(table, pathColIdx, connection, includeEmptyFolders);
        m_entryNameIter = new EntryNameIterator(table, entryNameColIdx);
    }

    @Override
    public CompressEntry next() {
        final String entryName = m_entryNameIter.next();
        return createEntry(p -> new MappedTruncator(p, entryName));
    }

    @Override
    public void close() {
        super.close();
        m_entryNameIter.close();
    }

    private static class EntryNameIterator implements ClosableIterator<String> {

        private final CloseableRowIterator m_entryNameIter;

        private final int m_entryNameColIdx;

        private final long m_size;

        private final Function<DataCell, String> m_cellToStringConverter;

        EntryNameIterator(final BufferedDataTable table, final int entryNameColIdx) {
            m_entryNameIter = table.filter(TableFilter.materializeCols(entryNameColIdx)).iterator();
            m_entryNameColIdx = entryNameColIdx;
            m_size = table.size();
            if (table.getSpec().getColumnSpec(entryNameColIdx).getType().isCompatible(FSLocationValue.class)) {
                m_cellToStringConverter = c -> ((FSLocationValue)c).getFSLocation().getPath();
            } else {
                m_cellToStringConverter = c -> ((StringValue)c).getStringValue();
            }
        }

        @Override
        public boolean hasNext() {
            return m_entryNameIter.hasNext();
        }

        @Override
        public String next() {
            final DataCell cell = m_entryNameIter.next().getCell(m_entryNameColIdx);
            if (cell.isMissing()) {
                throw new MissingValueException((MissingValue)cell, "Missing values are not supported");
            }
            return m_cellToStringConverter.apply(cell);
        }

        @Override
        public void close() {
            m_entryNameIter.close();
        }

        @Override
        public long size() {
            return m_size;
        }

    }

    private static class MappedTruncator implements PathToStringTruncator {

        private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\/|\\\\");

        private final Path m_base;

        private final String m_entryNamePrefix;

        private String m_separator;

        @SuppressWarnings("resource")
        MappedTruncator(final Path base, final String entryNamePrefix) throws AccessDeniedException {
            m_base = base;
            m_separator = m_base.getFileSystem().getSeparator();
            m_entryNamePrefix = cleanPrefix(m_base, entryNamePrefix, m_separator);
        }

        private static String cleanPrefix(final Path base, String entryNamePrefix, final String separator)
            throws AccessDeniedException {
            final boolean isFolder = FSFiles.isDirectory(base);
            entryNamePrefix =
                SEPARATOR_PATTERN.matcher(entryNamePrefix).replaceAll(Matcher.quoteReplacement(separator));
            if (isFolder && !entryNamePrefix.endsWith(separator)) {
                entryNamePrefix += separator;
            }
            if (entryNamePrefix.isEmpty() || entryNamePrefix.equals(separator)) {
                throw new IllegalArgumentException("The archive entry name cannot be empty");
            } else if (!isFolder && entryNamePrefix.endsWith(separator)) {
                throw new IllegalArgumentException(
                    String.format("The file %s is mapped to a folder %s entry", base.toString(), entryNamePrefix));
            } else {
                return entryNamePrefix;
            }
        }

        @Override
        public String getTruncatedString(final Path path) {
            final String suffix = m_base.relativize(path).toString();
            return m_entryNamePrefix + suffix;
        }

    }

}
