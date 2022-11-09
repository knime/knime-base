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
 *   Oct 21, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtostring;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.pathtostring.PathToStringNodeModel.GenerateColumnMode;

/**
 * The path to string node dialog.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PathToStringNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_selectedColumnNameComponent;

    private final SettingsModelString m_generatedColumnModeModel;

    private final DialogComponentButtonGroup m_generatedColumnModeComponent;

    private final DialogComponentString m_appendedColumnNameComponent;

    private final DialogComponentBoolean m_createKNIMEUrl;

    public PathToStringNodeDialog() {
        m_selectedColumnNameComponent = new DialogComponentColumnNameSelection(
            PathToStringNodeModel.createSettingsModelColumnName(), "", 0, new ColumnFilter() {
                @Override
                public final boolean includeColumn(final DataColumnSpec colSpec) {
                    return colSpec.getType().isCompatible(FSLocationValue.class);
                }

                @Override
                public final String allFilteredMsg() {
                    return "No applicable column available";
                }
            });
        m_appendedColumnNameComponent =
            new DialogComponentString(PathToStringNodeModel.createSettingsModelAppendedColumnName(), "", true, 15);
        m_generatedColumnModeModel = PathToStringNodeModel.createSettingsModelColumnMode();
        m_generatedColumnModeComponent =
            new DialogComponentButtonGroup(m_generatedColumnModeModel, null, true, GenerateColumnMode.values());
        m_generatedColumnModeComponent.getModel().addChangeListener(e -> checkGeneratedColumnMode());

        m_createKNIMEUrl = new DialogComponentBoolean(PathToStringNodeModel.createSettingsModelCreateKNIMEUrl(),
            "Create KNIME URL for 'Relative to' and 'Mountpoint' file systems");

        addTab("Settings", createPanel());
    }

    private void checkGeneratedColumnMode() {
        m_appendedColumnNameComponent.getModel()
            .setEnabled(PathToStringNodeModel.isAppendMode(m_generatedColumnModeModel));
    }

    private JPanel createPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(1).setWeightY(0).fillHorizontal().anchorLineStart();
        p.add(getColumnSelectionPanel(), gbc.build());

        gbc = gbc.incY();
        p.add(getNewColumnPanel(), gbc.build());

        gbc = gbc.incY().setWeightX(1).setWeightY(1).fillBoth();
        p.add(new JPanel(), gbc.build());

        return p;
    }

    private Component getColumnSelectionPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column selection "));

        GBCBuilder gbc = new GBCBuilder().resetX().resetY().setWeightX(0).setWeightY(0).fillNone().anchorLineStart();
        p.add(m_selectedColumnNameComponent.getComponentPanel(), gbc.build());

        gbc = gbc.incX().setWeightX(1).fillHorizontal();
        p.add(new JPanel(), gbc.build());
        return p;
    }

    private JPanel getNewColumnPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output "));

        GBCBuilder gbc =
            new GBCBuilder().resetX().resetY().setWeightX(0).setWeightY(0).fillNone().anchorFirstLineStart();
        gbc = gbc.insets(5, 5, 0, 0);
        p.add(m_generatedColumnModeComponent.getComponentPanel(), gbc.build());

        gbc = gbc.insets(5, 0, 0, 0).incX();
        p.add(m_appendedColumnNameComponent.getComponentPanel(), gbc.build());

        p.add(m_createKNIMEUrl.getComponentPanel(), gbc.resetX().incY().setWidth(2).insets(5, 5, 0, 0).build());

        gbc = gbc.incX().setWeightX(1).fillHorizontal();
        p.add(Box.createHorizontalBox(), gbc.build());

        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_selectedColumnNameComponent.saveSettingsTo(settings);
        m_generatedColumnModeComponent.saveSettingsTo(settings);
        m_appendedColumnNameComponent.saveSettingsTo(settings);
        m_createKNIMEUrl.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_selectedColumnNameComponent.loadSettingsFrom(settings, specs);
        m_generatedColumnModeComponent.loadSettingsFrom(settings, specs);
        m_appendedColumnNameComponent.loadSettingsFrom(settings, specs);
        m_createKNIMEUrl.loadSettingsFrom(settings, specs);
    }

}
