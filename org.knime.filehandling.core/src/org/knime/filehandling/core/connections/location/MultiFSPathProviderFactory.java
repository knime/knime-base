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
 *   Dec 17, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.location;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * A factory class allowing to easily create {@link FSPathProviderFactory}s. This class is especially useful when
 * multiple {@link FSLocation}s use the same {@link FileSystem} as the {@link FSPathProviderFactory}s are created only
 * once. If a {@link FSConnection} is available and set during construction, all incoming {@link FSLocation}s will be
 * resolved against the connected file system, no matter which file systems are referenced by them.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class MultiFSPathProviderFactory implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MultiFSPathProviderFactory.class);

    private final Map<DefaultFSLocationSpec, FSPathProviderFactory> m_factories;

    private final FSPathProviderFactory m_connectedFactory;

    /**
     * Constructor.
     *
     * @param fsConnection the {@link FSConnection}, can be {@code null}
     */
    public MultiFSPathProviderFactory(final FSConnection fsConnection) {
        m_factories = new HashMap<>();
        if (fsConnection != null) {
            m_connectedFactory = FSPathProviderFactory.newFactory(Optional.of(fsConnection),
                new DefaultFSLocationSpec(FSCategory.CONNECTED));
        } else {
            m_connectedFactory = null;
        }
    }

    /**
     * Constructor.
     */
    public MultiFSPathProviderFactory() {
        this(null);
    }

    /**
     * Converts and a new {@link FSLocation} to a {@link String} containing a path that starts with a KNIME URL.
     *
     * @param fsLocation the file system location
     * @return a {@link String} containing a path that starts with a KNIME URL
     */
    public FSPathProviderFactory getOrCreateFSPathProviderFactory(final FSLocation fsLocation) {
        if (m_connectedFactory != null) {
            return m_connectedFactory;
        }
        // check and fail if the fsLocation requires a connection
        if (fsLocation.getFSCategory() == FSCategory.CONNECTED) {
            throw new IllegalArgumentException(String.format(
                "The path '%s' references a connected file system (%s). Please add the missing file system connection "
                    + "port.",
                fsLocation.getPath(), fsLocation.getFileSystemSpecifier().orElse("")));
        }
        // check if the proper path provider factory has already been created and return if so or create a new one
        final DefaultFSLocationSpec defaultFSLocationSpec = new DefaultFSLocationSpec(fsLocation.getFSCategory(),
            fsLocation.getFileSystemSpecifier().orElseGet(() -> null));
        return m_factories.computeIfAbsent(defaultFSLocationSpec, MultiFSPathProviderFactory::createFactory);
    }

    private static FSPathProviderFactory createFactory(final FSLocationSpec fsLocationSpec) {
        return FSPathProviderFactory.newFactory(Optional.empty(), fsLocationSpec);
    }

    @Override
    public void close() {
        try {
            if (m_connectedFactory != null) {
                m_connectedFactory.close();
            }
            for (final FSPathProviderFactory f : m_factories.values()) {
                f.close();
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to close fs path provider factory.", e);
        }
    }

}
