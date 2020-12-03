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

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

import com.google.common.collect.Sets;

/**
 * Default implementation of {@code MultiTableReadFactory}.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the ReaderSpecificConfig type
 * @param <T> the type representing external data types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 */
public final class DefaultMultiTableReadFactory<I, C extends ReaderSpecificConfig<C>, T, V>
    implements MultiTableReadFactory<I, C, T> {

    private final Supplier<ReadAdapter<T, V>> m_readAdapterSupplier;

    private final RawSpecFactory<T> m_rawSpecFactory;

    private final GenericRowKeyGeneratorContextFactory<I, V> m_rowKeyGeneratorFactory;

    private final GenericTableReader<I, C, T, V> m_reader;

    private final TableTransformationFactory<T> m_transformationModelCreator;

    /**
     * Constructor.
     *
     * @param typeHierarchy the {@link TypeHierarchy}
     * @param rowKeyGeneratorFactory the {@link GenericRowKeyGeneratorContextFactory}
     * @param reader for the particular reader node
     * @param productionPathProvider provides {@link ProductionPath ProductionPaths} for external data types
     * @param readAdpaterSupplier creates new {@link ReadAdapter} instances
     */
    public DefaultMultiTableReadFactory(final TypeHierarchy<T, T> typeHierarchy,
        final GenericRowKeyGeneratorContextFactory<I, V> rowKeyGeneratorFactory,
        final GenericTableReader<I, C, T, V> reader, final ProductionPathProvider<T> productionPathProvider,
        final Supplier<ReadAdapter<T, V>> readAdpaterSupplier) {
        m_rawSpecFactory = new RawSpecFactory<>(typeHierarchy);
        m_rowKeyGeneratorFactory = rowKeyGeneratorFactory;
        m_reader = reader;
        m_transformationModelCreator = new TableTransformationFactory<>(productionPathProvider);
        m_readAdapterSupplier = readAdpaterSupplier;
    }

    @Override
    public StagedMultiTableRead<I, T> create(final String rootItem, final List<I> items,
        final MultiTableReadConfig<C> config, final ExecutionMonitor exec) throws IOException {
        final Map<I, TypedReaderTableSpec<T>> specs = readIndividualSpecs(items, config, exec);
        return create(rootItem, specs, config);
    }

    private Map<I, TypedReaderTableSpec<T>> readIndividualSpecs(final List<I> items,
        final MultiTableReadConfig<C> config, final ExecutionMonitor exec) throws IOException {
        final Map<I, TypedReaderTableSpec<T>> specs = new LinkedHashMap<>(items.size());
        for (I item : items) {
            final TypedReaderTableSpec<T> spec =
                m_reader.readSpec(item, config.getTableReadConfig(), exec.createSubProgress(1.0 / items.size()));
            specs.put(item, MultiTableUtils.assignNamesIfMissing(spec));
        }
        return specs;
    }

    private StagedMultiTableRead<I, T> create(final String rootItem,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs, final MultiTableReadConfig<C> config) {
        final RawSpec<T> rawSpec = m_rawSpecFactory.create(individualSpecs.values());

        if (config.failOnDifferingSpecs()) {
            verifySpecEquality(rawSpec);
        }

        final TableTransformation<T> defaultTransformation = m_transformationModelCreator.create(rawSpec, config);
        return createStagedMultiTableReader(rootItem, individualSpecs, config, defaultTransformation);
    }

    private DefaultStagedMultiTableRead<I, C, T, V> createStagedMultiTableReader(final String rootPath,
        final Map<I, TypedReaderTableSpec<T>> individualSpecs, final MultiTableReadConfig<C> config,
        final TableTransformation<T> defaultTransformation) {
        return new DefaultStagedMultiTableRead<>(m_reader, rootPath, individualSpecs, m_rowKeyGeneratorFactory,
                m_readAdapterSupplier, defaultTransformation, config.getTableReadConfig());
    }

    private void verifySpecEquality(final RawSpec<T> rawSpec) {
        final Set<String> unionNames = MultiTableUtils.extractNamesAfterInit(rawSpec.getUnion());
        final Set<String> intersectionNames = MultiTableUtils.extractNamesAfterInit(rawSpec.getIntersection());
        final Set<String> difference = Sets.difference(unionNames, intersectionNames);
        CheckUtils.checkArgument(difference.isEmpty(),
            "The following columns are not contained in all source files: %s", difference);
    }

    @Override
    public StagedMultiTableRead<I, T> createFromConfig(final String rootItem, final List<I> items,
        final MultiTableReadConfig<C> config) {
        final TableSpecConfig tableSpecConfig = config.getTableSpecConfig();
        final Map<I, TypedReaderTableSpec<T>> individualSpecs = getIndividualSpecs(items, tableSpecConfig);
        final TableTransformation<T> configuredTransformationModel = tableSpecConfig.getTransformationModel();
        return createStagedMultiTableReader(rootItem, individualSpecs, config, configuredTransformationModel);
    }

    private Map<I, TypedReaderTableSpec<T>> getIndividualSpecs(final List<I> items,
        final TableSpecConfig tableSpecConfig) {

        final TableTransformation<T> transformationModel = tableSpecConfig.getTransformationModel();
        final Map<String, T> typeMap = extractNameToTypeMap(transformationModel);

        return items.stream()//
            .collect(Collectors.toMap(//
                Function.identity() //
                , p -> resolveType(tableSpecConfig.getSpec(p.toString()), typeMap)//
                , (x, y) -> y // cannot happen
                , LinkedHashMap::new));
    }

    private Map<String, T> extractNameToTypeMap(final TableTransformation<T> transformationModel) {
        return transformationModel.getRawSpec().getUnion().stream()//
            .collect(toMap(MultiTableUtils::getNameAfterInit, TypedReaderColumnSpec::getType));
    }

    private TypedReaderTableSpec<T> resolveType(final ReaderTableSpec<?> tableSpec, final Map<String, T> typeMap) {
        List<TypedReaderColumnSpec<T>> cols = new ArrayList<>(tableSpec.size());
        for (ReaderColumnSpec column : tableSpec) {
            final String name = MultiTableUtils.getNameAfterInit(column);
            cols.add(TypedReaderColumnSpec.createWithName(name, typeMap.get(name), true));
        }
        return new TypedReaderTableSpec<>(cols);
    }

}