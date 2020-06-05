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
 *   May 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.local.LocalFileSystemBrowser;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FileSystemChooserUtils;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.DialogComponentFilterMode;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;
import org.knime.filehandling.core.util.CheckNodeContextUtil;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A dialog component for selecting files from a file system. It consists of:
 * <ul>
 * <li>A radio button bar for selecting the {@link FilterMode}.</li>
 * <li>A combo box for selecting the file system, and depending on the file system an additional component for further
 * specifications.</li>
 * <li>An editable combobox for specifying the path to the desired file as well as a browse button that opens a
 * {@link FileSystemBrowser} to select the desired file.</li>
 * <li>Depending on the selected {@link FilterMode} a button for specifying the filter options, as well as a checkbox
 * specifying whether subfolders should be included.</li>
 * <li>A status message label that displays errors, warnings and infos for the user.</li>
 * </ul>
 * </br>
 * If a file system is provided via a node input port, the file system combo box only contains the input file system and
 * the browse button can only be clicked if the corresponding file system connector node has established a connection to
 * the file system.</br>
 * In case of the mountpoint file system, the browse button is only clickable if the user is logged in to the selected
 * mountpoint.</br>
 * In case of the custom URL file system, the browse button is always disabled and the user has to provide the exact
 * path via the combobox.</br>
 * The file selection combo box stores previously selected paths in a history that can be accessed in the drop down.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DialogComponentFileChooser3 extends DialogComponent {

    private final DialogType m_dialogType;

    private final FileSystemChooser m_fsChooser;

    private final FileSelectionDialog m_fileSelection;

    private final DialogComponentFilterMode m_filterMode;

    private final JLabel m_fileSelectionLabel = new JLabel("File:");

    private final StatusView m_statusView = new StatusView(300);

    private final PriorityStatusConsumer m_statusConsumer = new PriorityStatusConsumer();

    private final FlowVariableModel m_locationFvm;

    private boolean m_replacedByFlowVar = false;

    private FSConnection m_connection = null;

    private StatusSwingWorker m_statusMessageWorker = null;

    /**
     * Constructor.
     *
     * @param model the {@link SettingsModelFileChooser3} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param dialogType the type of dialog i.e. open or save
     * @param locationFvm the {@link FlowVariableModel} for the location
     * @param filterModes the available {@link FilterMode FilterModes} (if a none are provided, the default filter mode
     *            from <b>model</b> is used)
     */
    public DialogComponentFileChooser3(final SettingsModelFileChooser3 model, final String historyID,
        final FileSystemBrowser.DialogType dialogType, final FlowVariableModel locationFvm,
        final FilterMode... filterModes) {
        super(model);
        m_dialogType = dialogType;
        m_locationFvm =
            CheckUtils.checkArgumentNotNull(locationFvm, "The location flow variable model must not be null.");
        Set<FilterMode> selectableFilterModes =
            Stream.concat(Stream.of(model.getFilterModeModel().getFilterMode()), Arrays.stream(filterModes)).distinct()
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FilterMode.class)));
        final Set<Choice> choices = EnumSet.allOf(Choice.class);
        if (!selectableFilterModes.contains(FilterMode.FILE)) {
            choices.remove(Choice.CUSTOM_URL_FS);
        }
        m_fsChooser = FileSystemChooserUtils.createFileSystemChooser(model.getFileSystemConfiguration(), choices);
        m_filterMode = new DialogComponentFilterMode(model.getFilterModeModel(), false,
            selectableFilterModes.toArray(new FilterMode[0]));
        Set<FileSystemBrowser.FileSelectionMode> supportedModes =
            model.getFileSystemConfiguration().getSupportedFileSelectionModes();
        m_fileSelection = new FileSelectionDialog(historyID, 10, new LocalFileSystemBrowser(), dialogType,
            supportedModes.iterator().next(), model.getFileExtensions());
        hookUpListeners();
        layout(selectableFilterModes.size() > 1);
    }

    private void hookUpListeners() {
        m_fileSelection.addListener(e -> handleFileSelectionChange());
        getModel().addChangeListener(e -> updateComponent());
        m_locationFvm.addChangeListener(e -> handleFlowVariableModelChange());
    }

    private void layout(final boolean displayFilterModes) {
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().fillHorizontal().setWeightX(1);
        panel.add(new JLabel(getFSLabel()), gbc.setWeightX(0).setWidth(1).build());
        panel.add(m_fsChooser.getPanel(), gbc.incX().fillNone().build());
        if (displayFilterModes) {
            panel.add(new JLabel("Mode:"), gbc.resetX().incY().insetLeft(-4).setWidth(1).build());
            panel.add(m_filterMode.getComponentPanel(), gbc.incX().widthRemainder().build());
            panel.add(m_filterMode.getFilterConfigPanel(), gbc.insetLeft(4).incY().build());
        }
        panel.add(m_fileSelectionLabel, gbc.setWidth(1).insetLeft(0).setWeightX(0).resetX().incY().build());
        panel.add(m_fileSelection.getPanel(), gbc.incX().fillHorizontal().setWeightX(1).setWidth(2).build());
        panel.add(new FlowVariableModelButton(m_locationFvm), gbc.incX(2).setWeightX(0).setWidth(1).build());
        addAdditionalComponents(panel, gbc.resetX().incY());
        panel.add(m_statusView.getLabel(), gbc.anchorLineStart().insetLeft(4).setX(1).widthRemainder().incY().build());
    }

    private String getFSLabel() {
        switch (m_dialogType) {
            case OPEN_DIALOG:
                return "Read from:";
            case SAVE_DIALOG:
                return "Write to:";
            default:
                throw new IllegalStateException("Unsupported DialogType: " + m_dialogType);

        }
    }

    /**
     * Hook for extending classes to add additional components to the dialog.</br>
     * The additional components are added below the file selection panel but above the status message.
     *
     * @param panel the dialog panel
     * @param gbc convenience builder for {@link GridBagConstraints}
     */
    protected void addAdditionalComponents(final JPanel panel, final GBCBuilder gbc) {
        // no additonal components to add
    }

    private void handleFlowVariableModelChange() {
        // FIXME when the flow variable is no longer present, the fvm still signals that the value is replaced which is wrong
        // and causes the components to stay disabled. A quick investigation did not find the bug,
        // so we will postpone this to a later bugfix in order to get this enhancement resolved
        m_replacedByFlowVar = m_locationFvm.isVariableReplacementEnabled();
        final Optional<FlowVariable> flowVar = m_locationFvm.getVariableValue();
        if (m_replacedByFlowVar && flowVar.isPresent()) {
            final FSLocation location = flowVar.get().getValue(FSLocationVariableType.INSTANCE);
            FileSystemConfiguration<FSLocationConfig> fsConfig = getSettingsModel().getFileSystemConfiguration();
            if (!fsConfig.hasFSPort()) {
                fsConfig.setLocationSpec(location);
            } else {
                // in the connected case only the path is overwritten
            }
            m_fileSelection.setSelected(location.getPath());
        }
        setEnabledComponents(!m_replacedByFlowVar);
    }

    private void handleFileSelectionChange() {
        final String selectedPath = m_fileSelection.getSelected();
        // triggers updateComponent
        getSettingsModel().getFileSystemConfiguration().getLocationConfig()
            .setPath(selectedPath == null ? "" : selectedPath);
    }

    @Override
    protected final void updateComponent() {
        final SettingsModelFileChooser3 sm = getSettingsModel();
        final FileSystemConfiguration<FSLocationConfig> config = sm.getFileSystemConfiguration();
        final FSLocationConfig locationConfig = config.getLocationConfig();
        final FSLocation location = locationConfig.getLocationSpec();

        m_fileSelection.setSelected(location.getPath());

        updateFilterMode();
        updateFileSelectionLabel(location);
        updateBrowser();
        // the file system chooser updates itself when its config changes

        updateStatus();
        handleFlowVariableModelChange();
    }

    /**
     * Hook for extending classes that allows to update additional components
     */
    protected void updateAdditionalComponents() {
        // no additional components to update
    }

    private void updateFilterMode() {
        final SettingsModelFileChooser3 sm = getSettingsModel();
        SettingsModelFilterMode filterModeModel = sm.getFilterModeModel();
        if (sm.getLocation().getFileSystemChoice() == Choice.CUSTOM_URL_FS) {
            filterModeModel.setFilterMode(FilterMode.FILE);
            filterModeModel.setEnabled(false);
        } else {
            filterModeModel.setEnabled(true);
        }
    }

    private void updateStatus() {
        m_statusConsumer.clear();
        getSettingsModel().report(m_statusConsumer);
        Optional<StatusMessage> status = m_statusConsumer.get();
        boolean isWarningOrError = false;
        if (status.isPresent()) {
            status.ifPresent(m_statusView::setStatus);
            MessageType type = status.get().getType();
            isWarningOrError = type == MessageType.ERROR || type == MessageType.WARNING;
        }
        // don't check file if there are warnings/errors or we are in a convenience fs on the server
        if (!isWarningOrError && !isRemoteJobView()) {
            triggerSwingWorker();
        } else if (!status.isPresent()) {
            m_statusView.clearStatus();
        } else {
            // there is a status message that needs to be displayed
        }
    }

    private static boolean isRemoteJobView() {
        return CheckNodeContextUtil.isRemoteWorkflowContext();
    }

    private boolean isConnectedFS() {
        return getSettingsModel().getFileSystemConfiguration().hasFSPort();
    }

    private void triggerSwingWorker() {
        if (m_statusMessageWorker != null) {
            m_statusMessageWorker.cancel(true);
        }
        try {
            m_statusMessageWorker = new StatusSwingWorker(getSettingsModel(), m_statusView::setStatus, m_dialogType);
            m_statusMessageWorker.execute();
        } catch (Exception ex) {
            NodeLogger.getLogger(DialogComponentFileChooser3.class)
                .error("An exception occurred while updating the status message.", ex);
            m_statusView.setStatus(new DefaultStatusMessage(MessageType.ERROR,
                "An error occurred while updating the status message: %s", ex.getMessage()));
        }
    }

    private void updateBrowser() {
        final SettingsModelFileChooser3 sm = getSettingsModel();
        final FSLocation location = sm.getLocation();
        if (m_connection != null) {
            m_connection.close();
        }
        if (location.getFileSystemChoice() != Choice.CUSTOM_URL_FS) {
            final Optional<FSConnection> connection = getConnection();
            final Optional<FileSystemBrowser> browser = connection.map(FSConnection::getFileSystemBrowser);
            // we can only browse if the file system connection is available
            if (!isRemoteJobView() && browser.isPresent()) {
                // the connection is present, otherwise browser couldn't be
                m_connection = connection.get();
                m_fileSelection.setEnableBrowsing(true);
                m_fileSelection.setFileExtensions(sm.getFileExtensions());
                m_fileSelection.setFileSystemBrowser(browser.get());
                m_fileSelection.setFileSelectionMode(getFileSelectionMode());
            } else {
                // if we are in the remote job view, we need to close the connection since we can't browse anyway
                connection.ifPresent(FSConnection::close);
                m_fileSelection.setEnableBrowsing(false);
            }
        } else {
            m_fileSelection.setEnableBrowsing(false);
        }
    }

    private Optional<FSConnection> getConnection() {
        Optional<FSConnection> connection;
        try {
            // FIXME throws exception for EXAMPLES server -> Ask Bj�rn what's up there
            connection = getSettingsModel().getConnection();
        } catch (Exception ex) {
            connection = Optional.empty();
        }
        return connection;
    }

    private FileSystemBrowser.FileSelectionMode getFileSelectionMode() {
        final FilterMode filterMode = getSettingsModel().getFilterModeModel().getFilterMode();
        return filterMode.getFileSelectionMode();
    }

    private String getFileSelectionLabel(final FSLocation location) {
        if (location.getFileSystemChoice() == Choice.CUSTOM_URL_FS) {
            return "URL:";
        }

        final FilterMode filterMode = getSettingsModel().getFilterModeModel().getFilterMode();
        if (filterMode == FilterMode.FILE) {
            return "File:";
        } else {
            return "Folder:";
        }
    }

    private void updateFileSelectionLabel(final FSLocation location) {
        m_fileSelectionLabel.setText(getFileSelectionLabel(location));
    }

    /**
     * Returns the {@link SettingsModelFileChooser3} of this dialog component.
     *
     * @return the {@link SettingsModelFileChooser3} of this dialog component
     */
    public SettingsModelFileChooser3 getSettingsModel() {
        return (SettingsModelFileChooser3)getModel();
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (m_statusMessageWorker != null) {
            m_statusMessageWorker.cancel(true);
            m_statusMessageWorker = null;
        }
        if (m_connection != null) {
            m_connection.close();
            m_connection = null;
        }
        getSettingsModel().getFileSystemConfiguration().validate();
        // checkStatus() only fails if the swing worker found an error,
        // otherwise the previous call should already have failed
        checkStatus();
        m_fileSelection.addCurrentSelectionToHistory();
    }

    private void checkStatus() throws InvalidSettingsException {
        final Optional<StatusMessage> status = m_statusView.getStatus();
        if (status.isPresent()) {
            final StatusMessage actualStatus = status.get();
            final MessageType type = actualStatus.getType();
            CheckUtils.checkSetting(type != MessageType.ERROR, actualStatus.getMessage());
        }
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing is checked here
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        final boolean actual = enabled && !m_replacedByFlowVar;
        m_fsChooser.setEnabled(actual);
        m_fileSelection.setEnabled(actual);
    }

    @Override
    public void setToolTipText(final String text) {
        m_fsChooser.setTooltip(text);
        m_fileSelection.setTooltip(text);
    }

}
