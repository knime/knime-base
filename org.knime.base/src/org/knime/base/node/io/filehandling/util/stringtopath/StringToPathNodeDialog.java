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
 *   Jul 28, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.base.node.io.filehandling.util.stringtopath;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.util.stringtopath.StringToPathNodeModel.GenerateColumnMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.DialogComponentFileSystem;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;

/**
 * The NodeDialog for the "String to Path" Node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class StringToPathNodeDialog extends NodeDialogPane {

    private final SettingsModelString m_generatedColumnModeModel;

    private final SettingsModelBoolean m_abortOnMissingFileModel;

    private final DialogComponentFileSystem m_fileSystemComponent;

    private final DialogComponentColumnNameSelection m_selectedColumnNameComponent;

    private final DialogComponentButtonGroup m_generatedColumnModeComponent;

    private final DialogComponentString m_appendedColumnNameComponent;

    private final DialogComponentBoolean m_abortOnMissingFileComponent;

    private final DialogComponentBoolean m_failOnMissingValuesComponent;

    /**
     * Constructor for node dialog with dynamic ports.
     *
     * @param portsConfig the ports configuration
     */
    StringToPathNodeDialog(final PortsConfiguration portsConfig) {
        final SettingsModelFileSystem fileSystemModel =
            StringToPathNodeModel.createSettingsModelFileSystem(portsConfig);

        final FlowVariableModel fvm =
            createFlowVariableModel(fileSystemModel.getKeysForFSLocation(), FSLocationSpecVariableType.INSTANCE);
        m_fileSystemComponent = new DialogComponentFileSystem(fileSystemModel, fvm);

        m_selectedColumnNameComponent = pathCompatibleDialogComponentColumnNameSelection();

        final SettingsModelString appendedColumnNameModel =
            StringToPathNodeModel.createSettingsModelAppendedColumnName();
        m_appendedColumnNameComponent = new DialogComponentString(appendedColumnNameModel, "", true, 15);

        m_generatedColumnModeModel = StringToPathNodeModel.createSettingsModelColumnMode();
        m_generatedColumnModeComponent =
            new DialogComponentButtonGroup(m_generatedColumnModeModel, null, true, GenerateColumnMode.values());
        m_generatedColumnModeComponent.getModel().addChangeListener(e -> checkGeneratedColumnMode());

        m_abortOnMissingFileModel = StringToPathNodeModel.createSettingsModelAbortOnMissingFile();
        m_abortOnMissingFileComponent =
            new DialogComponentBoolean(m_abortOnMissingFileModel, "Fail if file/folder does not exist");

        m_failOnMissingValuesComponent = new DialogComponentBoolean(
            StringToPathNodeModel.createSettingsModelFailOnMissingValues(), "Fail on missing values");

        addTab("Settings", getOptionsPanel());
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    private JPanel getOptionsPanel() {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        optionsPanel.add(getFSConnectionPanel(), gbc);
        gbc.gridy++;
        optionsPanel.add(getColSelectionPanel(), gbc);
        gbc.gridy++;
        optionsPanel.add(getNewColumnPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        optionsPanel.add(Box.createVerticalBox(), gbc);

        return optionsPanel;
    }

    private JPanel getFSConnectionPanel() {
        final JPanel fSConnectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        fSConnectionPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File system: "));

        gbc.insets = new Insets(0, 7, 0, 0);
        fSConnectionPanel.add(m_fileSystemComponent.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        fSConnectionPanel.add(Box.createHorizontalBox(), gbc);
        return fSConnectionPanel;
    }

    private JPanel getColSelectionPanel() {
        final JPanel colSelectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        colSelectionPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column selection: "));

        gbc.insets = new Insets(5, 0, 0, 0);
        colSelectionPanel.add(m_selectedColumnNameComponent.getComponentPanel(), gbc);

        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.gridy++;
        colSelectionPanel.add(m_abortOnMissingFileComponent.getComponentPanel(), gbc);

        gbc.gridy++;
        colSelectionPanel.add(m_failOnMissingValuesComponent.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        colSelectionPanel.add(Box.createHorizontalBox(), gbc);

        return colSelectionPanel;
    }

    private JPanel getNewColumnPanel() {
        final JPanel newColumnPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        newColumnPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output: "));

        gbc.insets = new Insets(5, 5, 0, 0);
        newColumnPanel.add(m_generatedColumnModeComponent.getComponentPanel(), gbc);

        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.gridx++;
        newColumnPanel.add(m_appendedColumnNameComponent.getComponentPanel(), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        newColumnPanel.add(Box.createHorizontalBox(), gbc);

        return newColumnPanel;
    }

    private void checkGeneratedColumnMode() {
        m_appendedColumnNameComponent.getModel()
            .setEnabled(StringToPathNodeModel.isAppendMode(m_generatedColumnModeModel));
    }

    private static DialogComponentColumnNameSelection pathCompatibleDialogComponentColumnNameSelection() {
        final SettingsModelString selectedColumnNameModel = StringToPathNodeModel.createSettingsModelColumnName();
        return new DialogComponentColumnNameSelection(selectedColumnNameModel, "", 0, new ColumnFilter() {
            @Override
            public boolean includeColumn(final DataColumnSpec colSpec) {
                DataType type = colSpec.getType();
                return type.isCompatible(StringValue.class);
            }

            @Override
            public String allFilteredMsg() {
                return "No applicable column available";
            }
        });
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_fileSystemComponent.saveSettingsTo(settings);
        m_selectedColumnNameComponent.saveSettingsTo(settings);
        m_abortOnMissingFileComponent.saveSettingsTo(settings);
        m_failOnMissingValuesComponent.saveSettingsTo(settings);
        m_generatedColumnModeComponent.saveSettingsTo(settings);
        m_appendedColumnNameComponent.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_fileSystemComponent.loadSettingsFrom(settings, specs);
        m_selectedColumnNameComponent.loadSettingsFrom(settings, specs);
        m_abortOnMissingFileComponent.loadSettingsFrom(settings, specs);
        m_failOnMissingValuesComponent.loadSettingsFrom(settings, specs);
        m_generatedColumnModeComponent.loadSettingsFrom(settings, specs);
        m_appendedColumnNameComponent.loadSettingsFrom(settings, specs);
        checkGeneratedColumnMode();
    }
}
