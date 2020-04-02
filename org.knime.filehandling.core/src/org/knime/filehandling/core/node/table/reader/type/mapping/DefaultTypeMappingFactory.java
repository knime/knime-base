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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;

/**
 * Default implementation of a {@link TypeMappingFactory} based on KNIME's type mapping framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type identifying external data types
 * @param <V> the type of values
 */
public final class DefaultTypeMappingFactory<T, V> implements TypeMappingFactory<T, V> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTypeMappingFactory.class);

    private ReadAdapterFactory<T, V> m_readAdapterFactory;

    private ProducerRegistry<T, ? extends ReadAdapter<T, V>> m_producerRegistry;

    private Map<T, DataType> m_defaultKnimeTypes;

    /**
     * Constructor.
     *
     * @param readAdapterFactory provides reader specific information for type mapping
     */
    public DefaultTypeMappingFactory(final ReadAdapterFactory<T, V> readAdapterFactory) {
        m_readAdapterFactory = readAdapterFactory;
        m_producerRegistry = m_readAdapterFactory.getProducerRegistry();
        m_defaultKnimeTypes = m_readAdapterFactory.getDefaultTypeMap();
    }

    @Override
    public TypeMapping<V> create(final ReaderTableSpec<T> mergedSpec) {
        final ProductionPath[] paths = mergedSpec.stream()//
            .map(ReaderColumnSpec::getType)//
            .map(this::getDefaultPath)//
            .toArray(ProductionPath[]::new);
        return new DefaultTypeMapping<>(m_readAdapterFactory::createReadAdapter, paths);
    }

    private ProductionPath getDefaultPath(final T externalType) {
        final DataType knimeType = m_defaultKnimeTypes.get(externalType);
        CheckUtils.checkState(knimeType != null, "No default KNIME type defined for external type '%s'.", externalType);
        return getPath(externalType, knimeType);
    }

    private ProductionPath getPath(final T externalType, final DataType knimeType) {
        final List<ProductionPath> paths = m_producerRegistry.getAvailableProductionPaths(externalType).stream()
            .filter(p -> p.getConverterFactory().getDestinationType().equals(knimeType)).collect(toList());
        CheckUtils.checkState(!paths.isEmpty(), "No mapping registered from external type '%s' to KNIME type '%s'.",
            externalType, knimeType);
        if (paths.size() > 1) {
            LOGGER.debugWithFormat(
                "Multiple mappings from external type '%s' to KNIME type '%s' found (%s). Taking the first.",
                externalType, knimeType, paths);
        }
        return paths.get(0);
    }
}
