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
 *   Oct 23, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.data.location.cell;

import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;

/**
 * A factory class allowing to easily create {@link FSLocationCell}s. This class is especially useful when the same
 * column contains {@link FSLocationCell}s originating from / pointing to different {@link FileSystem}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class MultiFSLocationCellFactory implements AutoCloseable {

    private final Map<DefaultFSLocationSpec, Pair<FileStoreFactory, FSLocationCellFactory>> m_factories;

    /**
     * Constructor.
     */
    public MultiFSLocationCellFactory() {
        m_factories = new HashMap<>();
    }

    /**
     * Create a new {@link FSLocationCell}.
     *
     * @param exec the {@link ExecutionContext} used to create the {@link FileStoreFactory}
     * @param fsLocation the file system location
     * @return a {@link FSLocationCell} containing a path and information about the file system
     * @throws NullPointerException if {@code fsLocation} is null
     * @throws IllegalArgumentException if the path is null or the file system type and specifier do not match the ones
     *             in the meta data
     */
    public FSLocationCell createCell(final ExecutionContext exec, final FSLocation fsLocation) {
        final DefaultFSLocationSpec defaultFSLocationSpec = new DefaultFSLocationSpec(fsLocation.getFSCategory(),
            fsLocation.getFileSystemSpecifier().orElseGet(() -> null));
        final FSLocationCellFactory fac = m_factories
            .computeIfAbsent(defaultFSLocationSpec, fsLocSpec -> createFactory(exec, fsLocation)).getSecond();
        return fac.createCell(fsLocation);
    }

    private static Pair<FileStoreFactory, FSLocationCellFactory> createFactory(final ExecutionContext exec,
        final FSLocationSpec fsLocSpec) {
        final FileStoreFactory fileStoreFac = FileStoreFactory.createFileStoreFactory(exec);
        return new Pair<>(fileStoreFac, new FSLocationCellFactory(fileStoreFac, fsLocSpec));
    }

    @Override
    public void close() {
        m_factories.values().stream()//
            .map(Pair::getFirst)//
            .forEach(FileStoreFactory::close);
    }

}
