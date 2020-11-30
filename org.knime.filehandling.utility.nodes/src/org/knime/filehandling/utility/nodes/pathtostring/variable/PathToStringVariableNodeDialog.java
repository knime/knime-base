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
 *   Nov 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtostring.variable;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.util.filter.variable.FlowVariableFilterPanel;
import org.knime.core.node.util.filter.variable.VariableTypeFilter;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * The path to string (variable) node dialog.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class PathToStringVariableNodeDialog extends NodeDialogPane {

    private final FlowVariableFilterPanel m_filter;

    private final DialogComponentString m_variableSuffixComponent;

    private final DialogComponentBoolean m_createKNIMEUrl;

    PathToStringVariableNodeDialog() {
        m_filter = new FlowVariableFilterPanel(new VariableTypeFilter(FSLocationVariableType.INSTANCE));

        m_variableSuffixComponent =
            new DialogComponentString(PathToStringVariableNodeModel.createSettingsModelVariableSuffix(),
                "Suffix added to the new variables:", true, 15);

        m_createKNIMEUrl = new DialogComponentBoolean(PathToStringVariableNodeModel.createSettingsModelCreateKNIMEUrl(),
            "Create KNIME URL for 'Relative to' and 'Mounpoint' file systems");

        addTab("Settings", createPanel());
    }

    private JPanel createPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(1).fillBoth().anchorLineStart();
        p.add(getVariableSelectionPanel(), gbc.build());

        gbc.incY().setWeightY(0);
        p.add(getOutputPanel(), gbc.build());
        return p;
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
            new GBCBuilder().resetX().resetY().setWeightX(0).setWeightY(0).fillNone().anchorFirstLineStart();
        gbc.insets(5, 5, 0, 0);
        p.add(m_variableSuffixComponent.getComponentPanel(), gbc.build());

        gbc.resetX().incY().setWidth(2).setWeightX(1);
        p.add(m_createKNIMEUrl.getComponentPanel(), gbc.build());

        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(PathToStringVariableNodeModel.CFG_VARIABLE_FILTER);
        m_filter.saveConfiguration(config);
        config.saveConfiguration(settings);
        m_variableSuffixComponent.saveSettingsTo(settings);
        m_createKNIMEUrl.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final FlowVariableFilterConfiguration config =
            new FlowVariableFilterConfiguration(PathToStringVariableNodeModel.CFG_VARIABLE_FILTER);
        config.loadConfigurationInDialog(settings, getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
        m_filter.loadConfiguration(config, getAvailableFlowVariables(FSLocationVariableType.INSTANCE));
        m_variableSuffixComponent.loadSettingsFrom(settings, specs);
        m_createKNIMEUrl.loadSettingsFrom(settings, specs);
    }

}
