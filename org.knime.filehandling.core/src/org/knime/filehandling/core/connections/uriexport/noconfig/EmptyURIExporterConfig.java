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
 *   Mar 11, 2021 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.uriexport.noconfig;

import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterConfig;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 * A stateless and immutable {@link URIExporterConfig} implementation that stores not settings at all. This class is a
 * singleton and its only instance can be retrieved with {@link #getInstance()}.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @since 4.3
 * @noreference non-public API
 */
public final class EmptyURIExporterConfig implements URIExporterConfig {

    private static final EmptyURIExporterConfig SINGLETON_INSTANCE = new EmptyURIExporterConfig();

    private EmptyURIExporterConfig() {
    }

    /**
     * @return the only available instance of this class.
     */
    public static EmptyURIExporterConfig getInstance() {
        return SINGLETON_INSTANCE;
    }

    @Override
    public void configureInModel(final PortObjectSpec[] specs, final Consumer<StatusMessage> statusMessageConsumer)
        throws InvalidSettingsException {
        //nothing to do here
    }

    @Override
    public void loadSettingsForPanel(final NodeSettingsRO settings) throws NotConfigurableException {
        //nothing to do here
    }

    @Override
    public void loadSettingsForExporter(final NodeSettingsRO settings) throws InvalidSettingsException {
        //nothing to do here
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //nothing to do here
    }

    @Override
    public void validate() throws InvalidSettingsException {
        //nothing to do here
    }

    @Override
    public void saveSettingsForPanel(final NodeSettingsWO settings) throws InvalidSettingsException {
        //nothing to do here
    }

    @Override
    public void saveSettingsForExporter(final NodeSettingsWO settings) {
        //nothing to do here
    }
}
