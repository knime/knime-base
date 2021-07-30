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
 *   Dec 17, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.fs.local.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.testing.DefaultFSTestInitializer;

/**
 * Implementation of a test initializer using the local file system.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @param <P> The path type.
 * @param <F> The file system type.
 */
public abstract class BasicLocalTestInitializer<P extends FSPath, F extends FSFileSystem<P>>
    extends DefaultFSTestInitializer<P, F> {

    private final Path m_localWorkingDir;

    /**
     * @param fsConnection The underlying connection.
     * @param localWorkingDir The working directory in the local file system (default FS provider).
     */
    public BasicLocalTestInitializer(final FSConnection fsConnection, final Path localWorkingDir) {
        super(fsConnection);
        m_localWorkingDir = localWorkingDir;
    }

    @Override
    protected void afterTestCaseInternal() throws IOException {
        try {
            FSFiles.deleteRecursively(getLocalTestCaseScratchDir());
        } catch (NoSuchFileException e) { // NOSONAR can be ignored
        }
    }

    @Override
    public final P getTestCaseScratchDir() {
        return toFSPath(getLocalTestCaseScratchDir());
    }

    /**
     * @return the testcase scratch dir as a path from the platform default provider.
     */
    protected Path getLocalTestCaseScratchDir() {
        return m_localWorkingDir.resolve(Integer.toString(getTestCaseId()));
    }

    /**
     * @return the working directory as a path from the platform default provider.
     */
    protected final Path getLocalWorkingDirectory() {
        return m_localWorkingDir;
    }

    /**
     *
     * @param localPath A path from the platform default provider (which must also be accessible using the
     *            {@link FSFileSystem}).
     * @return an {@link FSPath} from the {@link FSFileSystem}, that maps to the given local path.
     */
    protected abstract P toFSPath(final Path localPath);

    @Override
    public P createFileWithContent(final String content, final String... pathComponents) throws IOException {
        return toFSPath(createLocalFileWithContent(content, pathComponents));
    }

    /**
     * Creates a file with the given content and with the given name components below the test case scratch dir.
     *
     * @param content
     * @param pathComponents
     * @return the path (from default FS provider) to the created file.
     * @throws IOException
     */
    protected Path createLocalFileWithContent(final String content, final String... pathComponents) throws IOException {
        if (pathComponents == null || pathComponents.length == 0) {
            throw new IllegalArgumentException("path components can not be empty or null");
        }

        final Path file = Paths.get(getLocalTestCaseScratchDir().toString(), pathComponents);

        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return file;
    }

    @Override
    public P makePath(final String... pathComponents) {
        return toFSPath(Paths.get(getLocalTestCaseScratchDir().toString(), pathComponents));
    }

}
