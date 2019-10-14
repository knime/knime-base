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
 *   Aug 29, 2019 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.probdistribution.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.base.node.preproc.probdistribution.ExceptionHandling;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;

/**
 * Node dialog of the node that creates probability distributions of probability values.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
final class ProbabilityDistributionCreatorNodeDialog extends NodeDialogPane {

    //Button Group for picking between numeric and string column
    private final DialogComponentButtonGroup m_columnType = new DialogComponentButtonGroup(
        ProbabilityDistributionCreatorNodeModel.createColumnTypeModel(), null, true, ColumnType.values());

    //Numeric
    private final DialogComponentColumnFilter2 m_numericColumnFilter =
        new DialogComponentColumnFilter2(ProbabilityDistributionCreatorNodeModel.createColumnFilterModel(), 0);

    private final DialogComponentBoolean m_sumUpProbabilities =
        new DialogComponentBoolean(ProbabilityDistributionCreatorNodeModel.createPrecisionBooleanModel(),
            "Allow probabilities that sum up to 1 imprecisely");

    private final DialogComponentNumber m_precisionModel = new DialogComponentNumber(
        ProbabilityDistributionCreatorNodeModel
            .createPrecisionModel(ProbabilityDistributionCreatorNodeModel.createPrecisionBooleanModel()),
        "Precision (number of decimal digits)", 1);

    private final DialogComponentButtonGroup m_invalidHandling =
        new DialogComponentButtonGroup(ProbabilityDistributionCreatorNodeModel.createInvalidDistributionHandlingModel(),
            null, true, ExceptionHandling.values());

    //Strings
    @SuppressWarnings("unchecked")
    private final DialogComponentColumnNameSelection m_stringColumnSelection =
        new DialogComponentColumnNameSelection(ProbabilityDistributionCreatorNodeModel.createStringFilterModel(),
            "String column containing classes", 0, false, NominalValue.class);

    // General Options
    private final DialogComponentString m_outputName = new DialogComponentString(
        ProbabilityDistributionCreatorNodeModel.createColumnNameModel(), "Output column name", true, 20);

    private final DialogComponentBoolean m_includeColumns = new DialogComponentBoolean(
        ProbabilityDistributionCreatorNodeModel.createRemoveIncludedColsBooleanModel(), "Remove included columns");

    private final DialogComponentButtonGroup m_missingValueHandling =
        new DialogComponentButtonGroup(ProbabilityDistributionCreatorNodeModel.createMissingValueHandlingModel(), null,
            true, MissingValueHandling.values());

    public ProbabilityDistributionCreatorNodeDialog() {
        super();
        m_columnType.getModel().addChangeListener(e -> updateColumnType());
        JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(createNumericColumnPanel(), c);
        c.gridy++;
        c.weighty = 0;
        panel.add(createStringColumnPanel(), c);
        c.gridy++;
        panel.add(createGeneralOptionPanel(), c);
        addTab("Default Settings", panel);

    }

    private void updateColumnType() {
        final boolean isNumeric = m_columnType.getButton(ColumnType.NUMERIC_COLUMN.getActionCommand()).isSelected();
        m_numericColumnFilter.getModel().setEnabled(isNumeric);
        m_sumUpProbabilities.getModel().setEnabled(isNumeric);
        m_precisionModel.getModel().setEnabled(isNumeric);
        m_precisionModel.getLabel().setEnabled(isNumeric);
        m_invalidHandling.getModel().setEnabled(isNumeric);
        m_stringColumnSelection.getModel().setEnabled(!isNumeric);
    }

    private JPanel createNumericColumnPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Multiple Numeric Columns"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(m_columnType.getButton(ColumnType.NUMERIC_COLUMN.getActionCommand()), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(m_numericColumnFilter.getComponentPanel(), c);
        c.gridy++;
        c.weighty = 0;
        c.weightx = 0;
        panel.add(createPrecisionPanel(), c);
        c.gridy++;
        panel.add(createInvalidHandlingPanel(), c);
        return panel;
    }

    private JPanel createInvalidHandlingPanel() {
        JPanel invalidHandlingPanel = new JPanel();
        invalidHandlingPanel.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        invalidHandlingPanel.setBorder(BorderFactory.createTitledBorder("Invalid Probability Distribution Handling"));
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        invalidHandlingPanel.add(m_invalidHandling.getComponentPanel(), c);
        return invalidHandlingPanel;
    }

    private JPanel createPrecisionPanel() {
        final JPanel precisionPanel = new JPanel();
        precisionPanel.setLayout(new GridBagLayout());
        precisionPanel.setBorder(BorderFactory.createTitledBorder("Precision"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        precisionPanel.add(m_sumUpProbabilities.getComponentPanel(), c);
        c.gridy++;
        precisionPanel.add(m_precisionModel.getComponentPanel(), c);
        return precisionPanel;
    }

    private JPanel createStringColumnPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Single String Column"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        panel.add(m_columnType.getButton(ColumnType.STRING_COLUMN.getActionCommand()), c);
        c.gridy++;
        m_stringColumnSelection.getModel().setEnabled(false);
        panel.add(m_stringColumnSelection.getComponentPanel(), c);
        return panel;
    }

    private JPanel createGeneralOptionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("General"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(m_outputName.getComponentPanel(), c);
        c.gridx++;
        c.insets = new Insets(0, 20, 0, 0);
        panel.add(m_includeColumns.getComponentPanel(), c);
        c.gridx++;
        c.insets = new Insets(0, 20, 0, 0);
        JPanel missingValueComponent = m_missingValueHandling.getComponentPanel();
        missingValueComponent.setBorder(BorderFactory.createTitledBorder("Missing Value Handling"));
        missingValueComponent.setPreferredSize(new Dimension(125,100));
        panel.add(missingValueComponent, c);
        c.gridx++;
        c.weightx = 1;
        panel.add(Box.createHorizontalGlue(), c);
        return panel;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {

        m_numericColumnFilter.loadSettingsFrom(settings, specs);
        m_sumUpProbabilities.loadSettingsFrom(settings, specs);
        m_precisionModel.loadSettingsFrom(settings, specs);
        m_invalidHandling.loadSettingsFrom(settings, specs);

        m_stringColumnSelection.loadSettingsFrom(settings, specs);

        m_outputName.loadSettingsFrom(settings, specs);
        m_includeColumns.loadSettingsFrom(settings, specs);
        m_missingValueHandling.loadSettingsFrom(settings, specs);
        m_columnType.loadSettingsFrom(settings, specs);
        updateColumnType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (isStringColumnSelected() && isZeroMissingValueSelected()) {
            throw new InvalidSettingsException(
                "Invalid Missing Value Strategy for string columns. Please pick another strategy.");
        }
        m_columnType.saveSettingsTo(settings);

        m_numericColumnFilter.saveSettingsTo(settings);
        m_sumUpProbabilities.saveSettingsTo(settings);
        m_precisionModel.saveSettingsTo(settings);
        m_invalidHandling.saveSettingsTo(settings);

        m_stringColumnSelection.saveSettingsTo(settings);

        m_outputName.saveSettingsTo(settings);
        m_includeColumns.saveSettingsTo(settings);
        m_missingValueHandling.saveSettingsTo(settings);
    }

    private boolean isStringColumnSelected() {
        return m_columnType.getButton(ColumnType.STRING_COLUMN.getActionCommand()).isSelected();
    }

    private boolean isZeroMissingValueSelected() {
        return m_missingValueHandling.getButton(MissingValueHandling.ZERO.getActionCommand()).isSelected();
    }
}
