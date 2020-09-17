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
 *   Apr 16, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable3;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.workflow.VariableType;

/**
 * The node dialog for the "TableColumnToVariable3" node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableColumnToVariable3NodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_colSelection;

    private final DialogComponentBoolean m_ignoreMissings;

    /**
     * Constructor.
     */
    TableColumnToVariable3NodeDialog() {
        final ColumnFilter filter = new ColumnFilter() {
            @Override
            public final boolean includeColumn(final DataColumnSpec colSpec) {
                return CellToVariableConverterFactory.isSupported(colSpec.getType());
            }

            @Override
            public final String allFilteredMsg() {
                return "No column is compatible with any of the supported types '%s'"
                    + CellToVariableConverterFactory.getSupportedTargetVariableTypes().stream() //
                        .map(VariableType::getIdentifier) //
                        .map(String::toLowerCase) //
                        .collect(Collectors.joining(", "))
                    + ".";
            }
        };

        m_colSelection = new DialogComponentColumnNameSelection(TableColumnToVariable3NodeModel.createColumnSettings(),
            "Column name", 0, true, filter);
        m_colSelection.setToolTipText("Select the column whose rows must be converted to flow variables.");
        m_ignoreMissings =
            new DialogComponentBoolean(TableColumnToVariable3NodeModel.createIgnoreMissing(), "Skip missing values");
        m_ignoreMissings
            .setToolTipText("When unchecked, the execution fails if the selected column contains missing values.");

        addTab("Options", createOptionsPanel());
    }

    /**
     * Creates the panel.
     *
     * @return the options panel
     */
    private JPanel createOptionsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        p.add(m_colSelection.getComponentPanel(), gbc);
        ++gbc.gridy;
        p.add(m_ignoreMissings.getComponentPanel(), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.weighty = 1;
        p.add(Box.createHorizontalBox(), gbc);
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_colSelection.saveSettingsTo(settings);
        m_ignoreMissings.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_colSelection.loadSettingsFrom(settings, specs);
        m_ignoreMissings.loadSettingsFrom(settings, specs);
    }

}
