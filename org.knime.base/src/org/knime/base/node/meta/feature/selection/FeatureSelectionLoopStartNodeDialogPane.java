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
 *   15.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategies.Strategy;
import org.knime.base.node.meta.feature.selection.FeatureSelectionStrategies.StrategyType;
import org.knime.base.node.meta.feature.selection.genetic.CrossoverStrategy;
import org.knime.base.node.meta.feature.selection.genetic.SelectionStrategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 *
 * @author Adrian Nembach, KNIME.com
 * @author Simon Schmid, KNIME, Austin, USA
 */
public class FeatureSelectionLoopStartNodeDialogPane extends NodeDialogPane {

    // General components

    private final DataColumnSpecFilterPanel m_constantColumnsFilterPanel;

    private final JComboBox<Strategy> m_strategyComboBox;

    // Components for forward feature selection and backwards feature elimination

    private final JSpinner m_nrFeaturesThresholdSpinner;

    private final JLabel m_nrFeaturesLabel;

    private final JCheckBox m_useNrFeaturesThresholdCheckBox;

    // Components for genetic algorithm

    private final JCheckBox m_useNrFeaturesLowerBoundCheckBox;

    private final JSpinner m_nrFeaturesLowerBoundSpinner;

    private final JCheckBox m_useNrFeaturesUpperBoundCheckBox;

    private final JSpinner m_nrFeaturesUpperBoundSpinner;

    private final JSpinner m_popSizeSpinner;

    private final JSpinner m_maxNumGenerationsSpinner;

    private final JCheckBox m_useRandomSeedCheckBox;

    private final JTextField m_randomSeedTextField;

    private final JCheckBox m_useEarlyStopping;

    private final JSpinner m_earlyStoppingNumGenerations;

    private final JComboBox<CrossoverStrategy> m_crossoverStrategyComboBox;

    private final JComboBox<SelectionStrategy> m_selectionStrategyComboBox;

    private final JSpinner m_survivorsFractionSpinner;

    private final JSpinner m_crossoverRateSpinner;

    private final JSpinner m_mutationRateSpinner;

    private final JSpinner m_elitismRateSpinner;

