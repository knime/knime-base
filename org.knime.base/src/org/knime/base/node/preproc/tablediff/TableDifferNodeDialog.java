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
 * 11.12.2019 (Lars Schweikardt): created
 */
package org.knime.base.node.preproc.tablediff;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;

/**
 * Node dialog of the "Table Difference Finder" node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class TableDifferNodeDialog extends NodeDialogPane {

    private final DialogComponentBoolean m_diaCompareTablesEntirely;

    private final DialogComponentColumnFilter2 m_diaColumnFilter;

    private final DialogComponentButtonGroup m_diaFailMode;

    private final ChangeListener m_changeListener;

    /**
     * Constructor.
     */
    TableDifferNodeDialog() {
        m_diaCompareTablesEntirely = new DialogComponentBoolean(TableDifferNodeModel.createCompareTablesEntirelyModel(),
            "Compare entire tables");
        m_diaColumnFilter = new DialogComponentColumnFilter2(TableDifferNodeModel.createComparedColumnsModel(),
            TableDifferNodeModel.PORT_REFERENCE_TABLE);
        m_diaFailMode = new DialogComponentButtonGroup(TableDifferNodeModel.createFailureModeModel(), "Fail option",
            true, TableDifferNodeModel.FailureMode.values());
        m_changeListener = e -> m_diaColumnFilter.getModel().setEnabled(!m_diaCompareTablesEntirely.isSelected());
        m_diaCompareTablesEntirely.getModel().addChangeListener(m_changeListener);
        addTab("Settings", createLayout());
    }

    private Component createLayout() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);

        p.add(m_diaCompareTablesEntirely.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        final JPanel inner = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcInner = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

        inner.add(m_diaColumnFilter.getComponentPanel(), gbcInner);
        inner.setBorder(BorderFactory.createTitledBorder("Select columns from reference table"));
        p.add(inner, gbc);

        ++gbc.gridy;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        p.add(m_diaFailMode.getComponentPanel(), gbc);

        return p;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_diaCompareTablesEntirely.loadSettingsFrom(settings, specs);
        m_diaColumnFilter.loadSettingsFrom(settings, specs);
        m_diaFailMode.loadSettingsFrom(settings, specs);
        // enable / disable the column filter (cannot be done during constructor due to impl details of col filter)
        m_changeListener.stateChanged(null);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_diaCompareTablesEntirely.saveSettingsTo(settings);
        m_diaColumnFilter.saveSettingsTo(settings);
        m_diaFailMode.saveSettingsTo(settings);

    }

}
