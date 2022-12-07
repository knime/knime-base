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
 *   09.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.filechooser;

import java.awt.Component;
import java.io.File;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.NodeContext;
import org.knime.filehandling.core.connections.FSConnection;

/**
 * {@link FileSystemBrowser} implementation for NioFileSystems.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class NioFileSystemBrowser extends AbstractFileChooserBrowser {

    private final NioFileSystemView m_fileSystemView;

    /**
     * Constructs a new NioFileSystemBrowser.
     *
     * @param connection the connection to create a browser for
     */
    public NioFileSystemBrowser(final FSConnection connection) {
        this(new NioFileSystemView(connection));
    }

    /**
     * Constructs a new NioFileSystemBrowser.
     *
     * @param fileSystemView the FileSystemView to create a browser for
     */
    public NioFileSystemBrowser(final NioFileSystemView fileSystemView) {
        this(fileSystemView, Set.of());
    }

    /**
     * Constructs a new NioFileSystemBrowser.
     *
     * @param fileSystemView the FileSystemView to create a browser for
     * @param blacklistedPaths set of blacklisted paths
     */
    public NioFileSystemBrowser(final NioFileSystemView fileSystemView, final Set<String> blacklistedPaths) {
        super(blacklistedPaths);
        m_fileSystemView = fileSystemView;
    }

    @Override
    public boolean isCompatible() {
        return NodeContext.getContext().getNodeContainer() != null;
    }

    @Override
    protected FileSystemView getFileSystemView() {
        return m_fileSystemView;
    }

    @Override
    protected FileView getFileView() {
        return new NioFileView();
    }

    /**
     * Create a file with a normalized absolute path, this ensures that a path equals and
     * sun.awt.shell.ShellFolder#getNormalizedFile(File) does not create a new {@link File} using an URI.
     */
    @Override
    protected File createFileFromPath(final String filePath) {
        try {
            return m_fileSystemView.createFileObject(filePath).toPath().toAbsolutePath().normalize().toFile();
        } catch (Exception ex) {
            // returning null instead of throwing an exception will ensure the default directory is opened on browse
            return null;
        }
    }

    @Override
    protected File addFileExtension(final File file, final String fileExtension) {
        if (file instanceof NioFile) {
            return ((NioFile)file).withFileExtension(fileExtension);
        }

        return super.addFileExtension(file, fileExtension);
    }

    @Override
    public String openDialogAndGetSelectedFileName(final FileSelectionMode fileSelectionMode,
        final DialogType dialogType, final Component parent, final String forcedFileExtensionOnSave,
        final String selectedFile, final String[] suffixes) {

        // Provide the parent component to the view to show failure dialogs bounded to parent component.
        try {
            m_fileSystemView.setParentView(parent);
            return super.openDialogAndGetSelectedFileName(fileSelectionMode, dialogType, parent,
                forcedFileExtensionOnSave, selectedFile, suffixes);
        } finally {
            m_fileSystemView.setParentView(null);
        }
    }
}
