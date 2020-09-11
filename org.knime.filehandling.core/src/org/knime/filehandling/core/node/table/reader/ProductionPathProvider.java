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
 *   Aug 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;

/**
 * Creates default {@link ProductionPath ProductionPaths} for external types.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ProductionPathProvider<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ProductionPathProvider.class);

    private final ProducerRegistry<T, ?> m_producerRegistry;

    private final Map<T, DataType> m_defaultKnimeTypes;

    /**
     * Constructor.
     */
    ProductionPathProvider(final ProducerRegistry<T, ?> producerRegistry, final Map<T, DataType> defaultKnimeTypes) {
        m_producerRegistry = producerRegistry;
        m_defaultKnimeTypes = defaultKnimeTypes;
    }

    ProductionPath getDefaultProductionPath(final T externalType) {
        final DataType knimeType = m_defaultKnimeTypes.get(externalType);

        if (knimeType == null) {
            final ProductionPath productionPath = getFirstPath(externalType);
            if (productionPath != null) {
                return productionPath;
            }
        }

        CheckUtils.checkState(knimeType != null, "No default KNIME type defined for external type '%s'.", externalType);
        return getPath(externalType, knimeType);
    }

    private ProductionPath getFirstPath(final T externalType) {
        return m_producerRegistry.getAvailableProductionPaths(externalType).stream().findFirst().orElse(null);
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
