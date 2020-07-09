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
 *   Jun 5, 2020 (bjoern): created
 */
package org.knime.filehandling.core.testing;

import java.io.IOException;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Base implementation of a {@link FSTestInitializer}. The base implementation provides acess to the underlying file
 * system, and offers the {@link #getTestCaseScratchDir()} method that provides a new scratch dir path for each test
 * cases, which is inside the working directory of the underlying file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @param <P> The concrete path type of the file system.
 * @param <F> The concrete file system type.
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class DefaultFSTestInitializer<P extends FSPath, F extends FSFileSystem<P>>
    implements FSTestInitializer<P, F> {

    private final FSConnection m_fsConnection;

    private final F m_fileSystem;

    private int m_currTestCaseId = 0;

    /**
     * Creates a new instance.
     *
     * @param fsConnection The {@link FSConnection} to use.
     */
    @SuppressWarnings("unchecked")
    protected DefaultFSTestInitializer(final FSConnection fsConnection) {
        m_fsConnection = fsConnection;
        m_fileSystem = (F) m_fsConnection.getFileSystem();
    }

    @Override
    public FSConnection getFSConnection() {
        return m_fsConnection;
    }

    /**
     * @return a numeric identifier for the current test case.
     */
    protected int getTestCaseId() {
        return m_currTestCaseId;
    }

    @SuppressWarnings({"unchecked", "resource"})
    @Override
    public P getTestCaseScratchDir() {
        return (P)getFileSystem().getWorkingDirectory().resolve(Integer.toString(m_currTestCaseId));
    }

    @Override
    public F getFileSystem() {
        return m_fileSystem;
    }

    @Override
    public final void beforeTestCase() throws IOException {
        m_currTestCaseId++;
        beforeTestCaseInternal();
    }

    /**
     * Internal file-system-specific method to prepare the test case. The typical thing to implement here is to create
     * the test-case specific scratch directory returned by {@link #getTestCaseScratchDir()}.
     *
     * @throws IOException
     */
    protected abstract void beforeTestCaseInternal() throws IOException;

    @Override
    public final void afterTestCase() throws IOException {
        // reserved for future use (and for consistency with beforeTestCase()
        afterTestCaseInternal();
    }

    /**
     * Internal file-system-specific method to wrap up the leftovers of a test case. The typical thing to implement here
     * is to delete test-case specific scratch directory returned by {@link #getTestCaseScratchDir()}.
     *
     * @throws IOException
     */
    protected abstract void afterTestCaseInternal() throws IOException;

    @Override
    public P createFile(final String... pathComponents) throws IOException {
        return createFileWithContent("", pathComponents);
    }

    @Override
    public P makePath(final String... pathComponents) {
        return getFileSystem().getPath(getTestCaseScratchDir().toString(), pathComponents);
    }
}
