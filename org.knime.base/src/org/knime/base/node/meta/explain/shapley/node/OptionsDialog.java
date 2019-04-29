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
 *   14.03.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley.node;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

import com.google.common.collect.Sets;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class OptionsDialog {

    private final DataColumnSpecFilterPanel m_featureColumns = new DataColumnSpecFilterPanel();

    private final DataColumnSpecFilterPanel m_predictionColumns = new DataColumnSpecFilterPanel();

    private final JSpinner m_iterationsPerFeature =
        new JSpinner(new SpinnerNumberModel(1000, 1, Integer.MAX_VALUE, 1000));

    private final JTextField m_seedBox = new JTextField();

    private final JButton m_newSeedBtn = new JButton("New");

    private final JCheckBox m_useSeed = new JCheckBox("Use seed");

    private final JCheckBox m_treatCollectionsAsSingleFeature =
        new JCheckBox("Every collection column represents a single feature");

    private final JCheckBox m_dontUseElementNames = new JCheckBox("Don't use element names for collection features");

    /**
     *
     */
    public OptionsDialog() {
        setupListeners();
    }

    private void setupListeners() {
        m_newSeedBtn.addActionListener(e -> m_seedBox.setText(new Random().nextLong() + ""));
        m_useSeed.addActionListener(e -> reactToUseSeedCheckBox());
    }

    private void reactToUseSeedCheckBox() {
        final boolean useSeed = m_useSeed.isSelected();
        m_seedBox.setEnabled(useSeed);
        m_newSeedBtn.setEnabled(useSeed);
    }

    JPanel getPanel() {
        // === Options Tab ===

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createGbc();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(createFeaturePanel(), gbc);

        gbc.gridy++;
        m_predictionColumns.setBorder(BorderFactory.createTitledBorder("Target columns"));
        panel.add(m_predictionColumns, gbc);

        gbc.gridy++;

        panel.add(createSamplingOptionsPanel(), gbc);

        gbc.gridy++;
        panel.add(createOutputOptionsPanel(), gbc);

        return panel;
    }

    private JPanel createSamplingOptionsPanel() {
        final GridBagConstraints gbc = createGbc();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Sampling Options"));
        addComponent(panel, gbc, "Iterations per feature", m_iterationsPerFeature);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        panel.add(m_useSeed, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        panel.add(m_seedBox, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        panel.add(m_newSeedBtn, gbc);
        return panel;
    }

    private JPanel createOutputOptionsPanel() {
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Output options"));
        panel.add(m_dontUseElementNames);
        return panel;
    }

    private static GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }

    private JPanel createFeaturePanel() {
        final GridBagConstraints gbc = createGbc();
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Feature columns"));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(m_featureColumns, gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        panel.add(m_treatCollectionsAsSingleFeature, gbc);
        return panel;
    }

    private static void addComponent(final JPanel panel, final GridBagConstraints gbc, final String label,
        final Component component) {
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx++;
        panel.add(component, gbc);
    }

    void saveSettingsTo(final ShapleyValuesSettings cfg) throws InvalidSettingsException {
        checkDisjunctFeatureAndPredictionCols();
        m_featureColumns.saveConfiguration(cfg.getFeatureCols());
        m_predictionColumns.saveConfiguration(cfg.getPredictionCols());
        cfg.setIterationsPerFeature((int)m_iterationsPerFeature.getValue());
        cfg.setSeed(getSeedAsLong());
        cfg.setUseSeed(m_useSeed.isSelected());
        cfg.setTreatCollectionsAsSingleFeature(m_treatCollectionsAsSingleFeature.isSelected());
        cfg.setDontUseElementNames(m_dontUseElementNames.isSelected());
    }

    private long getSeedAsLong() throws InvalidSettingsException {
        final String longStr = m_seedBox.getText();
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            throw new InvalidSettingsException("The provided seed must be a long.");
        }
    }

    void loadSettingsFrom(final ShapleyValuesSettings cfg, final DataTableSpec inSpec) throws NotConfigurableException {
        m_featureColumns.loadConfiguration(cfg.getFeatureCols(), inSpec);
        m_predictionColumns.loadConfiguration(cfg.getPredictionCols(), inSpec);
        m_iterationsPerFeature.setValue(cfg.getIterationsPerFeature());
        m_treatCollectionsAsSingleFeature.setSelected(cfg.isTreatAllColumnsAsSingleFeature());
        m_seedBox.setText(cfg.getSeed() + "");
        m_useSeed.setSelected(cfg.getUseSeed());
        reactToUseSeedCheckBox();
        m_dontUseElementNames.setSelected(cfg.isDontUseElementNames());
    }

    private void checkDisjunctFeatureAndPredictionCols() throws InvalidSettingsException {
        final Set<String> features = m_featureColumns.getIncludedNamesAsSet();
        final Set<String> predictions = m_predictionColumns.getIncludedNamesAsSet();
        final Set<String> intersection = Sets.intersection(features, predictions);
        CheckUtils.checkSetting(intersection.isEmpty(),
            "The following column(s) are used as both feature and prediction column which is not allowed: %s",
            intersection);
    }
}
