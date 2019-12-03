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
 *   Dec 18, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.domain.editprobability;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.preproc.domain.editnominal.EditNominalDomainDialog;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionCellFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
final class EditProbabilityDistributionDomainNodeDialogPane extends NodeDialogPane {

    private final EditNominalDomainDialog m_editNominalDomainDialog;

    private final EditEpsilonConfiguration m_epsilonConfiguration;

    private final JSpinner m_epsilonSpinner;

    private static class NominalProbabilityTypeHandler implements EditNominalDomainDialog.TypeHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getClassType() {
            return NominalDistributionCellFactory.TYPE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ColumnFilter getColumnFilter() {
            return new ColumnFilter() {
                @Override
                public boolean includeColumn(final DataColumnSpec name) {
                    return name.getType().isCompatible(NominalDistributionValue.class);
                }

                @Override
                public String allFilteredMsg() {
                    return "";
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Set<DataCell>> getPossibleValues(final DataColumnSpec spec) {
            try {
                return Optional.ofNullable(NominalDistributionValueMetaData.extractFromSpec(spec).getValues().stream().map(StringCell::new)
                    .collect(Collectors.toSet()));
            }catch(IllegalStateException e) {
                return Optional.empty();
            }
        }
    }

    public EditProbabilityDistributionDomainNodeDialogPane() {
        m_editNominalDomainDialog = new EditNominalDomainDialog(new NominalProbabilityTypeHandler(), true);
        m_epsilonConfiguration = new EditEpsilonConfiguration();
        m_epsilonSpinner = new JSpinner(new SpinnerNumberModel(4, 1, Integer.MAX_VALUE, 1));
        addTab("Edit Domain Values", createProbabilityDistributionPanel());
    }

    private JPanel createProbabilityDistributionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(m_editNominalDomainDialog.getPanel(), c);
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        panel.add(createEpsilonSpinnerPanel(), c);
        return panel;

    }

    private JPanel createEpsilonSpinnerPanel() {
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        spinnerPanel.setBorder(BorderFactory.createTitledBorder("Precision"));
        JLabel epsilonLabel = new JLabel("Precision (number of decimal digits)");
        spinnerPanel.add(epsilonLabel);
        m_epsilonSpinner.setPreferredSize(new Dimension(100,20));
        spinnerPanel.add(m_epsilonSpinner);
        return spinnerPanel;
    }

    private void setEpsilonValue() {
        m_epsilonConfiguration.setEpsilonValue(getEpsilonValue());
    }

    private int getEpsilonValue() {
        return Integer.parseInt(m_epsilonSpinner.getValue().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_editNominalDomainDialog.save(settings);
        setEpsilonValue();
        m_epsilonConfiguration.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_editNominalDomainDialog.load(settings, specs[0]);
        m_epsilonConfiguration.loadConfigurationInDialog(settings);
        m_epsilonSpinner.setValue(m_epsilonConfiguration.getEpsilonValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {
        m_editNominalDomainDialog.close();
    }

}
