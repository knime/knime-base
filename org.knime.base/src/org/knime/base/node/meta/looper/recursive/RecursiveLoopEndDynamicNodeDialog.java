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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.meta.looper.recursive;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentFlowVariableNameSelection2;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Dialog for the recursive loop end.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on RecursiveLoopEndNodeDialog by Iris Adae
 */
final class RecursiveLoopEndDynamicNodeDialog extends NodeDialogPane {

    private final RecursiveLoopEndDynamicNodeSettings m_settings;

    private final Supplier<Map<String, FlowVariable>> m_variableSupplier =
        RecursiveLoopEndDynamicNodeModel.getValidVariablesSupplier(this::getAvailableFlowVariables);

    private final DialogComponentFlowVariableNameSelection2 m_flowVarSelection;

    private final JCheckBox m_useFlowVariable;

    private final JSpinner[] m_minRows;

    private final JSpinner m_maxIterations;

    private final JCheckBox[] m_onlyLastData;

    private final JCheckBox[] m_addIterationColumn;

    private final JCheckBox m_propagateVariables;

    /**
     * Create new dialog.
     */
    RecursiveLoopEndDynamicNodeDialog(final int nrRecursionTables, final int nrCollectectionTables) {
        m_settings = new RecursiveLoopEndDynamicNodeSettings(nrRecursionTables, nrCollectectionTables);
        m_useFlowVariable = new JCheckBox("End loop with variable: ");
        m_flowVarSelection = new DialogComponentFlowVariableNameSelection2(
            RecursiveLoopEndDynamicNodeModel.createEndLoopVarModel(), null, m_variableSupplier);
        m_minRows = new JSpinner[nrRecursionTables];
        m_maxIterations = new JSpinner(new SpinnerNumberModel(100, 1, Integer.MAX_VALUE, 1));
        m_onlyLastData = new JCheckBox[nrCollectectionTables];
        m_addIterationColumn = new JCheckBox[nrCollectectionTables];
        m_propagateVariables = new JCheckBox("Propagate modified loop variables");

        final var panel = new JPanel(new GridBagLayout());
        final var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gbc.weighty = 1.0;
        panel.add(initEndSettings(), gbc);
        panel.add(initDataSettings(), gbc);
        panel.add(initVariableSettings(), gbc);

        addTab("Settings", panel);
    }

