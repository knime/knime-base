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
 *   Apr 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.OriginAwareJavaToDataCellConverterRegistry;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.Source;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TraversableTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * This {@link ProductionPathProvider} uses the {@link TypeHierarchy} of the current node to find the available
 * {@link ProductionPath ProductionPaths} for any external type.<br>
 * The algorithm works as follows:
 * <ol>
 * <li>Start at the external type and add its {@link #getDefaultProductionPath(Object) default path} to the available
 * paths
 * <li>Climb up the hierarchy and add the default paths of the super-types
 * <li>Add any path of the current type that ends in a DataType that is not yet covered by any other path and isn't
 * blacklisted by a reader-dependent {@link BiPredicate pathBouncer}
 * </ol>
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external data types
 */
public final class HierarchyAwareProdutionPathProvider<T> implements ProductionPathProvider<T> {

    private final ProducerRegistry<T, ?> m_producerRegistry;

    private final TraversableTypeHierarchy<T> m_typeHierarchy;

    private final Function<T, DataType> m_defaultTypeProvider;

    private final BiPredicate<T, ProductionPath> m_pathTester;

    /**
     * Constructor.
     *
     * @param producerRegistry provides the {@link CellValueProducerFactory CellValueProducerFactories}
     * @param typeHierarchy of the reader node
     * @param defaultTypeProvider provides the default {@link DataType} for a particular external type
     * @param pathBouncer {@link BiPredicate} that allows to blacklist certain (non-default) {@link ProductionPath
     *            ProductionPaths}
     */
    public HierarchyAwareProdutionPathProvider(final ProducerRegistry<T, ?> producerRegistry,
        final TraversableTypeHierarchy<T> typeHierarchy, final Function<T, DataType> defaultTypeProvider,
        final BiPredicate<T, ProductionPath> pathBouncer) {
        m_producerRegistry = producerRegistry;
        m_typeHierarchy = typeHierarchy;
        m_defaultTypeProvider = defaultTypeProvider;
        m_pathTester = pathBouncer;
    }

    @Override
    public ProductionPath getDefaultProductionPath(final T externalType) {
        final DataType knimeType = m_defaultTypeProvider.apply(externalType);

        return getProducerFactories(externalType, m_producerRegistry)//
                .flatMap(p -> getConverterFactories(p.getDestinationType())//
                    .filter(f -> f.getDestinationType().equals(knimeType)) //
                    .map(f -> new ProductionPath(p, f)))//
                .findFirst()//
                .orElseThrow();
    }

    private <S extends Source<T>> Stream<CellValueProducerFactory<S, T, ?, ?>>
        getProducerFactories(final T externalType, final ProducerRegistry<T, S> registry) {
        Collection<CellValueProducerFactory<S, T, ?, ?>> producerFactories =
            registry.getFactoriesForSourceType(externalType);
        return producerFactories.stream();
    }

    private static Stream<JavaToDataCellConverterFactory<?>> getConverterFactories(final Class<?> javaClass) {
        final List<JavaToDataCellConverterFactory<?>> factories =
            OriginAwareJavaToDataCellConverterRegistry.INSTANCE.getConverterFactoriesBySourceType(javaClass);
        return factories.stream().sequential();
    }

    @Override
    public List<ProductionPath> getAvailableProductionPaths(final T externalType) {
        final Map<DataType, ProductionPath> coveredTypes = new HashMap<>();
        m_typeHierarchy.traverseToRoot(externalType, t -> {
            final ProductionPath defaultPath = getDefaultProductionPath(t);
            final DataType knimeType = defaultPath.getDestinationType();
            CheckUtils.checkArgument(!coveredTypes.containsKey(knimeType),
                "Coding error: Two default paths point to the DataType '%s'.", knimeType);
            coveredTypes.put(knimeType, defaultPath);
        });
        addUncoveredPaths(coveredTypes, externalType);
        final Comparator<ProductionPath> comparator =
            Comparator.<ProductionPath, String> comparing(p -> p.getDestinationType().getName());
        return coveredTypes.values().stream()//
            .sorted(comparator)//
            .collect(toList());
    }

    private void addUncoveredPaths(final Map<DataType, ProductionPath> coveredTypes, final T externalType) {
        final List<ProductionPath> prodPaths = getPathsFor(externalType);
        for (ProductionPath path : prodPaths) {
            final DataType knimeType = path.getDestinationType();
            if (!coveredTypes.containsKey(knimeType)) {
                coveredTypes.put(knimeType, path);
            }
        }
    }

    private List<ProductionPath> getPathsFor(final T externalType) {
        final Predicate<ProductionPath> pathBouncer = getPathTesterForCurrentType(externalType);
        return getProducerFactories(externalType, m_producerRegistry)//
            .flatMap(p -> getConverterFactories(p.getDestinationType())//
                .map(f -> new ProductionPath(p, f)))//
            .filter(pathBouncer)//
            .collect(toList());
    }

    private Predicate<ProductionPath> getPathTesterForCurrentType(final T externalType) {
        return p -> m_pathTester.test(externalType, p);
    }

}
