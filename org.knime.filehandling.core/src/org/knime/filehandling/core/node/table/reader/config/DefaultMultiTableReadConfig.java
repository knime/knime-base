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
 *   Feb 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import static org.knime.filehandling.core.node.table.reader.config.ReaderConfigUtils.getOrEmpty;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecConfig;

/**
 * Default implementation of {@link MultiTableReadConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultMultiTableReadConfig<C extends ReaderSpecificConfig<C>> implements MultiTableReadConfig<C> {

    private static final String CFG_SPEC_MERGE_MODE = "spec_merge_mode";

    //    private static final String CFG_TYPE_MAPPING_CONFIG = "type_mapping_config";

    private static final String CFG_TABLE_READ_CONFIG = "table_read_config";

    private static final String CFG_TABLE_SPEC_CONFIG = "table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

    private TableSpecConfig m_tableSpecConfig = null;

    private final TableReadConfig<C> m_tableReadConfig;

    //    private final TypeMappingConfig m_typeMappingConfig;

    private SpecMergeMode m_specMergeMode = SpecMergeMode.FAIL_ON_DIFFERING_SPECS;

    DefaultMultiTableReadConfig(final TableReadConfig<C> tableReadConfig) {
        m_tableReadConfig = tableReadConfig;
        //        m_typeMappingConfig = typeMappingConfig;
    }

    @Override
    public TableReadConfig<C> getTableReadConfig() {
        return m_tableReadConfig;
    }

    @Override
    public SpecMergeMode getSpecMergeMode() {
        return m_specMergeMode;
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        m_tableReadConfig.loadInModel(settings.getNodeSettings(CFG_TABLE_READ_CONFIG));
        //        m_typeMappingConfig.loadInModel(settings.getNodeSettings(CFG_TYPE_MAPPING_CONFIG));
        m_specMergeMode = SpecMergeMode.valueOf(settings.getString(CFG_SPEC_MERGE_MODE));
        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            m_tableSpecConfig = TableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), registry);
        }
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry) {
        m_tableReadConfig.loadInDialog(getOrEmpty(settings, CFG_TABLE_READ_CONFIG));
        //        m_typeMappingConfig.loadInDialog(getOrEmpty(settings, CFG_TYPE_MAPPING_CONFIG));
        m_specMergeMode = SpecMergeMode
            .valueOf(settings.getString(CFG_SPEC_MERGE_MODE, SpecMergeMode.FAIL_ON_DIFFERING_SPECS.name()));

        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            try {
                m_tableSpecConfig = TableSpecConfig.load(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), registry);
            } catch (InvalidSettingsException ex) {
                /* Can only happen in TableSpecConfig#load, since we checked #NodeSettingsRO#getNodeSettings(String)
                 * before. The framework takes care that #validate is called before load so we can assume that this
                 * exception does not occur.
                 */
            }
        }
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        m_tableReadConfig.save(settings.addNodeSettings(CFG_TABLE_READ_CONFIG));
        //        m_typeMappingConfig.save(settings.addNodeSettings(CFG_TYPE_MAPPING_CONFIG));
        settings.addString(CFG_SPEC_MERGE_MODE, m_specMergeMode.name());

        if (hasTableSpec()) {
            m_tableSpecConfig.save(settings.addNodeSettings(CFG_TABLE_SPEC_CONFIG));
        }
    }

    @Override
    public void validate(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        m_tableReadConfig.validate(settings.getNodeSettings(CFG_TABLE_READ_CONFIG));
        //        m_typeMappingConfig.validate(settings.getNodeSettings(CFG_TYPE_MAPPING_CONFIG));
        settings.getString(CFG_SPEC_MERGE_MODE);

        if (settings.containsKey(CFG_TABLE_SPEC_CONFIG)) {
            TableSpecConfig.validate(settings.getNodeSettings(CFG_TABLE_SPEC_CONFIG), registry);
        }
    }

    @Override
    public void setSpecMergeMode(final SpecMergeMode mode) {
        m_specMergeMode = mode;
    }

    @Override
    public TableSpecConfig getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public boolean hasTableSpec() {
        return m_tableSpecConfig != null;
    }

    @Override
    public void setTableSpecConfig(final TableSpecConfig config) {
        m_tableSpecConfig = config;
    }

}
