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
 *   Jan 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * A row-wise reader for data in table format.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of the {@link ReaderSpecificConfig}
 * @param <T> the type used to identify individual data types
 * @param <V> the type of tokens a row read in consists of
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface TableReader<C extends ReaderSpecificConfig<C>, T, V> extends GenericTableReader<FSPath, C, T, V> {

    @Override
    Read<V> read(FSPath path, TableReadConfig<C> config) throws IOException;

    @Override
    TypedReaderTableSpec<T> readSpec(FSPath path, TableReadConfig<C> config, ExecutionMonitor exec) throws IOException;

    @Override
    default DataColumnSpec createIdentifierColumnSpec(final FSPath item, final String name) {
        final DataColumnSpecCreator creator = new DataColumnSpecCreator(name, SimpleFSLocationCellFactory.TYPE);
        final FSLocationSpec spec = item.toFSLocation();
        creator.addMetaData(
            new FSLocationValueMetaData(spec.getFileSystemCategory(), spec.getFileSystemSpecifier().orElse(null)),
            true);
        return creator.createSpec();
    }

    @Override
    default DataCell createIdentifierCell(final FSPath item) {
        final FSLocation fsLocation = item.toFSLocation();
        return new SimpleFSLocationCellFactory(fsLocation).createCell(fsLocation);
    }

    @Override
    default boolean canBeReadInParallel(final SourceGroup<FSPath> sourceGroup) {
        return isLocalPath(sourceGroup.iterator().next());
    }

    /**
     * Checks if a FSPath is on this machine.
     *
     * @param path to check for being a local path
     * @return true if the path is located on the local machine
     */
    public static boolean isLocalPath(final FSPath path) {
        var fsType = path.toFSLocation().getFSType();
        return fsType == FSType.LOCAL_FS//
                || fsType == FSType.RELATIVE_TO_WORKFLOW//
                || fsType == FSType.RELATIVE_TO_WORKFLOW_DATA_AREA;
    }

}
