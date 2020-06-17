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
 *   Apr 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.fileselection;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.EnumSet;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node dialog for testing {@link FileSelectionDialog}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FileSelectionDialogTestNodeDialog extends NodeDialogPane {

    private static final String HISTORY_ID = "dummy_history";

    private final DialogComponentReaderFileChooser m_readDialog;

    private final DialogComponentWriterFileChooser m_overwriteDialog;

    private final DialogComponentWriterFileChooser m_overwritePolicyDialog;

    private final JTextField m_extensions = new JTextField(20);

    FileSelectionDialogTestNodeDialog(final PortsConfiguration portsConfig, final String fileSystemPortIdentifier) {
        final SettingsModelReaderFileChooser readSettings =
            new SettingsModelReaderFileChooser("read", portsConfig, fileSystemPortIdentifier, FilterMode.FILE);
        final FlowVariableModel readFvm =
            createFlowVariableModel(readSettings.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_readDialog = new DialogComponentReaderFileChooser(readSettings, HISTORY_ID, readFvm,
            FilterMode.values());
        m_readDialog.getComponentPanel().setBorder(BorderFactory.createTitledBorder("Read"));

        final SettingsModelWriterFileChooser binaryPolicySettings =
            new SettingsModelWriterFileChooser("write", portsConfig, fileSystemPortIdentifier, FilterMode.FILE,
                FileOverwritePolicy.OVERWRITE, EnumSet.of(FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.FAIL));

        final FlowVariableModel writeFvm =
            createFlowVariableModel(binaryPolicySettings.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_overwriteDialog = new DialogComponentWriterFileChooser(binaryPolicySettings, HISTORY_ID, writeFvm);

        m_overwriteDialog.getComponentPanel().setBorder(BorderFactory.createTitledBorder("Write"));

        final SettingsModelWriterFileChooser allPolicySettings =
            new SettingsModelWriterFileChooser("overwrite_policy", portsConfig, fileSystemPortIdentifier,
                FilterMode.FILE, FileOverwritePolicy.FAIL, EnumSet.allOf(FileOverwritePolicy.class));

        final FlowVariableModel overwritePolicyFvm =
            createFlowVariableModel(allPolicySettings.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_overwritePolicyDialog =
            new DialogComponentWriterFileChooser(allPolicySettings, HISTORY_ID, overwritePolicyFvm);

        m_overwritePolicyDialog.getComponentPanel().setBorder(BorderFactory.createTitledBorder("Overwrite policy"));

        m_extensions.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(final DocumentEvent e) {
                handleExtensionChange();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                handleExtensionChange();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                handleExtensionChange();
            }
        });
        addTab("Options", layout());
    }

    private JPanel layout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
        panel.add(new JLabel("File extensions:"), gbc.resetX().incY().build());
        panel.add(m_extensions, gbc.incX().build());
        panel.add(m_readDialog.getComponentPanel(),
            gbc.resetX().incY().setWidth(3).fillHorizontal().setWeightX(1).build());
        panel.add(m_overwriteDialog.getComponentPanel(), gbc.incY().build());
        panel.add(m_overwritePolicyDialog.getComponentPanel(), gbc.incY().build());
        return panel;
    }

    private void handleExtensionChange() {
        final String[] extensions = getExtensions();
        m_readDialog.getSettingsModel().setFileExtensions(extensions);
        m_overwriteDialog.getSettingsModel().setFileExtensions(extensions);
        m_overwritePolicyDialog.getSettingsModel().setFileExtensions(extensions);
    }

    private String[] getExtensions() {
        return Arrays.stream(m_extensions.getText().split(",")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_readDialog.saveSettingsTo(settings);
        m_overwriteDialog.saveSettingsTo(settings);
        m_overwritePolicyDialog.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_readDialog.loadSettingsFrom(settings, specs);
        m_overwriteDialog.loadSettingsFrom(settings, specs);
        m_overwritePolicyDialog.loadSettingsFrom(settings, specs);
    }

}
