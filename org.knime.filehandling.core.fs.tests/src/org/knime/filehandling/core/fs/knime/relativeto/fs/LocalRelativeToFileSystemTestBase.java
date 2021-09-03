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
 *   Jun 4, 2021 (bjoern): created
 */
package org.knime.filehandling.core.fs.knime.relativeto.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.config.RelativeToFSConnectionConfig;
import org.knime.filehandling.core.fs.knime.relativeto.fs.LocalRelativeToFileSystem;
import org.knime.filehandling.core.fs.knime.relativeto.fs.LocalRelativeToMountpointFSConnection;
import org.knime.filehandling.core.fs.knime.relativeto.fs.LocalRelativeToWorkflowFSConnection;
import org.knime.filehandling.core.testing.WorkflowTestUtil;

/**
 * Tests the {@link LocalRelativeToFileSystem}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class LocalRelativeToFileSystemTestBase {

    @Rule
    public final TemporaryFolder m_tempFolder = new TemporaryFolder();

    File m_mountpointRoot;

    WorkflowManager m_workflowManager;

    @Before
    public void beforeTestCase() throws IOException {
        m_mountpointRoot = m_tempFolder.newFolder("mountpoint-root");
        final Path currentWorkflow = WorkflowTestUtil.createWorkflowDir(m_mountpointRoot.toPath(), "current-workflow");
        WorkflowTestUtil.createWorkflowDir(m_mountpointRoot.toPath(), "other-workflow");
        m_workflowManager = WorkflowTestUtil.getWorkflowManager(m_mountpointRoot, currentWorkflow, false);
        NodeContext.pushContext(m_workflowManager);
    }

    @After
    public void afterTestCase() {
        try {
            WorkflowManager.ROOT.removeProject(m_workflowManager.getID());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    @SuppressWarnings("resource")
    static LocalRelativeToFileSystem getMountpointRelativeFS() throws IOException {
        return new LocalRelativeToMountpointFSConnection(new RelativeToFSConnectionConfig(RelativeTo.MOUNTPOINT)) //
                .getFileSystem(); // NOSONAR must not be closed here
    }

    @SuppressWarnings("resource")
    static LocalRelativeToFileSystem getWorkflowRelativeFS() throws IOException {
        return new LocalRelativeToWorkflowFSConnection(new RelativeToFSConnectionConfig(RelativeTo.WORKFLOW)) //
                .getFileSystem(); // NOSONAR must not be closed here
    }
}
