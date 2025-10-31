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
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner4;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.LearningRateStrategies;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Prior;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Solver;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * Dialog for the logistic regression learner.
 *
 * @author Heiko Hofer
 * @author Gabor Bakos
 * @author Adrian Nembach, KNIME.com
 * @since 3.1
 */
public final class LogRegLearnerNodeDialogPane extends NodeDialogPane {

    private static int NUMBER_INPUT_FIELD_COLS = 10;

    static final Long DEFAULT_RANDOM_SEED = System.currentTimeMillis();

    private DataColumnSpecFilterPanel m_filterPanel;

    private JComboBox<DataCell> m_targetReferenceCategory;

    private JCheckBox m_notSortTarget;

    private ColumnSelectionPanel m_selectionPanel;

    private JCheckBox m_notSortIncludes;

    private DataTableSpec m_inSpec;

    // new in version 3.4
    private JComboBox<Solver> m_solverComboBox;

    private JCheckBox m_lazyCalculationCheckBox;

    private JSpinner m_maxEpochSpinner;

    private JCheckBox m_calcCovMatrixCheckBox;

    private JTextField m_epsilonField;

    private JComboBox<LearningRateStrategies> m_learningRateStrategyComboBox;

    private JTextField m_initialLearningRateField;

    private JComboBox<Prior> m_priorComboBox;

    private JSpinner m_priorVarianceSpinner;

    private JCheckBox m_inMemoryCheckBox;

    private JCheckBox m_useSeedCheckBox;

    private JTextField m_seedField;

    private JButton m_newSeedButton;

    private JSpinner m_chunkSizeSpinner;

