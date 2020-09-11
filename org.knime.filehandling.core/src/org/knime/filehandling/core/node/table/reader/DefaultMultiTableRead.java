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
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type representing values
 */
final class DefaultMultiTableRead<V> implements MultiTableRead {

    private final DataTableSpec m_outputSpec;

    private final TableSpecConfig m_tableSpecConfig;

    private final TypeMapping<V> m_typeMapping;

    private final RowKeyGeneratorContext<V> m_keyGenContext;

    private final Map<Path, IndexMapper> m_indexMappers;

    private final CheckedExceptionFunction<Path, Read<V>, IOException> m_readFn;

    /**
     * Constructor.
     *
     * @param readFn produces a {@link Read} from a {@link Path}
     * @param tableSpecConfig corresponding to this instance
     * @param typeMapping {@link TypeMapping} for this instance
     * @param keyGenContext {@link RowKeyGeneratorContext} for creating row keys
     * @param indexMappers {@link Map} containing for each path the corresponding {@link IndexMapper}
     */
    DefaultMultiTableRead(final CheckedExceptionFunction<Path, Read<V>, IOException> readFn,
        final TableSpecConfig tableSpecConfig, final TypeMapping<V> typeMapping,
        final RowKeyGeneratorContext<V> keyGenContext, final Map<Path, IndexMapper> indexMappers) {
        m_outputSpec = tableSpecConfig.getDataTableSpec();
        m_tableSpecConfig = tableSpecConfig;
        m_typeMapping = typeMapping;
        m_keyGenContext = keyGenContext;
        m_indexMappers = indexMappers;
        m_readFn = readFn;
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_outputSpec;
    }

    private IndividualTableReader<V> createIndividualTableReader(final Path path, final FileStoreFactory fsFactory) {
        final IndexMapper idxMapper = m_indexMappers.get(path);
        final TypeMapper<V> typeMapper = m_typeMapping.createTypeMapper(fsFactory);
        final RowKeyGenerator<V> keyGen = m_keyGenContext.createKeyGenerator(path);
        return new DefaultIndividualTableReader<>(typeMapper, idxMapper, keyGen);
    }

    @Override
    public TableSpecConfig getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec,
        final FileStoreFactory fsFactory) throws Exception {
        for (Entry<Path, IndexMapper> entry : m_indexMappers.entrySet()) {
            exec.checkCanceled();
            final ExecutionMonitor progress = exec.createSubProgress(1.0 / m_indexMappers.size());
            final Path path = entry.getKey();
            final TypeMapper<V> typeMapper = m_typeMapping.createTypeMapper(fsFactory);
            final RowKeyGenerator<V> keyGen = m_keyGenContext.createKeyGenerator(path);
            final IndividualTableReader<V> reader = new DefaultIndividualTableReader<>(typeMapper, entry.getValue(), keyGen);
            try (final Read<V> read = m_readFn.apply(path)) {
                reader.fillOutput(read, output, progress);
            }
            progress.setProgress(1.0);
        }
        output.close();
    }

    @Override
    public PreviewRowIterator createPreviewIterator() {
        return new MultiTablePreviewRowIterator(m_indexMappers.keySet().iterator(), this::createIndividualTableIterator);
    }

    @SuppressWarnings("resource") // the read is closed by the IndividualTablePreviewRowIterator
    private PreviewRowIterator createIndividualTableIterator(final Path path, final FileStoreFactory fsFactory)
        throws IOException {
        final IndividualTableReader<V> reader = createIndividualTableReader(path, fsFactory);
        return new IndividualTablePreviewRowIterator<V>(m_readFn.apply(path), reader::toRow);
    }

}
