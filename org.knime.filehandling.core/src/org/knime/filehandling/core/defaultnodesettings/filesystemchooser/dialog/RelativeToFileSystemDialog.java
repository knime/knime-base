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

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.RelativeTo;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.RelativeToSpecificConfig;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * FileSystemDialog for the relative to file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class RelativeToFileSystemDialog implements FileSystemSpecificDialog {

    private final JComboBox<RelativeTo> m_relativeToCombo = new JComboBox<>(RelativeTo.values());

    private final JPanel m_relativeToPanel = new JPanel(new GridBagLayout());

    private final RelativeToSpecificConfig m_config;

    /**
     * Constructor.
     *
     * @param config the {@link RelativeToSpecificConfig} this dialog displays
     */
    public RelativeToFileSystemDialog(final RelativeToSpecificConfig config) {
        m_config = config;
        m_relativeToCombo.setSelectedItem(config.getRelativeTo());
        m_config.addChangeListener(e -> handleConfigChange());
        m_relativeToCombo.addActionListener(e -> handleComboBoxChange());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY();
        m_relativeToPanel.add(m_relativeToCombo, gbc.anchorLineStart().build());
        // add extra panel that receives any extra space available
        m_relativeToPanel.add(new JPanel(), gbc.incX().fillHorizontal().setWeightX(1).build());
    }

    private void handleConfigChange() {
        m_relativeToCombo.setSelectedItem(m_config.getRelativeTo());
    }

    private void handleComboBoxChange() {
        m_config.setRelativeTo(getSelected());
    }

    @Override
    public Component getSpecifierComponent() {
        return m_relativeToPanel;
    }

    @Override
    public boolean hasSpecifierComponent() {
        return true;
    }

    @Override
    public FSCategory getFileSystemCategory() {
        return FSCategory.RELATIVE;
    }

    private RelativeTo getSelected() {
        return (RelativeTo)m_relativeToCombo.getSelectedItem();
    }

    @Override
    public String toString() {
        return FSCategory.RELATIVE.getLabel();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        m_relativeToCombo.setEnabled(enabled);
    }

    @Override
    public Color getTextColor() {
        return Color.BLACK;
    }

    @Override
    public void setTooltip(final String tooltip) {
        m_relativeToCombo.setToolTipText(tooltip);
    }

}
