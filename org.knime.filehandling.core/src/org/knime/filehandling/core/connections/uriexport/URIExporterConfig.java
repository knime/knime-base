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
 *   Mar 10, 2021 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.uriexport;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * This interface to provides methods how to load, save and validate the settings of a {@link URIExporter}.
 *
 * <p>
 * A If a {@link URIExporter} requires settings to map paths to URIs, then its companion {@link URIExporterPanel}
 * provides a UI to edit those settings. By contract, differents methods of this interface are expected to be called,
 * depending on whether we want to edit settings in a {@link URIExporterPanel}, or apply settings with a
 * {@link URIExporter}.
 * </p>
 *
 * <p>
 * When editing settings with a {@link URIExporterPanel} settings must be loaded and saved through the respective
 * load/save method of the {@link URIExporterPanel}. Please refer to the Javadoc of {@link URIExporterPanel} for more
 * information.
 * </p>
 *
 * <p>
 * When mapping paths to URIs with a {@link URIExporter}, the following methods need to be called:
 * <ul>
 * <li>{@link #validateSettings(NodeSettingsRO)} to validate saved exporter settings before loading them (e.g. during
 * {@link NodeModel#validateSettings(NodeSettingsRO)}</li>
 * <li>{@link #loadSettingsForExporter(NodeSettingsRO)} to load previously saved exporter settings (e.g. during
 * {@link NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)}</li>
 * <li>{@link #configureInModel(PortObjectSpec[], Consumer)} once input port object specs become available (e.g. during
 * {@link NodeModel#configure(PortObjectSpec[])}. This is necessary because some {@link URIExporter}s may require the
 * input port object specs.</li>
 * </ul>
 * </p>
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @since 4.3
 * @noreference non-public API
 * @noimplement non-public API
 */
@SuppressWarnings("javadoc")
public interface URIExporterConfig {

    /**
     * Validates the provided specs against the current settings and either provides warnings via the
     * <b>statusMessageConsumer</b> if the issues are non fatal or throws an InvalidSettingsException if the current
     * configuration and the provided specs make it impossible to map paths to URIs.
     *
     * <p>This method must be called before applying loaded settingswith a {@link URIExporter}.</p>
     *
     * @param specs The input {@link PortObjectSpec specs} of the node.
     * @param statusMessageConsumer Consumer for status messages e.g. warnings.
     * @throws InvalidSettingsException if the specs are not compatible with the current settings.
     */
    void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException;

    /**
     * Loads settings from the given {@link NodeSettingsRO} (called by the {@link URIExporterPanel}).
     *
     * @param settings Node settings to load from.
     * @throws NotConfigurableException if there is no chance for the panel UI to show valid date, e.g. if the given
     *             specs lack some important columns or column types.
     */
    void loadSettingsForPanel(final NodeSettingsRO settings) throws NotConfigurableException;

    /**
     * Loads settings from the given {@link NodeSettingsRO} in order to apply them with a {@link URIExporter}.
     *
     * @param settings Node settings to load from.
     * @throws InvalidSettingsException if the settings cannot be loaded and the {@link URIExporter} would not be able
     *             to map paths to URIs.
     */
    void loadSettingsForExporter(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates the settings in the given {@link NodeSettingsRO} but does not load them.
     *
     * @param settings Node settings to validate.
     * @throws InvalidSettingsException if the settings are invalid so that a {@link URIExporter} would not be able to
     *             map paths to URIs.
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Validates the currently loaded settings. This method is called the {@link URIExporterPanel} to do any necessary
     * validations before invoking {@link #saveSettingsForPanel(NodeSettingsWO)}.
     *
     * @throws InvalidSettingsException if the settings are invalid in a way so they cannot or should not be saved.
     */
    void validate() throws InvalidSettingsException;

    /**
     * Saves the current settings to a {@link NodeSettingsWO} (called by the {@link URIExporterPanel}).
     *
     * @param settings {@link NodeSettingsWO} to save to.
     * @throws InvalidSettingsException if the settings are invalid in a way so they cannot or should not be saved.
     */
    void saveSettingsForPanel(final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Saves the settings (to be called by node model).
     *
     * @param settings Node settings to save to.
     */
    void saveSettingsForExporter(final NodeSettingsWO settings);
}
