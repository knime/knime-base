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
package org.knime.filehandling.core.node.table.reader.type.mapping;

import java.util.function.Function;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Default implementation of a {@link TypeMappingFactory} based on KNIME's type mapping framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type identifying external data types
 * @param <V> the type of values
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTypeMappingFactory<C extends ReaderSpecificConfig<C>, T, V>
    implements TypeMappingFactory<C, T, V> {

    private final ReadAdapterFactory<T, V> m_readAdapterFactory;

    private final Function<T, ProductionPath> m_defaultProductionPathFn;

    /**
     * Constructor.
     *
     * @param readAdapterFactory provides reader specific information for type mapping
     * @param defaultProductionPathFn provides default production paths
     */
    public DefaultTypeMappingFactory(final ReadAdapterFactory<T, V> readAdapterFactory, final Function<T, ProductionPath> defaultProductionPathFn) {
        m_readAdapterFactory = readAdapterFactory;
        m_defaultProductionPathFn = defaultProductionPathFn;
    }

    @Override
    public TypeMapping<V> create(final TypedReaderTableSpec<T> mergedSpec, final C config) {
        final ProductionPath[] paths = mergedSpec.stream()//
            .map(TypedReaderColumnSpec::getType)//
            .map(m_defaultProductionPathFn)//
            .toArray(ProductionPath[]::new);
        return create(paths, config);
    }

    @Override
    public TypeMapping<V> create(final TableSpecConfig config, final C readerSpecificConfig) {
        return create(config.getProductionPaths(), readerSpecificConfig);
    }

    private TypeMapping<V> create(final ProductionPath[] paths, final C config) {
        return new DefaultTypeMapping<>(m_readAdapterFactory::createReadAdapter, paths, config);
    }

    @Override
    public TypeMapping<V> create(final TypedReaderTableSpec<T> spec, final C readerSpecificConfig,
        final TransformationModel<T> transformation) {
        final ProductionPath[] paths = spec.stream()//
            .map(transformation::getProductionPath)//
            .toArray(ProductionPath[]::new);
        return create(paths, readerSpecificConfig);
    }
}
