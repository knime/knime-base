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

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncatePathOption;

/**
 * Node Dialog for the "Compress Files/Folder" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> an {@link AbstractCompressNodeConfig} instance
 */
public abstract class AbstractCompressNodeDialog<T extends AbstractCompressNodeConfig> extends NodeDialogPane {

    private static final String FILE_OUTPUT_HISTORY_ID = "compress_output_files_history";

    private static final Pattern PATTERN = Pattern.compile(//
        Arrays.stream(AbstractCompressNodeConfig.COMPRESSIONS)//
            .map(s -> "\\." + s)//
            .collect(Collectors.joining("|", "(", ")\\s*$")),
        Pattern.CASE_INSENSITIVE);

    private final DialogComponentWriterFileChooser m_destinationFileChooserPanel;

    private final DialogComponentStringSelection m_compressionSelection;

    private final JCheckBox m_includeEmptyFolders;

    private final JCheckBox m_flattenHierarchyPanel;

    private final T m_config;

    private final EnumMap<TruncatePathOption, JRadioButton> m_truncateMap;

    private final DialogComponentString m_truncateRegex;

    private boolean m_isLoading;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param config the configuration
     */
    protected AbstractCompressNodeDialog(final PortsConfiguration portsConfig, final T config) {
        m_config = config;

        final SettingsModelWriterFileChooser destinationFileChooserModel = m_config.getTargetFileChooserModel();

        m_includeEmptyFolders = new JCheckBox("Include empty folders");
        m_flattenHierarchyPanel = new JCheckBox("Flatten folder");

        final FlowVariableModel writeFvm = createFlowVariableModel(destinationFileChooserModel.getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);
        m_destinationFileChooserPanel =
            new DialogComponentWriterFileChooser(destinationFileChooserModel, FILE_OUTPUT_HISTORY_ID, writeFvm);

        SettingsModelString compressionModel = m_config.getCompressionModel();
        m_compressionSelection = new DialogComponentStringSelection(compressionModel, "Format",
            Arrays.asList(AbstractCompressNodeConfig.COMPRESSIONS));
        compressionModel.addChangeListener(l -> updateLocation());

        m_truncateMap = new EnumMap<>(TruncatePathOption.class);
        final ButtonGroup grp = new ButtonGroup();
        for (final TruncatePathOption opt : TruncatePathOption.values()) {
            final JRadioButton btn = new JRadioButton(opt.getLabel());
            btn.addActionListener(l -> toggleRegexField());
            m_truncateMap.put(opt, btn);
            grp.add(btn);
        }

        m_truncateRegex = new DialogComponentString(m_config.getTruncateRegexModel(), null, true, 15);

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
     * Returns the include empty folders check box.
     *
     * @return the include empty folders check box.
     */
    protected final JCheckBox getIncludeEmptyFoldersCheckBox() {
        return m_includeEmptyFolders;
    }

    /**
     * Returns the flatten hierarchy check box.
     *
     * @return the flatten hierarchy folders check box.
     */
    protected final JCheckBox getFlattenHierarchyCheckBox() {
        return m_flattenHierarchyPanel;
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

        gbc.incY().fillHorizontal().setWeightX(1);
        panel.add(createOptionsPanel(), gbc.build());

        gbc.incY().setWeightY(1).setWeightX(1).fillBoth();
        panel.add(new JPanel(), gbc.build());

        addTab("settings", panel);
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
    protected boolean isLoading() {
        return m_isLoading;
    }

    private JPanel createOutputPanel() {
        final JPanel outputPanel = new JPanel(new GridBagLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Destination"));

        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();

        outputPanel.add(m_destinationFileChooserPanel.getComponentPanel(),
            gbc.resetX().incY().fillHorizontal().setWeightX(1).build());

        return outputPanel;
    }

    private JPanel createOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Archive options"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_compressionSelection.getComponentPanel(), gbc.build());

        gbc.incY().setWeightX(1).fillHorizontal().insetTop(3);
        panel.add(createTruncatePanel(), gbc.build());
        panel.add(createContentPanel(), gbc.incY().build());
        panel.add(new JPanel(), gbc.insetTop(0).resetX().setHeight(1).incY().setWeightX(1).fillHorizontal().build());
        return panel;
    }

    private Component createTruncatePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source folder truncation"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();

        panel.add(m_truncateMap.get(TruncatePathOption.KEEP_FULL_PATH), gbc.build());
        panel.add(m_truncateMap.get(TruncatePathOption.KEEP_SRC_FOLDER), gbc.incX().build());
        panel.add(m_truncateMap.get(TruncatePathOption.TRUNCATE_SRC_FOLDER), gbc.resetX().incY().build());
        panel.add(getRegexPanel(), gbc.incX().build());
        panel.add(new JPanel(), gbc.setWeightX(1).setWidth(2).fillHorizontal().build());
        return panel;
    }

    private Component getRegexPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_truncateMap.get(TruncatePathOption.TRUNCATE_REGEX), gbc.build());
        panel.add(m_truncateRegex.getComponentPanel(), gbc.incX().build());
        return panel;
    }

    private Component createContentPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Source folder content"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();

        panel.add(m_flattenHierarchyPanel, gbc.build());
        gbc.incY();
        panel.add(m_includeEmptyFolders, gbc.build());
        panel.add(new JPanel(), gbc.setWeightX(1).incY().fillHorizontal().build());
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

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_destinationFileChooserPanel.saveSettingsTo(settings);
        m_compressionSelection.saveSettingsTo(settings);
        m_truncateRegex.saveSettingsTo(settings);
        m_config.setTrunacePathOption(getTruncateOption());
        m_config.includeEmptyFolders(m_includeEmptyFolders.isSelected());
        m_config.flattenHierarchy(m_flattenHierarchyPanel.isSelected());
        m_config.saveSettingsForDialog(settings);
    }

    private TruncatePathOption getTruncateOption() throws InvalidSettingsException {
        return m_truncateMap.entrySet().stream()//
            .filter(e -> e.getValue().isSelected())//
            .map(Entry::getKey)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("Please select one of the truncate options"));
    }

    private void selectTruncateOption(final TruncatePathOption truncatePathOption) {
        m_truncateMap.entrySet().stream()//
            .forEachOrdered(e -> e.getValue().setSelected(e.getKey() == truncatePathOption));
    }

    private void toggleRegexField() {
        m_truncateRegex.getModel().setEnabled(m_truncateMap.get(TruncatePathOption.TRUNCATE_REGEX).isSelected());
    }

    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_isLoading = true;
        loadSettings(settings, specs);
        selectTruncateOption(getConfig().getTruncatePathOption());
        m_isLoading = false;
        afterLoad();
    }

    /**
     * Invoked after loading the settings.
     */
    protected void afterLoad() {
        toggleRegexField();
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
        m_destinationFileChooserPanel.loadSettingsFrom(settings, specs);
        m_compressionSelection.loadSettingsFrom(settings, specs);

        m_config.loadSettingsForDialog(settings);
        m_truncateRegex.loadSettingsFrom(settings, specs);
        m_includeEmptyFolders.setSelected(m_config.includeEmptyFolders());
        m_flattenHierarchyPanel.setSelected(m_config.flattenHierarchy());
    }

    /**
     * Cancels any running swing worker when the dialog is closing.
     */
    @Override
    public void onClose() {
        m_destinationFileChooserPanel.onClose();
    }
}
