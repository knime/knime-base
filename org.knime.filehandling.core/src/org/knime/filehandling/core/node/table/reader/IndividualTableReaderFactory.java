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
 *   Nov 14, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleDecorator;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.IndexMapperFactory;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Creates {@link IndividualTableReader IndividualTableReaders} for particular items.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <T> the type representing external types
 * @param <V> the type representing values
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public final class IndividualTableReaderFactory<I, T, V> {

    private final IndexMapperFactory m_indexMapperFactory;

    private final Map<I, TypedReaderTableSpec<T>> m_specs;

    private final GenericRowKeyGeneratorContext<I, V> m_rowKeyGenContext;

    private final ProductionPath[] m_prodPaths;

    private final EmptyCheckingRandomAccessibleDecoratorFactory<V> m_emptyCheckerFactory;

    private final BiFunction<ProductionPath[], FileStoreFactory, TypeMapper<V>> m_typeMapperFactory;

    private final Function<I, DataCell> m_itemIdentifierCellFactory;

    /**
     * Constructor.
     *
     * @param specs the individual input specs
     * @param config {@link TableReadConfig}
     * @param tableTransformation the {@link TableTransformation}
     * @param typeMapperFactory type mapper factory
     * @param rowKeyGenContext GenericRowKeyGeneratorContextFactory
     * @param itemIdentifierCellFactory creates cells to identify individual items
     */
    public IndividualTableReaderFactory(final Map<I, TypedReaderTableSpec<T>> specs, final TableReadConfig<?> config,
        final TableTransformation<T> tableTransformation,
        final BiFunction<ProductionPath[], FileStoreFactory, TypeMapper<V>> typeMapperFactory,
        final GenericRowKeyGeneratorContext<I, V> rowKeyGenContext,
        final Function<I, DataCell> itemIdentifierCellFactory) {
        m_specs = specs;
        final List<ColumnTransformation<T>> outputTransformations =
            TableTransformationUtils.getCandidates(tableTransformation)//
                .filter(ColumnTransformation::keep)// remove columns filtered by user
                .sorted()//
                .collect(toList());
        // must contain columns that are suspected to be empty, so we can check if they really are during execution
        m_indexMapperFactory = new IndexMapperFactory(outputTransformations.stream()//
            .map(ColumnTransformation::getExternalSpec)//
            .map(MultiTableUtils::getNameAfterInit)//
            .collect(toList()), config);
        final boolean skipEmptyColumns = tableTransformation.skipEmptyColumns();
        if (skipEmptyColumns && outputTransformations.stream().anyMatch(IndividualTableReaderFactory::isColumnEmpty)) {
            // we only need to do empty checking if there are columns suspected to be empty
            m_emptyCheckerFactory = new EmptyCheckingRandomAccessibleDecoratorFactory<>(outputTransformations);
        } else {
            m_emptyCheckerFactory = null;
        }
        m_rowKeyGenContext = rowKeyGenContext;
        m_typeMapperFactory = typeMapperFactory;
        m_prodPaths = getRelevantProductionPaths(outputTransformations, skipEmptyColumns);
        m_itemIdentifierCellFactory = itemIdentifierCellFactory;
    }

    private ProductionPath[] getRelevantProductionPaths(final List<ColumnTransformation<T>> outputTransformations,
        final boolean skipEmptyColumns) {
        Stream<ColumnTransformation<T>> prodPathTransformations = outputTransformations.stream();
        if (skipEmptyColumns) {
            prodPathTransformations = prodPathTransformations.filter(c -> !isColumnEmpty(c));
        }
        return prodPathTransformations//
            .map(ColumnTransformation::getProductionPath)//
            .toArray(ProductionPath[]::new);
    }

    private static boolean isColumnEmpty(final ColumnTransformation<?> columnTransformation) {
        return !columnTransformation.getExternalSpec().hasType();
    }

    /**
     * @param item item to read from
     * @param fsFactory {@link FileStoreFactory}
     * @return {@link IndividualTableReader}
     */
    public DefaultIndividualTableReader<V> create(final I item, final FileStoreFactory fsFactory) {
        final RandomAccessibleDecorator<V> idxMapper = createIndexMapper(m_specs.get(item));
        final RowKeyGenerator<V> rowKeyGen = m_rowKeyGenContext.createKeyGenerator(item);
        final TypeMapper<V> typeMapper = m_typeMapperFactory.apply(m_prodPaths, fsFactory);
        final DataCell identifierCell = m_itemIdentifierCellFactory.apply(item);
        return new DefaultIndividualTableReader<>(typeMapper, idxMapper, rowKeyGen, identifierCell);
    }

    private RandomAccessibleDecorator<V> createIndexMapper(final TypedReaderTableSpec<T> spec) {
        final IndexMapper idxMapper = m_indexMapperFactory.createIndexMapper(spec);
        final IndexMappingRandomAccessibleDecorator<V> idxMappingDecorator =
            new IndexMappingRandomAccessibleDecorator<>(idxMapper);
        if (m_emptyCheckerFactory == null) {
            return idxMappingDecorator;
        }
        final EmptyCheckingRandomAccessibleDecorator<V> emptyCheckingDecorator = m_emptyCheckerFactory.create();
        return idxMappingDecorator.andThen(emptyCheckingDecorator);
    }

    private static class EmptyCheckingRandomAccessibleDecoratorFactory<V> {

        private final int[] m_emptyColumnPositions;

        private final String[] m_emptyColumnNames;

        <T> EmptyCheckingRandomAccessibleDecoratorFactory(final Iterable<ColumnTransformation<T>> transformations) {
            int i = 0;
            final List<String> emptyColumnNames = new ArrayList<>();
            final List<Integer> emptyColumnPositions = new ArrayList<>();
            for (ColumnTransformation<?> t : transformations) {
                if (isColumnEmpty(t)) {
                    emptyColumnNames.add(t.getOriginalName());
                    emptyColumnPositions.add(i);
                }
                i++;
            }
            m_emptyColumnNames = emptyColumnNames.toArray(new String[0]);
            m_emptyColumnPositions = emptyColumnPositions.stream()//
                .mapToInt(Integer::intValue)//
                .toArray();
        }

        EmptyCheckingRandomAccessibleDecorator<V> create() {
            return new EmptyCheckingRandomAccessibleDecorator<>(m_emptyColumnPositions, m_emptyColumnNames);
        }
    }

}