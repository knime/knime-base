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

import java.util.LinkedHashMap;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Serializer for {@link TableSpecConfig TableSpecConfigs} in KNIME Analytics Platform 4.4.0+.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class V44TableSpecConfigSerializer<T> implements TableSpecConfigSerializer<T> {

    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs";

    private static final String CFG_TABLE_TRANSFORMATION = "table_transformation";

    private static final String CFG_SOURCE_GROUP_ID = "source_group_id";

    private final TypedReaderTableSpecSerializer<T> m_tableSpecSerializer;

    private final TableTransformationSerializer<T> m_tableTransformationSerializer;

    private final ConfigIDSerializer m_configIDSerializer;

    V44TableSpecConfigSerializer(final ProductionPathSerializer productionPathSerializer,
        final ConfigIDLoader configIDLoader, final NodeSettingsSerializer<T> typeSerializer) {
        m_configIDSerializer = new ConfigIDSerializer(configIDLoader);
        final TypedReaderColumnSpecSerializer<T> columnSpecSerializer =
            new TypedReaderColumnSpecSerializer<>(typeSerializer);
        m_tableSpecSerializer = new TypedReaderTableSpecSerializer<>(columnSpecSerializer);
        m_tableTransformationSerializer = new TableTransformationSerializer<>(
            new ColumnTransformationSerializer<T>(columnSpecSerializer, productionPathSerializer));
    }

    @Override
    public void save(final TableSpecConfig<T> config, final NodeSettingsWO settings) {
        saveIndividualSpecs(config, settings.addNodeSettings(CFG_INDIVIDUAL_SPECS));
        m_tableTransformationSerializer.save(config.getTransformationModel(),
            settings.addNodeSettings(CFG_TABLE_TRANSFORMATION));
        settings.addString(CFG_SOURCE_GROUP_ID, config.getSourceGroupID());
        ConfigIDSerializer.saveID(config.getConfigID(), settings);
    }

    private void saveIndividualSpecs(final TableSpecConfig<T> config, final NodeSettingsWO settings) {
        for (String item : config.getItems()) {
            m_tableSpecSerializer.save(config.getSpec(item), settings.addNodeSettings(item));
        }
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings, final AdditionalParameters additionalParameters)
        throws InvalidSettingsException {
        final boolean skipEmptyColumns = additionalParameters.skipEmptyColumns();
        return load(settings, skipEmptyColumns);
    }

    private TableSpecConfig<T> load(final NodeSettingsRO settings, final boolean skipEmptyColumns)
        throws InvalidSettingsException {
        final TableTransformation<T> tableTransformation =
            m_tableTransformationSerializer.load(settings.getNodeSettings(CFG_TABLE_TRANSFORMATION), skipEmptyColumns);
        final LinkedHashMap<String, TypedReaderTableSpec<T>> individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS));
        final String sourceGroupID = settings.getString(CFG_SOURCE_GROUP_ID);
        final ConfigID configID = m_configIDSerializer.loadID(settings);
        return new DefaultTableSpecConfig<>(sourceGroupID, configID, individualSpecs, tableTransformation);
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        return load(settings, false);
    }

    private LinkedHashMap<String, TypedReaderTableSpec<T>> loadIndividualSpecs(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final LinkedHashMap<String, TypedReaderTableSpec<T>> specs = new LinkedHashMap<>();
        for (String item : settings) {
            final TypedReaderTableSpec<T> spec = m_tableSpecSerializer.load(settings.getNodeSettings(item));
            specs.put(item, spec);
        }
        return specs;
    }

}
