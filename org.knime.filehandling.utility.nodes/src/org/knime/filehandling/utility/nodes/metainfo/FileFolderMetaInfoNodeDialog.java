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
 *   Sep 9, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.metainfo;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnFilter;
import org.knime.filehandling.core.data.location.FSLocationValue;

/**
 * The {@link NodeDialog} of the node model allowing to extract meta information about files and folders.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class FileFolderMetaInfoNodeDialog extends NodeDialogPane {

    private final DialogComponentColumnNameSelection m_colSelection;

    private final DialogComponentBoolean m_failIfFilePathNotExists;

    private final DialogComponentBoolean m_calculateOverallFolderSize;

    private final DialogComponentBoolean m_appendPermissions;

    private final DialogComponentBoolean m_appendPosixAttrs;

    FileFolderMetaInfoNodeDialog(final PortsConfiguration portsConfiguration) {
        final ColumnFilter filter = new ColumnFilter() {
            @Override
            public final boolean includeColumn(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(FSLocationValue.class);
            }

            @Override
            public final String allFilteredMsg() {
                return "No applicable column available";
            }
        };

        m_colSelection = new DialogComponentColumnNameSelection(
            FileFolderMetaInfoNodeModel.createColumnSettingsModel(), "Column name", portsConfiguration
                .getInputPortLocation().get(FileFolderMetaInfoNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0],
            true, filter);
        m_colSelection
            .setToolTipText("Select the column containing the files/folders whose meta information must be extracted.");

        m_failIfFilePathNotExists = new DialogComponentBoolean(
            FileFolderMetaInfoNodeModel.createFailIfPathNotExistsSettingsModel(), "Fail if file/folder does not exist");

        m_calculateOverallFolderSize =
            new DialogComponentBoolean(FileFolderMetaInfoNodeModel.createCalculateOverallFolderSizeSettingsModel(),
                "Calculate overall folder size");

        m_appendPermissions = new DialogComponentBoolean(
            FileFolderMetaInfoNodeModel.createAppendPermissionsSettingsModel(), "Append permissions");

        m_appendPosixAttrs = new DialogComponentBoolean(FileFolderMetaInfoNodeModel.createAppendPosixAttrsSettingsModel(), "Append POSIX attributes");

        addTab("Settings", createDialog());

    }

    private Component createDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridy = 0;
        gbc.insets = new Insets(3, 3, 0, 0);
        p.add(m_colSelection.getComponentPanel(), gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        ++gbc.gridy;
        p.add(m_failIfFilePathNotExists.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_calculateOverallFolderSize.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_appendPermissions.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_appendPosixAttrs.getComponentPanel(), gbc);

        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(Box.createHorizontalBox(), gbc);

        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_colSelection.saveSettingsTo(settings);
        m_failIfFilePathNotExists.saveSettingsTo(settings);
        m_calculateOverallFolderSize.saveSettingsTo(settings);
        m_appendPermissions.saveSettingsTo(settings);
        m_appendPosixAttrs.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_colSelection.loadSettingsFrom(settings, specs);
        m_failIfFilePathNotExists.loadSettingsFrom(settings, specs);
        m_calculateOverallFolderSize.loadSettingsFrom(settings, specs);
        m_appendPermissions.loadSettingsFrom(settings, specs);
        m_appendPosixAttrs.loadSettingsFrom(settings, specs);
    }
}
