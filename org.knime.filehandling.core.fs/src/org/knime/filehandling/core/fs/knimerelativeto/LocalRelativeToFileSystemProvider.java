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
 *   Feb 11, 2020 (Sascha Wolke, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knimerelativeto;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.knime.core.node.workflow.WorkflowContext;
import org.knime.filehandling.core.connections.WorkflowAware;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Local KNIME relative to File System provider.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
final class LocalRelativeToFileSystemProvider extends BaseRelativeToFileSystemProvider<LocalRelativeToFileSystem>
    implements WorkflowAware {

    @SuppressWarnings("resource")
    @Override
    public void deployWorkflow(final File source, final Path dest, final boolean overwrite, final boolean attemptOpen)
        throws IOException {

        if (getFileSystemInternal().isWorkflowDataFileSystem()) {
            throw new UnsupportedOperationException("Cannot deploy workflow to the workflow data area.");
        }

        final RelativeToPath absoluteDest = checkCastAndAbsolutizePath(dest);

        final String currentMountpoint = getCurrentMountpoint();
        try {
            MountPointFileSystemAccessService.instance().deployWorkflow( //
                source, //
                new URI("knime", currentMountpoint, absoluteDest.toString(), null), //
               overwrite, //
               attemptOpen);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static String getCurrentMountpoint() {
        final WorkflowContext context = WorkflowContextUtil.getWorkflowContext();
        return context.getMountpointURI() //
                .orElseThrow(() -> new IllegalStateException("Cannot determine name of mountpoint to deploy workflow.")) //
                .getAuthority();
    }

    @Override
    public File toLocalWorkflowDir(final Path src) throws IOException {
        final Path absoluteSrc = src.toAbsolutePath().normalize();
        String currentMountpoint = getCurrentMountpoint();
        URI uri;
        try {
            uri = new URI("knime", currentMountpoint, absoluteSrc.toString(), null);
            return MountPointFileSystemAccessService.instance().toLocalWorkflowDir(uri);
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Converts a given local file system path into a path string using virtual relative-to path separators.
     *
     * Note: The local (windows) file system might use other separators than the relative-to file system.
     *
     * @param localPath path in local file system
     * @return absolute path in virtual relative to file system
     */
    static String localToRelativeToPathSeperator(final Path localPath) {
        final StringBuilder sb = new StringBuilder();
        final String[] parts = new String[localPath.getNameCount()];
        for (int i = 0; i < parts.length; i++) {
            sb.append(BaseRelativeToFileSystem.PATH_SEPARATOR).append(localPath.getName(i).toString());
        }

        return sb.toString();
    }

    @Override
    public Optional<String> getMountID() {
        // currently this is unused code, but it will need to be made functional
        // for AP-16355
        return Optional.empty();
    }
}
