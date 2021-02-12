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
 */

package org.knime.base.data.filter.row.dialog.component.editor;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.data.filter.row.dialog.OperatorPanel;
import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.component.config.EditorPanelConfig;
import org.knime.base.data.filter.row.dialog.component.handler.TreeConditionHandler;
import org.knime.base.data.filter.row.dialog.component.renderer.ColumnNameComboBoxCellRenderer;
import org.knime.base.data.filter.row.dialog.component.renderer.OperatorComboBoxCellRenderer;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Operation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.base.data.filter.row.dialog.registry.OperatorKey;
import org.knime.core.data.DataType;

/**
 * Panel for edit condition.
 *
 * @author Viktor Buria
 */
public final class ConditionEditorPanel extends JPanel {

    private static final long serialVersionUID = 419700229649678386L;

    private final JButton m_deleteButton;

    private final JComboBox<ColumnSpec> m_columnNameComboBox = new JComboBox<>();

    private final JComboBox<Operator> m_operatorComboBox = new JComboBox<>();

    private final OperatorPanelHolder m_operatorPanelHolder;

    private final ErrorLabel m_errorLabel;

    private final transient EditorPanelConfig m_config;

    private transient TreeConditionHandler m_condition;

    private transient Supplier<String[]> m_operatorPanelValuesSupplier;

    private volatile boolean m_displaying;

    /**
     * Constructs a {@link ConditionEditorPanel} object.
     *
     * @param config the {@link EditorPanelConfig}
     * @param errorLabel the {@link JLabel} component to show condition errors.
     */
    public ConditionEditorPanel(final EditorPanelConfig config, final ErrorLabel errorLabel) {
        m_config = Objects.requireNonNull(config, "config");
        m_errorLabel = Objects.requireNonNull(errorLabel, "errorLabel");

        setLayout(new GridBagLayout());

        final JLabel label = new JLabel("Edit condition");

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 2, 2, 2);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(label, gbc);

        gbc.gridx++;
        //Button "Delete"
        m_deleteButton = new JButton("Delete");
        m_deleteButton.addActionListener(e -> {
            if (m_condition != null) {
                m_condition.delete();
            }
        });

        gbc.gridx++;
        add(m_deleteButton, gbc);

        final JPanel conditionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Combo-box "Field"
        m_columnNameComboBox.setRenderer(new ColumnNameComboBoxCellRenderer());
        m_columnNameComboBox.setFont((new Font(m_columnNameComboBox.getFont().getName(), Font.PLAIN, 13)));
        conditionPanel.add(m_columnNameComboBox);

        // Combo-box "Operator"
        m_operatorComboBox.setRenderer(new OperatorComboBoxCellRenderer());
        m_operatorComboBox.setFont((new Font(m_operatorComboBox.getFont().getName(), Font.PLAIN, 13)));
        conditionPanel.add(m_operatorComboBox);

        // Panel "Filter Input"
        m_operatorPanelHolder = new OperatorPanelHolder();
        conditionPanel.add(m_operatorPanelHolder);

        gbc.weighty = 50;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        add(conditionPanel, gbc);

