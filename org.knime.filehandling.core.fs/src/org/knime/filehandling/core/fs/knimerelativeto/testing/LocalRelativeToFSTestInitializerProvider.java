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
 *   Mar 12, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knimerelativeto.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.testing.DefaultFSTestInitializerProvider;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.FSTestInitializerProvider;
import org.knime.filehandling.core.testing.WorkflowTestUtil;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Abstract {@link FSTestInitializerProvider} base class for all file systems in the category
 * {@link FSCategory#RELATIVE}. These file systems require a {@link WorkflowContext} to be instantiated. A
 * {@link WorkflowContext} may or may not exist, depending on where this {@link FSTestInitializerProvider} is executed
 * (e.g. inside a JUnit test, or as part of a test workflow). This class makes sure that either an existing
 * {@link WorkflowContext} is reused, or that a fake temporary one is created ad-hoc.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public abstract class LocalRelativeToFSTestInitializerProvider extends DefaultFSTestInitializerProvider {

    private final FSType m_fsType;

    private final FSLocationSpec m_fsLocationSpec;

    /**
     * Constructor.
     *
     * @param fsType The file system type.
     * @param fsLocationSpec The {@link FSLocationSpec} of the file system to be created.
     */
    protected LocalRelativeToFSTestInitializerProvider(final FSType fsType, final FSLocationSpec fsLocationSpec) {
        m_fsType = fsType;
        m_fsLocationSpec = fsLocationSpec;
    }

    @Override
    public final LocalRelativeToFSTestInitializer setup(final Map<String, String> configuration) throws IOException {
        final LocalRelativeToFSTestInitializer testInitializer;

        if (WorkflowContextUtil.hasWorkflowContext()) {
            testInitializer = createTestInitializer(configuration);
        } else {
            final Path localMountpointRoot = Files.createTempDirectory("knime-relative-test");
            final WorkflowManager workflowManager =
                WorkflowTestUtil.createAndLoadDummyWorkflow(localMountpointRoot);
            testInitializer = createTestInitializer(configuration);
            WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
            WorkflowTestUtil.clearDirectoryContents(localMountpointRoot);
        }

        return testInitializer;
    }

    /**
     * Subclasses need to implement this method to actually instantiate the {@link FSTestInitializer} including the
     * underlying {@link FSConnection}.
     *
     * @param configuration Any configuration that may be required to instantiate the {@link FSConnection};
     * @return a newly create {@link FSTestInitializer}.
     * @throws IOException if something went wrong while creating the file system or the test initializer.
     */
    protected abstract LocalRelativeToFSTestInitializer createTestInitializer(final Map<String, String> configuration)
        throws IOException;

    @Override
    public final FSType getFSType() {
        return m_fsType;
    }

    @Override
    public final FSLocationSpec createFSLocationSpec(final Map<String, String> configuration) {
        return m_fsLocationSpec;
    }
}
