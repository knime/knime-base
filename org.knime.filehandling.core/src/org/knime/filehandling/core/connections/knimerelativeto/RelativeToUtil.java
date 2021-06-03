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
 *   Jun 23, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.knimerelativeto;

import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactoryMapBuilder;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class RelativeToUtil {

    static final Map<URIExporterID, URIExporterFactory> RELATIVE_TO_URI_EXPORTERS = new URIExporterFactoryMapBuilder() //
        .add(URIExporterIDs.DEFAULT, LegacyKNIMEUrlExporterFactory.getInstance()) //
        .add(URIExporterIDs.LEGACY_KNIME_URL, LegacyKNIMEUrlExporterFactory.getInstance()) //
        .build();

    private RelativeToUtil() {
    }

    /**
     * @return current {@link WorkflowContext} from the {@link NodeContext}
     */
    public static WorkflowContext getWorkflowContext() {
        final NodeContext nodeContext = NodeContext.getContext();
        Validate.notNull(nodeContext, "Node context required.");

        final WorkflowContext workflowContext = nodeContext.getWorkflowManager().getContext();
        Validate.notNull(workflowContext, "Workflow context required.");

        return workflowContext;
    }

    /**
     * @return true, when we curently have a workflow context, false otherwise.
     */
    public static boolean hasWorkflowContext() {
        final NodeContext nodeContext = NodeContext.getContext();
        return nodeContext != null && nodeContext.getWorkflowManager().getContext() != null;
    }

    /**
     * Validates if the given context is running in a server context.
     *
     * @param context workflow context to validate
     * @return {@code true} if the given context is a server context
     */
    public static boolean isServerContext(final WorkflowContext context) {
        return context.getRemoteRepositoryAddress().isPresent() && context.getServerAuthToken().isPresent();
    }

    /**
     * Checks whether the calling thread is running in a KNIME node as part of a workflow running on KNIME Server.
     *
     * @return true if the current {@link WorkflowContext} belongs to a workflow running on KNIME Server, false
     *         otherwise.
     * @throws IllegalArgumentException If there is no current {@link NodeContext} or {@link WorkflowContext}.
     */
    public static boolean isServerContext() {
        final NodeContext nodeContext = NodeContext.getContext();
        CheckUtils.checkArgumentNotNull(nodeContext, "Node context required.");

        final WorkflowContext context = nodeContext.getWorkflowManager().getContext();
        CheckUtils.checkArgumentNotNull(context, "Workflow context required.");

        return isServerContext(context);
    }
}
