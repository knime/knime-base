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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.function.IntFunction;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.base.node.meta.looper.condition.LoopEndConditionDynamicSettings.Operator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * This class is the dialog for the condition loop tail node in which the user can enter the condition.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndConditionNodeDialog} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopEndConditionDynamicNodeDialog extends NodeDialogPane {
    private static final Operator[] NUMERIC_OPERATORS =
        {Operator.EQ, Operator.LE, Operator.LT, Operator.GE, Operator.GT, Operator.NE};

    private static final Operator[] NON_NUMERIC_OPERATORS = {Operator.EQ, Operator.NE};

    private final LoopEndConditionDynamicSettings m_settings;

    private final DefaultComboBoxModel<FlowVariable> m_variablesModel = new DefaultComboBoxModel<>();

    private final JComboBox<FlowVariable> m_variables = new JComboBox<>(m_variablesModel);

    private final JComboBox<Operator> m_operator = new JComboBox<>(NUMERIC_OPERATORS);

    private final JTextField m_value = new JTextField(5);

    private final JLabel m_selectedVariable = new JLabel();

    private JCheckBox[] m_addLastRows;

    private JCheckBox[] m_addLastRowsOnly;

    private JCheckBox[] m_addIterationColumn;

    private final JCheckBox m_propagateLoopVariables = new JCheckBox("Propagate modified loop variables");

    LoopEndConditionDynamicNodeDialog(final int numberOfPorts) {
        m_settings = new LoopEndConditionDynamicSettings(numberOfPorts);
        final var panel = new JPanel(new GridBagLayout());

        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 3, 2, 3);
        panel.add(new JLabel("Available flow variables   "), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        m_variables.setRenderer(new FlowVariableListCellRenderer());
        m_variables.addItemListener(e -> updateOperations(panel));
        panel.add(m_variables, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Finish loop if selected variable is "), gbc);

        gbc.gridx = 1;
        panel.add(m_operator, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(m_value, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(m_propagateLoopVariables, gbc);

        initPortSettings(numberOfPorts, panel, gbc);

        addTab("Default settings", panel);
    }

    private void initPortSettings(final int numberOfPorts, final JPanel parent, final GridBagConstraints gbc) {
        m_addLastRows = new JCheckBox[numberOfPorts];
        m_addLastRowsOnly = new JCheckBox[numberOfPorts];
        m_addIterationColumn = new JCheckBox[numberOfPorts];
        final IntFunction<Border> createBorder =
            numberOfPorts > 1 ? (i -> BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Port " + i))
                : (i -> BorderFactory.createEmptyBorder());

        for (var i = 0; i < numberOfPorts; i++) {
            final var ifinal = i;
            m_addLastRows[i] = new JCheckBox("Collect rows from last iteration");
            m_addLastRowsOnly[i] = new JCheckBox("Collect rows from last iteration only");
            m_addIterationColumn[i] = new JCheckBox("Add iteration column");
            final var panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(m_addLastRows[i]);
            panel.add(m_addLastRowsOnly[i]);
            panel.add(m_addIterationColumn[i]);
            panel.setBorder(createBorder.apply(i));

            gbc.gridy++;
            parent.add(panel, gbc);
            m_addLastRowsOnly[ifinal].addActionListener(new ActionListener() {
                boolean m_wasSelected = true;

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (m_addLastRowsOnly[ifinal].isSelected()) {
                        m_wasSelected = m_addLastRows[ifinal].isSelected();
                        m_addLastRows[ifinal].setSelected(true);
                        m_addLastRows[ifinal].setEnabled(false);
                    } else {
                        m_addLastRows[ifinal].setSelected(m_wasSelected);
                        m_addLastRows[ifinal].setEnabled(true);
                    }
                }
            });
        }
    }

    private void updateOperations(final JPanel panel) {
        if (m_variables.getSelectedIndex() >= 0) {
            final var flowvar = (FlowVariable)m_variablesModel.getSelectedItem();
            final var selected = (Operator)m_operator.getSelectedItem();
            m_operator.removeAllItems();
            final Operator[] pool;
            if (flowvar.getVariableType() == VariableType.StringType.INSTANCE
                || flowvar.getVariableType() == VariableType.BooleanType.INSTANCE) {
                pool = NON_NUMERIC_OPERATORS;
            } else {
                pool = NUMERIC_OPERATORS;
            }
            Arrays.stream(pool).forEach(m_operator::addItem);
            Arrays.stream(pool)// restore selection if possible
                .filter(selected::equals)//
                .findAny()//
                .ifPresent(m_operator::setSelectedItem);
            m_selectedVariable.setText(flowvar.getName());
            panel.revalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_variablesModel.removeAllElements();
        for (final var v : getAvailableFlowVariables(VariableType.StringType.INSTANCE, VariableType.IntType.INSTANCE,
            VariableType.LongType.INSTANCE, VariableType.DoubleType.INSTANCE, VariableType.BooleanType.INSTANCE)
                .values()) {
            m_variablesModel.addElement(v);
            if (v.getName().equals(m_settings.variableName())) {
                m_variables.setSelectedItem(v);
            }
        }

        m_operator.setSelectedItem(m_settings.operator());
        m_value.setText(m_settings.value());
        m_propagateLoopVariables.setSelected(m_settings.propagateLoopVariables());

        for (var i = 0; i < m_addLastRows.length; i++) {
            m_addLastRows[i].setSelected(m_settings.addLastRows(i) || m_settings.addLastRowsOnly(i));
            m_addLastRowsOnly[i].setSelected(m_settings.addLastRowsOnly(i));
            m_addLastRows[i].setEnabled(!m_settings.addLastRowsOnly(i));
            m_addIterationColumn[i].setSelected(m_settings.addIterationColumn(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final var selelcted = m_variables.getSelectedItem();
        if (selelcted == null) {
            m_settings.variable(null);
        } else {
            m_settings.variable((FlowVariable)selelcted);
        }

        m_settings.operator((Operator)m_operator.getSelectedItem());
        m_settings.value(m_value.getText());
        m_settings.propagateLoopVariables(m_propagateLoopVariables.isSelected());
        for (var i = 0; i < m_addLastRows.length; i++) {
            m_settings.addLastRows(i, m_addLastRows[i].isSelected());
            m_settings.addLastRowsOnly(i, m_addLastRowsOnly[i].isSelected());
            m_settings.addIterationColumn(i, m_addIterationColumn[i].isSelected());
        }

        m_settings.saveSettings(settings);
    }
}
