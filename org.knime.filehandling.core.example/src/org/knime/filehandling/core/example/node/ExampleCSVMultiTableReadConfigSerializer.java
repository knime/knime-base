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
 *   28 Jun 2021 (Moditha Hewasinghaget): created
 */
package org.knime.filehandling.core.example.node;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.config.ConfigSerializer;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDFactory;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * The {@link ConfigSerializer} for the example CSV reader node. This class
 * serializes the settings for the reader node.
 * 
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
enum ExampleCSVMultiTableReadConfigSerializer
        implements ConfigSerializer<ExampleCSVMultiTableReadConfig>, ConfigIDFactory<ExampleCSVMultiTableReadConfig> {

    /**
     * Singleton instance.
     */
    INSTANCE;

    @Override
    public void loadInDialog(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings,
            final PortObjectSpec[] specs) {
        loadSettingsTabInDialog(config, SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    private static void loadSettingsTabInDialog(final ExampleCSVMultiTableReadConfig config, // NOSONAR
            final NodeSettingsRO settings) {
        // the node has no settings yet
    }

    @Override
    public void loadInModel(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        loadSettingsTabInModel(config, settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
    }

    private static void loadSettingsTabInModel(final ExampleCSVMultiTableReadConfig config, // NOSONAR
            final NodeSettingsRO settings) {
        // the node has no settings yet
    }

    @Override
    public void saveInModel(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        // the node has no settings yet
    }

    @Override
    public void saveInDialog(final ExampleCSVMultiTableReadConfig config, final NodeSettingsWO settings) {
        saveInModel(config, settings);
    }

    @Override
    public void validate(final ExampleCSVMultiTableReadConfig config, final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // the node has no settings yet
    }

    @Override
    public ConfigID createFromConfig(final ExampleCSVMultiTableReadConfig config) {
        final NodeSettings settings = new NodeSettings("example_csv_reader");
        return new NodeSettingsConfigID(settings);
    }

    @Override
    public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        return new NodeSettingsConfigID(settings.getNodeSettings("example_csv_reader"));
    }
}
