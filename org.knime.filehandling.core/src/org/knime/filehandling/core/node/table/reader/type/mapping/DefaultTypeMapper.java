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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.type.mapping;

import java.util.stream.IntStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.convert.map.DataRowProducer;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * Handles mapping from {@link RandomAccessible RandomAccessibles} to {@link DataRow DataRows}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of values mapped to cells
 */
final class DefaultTypeMapper<V> implements TypeMapper<V> {

    private final ReadAdapter<?, V> m_readAdapter;

    private final ReadAdapterParams<ReadAdapter<?, V>>[] m_params;

    private final DataRowProducer<ReadAdapterParams<ReadAdapter<?, V>>> m_rowProducer;

    DefaultTypeMapper(final ReadAdapter<?, V> readAdapter, final ProductionPath[] productionPaths,
        final FileStoreFactory fsFactory) {
        m_readAdapter = readAdapter;
        m_rowProducer = MappingFramework.createDataRowProducer(fsFactory, m_readAdapter, productionPaths);
        // ReadAdapterParams are compatible with any ReadAdapter, the generics
        // are only necessary to shut up the compiler
        @SuppressWarnings("unchecked")
        final ReadAdapterParams<ReadAdapter<?, V>>[] params = IntStream.range(0, productionPaths.length)
            .mapToObj(ReadAdapterParams::new).toArray(ReadAdapterParams[]::new);
        m_params = params;
    }

    @Override
    public DataRow map(final RowKey key, final RandomAccessible<V> randomAccessible) throws Exception {
        CheckUtils.checkArgumentNotNull(key != null, "The row key must not be null.");
        CheckUtils.checkArgumentNotNull(randomAccessible, "The randomAccessible must not be null.");
        final int size = randomAccessible.size();
        CheckUtils.checkArgument(size == m_params.length,
            "The size of the randomAccessible is wrong. Expected %s but got %s.", size, m_params.length);
        m_readAdapter.setSource(randomAccessible);
        try {
            return m_rowProducer.produceDataRow(key, m_params);
        } catch (Exception ex) {
            throw new RuntimeException(String.format(
                "The following row could not be converted to the specified KNIME types: %s", randomAccessible), ex);
        }
    }

}
