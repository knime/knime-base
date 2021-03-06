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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Base interface for configuration classes used in the table reader framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface StorableReaderConfig {

    /**
     * Loads the configuration in the dialog.
     *
     * @param settings to load from
     * @param specs input {@link PortObjectSpec specs} of the node
     * @throws NotConfigurableException if the node can't be configured
     */
    void loadInDialog(final NodeSettingsRO settings, PortObjectSpec[] specs) throws NotConfigurableException;

    /**
     * Loads the configuration in the node model.
     *
     * @param settings to load from
     * @throws InvalidSettingsException if the settings are invalid or can't be loaded
     */
    void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     */
    void validate(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the configuration to settings in the node model.
     *
     * @param settings to save to
     */
    void saveInModel(final NodeSettingsWO settings);

    /**
     * Saves the configuration to settings in the node dialog.
     *
     * @param settings to save to
     * @throws InvalidSettingsException if the config is invalid
     */
    void saveInDialog(final NodeSettingsWO settings) throws InvalidSettingsException;

}
