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
 *   Feb 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import java.util.List;

import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.datacell.OriginAwareJavaToDataCellConverterRegistry;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Default implementation of a {@link ProductionPathSerializer} that uses {@link OriginAwareJavaToDataCellConverterRegistry}
 * for loading production paths and delegates to
 * {@link SerializeUtil#storeProductionPath(ProductionPath, ConfigBaseWO, String)} for storing them.
 *
 * The loading is customized because the {@link JavaToDataCellConverterRegistry} used by
 * {@link SerializeUtil#loadProductionPath(ConfigBaseRO, ProducerRegistry, String)} overwrites any existing converter if
 * their is an ID clash. Consequently, it is possible that a community extension can overwrite a converter provided by
 * KNIME. In order to remedy this, we use {@link OriginAwareJavaToDataCellConverterRegistry}, which holds a list of
 * converters for any ID. This list is ordered and favors converters provided by KNIME. Ideally the list would only hold
 * a single element but if it doesn't, then we print a warning into the log.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultProductionPathSerializer implements ProductionPathSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultProductionPathSerializer.class);

    private final ProducerRegistry<?, ?> m_registry;

    /**
     * Constructor.
     *
     * @param registry to use for {@link ProductionPath} serialization.
     */
    public DefaultProductionPathSerializer(final ProducerRegistry<?, ?> registry) {
        m_registry = registry;
    }

    @Override
    public ProductionPath loadProductionPath(final NodeSettingsRO config, final String key)
        throws InvalidSettingsException {
        // The JavaToDataCellConverterRegistry used by SerializeUtil overwrites any existing converter if
        // there is an ID clash. Therefore we introduced SecureJavaToDataCellConverterRegistry, that does not
        // overwrite but instead stores a list that is ordered by origin, so that converters provided by KNIME
        // are favored. This is by no means perfect but it prevents core converters being overwritten by community
        // extensions
        final JavaToDataCellConverterFactory<?> converter = loadConverterFactory(config, key + "_converter");
        final CellValueProducerFactory<?, ?, ?, ?> producer =
            SerializeUtil.loadConverterFactory(config, m_registry, key + "_producer")//
                .orElseThrow(() -> new InvalidSettingsException("Unknown CellValueProducerFactory."));
        return new ProductionPath(producer, converter);
    }

    private static JavaToDataCellConverterFactory<?> loadConverterFactory(final ConfigBaseRO config, final String key)
        throws InvalidSettingsException {
        final String id = config.getString(key);
        final List<JavaToDataCellConverterFactory<?>> factories =
            OriginAwareJavaToDataCellConverterRegistry.INSTANCE.getConverterFactoriesByIdentifier(id);
        CheckUtils.checkSetting(!factories.isEmpty(),
            "No JavaToDataCellConverterFactory with the id '%s' is known. Are you missing an extension?", id);
        if (factories.size() > 1) {
            LOGGER.warnWithFormat(
                "There are multiple JavaToDataCellConverterFactories with the id '%s' registered. Taking the first.",
                id);
        }
        return factories.get(0);
    }

    @Override
    public void saveProductionPath(final ProductionPath productionPath, final NodeSettingsWO settings,
        final String key) {
        // storing is independent of the JavaToDataCellConverterRegistry and we can therefore use
        // the default
        SerializeUtil.storeProductionPath(productionPath, settings, key);
    }

}