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
import java.util.function.Function;
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
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.FileSystemChooserUtils;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.DialogComponentFilterMode;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusSwingWorker;
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
 * The file selection combo box stores previously selected paths in a history that can be accessed in the drop
 * down.</br>
 * </br>
 *
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The actual type of the implementation of the AbstractSettingsModelFileChooser
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractDialogComponentFileChooser<T extends AbstractSettingsModelFileChooser<T>>
    extends DialogComponent {

    private static final String NOT_BROWSABLE_WARNING_TEMPLATE =
        "%s does not support listing/browsing of files. You can enter the path in the text box below.";

    private final DialogType m_dialogType;

    private final FileSystemChooser m_fsChooser;

    private final FileSelectionDialog m_fileSelection;

    private final DialogComponentFilterMode m_filterMode;

    private final JLabel m_fileSelectionLabel = new JLabel("File");

    private final StatusView m_statusView = new StatusView();

    private final StatusView m_notBrowsableWarning = new StatusView(false);

    private final PriorityStatusConsumer m_statusConsumer = new PriorityStatusConsumer();

    private final JLabel m_fsChooserLabel;

    private final JLabel m_modeLabel = new JLabel("Mode");

    private final FlowVariableModelButton m_locationFvmBtn;

    private final Function<T, StatusMessageReporter> m_statusMessageReporter;

    private StatusSwingWorker m_statusMessageWorker = null;

    /**
     * Constructor.</br>
     * </br>
     * In order to create the {@link FlowVariableModel}, prefix the key chain returned by
     * {@link AbstractSettingsModelFileChooser#getKeysForFSLocation()} with the path to the settings model within your
     * settings structure.</br>
     * Suppose your settings have the structure foo/bar/model, then you can create the FlowVariableModel as follows:
     *
     * <pre>
     * String[] keyChain =
     *     Stream.concat(Stream.of("foo", "bar"), Arrays.stream(model.getKeysForFSLocation())).toArray(String[]::new);
     *
     * FlowVariableModel fvm = createFlowVariableModel(keyChain, FSLocationVariableType.INSTANCE);
     * </pre>
     *
     * @param model the {@link AbstractSettingsModelFileChooser} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param dialogType the type of dialog i.e. open or save
     * @param fsChooserLabel label for the file system chooser
     * @param locationFvm the {@link FlowVariableModel} for the location
     * @param statusMessageReporter function to create a {@link StatusMessageReporter} used to update the status of this
     *            component
     */
    protected AbstractDialogComponentFileChooser(final T model, final String historyID,
        final FileSystemBrowser.DialogType dialogType, final String fsChooserLabel, final FlowVariableModel locationFvm,
        final Function<T, StatusMessageReporter> statusMessageReporter) {
        super(model);
        m_dialogType = dialogType;
        m_fsChooserLabel = new JLabel(fsChooserLabel);
        CheckUtils.checkArgumentNotNull(locationFvm, "The location flow variable model must not be null.");
        model.setLocationFlowVariableModel(locationFvm);
        m_locationFvmBtn = new FlowVariableModelButton(locationFvm);
        m_statusMessageReporter = statusMessageReporter;
        m_fsChooser = FileSystemChooserUtils.createFileSystemChooser(model.getFileSystemConfiguration());
        m_filterMode = new DialogComponentFilterMode(model.getFilterModeModel(), false);
        Set<FileSystemBrowser.FileSelectionMode> supportedModes =
            model.getFileSystemConfiguration().getSupportedFileSelectionModes();
        m_fileSelection = new FileSelectionDialog(historyID, 10, model::getConnection, dialogType,
            supportedModes.iterator().next(), model.getFileExtensions());
        hookUpListeners();
        layout(model.supportsMultipleFilterModes());
    }

    /**
     * Extracts the filter modes that will be selectable in the dialog.
     *
     * @param model the settings model (must not necessarily among the provided filter modes
     * @param filterModes the supported filter modes
     * @return the {@link Set} of selectable {@link FilterMode FilterModes}
     */
    protected static final Set<FilterMode> extractSelectableFilterModes(final AbstractSettingsModelFileChooser<?> model,
        final FilterMode... filterModes) {
        return Stream.concat(Stream.of(model.getFilterMode()), Arrays.stream(filterModes)).distinct()
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(FilterMode.class)));
    }

    /**
     * Checks if all file systems support at least one of the selectable filter modes.<br>
     *
     * @param model the settings model
     * @param selectableFilterModes the filter modes that are selectable in the dialog
     * @throws IllegalArgumentException if {@link FSCategory#CUSTOM_URL} is among the selectable file systems and
     *             {@link FilterMode#FILE} is not among the selectable filter modes
     */
    protected static final void checkFilterModesSupportedByAllFs(final AbstractSettingsModelFileChooser<?> model,
        final Set<FilterMode> selectableFilterModes) {
        if (model.getFileSystemConfiguration().getActiveFSCategories().contains(FSCategory.CUSTOM_URL)) {
            CheckUtils.checkArgument(selectableFilterModes.contains(FilterMode.FILE),
                "FilterMode.FILE must be among the selectable filter modes "
                    + "if FSCategory.CUSTOM_URL is an active file system.");
        }
    }

    /**
     * Returns the {@link DialogType}.
     *
     * @return the type of this dialog
     */
    public final DialogType getDialogType() {
        return m_dialogType;
    }

    private void hookUpListeners() {
        m_fileSelection.addListener(e -> handleFileSelectionChange());
        getModel().addChangeListener(e -> updateComponent());
    }

    private void layout(final boolean displayFilterModes) {
        final JPanel panel = getComponentPanel();
        panel.setLayout(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().fillHorizontal();
        panel.add(m_fsChooserLabel, gbc.setWeightX(0).setWidth(1).build());
        panel.add(m_fsChooser.getPanel(), gbc.incX().insetLeft(5).fillNone().build());
        gbc.fillHorizontal();
        if (displayFilterModes) {
            panel.add(m_modeLabel, gbc.incY().resetX().insetLeft(0).build());
            panel.add(createModePanel(), gbc.incX().setWeightX(1).widthRemainder().build());
        } else {
            panel.add(m_notBrowsableWarning.getLabel(),
                gbc.resetX().incX().incY().insetLeft(11).setWeightX(1).build());
        }
        panel.add(m_fileSelectionLabel, gbc.setWidth(1).insetLeft(0).setWeightX(0).resetX().incY().build());
        panel.add(m_fileSelection.getPanel(),
            gbc.incX().fillHorizontal().insetLeft(5).setWeightX(1).setWidth(2).build());
        panel.add(m_locationFvmBtn, gbc.incX(2).setWeightX(0).setWidth(1).build());
        addAdditionalComponents(panel, gbc.resetX().insetLeft(0).incY());
        panel.add(m_statusView.getLabel(),
            gbc.anchorLineStart().insetLeft(9).insetTop(4).insetBottom(4).setX(1).widthRemainder().incY().build());
    }

    private JPanel createModePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().fillHorizontal().setWeightX(0);
        panel.add(m_filterMode.getComponentPanel(), gbc.incX().build());
        panel.add(m_filterMode.getFilterConfigPanel(), gbc.incX().insetLeft(20).build());
        panel.add(m_notBrowsableWarning.getLabel(), gbc.incX().setWeightX(1).build());
        return panel;
    }

    /**
     * Hook for extending classes to add additional components to the dialog.</br>
     * The additional components are added below the file selection panel but above the status message.
     *
     * @param panel the dialog panel
     * @param gbc convenience builder for {@link GridBagConstraints}
     */
    protected void addAdditionalComponents(final JPanel panel, final GBCBuilder gbc) {
        // no additional components to add
    }

    private void handleFileSelectionChange() {
        final String selectedPath = m_fileSelection.getSelected();
        // triggers updateComponent
        getSettingsModel().setPath(selectedPath == null ? "" : selectedPath);
    }

    @Override
    public final void updateComponent() {
        final T sm = getSettingsModel();
        final FSLocation location = sm.getLocation();

        m_fileSelection.setSelected(location.getPath());

        updateFileSelectionLabel(location);
        updateBrowser();
        updateAdditionalComponents();
        updateStatus();
        setEnabledFlowVarSensitive(!sm.isLocationOverwrittenByFlowVariable());
        setEnabledComponents(sm.isEnabled());
    }

    /**
     * Hook for extending classes that allows to update additional components
     */
    protected void updateAdditionalComponents() {
        // no additional components to update
    }

    private void updateStatus() {
        m_statusView.clearStatus();
        m_statusConsumer.clear();
        final T sm = getSettingsModel();
        if (!sm.isEnabled()) {
            // Don't update the status message if the model is disabled
            return;
        }
        sm.report(m_statusConsumer);
        Optional<StatusMessage> status = m_statusConsumer.get();
        status.ifPresent(m_statusView::setStatus);
        // don't check file if there are warnings/errors or we are on the server or currently loading the settings
        if (sm.isLocationValid() && !isRemoteJobView() && !sm.isLoading()) {
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

    private void triggerSwingWorker() {
        if (m_statusMessageWorker != null) {
            m_statusMessageWorker.cancel(true);
        }
        try {
            m_statusMessageWorker = new StatusSwingWorker(m_statusView::setStatus,
                m_statusMessageReporter.apply(getSettingsModel().createClone()), true);
            m_statusMessageWorker.execute();
        } catch (Exception ex) {//NOSONAR we want to communicate any exception
            NodeLogger.getLogger(AbstractDialogComponentFileChooser.class)
                .error("An exception occurred while updating the status message.", ex);
            m_statusView.setStatus(new DefaultStatusMessage(MessageType.ERROR,
                "An error occurred while updating the status message: %s", ex.getMessage()));
        }
    }

    private void updateBrowser() {
        final T sm = getSettingsModel();
        boolean isRemoteJobView = isRemoteJobView();
        if (!isRemoteJobView && sm.canBrowse()) {
            m_fileSelection.setEnableBrowsing(true);
            m_fileSelection.setFileExtensions(sm.getFileExtensions());
            m_fileSelection.setFSConnectionSupplier(sm::getConnection);
            m_fileSelection.setFileSelectionMode(getFileSelectionMode());
        } else {
            // if we are in the remote job view, we need to close the connection since we can't browse anyway
            m_fileSelection.setEnableBrowsing(false);
        }
        updateNotBrowsableWarning();
    }

    private void updateNotBrowsableWarning() {
        final T sm = getSettingsModel();
        if (!sm.canListFiles()) {
            // only show the warning if we can connect to the file system (otherwise we don't know if we can browse)
            final StatusMessage msg =
                DefaultStatusMessage.mkWarning(NOT_BROWSABLE_WARNING_TEMPLATE, sm.getFileSystemName());
            m_notBrowsableWarning.setStatus(msg);
        } else {
            m_notBrowsableWarning.clearStatus();
        }
    }

    private FileSystemBrowser.FileSelectionMode getFileSelectionMode() {
        final FilterMode filterMode = getSettingsModel().getFilterMode();
        return filterMode.getFileSelectionMode();
    }

    private String getFileSelectionLabel(final FSLocation location) {
        if (location.getFSCategory() == FSCategory.CUSTOM_URL) {
            return "URL";
        }

        final FilterMode filterMode = getSettingsModel().getFilterMode();
        if (filterMode == FilterMode.FILE) {
            return "File";
        } else {
            return "Folder";
        }
    }

    private void updateFileSelectionLabel(final FSLocation location) {
        m_fileSelectionLabel.setText(getFileSelectionLabel(location));
    }

    /**
     * Returns the {@link AbstractSettingsModelFileChooser} of this dialog component.
     *
     * @return the {@link AbstractSettingsModelFileChooser} of this dialog component
     */
    @SuppressWarnings("unchecked")
    public T getSettingsModel() {
        return (T)getModel();
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        cancelStatusSwingWorker();
        getSettingsModel().validateConfig();
        m_fileSelection.addCurrentSelectionToHistory();
    }

    /**
     * This method cancels the {@link StatusSwingWorker}. This method must be called in the onClose method of a dialog.
     */
    public void onClose() {
        cancelStatusSwingWorker();
    }

    /**
     * This method cancels the {@link StatusSwingWorker}.
     */
    private void cancelStatusSwingWorker() {
        if (m_statusMessageWorker != null) {
            m_statusMessageWorker.cancel(true);
            m_statusMessageWorker = null;
        }
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // TODO remove once we can check the filter mode compatibility when loading the settings model
        getSettingsModel().checkConfigurability(specs, m_filterMode.isSupported(FilterMode.FILE));
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        setEnabledFlowVarSensitive(enabled);
        m_fsChooserLabel.setEnabled(enabled);
        // the filter mode panel is updated via the settings model
        m_modeLabel.setEnabled(enabled);
        m_locationFvmBtn.setEnabled(enabled);
        m_statusView.getLabel().setEnabled(enabled);
        m_fileSelectionLabel.setEnabled(enabled);
    }

    private void setEnabledFlowVarSensitive(final boolean enabled) {
        final boolean actual =
            enabled && getSettingsModel().isEnabled() && !getSettingsModel().isLocationOverwrittenByFlowVariable();
        m_fsChooser.setEnabled(actual);
        m_fileSelection.setEnabled(actual);
    }

    @Override
    public void setToolTipText(final String text) {
        m_fsChooser.setTooltip(text);
        m_fileSelection.setTooltip(text);
        m_filterMode.setToolTipText(text);
    }

}
