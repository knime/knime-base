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
package org.knime.filehandling.core.testing;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Interface defining the life cycle management of file system test environments. <br>
 * <br>
 * The parts of the life cycle managed are: <br>
 *  - Open a connection to the file system under test (constructor)<br>
 *  - Create files resolved from the configured test-root<br>
 *  - Setup before the execution of a single test case<br>
 *  - Clean up after the execution of a single test case<br>
 * <br>
 * {@link FSTestInitializer} is only meant to be instantiated from corresponding {@link FSTestInitializerProvider}.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @param <P> The concrete path type of the file system.
 * @param <F> The concrete file system type.
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface FSTestInitializer<P extends FSPath, F extends FSFileSystem<P>> {

    /**
     * Hook called before each test case, allowing each implementation to modify the initializer before each test case.
     *
     * @throws IOException
     */
    public void beforeTestCase() throws IOException;

    /**
     * Hook called after each test case, allowing each implementation to modify the initializer after each test case.
     *
     * @throws IOException
     */
    public void afterTestCase() throws IOException;

    /**
     * Called after all test cases of a AbstractParameterizedFSTest have completed. The default implementation
     * does nothing, but some file systems may need to do final cleanup, e.g. delete their working directory.
     *
     * @throws IOException
     */
    public default void afterClass() throws IOException {
        // default implementation does nothing
    }

    /**
     * Returns the file system connection of this test initializer.
     *
     * @return file system connection
     */
    public FSConnection getFSConnection();

    /**
     * Returns the underlying file system of this test initializer.
     *
     * @return the underlying file system
     */
    public F getFileSystem();

    /**
     * Returns a directory in the underlying {@link FSFileSystem} which is supposed to be used by all integration tests
     * to store and read their data.
     *
     * @return the scratch directory to be used by all integration tests to store and read their data.
     */
    public P getTestCaseScratchDir();

    /**
     * Creates a file with the provided path, starting from the configured root directory.
     *
     * @param pathComponents the path components of the file to be created
     * @return the path to the created file
     * @throws IOException
     */
    public P createFile(String... pathComponents) throws IOException;

    /**
     * Creates a file at the provided path destination, starting from the configured root directory, with the provided
     * content.
     *
     * @param content content to be written to the file
     * @param pathComponents the path
     * @return the path to the created file
     * @throws IOException
     */
    public P createFileWithContent(String content, String... pathComponents) throws IOException;

    /**
     * Creates a {@link Path} object for the given name components. The returned path is absolute and has the path from
     * {@link #getTestCaseScratchDir()} as prefix. Note that this method does not create a file or folder.
     *
     * @param pathComponents The path components of the path to create.
     * @return a {@link Path} object for the given name components.
     */
    public P makePath(final String... pathComponents);

}
