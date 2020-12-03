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
 *   May 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Node dialog of the CSV reader prototype node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class CSVTableReaderNodeDialog extends AbstractCSVTableReaderNodeDialog {

    private DialogComponentReaderFileChooser m_filePanel;

    private JCheckBox m_failOnDifferingSpecs;

    CSVTableReaderNodeDialog(final SettingsModelReaderFileChooser fileChooserModel,
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final MultiTableReadFactory<Path, CSVTableReaderConfig, Class<?>> multiReader,
        final ProductionPathProvider<Class<?>> productionPathProvider) {
        super(fileChooserModel, config, multiReader, productionPathProvider, true);
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_filePanel.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_filePanel.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    private JPanel createSpecMergePanel() {
        final JPanel specMergePanel = new JPanel(new GridBagLayout());
        specMergePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Options for multiple files"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        specMergePanel.add(m_failOnDifferingSpecs, gbc);
        ++gbc.gridx;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        specMergePanel.add(Box.createVerticalBox(), gbc);
        return specMergePanel;
    }

    @Override
    protected void init(final PathSettings fileChooserModel) {
        final SettingsModelReaderFileChooser sm = (SettingsModelReaderFileChooser)fileChooserModel;
        final String[] keyChain =
            Stream.concat(Stream.of("settings"), Arrays.stream(sm.getKeysForFSLocation())).toArray(String[]::new);
        final FlowVariableModel readFvm = createFlowVariableModel(keyChain, FSLocationVariableType.INSTANCE);
        m_filePanel = new DialogComponentReaderFileChooser((SettingsModelReaderFileChooser)fileChooserModel,
            "csv_reader_writer", readFvm, FilterMode.FILE, FilterMode.FILES_IN_FOLDERS);
        sm.addChangeListener(e -> setReadingMultipleFiles(sm.getFilterMode() != FilterMode.FILE));

        m_failOnDifferingSpecs = new JCheckBox("Fail if specs differ");
        m_filePanel.getSettingsModel().getFilterModeModel().addChangeListener(l -> toggleFailOnDifferingCheckBox());
    }

    private void toggleFailOnDifferingCheckBox() {
        final boolean enable = m_filePanel.getSettingsModel().getFilterMode() != FilterMode.FILE;
        m_failOnDifferingSpecs.setEnabled(enable);
    }

    @Override
    protected JPanel[] getPanels() {
        return new JPanel[]{createFilePanel()};
    }

    @Override
    protected JPanel[] getAdvancedPanels() {
        return new JPanel[]{createSpecMergePanel()};
    }

    @Override
    protected void registerPreviewChangeListeners() {
        super.registerPreviewChangeListeners();
        final ActionListener actionListener = l -> configChanged();

        m_failOnDifferingSpecs.addActionListener(actionListener);
        m_filePanel.getModel().addChangeListener(l -> configChanged());
    }

    @Override
    protected boolean validateFileSelection() {
        return m_filePanel.getSettingsModel().isLocationValid();
    }

    @Override
    protected void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // FIXME: saving of the file panel should be handled by the config (AP-14460 & AP-14462)
        m_filePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
    }

    @Override
    protected void loadAdditionalSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // FIXME: loading should be handled by the config (AP-14460 & AP-14462)
        m_filePanel.loadSettingsFrom(SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB), specs);
        m_failOnDifferingSpecs.setSelected(m_config.failOnDifferingSpecs());
        toggleFailOnDifferingCheckBox();
    }

    @Override
    protected void setAutodetectComponentsEnabled(final boolean enabled) {
        super.setAutodetectComponentsEnabled(enabled);
        m_filePanel.getModel().setEnabled(enabled);
    }

    @Override
    protected void saveConfig() throws InvalidSettingsException {
        super.saveConfig();
        m_config.setFailOnDifferingSpecs(m_failOnDifferingSpecs.isSelected());
    }

    SettingsModelFileChooser2 getFileChooserSettingsModel() {
        return ((SettingsModelFileChooser2)m_filePanel.getModel()).clone();
    }

    @Override
    public void onClose() {
        super.onClose();
        m_filePanel.onClose();
    }

}
