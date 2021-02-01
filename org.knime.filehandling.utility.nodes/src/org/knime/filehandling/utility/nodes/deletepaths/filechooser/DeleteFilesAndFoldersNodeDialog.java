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
package org.knime.filehandling.utility.nodes.deletepaths.filechooser;

import java.awt.GridBagLayout;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeDialog;

/**
 * Node dialog of the "Delete Files/Folders" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class DeleteFilesAndFoldersNodeDialog
    extends AbstractDeleteFilesAndFoldersNodeDialog<DeleteFilesAndFoldersNodeConfig> {

    private static final String HISTORY_ID = "delete_files_history";

    private final DialogComponentReaderFileChooser m_fileChooser;

    /**
     * Constructor.
     *
     * @param portsConfig this nodes ports configuration
     */
    DeleteFilesAndFoldersNodeDialog(final PortsConfiguration portsConfig) {
        super(new DeleteFilesAndFoldersNodeConfig(portsConfig));

        final FlowVariableModel readFvm = createFlowVariableModel(
            getConfig().getFileChooserSettings().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);

        m_fileChooser = new DialogComponentReaderFileChooser(getConfig().getFileChooserSettings(), HISTORY_ID, readFvm);

        createSettingsTab();
    }

    @Override
    protected JPanel createPathPanel() {
        final JPanel filePanel = new JPanel(new GridBagLayout());
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Select location"));
        final GBCBuilder gbc = createAndInitGBC();
        filePanel.add(m_fileChooser.getComponentPanel(), gbc.setWeightX(1).fillHorizontal().build());

        return filePanel;
    }

    @Override
    protected Optional<JPanel> additionalOptions() {
        //No additional options
        return Optional.empty();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_fileChooser.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_fileChooser.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
    }

    @Override
    public void onClose() {
        m_fileChooser.onClose();
    }
}
