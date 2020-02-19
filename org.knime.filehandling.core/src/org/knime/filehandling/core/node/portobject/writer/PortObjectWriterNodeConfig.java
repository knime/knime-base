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
 */
package org.knime.filehandling.core.node.portobject.writer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeConfig;

/**
 * Configuration class for port object writer nodes that can be extended with additional configurations.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public class PortObjectWriterNodeConfig extends PortObjectIONodeConfig {

    /** Config key for overwrite checkbox. */
    private static final String CFG_OVERWRITE = "overwrite";

    /** Config key for create directory checkbox. */
    private static final String CFG_CREATE_DIRECTORY = "create_directory";

    private final SettingsModelBoolean m_overwriteModel = new SettingsModelBoolean(CFG_OVERWRITE, false);

    private final SettingsModelBoolean m_createDirectoryModel = new SettingsModelBoolean(CFG_CREATE_DIRECTORY, false);

    /**
     * Constructor for configs in which the file chooser doesn't filter on file suffixes.
     */
    public PortObjectWriterNodeConfig() {
        super();
    }

    /**
     * Constructor for configs in which the file chooser filters on a set of file suffixes.
     *
     * @param fileSuffixes the suffixes to filter on
     */
    public PortObjectWriterNodeConfig(final String[] fileSuffixes) {
        super(fileSuffixes);
    }

    /**
     * @return the overwriteModel
     */
    public SettingsModelBoolean getOverwriteModel() {
        return m_overwriteModel;
    }

    /**
     * @return the createDirectoryModel
     */
    public SettingsModelBoolean getCreateDirectoryModel() {
        return m_createDirectoryModel;
    }

    @Override
    protected void validateConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateConfigurationForModel(settings);
        m_overwriteModel.validateSettings(settings);
        m_createDirectoryModel.validateSettings(settings);
    }

    @Override
    protected void saveConfigurationForModel(final NodeSettingsWO settings) {
        super.saveConfigurationForModel(settings);
        m_overwriteModel.saveSettingsTo(settings);
        m_createDirectoryModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationForModel(settings);
        m_overwriteModel.loadSettingsFrom(settings);
        m_createDirectoryModel.loadSettingsFrom(settings);
    }
}
