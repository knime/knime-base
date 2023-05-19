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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * A row-wise reader for data in table format.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item to read from
 * @param <C> the type of the {@link ReaderSpecificConfig}
 * @param <T> the type used to identify individual data types
 * @param <V> the type of tokens a row read in consists of
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface GenericTableReader<I, C extends ReaderSpecificConfig<C>, T, V> {

    /**
     * Creates a read object that can be used to read the table in an input item row by row.
     *
     * @param item of the table
     * @param config for reading the table
     * @return a {@link Read} that reads from an input item using the provided {@link TableReadConfig config}
     * @throws IOException if creating the read fails due to IO problems
     */
    // TODO add separate parameter for doing pushdown e.g. filtering
    Read<V> read(I item, TableReadConfig<C> config) throws IOException;

    /**
     * Creates a list of reads that correspond to chunks of item.
     * The order of the list must correspond to the order of the chunks in the table.
     *
     * @param item to read
     * @param config for reading
     * @return a list of reads that correspond to chunks of the item
     * @throws IOException if creating the read fails due to IO problems
     */
    @SuppressWarnings("resource")
    default List<Read<V>> multiRead(final I item, final TableReadConfig<C> config) throws IOException {
        return List.of(read(item, config));
    }

    /**
     * Indicates whether it is possible to read multiple instances of this SourceGroup in parallel.
     *
     * @param sourceGroup to potentially read in parallel
     * @return true if it is save to read multiple instances of the source group in parallel
     */
    default boolean canBeReadInParallel(final SourceGroup<I> sourceGroup) {
        return false;
    }

    /**
     * Reads the spec of the table stored at the input item. Note that the spec should not be filtered i.e. any
     * column filter should be ignored.
     *
     * @param item to read from
     * @param config specifying the read settings
     * @param exec the execution monitor
     * @return a {@link TypedReaderTableSpec} representing the data stored in <b>source</b>
     * @throws IOException if reading fails due to IO problems
     */
    TypedReaderTableSpec<T> readSpec(I item, TableReadConfig<C> config, ExecutionMonitor exec) throws IOException;

    /**
     * Creates the {@link DataColumnSpec} for the provided item with the provided name.
     *
     * @param item for which to create the spec
     * @param name the column should have
     * @return a {@link DataColumnSpec} for the <b>item</b> with the provided <b>name</b>
     */
    DataColumnSpec createIdentifierColumnSpec(I item, String name);

    /**
     * Creates the identifier cell for the provided item.
     *
     * @param item to create the cell for
     * @return a cell that represents <b>item</b>
     */
    DataCell createIdentifierCell(final I item);

}