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
 *   May 13, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Optional;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * A factory for creating {@link FSLocation} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FSLocationFactory implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FSLocationFactory.class);

    private final FSLocationSpec m_spec;

    private final boolean m_isRelativeTo;

    private final FSPathProviderFactory m_pathProviderFactory;

    /**
     * Creates a {@link FSLocationFactory} from the provided {@link FSLocationSpec}.
     *
     * @param spec identifying the used file system
     * @param connection the optional {@link FSConnection} (only non-empty if the file system is provided via port
     *            object)
     */
    public FSLocationFactory(final FSLocationSpec spec, final Optional<FSConnection> connection) {
        m_spec = CheckUtils.checkArgumentNotNull(spec, "The spec must not be null.");
        m_isRelativeTo = spec.getFileSystemChoice() == Choice.KNIME_FS;
        m_pathProviderFactory = FSPathProviderFactory.newFactory(connection, spec);
    }

    /**
     * Creates a {@link FSLocation} from the provided <b>path</b>.
     *
     * @param path to create a {@link FSLocation} for
     * @return the {@link FSLocation} corresponding to <b>path</b>
     * @throws InvalidPathException if the path is invalid
     */
    public FSLocation createLocation(final String path) {
        final FSLocation unvalidatedLocation =
            new FSLocation(m_spec.getFileSystemType(), m_spec.getFileSystemSpecifier()
                .orElseThrow(() -> new IllegalStateException("FSLocationSpec is missing specifier.")), path);
        FSLocation validatedLocation = null;
        try (FSPathProvider pathProvider = m_pathProviderFactory.create(unvalidatedLocation)) {
            final FSPath fsPath = pathProvider.getPath();
            if (m_isRelativeTo) {
                validateRelativeTo(fsPath);
            }
            validatedLocation = fsPath.toFSLocation();
        } catch (IOException ex) {
            // something went wrong while closing the provider (this is unlikely to happen)
            // we only log the issue because the actual computation succeeded.
            LOGGER.error("Closing the path provider caused an exception.", ex);
        }
        return validatedLocation;
    }

    private static void validateRelativeTo(final FSPath path) {
        if (path.isAbsolute()) {
            throw new InvalidPathException(path.toString(),
                "The path must be relative, i.e. it must not start with '/'.");
        }
    }

    @Override
    public void close() throws IOException {
        m_pathProviderFactory.close();
    }
}
