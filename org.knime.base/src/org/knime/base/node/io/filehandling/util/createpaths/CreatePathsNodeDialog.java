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
 *   27 Jul 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.createpaths;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.KeyValuePanel;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;

/**
 * The NodeDialog for the "Create Paths" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CreatePathsNodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "create_paths_history";

    private final KeyValuePanel m_additionalVariablePathPairPanel;

    private final DialogComponentCreatorFileChooser m_filePanel;

    private final CreatePathsNodeConfig m_config;

    CreatePathsNodeDialog(final PortsConfiguration portsConfig) {
        m_config = new CreatePathsNodeConfig(portsConfig);

        final FlowVariableModel fvm = createFlowVariableModel(m_config.getDirChooserModel().getKeysForFSLocation(),
            FSLocationVariableType.INSTANCE);
        m_filePanel = new DialogComponentCreatorFileChooser(m_config.getDirChooserModel(), FILE_HISTORY_ID, fvm);

        m_additionalVariablePathPairPanel = new KeyValuePanel();
        m_additionalVariablePathPairPanel.setKeyColumnLabel("Variable Name");
        m_additionalVariablePathPairPanel.setValueColumnLabel("File / Folder location");
        m_additionalVariablePathPairPanel.getTable().setDefaultRenderer(Object.class,
            new PathPrefixedCellRenderer(m_config.getDirChooserModel()));

        addTab("Settings", initLayout());
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    private JPanel initLayout() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(createAdditionalVariablesPanel(), gbc);

        return panel;
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Base location"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_filePanel.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_filePanel.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    private JPanel createAdditionalVariablesPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel additionalVarPanel = new JPanel(new GridBagLayout());
        additionalVarPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File/Folder variables"));

        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        m_additionalVariablePathPairPanel.getTable().setPreferredScrollableViewportSize(null);
        additionalVarPanel.add(m_additionalVariablePathPairPanel, gbc);
        return additionalVarPanel;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_filePanel.loadSettingsFrom(settings, specs);
        m_config.loadSettingsForDialog(settings);

        m_additionalVariablePathPairPanel.setTableData(m_config.getAdditionalVarNames(),
            m_config.getAdditionalVarValues());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_config.setAdditionalVarNames(m_additionalVariablePathPairPanel.getKeys());
        m_config.setAdditionalVarValues(m_additionalVariablePathPairPanel.getValues());

        m_filePanel.saveSettingsTo(settings);
        m_config.saveSettingsForDialog(settings);
    }

    /**
     * Implements a {@link DefaultTableCellRenderer} that prepends the selected root path to the path provided by the
     * user. The prefix is displayed in a light gray and cannot be modified.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static class PathPrefixedCellRenderer extends DefaultTableCellRenderer {//NOSONAR

        private static final long serialVersionUID = 1L;

        private static final String DEFAULT_PREFIX_FONT_FORMAT = "<html><font color=#696969>%s</font>%s</html>";

        private final transient SettingsModelCreatorFileChooser m_rootSelectionModel;

        private String m_pathSeparator;

        private FSLocation m_lastCategory;

        PathPrefixedCellRenderer(final SettingsModelCreatorFileChooser rootSelectionModel) {
            m_rootSelectionModel = rootSelectionModel;

            // This is a work around and needs to be replaced once AP-14001 has been implemented
            // corresponding ticket AP-15353
            m_lastCategory = m_rootSelectionModel.getLocation();
            m_rootSelectionModel.addChangeListener(this::getPathSeparator);
            assignSeparator(m_lastCategory.getFSCategory());
        }

        private void getPathSeparator(@SuppressWarnings("unused") final ChangeEvent e) {
            final FSLocation curLocation = m_rootSelectionModel.getLocation();
            final FSCategory curCategory = curLocation.getFSCategory();
            if (m_lastCategory.getFSCategory() != curCategory) {
                assignSeparator(curCategory);
            }
            if (!m_lastCategory.equals(curLocation)) {
                repaint();
            }
            m_lastCategory = curLocation;
        }

        private void assignSeparator(final FSCategory curCategory) {
            if ((curCategory == FSCategory.LOCAL || curCategory == FSCategory.RELATIVE) && SystemUtils.IS_OS_WINDOWS) {
                m_pathSeparator = "\\";
            } else {
                m_pathSeparator = "/";
            }
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {
            final DefaultTableCellRenderer c = (DefaultTableCellRenderer)super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
            if (column == 1) {
                final String prefix = getBaseDirPrefixFromLocation();
                final String suffix = table.getModel().getValueAt(row, column).toString();
                c.setText(String.format(DEFAULT_PREFIX_FONT_FORMAT, prefix, suffix));
            }
            return c;
        }

        private String getBaseDirPrefixFromLocation() {
            final String base = m_rootSelectionModel.getLocation().getPath();
            return base.endsWith(m_pathSeparator) ? base : (base + m_pathSeparator);
        }

    }

}