        registerActions();
    }

    /**
     * Cancels every execution that is currently in progress in this panel.
     */
    public void cancelEveryExecution() {
        m_operatorPanelHolder.getOperatorPanel().ifPresent(OperatorPanel::cancelEveryExecution);
    }

    @Override
    public void setVisible(final boolean aFlag) {
        if (!aFlag) {
            cancelEveryExecution();
        }
        super.setVisible(aFlag);
    }

    private void registerActions() {
        m_columnNameComboBox.addActionListener(e -> {
            if (!m_displaying) {
                final ColumnSpec selectedValue = (ColumnSpec)m_columnNameComboBox.getSelectedItem();
                m_condition.get().setColumnSpec(selectedValue);
                fillOperatorComboBox();
                updateTreeCondition();
            }
        });

        m_operatorComboBox.addActionListener(e -> {
            if (!m_displaying) {
                final Operator operator = (Operator)m_operatorComboBox.getSelectedItem();
                final Operation operation = m_condition.get().getOperation();
                operation.setOperator(operator);

                fillOperatorPanel();

                updateTreeCondition();
            }
        });
    }

    private void updateTreeCondition() {
        m_condition.updateView();

        final ValidationResult validationResult = m_condition.getValidationResult();

        m_errorLabel.setErrors(validationResult);

        m_operatorPanelHolder.getOperatorPanel().ifPresent(panel -> panel.setValidationResult(validationResult));
    }

    /**
     * Displays a condition object.
     *
     * @param condition the {@linkplain TreeConditionHandler condition}
     */
    public void display(final TreeConditionHandler condition) {
        m_displaying = true;
        try {
            m_condition = Objects.requireNonNull(condition, "condition");

            m_operatorPanelValuesSupplier = null;

            fillColumnNameComboBox();

            fillOperatorComboBox();

            fillOperatorPanel();
        } finally {
            m_displaying = false;
        }
    }

    /**
     * Save resent changes to condition domain model.
     */
    public void saveChanges() {
        if (m_condition != null) {

            if (m_operatorPanelValuesSupplier != null) {
                final Operation operation = m_condition.get().getOperation();
                operation.setValues(m_operatorPanelValuesSupplier.get());
            }

            updateTreeCondition();
        }
    }

    private void fillColumnNameComboBox() {
        m_columnNameComboBox.removeAllItems();
        m_columnNameComboBox.addItem(null);
        final List<ColumnSpec> columnSpecList = m_config.getColumnSpecList();
        if (columnSpecList != null && !columnSpecList.isEmpty()) {
            columnSpecList.stream().forEach(m_columnNameComboBox::addItem);
            setSelectedItemForFieldName();
        } else {
            final ColumnSpec conditionSpec = m_condition.get().getColumnSpec();
            if (conditionSpec != null) {
                final String fieldName = conditionSpec.getName();
                final DataType dataType = conditionSpec.getType();
                if (fieldName != null && dataType != null) {
                    m_columnNameComboBox.addItem(conditionSpec);
                    m_columnNameComboBox.setSelectedItem(conditionSpec);
                }
            }
        }
    }

    private void setSelectedItemForFieldName() {
        final ColumnSpec conditionSpec = m_condition.get().getColumnSpec();
        if (conditionSpec == null) {
            return;
        }

        final String fieldName = conditionSpec.getName();
        final DataType type = conditionSpec.getType();
        if (fieldName != null && type != null) {
            for (ColumnSpec columnSpec : m_config.getColumnSpecList()) {
                if (fieldName.equals(columnSpec.getName()) && type == columnSpec.getType()) {
                    m_columnNameComboBox.setSelectedItem(columnSpec);
                    break;
                }
            }
        }
    }

    private void fillOperatorComboBox() {
        m_operatorComboBox.removeAllItems();
        if (m_columnNameComboBox.getSelectedItem() == null) {
            m_operatorComboBox.addItem(null);
        } else {
            final ColumnSpec selectedValue = (ColumnSpec)m_columnNameComboBox.getSelectedItem();
            if (selectedValue != null) {
                final List<Operator> operators =
                    m_config.getOperatorRegistry().findRegisteredOperators(selectedValue.getType());
                operators.forEach(m_operatorComboBox::addItem);
                final Operation operation = m_condition.get().getOperation();
                if (operation != null) {
                    final Operator operator = operation.getOperator();
                    operators.stream().filter(p -> p.equals(operator)).findFirst()
                        .ifPresent(m_operatorComboBox::setSelectedItem);
                }
            }
        }
    }

    private void fillOperatorPanel() {
        m_operatorPanelHolder.cleanUp();

        final Operator operator = (Operator)m_operatorComboBox.getSelectedItem();
        if (operator != null) {

            final ColumnSpec columnSpec = m_condition.get().getColumnSpec();

            final Operation operation = m_condition.get().getOperation();

            final Optional<OperatorPanel> operatorPanel =
                m_config.getOperatorRegistry().findPanel(OperatorKey.key(columnSpec.getType(), operator));

            if (operatorPanel.isPresent()) {
                // When operator panel exists.
                OperatorPanel panel = operatorPanel.get();
                m_operatorPanelValuesSupplier = panel::getValues;

                final OperatorPanelParameters parameters = m_config.getOperatorPanelParameters();
                parameters.setParameters(new OperatorParameters(columnSpec, operator, operation.getValues()));
                panel.init(parameters);

                panel.setOnValuesChanged(values -> {
                    operation.setValues(values);
                    updateTreeCondition();
                });
                m_operatorPanelHolder.setOperatorPanel(panel);
            } else {
                // If operator panel cannot be found or an operation doesn't have a panel,i.e. IS NULL operator.
                m_operatorPanelValuesSupplier = null;
                operation.setValues(null);
            }
            m_operatorPanelHolder.refresh();
        }
        // Save latest changes if any.
        saveChanges();
    }

    /**
     * UI holder component for the {@link OperatorPanel}s.
     *
     * @author Viktor Buria
     */
    private static final class OperatorPanelHolder extends JPanel {

        private static final long serialVersionUID = 5129050792359250919L;

        private transient OperatorPanel m_operatorPanel;

        /**
         * Constructs an {@link OperatorPanelHolder} object.
         */
        OperatorPanelHolder() {
            setLayout(new GridLayout(1, 1, 0, 0));
        }

        /**
         * Gets the stored operator panel.
         *
         * @return the {@link OperatorPanel} object
         */
        public Optional<OperatorPanel> getOperatorPanel() {
            return Optional.ofNullable(m_operatorPanel);
        }

        /**
         * Sets the new operator panel.
         *
         * @param operatorPanel the {@link OperatorPanel} object
         */
        public void setOperatorPanel(final OperatorPanel operatorPanel) {
            m_operatorPanel = Objects.requireNonNull(operatorPanel, "operatorPanel");

            add(operatorPanel.getPanel());
        }

        /**
         * Removes all UI components from the operator panel holder.
         */
        public void cleanUp() {
            final OperatorPanel operatorPanel = m_operatorPanel;
            if (operatorPanel != null) {
                operatorPanel.cancelEveryExecution();
            }
            removeAll();
        }

        /**
         * Refreshes all UI components in the operator panel holder.
         */
        public void refresh() {
            revalidate();
            repaint();
        }

    }

}
