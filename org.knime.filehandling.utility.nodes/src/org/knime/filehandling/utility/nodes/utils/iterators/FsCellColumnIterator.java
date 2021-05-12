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
 *   Jan 29, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.utils.iterators;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;

/**
 * A {@link ClosableIterator} that processes an {@link BufferedDataTable} and iterates over an {@link FSLocationValue}
 * column.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class FsCellColumnIterator implements ClosableIterator<FSPath> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FsCellColumnIterator.class);

    private final FSPathProviderFactory m_pathProviderFactory;

    private final CloseableRowIterator m_iter;

    private final int m_pathColIdx;

    private final long m_size;

    private FSPathProvider m_pathProvider;

    /**
     * Constructor.
     *
     * @param table the input table containing a {@link FSLocationValue} column
     * @param pathColIdx the index of the {@link FSLocationValue} column
     * @param connection the {@link FSConnection}
     * @param metaData the {@link FSLocationValueMetaData}
     */
    public FsCellColumnIterator(final BufferedDataTable table, final int pathColIdx, final FSConnection connection, final FSLocationValueMetaData metaData) {
        m_pathProviderFactory = FSPathProviderFactory.newFactory(Optional.ofNullable(connection), metaData);
        m_iter = table.filter(TableFilter.materializeCols(pathColIdx)).iterator();
        m_pathColIdx = pathColIdx;
        m_size = table.size();
    }

    @Override
    public boolean hasNext() {
        if (m_iter.hasNext()) {
            return true;
        }
        closePathProvider();
        m_iter.close();
        return false;
    }

    private void closePathProvider() {
        if (m_pathProvider != null) {
            try {
                m_pathProvider.close();
            } catch (IOException e) {
                LOGGER.debug("Unable to close fs path provider.", e);
            }
        }
    }

    private void closePathProviderFactory() {
        if (m_pathProviderFactory != null) {
            try {
                m_pathProviderFactory.close();
            } catch (IOException e) {
                LOGGER.debug("Unable to close fs path provider factory.", e);
            }
        }
    }

    @Override
    public FSPath next() {
        return getPath(m_iter.next());

    }

    private FSPath getPath(final DataRow next) {
        closePathProvider();
        final DataCell cell = next.getCell(m_pathColIdx);
        if (cell.isMissing()) {
            throw new MissingValueException((MissingValue)cell, "Missing values are not supported");
        }
        final FSLocationValue pathValue = (FSLocationValue)cell;
        final FSLocation fsLocation = pathValue.getFSLocation();
        m_pathProvider = m_pathProviderFactory.create(fsLocation);
        return m_pathProvider.getPath();
    }

    @Override
    public void close() {
        closePathProvider();
        closePathProviderFactory();
    }

    @Override
    public long size() {
        return m_size;
    }
}
