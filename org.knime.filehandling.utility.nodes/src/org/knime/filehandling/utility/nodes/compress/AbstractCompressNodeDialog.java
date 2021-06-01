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
 *   27 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DefaultWriterStatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.truncator.TruncationPanel;

/**
 * Node Dialog for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> an {@link AbstractCompressNodeConfig} instance
 */
public abstract class AbstractCompressNodeDialog<T extends AbstractCompressNodeConfig<?>> extends NodeDialogPane {

    /** The archive option title. */
    protected static final String ARCHIVE_OPTION_TITLE = "Archive entry path";

    private static final String FILE_OUTPUT_HISTORY_ID = "compress_output_files_history";

    private static final Pattern PATTERN = Pattern.compile(//
        Arrays.stream(AbstractCompressNodeConfig.COMPRESSIONS)//
            .map(s -> "\\." + s)//
            .collect(Collectors.joining("|", "(", ")\\s*$")),
        Pattern.CASE_INSENSITIVE);

    private final DialogComponentWriterFileChooser m_destinationFileChooserPanel;

    private final DialogComponentStringSelection m_compressionSelection;

    private final TruncationPanel m_truncationPanel;

    private final DialogComponentBoolean m_includeEmptyFolders;

    private final T m_config;

    private boolean m_isLoading;

    private final DialogComponentButtonGroup m_compressPolicy;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param config the configuration
     * @param truncationPanel the truncation panel
     */
    protected AbstractCompressNodeDialog(final PortsConfiguration portsConfig, final T config,
        final TruncationPanel truncationPanel) {
        m_config = config;

        final SettingsModelWriterFileChooser destinationFileChooserModel = m_config.getTargetFileChooserModel();

        m_includeEmptyFolders = new DialogComponentBoolean(config.includeEmptyFoldersModel(), "Include empty folders");

        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserModel.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);

        m_destinationFileChooserPanel =
            new DialogComponentWriterFileChooser(destinationFileChooserModel, FILE_OUTPUT_HISTORY_ID, writeFvm,
                fileChooserModel -> new DefaultWriterStatusMessageReporter(fileChooserModel,
                    m_config.getCompressPolicy().getFileOverwritePolicy()));

        m_truncationPanel = truncationPanel;

        final SettingsModelString compressionModel = m_config.getCompressionModel();
        m_compressionSelection = new DialogComponentStringSelection(compressionModel, "Compression",
            Arrays.asList(AbstractCompressNodeConfig.COMPRESSIONS));
        compressionModel.addChangeListener(l -> updateLocation());

        m_compressPolicy =
            new DialogComponentButtonGroup(m_config.getCompressPolicyModel(), null, false, CompressPolicy.values());
        m_compressPolicy.getModel().addChangeListener(l -> updateDestinationStatusMessage());