    private Component initEndSettings() {
        final var parent = new JPanel(new GridBagLayout());
        parent
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "End recursion settings"));
        final var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(2, 2, 2, 2);

        final IntFunction<JLabel> labelSuplier = m_minRows.length == 1//
            ? (i -> new JLabel("Minimal number of rows: ")) : (i -> new JLabel("Recursion port " + i + ": "));
        final Supplier<JSpinner> minRowSupplier = () -> new JSpinner(
            new SpinnerNumberModel(Long.valueOf(1), Long.valueOf(0), Long.valueOf(Long.MAX_VALUE), Long.valueOf(1)));

        if (m_minRows.length > 1) {
            final var portBox = new JPanel(new GridBagLayout());
            final var portgbc = new GridBagConstraints();
            portgbc.anchor = GridBagConstraints.NORTHWEST;
            portgbc.insets = new Insets(2, 2, 2, 2);
            portgbc.gridx = 0;
            portgbc.gridy = 0;
            portBox.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Minimal number of rows"));
            for (var i = 0; i < m_minRows.length; i++) {
                m_minRows[i] = minRowSupplier.get();
                addPair(portBox, portgbc, labelSuplier.apply(i), m_minRows[i]);
            }

            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            parent.add(portBox, gbc);

            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
        } else {
            m_minRows[0] = minRowSupplier.get();
            addPair(parent, gbc, labelSuplier.apply(0), m_minRows[0]);
        }
        addPair(parent, gbc, new JLabel("Maximum number of iterations: "), m_maxIterations);
        addPair(parent, gbc, m_useFlowVariable, m_flowVarSelection.getComponentPanel());

        final var hasVariables = !m_variableSupplier.get().isEmpty();
        m_useFlowVariable.setEnabled(hasVariables);
        m_useFlowVariable.addActionListener(a -> m_flowVarSelection.getModel().setEnabled(m_useFlowVariable.isSelected() && hasVariables));
        m_flowVarSelection.getModel().setEnabled(m_useFlowVariable.isSelected() && hasVariables);

        return parent;
    }

    private Component initDataSettings() {
        final var parent = new JPanel(new GridBagLayout());
        parent.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Data collection settings"));
        final var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        final IntFunction<Border> createBorder = m_onlyLastData.length > 1 //
            ? (i -> BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Collection port " + i))
            : (i -> BorderFactory.createEmptyBorder());

        for (var i = 0; i < m_onlyLastData.length; i++) {
            final var finali = i;
            m_onlyLastData[i] = new JCheckBox("Collect data from last iteration only");
            m_addIterationColumn[i] = new JCheckBox("Add iteration column");
            final var panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            m_onlyLastData[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            m_addIterationColumn[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(m_onlyLastData[i]);
            panel.add(m_addIterationColumn[i]);
            panel.setBorder(createBorder.apply(i));

            m_onlyLastData[i].addActionListener(new ActionListener() {
                private boolean m_wasSelected = false;

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (m_onlyLastData[finali].isSelected()) {
                        m_wasSelected = m_addIterationColumn[finali].isSelected();
                        m_addIterationColumn[finali].setSelected(false);
                        m_addIterationColumn[finali].setEnabled(false);
                    } else {
                        m_addIterationColumn[finali].setSelected(m_wasSelected);
                        m_addIterationColumn[finali].setEnabled(true);
                    }
                }
            });

            gbc.gridy++;
            parent.add(panel, gbc);
        }

        return parent;
    }

    private Component initVariableSettings() {
        final var parent = new JPanel();
        parent.setLayout(new BoxLayout(parent, BoxLayout.Y_AXIS));
        parent.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Variable settings"));

        m_propagateVariables.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(m_propagateVariables);
        return parent;
    }

    private static void addPair(final JPanel panel, final GridBagConstraints gbc, final Component left,
        final Component right) {
        final var insets = gbc.insets;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = (Insets)insets.clone();
        gbc.insets.right = gbc.insets.right*3;
        panel.add(left, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = (Insets)insets.clone();
        gbc.insets.left = gbc.insets.left*3;
        gbc.weightx = 1.0;
        panel.add(right, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets = insets;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // only save value if the box is enabled to preserve user setting
        if (m_useFlowVariable.isEnabled()) {
            m_settings.setUseFlowVariable(m_useFlowVariable.isSelected());
        }
        for (var i = 0; i < m_minRows.length; i++) {
            m_settings.setMinNumberOfRows(i, (Long)m_minRows[i].getValue());
        }
        m_settings.setMaxIterations((Integer)m_maxIterations.getValue());
        for (var i = 0; i < m_onlyLastData.length; i++) {
            m_settings.setOnlyLastData(i, m_onlyLastData[i].isSelected());
            if (!m_settings.hasOnlyLastData(i)) {
                m_settings.setAddIterationColumn(i, m_addIterationColumn[i].isSelected());
            }
        }
        m_settings.setPropagateVariables(m_propagateVariables.isSelected());

        m_flowVarSelection.saveSettingsTo(settings);
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        final var hasVariables = !m_variableSupplier.get().isEmpty();
        Arrays.stream(m_useFlowVariable.getActionListeners()).forEach(m_useFlowVariable::removeActionListener);

        m_flowVarSelection.loadSettingsFrom(settings, specs);
        m_useFlowVariable.setEnabled(hasVariables);
        m_useFlowVariable.setSelected(m_settings.hasFlowVariable() && hasVariables);
        m_useFlowVariable.addActionListener(a -> m_flowVarSelection.getModel().setEnabled(m_useFlowVariable.isSelected() && hasVariables));
        m_flowVarSelection.getModel().setEnabled(hasVariables && m_settings.hasFlowVariable());

        for (var i = 0; i < m_minRows.length; i++) {
            m_minRows[i].setValue(m_settings.getMinNumberOfRows(i));
        }
        m_maxIterations.setValue(m_settings.getMaxIterations());
        for (var i = 0; i < m_onlyLastData.length; i++) {
            if (m_settings.hasOnlyLastData(i)) {
                m_onlyLastData[i].setSelected(true);
                m_addIterationColumn[i].setEnabled(false);
                m_addIterationColumn[i].setSelected(false);
            } else {
                m_onlyLastData[i].setSelected(false);
                m_addIterationColumn[i].setEnabled(true);
                m_addIterationColumn[i].setSelected(m_settings.hasIterationColumn(i));
            }
        }
        m_propagateVariables.setSelected(m_settings.isPropagateVariables());
    }
}
