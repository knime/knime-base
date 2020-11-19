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
 *   Mar 6, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Abstract super class for implemented by all NIO file systems in KNIME to represent paths. This class adds conversion
 * to {@link FSLocation} (see {@link #toFSLocation()}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public abstract class FSPath implements Path {

    @Override
    public abstract FSFileSystem<? extends FSPath> getFileSystem();

    /**
     * Creates a {@link Stream} over the name components of this path, that result from invoking {@link #toString()} on
     * each of them.
     *
     * @return a {@link Stream} over the stringified name components of this path.
     */
    public Stream<String> stringStream() {
        return pathStream().map(Path::toString);
    }

    /**
     * Creates a {@link Stream} over the name components of this path.
     *
     * @return a {@link Stream} over the name components of this path.
     */
    public Stream<Path> pathStream() {
        final Path[] paths = new Path[getNameCount()];
        for (int i = 0; i < getNameCount(); i++) {
            paths[i] = getName(i);
        }
        return Arrays.stream(paths);
    }

    /**
     * Converts this path into an {@link FSLocation} object.
     *
     * @return a new {@link FSLocation} object
     * @see FSFileSystem#getPath(FSLocation)
     */
    @SuppressWarnings("resource")
    public FSLocation toFSLocation() {
        final FSFileSystem<?> fs = getFileSystem();
        return new FSLocation(fs.getFileSystemCategory().toString(), fs.getFileSystemSpecifier().orElseGet(() -> null),
            toString());
    }

    @Override
    public final URI toUri() {
        try {
            @SuppressWarnings("resource")
            final URI baseURI = getFileSystem().getFileSystemBaseURI();
            return new URI(baseURI.getScheme(), baseURI.getAuthority(), toAbsolutePath().toString(), null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
