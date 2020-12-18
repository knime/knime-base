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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.type.mapping.MappingRuntimeException;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapperException;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <T> the type used to identify external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
public final class DefaultMultiTableRead<I, T, V> implements MultiTableRead<T> {

    private final DataTableSpec m_outputSpec;

    private final TableSpecConfig<T> m_tableSpecConfig;

    private final TableReadConfig<?> m_tableReadConfig;

    private final SourceGroup<I> m_sourceGroup;

    private final CheckedExceptionFunction<I, ? extends Read<I, V>, IOException> m_readFn;

    private final Supplier<BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<I, V>>> m_individualTableReaderFactorySupplier;

    /**
     * Constructor.
     *
     * @param sourceGroup the {@link SourceGroup}
     * @param readFn produces a {@link Read} from a item
     * @param individualTableReaderFactorySupplier creates {@link IndividualTableReader IndividualTableReaders} from
     *            item
     * @param tableReadConfig the {@link TableReadConfig}
     * @param tableSpecConfig corresponding to this instance
     * @param outputSpec {@link DataTableSpec} of the output table
     */
    public DefaultMultiTableRead(final SourceGroup<I> sourceGroup,
        final CheckedExceptionFunction<I, ? extends Read<I, V>, IOException> readFn,
        final Supplier<BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<I, V>>> individualTableReaderFactorySupplier,
        final TableReadConfig<?> tableReadConfig, final TableSpecConfig<T> tableSpecConfig,
        final DataTableSpec outputSpec) {
        m_outputSpec = outputSpec;
        m_tableSpecConfig = tableSpecConfig;
        m_tableReadConfig = tableReadConfig;
        m_readFn = readFn;
        m_sourceGroup = sourceGroup;
        m_individualTableReaderFactorySupplier = individualTableReaderFactorySupplier;
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_outputSpec;
    }

    @Override
    public TableSpecConfig<T> getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public void fillRowOutput(final RowOutput output, final ExecutionMonitor exec, final FileStoreFactory fsFactory)
        throws Exception {
        final BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<I, V>> individualTableReaderFactory =
            m_individualTableReaderFactorySupplier.get();
        for (I item : m_sourceGroup) {
            exec.checkCanceled();
            final ExecutionMonitor progress = exec.createSubProgress(1.0 / m_sourceGroup.size());
            final IndividualTableReader<I, V> reader = individualTableReaderFactory.apply(item, fsFactory);
            try (final Read<I, V> read = m_readFn.apply(item)) {
                reader.fillOutput(read, output, progress);
            } catch (TypeMapperException e) {
                processAndThrowTypeMapperException(item, e);
            }
            progress.setProgress(1.0);
        }
        output.close();
    }

    @SuppressWarnings("resource")
    @Override
    public final PreviewRowIterator createPreviewIterator() {
        final BiFunction<I, FileStoreFactory, ? extends IndividualTableReader<I, V>> individualTableReaderFactory =
            m_individualTableReaderFactorySupplier.get();
        return new MultiTablePreviewRowIterator<>(m_sourceGroup.iterator(), (p, f) -> {
            final IndividualTableReader<I, V> reader = individualTableReaderFactory.apply(p, f);
            return new IndividualTablePreviewRowIterator<>(m_readFn.apply(p), reader::toRow);
        });
    }

    private void processAndThrowTypeMapperException(final I item, final TypeMapperException e) {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("Row with ID '%s' ", e.getRowKey()));
        if (m_sourceGroup.size() > 1) {
            builder.append(String.format("in file '%s' ", item.toString()));
        }
        builder.append("can't be converted to the configured data types.");
        if (m_tableReadConfig.limitRowsForSpec()) {
            builder
            .append(" Increasing the number of scanned rows or changing the target types might resolve the issue.");
        } else {
            builder.append(" Changing the target types might resolve the issue.");
        }
        builder.append(String.format(" Content of row: %s", e.getRandomAccessible()));
        throw new MappingRuntimeException(builder.toString(), e);
    }
}