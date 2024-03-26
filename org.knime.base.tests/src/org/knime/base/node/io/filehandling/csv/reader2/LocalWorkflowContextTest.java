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
 *   Mar 27, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Initializes a workflow context. Needed, e.g., when accessing file systems.
 *
 * @author Paul Bärnreuther
 */
abstract class LocalWorkflowContextTest {

    final private static String SOURCE_OBJ = "workflowContextSourceObject";

    private NodeContext.ContextObjectSupplier m_contextObjectSupplier;

    /**
     * A workflow with local context named "workflow"
     */
    protected WorkflowManager m_wfm;

    @BeforeEach
    void createWorkflowManagerWithContext() throws IOException {
        final var workflowContext = createLocalWorkflowContext(Files.createTempDirectory("workflow"));

        m_contextObjectSupplier = new NodeContext.ContextObjectSupplier() {
            @SuppressWarnings("unchecked")
            @Override
            public <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final Object srcObj) {
                if ((WorkflowContextV2.class.isAssignableFrom(contextObjClass)) && SOURCE_OBJ.equals(srcObj)) {
                    return Optional.of((C)workflowContext);
                }
                return Optional.empty();
            }
        };

        NodeContext.addContextObjectSupplier(m_contextObjectSupplier);
        NodeContext.pushContext(SOURCE_OBJ);
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
    }

    private static WorkflowContextV2 createLocalWorkflowContext(final Path workflowPath) throws IOException {
        var executorInfo =
            AnalyticsPlatformExecutorInfo.builder().withUserId("knime").withLocalWorkflowPath(workflowPath).build();
        var locationInfo = LocalLocationInfo.getInstance(null);
        return WorkflowContextV2.builder().withExecutor(executorInfo).withLocation(locationInfo).build();
    }

    @AfterEach
    void removeContextAndDisposeWorkflow() {
        NodeContext.removeContextObjectSupplier(m_contextObjectSupplier);
        NodeContext.removeLastContext();
        WorkflowManagerUtil.disposeWorkflow(m_wfm);
    }

}
