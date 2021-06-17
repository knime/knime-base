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
 *   Apr 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.EnumMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FileSystemConfiguration;

/**
 * Consists of a combo box for selecting file systems as well as a file system dependent specifier component (e.g.
 * another combo box).
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FileSystemChooser {

    private static final String NO_SPECIFIER = "NO_SPECIFIER";

    private final JPanel m_panel = new JPanel(new GridBagLayout());

    private final JPanel m_specifierPanel = new JPanel(new CardLayout());

    private final JComboBox<FileSystemSpecificDialog> m_fileSystemComboBox;

    private final EnumMap<FSCategory, FileSystemSpecificDialog> m_fileSystemDialogs = new EnumMap<>(FSCategory.class);

    private final FileSystemConfiguration<?> m_config;

    /**
     * Constructor.
     *
     * @param fsConfig the {@link FileSystemConfiguration} displayed by this {@link FileSystemChooser}
     * @param fileSystemDialogs the dialogs of the individual file systems
     */
    public FileSystemChooser(final FileSystemConfiguration<?> fsConfig,
        final FileSystemSpecificDialog... fileSystemDialogs) {
        CheckUtils.checkArgumentNotNull(fileSystemDialogs, "The fileSystemDialogs must not be null.");
        CheckUtils.checkArgument(fileSystemDialogs.length > 0, "At least one FileSystemDialog must be provided.");
        Arrays.stream(fileSystemDialogs).forEach(d -> m_fileSystemDialogs.put(d.getFileSystemCategory(), d));
        m_fileSystemComboBox = new JComboBox<>(fileSystemDialogs);
        m_fileSystemComboBox.setRenderer(new FileSystemDialogRenderer());
        setupTopLevelPanel();
        setupFileSystemSpecifierPanel();
        m_fileSystemComboBox.addActionListener(e -> handleFileSystemSelection());
        m_config = fsConfig;
        fsConfig.addChangeListener(e -> handleConfigChange());
        handleConfigChange();
    }

    /**
     * Retrieves the {@link JPanel} for integration in a larger UI.
     *
     * @return the {@link JPanel} containing the visual components
     */
    public JPanel getPanel() {
        return m_panel;
    }

    private void handleConfigChange() {
        FSCategory category = m_config.getFSCategory();
        final FileSystemSpecificDialog fsd = m_fileSystemDialogs.get(category);
        if (fsd != null) {
            setSpecificDialog(fsd);
        }
    }

    private void setSpecificDialog(final FileSystemSpecificDialog fsd) {
        m_fileSystemComboBox.setSelectedItem(fsd);
        m_fileSystemComboBox.setForeground(fsd.getTextColor());
    }

    private void handleFileSystemSelection() {
        final FileSystemSpecificDialog fsd = getSelectedFileSystem();
        m_config.setFSCategory(fsd.getFileSystemCategory());
        final CardLayout cardLayout = (CardLayout)m_specifierPanel.getLayout();
        cardLayout.show(m_specifierPanel,
            fsd.hasSpecifierComponent() ? fsd.getFileSystemCategory().name() : NO_SPECIFIER);
    }

    private void setupFileSystemSpecifierPanel() {
        // default is no specifier
        m_specifierPanel.add(new JPanel(), NO_SPECIFIER);
        m_fileSystemDialogs.values().stream()//
            .filter(FileSystemSpecificDialog::hasSpecifierComponent)//
            .forEach(this::addSpecifierCard);
    }

    private void addSpecifierCard(final FileSystemSpecificDialog fsd) {
        final Component specifierComponent = fsd.getSpecifierComponent();
        m_specifierPanel.add(specifierComponent, fsd.getFileSystemCategory().name());
    }

    private void setupTopLevelPanel() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_panel.add(m_fileSystemComboBox, gbc);
        gbc.gridx++;
        m_panel.add(m_specifierPanel, gbc);
        gbc.weightx = 1;
        gbc.gridx++;
        m_panel.add(new JPanel(), gbc);
    }

    private FileSystemSpecificDialog getSelectedFileSystem() {
        return m_fileSystemComboBox.getItemAt(m_fileSystemComboBox.getSelectedIndex());
    }

    /**
     * Sets the provided <b>tooltip</b> on all contained components.
     *
     * @param tooltip to set
     */
    public void setTooltip(final String tooltip) {
        m_fileSystemComboBox.setToolTipText(tooltip);
        m_fileSystemDialogs.values().forEach(d -> d.setTooltip(tooltip));
    }

    /**
     * Enables/disables the {@link FileSystemChooser} depending on the value of <b>enable</b>.
     *
     * @param enable whether the instance should be enabled/disabled
     */
    public void setEnabled(final boolean enable) {
        m_fileSystemComboBox.setEnabled(enable);
        m_fileSystemDialogs.values().forEach(d -> d.setEnabled(enable));
    }

    private static class FileSystemDialogRenderer implements ListCellRenderer<FileSystemSpecificDialog> {

        private final DefaultListCellRenderer m_renderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(final JList<? extends FileSystemSpecificDialog> list,
            final FileSystemSpecificDialog value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
            // DefaultListCellRenderer.getListCellRendererComponent updates and returns itself
            m_renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            m_renderer.setForeground(value.getTextColor());

            return m_renderer;
        }

    }

}