    /**
     *
     */
    public FeatureSelectionLoopStartNodeDialogPane() {
        m_constantColumnsFilterPanel = new DataColumnSpecFilterPanel();
        m_constantColumnsFilterPanel.setExcludeTitle("Static Columns");
        m_constantColumnsFilterPanel.setIncludeTitle("Variable Columns ('Features')");
        m_strategyComboBox = new JComboBox<Strategy>(Strategy.values());
        m_nrFeaturesThresholdSpinner = new JSpinner(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 1));
        m_useNrFeaturesThresholdCheckBox = new JCheckBox("Use threshold for number of features");
        m_nrFeaturesLabel = new JLabel("Threshold for number of features in subset");
        m_useNrFeaturesThresholdCheckBox.addChangeListener(e -> {
            final boolean enable = m_useNrFeaturesThresholdCheckBox.isSelected();
            m_nrFeaturesThresholdSpinner.setEnabled(enable);
            m_nrFeaturesLabel.setEnabled(enable);
        });
        m_useNrFeaturesThresholdCheckBox.doClick();
        m_useNrFeaturesLowerBoundCheckBox = new JCheckBox("Use lower bound for number of features");
        m_nrFeaturesLowerBoundSpinner = new JSpinner(new SpinnerNumberModel(2, 2, Integer.MAX_VALUE, 1));
        m_useNrFeaturesLowerBoundCheckBox.addChangeListener(
            l -> m_nrFeaturesLowerBoundSpinner.setEnabled(m_useNrFeaturesLowerBoundCheckBox.isSelected()));
        m_nrFeaturesLowerBoundSpinner.setEnabled(m_useNrFeaturesLowerBoundCheckBox.isSelected());
        m_useNrFeaturesUpperBoundCheckBox = new JCheckBox("Use upper bound for number of features");
        m_nrFeaturesUpperBoundSpinner = new JSpinner(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 1));
        m_useNrFeaturesUpperBoundCheckBox.addChangeListener(
            l -> m_nrFeaturesUpperBoundSpinner.setEnabled(m_useNrFeaturesUpperBoundCheckBox.isSelected()));
        m_nrFeaturesUpperBoundSpinner.setEnabled(m_useNrFeaturesUpperBoundCheckBox.isSelected());
        m_popSizeSpinner = new JSpinner(new SpinnerNumberModel(10, 2, Integer.MAX_VALUE, 1));
        m_maxNumGenerationsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, Integer.MAX_VALUE, 1));
        m_selectionStrategyComboBox = new JComboBox<>(SelectionStrategy.values());
        m_survivorsFractionSpinner = new JSpinner(new SpinnerNumberModel(0.4, 0.0, 1.0, 0.1));
        m_useRandomSeedCheckBox = new JCheckBox("Use static random seed");
        m_randomSeedTextField = new JTextField(10);
        m_randomSeedTextField.setEnabled(m_useRandomSeedCheckBox.isSelected());
        m_randomSeedTextField.setText(Long.toString(System.currentTimeMillis()));
        m_useRandomSeedCheckBox
            .addChangeListener(l -> m_randomSeedTextField.setEnabled(m_useRandomSeedCheckBox.isSelected()));
        m_useEarlyStopping = new JCheckBox("Enable early stopping", true);
        m_earlyStoppingNumGenerations = new JSpinner(new SpinnerNumberModel(3, 1, Integer.MAX_VALUE, 1));
        m_crossoverStrategyComboBox = new JComboBox<>(CrossoverStrategy.values());
        m_crossoverRateSpinner = new JSpinner(new SpinnerNumberModel(0.6, 0.0, 1.0, 0.1));
        m_mutationRateSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 1.0, 0.1));
        m_elitismRateSpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 1.0, 0.1));

        layout();
    }

    private void layout() {
        // === Options Tab ===

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(new JLabel(
            "<html>" + "<body>" + "The list on the left contains 'static' columns such as the target column. <br />"
                + " The columns to choose from need to be in the list on the right." + "</body></html>"),
            gbc);
        gbc.gridy += 1;
        gbc.weighty = 1;
        panel.add(m_constantColumnsFilterPanel, gbc);
        gbc.weighty = 0;
        gbc.weightx = 0;

        gbc.gridwidth = 1;
        gbc.gridy += 1;
        panel.add(new JLabel("Feature selection strategy"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(m_strategyComboBox, gbc);

        final CardLayout cardLayout = new CardLayout();
        final JPanel panelSettings = new JPanel(cardLayout);
        for (final StrategyType strategyType : StrategyType.values()) {
            panelSettings.add(getSettingsPanel(strategyType), strategyType.toString());
        }
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        panel.add(panelSettings, gbc);

        addTab("Options", panel);

        // === Advanced Options Tab ===

        final CardLayout cardLayoutAdvanced = new CardLayout();
        final JPanel panelSettingsAdvanced = new JPanel(cardLayoutAdvanced);
        for (final StrategyType strategyType : StrategyType.values()) {
            panelSettingsAdvanced.add(getAdvancedSettingsPanel(strategyType), strategyType.toString());
        }

        final String tabAdvancedTitle = "Advanced Options";
        addTab(tabAdvancedTitle, panelSettingsAdvanced);

        // === Action Listeners ===
        m_strategyComboBox.addActionListener(l -> {
            cardLayout.show(panelSettings, ((Strategy)m_strategyComboBox.getSelectedItem()).getType().toString());
            cardLayoutAdvanced.show(panelSettingsAdvanced,
                ((Strategy)m_strategyComboBox.getSelectedItem()).getType().toString());
            setEnabled(((Strategy)m_strategyComboBox.getSelectedItem()).getType().hasAdvancedSettings(), tabAdvancedTitle);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final FeatureSelectionLoopStartSettings cfg = new FeatureSelectionLoopStartSettings();
        if (m_constantColumnsFilterPanel.getExcludeList().isEmpty()) {
            throw new InvalidSettingsException(
                "All input columns are declared as constant. Please make at least one column non constant.");
        }
        final int sizeIncludes = m_constantColumnsFilterPanel.getIncludeList().size();
        if (sizeIncludes < 1) {
            throw new InvalidSettingsException("At least one column must be included!");
        }
        if (m_strategyComboBox.getSelectedItem() == FeatureSelectionStrategies.Strategy.GeneticAlgorithm) {
            if (sizeIncludes < 1) {
                throw new InvalidSettingsException(
                    "In order to use a genetic algorithm, at least one columns must be included!");
            }
            if (m_useNrFeaturesLowerBoundCheckBox.isSelected() && m_useNrFeaturesUpperBoundCheckBox.isSelected()
                && (int)m_nrFeaturesLowerBoundSpinner.getValue() > (int)m_nrFeaturesUpperBoundSpinner.getValue()) {
                throw new InvalidSettingsException(
                    "The lower bound of number of features must not be greater than the upper bound!");
            }
        }

        m_constantColumnsFilterPanel.saveConfiguration(cfg.getStaticColumnsFilterConfiguration());
        cfg.setStrategy((Strategy)m_strategyComboBox.getSelectedItem());
        cfg.setNrFeaturesThreshold(
            m_useNrFeaturesThresholdCheckBox.isSelected() ? (int)m_nrFeaturesThresholdSpinner.getValue() : -1);
        cfg.setNrFeaturesLowerBound(
            m_useNrFeaturesLowerBoundCheckBox.isSelected() ? (int)m_nrFeaturesLowerBoundSpinner.getValue() : -1);
        cfg.setNrFeaturesUpperBound(
            m_useNrFeaturesUpperBoundCheckBox.isSelected() ? (int)m_nrFeaturesUpperBoundSpinner.getValue() : -1);
        cfg.setEarlyStopping(m_useEarlyStopping.isSelected() ? (int)m_earlyStoppingNumGenerations.getValue() : -1);
        cfg.setPopSize((int)m_popSizeSpinner.getValue());
        cfg.setMaxNumGenerations((int)m_maxNumGenerationsSpinner.getValue());
        cfg.setUseRandomSeed(m_useRandomSeedCheckBox.isSelected());
        cfg.setRandomSeed(Long.parseLong(m_randomSeedTextField.getText()));
        cfg.setSurvivorsFraction((double)m_survivorsFractionSpinner.getValue());
        cfg.setCrossoverRate((double)m_crossoverRateSpinner.getValue());
        cfg.setMutationRate((double)m_mutationRateSpinner.getValue());
        cfg.setElitismRate((double)m_elitismRateSpinner.getValue());
        cfg.setCrossoverStrategy((CrossoverStrategy)m_crossoverStrategyComboBox.getSelectedItem());
        cfg.setSelectionStrategy((SelectionStrategy)m_selectionStrategyComboBox.getSelectedItem());
        cfg.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final FeatureSelectionLoopStartSettings cfg = new FeatureSelectionLoopStartSettings();
        cfg.loadInDialog(settings, specs[0]);
        m_constantColumnsFilterPanel.loadConfiguration(cfg.getStaticColumnsFilterConfiguration(), specs[0]);
        m_strategyComboBox.setSelectedItem(cfg.getStrategy());

        final boolean useFeatureThreshold = cfg.useNrFeaturesThreshold();
        m_useNrFeaturesThresholdCheckBox.setSelected(useFeatureThreshold);
        // only set spinner value if the threshold is used
        if (useFeatureThreshold) {
            m_nrFeaturesThresholdSpinner.setValue(cfg.getNrFeaturesThreshold());
        }
        final boolean useFeatureLowerBound = cfg.useNrFeaturesLowerBound();
        m_useNrFeaturesLowerBoundCheckBox.setSelected(useFeatureLowerBound);
        // only set spinner value if the lower bound is used
        if (useFeatureLowerBound) {
            m_nrFeaturesLowerBoundSpinner.setValue(cfg.getNrFeaturesLowerBound());
        }
        final boolean useFeatureUpperBound = cfg.useNrFeaturesUpperBound();
        m_useNrFeaturesUpperBoundCheckBox.setSelected(useFeatureUpperBound);
        // only set spinner value if the upper bound is used
        if (useFeatureUpperBound) {
            m_nrFeaturesUpperBoundSpinner.setValue(cfg.getNrFeaturesUpperBound());
        }
        final boolean useEarlyStopping = cfg.useEarlyStopping();
        m_useEarlyStopping.setSelected(useEarlyStopping);
        // only set spinner value if the early stopping is enabled
        if (useEarlyStopping) {
            m_earlyStoppingNumGenerations.setValue(cfg.getEarlyStopping());
        }
        m_popSizeSpinner.setValue(cfg.getPopSize());
        m_maxNumGenerationsSpinner.setValue(cfg.getMaxNumGenerations());
        m_useRandomSeedCheckBox.setSelected(cfg.isUseRandomSeed());
        m_randomSeedTextField.setText(Long.toString(cfg.getRandomSeed()));
        m_survivorsFractionSpinner.setValue(cfg.getSurvivorsFraction());
        m_crossoverRateSpinner.setValue(cfg.getCrossoverRate());
        m_mutationRateSpinner.setValue(cfg.getMutationRate());
        m_elitismRateSpinner.setValue(cfg.getElitismRate());
        m_crossoverStrategyComboBox.setSelectedItem(cfg.getCrossoverStrategy());
        m_selectionStrategyComboBox.setSelectedItem(cfg.getSelectionStrategy());
    }

    private JPanel getSettingsPanel(final StrategyType strategyType) {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder(LineBorder.createGrayLineBorder(), strategyType.getDialogPanelTitle()));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        switch (strategyType) {
            case Genetic:
                panel.add(m_useNrFeaturesLowerBoundCheckBox, gbc);
                gbc.gridx++;
                panel.add(m_nrFeaturesLowerBoundSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(m_useNrFeaturesUpperBoundCheckBox, gbc);
                gbc.gridx++;
                panel.add(m_nrFeaturesUpperBoundSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Population size"), gbc);
                gbc.gridx++;
                panel.add(m_popSizeSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Max. number of generations"), gbc);
                gbc.gridx++;
                panel.add(m_maxNumGenerationsSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                m_useRandomSeedCheckBox
                    .setPreferredSize(new Dimension((int)m_useRandomSeedCheckBox.getPreferredSize().getWidth() + 83,
                        (int)m_useRandomSeedCheckBox.getPreferredSize().getHeight()));
                panel.add(m_useRandomSeedCheckBox, gbc);
                gbc.gridx++;
                m_randomSeedTextField
                    .setPreferredSize(new Dimension((int)m_randomSeedTextField.getPreferredSize().getWidth() + 55,
                        (int)m_randomSeedTextField.getPreferredSize().getHeight()));
                panel.add(m_randomSeedTextField, gbc);

                return panel;
            case Sequential:
                panel.add(m_useNrFeaturesThresholdCheckBox, gbc);
                gbc.gridy++;
                panel.add(new JLabel("Select threshold for number of features"), gbc);
                gbc.gridx = 1;
                panel.add(m_nrFeaturesThresholdSpinner, gbc);
                gbc.weighty = 1;
                gbc.gridy++;
                panel.add(new JLabel(""));
                return panel;
            default:
                throw new IllegalStateException("Unexpected strategy: " + strategyType.toString());
        }
    }

    private JPanel getAdvancedSettingsPanel(final StrategyType strategyType) {
        if (!strategyType.hasAdvancedSettings()) {
            return new JPanel();
        }
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            new TitledBorder(LineBorder.createGrayLineBorder(), "Advanced " + strategyType.getDialogPanelTitle()));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        switch (strategyType) {
            case Genetic:
                panel.add(new JLabel("Selection strategy"), gbc);
                gbc.gridx++;
                panel.add(m_selectionStrategyComboBox, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Fraction of survivors"), gbc);
                gbc.gridx++;
                panel.add(m_survivorsFractionSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Elitism rate"), gbc);
                gbc.gridx++;
                panel.add(m_elitismRateSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Crossover strategy"), gbc);
                gbc.gridx++;
                panel.add(m_crossoverStrategyComboBox, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Crossover rate"), gbc);
                gbc.gridx++;
                panel.add(m_crossoverRateSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(new JLabel("Mutation rate"), gbc);
                gbc.gridx++;
                panel.add(m_mutationRateSpinner, gbc);

                gbc.gridx = 0;
                gbc.gridy++;
                panel.add(m_useEarlyStopping, gbc);
                gbc.gridy++;
                final JLabel labelEarlyStopping = new JLabel("Number of generations without improvement");
                panel.add(labelEarlyStopping, gbc);
                gbc.gridx++;
                panel.add(m_earlyStoppingNumGenerations, gbc);
                m_useEarlyStopping.addChangeListener(l -> {
                    boolean enabled = m_useEarlyStopping.isSelected();
                    labelEarlyStopping.setEnabled(enabled);
                    m_earlyStoppingNumGenerations.setEnabled(enabled);
                });

                // add two dummy labels to keep components on top left
                gbc.gridx++;
                gbc.weightx = 1;
                panel.add(new JLabel(), gbc);
                gbc.gridy++;
                gbc.weighty = 1;
                panel.add(new JLabel(), gbc);

                return panel;
            default:
                throw new IllegalStateException("Unexpected strategy: " + strategyType.toString());
        }
    }

}
