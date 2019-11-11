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
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystem;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemBrowser;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemProvider;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemView;
import org.knime.filehandling.core.filechooser.NioFileSystemView;
import org.knime.filehandling.core.filefilter.FileFilter.FilterType;
import org.knime.filehandling.core.filefilter.FileFilterDialog;
import org.knime.filehandling.core.filefilter.FileFilterPanel;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.MountPointIDProviderService;

/**
 * Dialog component that allows selecting a file or multiple files in a folder. It provides the possibility to connect
 * to different file systems, as well as file filtering based on file extensions, regular expressions or wildcard.
 *
 * @author Bjoern Lohrmann, KNIME GmbH, Berlin, Germany
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
public class DialogComponentFileChooser2 extends DialogComponent {

    /** Node logger */
    static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentFileChooser2.class);

    /** Flow variable provider used to retrieve connection information to different file systems */
    private Optional<FSConnection> m_fs;

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

    /** Index of optional input port */
    private int m_inPort;

    /** Timeout in milliseconds */
    private int m_timeoutInMillis = FileUtil.getDefaultURLTimeoutMillis();

    /**
     * Creates a new instance of {@code DialogComponentFileChooser2}.
     *
     * @param inPort the index of the optional {@link FileSystemPortObject}
     * @param settingsModel the settings model storing all the necessary information
     * @param historyId id used to store file history used by {@link FilesHistoryPanel}
     * @param dialogPane the {@link NodeDialogPane} this component is used for
     * @param suffixes array of file suffixes used as defaults for file filtering options
     */
    public DialogComponentFileChooser2(final int inPort, final SettingsModelFileChooser2 settingsModel,
        final String historyId, final NodeDialogPane dialogPane, final String... suffixes) {
        super(settingsModel);
        m_inPort = inPort;

        m_ignoreUpdates = false;

        final Optional<String> legacyConfigKey = settingsModel.getLegacyConfigKey();
        m_pathFlowVariableModel =
            legacyConfigKey.isPresent() ? dialogPane.createFlowVariableModel(legacyConfigKey.get(), Type.STRING)
                : dialogPane.createFlowVariableModel(
                    new String[]{settingsModel.getConfigName(), SettingsModelFileChooser2.PATH_OR_URL_KEY},
                    Type.STRING);

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
        getModel().addChangeListener(e -> updateComponent());
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
        updateFileHistoryPanel();
        updateEnabledness();
    }

    /**
     *
     */
    private void updateFileHistoryPanel() {
        m_fileHistoryPanel.setEnabled(true);
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());

        switch (fsChoice.getType()) {
            case CUSTOM_URL_FS:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setBrowseable(false);
                break;
            case CONNECTED_FS:
                final Optional<FSConnection> fs =
                    FileSystemPortObjectSpec.getFileSystemConnection(getLastTableSpecs(), m_inPort);
                applySettingsForConnection(fsChoice, fs);
                break;
            case KNIME_FS:
                final KNIMEConnection knimeConnection = (KNIMEConnection)m_knimeConnections.getSelectedItem();
                if (knimeConnection.getType() == KNIMEConnection.Type.MOUNTPOINT_ABSOLUTE) {
                    // Leave like this until remote part is implemented.
                    m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                    m_statusMessage.setText("");
                    m_fileHistoryPanel.setBrowseable(true);
                } else {
                    try (FileSystem fileSystem = getFileSystem(knimeConnection)) {
                        NioFileSystemView fsView = new KNIMEFileSystemView((KNIMEFileSystem) fileSystem);
                        m_fileHistoryPanel.setFileSystemBrowser(
                            new KNIMEFileSystemBrowser(fsView, ((KNIMEFileSystem) fileSystem).getBasePath()));
                        m_statusMessage.setText("");
                        m_fileHistoryPanel.setBrowseable(true);
                    } catch (IOException ex) {
                        m_statusMessage.setForeground(Color.RED);
                        m_statusMessage.setText(
                            "Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(ex, false));
                        m_fileHistoryPanel.setBrowseable(false);
                        LOGGER.debug("Exception when creating or closing the file system:", ex);
                    }
                }
                break;
            case LOCAL_FS:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setBrowseable(true);
                break;
            default:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setBrowseable(false);
        }

    }

    private void applySettingsForConnection(final FileSystemChoice fsChoice, final Optional<FSConnection> fs) {
        if (fs.isPresent()) {
            m_fileHistoryPanel.setFileSystemBrowser(fs.get().getFileSystemBrowser());
            m_fileHistoryPanel.setBrowseable(true);
            m_statusMessage.setText("");
        } else {
            m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
            m_fileHistoryPanel.setBrowseable(false);
            m_statusMessage.setForeground(Color.RED);
            m_statusMessage.setText(
                String.format("Connection to %s not available. Please execute the connector node.", fsChoice.getId()));
        }
    }

    private static FileSystem getFileSystem(final KNIMEConnection knimeConnection) throws IOException {
        URI fsKey = URI.create(knimeConnection.getType().getSchemeAndHost());
        return KNIMEFileSystemProvider.getInstance().getOrCreateFileSystem(fsKey);
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
        final FilterType filterType = m_fileFilterPanel.getSelectedFilterType();
        final String filterExpression = m_fileFilterPanel.getSelectedFilterExpression();
        final boolean caseSensitive = m_fileFilterPanel.getCaseSensitive();

        m_fileFilterDialog.setLocationRelativeTo(c);
        m_fileFilterDialog.setVisible(true);

        if (m_fileFilterDialog.getResultStatus() == JOptionPane.OK_OPTION) {
            // updates the settings model, which in turn updates the UI
            updateSettingsModel();
            triggerStatusMessageUpdate();
        } else {
            // overwrites the values in the file filter panel components with those
            // from the settings model
            m_fileFilterPanel.setFilterType(filterType);
            m_fileFilterPanel.setFilterExpression(filterExpression);
            m_fileFilterPanel.setCaseSensitive(caseSensitive);
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

            m_includeSubfolders.setEnabled(true);
            m_filterFiles.setEnabled(true);
            m_configureFilter.setEnabled(m_filterFiles.isSelected());

        } else if (m_connections.getSelectedItem().equals(FileSystemChoice.getCustomFsUrlChoice())) {
            // Custom URLs are selected
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);

            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(URL_LABEL);

            m_includeSubfolders.setEnabled(false);
            m_filterFiles.setEnabled(false);
            m_configureFilter.setEnabled(false);
        } else {
            // some flow variable connection is selected, or we are using the local FS
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);

            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(FILE_FOLDER_LABEL);

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
            try {
                final FileChooserHelper helper =
                    new FileChooserHelper(m_fs, ((SettingsModelFileChooser2)getModel()).clone(), m_timeoutInMillis);
                m_statusMessageSwingWorker = new StatusMessageSwingWorker(helper, m_statusMessage);
                m_statusMessageSwingWorker.execute();
            } catch (Exception ex) {
                m_statusMessage.setForeground(Color.RED);
                m_statusMessage
                    .setText("Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(ex, false));
            }
        } else {
            m_statusMessage.setText("");
        }
    }

    /**
     * @param timeoutInMillis the timeout in milliseconds for the custom url file system
     */
    public void setTimeout(final int timeoutInMillis) {
        m_timeoutInMillis = timeoutInMillis;
    }

    @Override
    protected void updateComponent() {
        if (m_ignoreUpdates) {
            return;
        }

        m_fs = FileSystemPortObjectSpec.getFileSystemConnection(getLastTableSpecs(), m_inPort);

        m_ignoreUpdates = true;

        // add any connected connections
        updateConnectedConnectionsCombo();

        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();

        // sync connection combo box
        final FileSystemChoice fileSystem = model.getFileSystemChoice();
        if ((fileSystem != null) && !fileSystem.equals(m_connections.getSelectedItem())) {
            m_connections.setSelectedItem(fileSystem);
        }
        // sync knime connection check box
        final KNIMEConnection knimeFileSystem =
            KNIMEConnection.getOrCreateMountpointAbsoluteConnection(model.getKNIMEFileSystem());

        if ((knimeFileSystem != null) && !knimeFileSystem.equals(m_knimeConnections.getSelectedItem())) {
            final DefaultComboBoxModel<KNIMEConnection> knimeConnectionsModel =
                (DefaultComboBoxModel<KNIMEConnection>)m_knimeConnections.getModel();
            if (knimeConnectionsModel.getIndexOf(knimeFileSystem) == -1) {
                //If the Connection did not exsist before
                knimeConnectionsModel.addElement(knimeFileSystem);
            }
            m_knimeConnections.setSelectedItem(knimeFileSystem);
        }

        // sync file history panel
        final String pathOrUrl = model.getPathOrURL() != null ? model.getPathOrURL() : "";
        if (!pathOrUrl.equals(m_fileHistoryPanel.getSelectedFile())) {
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

        setEnabledComponents(model.isEnabled());
        updateFileHistoryPanel();
        triggerStatusMessageUpdate();
        m_ignoreUpdates = false;
    }

    /** Method to update and add default file system connections to combo box */
    private void updateConnectionsCombo() {
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();
        connectionsModel.removeAllElements();
        FileSystemChoice.getDefaultChoices().stream().forEach(c -> connectionsModel.addElement(c));
    }

    /** Method to update and add connected file system connections to combo box */
    private void updateConnectedConnectionsCombo() {
        updateConnectionsCombo();
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();
        if (getLastTableSpecs() != null && getLastTableSpecs().length > 0) {
            final FileSystemPortObjectSpec fspos = (FileSystemPortObjectSpec)getLastTableSpec(m_inPort);
            if (fspos != null) {
                final FileSystemChoice choice =
                    FileSystemChoice.createConnectedFileSystemChoice(fspos.getFileSystemType());
                if (connectionsModel.getIndexOf(choice) < 0) {
                    connectionsModel.insertElementAt(choice, 0);
                }
            }
        }
    }

    /** Method to update and add KNIME file system connections to combo box */
    private void updateKNIMEConnectionsCombo() {
        final DefaultComboBoxModel<KNIMEConnection> knimeConnectionsModel =
            (DefaultComboBoxModel<KNIMEConnection>)m_knimeConnections.getModel();
        knimeConnectionsModel.removeAllElements();
        MountPointIDProviderService.instance().getAllMountedIDs().stream().forEach(
            id -> knimeConnectionsModel.addElement(KNIMEConnection.getOrCreateMountpointAbsoluteConnection(id)));
        knimeConnectionsModel.addElement(KNIMEConnection.MOUNTPOINT_RELATIVE_CONNECTION);
        knimeConnectionsModel.addElement(KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION);
        knimeConnectionsModel.addElement(KNIMEConnection.NODE_RELATIVE_CONNECTION);

    }

    private void updateSettingsModel() {
        m_ignoreUpdates = true;
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());
        model.setFileSystem(fsChoice.getId());
        if (fsChoice.equals(FileSystemChoice.getKnimeFsChoice())) {
            final KNIMEConnection connection = (KNIMEConnection)m_knimeConnections.getModel().getSelectedItem();
            model.setKNIMEFileSystem(connection.getId());
        }
        model.setPathOrURL(m_fileHistoryPanel.getSelectedFile());
        model.setIncludeSubfolders(m_includeSubfolders.isEnabled() && m_includeSubfolders.isSelected());
        model.setFilterFiles(m_filterFiles.isEnabled() && m_filterFiles.isSelected());
        model.setFilterConditions(m_fileFilterPanel.getSelectedFilterType(),
            m_fileFilterPanel.getSelectedFilterExpression(), m_fileFilterPanel.getCaseSensitive());
        m_ignoreUpdates = false;
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateSettingsModel();
        //FIXME in case of KNIME connection check whether it is a valid connection
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
