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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Loads, saves and validates configs in the node dialog and model.</br>
 * Different serializers can save the settings in different structures which allows to adjust the settings structure to
 * match the node dialog.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of config this serializer operates on
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface ConfigSerializer<C> {

    /**
     * Loads the config in the node dialog.
     *
     * @param config to load the {@link NodeSettingsRO} into
     * @param settings to load from
     * @param specs input specs of the node
     * @throws NotConfigurableException if the specs don't allow a configuration of the node
     */
    void loadInDialog(final C config, final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException;

    /**
     * Loads the config in the node model.
     *
     * @param config to laod the {@link NodeSettingsRO} into
     * @param settings to load from
     * @throws InvalidSettingsException shouldn't be thrown since the framework ensures that
     *             {@link #validate(Object, NodeSettingsRO)} is called first
     */
    void loadInModel(final C config, final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the config in the node model.
     *
     * @param config to save
     * @param settings {@link NodeSettingsWO} to save to
     */
    void saveInModel(final C config, final NodeSettingsWO settings);

    /**
     * Saves the config in the node dialog.
     *
     * @param config to save
     * @param settings {@link NodeSettingsWO} to save to
     * @throws InvalidSettingsException if the settings are invalid
     */
    void saveInDialog(final C config, final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Validates the provided {@link NodeSettingsRO}.
     *
     * @param config to validate
     * @param settings {@link NodeSettingsRO} to validate
     *
     * @throws InvalidSettingsException if the settings are invalid
     */
    void validate(final C config, final NodeSettingsRO settings) throws InvalidSettingsException;

}
