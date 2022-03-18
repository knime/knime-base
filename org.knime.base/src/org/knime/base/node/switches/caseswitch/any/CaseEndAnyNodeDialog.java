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
 */
package org.knime.base.node.switches.caseswitch.any;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.switches.caseswitch.any.CaseEndAnyNodeModel.MultipleActiveHandling;
import org.knime.base.node.switches.endcase.EndcaseNodeDialog;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * Dialog for the node collecting data from an arbitrary amount of - potentially inactive - branches.<br>
 * <br>
 * If the type of the port is a {@link BufferedDataTable}, it allows for treatment of duplicate row keys in case of both
 * branches carrying data. Possible options are: (1) skip duplicate rows, (2) append suffix to key.<br>
 * <br>
 * Otherwise the user can choose whether to fail if multiple branches are active.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author M. Berthold, University of Konstanz (copied from AppendedRowsNodeDialog) (original {@link EndcaseNodeDialog}
 *         as a base)
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany (original {@link CaseEndNodeDialogPane} as a base)
 */
final class CaseEndAnyNodeDialog extends NodeDialogPane {

    //// BufferedDataTable ////

    private JRadioButton m_appendSuffixButton;

    private JRadioButton m_skipRowButton;

    private JTextField m_suffixField;

    private JCheckBox m_enableHiliting;

    //// Shared ////
    private DialogComponentButtonGroup m_handling;

    /**
     * Constructor to init the gui and set a title.
     *
     * @param inPorts the input ports used to decide how to behave
     */
    public CaseEndAnyNodeDialog(final PortType[] inPorts) {
        if (inPorts.length == 0) {
            return; // we are invalid
        }
        final var name = inPorts[0].getName();
        final var bufferedDataTable = inPorts[0].equals(BufferedDataTable.TYPE);
        m_handling = new DialogComponentButtonGroup(CaseEndAnyNodeModel.createMultipleActiveHandlingSettingsModel(),
            "If multiple inputs are active", true,
            bufferedDataTable ? MultipleActiveHandling.values() : MultipleActiveHandling.OPTIONS_OTHER);
        // init both components even if only one is used so that we can keep settings more easily
        final var panelBufferedDataTable = initBufferedDataTableTab(name, bufferedDataTable);
        final var panelOther = initOtherTab(name, bufferedDataTable);

        addTab("Settings", bufferedDataTable ? panelBufferedDataTable : panelOther);
    }

    private JPanel initBufferedDataTableTab(final String name, final boolean bufferedDataTable) {
        final ActionListener actionListener = a -> m_suffixField.setEnabled(m_appendSuffixButton.isSelected());
        m_suffixField = new JTextField(8);

        final var buttonGroup = new ButtonGroup();

        m_skipRowButton = new JRadioButton("Skip Rows");
        m_skipRowButton.setToolTipText("Will skip duplicate rows and print a warning message.");
        m_skipRowButton.addActionListener(actionListener);
        buttonGroup.add(m_skipRowButton);

        m_appendSuffixButton = new JRadioButton("Append Suffix: ");
        m_appendSuffixButton.setToolTipText("Will append a suffix to any duplicate row ID to make it unique.");
        m_appendSuffixButton.addActionListener(actionListener);
        buttonGroup.add(m_appendSuffixButton);

        final var panel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;

        panel.add(new JLabel("Port type name: " + name), constraints);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        constraints.weightx = 1.0;
        if (bufferedDataTable) { // components can only be added once
            panel.add(m_handling.getComponentPanel(), constraints);
        }

        final var centerPanel = new JPanel(new GridLayout(0, 1));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Duplicate row ID handling"));
        final var skipButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skipButtonPanel.add(m_skipRowButton);
        centerPanel.add(skipButtonPanel);
        final var suffixButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        suffixButtonPanel.add(m_appendSuffixButton);
        suffixButtonPanel.add(m_suffixField);
        centerPanel.add(suffixButtonPanel);
        if (CaseEndAnyNodeModel.DEF_APPEND_SUFFIX) {
            m_appendSuffixButton.doClick();
        } else {
            m_skipRowButton.doClick();
        }
        panel.add(centerPanel, constraints);

        final var hilitePanel = new JPanel(new BorderLayout());
        hilitePanel.setBorder(BorderFactory.createTitledBorder("Hiliting"));
        m_enableHiliting = new JCheckBox("Enable hiliting");
        hilitePanel.add(m_enableHiliting);
        panel.add(hilitePanel, constraints);

        m_handling.getModel().addChangeListener(c -> setBufferedDataTableSettingsEnabled(centerPanel));
        setBufferedDataTableSettingsEnabled(centerPanel); // trigger update to be sure
        return panel;
    }

    private JPanel initOtherTab(final String name, final boolean bufferedDataTable) {
        final var panel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Port type name: " + name), constraints);
        if (!bufferedDataTable) { // components can only be added once
            panel.add(m_handling.getComponentPanel(), constraints);
        }

        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1.0;
        panel.add(Box.createGlue(), constraints);

        return panel;
    }

    private void setBufferedDataTableSettingsEnabled(final JPanel centerPanel) {
        final var enabled = ((SettingsModelString)m_handling.getModel()).getStringValue()
            .equals(MultipleActiveHandling.Merge.getActionCommand());
        centerPanel.setEnabled(enabled);
        m_appendSuffixButton.setEnabled(enabled);
        m_suffixField.setEnabled(enabled && m_appendSuffixButton.isSelected());
        m_skipRowButton.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs.length == 0) {
            throw new NotConfigurableException("Please select an output type!");
        }
        final var appendSuffix =
            settings.getBoolean(CaseEndAnyNodeModel.CFG_APPEND_SUFFIX, CaseEndAnyNodeModel.DEF_APPEND_SUFFIX);
        final var suffix = settings.getString(CaseEndAnyNodeModel.CFG_SUFFIX, CaseEndAnyNodeModel.DEF_SUFFIX);
        if (appendSuffix) {
            m_appendSuffixButton.setSelected(true);
        } else {
            m_skipRowButton.setSelected(true);
        } // m_suffixField is updated by setBufferedDataTableSettingsEnabled(JPanel) triggered when loading m_handling
        m_suffixField.setText(suffix);
        m_enableHiliting
            .setSelected(settings.getBoolean(CaseEndAnyNodeModel.CFG_HILITING, CaseEndAnyNodeModel.DEF_HILITING));

        m_handling.loadSettingsFrom(settings, specs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        boolean isSuffix = m_appendSuffixButton.isSelected();
        String suffix = m_suffixField.getText();
        settings.addBoolean(CaseEndAnyNodeModel.CFG_APPEND_SUFFIX, isSuffix);
        settings.addString(CaseEndAnyNodeModel.CFG_SUFFIX, suffix);
        settings.addBoolean(CaseEndAnyNodeModel.CFG_HILITING, m_enableHiliting.isSelected());
        m_handling.saveSettingsTo(settings);
    }
}
