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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * Class for the ARFF reader which implements {@link Read}. We read as {@link DataCell} tokens.
 *
 * @author Dragan Keselj, KNIME GmbH
 *
 */
final class ARFFRead implements Read<DataCell> {

    private final long m_size;

    private final ARFFTable m_table;

    private final ARFFRowIterator m_rowIterator;

    /**
     * Constructor.
     *
     * @param path the {@link Path} to the file
     * @param config the {@link TableReadConfig} of the node
     * @param exec the execution monitor
     * @throws IOException
     *
     */
    ARFFRead(final FSPath path, final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        final var charset = Charset.defaultCharset();
        m_size = Files.size(path);
        m_table = new ARFFTable(path, charset, config);
        m_rowIterator = m_table.iterator();
    }

    /**
     * Reads and returns the next row
     */
    @Override
    public RandomAccessible<DataCell> next() throws IOException {
        if (!m_rowIterator.hasNext()) {
            return null;
        }
        final DataRow row = m_rowIterator.next();
        return RandomAccessibleUtils.createFromArray(row.stream().toArray(DataCell[]::new));
    }

    @Override
    public void close() throws IOException {
        m_rowIterator.close();
    }

    @Override
    public OptionalLong getMaxProgress() {
        return OptionalLong.of(m_size);
    }

    @Override
    public long getProgress() {
        return m_rowIterator.getReadBytes();
    }

    DataTableSpec getTableSpec() {
        return m_table.getDataTableSpec();
    }
}
