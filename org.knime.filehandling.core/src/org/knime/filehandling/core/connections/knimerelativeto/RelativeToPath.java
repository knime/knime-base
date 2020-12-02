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
package org.knime.filehandling.core.connections.knimerelativeto;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.knime.filehandling.core.connections.WorkflowAwarePath;
import org.knime.filehandling.core.connections.base.UnixStylePath;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection.Type;

/**
 * KNIME relative-to file system path.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class RelativeToPath extends UnixStylePath implements WorkflowAwarePath {

    /**
     * Creates a path using a given file system and path parts.
     *
     * @param fileSystem the file system
     * @param first first part of the path
     * @param more subsequent parts of the path
     */
    public RelativeToPath(final BaseRelativeToFileSystem fileSystem, final String first, final String... more) {
        super(fileSystem, first, more);
    }

    public URI toKNIMEProtocolURI() {
        try {

            final Type type = ((BaseRelativeToFileSystem)getFileSystem()).getType();

            switch (type) {
                case MOUNTPOINT_RELATIVE:
                    return toMountpointRelativeURI();
                case WORKFLOW_RELATIVE:
                    return toWorkflowRelativeURI();
                case WORKFLOW_DATA_RELATIVE:
                    return toWorkflowDataRelativeURI();
                default:
                    throw new IllegalArgumentException("Illegal type " + type);
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private URI toWorkflowDataRelativeURI() throws URISyntaxException {
        final String path = getFileSystem().getSeparator() + "data" + toAbsolutePath().toString();
        return new URI("knime", "knime.workflow", path, null);
    }

    private URI toWorkflowRelativeURI() throws URISyntaxException {
        final String urlPath = getFileSystem().getSeparator()
            + getFileSystem().getWorkingDirectory().relativize(toAbsolutePath()).toString();
        return new URI("knime", "knime.workflow", urlPath, null);
    }

    private URI toMountpointRelativeURI() throws URISyntaxException {
        final String urlPath = toAbsolutePath().toString();
        return new URI("knime", "knime.mountpoint", urlPath, null);
    }

    /**
     * Appends this path to the given base directory without file system specific separators.
     *
     * @param baseDir base directory to append this path to
     * @return base directory with this path appended
     */
    public Path appendToBaseDir(final Path baseDir) {
        return Paths.get(baseDir.toString(), m_pathParts.toArray(new String[0]));
    }

    @Override
    public boolean isWorkflow() throws IOException {
        @SuppressWarnings("resource")
        final BaseRelativeToFileSystem fs = (BaseRelativeToFileSystem)getFileSystem();
        return fs.isWorkflowDirectory(this);
    }
}
