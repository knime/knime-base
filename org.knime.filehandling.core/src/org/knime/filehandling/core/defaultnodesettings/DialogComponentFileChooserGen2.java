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
 *   Aug 15, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.filehandling.core.filefilter.FileFilterPanel;

/**
 *
 * @author bjoern
 */
public class DialogComponentFileChooserGen2 extends DialogComponent {

    private final SettingsModelFileChooserGen2 m_settingsModel;

    private SettingsModelString m_buttonGroupSettings;

    private final NodeDialogPane m_dialogPane;

    private FSConnectionFlowVariableProvider m_connectionFlowVariableProvider;

    private final CardLayout m_modeCardLayout;

    private final JPanel m_modePanelsPanel;

    private FlowVariableModel m_pathFlowVariableModel;

    private final List<JComboBox<String>> m_connectionCombos = new ArrayList<>();

    public DialogComponentFileChooserGen2(final SettingsModelFileChooserGen2 settingsModel,
        final NodeDialogPane dialogPane, final String suffixes) {
        super(settingsModel);

        m_settingsModel = settingsModel;
        m_dialogPane = dialogPane;

        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.Y_AXIS));

        getComponentPanel().add(createModeSelectionButtonGroup());

        m_pathFlowVariableModel = m_dialogPane.createFlowVariableModel(m_settingsModel.getPath().getKey(), Type.STRING);
        m_modeCardLayout = new CardLayout();
        m_modePanelsPanel = new JPanel(m_modeCardLayout);
        m_modePanelsPanel.add(createReadSingleFileModePanel(suffixes));
        m_modePanelsPanel.add(createReadFolderModePanel(suffixes));
        m_modeCardLayout.addLayoutComponent(m_modePanelsPanel.getComponent(0), "single");
        m_modeCardLayout.addLayoutComponent(m_modePanelsPanel.getComponent(1), "folder");
        getComponentPanel().add(m_modePanelsPanel);

    }

    private Component createReadFolderModePanel(final String suffixes) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(createConnectionAndPathSelectionPanel(suffixes));

        final FileFilterPanel filterPanel = new FileFilterPanel();
        panel.add(filterPanel);

        return panel;
    }

    private Component createReadSingleFileModePanel(final String suffixes) {
        return createConnectionAndPathSelectionPanel(suffixes);
    }

    private Component createConnectionAndPathSelectionPanel(final String suffixes) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        final JComboBox connectionCombo = new JComboBox<String>(new String[0]);
        panel.add(connectionCombo);
        m_connectionCombos.add(connectionCombo);

        final FilesHistoryPanel fileHistoryPanel =
            new FilesHistoryPanel(m_pathFlowVariableModel, "filechoosergen2", LocationValidation.None, suffixes);
        fileHistoryPanel.setDialogType(JFileChooser.OPEN_DIALOG);
        fileHistoryPanel.setAllowRemoteURLs(false);
        fileHistoryPanel.setShowConnectTimeoutField(false);
        //FIXME we need to invoke fileHistoryPanel.addToHistory() when saving the dialog settings
        fileHistoryPanel
            .addChangeListener((e) -> m_settingsModel.getPath().setStringValue(fileHistoryPanel.getSelectedFile()));
        panel.add(fileHistoryPanel);

        return panel;
    }

    private JPanel createModeSelectionButtonGroup() {
        m_buttonGroupSettings =
            new SettingsModelString("file_reading_mode", "Read single file");
        m_buttonGroupSettings.addChangeListener(e -> updateCardLayout());

        final ButtonGroupEnumInterface[] options = new ButtonGroupEnumInterface[2];
        options[0] = new FileReadingModeButtonGroup("Read single file", true, "Reads in a single file", "SINGLE");
        options[1] = new FileReadingModeButtonGroup("Read folder", false, "Reads in a folder", "MULTIPLE");

        final DialogComponentButtonGroup buttonGroup =
            new DialogComponentButtonGroup(m_buttonGroupSettings, "", false, options);

        return buttonGroup.getComponentPanel();
    }

    private void updateCardLayout() {
        if (m_buttonGroupSettings.getStringValue().equals("SINGLE")) {
            m_modeCardLayout.show(m_modePanelsPanel, "single");
        } else {
            m_modeCardLayout.show(m_modePanelsPanel, "folder");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        m_connectionFlowVariableProvider = new FSConnectionFlowVariableProvider(m_dialogPane);

        for (final JComboBox<String> connectionCombo : m_connectionCombos) {
            final DefaultComboBoxModel<String> comboBoxModel = (DefaultComboBoxModel<String>)connectionCombo.getModel();
            comboBoxModel.removeAllElements();

            for (final String connectionName : m_connectionFlowVariableProvider.allConnectionNames()) {
                comboBoxModel.addElement(connectionName);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_buttonGroupSettings.setEnabled(enabled);
        for (Component modePanel : m_modePanelsPanel.getComponents()) {
            for (Component modePanelElement : ((JPanel)modePanel).getComponents()) {
                modePanelElement.setEnabled(enabled);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        // TODO Auto-generated method stub
    }

    private final class FileReadingModeButtonGroup implements ButtonGroupEnumInterface {

        private final String m_text;

        private final String m_tooltip;

        private final boolean m_default;

        private final String m_command;

        private FileReadingModeButtonGroup(final String text, final boolean isDefault, final String tooltip,
            final String command) {
            m_text = text;
            m_tooltip = tooltip;
            m_default = isDefault;
            m_command = command;
        }


        @Override
        public String getText() {
            return m_text;
        }

        @Override
        public String getActionCommand() {
            return m_command;
        }

        @Override
        public String getToolTip() {
            return m_tooltip;
        }

        @Override
        public boolean isDefault() {
            return m_default;
        }

    }
}
