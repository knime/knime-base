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
 *   27 May 2021 (moditha.hewasinghge): created
 */
package org.knime.base.node.io.filehandling.table.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

/**
 * Reader for the table reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
final class KnimeTableReader implements TableReader<TableManipulatorConfig, DataType, DataValue> {

    private static final String ERROR_MSG_DATA_BIN_EXCEPTION =
        "Cannot read file! The file is either not in KNIME table format or corrupted.";

    private static final String DATA_BIN_EXCEPTION = "No entry data.bin in file";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(KnimeTableReader.class);

    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    @Override
    public Read<DataValue> read(final FSPath path, final TableReadConfig<TableManipulatorConfig> config)
        throws IOException {
        return decorateForReading(new KnimeTableRead(path, config), config);
    }

    @Override
    public TypedReaderTableSpec<DataType> readSpec(final FSPath path,
        final TableReadConfig<TableManipulatorConfig> config, final ExecutionMonitor exec) throws IOException {

        final List<TypedReaderColumnSpec<DataType>> columnSpecs = new ArrayList<>();
        final DataTableSpec spec = readTableSpec(path);
        for (final DataColumnSpec colSpec : spec) {
            columnSpecs.add(TypedReaderColumnSpec.createWithName(colSpec.getName(), colSpec.getType(), true));
        }
        return new TypedReaderTableSpec<>(columnSpecs);

    }

    private static InputStream openInputStream(final Path path) throws IOException {
        return new CompressionAwareCountingInputStream(path);
    }

    private static DataTableSpec readTableSpec(final Path path) throws IOException {
        Optional<DataTableSpec> spec;
        try {
            spec = getTableSpec(path);
            if (spec.isEmpty()) { // if written with 1.3.x and before
                spec = Optional.of(getTableSpecForOldFiles(path));
            }
        } catch (IOException ioe) {
            String message = ioe.getMessage();
            if (message == null) {
                message = "Unable to read spec from file, no detailed message available.";
            }
            if (message.equals(DATA_BIN_EXCEPTION)) {
                message = ERROR_MSG_DATA_BIN_EXCEPTION;
            }
            throw new IOException(message, ioe);
        }
        return spec.get();
    }

    private static Optional<DataTableSpec> getTableSpec(final Path path) throws IOException {
        try (InputStream in = openInputStream(path)) {
            return DataContainer.peekDataTableSpec(in);
        }
    }

    private static DataTableSpec getTableSpecForOldFiles(final Path path) throws IOException {
        DataTableSpec spec;
        LOGGER.debug("Table spec is not first entry in input file, need to deflate entire file");
        try (InputStream in = openInputStream(path); ContainerTable outTable = DataContainer.readFromStream(in)) {
            spec = outTable.getDataTableSpec();
        }
        return spec;
    }

    /*
    * Creates a decorated {@link Read} from {@link KnimeTableRead}, taking into account how many rows should be skipped or
    * what is the maximum number of rows to read.
    *
    * @param path the path of the file to read
    * @param config the {@link TableReadConfig} used
    * @return a decorated read of type {@link Read}
    * @throws IOException if a stream can not be created from the provided file.
    */
    @SuppressWarnings("resource") // closing the read is the responsibility of the caller
    private static Read<DataValue> decorateForReading(final KnimeTableRead read,
        final TableReadConfig<TableManipulatorConfig> config) {
        Read<DataValue> filtered = read;
        final boolean skipRows = config.skipRows();
        if (skipRows) {
            final long numRowsToSkip = config.getNumRowsToSkip();
            filtered = ReadUtils.skip(filtered, numRowsToSkip);
        }
        if (config.limitRows()) {
            final long numRowsToKeep = config.getMaxRows();
            filtered = ReadUtils.limit(filtered, numRowsToKeep);
        }
        return filtered;
    }
}
