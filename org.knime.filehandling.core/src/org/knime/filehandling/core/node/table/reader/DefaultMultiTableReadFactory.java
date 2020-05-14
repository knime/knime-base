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
 *   Mar 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMappingFactory;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.MultiTableReadFactory;

/**
 * Default implementation of {@code MultiTableReadFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type representing external data types
 * @param <V> the type representing values
 */
final class DefaultMultiTableReadFactory<T, V> implements MultiTableReadFactory<T, V> {

    private final TypeMappingFactory<T, V> m_typeMappingFactory;

    private final TypeHierarchy<T, T> m_typeHierarchy;

    private final RowKeyGeneratorContextFactory<V> m_rowKeyGeneratorFactory;

    /**
     * Constructor.
     *
     * @param typeMappingFactory creates a {@link TypeMapping} from {@link TypedReaderTableSpec}
     * @param typeHierarchy the {@link TypeHierarchy}
     * @param rowKeyGeneratorFactory the {@link RowKeyGeneratorContextFactory}
     */
    DefaultMultiTableReadFactory(final TypeMappingFactory<T, V> typeMappingFactory,
        final TypeHierarchy<T, T> typeHierarchy, final RowKeyGeneratorContextFactory<V> rowKeyGeneratorFactory) {
        m_typeMappingFactory = typeMappingFactory;
        m_typeHierarchy = typeHierarchy;
        m_rowKeyGeneratorFactory = rowKeyGeneratorFactory;
    }

    @Override
    public MultiTableRead<V> create(final String rootPath, final Map<Path, TypedReaderTableSpec<T>> individualSpecs,
        final MultiTableReadConfig<?> config) {
        final TypedReaderTableSpec<T> mergedSpec =
            config.getSpecMergeMode().mergeSpecs(individualSpecs.values(), m_typeHierarchy);
        final TypeMapping<V> typeMapping = m_typeMappingFactory.create(mergedSpec);
        final DataTableSpec outputSpec = typeMapping.map(mergedSpec);
        final RowKeyGeneratorContext<V> keyGenFn = m_rowKeyGeneratorFactory.createContext(config.getTableReadConfig());
        return new DefaultMultiTableRead<>(rootPath, individualSpecs, outputSpec, typeMapping, keyGenFn);
    }

    @Override
    public MultiTableRead<V> create(final String rootPath, final List<Path> paths,
        final MultiTableReadConfig<?> config) {
        final TableSpecConfig tableSpecConfig = config.getTableSpecConfig();
        final Map<Path, ReaderTableSpec<?>> individualSpecs = getIndividualSpecs(paths, tableSpecConfig);
        final TypeMapping<V> typeMapping = m_typeMappingFactory.create(config.getTableSpecConfig());
        final RowKeyGeneratorContext<V> keyGenFn = m_rowKeyGeneratorFactory.createContext(config.getTableReadConfig());
        return new DefaultMultiTableRead<>(rootPath, individualSpecs, tableSpecConfig.getDataTableSpec(), typeMapping,
            keyGenFn);
    }

    private static Map<Path, ReaderTableSpec<?>> getIndividualSpecs(final List<Path> paths,
        final TableSpecConfig tableSpecConfig) {
        return paths.stream()//
            .collect(Collectors.toMap(//
                Function.identity() //
                , p -> tableSpecConfig.getSpec(p.toString())//
                , (x, y) -> y // cannot happen
                , LinkedHashMap::new));
    }

}
