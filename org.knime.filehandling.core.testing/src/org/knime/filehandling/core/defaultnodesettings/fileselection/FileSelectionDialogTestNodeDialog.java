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
 *   Apr 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.fileselection;

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.filehandling.core.connections.local.LocalFileSystemBrowser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node dialog for testing {@link FileSelectionDialog}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FileSelectionDialogTestNodeDialog extends NodeDialogPane {

    private final FileSelectionDialog m_readDialog;

    private final FileSelectionDialog m_writeDialog;

    private final JTextField m_extensions = new JTextField(20);

    private final JRadioButton m_onlyFiles = new JRadioButton("Files");

    private final JRadioButton m_onlyFolders = new JRadioButton("Folders");

    private final JRadioButton m_filesAndFolders = new JRadioButton("Files and Folders");

    FileSelectionDialogTestNodeDialog() {
        //		FileSystemBrowser fileSystemBrowser = new NioFileSystemBrowser(new LocalFSConnection());
        FileSystemBrowser fileSystemBrowser = new LocalFileSystemBrowser();
        m_readDialog = new FileSelectionDialog("testReadDialog", 8, fileSystemBrowser, DialogType.OPEN_DIALOG,
            FileSelectionMode.FILES_ONLY, new String[0]);
        m_writeDialog = new FileSelectionDialog("testWriteDialog", 8, fileSystemBrowser, DialogType.SAVE_DIALOG,
            FileSelectionMode.FILES_ONLY, new String[0]);
        m_extensions.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                handleExtensionChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleExtensionChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleExtensionChange();
            }
        });
        setupFileSelectionModeButtonGroup();
        addTab("Options", layout());
    }

    private JPanel layout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
        panel.add(m_onlyFiles, gbc.build());
        panel.add(m_onlyFolders, gbc.incX().build());
        panel.add(m_filesAndFolders, gbc.incX().build());
        panel.add(new JLabel("File extensions:"), gbc.resetX().incY().build());
        panel.add(m_extensions, gbc.incX().build());
        panel.add(m_readDialog.getPanel(), gbc.resetX().incY().setWidth(3).fillHorizontal().setWeightX(1).build());
        panel.add(m_writeDialog.getPanel(), gbc.incY().build());
        return panel;
    }

    private void setupFileSelectionModeButtonGroup() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(m_onlyFiles);
        bg.add(m_onlyFolders);
        bg.add(m_filesAndFolders);
        bg.setSelected(m_onlyFiles.getModel(), true);
        m_onlyFiles.addActionListener(e -> handleFileSelectionModeChange());
        m_onlyFolders.addActionListener(e -> handleFileSelectionModeChange());
        m_filesAndFolders.addActionListener(e -> handleFileSelectionModeChange());
    }

    private void handleExtensionChange() {
        final String[] extensions = getExtensions();
        m_readDialog.setFileExtensions(extensions);
        m_writeDialog.setFileExtensions(extensions);
    }

    private String[] getExtensions() {
        return Arrays.stream(m_extensions.getText().split(",")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    private void handleFileSelectionModeChange() {
        final FileSelectionMode newMode = getCurrentMode();
        m_readDialog.setFileSelectionMode(newMode);
        m_writeDialog.setFileSelectionMode(newMode);
    }

    private FileSelectionMode getCurrentMode() {
        if (m_onlyFiles.isSelected()) {
            return FileSelectionMode.FILES_ONLY;
        } else if (m_onlyFolders.isSelected()) {
            return FileSelectionMode.DIRECTORIES_ONLY;
        } else if (m_filesAndFolders.isSelected()) {
            return FileSelectionMode.FILES_AND_DIRECTORIES;
        } else {
            throw new IllegalStateException("No file selection mode selected.");
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_readDialog.addCurrentSelectionToHistory();
        m_writeDialog.addCurrentSelectionToHistory();
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        // nothing to load
    }

}
