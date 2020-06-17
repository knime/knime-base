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
 *   Apr 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.MountpointSpecificConfig;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * {@link FileSystemSpecificDialog} for the Mountpoint file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class MountpointFileSystemDialog implements FileSystemSpecificDialog {

    private final JComboBox<KNIMEConnection> m_mountpointsCombo;

    private final JPanel m_mountpointsPanel = new JPanel(new GridBagLayout());

    /**
     * Constructor.
     *
     * @param config the config this dialog represents
     */
    public MountpointFileSystemDialog(final MountpointSpecificConfig config) {
        m_mountpointsCombo = new JComboBox<>(config);
        m_mountpointsCombo.setRenderer(new KNIMEConnectionRenderer());
        GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart();
        m_mountpointsPanel.add(m_mountpointsCombo, gbc.build());
        // add extra panel that receives any extra space available
        m_mountpointsPanel.add(new JPanel(), gbc.fillHorizontal().setWeightX(1).incX().build());
        config.addChangeListener(e -> updateMountpointColor());
    }

    private void updateMountpointColor() {
        m_mountpointsCombo.setForeground(KNIMEConnectionRenderer.getForegroundColor(
            (KNIMEConnection)m_mountpointsCombo.getSelectedItem(), m_mountpointsPanel.getForeground()));
    }

    @Override
    public Component getSpecifierComponent() {
        return m_mountpointsPanel;
    }

    @Override
    public boolean hasSpecifierComponent() {
        return true;
    }

    @Override
    public String toString() {
        return FSCategory.MOUNTPOINT.getLabel();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        m_mountpointsCombo.setEnabled(enabled);
    }

    @Override
    public FSCategory getFileSystemCategory() {
        return FSCategory.MOUNTPOINT;
    }

    @Override
    public Color getTextColor() {
        return Color.BLACK;
    }

    @Override
    public void setTooltip(final String tooltip) {
        m_mountpointsCombo.setToolTipText(tooltip);
    }

    private static class KNIMEConnectionRenderer implements ListCellRenderer<KNIMEConnection> {

        private final DefaultListCellRenderer m_defaultRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(final JList<? extends KNIMEConnection> list,
            final KNIMEConnection value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            // DefaultListCellRenderer returns itself when getListCellRenderer is called
            m_defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            m_defaultRenderer.setForeground(getForegroundColor(value, list.getParent().getForeground()));
            if (value != null) {
                m_defaultRenderer.setText(value.getId());
            }

            if (!isSelected) {
                m_defaultRenderer.setBackground(list.getBackground());
            } else {
                m_defaultRenderer.setBackground(list.getSelectionBackground());
                m_defaultRenderer.setForeground(list.getSelectionForeground());
            }
            return m_defaultRenderer;
        }

        /**
         * Returns the foreground color for the given KNIMEConnection
         *
         * @param connection the KNIMEConnection
         * @param defaultColor the default color
         * @return the foreground color for the given KNIMEConnection
         */
        private static Color getForegroundColor(final KNIMEConnection connection, final Color defaultColor) {
            if (connection != null && !connection.isValid()) {
                return Color.RED;
            } else if (connection != null && !connection.isConnected()) {
                return Color.GRAY;
            } else {
                return defaultColor;
            }
        }

    }

}
