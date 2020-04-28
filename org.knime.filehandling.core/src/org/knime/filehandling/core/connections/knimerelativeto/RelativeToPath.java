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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.knime.filehandling.core.connections.base.UnixStylePath;

/**
 * KNIME relative-to file system path.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class RelativeToPath extends UnixStylePath {

    /**
     * Creates a path using a given file system and path parts.
     *
     * @param fileSystem the file system
     * @param first first part of the path
     * @param more subsequent parts of the path
     */
    public RelativeToPath(final BaseRelativeToFileSystem fileSystem, final String first,
        final String... more) {
        super(fileSystem, first, more);
    }

    @Override
    public URI toUri() {
        try {
            final boolean workflowRelativeFS =
                ((BaseRelativeToFileSystem)getFileSystem()).isWorkflowRelativeFileSystem();
            final String path;

            if (workflowRelativeFS && isAbsolute()) {
                path = getFileSystem().getSeparator() + getFileSystem().getWorkingDirectory().relativize(this);
            } else if (workflowRelativeFS) {
                path = getFileSystem().getSeparator() + this;
            } else {
                path = toAbsolutePath().toString();
            }

            return new URI(m_fileSystem.getSchemeString(), m_fileSystem.getHostString(), //
                URIUtil.encodePath(path), null);
        } catch (URIException | URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
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
}
