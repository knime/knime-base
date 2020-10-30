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
 *   Oct 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.selector.Transformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.IndexMapperFactory;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Creates {@link IndividualTableReader IndividualTableReaders} for particular {@link Path Paths}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class IndividualTableReaderFactory<T, V> {

    private final IndexMapperFactory m_indexMapperFactory;

    private final Map<Path, TypedReaderTableSpec<T>> m_specs;

    private final RowKeyGeneratorContext<V> m_rowKeyGenContext;

    private final ProductionPath[] m_prodPaths;

    private final BiFunction<ProductionPath[], FileStoreFactory, TypeMapper<V>> m_typeMapperFactory;

    IndividualTableReaderFactory(final Map<Path, TypedReaderTableSpec<T>> specs, final TableReadConfig<?> config,
        final List<Transformation<T>> outputTransformations,
        final BiFunction<ProductionPath[], FileStoreFactory, TypeMapper<V>> typeMapperFactory,
        final RowKeyGeneratorContext<V> rowKeyGenContext) {
        m_specs = specs;
        m_indexMapperFactory = new IndexMapperFactory(outputTransformations.stream()//
            .map(Transformation::getExternalSpec)//
            .map(MultiTableUtils::getNameAfterInit)//
            .collect(toList()), config);
        m_rowKeyGenContext = rowKeyGenContext;
        m_typeMapperFactory = typeMapperFactory;
        m_prodPaths = outputTransformations.stream()//
            .map(Transformation::getProductionPath)//
            .toArray(ProductionPath[]::new);
    }

    IndividualTableReader<V> create(final Path path, final FileStoreFactory fsFactory) {
        final IndexMapper idxMapper = m_indexMapperFactory.createIndexMapper(m_specs.get(path));
        final RowKeyGenerator<V> rowKeyGen = m_rowKeyGenContext.createKeyGenerator(path);
        final TypeMapper<V> typeMapper = m_typeMapperFactory.apply(m_prodPaths, fsFactory);
        return new DefaultIndividualTableReader<>(typeMapper, idxMapper, rowKeyGen);
    }
}
