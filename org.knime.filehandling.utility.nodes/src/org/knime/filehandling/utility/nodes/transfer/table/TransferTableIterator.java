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
 *   Feb 24, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.knime.core.node.BufferedDataTable;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.utility.nodes.pathtostring.PathToStringUtils;
import org.knime.filehandling.utility.nodes.transfer.iterators.FileAndFoldersCollector;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferIterator;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferPair;
import org.knime.filehandling.utility.nodes.utils.iterators.FsCellColumnIterator;

/**
 * A {@link TransferIterator} processing an input table that defines the source and destination pairs.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferTableIterator implements TransferIterator {

    private final FsCellColumnIterator m_sourceIterator;

    private final FsCellColumnIterator m_destionationIterator;

    TransferTableIterator(final BufferedDataTable table, final int srcColIdx, final int destColIdx,
        final FSConnection srcConnection, final FSConnection destConnection) {
        m_sourceIterator = new FsCellColumnIterator(table, srcColIdx, srcConnection);
        m_destionationIterator = new FsCellColumnIterator(table, destColIdx, destConnection);
    }

    @Override
    public boolean hasNext() {
        return m_sourceIterator.hasNext();
    }

    @Override
    public TransferEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return new TransferKnownTargetEntry(m_sourceIterator.next(), m_destionationIterator.next());
    }

    @Override
    public void close() {
        m_sourceIterator.close();
        m_destionationIterator.close();
    }

    @Override
    public long size() {
        return m_sourceIterator.size();
    }

    private static class TransferKnownTargetEntry implements TransferEntry {

        private final FSPath m_source;

        private final FSPath m_destination;

        /**
         * Constructor.
         *
         * @param source the source file/folder
         * @param destination the destination file/folder
         */
        TransferKnownTargetEntry(final FSPath source, final FSPath destination) {
            m_source = source;
            m_destination = destination;
        }

        @Override
        public TransferPair getSrcDestPair() throws IOException {
            return new TransferPair(m_source, m_destination);
        }

        @Override
        public List<TransferPair> getPathsToCopy() throws IOException {
            if (FSFiles.isDirectory(m_source)) {
                return FileAndFoldersCollector.getPaths(m_source).stream()//
                    .map(this::toTransferPair)//
                    .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }

        private TransferPair toTransferPair(final FSPath source) {
            // FIXME: handle ../ prefix (AP-16364)
            return new TransferPair(source,
                m_destination.resolve(PathToStringUtils.split(m_source.relativize(source))));
        }

    }
}
