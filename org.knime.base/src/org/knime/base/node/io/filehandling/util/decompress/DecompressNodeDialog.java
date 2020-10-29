/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 24, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.decompress;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * The NodeDialog for the "Decompress Files" Node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DecompressNodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "unzip_files_history";

    private final DialogComponentReaderFileChooser m_inputFileChooserPanel;

    private final DialogComponentWriterFileChooser m_outputDirChooserPanel;

    private final DecompressNodeConfig m_config;

    DecompressNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new DecompressNodeConfig(portsConfig);

        final FlowVariableModel writeFvm = createFlowVariableModel(
            m_config.getOutputDirChooserModel().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_outputDirChooserPanel =
            new DialogComponentWriterFileChooser(m_config.getOutputDirChooserModel(), FILE_HISTORY_ID, writeFvm,
                s -> new DecompressStatusMessageReporter(s, m_config.getInputFileChooserModel().createClone()));

        final FlowVariableModel readFvm = createFlowVariableModel(
            m_config.getInputFileChooserModel().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_inputFileChooserPanel = new DialogComponentReaderFileChooser(m_config.getInputFileChooserModel(),
            FILE_HISTORY_ID, readFvm, FilterMode.FILE);

        m_config.getInputFileChooserModel().addChangeListener(l -> m_outputDirChooserPanel.updateComponent());

        addTab("Settings", initLayout());
    }

    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().fillHorizontal().setWeightX(1);

        panel.add(createInputFilePanel(), gbc.build());
        gbc.incY();
        panel.add(createOutputDirPanel(), gbc.build());
        gbc.incY().setWeightY(1);
        panel.add(new JPanel(), gbc.build());

        return panel;
    }

    private JPanel createInputFilePanel() {
        final JPanel filePanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY();
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source"));
        filePanel.add(m_inputFileChooserPanel.getComponentPanel(), gbc.fillHorizontal().setWeightX(1).build());
        return filePanel;
    }

    private JPanel createOutputDirPanel() {
        final JPanel filePanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY();
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));
        filePanel.add(m_outputDirChooserPanel.getComponentPanel(), gbc.fillHorizontal().setWeightX(1).build());
        return filePanel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_inputFileChooserPanel.saveSettingsTo(settings);
        m_outputDirChooserPanel.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_inputFileChooserPanel.loadSettingsFrom(settings, specs);
        m_outputDirChooserPanel.loadSettingsFrom(settings, specs);
    }
}
