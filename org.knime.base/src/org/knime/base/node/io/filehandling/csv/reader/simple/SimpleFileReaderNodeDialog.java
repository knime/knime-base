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
 *   May 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.simple;

import java.nio.file.Path;

import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.csv.reader.AbstractCSVTableReaderNodeDialog;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Node dialog for the simple file reader node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class SimpleFileReaderNodeDialog extends AbstractCSVTableReaderNodeDialog {

    private PathAwareFileHistoryPanel m_filePanel;

    private static final String HISTORY_ID = "simple_file_reader";

    SimpleFileReaderNodeDialog(final PathAwareFileHistoryPanel pathSettings,
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final MultiTableReadFactory<Path, CSVTableReaderConfig, Class<?>> multiReader,
        final ProductionPathProvider<Class<?>> productionPathProvider) {
        super(pathSettings, config, multiReader, productionPathProvider, false);
    }

    @Override
    protected boolean areIOComponentsDisabled() {
        return false;
    }

    @Override
    protected JPanel[] getPanels() {
        return new JPanel[]{m_filePanel.createFilePanel()};
    }

    @Override
    protected void init(final PathSettings pathSettings) {
        m_filePanel = (PathAwareFileHistoryPanel)pathSettings;
        m_filePanel.createFileHistoryPanel(
            createFlowVariableModel(new String[]{"settings", PathAwareFileHistoryPanel.CFG_KEY_LOCATION},
                StringType.INSTANCE),
            SimpleFileReaderNodeDialog.HISTORY_ID);
    }

    @Override
    protected void registerPreviewChangeListeners() {
        m_filePanel.getFileHistoryPanel().addChangeListener(l -> configChanged());
        super.registerPreviewChangeListeners();
    }

    @Override
    protected boolean validateFileSelection() {
        return m_filePanel.getFileHistoryPanel().getSelectedFile() != null
            && !m_filePanel.getFileHistoryPanel().getSelectedFile().trim().isEmpty();
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // FIXME: saving of the file panel should be handled by the config (AP-14460 & AP-14462)
        m_filePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    @Override
    protected void loadAdditionalSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            // FIXME: loading of the file panel should be handled by the config (AP-14460 & AP-14462)
            m_filePanel.loadSettingsFrom(settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB));
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

}
