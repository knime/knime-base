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
 *   Jan 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.stream.IntStream;

import org.knime.core.data.DataRow;
import org.knime.core.data.convert.map.DataRowProducer;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowOutput;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;

/**
 * Coordinates reading and {@link DataRow} creation via the {@link MappingFramework}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <A> the concrete type of {@link ReadAdapter}
 */
final class IndividualTableReader<A extends ReadAdapter<?, ?>> {

    private final DataRowProducer<ReadAdapterParams<A>> m_rowProducer;

    private final A m_readAdapter;

    private final ReadAdapterParams<A>[] m_params;

    /**
     * Constructor
     *
     * @param readAdapter to create rows from
     * @param fsFactory required by the mapping framework
     * @param productionPaths defining how to map all columns
     */
    IndividualTableReader(final A readAdapter, final FileStoreFactory fsFactory,
        final ProductionPath[] productionPaths) {
        m_readAdapter = readAdapter;
        // ReadAdapterParams are compatible with any ReadAdapter, the generics
        // are only necessary to shut up the compiler
        @SuppressWarnings("unchecked")
        final ReadAdapterParams<A>[] params = IntStream.range(0, productionPaths.length)
            .mapToObj(ReadAdapterParams::new).toArray(ReadAdapterParams[]::new);
        m_params = params;
        m_rowProducer = MappingFramework.createDataRowProducer(fsFactory, m_readAdapter, productionPaths);
    }

    private DataRow next() throws Exception {
        if (m_readAdapter.next() == null) {
            return null;
        }
        // reads the tokens from m_readAdapter and converts them into a DataRow
        return m_rowProducer.produceDataRow(m_readAdapter.getKey(), m_params);
    }

    /**
     * Pushes all rows contained in the {@link ReadAdapter} provided in the constructor to {@link RowOutput output}.
     *
     * @param output to push to (must be compatible i.e. have the same spec)
     * @param progress used for cancelation and progress reporting (provided the size of the read is known)
     * @throws Exception if something goes astray
     */
    void fillOutput(final RowOutput output, final ExecutionMonitor progress) throws Exception {
        if (m_readAdapter.getEstimatedSizeInBytes().isPresent()) {
            fillOutputWithProgress(output, progress);
        } else {
            fillOutputWithoutProgress(output, progress);
        }
    }

    private void fillOutputWithoutProgress(final RowOutput output, final ExecutionMonitor progress) throws Exception {
        DataRow next;
        for (long i = 1; (next = next()) != null; i++) {
            progress.checkCanceled();
            final long finalI = i;
            progress.setMessage(() -> String.format("Reading row %s", finalI));
            output.push(next);
        }
    }

    private void fillOutputWithProgress(final RowOutput output, final ExecutionMonitor progress) throws Exception {
        final long size = m_readAdapter.getEstimatedSizeInBytes().orElseThrow(() -> new IllegalStateException(
            "Coding error! Only call this method if the estimated size of the read is known."));
        final double doubleSize = size;
        DataRow next;
        for (long i = 1; (next = next()) != null; i++) {
            progress.checkCanceled();
            final long finalI = i;
            progress.setProgress(i / doubleSize, () -> String.format("Reading row %s/%s", finalI, size));
            output.push(next);
        }
    }

}