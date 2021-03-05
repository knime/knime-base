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
 *   Mar 2, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeConfig;

/**
 * Node configuration of the Transfer Files/Folder (Table input) node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFilesTableNodeConfig extends AbstractTransferFilesNodeConfig {

    private static final String CFG_INPUT_COLUMN = "source_column";

    private static final String CFG_DESTINATION_DEFINED_BY_TABLE = "from_table";

    private static final String CFG_DESTINATION_COLUMN = "destination_column";

    private final SettingsModelString m_srcPathColModel;

    private final SettingsModelString m_destPathColModel;

    private boolean m_destDefinedByTableCol;

    /**
     * Constructor.
     *
     * @param destinationFileChooserSettings the destination file chooser settings model
     */
    TransferFilesTableNodeConfig(final SettingsModelWriterFileChooser destFileChooserSettings) {
        super(destFileChooserSettings);
        m_srcPathColModel = new SettingsModelString(CFG_INPUT_COLUMN, null);
        m_destPathColModel = new SettingsModelString(CFG_DESTINATION_COLUMN, null);
        m_destDefinedByTableCol = false;
    }

    SettingsModelString getSrcPathColModel() {
        return m_srcPathColModel;
    }

    SettingsModelString getDestPathColModel() {
        return m_destPathColModel;
    }

    void destDefinedByTableColumn(final boolean destDefinedByTableCol) {
        m_destDefinedByTableCol = destDefinedByTableCol;
    }

    boolean destDefinedByTableColumn() {
        return m_destDefinedByTableCol;
    }

    @Override
    protected void validateAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_srcPathColModel.validateSettings(settings);
        m_destPathColModel.validateSettings(settings);
        if (settings.getBoolean(CFG_DESTINATION_DEFINED_BY_TABLE)
            && settings.getString(CFG_INPUT_COLUMN, "").equals(settings.getString(CFG_DESTINATION_COLUMN, ""))) {
            throw new InvalidSettingsException("The source and destination column cannot be the same");
        }
    }

    @Override
    protected void saveAdditionalSettingsForModel(final NodeSettingsWO settings) {
        m_srcPathColModel.saveSettingsTo(settings);
        settings.addBoolean(CFG_DESTINATION_DEFINED_BY_TABLE, m_destDefinedByTableCol);
        m_destPathColModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadSourceLocationSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_srcPathColModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadAdditionalSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_destDefinedByTableCol = settings.getBoolean(CFG_DESTINATION_DEFINED_BY_TABLE);
        m_destPathColModel.loadSettingsFrom(settings);
    }

    void saveDestDefinedByTableColInDialog(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_DESTINATION_DEFINED_BY_TABLE, m_destDefinedByTableCol);
    }

    void loadDestDefinedByTableColInDialog(final NodeSettingsRO settings) {
        m_destDefinedByTableCol = settings.getBoolean(CFG_DESTINATION_DEFINED_BY_TABLE, m_destDefinedByTableCol);
    }

}
