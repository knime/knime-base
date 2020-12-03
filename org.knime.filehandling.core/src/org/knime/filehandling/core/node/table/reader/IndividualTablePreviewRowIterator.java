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
 *   Nov 15, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.knime.core.data.DataRow;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.node.table.reader.preview.PreviewExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Represents a {@link Read} of {@link RandomAccessible} as {@link DataRow} via a provided {@link Function rowMapper}.
 * Errors are reported to a {@link PreviewExecutionMonitor}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class IndividualTablePreviewRowIterator<I, V> extends PreviewRowIterator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(IndividualTablePreviewRowIterator.class);

    private final Read<I, V> m_read;

    private final CheckedExceptionFunction<RandomAccessible<V>, DataRow, Exception> m_rowMapper;

    private DataRow m_next;

    /**
     * Constructor.
     *
     * @param read the {@link Read} to use
     * @param rowMapper the row mapper
     */
    public IndividualTablePreviewRowIterator(final Read<I, V> read,
        final CheckedExceptionFunction<RandomAccessible<V>, DataRow, Exception> rowMapper) {
        m_rowMapper = rowMapper;
        m_read = read;
    }

    @Override
    public DataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final DataRow next = m_next;
        m_next = null;
        return next;
    }

    @Override
    public boolean hasNext() {
        if (m_next != null) {
            return true;
        }
        return fetchNext();
    }

    private boolean fetchNext() {
        try {
            final RandomAccessible<V> next = m_read.next();
            m_next = next == null ? null : m_rowMapper.apply(next);
        } catch (Exception e) {
            throw new PreviewIteratorException(e.getMessage(), e);
        }
        return m_next != null;
    }

    @Override
    public void close() {
        try {
            m_read.close();
        } catch (IOException ex) {
            // then don't close it
            LOGGER.debug("Failed to close read.", ex);
        }
    }

}