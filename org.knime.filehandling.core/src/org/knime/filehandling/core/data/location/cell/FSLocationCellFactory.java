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
 *   Feb 24, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.data.location.cell;

import java.util.Objects;

import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * Factory for {@link SimpleFSLocationCell}s. Cells created by such a factory share the same meta data and {@link FileStore}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
// TODO rename?
public final class FSLocationCellFactory {

    /**
     * The {@link DataType} of cells created by this factory.
     */
    public static final DataType TYPE = SimpleFSLocationCell.TYPE;

    private final String m_fsCategory;

    private final String m_fsSpecifier;

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link SimpleFSLocationCell}s.
     *
     * @param fsLocationSpec the file system location spec object holding the type and specifier of the file system
     */
    public FSLocationCellFactory(final FSLocationSpec fsLocationSpec) {
        m_fsCategory = fsLocationSpec.getFileSystemCategory();
        m_fsSpecifier = fsLocationSpec.getFileSystemSpecifier().orElse(null);
    }

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link SimpleFSLocationCell}s.
     *
     * @param fileSystemCategory the file system category
     * @param fileSystemSpecifier the file system specifier, can be {@code null}
     */
    public FSLocationCellFactory(final String fileSystemCategory, final String fileSystemSpecifier) {
        m_fsCategory = fileSystemCategory;
        m_fsSpecifier = fileSystemSpecifier;
    }

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link FSLocationCell}s.
     *
     * @param fileStoreFactory used to create the {@link FileStore} shared by the cells created by this factory
     * @param fileSystemType the file system type
     * @param fileSystemSpecifier the file system specifier, can be {@code null}
     * @deprecated use {@link #FSLocationCellFactory(String, String)} instead
     */
    @Deprecated
    public FSLocationCellFactory(final FileStoreFactory fileStoreFactory, final String fileSystemType,
        final String fileSystemSpecifier) {
        this(fileSystemType, fileSystemSpecifier);
    }

    /**
     * Constructs a {@link FSLocationCellFactory} for the creation of {@link FSLocationCell}s.
     *
     * @param fileStoreFactory used to create the {@link FileStore} shared by the cells created by this factory
     * @param fsLocationSpec the file system location spec object holding the type and specifier of the file system
     * @deprecated use {@link #FSLocationCellFactory(FSLocationSpec)} instead
     */
    @Deprecated
    public FSLocationCellFactory(final FileStoreFactory fileStoreFactory, final FSLocationSpec fsLocationSpec) {
        this(fsLocationSpec);
    }

    /**
     * Create a new {@link FSLocationCell}.
     *
     * @param fsLocation the file system location
     * @return a {@link FSLocationCell} containing a path and information about the file system
     * @throws NullPointerException if {@code fsLocation} is null
     * @throws IllegalArgumentException if the path is null or the file system type and specifier do not match the ones
     *             in the meta data
     */
    public SimpleFSLocationCell createCell(final FSLocation fsLocation) {
        // Check null
        CheckUtils.checkNotNull(fsLocation, "The file system location must not be null.");
        CheckUtils.checkArgumentNotNull(fsLocation.getPath(), "The path must not be null.");
        // Check that fs type and specifier are compatible with meta data
        final String fileSystemCategory = fsLocation.getFileSystemCategory();
        CheckUtils.checkArgument(fileSystemCategory.equals(m_fsCategory),
            "The file system type (%s) must match the file system type in the meta data (%s).", fileSystemCategory,
            m_fsCategory);
        final String fileSystemSpecifier = fsLocation.getFileSystemSpecifier().orElse(null);
        CheckUtils.checkArgument(Objects.equals(fileSystemSpecifier, m_fsSpecifier),
            "The file system specifier (%s) must match the file system specifier in the meta data (%s).",
            fileSystemSpecifier, m_fsSpecifier);
        return new SimpleFSLocationCell(fsLocation);
    }

    /**
     * Create a new {@link FSLocationCell}.
     *
     * @param path the path
     * @return a {@link FSLocationCell} containing a path and information about the file system
     * @throws NullPointerException if {@code path} is null
     */
    public SimpleFSLocationCell createCell(final String path) {
        // Check null
        CheckUtils.checkNotNull(path, "The path must not be null.");
        final FSLocation fsLocation = new FSLocation(m_fsCategory, m_fsSpecifier, path);
        return new SimpleFSLocationCell(fsLocation);
    }
}
