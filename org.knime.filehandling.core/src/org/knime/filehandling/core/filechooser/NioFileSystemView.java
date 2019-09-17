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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import org.knime.filehandling.core.connections.FSConnection;

/**
 *
 * @author Julian Bunzel, KNIME
 */
public class NioFileSystemView extends FileSystemView {

    /**
     * The base root directory. e.g. "/" for Unix. Might also be set to the first directory in the
     * {@link #m_rootDirectories} set. *
     */
    final Path m_base;

    /** Set of all root directories of the given file system */
    final Set<Path> m_rootDirectories;

    /** File system */
    final FileSystem m_fileSystem;

    /** File system provider */
    final FileSystemProvider m_fsProvider;

    /**
     * Constructor for a NIO implementation of the {@link FileSystemView}.
     *
     * @param conn The connection used to provide the file system to be shown in the {@code JFileChooser} instance.
     */
    public NioFileSystemView(final FSConnection conn) {
        this.m_fileSystem = conn.getFileSystem();
        this.m_base = m_fileSystem.getRootDirectories().iterator().next();
        this.m_fsProvider = m_fileSystem.provider();
        this.m_rootDirectories = new LinkedHashSet<>();
        this.m_fileSystem.getRootDirectories().forEach(m_rootDirectories::add);
    }

    @Override
    public String getSystemTypeDescription(final File f) {
        return m_fileSystem.getPath(f.getPath()).toString();
    }

    @Override
    public String getSystemDisplayName(final File f) {
        return f.getName();
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
        return new NioFile(dir.getPath(), m_fileSystem).listFiles();
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
        return new NioFile(m_base.toString(), m_fileSystem);
    }

    @Override
    public File getHomeDirectory() {
        return new NioFile(m_base.toString(), m_fileSystem);
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
        final Path filePath = m_fileSystem.getPath(file.getPath());
        final Path folderPath = m_fileSystem.getPath(folder.getPath());
        return filePath.getParent().equals(folderPath);
    }

    @Override
    public Boolean isTraversable(final File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    @Override
    public boolean isRoot(final File f) {
        if (f != null) {
            return m_rootDirectories.stream().anyMatch(p -> p.equals(f.toPath()));
        }
        return false;
    }

    @Override
    public File createNewFolder(final File containingDir) throws IOException {
        final Path newFolder = m_fileSystem.getPath(containingDir.getPath(), "newFolder/");
        Files.createDirectory(newFolder);
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
        return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
    }

}
