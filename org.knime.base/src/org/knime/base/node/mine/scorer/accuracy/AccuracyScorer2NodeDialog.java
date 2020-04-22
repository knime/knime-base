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
package org.knime.base.node.mine.scorer.accuracy;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.knime.base.util.SortingOptionPanel;
import org.knime.base.util.SortingStrategy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;

/**
 * A dialog for the scorer to set the two table columns to score for.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 * @author Lars Schweikardt, KNIME GmbH, Konstanz
 *
 */
final class AccuracyScorer2NodeDialog extends NodeDialogPane {
    /**
     * The selection panel for the first column to compare. The first column represents the real classes of the data
     */
    private final ColumnSelectionPanel m_firstColumns;

    /**
     * The selection panel for the second column to compare The second column represents the predicted classes of the
     * data
     */
    private final ColumnSelectionPanel m_secondColumns;

    /**
     * The sorting option for the values.
     */
    private final SortingOptionPanel m_sortingOptions;

    /** The check box specifying if a prefix should be added or not. */
    private JCheckBox m_flowvariableBox;

    /** The text field specifying the prefix for the flow variables. */
    private JTextField m_flowVariablePrefixTextField;

    /** SortingStrategy Array containing the sorting strategies for numbers */
    private static final SortingStrategy[] SUPPORTED_NUMBER_SORT_STRATEGIES =
        new SortingStrategy[]{SortingStrategy.InsertionOrder, SortingStrategy.Numeric, SortingStrategy.Lexical};

    /** SortingStrategy Array containing the sorting strategies for strings */
    private static final SortingStrategy[] SUPPORTED_STRING_SORT_STRATEGIES =
        new SortingStrategy[]{SortingStrategy.InsertionOrder, SortingStrategy.Lexical};

    /** Array containing the compatible datatypes for the column selection. */
    @SuppressWarnings("unchecked")
    private static final Class<? extends DataValue>[] COMPATABILE_DATATYPES =
        new Class[]{LongValue.class, StringValue.class, NominalValue.class};

    /** Radio button for ignoring missing values, default for compatibility. */
    private final JRadioButton m_ignoreMissingValues = new JRadioButton("Ignore", true);

    /** Radio button to fail on missing values. */
    private final JRadioButton m_failOnMissingValues = new JRadioButton("Fail", false);

    /** Missing value flag. */
    private boolean m_missingValueOption;

    /**
     * Creates a new {@link NodeDialogPane} for scoring in order to set the two columns to compare.
     */
    AccuracyScorer2NodeDialog() {
        this(true);
    }

