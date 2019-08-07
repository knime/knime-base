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
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import org.knime.filehandling.core.connections.FSConnection;

import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;

/**
 *
 * @author Julian Bunzel, KNIME
 */
public class NioFileSystemView extends FileSystemView {

    /**
     * The base root directory. e.g. "/" for Unix. Might also be set to the first directory in the
     * {@link #rootDirectories} set. *
     */
    final Path base;

    /** Set of all root directories of the given file system */
    final Set<Path> rootDirectories;

    /** File system */
    final FileSystem fileSystem;

    /** File system provider */
    final FileSystemProvider fsProvider;

    /**
     * @param conn The connection used to provide the file system to be shown in the {@code JFileChooser} instance.
     * @throws URISyntaxException
     * @throws IOException
     */
    public NioFileSystemView(final FSConnection conn) throws URISyntaxException, IOException {
        this.fileSystem = conn.getFileSystem();
        this.base = fileSystem.getRootDirectories().iterator().next();
        this.fsProvider = fileSystem.provider();
        this.rootDirectories = new HashSet<>();
        this.fileSystem.getRootDirectories().forEach(p -> rootDirectories.add(p));
    }

    @Override
    public String getSystemTypeDescription(final File f) {
        return fileSystem.getPath(f.getPath()).toString();
    }

    @Override
    public String getSystemDisplayName(final File f) {
        return f.getName();
    }

    @Override
    public File getParentDirectory(final File dir) {
        Path path = fileSystem.getPath(cutPath(dir.getPath()));
        return new NioFile(path.getParent());
    }

    @Override
    public File[] getFiles(final File dir, final boolean useFileHiding) {
        return new NioFile(cutPath(dir.getPath())).listFiles();
    }

    @Override
    public File createFileObject(final String path) {
        return new NioFile(path);
    }

    @Override
    public File createFileObject(final File dir, final String filename) {
        Path fileObject;
        if (dir != null) {
            fileObject = fileSystem.getPath(dir.getPath(), filename);
        } else {
            fileObject = fileSystem.getPath(filename);
        }
        return new NioFile(fileObject.toString());
    }

    @Override
    public File getDefaultDirectory() {
        return new NioFile(cutPath(base.toString()));
    }

    @Override
    public File getHomeDirectory() {
        return new NioFile(cutPath(base.toString()));
    }

    @Override
    public File[] getRoots() {
        return rootDirectories.stream().map(p -> new NioFile(cutPath(p.toString()))).toArray(File[]::new);
    }

    @Override
    public boolean isFileSystemRoot(final File dir) {
        return fileSystem.getPath(cutPath(dir.toString())).getParent() == null;
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
        return new NioFile(parent, fileName);
    }

    @Override
    public boolean isParent(final File folder, final File file) {
        final Path filePath = fileSystem.getPath(file.getPath());
        final Path folderPath = fileSystem.getPath(folder.getPath());
        return filePath.getParent().equals(folderPath);
    }

    @Override
    public Boolean isTraversable(final File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    @Override
    public boolean isRoot(final File f) {
        if (f != null) {
            return rootDirectories.stream().anyMatch(p -> p.equals(f.toPath()));
        }
        return false;
    }

    @Override
    public File createNewFolder(final File containingDir) throws IOException {
        Path newFolder = fileSystem.getPath(containingDir.getPath(), "newFolder/");
        Files.createDirectory(newFolder);
        return newFolder.toFile();
    }

    @Override
    protected File createFileSystemRoot(final File f) {
        return new NioFile(f, "");
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

    private static final String cutPath(final String string) {
        return string.replace("s3://AKIA2UFJCI35RVRPOH46@s3-eu-west-1.amazonaws.com", "");
    }

    private final class NioFile extends File {

        private static final long serialVersionUID = -5343680976176201255L;

        private final Path path;

        private NioFile(final Path p) {
            super(p.toString());
            path = p;
        }

        private NioFile(final String pathname) {
            super(pathname);
            path = fileSystem.getPath(pathname);
        }

        public NioFile(final File parent, final String child) {
            super(parent, child);
            path = null;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isDirectory() {
            return Boolean
                .valueOf(Files.isDirectory(new S3Path((S3FileSystem)fileSystem, cutPath(this.getPath()), "")));
        }

        @Override
        public File getCanonicalFile() throws IOException {
            return new NioFile(cutPath(path.toString()));
        }

        @Override
        public File[] listFiles() {
            if (Files.isDirectory(path)) {
                List<File> files = new ArrayList<>();
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                    directoryStream.iterator().forEachRemaining(p -> files.add(new NioFile(cutPath(p.toString()))));
                } catch (IOException ex) {
                    // Log ...
                }
                return files.toArray(new NioFile[files.size()]);
            }
            return new File[0];
        }

        @Override
        public String getPath() {
            return path.toString();
        }
    }

}
