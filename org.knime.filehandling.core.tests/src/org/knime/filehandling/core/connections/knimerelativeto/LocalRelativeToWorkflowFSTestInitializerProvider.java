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
 */
package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;

/**
 * Test initializer provider for the local workflow relative file system.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToWorkflowFSTestInitializerProvider implements FSTestInitializerProvider {

    private static final String FS_NAME = "knime-local-relative-workflow";

    @Override
    public FSTestInitializer setup(final Map<String, String> configuration) throws IOException {
        final Path localMountPointRoot = Files.createTempDirectory("knime-relative-workflow-test");

        final WorkflowManager workflowManager = LocalRelativeToTestUtil.createAndLoadDummyWorkflow(localMountPointRoot);
        final LocalRelativeToFSConnection fsConnection = new LocalRelativeToFSConnection(Type.WORKFLOW_RELATIVE, true);
        LocalRelativeToTestUtil.shutdownWorkflowManager(workflowManager);
        LocalRelativeToTestUtil.clearDirectoryContents(localMountPointRoot);

        return new LocalRelativeToFSTestInitializer(fsConnection, localMountPointRoot) {
            @Override
            public RelativeToPath getTestCaseScratchDir() {
                // we need to move the scratch dir to outside of the workflow because the workflow
                // directory cannot be access anymore.
                return (RelativeToPath)getFileSystem().getWorkingDirectory().resolve("..")
                    .resolve(Integer.toString(getTestCaseId()));
            }
        };
    }

    @Override
    public String getFSType() {
        return FS_NAME;
    }

    @Override
    public FSLocationSpec createFSLocationSpec(final Map<String, String> configuration) {
        return BaseRelativeToFileSystem.CONNECTED_WORKFLOW_RELATIVE_FS_LOCATION_SPEC;
    }
}