    /**
     * Creates a new {@link NodeDialogPane} for scoring in order to set the two columns to compare.
     *
     * @param missingValueOption whether the missing value option should be shown in the dialogue
     */
    AccuracyScorer2NodeDialog(final boolean missingValueOption) {
        super();

        m_missingValueOption = missingValueOption;
        m_firstColumns = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), COMPATABILE_DATATYPES);
        m_firstColumns.addItemListener(e -> updateSortingStrategies());
        m_secondColumns = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), COMPATABILE_DATATYPES);
        m_secondColumns.addItemListener(e -> updateSortingStrategies());
        m_sortingOptions = new SortingOptionPanel();
        m_flowvariableBox = new JCheckBox("Use name prefix");
        m_flowvariableBox.addActionListener(e -> updateFlowVariableTextfield());

        m_flowVariablePrefixTextField = new JTextField(10);
        m_flowVariablePrefixTextField.setSize(new Dimension(10, 3));
        m_flowvariableBox.doClick(); // sync states

        super.addTab("Scorer", createPanel());
    }

    private JPanel createPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 1;

        ++gbc.gridy;
        p.add(createInnerPanel(m_firstColumns, "First Column"), gbc);

        ++gbc.gridy;
        p.add(createInnerPanel(m_secondColumns, "Second Column"), gbc);

        ++gbc.gridy;
        p.add(createInnerPanel(m_sortingOptions, "Sorting of values in tables"), gbc);

        final JPanel flowVariablePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcFlowVariable = createInnerPanelGridBagConstraint();
        flowVariablePanel.setBorder(BorderFactory.createTitledBorder("Provide scores as flow variables"));
        gbcFlowVariable.weightx = 0;
        flowVariablePanel.add(m_flowvariableBox, gbcFlowVariable);
        gbcFlowVariable.insets = new Insets(0, 10, 0, 0);
        gbcFlowVariable.weightx = 1;
        flowVariablePanel.add(m_flowVariablePrefixTextField, gbcFlowVariable);
        ++gbc.gridy;
        p.add(flowVariablePanel, gbc);

        if (m_missingValueOption) {
            ++gbc.gridy;
            p.add(createMissingValuePanel(), gbc);
        }
        return p;
    }

    /**
     * Creates a JPanel {@link JPanel} with the passed {@link Component} and sets a title of the border.
     *
     * @param component Component which will be added to the JPanel
     * @param title title of the Border
     * @return a {@link JPanel}
     */
    private static JPanel createInnerPanel(final Component component, final String title) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createInnerPanelGridBagConstraint();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, gbc);
        return panel;
    }

    /**
     * Creates the default {@link GridBagConstraints} for the inner {@link JPanel}
     *
     * @return the default {@link GridBagConstraints}
     */
    private static GridBagConstraints createInnerPanelGridBagConstraint() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1;
        gbc.weighty = 1;
        return gbc;
    }

    /**
     * Creates the {@link JPanel} for the missing values section.
     *
     * @return {@link JPanel}
     */
    private JPanel createMissingValuePanel() {
        final ButtonGroup missingValueGroup = new ButtonGroup();
        missingValueGroup.add(m_ignoreMissingValues);
        missingValueGroup.add(m_failOnMissingValues);
        final JPanel missingValues = new JPanel(new GridBagLayout());
        missingValues.setBorder(BorderFactory.createTitledBorder("Missing values"));
        final GridBagConstraints gbc = createInnerPanelGridBagConstraint();
        gbc.weightx = 0;
        JLabel label = new JLabel("In case of missing values:");
        missingValues.add(label, gbc);
        gbc.insets = new Insets(0, 10, 0, 0);
        gbc.weightx = 1;
        gbc.gridx = 1;
        missingValues.add(m_ignoreMissingValues, gbc);
        gbc.gridy = 1;
        missingValues.add(m_failOnMissingValues, gbc);

        return missingValues;
    }

    /** Enables flowvariable text field based on the selection of checkbox. */
    private void updateFlowVariableTextfield() {
        if (m_flowvariableBox.isSelected()) {
            m_flowVariablePrefixTextField.setEnabled(true);
        } else {
            m_flowVariablePrefixTextField.setEnabled(false);
        }
    }

    /** Updates the sorting strategies based on the selected columns. */
    private void updateSortingStrategies() {
        final DataColumnSpec specFirst = m_firstColumns.getSelectedColumnAsSpec();
        final DataColumnSpec specSecond = m_secondColumns.getSelectedColumnAsSpec();
        if (specFirst == null || specSecond == null) {
            return;
        }
        if (specFirst.getType().isCompatible(DoubleValue.class)
            && specSecond.getType().isCompatible(DoubleValue.class)) {
            m_sortingOptions.setPossibleSortingStrategies(SUPPORTED_NUMBER_SORT_STRATEGIES);
        } else if (specFirst.getType().isCompatible(StringValue.class)
            && specSecond.getType().isCompatible(StringValue.class)) {
            m_sortingOptions.setPossibleSortingStrategies(SUPPORTED_STRING_SORT_STRATEGIES);
        } else {
            m_sortingOptions.setPossibleSortingStrategies(SortingStrategy.InsertionOrder);
        }
    }

    /**
     * Fills the two combo boxes with all column names retrieved from the input table spec. The second and last column
     * will be selected by default unless the settings object contains others.
     *
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        assert (settings != null && specs != null);

        DataTableSpec spec = specs[AbstractAccuracyScorerNodeModel.INPORT];

        if ((spec == null) || (spec.getNumColumns() < 2)) {
            throw new NotConfigurableException("Scorer needs an input table " + "with at least two columns");
        }

        int numCols = spec.getNumColumns();
        String col2DefaultName = (numCols > 0) ? spec.getColumnSpec(numCols - 1).getName() : null;
        String col1DefaultName = (numCols > 1) ? spec.getColumnSpec(numCols - 2).getName() : col2DefaultName;
        DataColumnSpec col1 =
            spec.getColumnSpec(settings.getString(AbstractAccuracyScorerNodeModel.FIRST_COMP_ID, col1DefaultName));
        DataColumnSpec col2 =
            spec.getColumnSpec(settings.getString(AbstractAccuracyScorerNodeModel.SECOND_COMP_ID, col2DefaultName));

        //Check for null in case settings and table are not matching anymore and set the default cols
        m_firstColumns.update(specs[0], col1 == null ? col1DefaultName : col1.getName());
        m_secondColumns.update(specs[0], col2 == null ? col2DefaultName : col2.getName());

        String varPrefix = settings.getString(AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX, null);

        boolean useFlowVar = varPrefix != null;

        if (m_flowvariableBox.isSelected() != useFlowVar) {
            m_flowvariableBox.doClick();
        }
        if (varPrefix != null) {
            m_flowVariablePrefixTextField.setText(varPrefix);
        }

        try {
            m_sortingOptions.loadDefault(settings);
        } catch (InvalidSettingsException e) {
            m_sortingOptions.setSortingStrategy(SortingStrategy.InsertionOrder);
            m_sortingOptions.setReverseOrder(false);
        }
        m_sortingOptions.updateControls();
        if (m_missingValueOption) {
            boolean ignoreMissingValues = settings.getBoolean(AbstractAccuracyScorerNodeModel.ACTION_ON_MISSING_VALUES,
                AbstractAccuracyScorerNodeModel.DEFAULT_IGNORE_MISSING_VALUES);
            m_ignoreMissingValues.setSelected(ignoreMissingValues);
            m_failOnMissingValues.setSelected(!ignoreMissingValues);
        }
    }

    /**
     * Sets the selected columns inside the {@link AccuracyScorer2NodeModel}.
     *
     * @param settings the object to write the settings into
     * @throws InvalidSettingsException if the column selection is invalid
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        assert (settings != null);

        String firstColumn = m_firstColumns.getSelectedColumn();
        String secondColumn = m_secondColumns.getSelectedColumn();

        if ((firstColumn == null) || (secondColumn == null)) {
            throw new InvalidSettingsException("Select two valid column names " + "from the lists (or press cancel).");
        }
        if (m_firstColumns.getNrItemsInList() > 1 && firstColumn.equals(secondColumn)) {
            throw new InvalidSettingsException("First and second column cannot be the same.");
        }
        settings.addString(AbstractAccuracyScorerNodeModel.FIRST_COMP_ID, firstColumn);
        settings.addString(AbstractAccuracyScorerNodeModel.SECOND_COMP_ID, secondColumn);

        boolean useFlowVar = m_flowvariableBox.isSelected();

        String flowVariableName = m_flowVariablePrefixTextField.getText();

        settings.addString(AbstractAccuracyScorerNodeModel.FLOW_VAR_PREFIX, useFlowVar ? flowVariableName : null);

        m_sortingOptions.saveDefault(settings);

        if (m_missingValueOption) {
            settings.addBoolean(AbstractAccuracyScorerNodeModel.ACTION_ON_MISSING_VALUES,
                m_ignoreMissingValues.isSelected());
        }
    }
}
