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
 *   Jun 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Default implementation of a {@link ConfigSerializer} for {@link DefaultMultiTableReadConfig}.</br>
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig} used by the reader implementation
 * @param <TC> the type of {@link TableReadConfig} used by the reader implementation
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultMultiTableReadConfigSerializer<C extends ReaderSpecificConfig<C>, TC extends TableReadConfig<C>>
    implements ConfigSerializer<DefaultMultiTableReadConfig<C, TC>> {

    /**
     * Only kept for backwards compatibility with 4.2.</br>
     * Newer versions no longer store the SpecMergeMode here but instead store the ColumnFilterMode.
     */
    private static final String CFG_SPEC_MERGE_MODE = "spec_merge_mode";

    private static final String CFG_TABLE_READ_CONFIG = "table_read_config";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private final ProducerRegistry<?, ?> m_producerRegistry;

    private final Object m_mostGenericExternalType;

    private final ConfigSerializer<TC> m_tableReadConfigSerializer;

    /**
     * Constructor.
     *
     * @param tableReadConfigSerializer serializer for the {@link TableReadConfig} (also expected to serialize the
     *            {@link ReaderSpecificConfig})
     * @param producerRegistry {@link ProducerRegistry} for de-serializing the type mapping production paths
     * @param mostGenericExternalType the identifier for the most generic external type
     */
    public DefaultMultiTableReadConfigSerializer(final ConfigSerializer<TC> tableReadConfigSerializer,
        final ProducerRegistry<?, ?> producerRegistry, final Object mostGenericExternalType) {
        m_tableReadConfigSerializer = tableReadConfigSerializer;
        m_producerRegistry = producerRegistry;
        m_mostGenericExternalType = mostGenericExternalType;
    }

    @Override
    public void loadInDialog(final DefaultMultiTableReadConfig<C, TC> config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        m_tableReadConfigSerializer.loadInDialog(config.getTableReadConfig(),
            SettingsUtils.getOrEmpty(settings, CFG_TABLE_READ_CONFIG), specs);

        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            try {
                config.setTableSpecConfig(DefaultTableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                    m_producerRegistry, m_mostGenericExternalType, loadSpecMergeMode(settings)));
            } catch (InvalidSettingsException ex) {
                /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
                 * before. The framework takes care that #validate is called before load so we can assume that this
                 * exception does not occur.
                 */
            }
        } else {
            config.setTableSpecConfig(null);
        }
    }

    /**
     * Loads the {@link SpecMergeMode} for backwards compatibility with 4.2. 4.3 onwards will store the ColumnFilterMode
     * as part of the TableSpecConfig.
     *
     * @param settings to load from
     * @return the {@link SpecMergeMode} for workflows stored with 4.2 or {@code null} for workflows stored with 4.3 or
     *         later
     */
    private static SpecMergeMode loadSpecMergeMode(final NodeSettingsRO settings) {
        try {
            // workflows stored with 4.2 save the SpecMergeMode with the MultiTableReadConfig
            return SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE));
        } catch (InvalidSettingsException ise) {
            // workflows stored with 4.3 and later no longer use SpecMergeMode but instead rely
            // on ColumnFilterMode which is stored as part of the TableSpecConfig
            return null;
        }
    }

    @Override
    public void loadInModel(final DefaultMultiTableReadConfig<C, TC> config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_tableReadConfigSerializer.loadInModel(config.getTableReadConfig(), settings);
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            config.setTableSpecConfig(DefaultTableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG),
                m_producerRegistry, m_mostGenericExternalType, loadSpecMergeMode(settings)));
        } else {
            config.setTableSpecConfig(null);
        }

    }

    @Override
    public void saveInModel(final DefaultMultiTableReadConfig<C, TC> config, final NodeSettingsWO settings) {
        m_tableReadConfigSerializer.saveInModel(config.getTableReadConfig(),
            settings.addNodeSettings(CFG_TABLE_READ_CONFIG));
//        settings.addString(CFG_SPEC_MERGE_MODE, config.getSpecMergeMode().name());

        if (config.hasTableSpecConfig()) {
            config.getTableSpecConfig().save(settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }

    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_tableReadConfigSerializer.validate(settings.getNodeSettings(CFG_TABLE_READ_CONFIG));

        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            DefaultTableSpecConfig.validate(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), m_producerRegistry);
        }

    }

    @Override
    public void saveInDialog(final DefaultMultiTableReadConfig<C, TC> config, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        saveInModel(config, settings);
    }

}
