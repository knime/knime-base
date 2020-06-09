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
package org.knime.base.node.io.filehandling.table.csv.simple;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.table.csv.AbstractCSVTableReaderNodeDialog;
import org.knime.base.node.io.filehandling.table.csv.reader.CSVTableReaderConfig;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 * Node dialog for the simple file reader node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class SimpleFileReaderNodeDialog extends AbstractCSVTableReaderNodeDialog {

    private PathAwareFileHistoryPanel m_filePanel;

    private static final String HISTORY_ID = "simple_file_reader";

    SimpleFileReaderNodeDialog(final PathAwareFileHistoryPanel pathSettings,
        final MultiTableReadConfig<CSVTableReaderConfig> config,
        final MultiTableReader<CSVTableReaderConfig, Class<?>, String> multiReader,
        final ProducerRegistry<?, ?> producerRegistry) {
        super(pathSettings, config, multiReader, producerRegistry);
        m_disableComponentsRemoteContext = false;
    }

    @Override
    protected JPanel[] getPanels() {
        return new JPanel[]{createFilePanel()};
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        final FilesHistoryPanel fileHistoryPanel = m_filePanel.getFileHistoryPanel();
        fileHistoryPanel.setPreferredSize(new Dimension(700, fileHistoryPanel.getPreferredSize().height));
        fileHistoryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileHistoryPanel.getPreferredSize().height));
        filePanel.add(fileHistoryPanel);
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    @Override
    protected void init(final PathSettings pathSettings) {
        m_filePanel = (PathAwareFileHistoryPanel)pathSettings;
        m_filePanel.createFileHistoryPanel(
            createFlowVariableModel(m_filePanel.getConfigKey(), StringType.INSTANCE),
            SimpleFileReaderNodeDialog.HISTORY_ID);
    }

    @Override
    protected void registerPreviewChangeListeners() {
        m_filePanel.getFileHistoryPanel().addChangeListener(l -> m_tableReaderPreview.configChanged());
        super.registerPreviewChangeListeners();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void loadAdditionalSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_filePanel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
    }

}
