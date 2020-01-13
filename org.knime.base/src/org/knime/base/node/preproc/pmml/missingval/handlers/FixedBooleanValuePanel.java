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
 *   Jan 13, 2020 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.pmml.missingval.handlers;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.preproc.pmml.missingval.MissingValueHandlerPanel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.port.PortObjectSpec;

/**
 * This class creates a panel for substituting missing boolean values with a fixed value.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
final class FixedBooleanValuePanel extends MissingValueHandlerPanel {

    private static final long serialVersionUID = 1L;

    private final JRadioButton m_buttonTrue;

    private final JRadioButton m_buttonFalse;

    private final SettingsModelBoolean m_booleanValue =
        FixedBooleanValueMissingCellHandler.createBooleanValueSettingsModel();

    /**
     * Constructor to create the panel for the fixed boolean value
     */
    public FixedBooleanValuePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        final JLabel label = new JLabel("Value");
        panel.add(label, c);
        m_buttonTrue = new JRadioButton(Boolean.toString(true));
        m_buttonFalse = new JRadioButton(Boolean.toString(false));
        createButtonGroup();
        c.gridx++;
        panel.add(m_buttonTrue, c);
        c.gridx++;
        panel.add(m_buttonFalse, c);
        this.add(panel);
    }

    private void createButtonGroup() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(m_buttonTrue);
        buttonGroup.add(m_buttonFalse);
        m_buttonTrue.setSelected(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
         m_booleanValue.setBooleanValue(m_buttonTrue.isSelected());
         m_booleanValue.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException, InvalidSettingsException {
        if (settings.containsKey(FixedBooleanValueMissingCellHandler.FIX_VAL_CFG)) {
            m_booleanValue.loadSettingsFrom(settings);
            final boolean ifSetTrue = m_booleanValue.getBooleanValue();
            m_buttonTrue.setSelected(ifSetTrue);
            m_buttonFalse.setSelected(!ifSetTrue);
        }
    }

}
