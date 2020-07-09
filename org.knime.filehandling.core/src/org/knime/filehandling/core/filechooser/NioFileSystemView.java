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
 *   Jul 31, 2019 (julian): created
 */
package org.knime.filehandling.core.filechooser;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;

/**
 * {@link FileSystemView} for java.nio file systems.
 *
 * @author Julian Bunzel, KNIME
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class NioFileSystemView extends FileSystemView {

    private static final String UNABLE_TO_LIST_FILES_MSG = "Unable to list files";

    /**
     * The "default directory" on this particular file system connection. This is the directory where the browsing
     * starts by default.
     */
    private final Path m_defaultDirectory;

    /**
     * The "home directory" on this particular file system connection. This will typically be the working directory of
     * the file system.
     */
    private final Path m_homeDirectory;

    /** Set of all root directories of the given file system */
    final Set<Path> m_rootDirectories;

    /** File system */
    private final FSFileSystem<FSPath> m_fileSystem;

    /**
     * Parent component, might be {@code null}. Useful in dialogs.
     */
    private Component m_parentComponent;

    /**
     * Constructor for a NIO implementation of the {@link FileSystemView}.
     *
     * @param conn The connection used to provide the file system to be shown in the {@code JFileChooser} instance.
     */
    @SuppressWarnings("unchecked")
    public NioFileSystemView(final FSConnection conn) {
        this(conn.getFileSystem(), conn.getFileSystem().getWorkingDirectory(),
            conn.getFileSystem().getWorkingDirectory());
    }

    /**
     * Constructor for a NIO implementation of the {@link FileSystemView}.
     *
     * @param fileSystem
     * @param base
     */
    public NioFileSystemView(final FSFileSystem<?> fileSystem) {
        this(fileSystem, fileSystem.getWorkingDirectory(), fileSystem.getWorkingDirectory());
    }

    /**
     * @param fileSystem
     * @param defaultDirectory
     * @param homeDirectory
     */
    public NioFileSystemView(final FSFileSystem<?> fileSystem, final FSPath defaultDirectory,
        final FSPath homeDirectory) {
        m_fileSystem = (FSFileSystem<FSPath>)fileSystem;
        m_homeDirectory = homeDirectory;
        m_defaultDirectory = defaultDirectory;
        m_rootDirectories = new LinkedHashSet<>();
        m_fileSystem.getRootDirectories().forEach(m_rootDirectories::add);
    }

    @Override
    public String getSystemTypeDescription(final File f) {
        return m_fileSystem.getPath(f.getPath()).toString();
    }

    @Override
    public String getSystemDisplayName(final File f) {
        String name = f.getName();
        if (name == null || name.length() == 0) {
            name = f.getPath(); // e.g. "/"
        }
        return name;
    }

    @Override
    public File getParentDirectory(final File dir) {
        if (dir == null) {
            return null;
        }
        final Path path = m_fileSystem.getPath(dir.getPath());
        if (path.getParent() == null) {
            return null;
        }
        return new NioFile(path.getParent());
    }

    @Override
    public File[] getFiles(final File dir, final boolean useFileHiding) {
        final File[] result =
            new NioFile(dir.getPath(), m_fileSystem).listFiles(f -> !useFileHiding || !isHiddenFile(f));

        if (result == null) {
            final Exception e = NioFile.popThreadLocalException();

            if (e != null) {
                if (e instanceof NoSuchFileException) {
                    JOptionPane.showMessageDialog(
                        m_parentComponent, ExceptionUtil
                            .limitMessageLength(String.format("No such file or folder: %s", e.getMessage()), 120),
                        UNABLE_TO_LIST_FILES_MSG, JOptionPane.ERROR_MESSAGE);
                } else if (e instanceof AccessDeniedException) {
                    JOptionPane.showMessageDialog(m_parentComponent,
                        ExceptionUtil.limitMessageLength(String.format("Access denied: %s", e.getMessage()), 120),
                        UNABLE_TO_LIST_FILES_MSG, JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(
                        m_parentComponent, ExceptionUtil
                            .limitMessageLength(ExceptionUtils.getMessage(ExceptionUtils.getRootCause(e)), 120),
                        UNABLE_TO_LIST_FILES_MSG, JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(m_parentComponent, "Error in directory listing, see KNIME console/log.",
                    UNABLE_TO_LIST_FILES_MSG, JOptionPane.ERROR_MESSAGE);
            }

            return new File[0];
        } else {
            return result;
        }
    }

    @Override
    public File createFileObject(final String path) {
        return new NioFile(path, m_fileSystem);
    }

    @Override
    public File createFileObject(final File dir, final String filename) {
        Path fileObject;
        if (dir != null) {
            fileObject = m_fileSystem.getPath(dir.getPath(), filename);
        } else {
            fileObject = m_fileSystem.getPath(filename);
        }
        return new NioFile(fileObject.toString(), m_fileSystem);
    }

    @Override
    public File getDefaultDirectory() {
        return new NioFile(m_defaultDirectory.toString(), m_fileSystem);
    }

    @Override
    public File getHomeDirectory() {
        return new NioFile(m_homeDirectory.toString(), m_fileSystem);
    }

    @Override
    public File[] getRoots() {
        return m_rootDirectories.stream().map(p -> new NioFile(p.toString(), m_fileSystem)).toArray(File[]::new);
    }

    @Override
    public boolean isFileSystemRoot(final File dir) {
        return m_fileSystem.getPath(dir.toString()).getParent() == null;
    }

    @Override
    public boolean isHiddenFile(final File f) {
        return f.isHidden();
    }

    @Override
    public boolean isFileSystem(final File f) {
        return !isFileSystemRoot(f);
    }

    @Override
    public File getChild(final File parent, final String fileName) {
        return new NioFile(parent, fileName, m_fileSystem);
    }

    @Override
    public boolean isParent(final File folder, final File file) {
        if (folder == null || file == null) {
            return false;
        }

        final Path filePath = m_fileSystem.getPath(file.getPath());
        final Path folderPath = m_fileSystem.getPath(folder.getPath());
        return folderPath.equals(filePath.getParent());
    }

    @Override
    public Boolean isTraversable(final File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    @Override
    public boolean isRoot(final File f) {
        if (f != null) {
            return m_fileSystem.getPath(f.toString()).getParent() == null;
        }
        return false;
    }

    @Override
    public File createNewFolder(final File containingDir) throws IOException {
        final Path newFolder = m_fileSystem.getPath(containingDir.getPath(), "newFolder");
        if (Files.exists(newFolder)) {
            throw new IOException("Directory 'newFolder' already exists.");
        } else {
            Files.createDirectory(newFolder);
        }
        return newFolder.toFile();
    }

    @Override
    protected File createFileSystemRoot(final File f) {
        return new NioFile(f, "", m_fileSystem);
    }

    @Override
    public boolean isComputerNode(final File dir) {
        return false;
    }

    @Override
    public boolean isFloppyDrive(final File dir) {
        return false;
    }

    @Override
    public boolean isDrive(final File dir) {
        return false;
    }

    @Override
    public Icon getSystemIcon(final File f) {
        return f.isDirectory() ? NioFileView.DIR_ICON : NioFileView.FILE_ICON;
    }

    /**
     * Set the parent dialog to show failure dialogs with a working window binding.
     *
     * @param parent parent component or {@code null}
     */
    protected void setParentView(final Component parent) {
        m_parentComponent = parent;
    }
}
