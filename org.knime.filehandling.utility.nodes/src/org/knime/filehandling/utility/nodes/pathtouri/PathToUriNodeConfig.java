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
 *   Dec 22, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtouri;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;

/**
 * A centralized class for encapsulating the SettingsModel Objects. The save, validate and load in NodeModel simply
 * invokes this functionality
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class PathToUriNodeConfig {

    static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    static final String DATA_TABLE_INPUT_PORT_GRP_NAME = "Input Table";

    static final String DATA_TABLE_OUTPUT_PORT_GRP_NAME = "Output Table";

    private static final String CFG_COLUMN = "column";

    private static final String CFG_URL_FORMAT = "url_format";

    private static final String CFG_GENERATED_COLUMN_MODE = "generated_column_mode";

    private static final String CFG_APPENDED_COLUMN_NAME = "appended_column_name";

    private static final String CFG_URL_FORMAT_SETTINGS = "url_format_settings";

    private static final String CFG_FAIL_IF_PATH_NOT_EXISTS = "fail_if_path_not_exists";

    private static final String CFG_FAIL_ON_MISSING_VALS = "fail_on_missing_values";

    private final int m_dataTablePortIndex;

    private final int m_fileSystemConnectionPortIndex;

    private final SettingsModelString m_pathColumnNameModel;

    private final SettingsModelBoolean m_failIfPathNotExists;

    private final SettingsModelBoolean m_failOnMissingValues;

    private final SettingsModelString m_uriExporterModel;

    private final SettingsModelString m_generatedColumnMode;

    private final SettingsModelString m_appendedColumnName;

    private final URIExporterDialogHelper m_exporterDialogHelper;

    private final URIExporterModelHelper m_exporterModelHelper;

    /**
     * Constructor for the Configuration class
     *
     * @param portsConfig An object of PortsConfiguration Class
     */
    public PathToUriNodeConfig(final PortsConfiguration portsConfig) {
        m_dataTablePortIndex =
            getFirstPortIndexInGroup(portsConfig, PathToUriNodeConfig.DATA_TABLE_INPUT_PORT_GRP_NAME);
        m_fileSystemConnectionPortIndex =
            getFirstPortIndexInGroup(portsConfig, PathToUriNodeConfig.CONNECTION_INPUT_PORT_GRP_NAME);

        m_pathColumnNameModel = new SettingsModelString(CFG_COLUMN, "");

        m_failIfPathNotExists = new SettingsModelBoolean(CFG_FAIL_IF_PATH_NOT_EXISTS, true);
        m_failOnMissingValues = new SettingsModelBoolean(CFG_FAIL_ON_MISSING_VALS, true);

        m_uriExporterModel = new SettingsModelString(CFG_URL_FORMAT, URIExporterIDs.DEFAULT.toString());

        m_generatedColumnMode =
            new SettingsModelString(CFG_GENERATED_COLUMN_MODE, GenerateColumnMode.APPEND_NEW.getActionCommand());
        m_appendedColumnName = new SettingsModelString(CFG_APPENDED_COLUMN_NAME, "URI");

        m_exporterDialogHelper = new URIExporterDialogHelper(m_pathColumnNameModel, //
            m_uriExporterModel, //
            m_fileSystemConnectionPortIndex, //
            m_dataTablePortIndex);

        m_exporterModelHelper = new URIExporterModelHelper(m_pathColumnNameModel, //
            m_uriExporterModel, //
            m_fileSystemConnectionPortIndex, //
            m_dataTablePortIndex);

        m_generatedColumnMode.addChangeListener(e -> updateEnabledness());
    }

    private void updateEnabledness() {
        m_appendedColumnName.setEnabled(
            m_generatedColumnMode.getStringValue().equals(GenerateColumnMode.APPEND_NEW.getActionCommand()));
    }

    int getDataTablePortIndex() {
        return m_dataTablePortIndex;
    }

    int getFileSystemConnectionPortIndex() {
        return m_fileSystemConnectionPortIndex;
    }

    private static int getFirstPortIndexInGroup(final PortsConfiguration portsConfig, final String portGroupName) {
        final int[] portsInGroup = portsConfig.getInputPortLocation().get(portGroupName);
        if (portsInGroup != null && portGroupName.length() > 0) {
            return portsInGroup[0];
        } else {
            return -1;
        }
    }

    SettingsModelString getPathColumnNameModel() {
        return m_pathColumnNameModel;
    }

    String getPathColumnName() {
        return m_pathColumnNameModel.getStringValue();
    }

    SettingsModelString getURIExporterModel() {
        return m_uriExporterModel;
    }

    URIExporterID getURIExporterID() {
        return new URIExporterID(m_uriExporterModel.getStringValue());
    }

    SettingsModelString getAppendedColumnNameModel() {
        return m_appendedColumnName;
    }

    String getAppendedColumnName() {
        return m_appendedColumnName.getStringValue();
    }

    SettingsModelString getGeneratedColumnModeModel() {
        return m_generatedColumnMode;
    }

    GenerateColumnMode getGeneratedColumnMode() {
        if (m_generatedColumnMode.getStringValue().equals(GenerateColumnMode.APPEND_NEW.getActionCommand())) {
            return GenerateColumnMode.APPEND_NEW;
        } else {
            return GenerateColumnMode.REPLACE_SELECTED;
        }
    }

    boolean shouldAppendColumn() {
        return getGeneratedColumnMode() == GenerateColumnMode.APPEND_NEW;
    }

    SettingsModelBoolean getFailIfPathNotExistsModel() {
        return m_failIfPathNotExists;
    }

    boolean failIfPathNotExists() {
        return m_failIfPathNotExists.getBooleanValue();
    }

    SettingsModelBoolean getFailOnMissingValuesModel() {
        return m_failOnMissingValues;
    }

    boolean failOnMissingValues() {
        return m_failOnMissingValues.getBooleanValue();
    }

    URIExporterDialogHelper getExporterDialogHelper() {
        return m_exporterDialogHelper;
    }

    URIExporterModelHelper getExporterModelHelper() {
        return m_exporterModelHelper;
    }

    /**
     * Implements validate settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pathColumnNameModel.validateSettings(settings);
        m_failIfPathNotExists.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_uriExporterModel.validateSettings(settings);
        m_generatedColumnMode.validateSettings(settings);
        m_appendedColumnName.validateSettings(settings);

        if (!settings.containsKey(CFG_URL_FORMAT_SETTINGS)) {
            throw new InvalidSettingsException(String.format("Settings key %s not found", CFG_URL_FORMAT_SETTINGS));
        }
    }

    void loadValidatedSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pathColumnNameModel.loadSettingsFrom(settings);
        m_failIfPathNotExists.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_uriExporterModel.loadSettingsFrom(settings);
        m_generatedColumnMode.loadSettingsFrom(settings);
        m_appendedColumnName.loadSettingsFrom(settings);
        m_exporterModelHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws InvalidSettingsException {

        m_failIfPathNotExists.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_uriExporterModel.loadSettingsFrom(settings);

        m_exporterDialogHelper.setPortObjectSpecs(specs);
        // we validate the settings, overwriting invalid values (so we can open the dialog),
        m_exporterDialogHelper.validate(m -> {}, true);
        m_exporterDialogHelper.loadSettingsFrom(settings.getNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_pathColumnNameModel.saveSettingsTo(settings);
        m_failIfPathNotExists.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_uriExporterModel.saveSettingsTo(settings);
        m_generatedColumnMode.saveSettingsTo(settings);
        m_appendedColumnName.saveSettingsTo(settings);
        m_exporterModelHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_failIfPathNotExists.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_uriExporterModel.saveSettingsTo(settings);
        m_exporterDialogHelper.saveSettingsTo(settings.addNodeSettings(CFG_URL_FORMAT_SETTINGS));
    }

    /**
     * Options used to decide whether the newly generated column should replace selected column or should be appended as
     * new.
     *
     * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
     */
    enum GenerateColumnMode implements ButtonGroupEnumInterface {

            APPEND_NEW("Append column:"), //

            REPLACE_SELECTED("Replace selected column");

        private static final String TOOLTIP =
            "Choose whether a new column should be appended with the given name or the selected one be replaced.";

        private final String m_text;

        GenerateColumnMode(final String text) {
            this.m_text = text;
        }

        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return name();
        }

        @Override
        public String getToolTip() {
            return TOOLTIP;
        }

        @Override
        public boolean isDefault() {
            return this == APPEND_NEW;
        }
    }
}
