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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

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
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        m_firstColumns = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), COMPATABILE_DATATYPES);
        m_secondColumns = new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), COMPATABILE_DATATYPES);

        m_sortingOptions = new SortingOptionPanel();
        m_sortingOptions.setBorder(new TitledBorder("Sorting of values in tables"));

        JPanel firstColumnPanel = new JPanel(new GridLayout(1, 1));
        firstColumnPanel.setBorder(BorderFactory.createTitledBorder("First Column"));
        JPanel flowLayout = new JPanel(new FlowLayout());
        flowLayout.add(m_firstColumns);
        firstColumnPanel.add(flowLayout);

        JPanel secondColumnPanel = new JPanel(new GridLayout(1, 1));
        secondColumnPanel.setBorder(BorderFactory.createTitledBorder("Second Column"));
        flowLayout = new JPanel(new FlowLayout());
        flowLayout.add(m_secondColumns);

        secondColumnPanel.add(flowLayout);

        m_flowvariableBox = new JCheckBox("Use name prefix");
        m_flowVariablePrefixTextField = new JTextField(10);
        m_flowVariablePrefixTextField.setSize(new Dimension(10, 3));

        m_flowvariableBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                if (m_flowvariableBox.isSelected()) {
                    m_flowVariablePrefixTextField.setEnabled(true);
                } else {
                    m_flowVariablePrefixTextField.setEnabled(false);
                }

            }
        });
        m_flowvariableBox.doClick(); // sync states

        JPanel thirdColumnPanel = new JPanel(new GridLayout(1, 1));
        thirdColumnPanel.setBorder(BorderFactory.createTitledBorder("Provide scores as flow variables"));
        flowLayout = new JPanel(new FlowLayout());
        flowLayout.add(m_flowvariableBox);
        flowLayout.add(m_flowVariablePrefixTextField);
        thirdColumnPanel.add(flowLayout);

        p.add(firstColumnPanel);

        p.add(secondColumnPanel);

        p.add(m_sortingOptions);

        p.add(thirdColumnPanel);

        final ItemListener colChangeListener = new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                DataColumnSpec specFirst = m_firstColumns.getSelectedColumnAsSpec();
                DataColumnSpec specSecond = m_secondColumns.getSelectedColumnAsSpec();
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
        };

        m_firstColumns.addItemListener(colChangeListener);
        m_secondColumns.addItemListener(colChangeListener);
        m_sortingOptions.updateControls();

        if (m_missingValueOption) {
            ButtonGroup missingValueGroup = new ButtonGroup();
            missingValueGroup.add(m_ignoreMissingValues);
            missingValueGroup.add(m_failOnMissingValues);
            JPanel missingValues = new JPanel(new GridBagLayout());
            missingValues.setBorder(new TitledBorder("Missing values"));
            GridBagConstraints gbc = new GridBagConstraints();
            JLabel label = new JLabel("In case of missing values...");
            gbc.gridx = 0;
            gbc.gridy = 0;
            missingValues.add(label, gbc);
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.gridx = 1;
            missingValues.add(m_ignoreMissingValues, gbc);
            gbc.gridy = 1;
            missingValues.add(m_failOnMissingValues, gbc);
            p.add(missingValues);
        }
        super.addTab("Scorer", p);
    } // ScorerNodeDialog(NodeModel)

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