        m_isLoading = false;
    }

    @Override
    public final FlowVariableModel createFlowVariableModel(final String[] keys, final VariableType<?> type) {
        return super.createFlowVariableModel(keys, type);
    }

    /**
     * Returns the config.
     *
     * @return the config
     */
    protected final T getConfig() {
        return m_config;
    }

    /**
     * Enables the empty folder dialog.
     *
     * @param enabled {@code true} if the empty folder dialog should be enabled, false otherwise
     */
    protected final void enableEmptyFolderDialog(final boolean enabled) {
        m_includeEmptyFolders.getModel().setEnabled(enabled);
    }

    /**
     * Creates the settings tab.
     */
    protected final void createSettingsTab() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().fillHorizontal().setWeightX(1).anchorLineStart();
        panel.add(createInputPanel(), gbc.build());

        gbc.incY();
        panel.add(createOutputPanel(), gbc.build());

        gbc.incY();
        panel.add(m_truncationPanel.getPanel(), gbc.build());

        gbc.incY().fillHorizontal().setWeightX(1);
        panel.add(createOptionsPanel(), gbc.build());

        gbc.incY().setWeightY(1).setWeightX(1).fillBoth();
        panel.add(new JPanel(), gbc.build());

        addTab("Settings", panel);
    }

    /**
     * Creates the input panel, i.e., the panel containing the the location the files/folders to be compressed originate
     * from.
     *
     * @return the panel containing all elements required to specify the files/folders to compress
     */
    protected abstract JPanel createInputPanel();

    /**
     * Returns whether or not the dialog is loading the settings.
     *
     * @return {@code true} if the dialog is still loading the settings and {@code false} otherwise
     */
    protected final boolean isLoading() {
        return m_isLoading;
    }

    private JPanel createOutputPanel() {
        final JPanel outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));

        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();

        outputPanel.add(m_destinationFileChooserPanel.getComponentPanel(),
            gbc.resetX().incY().fillHorizontal().setWeightX(1).build());

        outputPanel.add(createCompressPolicyPanel(), gbc.resetX().incY().fillHorizontal().setWeightX(1).build());

        return outputPanel;
    }

    private JPanel createCompressPolicyPanel() {
        final JPanel compressPolicyPanel = new JPanel(new GridBagLayout());

        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();

        compressPolicyPanel.add(new JLabel("If exists:"), gbc.anchorLineStart().build());
        compressPolicyPanel.add(m_compressPolicy.getComponentPanel(), gbc.incX().insetLeft(21).build());
        compressPolicyPanel.add(new JPanel(), gbc.incX().setWeightX(1.0).fillHorizontal().build());

        return compressPolicyPanel;
    }

    private JPanel createOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Archive options"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_compressionSelection.getComponentPanel(), gbc.build());
        panel.add(m_includeEmptyFolders.getComponentPanel(), gbc.incY().build());
        panel.add(new JPanel(), gbc.insetTop(0).resetX().setHeight(1).incY().setWeightX(1).fillHorizontal().build());
        return panel;
    }

    private void updateLocation() {
        final String compression = "." + ((SettingsModelString)m_compressionSelection.getModel()).getStringValue();
        SettingsModelWriterFileChooser writerModel = m_destinationFileChooserPanel.getSettingsModel();
        if (!isLoading() && !writerModel.isOverwrittenByFlowVariable()) {
            FSLocation location = writerModel.getLocation();
            final String locPath = location.getPath();
            final String newPath = PATTERN.matcher(locPath).replaceAll(compression);
            if (!newPath.equals(locPath)) {
                writerModel.setLocation(
                    new FSLocation(location.getFSCategory(), location.getFileSystemSpecifier().orElse(null), newPath));
            }
        }
        writerModel.setFileExtensions(compression);
    }

    private void updateDestinationStatusMessage() {
        if (!isLoading()) {
            m_destinationFileChooserPanel.updateComponent();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_destinationFileChooserPanel.saveSettingsTo(settings);
        m_compressPolicy.saveSettingsTo(settings);
        m_truncationPanel.saveSettingsTo(settings);
        m_compressionSelection.saveSettingsTo(settings);
        m_includeEmptyFolders.saveSettingsTo(settings);
    }

    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_isLoading = true;
        loadSettings(settings, specs);
        m_isLoading = false;
        afterLoad();
    }

    /**
     * Invoked after loading the settings.
     */
    protected void afterLoad() {
        updateLocation();
    }

    /**
     * Loads the settings.
     *
     * @param settings the settings
     * @param specs the specs
     * @throws NotConfigurableException - If something goes wrong while loading the settings
     */
    protected void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_compressPolicy.loadSettingsFrom(settings, specs);
        m_destinationFileChooserPanel.loadSettingsFrom(settings, specs);
        m_compressionSelection.loadSettingsFrom(settings, specs);
        m_truncationPanel.loadSettings(settings, specs);
        m_includeEmptyFolders.loadSettingsFrom(settings, specs);
    }

    /**
     * Cancels any running swing worker when the dialog is closing.
     */
    @Override
    public void onClose() {
        m_destinationFileChooserPanel.onClose();
    }
}
