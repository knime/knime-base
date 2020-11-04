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
 */
package org.knime.base.node.preproc.manipulator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.knime.base.node.preproc.manipulator.table.Table;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.GenericDefaultMultiTableRead;
import org.knime.filehandling.core.node.table.reader.GenericIndividualTablePreviewRowIterator;
import org.knime.filehandling.core.node.table.reader.PreviewRowIterator;
import org.knime.filehandling.core.node.table.reader.config.GenericTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.GenericRead;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.util.GenericIndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.util.CheckedExceptionFunction;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
final class RowInputMultiTableRead extends GenericDefaultMultiTableRead<Table, DataValue> {

    /**
     * Constructor.
     *
     * @param paths the list of {@link Table Paths} to read from
     * @param readFn produces a {@link Read} from a {@link Path}
     * @param individualTableReaderFactory creates {@link IndividualTableReader IndividualTableReaders} from {@link Path
     *            Paths}
     * @param tableSpecConfig corresponding to this instance
     * @param outputSpec {@link DataTableSpec} of the output table
     */
    RowInputMultiTableRead(final List<Table> paths,
        final CheckedExceptionFunction<Table, ? extends GenericRead<Table, DataValue>, IOException> readFn,
        final Supplier<BiFunction<Table, FileStoreFactory, ? extends GenericIndividualTableReader<Table, DataValue>>> individualTableReaderFactorySupplier,
        final GenericTableSpecConfig<Table> tableSpecConfig, final DataTableSpec outputSpec) {
        super(paths, readFn, individualTableReaderFactorySupplier, tableSpecConfig, outputSpec);
    }

    @SuppressWarnings("resource")
    @Override
    public PreviewRowIterator createPreviewIterator() {
        final BiFunction<Table, FileStoreFactory, ? extends GenericIndividualTableReader<Table, DataValue>> individualTableReaderFactory =
            getIndividualTableReaderFactory();
        return new RowInputMultiTablePreviewRowIterator(getItems().iterator(), (p, f) -> {
            final GenericIndividualTableReader<Table, DataValue> reader = individualTableReaderFactory.apply(p, f);
            return new GenericIndividualTablePreviewRowIterator<>(getReadFn().apply(p), reader::toRow);
        });
    }
}
