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

import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.transformToString;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.DefaultTypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.TableTransformationFactory;

/**
 * Default implementation of a {@link StagedMultiTableRead}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
final class DefaultStagedMultiTableRead<I, C extends ReaderSpecificConfig<C>, T, V>
    implements StagedMultiTableRead<I, T> {

    private final Map<I, TypedReaderTableSpec<T>> m_individualSpecs;

    private final RawSpec<T> m_rawSpec;

    private final GenericRowKeyGeneratorContextFactory<I, V> m_rowKeyGenFactory;

    private final Supplier<ReadAdapter<T, V>> m_readAdapterSupplier;

    private final MultiTableReadConfig<C, T> m_config;

    private final TableTransformationFactory<T> m_tableTransformationFactory;

    private final GenericTableReader<I, C, T, V> m_reader;

    /**
     * Constructor.
     *
     * @param reader {@link GenericTableReader}
     * @param individualSpecs individuals specs
     * @param rowKeyGenFactory {@link GenericRowKeyGeneratorContextFactory}
     * @param rawSpec the {@link RawSpec}
     * @param readAdapterSupplier {@link ReadAdapter} supplier
     * @param tableTransformationFactory for creating a default {@link TableTransformation} if necessary
     * @param config the {@link MultiTableReadConfig} for this read process
     */
    DefaultStagedMultiTableRead(final GenericTableReader<I, C, T, V> reader,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs,
        final GenericRowKeyGeneratorContextFactory<I, V> rowKeyGenFactory, final RawSpec<T> rawSpec,
        final Supplier<ReadAdapter<T, V>> readAdapterSupplier,
        final TableTransformationFactory<T> tableTransformationFactory, final MultiTableReadConfig<C, T> config) {
        m_rawSpec = rawSpec;
        m_individualSpecs = individualSpecs;
        m_rowKeyGenFactory = rowKeyGenFactory;
        m_config = config;
        m_tableTransformationFactory = tableTransformationFactory;
        m_reader = reader;
        m_readAdapterSupplier = readAdapterSupplier;
    }

    @Override
    public MultiTableRead<T> withoutTransformation(final SourceGroup<I> sourceGroup) {
        if (m_config.hasTableSpecConfig()) {
            final TableSpecConfig<T> tableSpecConfig = m_config.getTableSpecConfig();
            final TableTransformation<T> configuredTransformation = tableSpecConfig.getTransformationModel();
            if (tableSpecConfig.isConfiguredWith(m_config.getConfigID(),
                transformToString(sourceGroup))) {
                return createMultiTableRead(sourceGroup, configuredTransformation, m_config.getTableReadConfig(),
                    tableSpecConfig);
            } else {
                final TableTransformation<T> adaptedTransformation =
                    m_tableTransformationFactory.createFromExisting(m_rawSpec, configuredTransformation);
                return withTransformation(sourceGroup, adaptedTransformation);
            }
        } else {
            final TableTransformation<T> newTransformation =
                m_tableTransformationFactory.createNew(m_rawSpec, m_config);
            return withTransformation(sourceGroup, newTransformation);
        }
    }

    @Override
    public MultiTableRead<T> withTransformation(final SourceGroup<I> sourceGroup,
        final TableTransformation<T> transformationModel) {
        final TableReadConfig<C> tableReadConfig = m_config.getTableReadConfig();
        final ConfigID id = m_config.getConfigID();
        final TableSpecConfig<T> tableSpecConfig = DefaultTableSpecConfig
            .createFromTransformationModel(sourceGroup.getID(), id, m_individualSpecs, transformationModel);
        return createMultiTableRead(sourceGroup, transformationModel, tableReadConfig, tableSpecConfig);
    }

    private DefaultMultiTableRead<I, T, V> createMultiTableRead(final SourceGroup<I> sourceGroup,
        final TableTransformation<T> transformationModel, final TableReadConfig<C> tableReadConfig,
        final TableSpecConfig<T> tableSpecConfig) {
        return new DefaultMultiTableRead<>(sourceGroup, p -> createRead(p, tableReadConfig), () -> {
            IndividualTableReaderFactory<I, T, V> factory = createIndividualTableReaderFactory(transformationModel);
            return factory::create;
        }, tableReadConfig, tableSpecConfig, TableTransformationUtils.toDataTableSpec(transformationModel));
    }

    private IndividualTableReaderFactory<I, T, V>
        createIndividualTableReaderFactory(final TableTransformation<T> transformationModel) {
        final TableReadConfig<C> tableReadConfig = m_config.getTableReadConfig();
        return new IndividualTableReaderFactory<>(m_individualSpecs, tableReadConfig,
            transformationModel, this::createTypeMapper,
            m_rowKeyGenFactory.createContext(tableReadConfig));
    }

    private Read<I, V> createRead(final I path, final TableReadConfig<C> config) throws IOException {
        final Read<I, V> rawRead = m_reader.read(path, config);
        if (config.decorateRead()) {
            return ReadUtils.decorateForReading(rawRead, config);
        }
        return rawRead;
    }

    private TypeMapper<V> createTypeMapper(final ProductionPath[] prodPaths, final FileStoreFactory fsFactory) {
        return new DefaultTypeMapper<>(m_readAdapterSupplier.get(), prodPaths, fsFactory,
            m_config.getTableReadConfig().getReaderSpecificConfig());
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean isValidFor(final SourceGroup<I> sourceGroup) {
        return sourceGroup.size() == m_individualSpecs.size() && containsAll(m_individualSpecs.keySet(), sourceGroup);
    }

    private static <I> boolean containsAll(final Set<I> keys, final SourceGroup<I> sourceGroup) {
        return sourceGroup.stream().allMatch(keys::contains);
    }
}