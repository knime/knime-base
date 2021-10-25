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
package org.knime.filehandling.core.fs.knime.local.relativeto.fs;

import java.io.IOException;
import java.nio.file.Path;

import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.fs.knime.local.workflowaware.LocalWorkflowAwareFileSystem;
import org.knime.filehandling.core.fs.knime.local.workflowaware.LocalWorkflowAwarePath;

/**
 * Local KNIME relative to File System implementation.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public final class LocalRelativeToFileSystem extends LocalWorkflowAwareFileSystem {

    private final RelativeTo m_type;


    /**
     * Default constructor.
     *
     * @param uri URI without a path
     * @param localRoot Where this file system is rooted in the local (platform default) file system.
     * @param type The relative-to type of this file system (workflow, mountpoint, ...).
     * @param workingDir Path (in this file system) that specifies the working directory.
     * @throws IOException
     */
    LocalRelativeToFileSystem(final Path localRoot, //
        final RelativeTo type, //
        final String workingDir, //
        final FSLocationSpec fsLocationSpec) {

        super(new LocalRelativeToFileSystemProvider(), //
            localRoot, //
            workingDir, //
            fsLocationSpec);

        m_type = type;
    }


    @Override
    public LocalWorkflowAwarePath getPath(final String first, final String... more) {
        return new LocalWorkflowAwarePath(this, first, more);
    }

    /**
     * @return {@code true} if this is a workflow relative and {@code false} otherwise.
     */
    boolean isWorkflowRelativeFileSystem() {
        return m_type == RelativeTo.WORKFLOW;
    }

    /**
     * @return {@code true} if this is a mountpoint relative and {@code false} otherwise.
     */
    boolean isMountpointRelativeFileSystem() {
        return m_type == RelativeTo.MOUNTPOINT;
    }

    /**
     * @return {@code true} if this is a workflow data area file system, and {@code false} otherwise.
     */
    boolean isWorkflowDataFileSystem() {
        return m_type == RelativeTo.WORKFLOW_DATA;
    }

    /**
     * @return the {@link RelativeTo} type of this file system.
     */
     RelativeTo getType() {
        return m_type;
    }

    /**
     * Converts a given local file system path into a path string using virtual relative-to path separators.
     *
     * Note: The local (windows) file system might use other separators than the relative-to file system.
     *
     * @param localPath path in local file system
     * @return absolute path in virtual relative to file system
     */
    static String localToRelativeToPathSeparator(final Path localPath) {
        final StringBuilder sb = new StringBuilder();
        final String[] parts = new String[localPath.getNameCount()];
        for (int i = 0; i < parts.length; i++) {
            sb.append(LocalWorkflowAwareFileSystem.PATH_SEPARATOR).append(localPath.getName(i).toString());
        }

        return sb.toString();
    }

}
