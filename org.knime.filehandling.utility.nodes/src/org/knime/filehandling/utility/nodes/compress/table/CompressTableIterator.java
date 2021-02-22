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
 *   Feb 1, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.table;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.knime.core.node.BufferedDataTable;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressFileFolderEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressIterator;
import org.knime.filehandling.utility.nodes.utils.iterators.ClosableIterator;
import org.knime.filehandling.utility.nodes.utils.iterators.FsCellColumnIterator;

/**
 * An instance of {@link CompressIterator} processing a {@link BufferedDataTable} containing a path column.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class CompressTableIterator implements CompressIterator {

    private final ClosableIterator<FSPath> m_iterator;

    private final boolean m_includeEmptyFolders;

    CompressTableIterator(final BufferedDataTable table, final int pathColIdx, final FSConnection connection,
        final boolean includeEmptyFolders) {
        m_iterator = new FsCellColumnIterator(table, pathColIdx, connection);
        m_includeEmptyFolders = includeEmptyFolders;
    }

    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    @Override
    public CompressEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return new CompressFileFolderEntry(m_iterator.next(), m_includeEmptyFolders);
    }

    @Override
    public void close() throws IOException {
        m_iterator.close();
    }

    @Override
    public long size() {
        return m_iterator.size();
    }

}
