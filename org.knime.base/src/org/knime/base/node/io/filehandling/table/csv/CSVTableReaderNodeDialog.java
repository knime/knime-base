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
package org.knime.base.node.io.filehandling.table.csv;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.io.filehandling.table.csv.reader.CSVTableReaderConfig;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractDialogComponentFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;

/**
 * Node dialog of the CSV reader prototype node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class CSVTableReaderNodeDialog extends AbstractCSVTableReaderNodeDialog {

    private AbstractDialogComponentFileChooser m_filePanel;

    private JRadioButton m_failOnDifferingSpecs;

    private JRadioButton m_union;

    private JRadioButton m_intersection;

    CSVTableReaderNodeDialog(final SettingsModelReaderFileChooser fileChooserModel,
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final MultiTableReader<CSVTableReaderConfig, Class<?>, String> multiReader) {
        super(fileChooserModel, config, multiReader);
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
            "Spec merge options (multiple files)"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        specMergePanel.add(m_failOnDifferingSpecs, gbc);
        gbc.gridx = 1;
        specMergePanel.add(m_intersection, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        specMergePanel.add(m_union, gbc);
        return specMergePanel;
    }

    @Override
    protected void init(final PathSettings fileChooserModel) {
        final FlowVariableModel readFvm = createFlowVariableModel(
            ((SettingsModelReaderFileChooser)fileChooserModel).getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_filePanel = new DialogComponentReaderFileChooser((SettingsModelReaderFileChooser)fileChooserModel,
            "csv_reader_prototype", readFvm, FilterMode.FILE, FilterMode.FILES_IN_FOLDERS);

        m_failOnDifferingSpecs = new JRadioButton("Fail if specs differ");
        m_union = new JRadioButton("Union");
        m_intersection = new JRadioButton("Intersection");
        ButtonGroup specMergeGroup = new ButtonGroup();
        specMergeGroup.add(m_failOnDifferingSpecs);
        specMergeGroup.add(m_intersection);
        specMergeGroup.add(m_union);
    }

    @Override
    protected JPanel[] getPanels() {
        return new JPanel[]{createFilePanel(), createSpecMergePanel()};
    }

    @Override
    protected void registerPreviewChangeListeners() {
        super.registerPreviewChangeListeners();
        final ActionListener actionListener = l -> {
            if (validateFileSelection()) {
                m_tableReaderPreview.configChanged();
            }
        };
        m_failOnDifferingSpecs.addActionListener(actionListener);
        m_intersection.addActionListener(actionListener);
        m_union.addActionListener(actionListener);
        m_filePanel.getModel().addChangeListener(l -> {
            if (validateFileSelection()) {
                m_tableReaderPreview.configChanged();
            }
        });
    }

    @Override
    protected boolean validateFileSelection() {
        final PriorityStatusConsumer consumer = new PriorityStatusConsumer();
        m_filePanel.getSettingsModel().report(consumer);
        final Optional<StatusMessage> statusMsg = consumer.get();
        return !statusMsg.isPresent() || statusMsg.get().getType() == MessageType.INFO;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void loadAdditionalSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_filePanel.loadSettingsFrom(settings, specs);
        setSpecMergeMode();
    }

    @Override
    protected void setAutodetectComponentsEnabled(final boolean enabled) {
        super.setAutodetectComponentsEnabled(enabled);
        m_filePanel.getModel().setEnabled(enabled);
    }

    @Override
    protected void saveConfig() throws InvalidSettingsException {
        super.saveConfig();
        m_config.setSpecMergeMode(getSpecMergeMode());
    }

    /**
     * get the selected {@link SpecMergeMode} from the dialog.
     *
     * @return the selected {@link SpecMergeMode}
     * @throws InvalidSettingsException
     */
    private SpecMergeMode getSpecMergeMode() throws InvalidSettingsException {
        if (m_failOnDifferingSpecs.isSelected()) {
            return SpecMergeMode.FAIL_ON_DIFFERING_SPECS;
        } else if (m_intersection.isSelected()) {
            return SpecMergeMode.INTERSECTION;
        } else if (m_union.isSelected()) {
            return SpecMergeMode.UNION;
        } else {
            throw new InvalidSettingsException("No spec merge mode selected!");
        }
    }

    /**
     * sets the Spec merge options in the dialog.
     *
     * @throws NotConfigurableException
     */
    private void setSpecMergeMode() throws NotConfigurableException {
        switch (m_config.getSpecMergeMode()) {
            case FAIL_ON_DIFFERING_SPECS:
                m_failOnDifferingSpecs.setSelected(true);
                break;
            case INTERSECTION:
                m_intersection.setSelected(true);
                break;
            case UNION:
                m_union.setSelected(true);
                break;
            default:
                throw new NotConfigurableException("Unknown spec merge mode " + m_config.getSpecMergeMode());

        }
    }

    SettingsModelFileChooser2 getFileChooserSettingsModel() {
        return ((SettingsModelFileChooser2)m_filePanel.getModel()).clone();
    }

}
