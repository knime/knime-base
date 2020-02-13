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
 * -------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.node.portobject.writer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.FilesHistoryPanel;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeDialog;

/**
 * Node dialog for port object writer nodes that can be extended with additional settings components.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the config of the node
 */
public class PortObjectWriterNodeDialog<C extends PortObjectWriterNodeConfig> extends PortObjectIONodeDialog<C> {

    private final DialogComponentBoolean m_overwriteCheckbox;

    private final DialogComponentBoolean m_createDirectoriesCheckbox;

    private final PortsConfiguration m_portsConfig;

    /**
     * Constructor.
     *
     * @param portsConfig the ports configuration
     * @param config the config
     * @param fileChooserHistoryId id used to store file history used by {@link FilesHistoryPanel}
     * @param fileChooserSelectionMode integer defining the file chooser dialog type (see
     *            {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#FILES_AND_DIRECTORIES} and
     *            {@link JFileChooser#DIRECTORIES_ONLY}
     */
    public PortObjectWriterNodeDialog(final PortsConfiguration portsConfig, final C config,
        final String fileChooserHistoryId, final int fileChooserSelectionMode) {
        super(portsConfig, config, fileChooserHistoryId, JFileChooser.SAVE_DIALOG, fileChooserSelectionMode);
        m_overwriteCheckbox =
            new DialogComponentBoolean(getConfig().getOverwriteModel(), getOverwriteLabel(fileChooserSelectionMode));
        m_createDirectoriesCheckbox =
            new DialogComponentBoolean(getConfig().getCreateDirectoryModel(), "Create parent directories if required");
        addAdditionalPanel(createOverwriteSettingsPanel());
        m_portsConfig = portsConfig;
    }

    /**
     * @param fileChooserSelectionMode integer defining the file chooser dialog type (see
     *            {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#FILES_AND_DIRECTORIES} and
     *            {@link JFileChooser#DIRECTORIES_ONLY}
     *
     * @return the label of the overwrite checkbox w.r.t. the selection mode
     */
    private static String getOverwriteLabel(final int fileChooserSelectionMode) {
        if (fileChooserSelectionMode == JFileChooser.FILES_ONLY) {
            return "Overwrite file if exists";
        } else if (fileChooserSelectionMode == JFileChooser.DIRECTORIES_ONLY) {
            return "Overwrite folder content if exists";
        } else if (fileChooserSelectionMode == JFileChooser.FILES_AND_DIRECTORIES) {
            return "Overwrite folder content/file if exists";
        }
        throw new IllegalArgumentException("The provided selection mode is not invalid.");
    }

    private JPanel createOverwriteSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Writing"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.weightx = 1;
        panel.add(m_overwriteCheckbox.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_createDirectoriesCheckbox.getComponentPanel(), gbc);
        return panel;
    }

    /**
     * Method to obtain the index of port object written by this writer.
     *
     * @return the index of the to be-written port object
     */
    protected int getPortObjectIndex() {
        return m_portsConfig.getInputPortLocation().get(PortObjectToPathWriterNodeModel.PORT_OBJECT_INPUT_GRP_NAME)[0];
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_overwriteCheckbox.saveSettingsTo(settings);
        m_createDirectoriesCheckbox.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_overwriteCheckbox.loadSettingsFrom(settings, specs);
        m_createDirectoriesCheckbox.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
    }
}
