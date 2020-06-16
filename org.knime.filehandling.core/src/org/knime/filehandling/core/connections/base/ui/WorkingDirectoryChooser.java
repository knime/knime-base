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
 *   Jun 16, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.base.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Panel for file system connector dialogs, which allows to specify a working directory.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class WorkingDirectoryChooser extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel m_label;

    private final FileSelectionDialog m_fileSelector;

    /** Constructor.
    *
    * @param historyID ID for storing a file history
    * @param connectionSupplier the initial supplier for the {@link FSConnection}
    */
    public WorkingDirectoryChooser(final String historyID, final IOESupplier<FSConnection> connectionSupplier) {
        m_fileSelector = new FileSelectionDialog(historyID, //
            25, //
            connectionSupplier, //
            DialogType.SAVE_DIALOG, //
            FileSelectionMode.DIRECTORIES_ONLY, new String[0]);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        m_label = new JLabel("Working directory: ");
        add(m_label, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(m_fileSelector.getPanel(), gbc);
    }

    /**
     * Adds a {@link ChangeListener}.
     *
     * @param listener to add
     */
    public void addListener(final ChangeListener listener) {
        m_fileSelector.addListener(listener);
    }

    /**
     * Returns the currently selected working directory.
     *
     * @return the currently selected working directory.
     */
    public String getSelectedWorkingDirectory() {
        return m_fileSelector.getSelected();
    }

    /**
     * Allows to set the currently selected working directory.
     *
     * @param workingDir the working directory that should be selected
     */
    public void setSelectedWorkingDirectory(final String workingDir) {
        m_fileSelector.setSelected(workingDir);
    }

    /**
     * Removes a {@link ChangeListener}.
     *
     * @param listener to remove
     */
    public void removeListener(final ChangeListener listener) {
        m_fileSelector.removeListener(listener);
    }

    /**
     * To be called when the dialog is closed (by pressing OK or Cancel).
     */
    public void onClose() {
        m_fileSelector.onClose();
    }

    /**
     * Enables/disables the dialog.
     *
     * @param enabled {@code true} if the dialog should be enabled {@code false} if it should be disabled
     */
    @Override
    public void setEnabled(final boolean enabled) {
        m_label.setEnabled(false);
        m_fileSelector.setEnabled(enabled);
    }

    /**
     * Adds the currently selected working directory to the history.
     */
    public void addCurrentSelectionToHistory() {
        m_fileSelector.addCurrentSelectionToHistory();
    }
}
