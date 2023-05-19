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

import java.util.OptionalLong;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleDecorator;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;

/**
 * Generic default implementation of {@link IndividualTableReader}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public final class DefaultIndividualTableReader<V> implements IndividualTableReader<V> {

    private final RowKeyGenerator<V> m_rowKeyGenerator;

    private final RandomAccessibleDecorator<V> m_mapper;

    private final TypeMapper<V> m_typeMapper;

    private final DataCell m_identifierCell;

    /**
     * Constructor.
     *
     * @param typeMapper maps from {@link RandomAccessible RandomAccessibles} to {@link DataRow DataRows} given a
     *            {@link RowKey}
     * @param indexMapper represents the mapping from the global columns to the columns in the individual table
     * @param rowKeyGenerator creates {@link RowKey RowKeys} from {@link RandomAccessible RandomAccessibles.}
     * @param identifierCell cell containing the item identifier or null if no identifier should be appended
     */
    public DefaultIndividualTableReader(final TypeMapper<V> typeMapper, final RandomAccessibleDecorator<V> indexMapper,
        final RowKeyGenerator<V> rowKeyGenerator, final DataCell identifierCell) {
        m_mapper = indexMapper;
        m_rowKeyGenerator = rowKeyGenerator;
        m_typeMapper = typeMapper;
        m_identifierCell = identifierCell;
    }

    @Override
    public DataRow toRow(final RandomAccessible<V> randomAccessible) throws Exception {
        m_mapper.set(randomAccessible);
        final RowKey key = m_rowKeyGenerator.createKey(randomAccessible);
        // reads the tokens from m_readAdapter and converts them into a DataRow
        final DataRow row = m_typeMapper.map(key, m_mapper);
        if (m_identifierCell != null) {
            return new AppendedColumnRow(row, m_identifierCell);
        } else {
            return row;
        }
    }

    @Override
    public void fillOutput(final Read<V> read, final RowOutput output, final ExecutionMonitor progress)
        throws Exception {
        final OptionalLong maxProgress = read.getMaxProgress();
        if (maxProgress.isPresent()) {
            fillOutputWithProgress(read, output, progress, maxProgress.getAsLong());
        } else {
            fillOutputWithoutProgress(read, output, progress);
        }
    }

    private void fillOutputWithoutProgress(final Read<V> read, final RowOutput output,
        final ExecutionMonitor progress) throws Exception {
        RandomAccessible<V> next;
        for (long i = 1; (next = read.next()) != null; i++) {
            progress.checkCanceled();
            if (reportProgress(i)) {
                final long finalI = i;
                progress.setMessage(() -> String.format("Reading row %s", finalI));
            }
            output.push(toRow(next));
        }
    }

    private void fillOutputWithProgress(final Read<V> read, final RowOutput output,
        final ExecutionMonitor progress, final double size) throws Exception {
        RandomAccessible<V> next;
        for (long i = 1; (next = read.next()) != null; i++) {
            progress.checkCanceled();
            if (reportProgress(i)) {
                final long finalI = i;
                progress.setProgress(read.getProgress() / size, () -> String.format("Reading row %s", finalI));
            }
            output.push(toRow(next));
        }
    }

    private static boolean reportProgress(final long rowIndex) {
        return rowIndex % 973 == 0;
    }

}