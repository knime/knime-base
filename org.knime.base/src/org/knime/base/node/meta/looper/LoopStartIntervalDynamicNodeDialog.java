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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.02.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;

/**
 * This is the dialog for the interval looper node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopStartIntervalNodeDialog} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopStartIntervalDynamicNodeDialog extends NodeDialogPane {
    private final JTextField m_prefix = new JTextField(10);

    private final JTextField m_from = new JTextField(10);

    private final JTextField m_to = new JTextField(10);

    private final JTextField m_step = new JTextField(10);

    private final LoopStartIntervalDynamicSettings m_settings = new LoopStartIntervalDynamicSettings();

    private final DialogComponentButtonGroup m_variableType;

    /**
     * Creates a new dialog for the looper node.
     */
    LoopStartIntervalDynamicNodeDialog() {
        final var p = new JPanel(new GridBagLayout());
        final var c = new GridBagConstraints();
        m_variableType = new DialogComponentButtonGroup(LoopStartIntervalDynamicNodeModel.createVariableTypeSettings(),
            null, true, LoopStartIntervalDynamicNodeModel.OutputType.values());

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("From   "), c);
        c.gridx = 1;
        p.add(m_from, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("To   "), c);
        c.gridx = 1;
        p.add(m_to, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Step   "), c);
        c.gridx = 1;
        p.add(m_step, c);

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Loop variable is   "), c);
        c.gridx = 1;
        p.add(m_variableType.getComponentPanel(), c);

        c.gridx = 0;
        c.gridy++;

        c.gridx = 0;
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Variable prefix   "), c);
        c.gridx = 1;
        p.add(m_prefix, c);

        addTab("Standard settings", p);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);
        m_prefix.setText(m_settings.prefix());
        m_from.setText(Double.toString(m_settings.from()));
        m_to.setText(Double.toString(m_settings.to()));
        m_step.setText(Double.toString(m_settings.step()));
        m_variableType.loadSettingsFrom(settings, specs);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_settings.prefix(m_prefix.getText());
        m_settings.from(Double.parseDouble(m_from.getText()));
        m_settings.to(Double.parseDouble(m_to.getText()));
        m_settings.step(Double.parseDouble(m_step.getText()));
        m_variableType.saveSettingsTo(settings);
        m_settings.saveSettingsTo(settings);
    }
}
