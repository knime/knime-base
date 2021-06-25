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
 *   Jun 23, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.knimeremote;

import java.io.IOException;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSDescriptor;
import org.knime.filehandling.core.connections.meta.FSDescriptorProvider;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.meta.FSTypeRegistry;
import org.knime.filehandling.core.connections.meta.base.BaseFSDescriptor;
import org.knime.filehandling.core.connections.meta.base.BaseFSDescriptorProvider;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Special {@link FSDescriptorProvider} for {@link FSType#MOUNTPOINT}, which usually delegates to a KNIME Explorer-based
 * file system. Only when running on a Server Executor and trying a to access the remote workflow repository, then this
 * delegates to a REST-based file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 *
 */
public final class MountpointFSDescriptorProvider extends BaseFSDescriptorProvider {

    private static final String SERVER_SIDE_FS_TYPE = "knime-rest-absolute-mountpoint";

    /**
     * Constructor.
     */
    public MountpointFSDescriptorProvider() {
        super(FSType.MOUNTPOINT, //
            new BaseFSDescriptor.Builder() //
                .withConnectionFactory(MountpointFSDescriptorProvider::getActualFSConnection) //
                .withIsWorkflowAware(true) //
                .withURIExporterFactory(URIExporterIDs.DEFAULT, LegacyKNIMEUrlExporterFactory.getInstance()) //
                .withURIExporterFactory(URIExporterIDs.LEGACY_KNIME_URL, LegacyKNIMEUrlExporterFactory.getInstance()) //
                .build());
    }


    private static FSConnection getActualFSConnection(final MountpointFSConnectionConfig config) throws IOException {
        final var workflowContext = WorkflowContextUtil.getWorkflowContext();

        if (WorkflowContextUtil.isServerContext(workflowContext)) {
            final String remoteMountId = workflowContext.getRemoteMountId() //
                .orElseThrow(() -> new IllegalStateException("No remote mount ID"));

            if (config.getMountID().equals(remoteMountId)) {
                return getRestFSDescriptor().getConnectionFactory().createConnection(config);
            }
        }

        return new KNIMERemoteFSConnection(config);
    }

    private static FSDescriptor getRestFSDescriptor() {
        final FSType restFsType = FSTypeRegistry.getFSType(SERVER_SIDE_FS_TYPE) //
                .orElseThrow(() -> new IllegalStateException("REST-based Mountpoint FSType not registered"));

        return FSDescriptorRegistry.getFSDescriptor(restFsType) //
                .orElseThrow(() -> new IllegalStateException("REST-based Mountpoint file system not registered"));
    }
}
