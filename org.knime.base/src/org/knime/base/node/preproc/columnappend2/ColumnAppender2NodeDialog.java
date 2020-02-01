/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   23.01.2019 (Temesgen H. Dadi): created
 */
package org.knime.base.node.preproc.columnappend2;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;

import org.knime.base.node.preproc.columnappend2.ColumnAppender2NodeModel.RowKeyMode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "ColumnAppender" Node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public class ColumnAppender2NodeDialog extends NodeDialogPane {
    private final int m_numInputPorts;

    private final SettingsModelString m_rowKeyModeModel;

    private final SettingsModelIntegerBounded m_selectedTableIndexModel;

    private final DialogComponentButtonGroup m_rowKeyModeComponent;

    private final DialogComponentNumber m_selectedTableIndexComponent;

    private final DialogComponentLabel m_invalidTableIndexComponent;

    private final String m_tableIndexValueInfo;

    private final JPanel m_panel;

    /**
     * Constructor for dynamic ports
     *
     * @param portsConfiguration the ports configuration
     */
    public ColumnAppender2NodeDialog(final PortsConfiguration portsConfiguration) {

        m_numInputPorts = portsConfiguration.getInputPorts().length;

        m_rowKeyModeModel = ColumnAppender2NodeModel.createRowIDModeSelectModel();
        m_selectedTableIndexModel = ColumnAppender2NodeModel.createRowIDTableSelectModel();
        m_rowKeyModeModel.addChangeListener(e -> controlTableSelect());
        m_selectedTableIndexModel.addChangeListener(e -> validateTableSelection());

        m_rowKeyModeComponent =
            new DialogComponentButtonGroup(m_rowKeyModeModel, null, true, ColumnAppender2NodeModel.RowKeyMode.values());
        m_rowKeyModeComponent.setToolTipText(RowKeyMode.GENERATE.getToolTip());

        m_selectedTableIndexComponent = new DialogComponentNumber(m_selectedTableIndexModel, "", 1, 4);

        m_invalidTableIndexComponent = new DialogComponentLabel("");

        m_tableIndexValueInfo = "<html>The selected port number for row key must be an integer <br> between 1 and "
            + m_numInputPorts + " (the number of input tables)</html>";

        m_panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        m_panel.add(m_rowKeyModeComponent.getComponentPanel(), gbc);

        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.gridx = 1;
        gbc.gridy = 0;
        m_panel.add(m_selectedTableIndexComponent.getComponentPanel(), gbc);

        gbc.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 1;

        m_panel.add(m_invalidTableIndexComponent.getComponentPanel(), gbc);
        addTab("Options", m_panel);

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (m_selectedTableIndexModel.getIntValue() < 1 || m_selectedTableIndexModel.getIntValue() > m_numInputPorts) {
            throw new InvalidSettingsException("Table number should be between 1 and " + m_numInputPorts + "!");
        }
        m_rowKeyModeComponent.saveSettingsTo(settings);
        m_selectedTableIndexComponent.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_rowKeyModeComponent.loadSettingsFrom(settings, specs);
        m_selectedTableIndexComponent.loadSettingsFrom(settings, specs);
        validateTableSelection();
        controlTableSelect();
    }

    /* trick borrowed from  LDA2NodeDialog */
    /**
     * A way to prevent overwriting on existing settings when the selected table is no more
     * there, e.g., when the input port providing the table is deleted.
     *
     */
    private void validateTableSelection() {
        m_selectedTableIndexComponent.setToolTipText(m_tableIndexValueInfo);
        final JSpinner spinner = (JSpinner)m_selectedTableIndexComponent.getComponentPanel().getComponent(1);
        final JLabel errMsgLabel = (JLabel)m_invalidTableIndexComponent.getComponentPanel().getComponent(0);
        final JFormattedTextField spinnerTextField = ((DefaultEditor)spinner.getEditor()).getTextField();
        if (m_selectedTableIndexModel.getIntValue() < 1 || m_selectedTableIndexModel.getIntValue() > m_numInputPorts) {
            spinnerTextField.setBackground(Color.RED);
            spinnerTextField.setEnabled(true);
            spinner.setEnabled(true);
            errMsgLabel.setText(m_tableIndexValueInfo);
            errMsgLabel.setForeground(Color.RED);
        } else {
            spinnerTextField.setBackground(Color.WHITE);
            spinnerTextField.setEnabled(true);
            spinner.setEnabled(true);
            errMsgLabel.setText("");
        }
    }

    private void controlTableSelect() {
        final JSpinner spinner = (JSpinner)m_selectedTableIndexComponent.getComponentPanel().getComponent(1);
        final JFormattedTextField spinnerTextField = ((DefaultEditor)spinner.getEditor()).getTextField();
        if (m_rowKeyModeModel.getStringValue().equals("KEY_TABLE")) {
            spinnerTextField.setEnabled(true);
            spinner.setEnabled(true);
        } else {
            spinnerTextField.setEnabled(false);
            spinner.setEnabled(false);
        }
    }

}