    /**
     * Create new dialog for linear regression model.
     */
    public LogRegLearnerNodeDialogPane() {
        super();
        // instantiate members
        final ColumnSelectionPanel columnSelectionPanel =
            new ColumnSelectionPanel(new EmptyBorder(0, 0, 0, 0), NominalValue.class, NominalDistributionValue.class);
        m_selectionPanel = columnSelectionPanel;

        m_targetReferenceCategory = new JComboBox<>();
        m_filterPanel = new DataColumnSpecFilterPanel(false);
        m_notSortTarget =
            new JCheckBox("Use order from target column domain (only relevant for output representation)");
        m_notSortIncludes = new JCheckBox("Use order from column domain (applies only to nominal columns). "
            + "First value is chosen as reference for dummy variables.");

        m_lazyCalculationCheckBox =
            new JCheckBox("Perform calculations lazily (more memory expensive but often faster)");
        m_calcCovMatrixCheckBox = new JCheckBox("Calculate statistics for coefficients");
        m_maxEpochSpinner =
            new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_MAX_EPOCH, 1, Integer.MAX_VALUE, 1));
        m_epsilonField =
            new JTextField(Double.toString(LogRegLearnerSettings.DEFAULT_EPSILON), NUMBER_INPUT_FIELD_COLS);
        m_initialLearningRateField =
            new JTextField(Double.toString(LogRegLearnerSettings.DEFAULT_EPSILON), NUMBER_INPUT_FIELD_COLS);
        m_priorVarianceSpinner = new JSpinner(
            new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_PRIOR_VARIANCE, Double.MIN_VALUE, 1e3, 0.1));
        m_priorComboBox = new JComboBox<>(Prior.values());
        m_learningRateStrategyComboBox = new JComboBox<>(LearningRateStrategies.values());
        m_solverComboBox = new JComboBox<>(Solver.values());

        m_inMemoryCheckBox = new JCheckBox("Hold data in memory");
        m_useSeedCheckBox = new JCheckBox("Use seed");
        m_seedField = new JTextField(NUMBER_INPUT_FIELD_COLS);
        m_newSeedButton = new JButton("New");
        m_chunkSizeSpinner =
            new JSpinner(new SpinnerNumberModel(LogRegLearnerSettings.DEFAULT_CHUNK_SIZE, 1, Integer.MAX_VALUE, 1000));

        // register listeners
        m_selectionPanel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                updateTargetCategories((DataCell)m_targetReferenceCategory.getSelectedItem());
                updateFilterPanel();
            }
        });
        m_newSeedButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_seedField.setText(Long.toString(new Random().nextLong()));

            }
        });
        m_useSeedCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleSeedComponents();
            }
        });

        m_solverComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                Solver selected = (Solver)m_solverComboBox.getSelectedItem();
                solverChanged(selected);
            }
        });

        m_priorComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                Prior selected = (Prior)m_priorComboBox.getSelectedItem();
                m_priorVarianceSpinner.setEnabled(selected.hasVariance());
            }
        });

        m_learningRateStrategyComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                LearningRateStrategies selected =
                    (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
                enforceLRSCompatibilities(selected);
            }

        });

        m_inMemoryCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_chunkSizeSpinner.setEnabled(!m_inMemoryCheckBox.isSelected());
            }
        });

        // create tabs
        JPanel settingsPanel = createSettingsPanel();
        addTab("Settings", settingsPanel);
        JPanel advancedSettingsPanel = createAdvancedSettingsPanel();
        addTab("Advanced", advancedSettingsPanel);
    }

    private void toggleSeedComponents() {
        m_seedField.setEnabled(m_useSeedCheckBox.isSelected());
        m_newSeedButton.setEnabled(m_useSeedCheckBox.isSelected());
    }

    private void updateFilterPanel() {
        DataColumnSpec targetSpec = m_selectionPanel.getSelectedColumnAsSpec();
        m_filterPanel.resetHiding();
        m_filterPanel.hideNames(targetSpec);
    }

    private void enforcePriorCompatibilities(final Prior prior) {
        m_priorVarianceSpinner.setEnabled(prior.hasVariance());
    }

    private void enforceLRSCompatibilities(final LearningRateStrategies lrs) {
        m_initialLearningRateField.setEnabled(lrs.hasInitialValue());
    }

    private void solverChanged(final Solver solver) {

        boolean sgMethod = solver != Solver.IRLS;
        if (sgMethod) {
            setEnabledSGRelated(true);
            m_lazyCalculationCheckBox.setEnabled(solver.supportsLazy());

            ComboBoxModel<Prior> oldPriorModel = m_priorComboBox.getModel();
            EnumSet<Prior> compatiblePriors = solver.getCompatiblePriors();
            Prior oldSelectedPrior = (Prior)oldPriorModel.getSelectedItem();
            m_priorComboBox
                .setModel(new DefaultComboBoxModel<>(compatiblePriors.toArray(new Prior[compatiblePriors.size()])));
            Prior newSelectedPrior;
            if (compatiblePriors.contains(oldSelectedPrior)) {
                m_priorComboBox.setSelectedItem(oldSelectedPrior);
                newSelectedPrior = oldSelectedPrior;
            } else {
                newSelectedPrior = (Prior)m_priorComboBox.getSelectedItem();
                // TODO warn user that the prior selection changed
            }
            enforcePriorCompatibilities(newSelectedPrior);

            LearningRateStrategies oldSelectedLRS =
                (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
            EnumSet<LearningRateStrategies> compatibleLRS = solver.getCompatibleLearningRateStrategies();
            m_learningRateStrategyComboBox.setModel(
                new DefaultComboBoxModel<>(compatibleLRS.toArray(new LearningRateStrategies[compatibleLRS.size()])));
            LearningRateStrategies newSelectedLRS =
                (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
            if (compatibleLRS.contains(oldSelectedLRS)) {
                m_learningRateStrategyComboBox.setSelectedItem(oldSelectedLRS);
                newSelectedLRS = oldSelectedLRS;
            } else {
                newSelectedLRS = (LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem();
                // TODO warn user that the selected learning rate strategy changed
            }
            enforceLRSCompatibilities(newSelectedLRS);
        } else {
            setEnabledSGRelated(false);
        }
    }

    private void setEnabledSGRelated(final boolean enable) {
        m_lazyCalculationCheckBox.setEnabled(enable);
        m_learningRateStrategyComboBox.setEnabled(enable);
        m_initialLearningRateField.setEnabled(enable);
        m_priorComboBox.setEnabled(enable);
        m_priorVarianceSpinner.setEnabled(enable);
    }

    private JPanel createAdvancedSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);

        JPanel solverOptionsPanel = createSolverOptionsPanel();
        solverOptionsPanel.setBorder(BorderFactory.createTitledBorder("Solver options"));
        panel.add(solverOptionsPanel, c);

        c.gridy++;

        JPanel terminationConditionsPanel = createTerminationConditionsPanel();
        terminationConditionsPanel.setBorder(BorderFactory.createTitledBorder("Termination conditions"));
        panel.add(terminationConditionsPanel, c);

        c.gridy++;

        JPanel learningRateStrategyPanel = createLearningRateStrategyPanel();
        learningRateStrategyPanel.setBorder(BorderFactory.createTitledBorder("Learning rate / step size"));
        panel.add(learningRateStrategyPanel, c);

        c.gridy++;
        JPanel priorPanel = createPriorPanel();
        priorPanel.setBorder(BorderFactory.createTitledBorder("Regularization"));
        panel.add(priorPanel, c);

        c.gridy++;
        JPanel dataHandlingPanel = createDataHandlingPanel();
        dataHandlingPanel.setBorder(BorderFactory.createTitledBorder("Data handling"));
        panel.add(dataHandlingPanel, c);

        return panel;
    }

    private JPanel createTerminationConditionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Maximal number of epochs:"), c);
        c.gridx++;
        panel.add(m_maxEpochSpinner, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Epsilon:"), c);
        c.gridx++;
        panel.add(m_epsilonField, c);

        return panel;
    }

    private JPanel createSolverOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        c.gridwidth = 2;
        panel.add(m_lazyCalculationCheckBox, c);
        c.gridy++;
        panel.add(m_calcCovMatrixCheckBox, c);

        return panel;
    }

    private JPanel createPriorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Prior:"), c);
        c.gridx++;
        panel.add(m_priorComboBox, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Variance:"), c);
        c.gridx++;
        panel.add(m_priorVarianceSpinner, c);

        return panel;
    }

    private JPanel createDataHandlingPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();
        c.gridwidth = 3;
        panel.add(m_inMemoryCheckBox, c);
        c.gridy++;
        c.gridwidth = 1;
        panel.add(new JLabel("Chunk size:"), c);
        c.gridx++;
        panel.add(m_chunkSizeSpinner, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        panel.add(m_useSeedCheckBox, c);
        c.gridy++;
        c.gridwidth = 1;
        panel.add(new JLabel("Seed:"), c);
        c.gridx++;
        panel.add(m_seedField, c);
        c.gridx++;
        panel.add(m_newSeedButton, c);

        return panel;
    }

    private static GridBagConstraints makeSettingsConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);
        return c;
    }

    private JPanel createLearningRateStrategyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        panel.add(new JLabel("Learning rate strategy:"), c);
        c.gridx++;
        panel.add(m_learningRateStrategyComboBox, c);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel("Step size:"), c);
        c.gridx++;
        panel.add(m_initialLearningRateField, c);

        return panel;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);
        JPanel northPanel = createTargetOptionsPanel();
        northPanel.setBorder(BorderFactory.createTitledBorder("Target"));
        panel.add(northPanel, c);
        c.gridy++;

        JPanel centerPanel = createSolverPanel();
        centerPanel.setBorder(BorderFactory.createTitledBorder("Solver"));
        panel.add(centerPanel, c);
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;

        JPanel southPanel = createIncludesPanel();
        southPanel.setBorder(BorderFactory.createTitledBorder("Feature selection"));
        panel.add(southPanel, c);
        return panel;
    }

    private JPanel createSolverPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();
        p.add(new JLabel("Select solver:"), c);
        c.gridx++;
        c.weightx = 1;
        p.add(m_solverComboBox, c);
        return p;
    }

    /**
     * Create options panel for the target.
     */
    private JPanel createTargetOptionsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = makeSettingsConstraints();

        p.add(new JLabel("Target column:"), c);

        c.gridx++;

        p.add(m_selectionPanel, c);

        c.gridx = 0;
        c.gridy++;
        p.add(new JLabel("Reference category:"), c);

        c.gridx++;
        p.add(m_targetReferenceCategory, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 3;
        c.weightx = 1;
        p.add(m_notSortTarget, c);

        return p;
    }

    /**
     * Create options panel for the included columns.
     */
    private JPanel createIncludesPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.BASELINE_LEADING;
        c.insets = new Insets(5, 5, 0, 0);

        p.add(m_filterPanel, c);

        c.gridy++;

        p.add(m_notSortIncludes, c);

        return p;
    }

    /**
     * Update list of target categories.
     */
    private void updateTargetCategories(final DataCell selectedItem) {
        m_targetReferenceCategory.removeAllItems();

        String selectedColumn = m_selectionPanel.getSelectedColumn();
        if (selectedColumn != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selectedColumn);
            if (null != colSpec) {
                // select last as default
                DataCell newSelectedItem = null;
                DataCell lastItem = null;
                final Collection<DataCell> values = getValues(colSpec);
                if (!values.isEmpty()) {
                    for (DataCell cell : values) {
                        m_targetReferenceCategory.addItem(cell);
                        lastItem = cell;
                        if (cell.equals(selectedItem)) {
                            newSelectedItem = selectedItem;
                        }
                    }
                    if (newSelectedItem == null) {
                        newSelectedItem = lastItem;
                    }
                    m_targetReferenceCategory.getModel().setSelectedItem(newSelectedItem);
                }
            }
        }
        m_targetReferenceCategory.setEnabled(m_targetReferenceCategory.getModel().getSize() > 0);
    }

    private static Collection<DataCell> getValues(final DataColumnSpec targetSpec) {
        if (targetSpec.getType().isCompatible(NominalDistributionValue.class)) {
            return NominalDistributionValueMetaData.extractFromSpec(targetSpec).getValues().stream()
                .map(StringCell::new).collect(Collectors.toList());
        } else {
            final DataColumnDomain domain = targetSpec.getDomain();
            return domain.hasValues() ? domain.getValues() : Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO s, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final LogRegLearnerSettings settings = new LogRegLearnerSettings();
        m_inSpec = (DataTableSpec)specs[0];
        settings.loadSettingsForDialog(s, m_inSpec);
        final DataColumnSpecFilterConfiguration config = settings.getIncludedColumns();
        m_filterPanel.loadConfiguration(config, m_inSpec);

        String target = settings.getTargetColumn();

        m_selectionPanel.update(m_inSpec, target);
        //m_filterPanel.updateWithNewConfiguration(config); is not enough, we have to reload things as selection update might change the UI
        m_filterPanel.loadConfiguration(config, m_inSpec);
        // must hide the target from filter panel
        // updating m_filterPanel first does not work as the first
        // element in the spec will always be in the exclude list.
        String selected = m_selectionPanel.getSelectedColumn();
        if (null == selected) {
            for (DataColumnSpec colSpec : m_inSpec) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    selected = colSpec.getName();
                    break;
                }
            }
        }
        if (selected != null) {
            DataColumnSpec colSpec = m_inSpec.getColumnSpec(selected);
            m_filterPanel.hideNames(colSpec);
        }

        updateTargetCategories(settings.getTargetReferenceCategory());

        m_notSortTarget.setSelected(settings.getUseTargetDomainOrder());
        m_notSortIncludes.setSelected(settings.getUseFeatureDomainOrder());
        Solver solver = settings.getSolver();
        m_solverComboBox.setSelectedItem(solver);
        if (solver == Solver.IRLS) {
            setEnabledSGRelated(false);
        }
        m_maxEpochSpinner.setValue(settings.getMaxEpoch());
        m_lazyCalculationCheckBox.setSelected(settings.isPerformLazy());
        m_calcCovMatrixCheckBox.setSelected(settings.isCalcCovMatrix());
        double epsilon = settings.getEpsilon();
        m_epsilonField.setText(Double.toString(epsilon));
        m_learningRateStrategyComboBox.setSelectedItem(settings.getLearningRateStrategy());
        m_initialLearningRateField.setText(Double.toString(settings.getInitialLearningRate()));
        m_priorComboBox.setSelectedItem(settings.getPrior());
        m_priorVarianceSpinner.setValue(settings.getPriorVariance());

        m_inMemoryCheckBox.setSelected(settings.isInMemory());
        Long seed = settings.getSeed();
        toggleSeedComponents();
        m_seedField.setText(Long.toString(seed != null ? seed : DEFAULT_RANDOM_SEED));
        m_chunkSizeSpinner.setValue(settings.getChunkSize());
        m_chunkSizeSpinner.setEnabled(!settings.isInMemory());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO s) throws InvalidSettingsException {
        final LogRegLearnerSettings settings = new LogRegLearnerSettings();
        final DataColumnSpecFilterConfiguration config = LogRegLearnerNodeModel.createDCSFilterConfiguration();
        m_filterPanel.saveConfiguration(config);
        settings.setIncludedColumns(config);
        settings.setTargetColumn(m_selectionPanel.getSelectedColumn());
        settings.setTargetReferenceCategory((DataCell)m_targetReferenceCategory.getSelectedItem());
        settings.setUseTargetDomainOrder(m_notSortTarget.isSelected());
        settings.setUseFeatureDomainOrder(m_notSortIncludes.isSelected());
        settings.setSolver((Solver)m_solverComboBox.getSelectedItem());
        settings.setMaxEpoch((int)m_maxEpochSpinner.getValue());
        settings.setPerformLazy(m_lazyCalculationCheckBox.isSelected());
        settings.setCalcCovMatrix(m_calcCovMatrixCheckBox.isSelected());
        try {
            String str = m_epsilonField.getText();
            double epsilon = Double.valueOf(str);
            settings.setEpsilon(epsilon);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Please provide a valid value for epsilon.");
        }
        settings.setLearningRateStrategy((LearningRateStrategies)m_learningRateStrategyComboBox.getSelectedItem());
        try {
            String str = m_initialLearningRateField.getText();
            double lr = Double.valueOf(str);
            settings.setInitialLearningRate(lr);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException("Please provide a valid value for the initial learning rate.");
        }
        settings.setPrior((Prior)m_priorComboBox.getSelectedItem());
        settings.setPriorVariance((double)m_priorVarianceSpinner.getValue());

        settings.setInMemory(m_inMemoryCheckBox.isSelected());
        Long seed;
        if (m_useSeedCheckBox.isSelected()) {
            final String seedText = m_seedField.getText();
            try {
                seed = Long.valueOf(seedText);
            } catch (Exception e) {
                throw new InvalidSettingsException("Unable to parse seed \"" + seedText + "\"", e);
            }
        } else {
            seed = null;
        }
        settings.setSeed(seed);

        settings.setChunkSize((int)m_chunkSizeSpinner.getValue());

        settings.validate();

        settings.saveSettings(s);
    }
}
