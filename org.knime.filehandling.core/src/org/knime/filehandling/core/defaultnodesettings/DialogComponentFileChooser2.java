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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.core.node.util.LocalFileSystemBrowser;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystem;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemBrowser;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemProvider;
import org.knime.filehandling.core.connections.knime.KNIMEFileSystemView;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFileSystem;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFileSystemBrowser;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFileSystemProvider;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemoteFileSystemView;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2.FileOrFolderEnum;
import org.knime.filehandling.core.filechooser.NioFileSystemView;
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
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class DialogComponentFileChooser2 extends DialogComponent {

    /** Node logger */
    static final NodeLogger LOGGER = NodeLogger.getLogger(DialogComponentFileChooser2.class);

    /** Flow variable provider used to retrieve connection information to different file systems */
    private Optional<FSConnection> m_fs = Optional.empty();

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

    private final DialogComponentButtonGroup m_fileOrFolderButtonGroup;

    /** Check box to select whether to include sub folders while listing files in folders */
    private final JCheckBox m_includeSubfolders;

    /** Label for the {@code FilesHistoryPanel} */
    private final JLabel m_fileFolderLabel;

    /** FilesHistoryPanel used to select a file or a folder */
    private final FilesHistoryPanel m_fileHistoryPanel;

    /** Button to open the dialog that contains options for file filtering */
    private final JButton m_configureFilter;

    /** Panel for the filter options */
    private JPanel m_fileFilterOptionPanel;

    /** Panel containing options for file filtering */
    private final FileFilterPanel m_fileFilterConfigurationPanel;

    /** Extra dialog containing the file filter panel */
    private FileFilterDialog m_fileFilterDialog;

    /** Label containing status messages */
    private final JLabel m_statusMessage;

    /** Swing worker used to do file scanning in the background */
    private StatusMessageSwingWorker m_statusMessageSwingWorker;

    /** {@link DialogType} defining whether the component is used to read or write */
    private final DialogType m_dialogType;

    /** {@link DialogType} defining whether the component is allowed to select files, folders or both */
    private final FileSelectionMode m_fileSelectionMode;

    /** String used for file system connection label. */
    private static final String CONNECTION_LABEL_READ = "Read from: ";

    /** String used for file system connection label. */
    private static final String CONNECTION_LABEL_WRITE = "Write to: ";

    /** String used for the label next to the {@code FilesHistoryPanel} */
    private static final String FOLDER_LABEL = "Folder:";

    /** String used for the label next to the {@code FilesHistoryPanel} */
    private static final String FILE_LABEL = "File:";

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

    /** String used as button label */
    private static final String CONFIGURE_BUTTON_LABEL = "Filter options";

    /** Index of optional input port */
    private final int m_inPort;

    /** Timeout in milliseconds */
    private int m_timeoutInMillis = FileUtil.getDefaultURLTimeoutMillis();

    private final JPanel m_buttonGroupPanel;

    private boolean m_ignoreUpdates;

    /**
     * Creates a new instance of {@code DialogComponentFileChooser2}.
     *
     * @param inPort the index of the optional {@link FileSystemPortObject}
     * @param settingsModel the settings model storing all the necessary information
     * @param historyId id used to store file history used by {@link FilesHistoryPanel}
     * @param dialogType integer defining the dialog type (see {@link JFileChooser#OPEN_DIALOG},
     *            {@link JFileChooser#SAVE_DIALOG})
     * @param selectionMode integer defining the dialog type (see {@link JFileChooser#FILES_ONLY},
     *            {@link JFileChooser#FILES_AND_DIRECTORIES} and {@link JFileChooser#DIRECTORIES_ONLY}
     * @param fvm {@link FlowVariableModel} to use
     */
    public DialogComponentFileChooser2(final int inPort, final SettingsModelFileChooser2 settingsModel,
        final String historyId, final int dialogType, final int selectionMode, final FlowVariableModel fvm) {
        super(settingsModel);
        m_inPort = inPort;
        m_pathFlowVariableModel = fvm;

        m_connectionLabel =
            new JLabel(dialogType == JFileChooser.OPEN_DIALOG ? CONNECTION_LABEL_READ : CONNECTION_LABEL_WRITE);

        m_connections = new JComboBox<>(new FileSystemChoice[0]);
        m_connections.setEnabled(true);

        m_knimeConnections = new JComboBox<>();
        m_knimeConnections.setRenderer(new KNIMEConnectionRenderer());

        m_fileFolderLabel = new JLabel(FILE_LABEL);

        m_dialogType = DialogType.fromJFileChooserCode(dialogType);
        m_fileSelectionMode = FileSelectionMode.fromJFileChooserCode(selectionMode);
        m_fileHistoryPanel = new FilesHistoryPanel(m_pathFlowVariableModel, historyId, new LocalFileSystemBrowser(),
            m_fileSelectionMode, m_dialogType, convertExtensions(settingsModel.getDefaultSuffixes()));

        m_fileOrFolderButtonGroup = new DialogComponentButtonGroup(settingsModel.getFileOrFolderSettingsModel(), null,
            false, SettingsModelFileChooser2.FileOrFolderEnum.values());
        m_buttonGroupPanel = new JPanel();
        m_buttonGroupPanel.add(m_fileOrFolderButtonGroup.getComponentPanel());

        m_includeSubfolders = new JCheckBox(INCLUDE_SUBFOLDERS_LABEL);
        m_configureFilter = new JButton(CONFIGURE_BUTTON_LABEL);
        m_configureFilter.addActionListener(e -> showFileFilterConfigurationDialog());
        m_fileFilterConfigurationPanel = new FileFilterPanel();

        initFilterOptionsPanel();
        setFileFolderLabel();
        setFileFilterPanelVisibility();

        m_statusMessage = new JLabel(EMPTY_STRING);

        m_statusMessageSwingWorker = null;

        initLayout();

        // Fill combo boxes
        addEventHandlers();

        updateComponent();
    }

    /** Converts the string array containing file extensions to a concatenated String used for the FilesHistoryPanel */
    private static final String convertExtensions(final String[] extensions) {
        final StringBuilder ext = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            final String extension = extensions[i];
            ext.append(extension.charAt(0) == '.' ? extension : "." + extension);
            if (i < extensions.length - 1) {
                ext.append("|");
            }
        }
        return ext.toString();
    }

    /** Initialize the layout of the dialog component */
    private final void initLayout() {
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(m_buttonGroupPanel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(m_connectionLabel, gbc);

        gbc.gridx++;
        panel.add(m_connections, gbc);

        gbc.gridx++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        initConnectionSettingsPanelLayout();
        panel.add(m_connectionSettingsPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(m_fileFolderLabel, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(m_fileHistoryPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(m_fileFilterOptionPanel, gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        panel.add(m_statusMessage, gbc);
    }

    private final void initFilterOptionsPanel() {
        m_fileFilterOptionPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        m_fileFilterOptionPanel.add(m_configureFilter, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        m_fileFilterOptionPanel.add(m_includeSubfolders, gbc);
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
        m_fileOrFolderButtonGroup.getModel().addChangeListener(e -> handleFileOrFolderButtonUpdate());

        m_connections.addActionListener(e -> handleConnectionsChoiceUpdate());

        m_knimeConnections.addActionListener(e -> handleKnimeConnectionUpdate());
        m_fileHistoryPanel.addChangeListener(e -> handleHistoryPanelUpdate());

        m_includeSubfolders.addActionListener(e -> handleIncludeSubfolderUpdate());
    }

    private void handleFileOrFolderButtonUpdate() {
        setFileFilterPanelVisibility();
        setFileFolderLabel();
        triggerStatusMessageUpdate();
    }

    private void setFileFolderLabel() {
        final SettingsModelString model = ((SettingsModelFileChooser2)getModel()).getFileOrFolderSettingsModel();
        m_fileFolderLabel.setText(
            FileOrFolderEnum.valueOf(model.getStringValue()).equals(FileOrFolderEnum.FILE) ? FILE_LABEL : FOLDER_LABEL);
    }

    private void handleHistoryPanelUpdate() {
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        model.setPathOrURL(m_fileHistoryPanel.getSelectedFile().trim());
        triggerStatusMessageUpdate();
    }

    private void handleConnectionsChoiceUpdate() {
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());
        if (fsChoice != null) {
            model.setFileSystem(fsChoice.getId());
            if (fsChoice.equals(FileSystemChoice.getKnimeFsChoice())
                || fsChoice.equals(FileSystemChoice.getKnimeMountpointChoice())) {
                updateKNIMEConnectionsCombo();
                handleKnimeConnectionUpdate();
            } else {
                updateEnabledness();
                updateFileHistoryPanel();
                triggerStatusMessageUpdate();
            }
        }
        setConnectionChoiceColor();
    }

    private void handleKnimeConnectionUpdate() {
        if (!m_ignoreUpdates) {
            final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
            final KNIMEConnection connection = (KNIMEConnection)m_knimeConnections.getModel().getSelectedItem();
            if (connection != null) {
                if (model.getFileSystemChoice().equals(FileSystemChoice.getKnimeFsChoice())) {
                    model.setKNIMEFileSystem(connection.getId());
                } else if (model.getFileSystemChoice().equals(FileSystemChoice.getKnimeMountpointChoice())) {
                    model.setKnimeMountpointFileSystem(connection.getId());
                }
                updateEnabledness();
                setKnimeConnectionColors();
                updateFileHistoryPanel();
                triggerStatusMessageUpdate();
            }
        }
    }

    private void handleIncludeSubfolderUpdate() {
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        model.setIncludeSubfolders(m_includeSubfolders.isEnabled() && m_includeSubfolders.isSelected());
        triggerStatusMessageUpdate();
    }

    private void handleFilterConfigurationPanelUpdate() {
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        model.setFilterSettings(m_fileFilterConfigurationPanel.getFileFilterSettings());
        triggerStatusMessageUpdate();
    }

    private void setFileFilterPanelVisibility() {
        final SettingsModelString model = ((SettingsModelFileChooser2)getModel()).getFileOrFolderSettingsModel();
        m_fileFilterOptionPanel.setVisible(model.isEnabled()
            && FileOrFolderEnum.valueOf(model.getStringValue()).equals(FileOrFolderEnum.FILE_IN_FOLDER));
    }

    private void updateFileHistoryPanel() {
        m_fileHistoryPanel.setEnabled(true);
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());

        switch (fsChoice.getType()) {
            case CUSTOM_URL_FS:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setEnabled(true);
                m_fileHistoryPanel.setBrowseable(false);
                break;
            case CONNECTED_FS:
                final Optional<FSConnection> fs =
                    FileSystemPortObjectSpec.getFileSystemConnection(getLastTableSpecs(), m_inPort);
                applySettingsForConnection(fsChoice, fs);
                break;
            case KNIME_FS:
                final KNIMEConnection knimeConnection = (KNIMEConnection)m_knimeConnections.getSelectedItem();

                try (FileSystem fileSystem = getFileSystem(knimeConnection)) {
                    final NioFileSystemView fsView = new KNIMEFileSystemView((KNIMEFileSystem)fileSystem);
                    m_fileHistoryPanel.setFileSystemBrowser(
                        new KNIMEFileSystemBrowser(fsView, ((KNIMEFileSystem)fileSystem).getBasePath()));
                    m_statusMessage.setText("");
                    m_fileHistoryPanel.setEnabled(true);
                    m_fileHistoryPanel.setBrowseable(true);
                } catch (final IOException ex) {
                    m_statusMessage.setForeground(Color.RED);
                    m_statusMessage
                        .setText("Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(ex, false));
                    m_fileHistoryPanel.setEnabled(true);
                    m_fileHistoryPanel.setBrowseable(false);
                    LOGGER.debug("Exception when creating or closing the file system:", ex);
                }

                break;
            case KNIME_MOUNTPOINT:
                final KNIMEConnection knimeMountConnection = (KNIMEConnection)m_knimeConnections.getSelectedItem();
                final String schemeAndHost = knimeMountConnection.getSchemeAndHost();
                final URI fsKey = URI.create(schemeAndHost);

                try {
                    @SuppressWarnings("resource")
                    final KNIMERemoteFileSystem fileSystem =
                        (KNIMERemoteFileSystem)KNIMERemoteFileSystemProvider.getInstance().getOrCreateFileSystem(fsKey);

                    final KNIMERemoteFileSystemView fsView = new KNIMERemoteFileSystemView(fileSystem);
                    final KNIMERemoteFileSystemBrowser fsBrowser = new KNIMERemoteFileSystemBrowser(fsView);

                    m_fileHistoryPanel.setFileSystemBrowser(fsBrowser);
                    m_statusMessage.setText("");
                    final boolean isReadable = MountPointIDProviderService.instance().isReadable(fsKey);
                    m_fileHistoryPanel.setEnabled(true);
                    m_fileHistoryPanel.setBrowseable(isReadable);
                } catch (final IOException ex) {
                    m_statusMessage.setForeground(Color.RED);
                    m_statusMessage
                        .setText("Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(ex, false));
                    m_fileHistoryPanel.setEnabled(true);
                    m_fileHistoryPanel.setBrowseable(false);
                    LOGGER.debug("Exception when creating or closing the file system:", ex);
                }
                break;
            case LOCAL_FS:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setBrowseable(true);
                m_fileHistoryPanel.setEnabled(true);
                break;
            default:
                m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
                m_fileHistoryPanel.setBrowseable(false);
                m_fileHistoryPanel.setEnabled(false);
        }

        if (excutedOnServer() && !fsChoice.getType().equals(Choice.CONNECTED_FS)) {
            m_fileHistoryPanel.setBrowseable(false);
            m_fileHistoryPanel.setEnabled(false);
            m_statusMessage.setForeground(Color.RED);
            m_statusMessage.setText("Browsing is not supported in job view.");
        }
        getComponentPanel().repaint();
    }

    private void applySettingsForConnection(final FileSystemChoice fsChoice, final Optional<FSConnection> fs) {
        if (fs.isPresent()) {
            m_fileHistoryPanel.setFileSystemBrowser(fs.get().getFileSystemBrowser());
            m_fileHistoryPanel.setBrowseable(true);
            m_fileHistoryPanel.setEnabled(true);
            m_statusMessage.setText("");
        } else {
            m_fileHistoryPanel.setFileSystemBrowser(new LocalFileSystemBrowser());
            m_fileHistoryPanel.setBrowseable(false);
            m_fileHistoryPanel.setEnabled(true);
            m_statusMessage.setForeground(Color.RED);
            m_statusMessage.setText(
                String.format("Connection to %s not available. Please execute the connector node.", fsChoice.getId()));
        }
    }

    private static FileSystem getFileSystem(final KNIMEConnection knimeConnection) throws IOException {
        final URI fsKey = URI.create(knimeConnection.getType().getSchemeAndHost());
        return KNIMEFileSystemProvider.getInstance().getOrCreateFileSystem(fsKey);
    }

    /** Method called if file filter configuration button is clicked */
    private void showFileFilterConfigurationDialog() {

        final Container c = getComponentPanel().getParent();
        Frame parentFrame = null;
        Container parent = getComponentPanel();
        while (parent != null) {
            if (parent instanceof Frame) {
                parentFrame = (Frame)parent;
                break;
            }
            parent = parent.getParent();
        }

        m_fileFilterDialog = new FileFilterDialog(parentFrame, m_fileFilterConfigurationPanel);
        m_fileFilterDialog.setLocationRelativeTo(c);
        m_fileFilterDialog.setVisible(true);

        if (m_fileFilterDialog.getResultStatus() == JOptionPane.OK_OPTION) {
            // updates the settings model
            handleFilterConfigurationPanelUpdate();

        } else {
            // overwrites the values in the file filter panel components with those
            // from the settings model
            final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
            m_fileFilterConfigurationPanel.setFileFilterSettings(model.getFileFilterSettings());
        }
    }

    /** Method to update enabledness of components */
    private void updateEnabledness() {
        final SettingsModelString fileOrFolderModel = (SettingsModelString)m_fileOrFolderButtonGroup.getModel();
        final boolean fileSelected =
            FileOrFolderEnum.valueOf(fileOrFolderModel.getStringValue()).equals(FileOrFolderEnum.FILE);
        if (excutedOnServer()
            && !((FileSystemChoice)m_connections.getSelectedItem()).getType().equals(Choice.CONNECTED_FS)) {
            //We are on the server, and do not have a connected connection
            //so we disable everything but the connection combobox

            fileOrFolderModel.setEnabled(false);
            m_knimeConnections.setEnabled((m_connections.getSelectedItem().equals(FileSystemChoice.getKnimeFsChoice())
                || m_connections.getSelectedItem().equals(FileSystemChoice.getKnimeMountpointChoice())));
            m_fileFolderLabel.setEnabled(false);

            m_includeSubfolders.setEnabled(false);
            m_configureFilter.setEnabled(false);
            m_fileHistoryPanel.setEnabled(false);

        } else if (m_connections.getSelectedItem().equals(FileSystemChoice.getKnimeFsChoice())
            || m_connections.getSelectedItem().equals(FileSystemChoice.getKnimeMountpointChoice())) {
            // KNIME connections are selected
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, KNIME_CARD_VIEW_IDENTIFIER);

            fileOrFolderModel.setEnabled(true);
            m_knimeConnections.setEnabled(true);
            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(fileSelected ? FILE_LABEL : FOLDER_LABEL);

            m_includeSubfolders.setEnabled(true);
            m_configureFilter.setEnabled(true);

        } else if (m_connections.getSelectedItem().equals(FileSystemChoice.getCustomFsUrlChoice())) {
            // Custom URLs are selected
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);

            fileOrFolderModel.setEnabled(false);
            m_knimeConnections.setEnabled(false);
            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(URL_LABEL);

            m_includeSubfolders.setEnabled(false);
            m_configureFilter.setEnabled(false);
        } else {
            // some remote connection is selected, or we are using the local FS
            m_connectionSettingsCardLayout.show(m_connectionSettingsPanel, DEFAULT_CARD_VIEW_IDENTIFIER);
            fileOrFolderModel.setEnabled(true);
            m_knimeConnections.setEnabled(false);
            m_fileFolderLabel.setEnabled(true);
            m_fileFolderLabel.setText(fileSelected ? FILE_LABEL : FOLDER_LABEL);

            m_includeSubfolders.setEnabled(true);
            m_configureFilter.setEnabled(true);
        }
        setFileFilterPanelVisibility();
        getComponentPanel().repaint();
    }

    /** Method to update the status message */
    private void triggerStatusMessageUpdate() {
        if (m_statusMessageSwingWorker != null) {
            m_statusMessageSwingWorker.cancel(true);
            m_statusMessageSwingWorker = null;
        }
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        final FileSystemChoice fsChoice = model.getFileSystemChoice();
        if (excutedOnServer() && !fsChoice.getType().equals(Choice.CONNECTED_FS)) {
            return;
        }

        if (model.getPathOrURL() != null) {
            try {
                final FileChooserHelper helper = new FileChooserHelper(m_fs, model.clone(), m_timeoutInMillis);
                final FileSelectionMode fileOrFolder =
                    model.readFilesFromFolder() ? FileSelectionMode.DIRECTORIES_ONLY : FileSelectionMode.FILES_ONLY;

                m_statusMessageSwingWorker =
                    new StatusMessageSwingWorker(helper, m_statusMessage, m_dialogType, fileOrFolder);
                m_statusMessageSwingWorker.execute();
            } catch (final Exception ex) {
                if (fsChoice != null && fsChoice.getType().equals(Choice.CONNECTED_FS) && !m_fs.isPresent()) {
                    m_statusMessage.setForeground(Color.RED);
                    m_statusMessage.setText(String.format(
                        "Connection to %s not available. Please execute the connector node.", fsChoice.getId()));
                } else {
                    m_statusMessage.setForeground(Color.RED);
                    m_statusMessage
                        .setText("Could not get file system: " + ExceptionUtil.getDeepestErrorMessage(ex, false));
                }
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

        m_fs = FileSystemPortObjectSpec.getFileSystemConnection(getLastTableSpecs(), m_inPort);
        m_connections.setRenderer(new FileSystemChoiceRenderer(m_fs));

        final boolean filesAndDirectories = m_fileSelectionMode.equals(FileSelectionMode.FILES_AND_DIRECTORIES);
        m_fileOrFolderButtonGroup.getModel().setEnabled(filesAndDirectories);
        m_buttonGroupPanel.setVisible(filesAndDirectories);

        // add any connected connections
        updateConnectedConnectionsCombo();

        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        final FileSystemChoice fileSystem = model.getFileSystemChoice();
        if ((fileSystem != null) && !fileSystem.equals(m_connections.getSelectedItem())) {
            m_connections.setSelectedItem(fileSystem);
        }

        if ((fileSystem != null) && fileSystem.equals(FileSystemChoice.getKnimeFsChoice())) {
            final String knimeFileSystem = model.getKNIMEFileSystem();
            if (knimeFileSystem != null
                && !knimeFileSystem.equalsIgnoreCase(((KNIMEConnection)m_knimeConnections.getSelectedItem()).getId())) {
                m_knimeConnections.setSelectedItem(knimeFileSystem);
            }
        } else if ((fileSystem != null) && fileSystem.equals(FileSystemChoice.getKnimeFsChoice())) {
            final String knimeMountpoint = model.getKNIMEFileSystem();
            if (knimeMountpoint != null
                && !knimeMountpoint.equalsIgnoreCase(((KNIMEConnection)m_knimeConnections.getSelectedItem()).getId())) {
                m_knimeConnections.setSelectedItem(knimeMountpoint);
            }
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

        m_fileFilterConfigurationPanel.setFileFilterSettings(model.getFileFilterSettings());

        setEnabledComponents(model.isEnabled());
        updateFileHistoryPanel();
        triggerStatusMessageUpdate();
    }

    /** Method to update and add default file system connections to combo box */
    private void updateConnectionsCombo() {
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();
        if (connectionsModel.getSize() > 0) {
            while (connectionsModel.getSize() > 4) {
                connectionsModel.removeElementAt(0);
            }
        } else {
            FileSystemChoice.getDefaultChoices().stream().forEach(connectionsModel::addElement);
        }

        if (connectionsModel.getSelectedItem().equals(FileSystemChoice.getKnimeFsChoice())
            || connectionsModel.getSelectedItem().equals(FileSystemChoice.getKnimeMountpointChoice())) {
            updateKNIMEConnectionsCombo();
        }
        updateEnabledness();
    }

    /** Method to update and add connected file system connections to combo box */
    private void updateConnectedConnectionsCombo() {
        final DefaultComboBoxModel<FileSystemChoice> connectionsModel =
            (DefaultComboBoxModel<FileSystemChoice>)m_connections.getModel();

        if (getLastTableSpecs() != null && getLastTableSpecs().length > m_inPort) {
            final FileSystemPortObjectSpec fspos = (FileSystemPortObjectSpec)getLastTableSpec(m_inPort);
            if (fspos != null) {
                final FileSystemChoice choice =
                    FileSystemChoice.createConnectedFileSystemChoice(fspos.getFileSystemType());
                if (connectionsModel.getIndexOf(choice) < 0) {
                    connectionsModel.insertElementAt(choice, 0);
                }
            }
        } else {
            updateConnectionsCombo();
        }
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        connectionsModel.setSelectedItem(model.getFileSystemChoice());
        triggerStatusMessageUpdate();
    }

    /** Method to update and add KNIME file system connections to combo box */
    private void updateKNIMEConnectionsCombo() {
        m_ignoreUpdates = true;
        final DefaultComboBoxModel<KNIMEConnection> knimeConnectionsModel =
            (DefaultComboBoxModel<KNIMEConnection>)m_knimeConnections.getModel();
        final FileSystemChoice fsChoice = ((FileSystemChoice)m_connections.getSelectedItem());
        knimeConnectionsModel.removeAllElements();
        final SettingsModelFileChooser2 model = (SettingsModelFileChooser2)getModel();
        if (fsChoice.equals(FileSystemChoice.getKnimeMountpointChoice())) {
            MountPointIDProviderService.instance().getAllMountedIDs().stream().forEach(
                id -> knimeConnectionsModel.addElement(KNIMEConnection.getOrCreateMountpointAbsoluteConnection(id)));
            final String modelMountpointValue = model.getKnimeMountpointFileSystem();

            if (modelMountpointValue != null && !modelMountpointValue.isEmpty()) {
                knimeConnectionsModel.setSelectedItem(
                    KNIMEConnection.getOrCreateMountpointAbsoluteConnection(model.getKnimeMountpointFileSystem()));
            } else {
                knimeConnectionsModel.setSelectedItem(knimeConnectionsModel.getElementAt(0));
            }
            setKnimeConnectionColors();
        } else if (fsChoice.equals(FileSystemChoice.getKnimeFsChoice())) {
            knimeConnectionsModel.addElement(KNIMEConnection.MOUNTPOINT_RELATIVE_CONNECTION);
            knimeConnectionsModel.addElement(KNIMEConnection.WORKFLOW_RELATIVE_CONNECTION);
            knimeConnectionsModel.addElement(KNIMEConnection.NODE_RELATIVE_CONNECTION);

            knimeConnectionsModel.setSelectedItem(KNIMEConnection.getConnection(model.getKNIMEFileSystem()));
        }
        m_ignoreUpdates = false;
    }

    private void setKnimeConnectionColors() {
        m_knimeConnections.setForeground(KNIMEConnectionRenderer.getForegroundColor(
            (KNIMEConnection)m_knimeConnections.getSelectedItem(), getComponentPanel().getForeground()));
    }

    private void setConnectionChoiceColor() {
        final FileSystemChoice fsChoice = (FileSystemChoice)m_connections.getSelectedItem();
        m_connections.setForeground(
            FileSystemChoiceRenderer.getForegroundColor(fsChoice, getComponentPanel().getForeground(), m_fs));
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        m_fileHistoryPanel.addToHistory();
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
            m_configureFilter.setEnabled(enabled);
        }
    }

    @Override
    public void setToolTipText(final String text) {
        // Nothing to show here ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {

    }

    /**
     * Sets the file extension that is being used, if none exists for the selected file/folder.
     *
     * @param forcedExtension the file extension
     */
    public void setForceExtensionOnSave(final String forcedExtension) {
        m_fileHistoryPanel.setForceExtensionOnSave(forcedExtension);
    }

    /**
     * @return whether the dialog is opened on the server
     */
    protected boolean excutedOnServer() {

        return Optional.ofNullable(NodeContext.getContext())
            .map(nodeCtx -> nodeCtx.getContextObjectForClass(WorkflowManagerUI.class).orElse(null))
            .map(WorkflowManagerUI::getContext)
            .map(ctx -> ((ctx instanceof RemoteWorkflowContext) ? (RemoteWorkflowContext)ctx : null)).isPresent();

    }

}
