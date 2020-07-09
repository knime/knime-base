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
 *   Mar 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.UUID;

import org.knime.core.node.util.CheckUtils;

/**
 * Abstract super class implemented by all NIO file system providers in KNIME. This class provides the following
 * functionality:
 * <ul>
 * <li>Creation of temporary files and directories.</li>
 * <li>Generics to make file systems more convenient to implement (fewer type casts are required).</li>
 * </ul>
 *
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <P> The type of path that this file system provider works with.
 * @param <F> The file system type of this provider.
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class FSFileSystemProvider<P extends FSPath, F extends FSFileSystem<P>> extends FileSystemProvider {

    @Override
    public abstract F newFileSystem(URI uri, Map<String, ?> env) throws IOException;

    @Override
    public abstract F getFileSystem(URI uri);

    @Override
    public abstract P getPath(URI uri);

    /**
     * Creates a new directory with a randomized name in the given parent directory.
     *
     * @param parent The parent directory in which to create the temp directory.
     * @param prefix A string prefix for the directory name.
     * @param suffix A string suffix for the directory name.
     * @return the path of the newly created temporary directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     * @see FSFiles#createTempDirectory(FSPath, String, String)
     */
    public P createRandomizedDirectory(final P parent, final String prefix, final String suffix) throws IOException {
        CheckUtils.checkArgumentNotNull(parent, "parent directory must not be null");
        CheckUtils.checkArgumentNotNull(prefix, "prefix must not be null");
        CheckUtils.checkArgumentNotNull(suffix, "suffix must not be null");

        final P tempDir = createNonExistentPath(parent, prefix, suffix);
        createDirectory(tempDir);
        return tempDir;
    }

    /**
     * Creates a new temporary directory in the given parent directory. The directory and everything in it will be
     * automatically deleted when the underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp directory.
     * @param prefix A string prefix for the directory name.
     * @param suffix A string suffix for the directory name.
     * @return the path of the newly created temporary directory.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the directory.
     * @see FSFiles#createTempDirectory(FSPath, String, String)
     */
    @SuppressWarnings({"resource"})
    public P createTempDirectory(final P parent, final String prefix, final String suffix) throws IOException {
        final P tempDir = createRandomizedDirectory(parent, prefix, suffix);
        tempDir.getFileSystem().registerCloseable(wrapRecursiveDelete(tempDir));
        return tempDir;
    }

    /**
     * Creates a new temporary file in the given parent directory. The file will be automatically deleted when the
     * underlying {@link FSFileSystem} is closed.
     *
     * @param parent The parent directory in which to create the temp file.
     * @param prefix A string prefix for the file name.
     * @param suffix A string suffix for the file name.
     * @return the path of the newly created temporary file.
     *
     * @throws NoSuchFileException when the parent directory does not exist.
     * @throws IOException When something else went wrong while creating the temp file.
     * @see FSFiles#createTempFile(FSPath, String, String)
     */
    @SuppressWarnings({"resource"})
    public P createTempFile(final P parent, final String prefix, final String suffix) throws IOException {
        CheckUtils.checkArgumentNotNull(parent, "parent directory must not be null");
        CheckUtils.checkArgumentNotNull(prefix, "prefix must not be null");
        CheckUtils.checkArgumentNotNull(suffix, "suffix must not be null");

        final P tempFile = createNonExistentPath(parent, prefix, suffix);
        Files.createFile(tempFile);
        tempFile.getFileSystem().registerCloseable(wrapDelete(tempFile));
        return tempFile;
    }

    @SuppressWarnings({"unchecked"})
    private P createNonExistentPath(final P parent, final String prefix, final String suffix) {

        P toReturn;
        do {
            final String currName =
                String.format("%s%s%s", prefix, UUID.randomUUID().toString().replace("-", "").substring(0, 16), suffix);
            toReturn = (P)parent.resolve(currName);
        } while (Files.exists(toReturn));

        return toReturn;
    }

    private static Closeable wrapRecursiveDelete(final FSPath folderToDelete) {
        return new Closeable() {
            @SuppressWarnings("resource")
            @Override
            public void close() {
                try {
                    FSFiles.deleteRecursively(folderToDelete);
                } catch (IOException e) {
                    // nothing to do here
                } finally {
                    folderToDelete.getFileSystem().unregisterCloseable(this);
                }
            }
        };
    }

    private static Closeable wrapDelete(final FSPath toDelete) {
        return new Closeable() {
            @SuppressWarnings("resource")
            @Override
            public void close() {
                try {
                    FSFiles.deleteSafely(toDelete);
                } finally {
                    toDelete.getFileSystem().unregisterCloseable(this);
                }
            }
        };
    }
}
