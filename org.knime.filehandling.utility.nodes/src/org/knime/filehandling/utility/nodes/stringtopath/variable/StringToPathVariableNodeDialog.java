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
 *   Nov 30, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */

package org.knime.filehandling.utility.nodes.stringtopath.variable;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer.FlowVariableCell;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.DialogComponentFileSystem;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * The node dialog for the "String to Path (Variable)" node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class StringToPathVariableNodeDialog extends NodeDialogPane {

    private final FlowVariableFilterPanel m_filter;

    private final DialogComponentString m_variableSuffixComponent;

    private final DialogComponentFileSystem m_fileSystemComponent;

    private final DialogComponentBoolean m_abortOnMissingFileComponent;

    /**
     * Constructor for node dialog with dynamic ports.
     *
     * @param portsConfig the ports configuration
     */
    StringToPathVariableNodeDialog(final PortsConfiguration portsConfig) {
        m_filter = new FlowVariableFilterPanel(new InputFilter<FlowVariableCell>() {
            @Override
            public boolean include(final FlowVariableCell cell) {
                // true if variable does not start with "knime" and is of type String (see VariableTypeFilter)
                return !cell.getName().startsWith(Scope.Global.getPrefix())
                    && (!cell.isValid() || cell.getFlowVariable().getVariableType() == StringType.INSTANCE);
            }
        });

        final SettingsModelFileSystem fileSystemModel =
            StringToPathVariableNodeModel.createSettingsModelFileSystem(portsConfig);
        final FlowVariableModel fvm =
            createFlowVariableModel(fileSystemModel.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_fileSystemComponent = new DialogComponentFileSystem(fileSystemModel, fvm);

        m_variableSuffixComponent =
            new DialogComponentString(StringToPathVariableNodeModel.createSettingsModelVariableSuffix(),
                "Suffix added to the new variables:", true, 15);

        m_abortOnMissingFileComponent =
            new DialogComponentBoolean(StringToPathVariableNodeModel.createSettingsModelAbortOnMissingFile(),
                "Fail if file/folder does not exist");

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
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        optionsPanel.add(getVariableSelectionPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 0;
        optionsPanel.add(getFSConnectionPanel(), gbc);
        gbc.gridy++;
        optionsPanel.add(getOutputPanel(), gbc);
        return optionsPanel;
    }

    private JPanel getFSConnectionPanel() {
        final JPanel fSConnectionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        fSConnectionPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File system "));
        gbc.insets = new Insets(0, 7, 0, 0);
        fSConnectionPanel.add(m_fileSystemComponent.getComponentPanel(), gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        fSConnectionPanel.add(Box.createHorizontalBox(), gbc);
        return fSConnectionPanel;
    }

    private Component getVariableSelectionPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Variable selection "));
        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(1).fillBoth().anchorLineStart();
        p.add(m_filter, gbc.build());
        return p;
    }

    private JPanel getOutputPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output "));
        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(0).fillNone().anchorFirstLineStart();
        gbc.insets(0, 5, 0, 0);
        p.add(m_variableSuffixComponent.getComponentPanel(), gbc.build());
        p.add(m_abortOnMissingFileComponent.getComponentPanel(), gbc.incY().build());
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(StringToPathVariableNodeModel.CFG_VARIABLE_FILTER);
        m_filter.saveConfiguration(config);
        config.saveConfiguration(settings);
        m_fileSystemComponent.saveSettingsTo(settings);
        m_variableSuffixComponent.saveSettingsTo(settings);
        m_abortOnMissingFileComponent.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(StringToPathVariableNodeModel.CFG_VARIABLE_FILTER);
        config.loadConfigurationInDialog(settings, getAvailableFlowVariables(StringType.INSTANCE));
        m_filter.loadConfiguration(config, getAvailableFlowVariables(StringType.INSTANCE));
        m_fileSystemComponent.loadSettingsFrom(settings, specs);
        m_abortOnMissingFileComponent.loadSettingsFrom(settings, specs);
        m_variableSuffixComponent.loadSettingsFrom(settings, specs);
    }
}
