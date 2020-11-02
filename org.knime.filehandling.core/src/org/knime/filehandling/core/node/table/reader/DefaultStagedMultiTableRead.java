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
 *   Aug 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.DefaultTypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Default implementation of a {@link StagedMultiTableRead}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external types
 * @param <V> the type representing values
 */
final class DefaultStagedMultiTableRead<C extends ReaderSpecificConfig<C>, T, V> implements StagedMultiTableRead<T> {

    private final Map<Path, TypedReaderTableSpec<T>> m_individualSpecs;

    private final String m_rootPath;

    private final RawSpec<T> m_rawSpec;

    private final RowKeyGeneratorContextFactory<V> m_rowKeyGenFactory;

    private final Supplier<ReadAdapter<T, V>> m_readAdapterSupplier;

    private final TableReadConfig<C> m_tableReadConfig;

    private final TableTransformation<T> m_defaultTransformation;

    private final TableReader<C, T, V> m_reader;

    DefaultStagedMultiTableRead(final TableReader<C, T, V> reader, final String rootPath,
        final Map<Path, TypedReaderTableSpec<T>> individualSpecs,
        final RowKeyGeneratorContextFactory<V> rowKeyGenFactory, final Supplier<ReadAdapter<T, V>> readAdapterSupplier,
        final TableTransformation<T> defaultTransformation, final TableReadConfig<C> tableReadConfig) {
        m_rawSpec = defaultTransformation.getRawSpec();
        m_rootPath = rootPath;
        m_individualSpecs = individualSpecs;
        m_rowKeyGenFactory = rowKeyGenFactory;
        m_tableReadConfig = tableReadConfig;
        m_defaultTransformation = defaultTransformation;
        m_reader = reader;
        m_readAdapterSupplier = readAdapterSupplier;
    }

    @Override
    public MultiTableRead withoutTransformation() {
        return withTransformation(m_defaultTransformation);
    }

    @Override
    public MultiTableRead withTransformation(final TableTransformation<T> transformationModel) {
        final TableSpecConfig tableSpecConfig =
            DefaultTableSpecConfig.createFromTransformationModel(m_rootPath, m_individualSpecs, transformationModel);
        return new DefaultMultiTableRead<>(new ArrayList<>(m_individualSpecs.keySet()),
            p -> createRead(p, m_tableReadConfig), () -> {
                IndividualTableReaderFactory<T, V> factory = createIndividualTableReaderFactory(transformationModel);
                return factory::create;
            }, tableSpecConfig, TableTransformationUtils.toDataTableSpec(transformationModel));
    }

    private IndividualTableReaderFactory<T, V>
        createIndividualTableReaderFactory(final TableTransformation<T> transformationModel) {
        return new IndividualTableReaderFactory<>(m_individualSpecs, m_tableReadConfig,
            TableTransformationUtils.getOutputTransformations(transformationModel), this::createTypeMapper,
            m_rowKeyGenFactory.createContext(m_tableReadConfig));
    }

    private TypeMapper<V> createTypeMapper(final ProductionPath[] prodPaths, final FileStoreFactory fsFactory) {
        return new DefaultTypeMapper<>(m_readAdapterSupplier.get(), prodPaths, fsFactory,
            m_tableReadConfig.getReaderSpecificConfig());
    }

    // The rawRead will be closed by the decoratedRead which in turn needs to be closed by the caller
    @SuppressWarnings("resource")
    private Read<V> createRead(final Path path, final TableReadConfig<C> config) throws IOException {
        final Read<V> rawRead = m_reader.read(path, config);
        return ReadUtils.decorateForReading(rawRead, config);
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean isValidFor(final Collection<Path> paths) {
        return paths.size() == m_individualSpecs.size() && m_individualSpecs.keySet().containsAll(paths);
    }

}
