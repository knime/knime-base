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
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.core.node.util.LocalFileSystemBrowser;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.filefilter.FileFilter.FilterType;
import org.knime.filehandling.core.filefilter.FileFilterDialog;
import org.knime.filehandling.core.filefilter.FileFilterPanel;
import org.knime.workbench.explorer.ExplorerMountTable;

/**
 * Dialog component that allows selecting a file or multiple files in a folder. It provides the possibility to connect
 * to different file systems, as well as file filtering based on file extensions, regular expressions or wildcard.
 *
 * @author Björn Lohrmann, KNIME GmbH, Berlin, Germany
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
public class DialogComponentFileChooser2 extends DialogComponent {

    /** Node logger */
    static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentFileChooser2.class);

    /** Flow variable provider used to retrieve connection information to different file systems */
    private FSConnectionFlowVariableProvider m_connectionFlowVariableProvider;

    /** Node dialog pane */
    private final NodeDialogPane m_dialogPane;

    /** Flow variable model */
    private final FlowVariableModel m_pathFlowVariableModel;

    /** JPanel holding the file system connection label and combo boxes */
    private final JPanel m_connectionSettingsPanel = new JPanel();

    /** Card layout used to swap between different views (show KNIME file systems or not) */
    private final CardLayout m_connectionSettingsCardLayout = new CardLayout();

    /** Label for the file system connection combo boxes */
    private final JLabel m_connectionLabel;

    /** Combo box to select the file system */
    private final JComboBox<FileSystemChoice> m_connections;

    /** Combo box to select the KNIME file system connections */
    private final JComboBox<KNIMEConnection> m_knimeConnections;

    /** Check box to select whether to include sub folders while listing files in folders */
    private final JCheckBox m_includeSubfolders;

    /** Check box to select whether to filter files or not */
    private final JCheckBox m_filterFiles;

    /** Label for the {@code FilesHistoryPanel} */
    private final JLabel m_fileFolderLabel;

    /** FilesHistoryPanel used to select a file or a folder */
    private final FilesHistoryPanel m_fileHistoryPanel;

    /** Button to open the dialog that contains options for file filtering */
    private final JButton m_configureFilter;

    /** Panel containing options for file filtering */
    private final FileFilterPanel m_fileFilterPanel;

    /** Extra dialog containing the file filter panel */
    private FileFilterDialog m_fileFilterDialog;

    /** Label containing status messages */
    private final JLabel m_statusMessage;

    /** Swing worker used to do file scanning in the background */
    private StatusMessageSwingWorker m_statusMessageSwingWorker;

    /** Flag to temporarily disable event processing from Swing components or the settings model */
    private boolean m_ignoreUpdates;

    /** String used for file system connection label. */
    private static final String CONNECTION_LABEL = "Read from: ";

    /** String used for the label next to the {@code FilesHistoryPanel} */
    private static final String FILE_FOLDER_LABEL = "File/Folder:";

    /** String used for the label next to the {@code FilesHistoryPanel} */
    private static final String URL_LABEL = "URL:";

    /** Empty string used for the status message label */
    private static final String EMPTY_STRING = " ";

    /** Identifier for the KNIME file system connection view in the card layout */
    private static final String KNIME_CARD_VIEW_IDENTIFIER = "KNIME";

    /** Identifier for the default file system connection view in the card layout */
    private static final String DEFAULT_CARD_VIEW_IDENTIFIER = "DEFAULT";

    /** String used as label for the include sub folders check box */
    private static final String INCLUDE_SUBFOLDERS_LABEL = "Include subfolders";

    /** String used as label for the filter files check box */
    private static final String FILTER_FILES_LABEL = "Filter files in folder";

    /** String used as button label */
    private static final String CONFIGURE_BUTTON_LABEL = "Configure";

    /**
     * Creates a new instance of {@code DialogComponentFileChooser2}.
     *
     * @param settingsModel the settings model storing all the necessary information
     * @param historyId id used to store file history used by {@link FilesHistoryPanel}
     * @param dialogPane the {@link NodeDialogPane} this component is used for
     * @param suffixes array of file suffixes used as defaults for file filtering options
     */
    public DialogComponentFileChooser2(final SettingsModelFileChooser2 settingsModel, final String historyId,
        final NodeDialogPane dialogPane, final String... suffixes) {
        super(settingsModel);

        m_ignoreUpdates = false;

        m_dialogPane = dialogPane;
        m_pathFlowVariableModel =
            m_dialogPane.createFlowVariableModel(SettingsModelFileChooser2.PATH_OR_URL_KEY, Type.STRING);

        m_connectionLabel = new JLabel(CONNECTION_LABEL);

        m_connections = new JComboBox<>(new FileSystemChoice[0]);
        m_connections.setEnabled(true);

        m_knimeConnections = new JComboBox<>();

        m_fileFolderLabel = new JLabel(FILE_FOLDER_LABEL);

        m_fileHistoryPanel = new FilesHistoryPanel(m_pathFlowVariableModel, historyId, new LocalFileSystemBrowser(),
            FileSelectionMode.FILES_AND_DIRECTORIES, DialogType.OPEN_DIALOG, suffixes);

        m_includeSubfolders = new JCheckBox(INCLUDE_SUBFOLDERS_LABEL);
        m_filterFiles = new JCheckBox(FILTER_FILES_LABEL);
        m_configureFilter = new JButton(CONFIGURE_BUTTON_LABEL);
        m_configureFilter.addActionListener(e -> showFileFilterConfigurationDialog());

        m_fileFilterPanel = new FileFilterPanel(suffixes);

        m_statusMessage = new JLabel(EMPTY_STRING);

        m_statusMessageSwingWorker = null;

        initLayout();

        // Fill combo boxes
        updateConnectionsCombo();
        updateKNIMEConnectionsCombo();
        addEventHandlers();
        updateComponent();
        getModel().addChangeListener((e) -> updateComponent());
    }

    /** Initialize the layout of the dialog component */
    private final void initLayout() {
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel.add(m_connectionLabel, gbc);

        gbc.gridx++;
        panel.add(m_connections, gbc);

        gbc.gridx++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        initConnectionSettingsPanelLayout();
        panel.add(m_connectionSettingsPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = new Insets(5, 5, 5, 0);
        panel.add(m_fileFolderLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(m_fileHistoryPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(m_includeSubfolders, gbc);

        gbc.gridx++;
        panel.add(m_filterFiles, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(m_configureFilter, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        panel.add(m_statusMessage, gbc);
    }

    /** Initialize the file system connection panel layout */
    private final void initConnectionSettingsPanelLayout() {
        m_connectionSettingsPanel.setLayout(m_connectionSettingsCardLayout);

        m_connectionSettingsPanel.add(initKNIMEConnectionPanel(), KNIME_CARD_VIEW_IDENTIFIER);
        m_connectionSettingsPanel.add(new JPanel(), DEFAULT_CARD_VIEW_IDENTIFIER);
    }

    /** Initialize the KNIME file system connection component */
    private final Component initKNIMEConnectionPanel() {
        final JPanel knimeConnectionPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        knimeConnectionPanel.add(m_knimeConnections, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        knimeConnectionPanel.add(Box.createHorizontalGlue(), gbc);

        return knimeConnectionPanel;
    }

    /** Add event handlers for UI components */
    private void addEventHandlers() {
        m_connections.addActionListener(e -> handleUIChange());
        m_knimeConnections.addActionListener(e -> handleUIChange());
        m_fileHistoryPanel.addChangeListener(e -> handleUIChange());
        m_includeSubfolders.addActionListener(e -> handleUIChange());
        m_filterFiles.addActionListener(e -> handleUIChange());
    }

    /** Method to update settings model */
    private void handleUIChange() {
        if (m_ignoreUpdates) {
            return;
        }
        updateSettingsModel();
        triggerStatusMessageUpdate();
        updateEnabledness();
        //FIXME put that in action listener
        updateFileHistoryPanel();
    }

    /**
     *
     */
    private void updateFileHistoryPanel() {
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());
        if (fsChoice.getType() == Choice.FLOW_VARIABLE_FS) {
            final Optional<String> key = m_connectionFlowVariableProvider.connectionKeyOf(fsChoice.getId());
            if (key.isPresent()) {
                final Optional<FSConnection> connection = FSConnectionRegistry.getInstance().retrieve(key.get());
                if (connection.isPresent()) {
                    m_fileHistoryPanel.setFileSystemBrowser(connection.get().getFileSystemBrowser());
                }
            }
        } else {
            m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
        }

    }

    /** Method called if file filter configuration button is clicked */
    private void showFileFilterConfigurationDialog() {
        Frame f = null;
        Container c = getComponentPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }
        if (m_fileFilterDialog == null) {
            m_fileFilterDialog = new FileFilterDialog(f, m_fileFilterPanel);
        }
        m_fileFilterDialog.setLocationRelativeTo(c);
        m_fileFilterDialog.setVisible(true);

        if (m_fileFilterDialog.getResultStatus() == JOptionPane.OK_OPTION) {
            // updates the settings model, which in turn updates the UI
            updateSettingsModel();
        } else {
            // overwrites the values in the file filter panel components with those
            // from the settings model
            updateComponent();
        }
    }

    /** Method to update enabledness of components */
    private void updateEnabledness() {
        if (m_connections.getSelectedItem().equals(FileSystemChoice.getKnimeFsChoice())) {
            // KNIME connections are selected
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, KNIME_CARD_VIEW_IDENTIFIER);

            m_knimeConnections.setEnabled(true);
            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(FILE_FOLDER_LABEL);
            m_fileHistoryPanel.setEnabled(true);
            m_fileHistoryPanel.setBrowseable(true);

            m_includeSubfolders.setEnabled(true);
            m_filterFiles.setEnabled(true);
            m_configureFilter.setEnabled(m_filterFiles.isSelected());

        } else if (m_connections.getSelectedItem().equals(FileSystemChoice.getCustomFsUrlChoice())) {
            // Custom URLs are selected
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);

            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(URL_LABEL);
            m_fileHistoryPanel.setEnabled(true);
            m_fileHistoryPanel.setBrowseable(false);

            m_includeSubfolders.setEnabled(false);
            m_filterFiles.setEnabled(false);
            m_configureFilter.setEnabled(false);
        } else {
            // some flow variable connection is selected, or we are using the local FS
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);

            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(FILE_FOLDER_LABEL);
            m_fileHistoryPanel.setEnabled(true);
            m_fileHistoryPanel.setBrowseable(true);

            m_includeSubfolders.setEnabled(true);
            m_filterFiles.setEnabled(true);
            m_configureFilter.setEnabled(m_filterFiles.isSelected());
        }

        getComponentPanel().repaint();
    }

    /** Method to update the status message */
    private void triggerStatusMessageUpdate() {
        if (m_statusMessageSwingWorker != null) {
            m_statusMessageSwingWorker.cancel(true);
            m_statusMessageSwingWorker = null;
        }

        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        if (model.getPathOrURL() != null && !model.getPathOrURL().isEmpty()) {
            m_statusMessageSwingWorker = new StatusMessageSwingWorker(m_connectionFlowVariableProvider,
                ((SettingsModelFileChooser2)getModel()).clone(), m_statusMessage);
            m_statusMessageSwingWorker.execute();
        }
    }

    @Override
    protected void updateComponent() {
        if (m_ignoreUpdates) {
            return;
        }

        m_ignoreUpdates = true;

        // add connections coming from flow variables
        updateFlowVariableConnectionsCombo();

        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();

        // sync connection combo box
        final FileSystemChoice fileSystem = model.getFileSystemChoice();
        if ((fileSystem != null) && !fileSystem.equals(m_connections.getSelectedItem())) {
            m_connections.setSelectedItem(fileSystem);
        }
        // sync knime connection check box
        final String knimeFileSystem = model.getKNIMEFileSystem();
        if ((knimeFileSystem != null) && !knimeFileSystem.equals(m_knimeConnections.getSelectedItem())) {
            m_knimeConnections.setSelectedItem(knimeFileSystem);
        }
        // sync file history panel
        final String pathOrUrl = model.getPathOrURL();
        if ((pathOrUrl != null) && !pathOrUrl.equals(m_fileHistoryPanel.getSelectedFile())) {
            m_fileHistoryPanel.setSelectedFile(pathOrUrl);
        }
        // sync sub folder check box
        final boolean includeSubfolders = model.getIncludeSubfolders();
        if (includeSubfolders != m_includeSubfolders.isSelected()) {
            m_includeSubfolders.setSelected(includeSubfolders);
        }
        // sync filter files check box
        final boolean filterFiles = model.getFilterFiles();
        if (filterFiles != m_filterFiles.isSelected()) {
            m_filterFiles.setSelected(filterFiles);
        }

        // sync filter mode combo box
        final String filterMode = model.getFilterMode();
        if ((filterMode != null) && !filterMode.equals(m_fileFilterPanel.getSelectedFilterType().getDisplayText())) {
            m_fileFilterPanel.setFilterType(FilterType.fromDisplayText(filterMode));
        }
        // sync filter expression combo box
        final String filterExpr = model.getFilterExpression();
        if ((filterExpr != null) && !filterExpr.equals(m_fileFilterPanel.getSelectedFilterExpression())) {
            m_fileFilterPanel.setFilterExpression(filterExpr);
        }
        // sync case sensitivity check box
        final boolean caseSensitive = model.getCaseSensitive();
        if (caseSensitive != m_fileFilterPanel.getCaseSensitive()) {
            m_fileFilterPanel.setCaseSensitive(caseSensitive);
        }

        triggerStatusMessageUpdate();
        setEnabledComponents(model.isEnabled());

        m_ignoreUpdates = false;
    }

    /** Method to update and add default file system connections to combo box */
    private void updateConnectionsCombo() {
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();
        FileSystemChoice.getDefaultChoices().stream().forEach(c -> connectionsModel.addElement(c));
    }

    /** Method to update and add flow variable file system connections to combo box */
    private void updateFlowVariableConnectionsCombo() {
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();
        m_connectionFlowVariableProvider = new FSConnectionFlowVariableProvider(m_dialogPane);
        for (final String connectionName : m_connectionFlowVariableProvider.allConnectionNames()) {
            if (connectionsModel.getIndexOf(connectionName) < 0) {
                connectionsModel.insertElementAt(FileSystemChoice.createFlowVariableFileSystemChoice(connectionName),
                    0);
            }
        }
    }

    /** Method to update and add KNIME file system connections to combo box */
    private void updateKNIMEConnectionsCombo() {
        final DefaultComboBoxModel<KNIMEConnection> knimeConnectionsModel =
            (DefaultComboBoxModel<KNIMEConnection>)m_knimeConnections.getModel();
        knimeConnectionsModel.removeAllElements();
        ExplorerMountTable.getAllMountedIDs().stream().forEach(
            id -> knimeConnectionsModel.addElement(KNIMEConnection.getOrCreateMountpointAbsoluteConnection(id)));
        knimeConnectionsModel.addElement(KNIMEConnection.MOUNTPOINT_RELATIVE_CONNECTION);
        knimeConnectionsModel.addElement(KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION);
        knimeConnectionsModel.addElement(KNIMEConnection.NODE_RELATIVE_CONNECTION);
    }

    private void updateSettingsModel() {
        m_ignoreUpdates = true;
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        model.setFileSystem(((FileSystemChoice)m_connections.getSelectedItem()).getId());
        model.setPathOrURL(m_fileHistoryPanel.getSelectedFile());
        model.setIncludeSubfolders(m_includeSubfolders.isEnabled() && m_includeSubfolders.isSelected());
        model.setFilterFiles(m_filterFiles.isEnabled() && m_filterFiles.isSelected());
        model.setFilterMode(m_fileFilterPanel.getSelectedFilterType().getDisplayText());
        model.setFilterExpression(m_fileFilterPanel.getSelectedFilterExpression());
        model.setCaseSensitive(m_fileFilterPanel.getCaseSensitive());
        m_ignoreUpdates = false;

        updateComponent();
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateSettingsModel();
        m_fileHistoryPanel.addToHistory();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // this 'should' be done during #loadSettingsFrom (which is final) -- this method is called from there
        m_fileHistoryPanel.updateHistory();
        // otherwise we are good - independent of the incoming spec
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_connectionLabel.setEnabled(enabled);
        m_fileHistoryPanel.setEnabled(enabled);
        if (enabled) {
            updateEnabledness();
        } else {
            // disable
            m_includeSubfolders.setEnabled(enabled);
            m_filterFiles.setEnabled(enabled);
            m_configureFilter.setEnabled(enabled);
        }
    }

    @Override
    public void setToolTipText(final String text) {
        // Nothing to show here ...

    }
}
