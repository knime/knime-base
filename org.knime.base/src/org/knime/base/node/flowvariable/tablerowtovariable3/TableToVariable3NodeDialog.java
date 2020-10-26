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
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.flowvariable.tablerowtovariable3;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "TableRowToVariable" node. Exports the first row of a table into variables.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class TableToVariable3NodeDialog extends NodeDialogPane {

    private final DialogComponentButtonGroup m_onMissing;

    private final DialogComponentNumber m_replaceDouble;

    private final DialogComponentNumber m_replaceInteger;

    private final DialogComponentNumber m_replaceLong;

    private final DialogComponentString m_replaceString;

    private final DialogComponentStringSelection m_replaceBoolean;

    private final DialogComponentColumnFilter2 m_columnFilter;

    /**
     * Constructor.
     */
    public TableToVariable3NodeDialog() {
        m_onMissing = new DialogComponentButtonGroup(TableToVariable3NodeModel.getOnMissing(), null, true,
            MissingValuePolicy.values());
        m_replaceString =
            new DialogComponentString(TableToVariable3NodeModel.getReplaceString(), "String      ", true, 10);
        m_replaceBoolean = new DialogComponentStringSelection(TableToVariable3NodeModel.getReplaceBoolean(),
            "Boolean  ", "false", "true");
        m_replaceInteger =
            new DialogComponentNumber(TableToVariable3NodeModel.getReplaceInteger(), "Integer    ", 1, 11);
        m_replaceLong = new DialogComponentNumber(TableToVariable3NodeModel.getReplaceLong(), "Long        ", 1L, 11);
        m_replaceDouble =
            new DialogComponentNumber(TableToVariable3NodeModel.getReplaceDouble(), "Double     ", 0.1, 10);
        m_columnFilter = new DialogComponentColumnFilter2(TableToVariable3NodeModel.getColumnFilter(), 0);

        m_onMissing.getModel().addChangeListener(e -> policyChanged());

        addTab("Settings", createPanel());

        // get the initial state right
        policyChanged();
    }

    private Component createPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = getGbc();

        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(createMissingValueHandlingPanel(), gbc);

        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(createColumnSelectionPanel(), gbc);

        return p;
    }

    private static GridBagConstraints getGbc() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridy = 0;
        return gbc;
    }

    private Component createMissingValueHandlingPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = getGbc();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing values"));

        p.add(createHandlingPanel(), gbc);

        ++gbc.gridy;
        p.add(createDefaultsPanel(), gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(Box.createVerticalBox(), gbc);

        return p;
    }

    private JPanel createHandlingPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = getGbc();
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Handling"));
        p.add(m_onMissing.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(Box.createVerticalBox(), gbc);

        return p;
    }

    private Component createDefaultsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = getGbc();

        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Defaults"));

        p.add(m_replaceString.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_replaceBoolean.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_replaceInteger.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_replaceLong.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_replaceDouble.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        p.add(Box.createVerticalBox(), gbc);

        return p;
    }

    private Component createColumnSelectionPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column selection"));
        p.add(m_columnFilter.getComponentPanel(), gbc);
        return p;
    }

    private void policyChanged() {
        final MissingValuePolicy policy =
            MissingValuePolicy.valueOf(((SettingsModelString)m_onMissing.getModel()).getStringValue());
        final boolean enabled = MissingValuePolicy.OMIT != policy;
        m_replaceString.getModel().setEnabled(enabled);
        m_replaceBoolean.getModel().setEnabled(enabled);
        m_replaceInteger.getModel().setEnabled(enabled);
        m_replaceLong.getModel().setEnabled(enabled);
        m_replaceDouble.getModel().setEnabled(enabled);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_onMissing.saveSettingsTo(settings);
        m_replaceString.saveSettingsTo(settings);
        m_replaceBoolean.saveSettingsTo(settings);
        m_replaceInteger.saveSettingsTo(settings);
        m_replaceLong.saveSettingsTo(settings);
        m_replaceDouble.saveSettingsTo(settings);
        m_columnFilter.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_replaceString.loadSettingsFrom(settings, specs);
        m_replaceBoolean.loadSettingsFrom(settings, specs);
        m_replaceInteger.loadSettingsFrom(settings, specs);
        m_replaceLong.loadSettingsFrom(settings, specs);
        m_replaceDouble.loadSettingsFrom(settings, specs);
        m_columnFilter.loadSettingsFrom(settings, specs);
        m_onMissing.loadSettingsFrom(settings, specs);
    }
}
