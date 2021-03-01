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
 *   Aug 3, 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.deletepaths.table;

import java.awt.GridBagLayout;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeDialog;

/**
 * Node dialog of the "Delete Files/Folders (Table based)" node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersTableNodeDialog
    extends AbstractDeleteFilesAndFoldersNodeDialog<DeleteFilesAndFoldersTableNodeConfig> {

    private final DialogComponentColumnNameSelection m_selectedColumnNameComponent;

    private final DialogComponentBoolean m_abortIfFileNotExist;

    /**
     * Constructor.
     *
     * @param portsConfig this nodes ports configuration
     */
    @SuppressWarnings("unchecked")
    DeleteFilesAndFoldersTableNodeDialog(final PortsConfiguration portsConfig) {
        super(new DeleteFilesAndFoldersTableNodeConfig());

        m_selectedColumnNameComponent = new DialogComponentColumnNameSelection(getConfig().getColumnSelection(), "",
            portsConfig.getInputPortLocation().get(DeleteFilesAndFoldersTableNodeFactory.TABLE_INPUT_PORT_GRP_NAME)[0],
            FSLocationValue.class);

        m_abortIfFileNotExist =
            new DialogComponentBoolean(getConfig().getAbortIfFileNotExistsModel(), "Abort if file does not exist");

        createSettingsTab();
    }

    @Override
    protected JPanel createPathPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = createAndInitGBC();
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column selection"));
        panel.add(m_selectedColumnNameComponent.getComponentPanel(), gbc.build());
        panel.add(new JPanel(), gbc.incX().setWeightX(1).fillHorizontal().build());
        return panel;
    }

    @Override
    protected Optional<JPanel> additionalOptions() {
        return Optional.of(m_abortIfFileNotExist.getComponentPanel());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_selectedColumnNameComponent.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
        m_abortIfFileNotExist.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_selectedColumnNameComponent.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
        m_abortIfFileNotExist.loadSettingsFrom(settings, specs);
    }
}
