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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * {@link TableReader} that reads ARFF file.
 *
 * The {@link TableReader} can read the spec and table from a single file. Then, we use {@link TableSpecGuesser} to
 * guess the column types based on the file content.
 *
 * @author Dragan Keselj, Redfield SE
 */
final class ARFFReader implements TableReader<ARFFReaderConfig, DataType, String> {

    /**
     * Column names and types are defined in the ARFF file. {@link TableSpecGuesser} will be used for some ARFF column
     * types, like NUMERIC and DATE, to determine the most specific type for those columns.
     */
    @Override
    public TypedReaderTableSpec<DataType> readSpec(final FSPath path, final TableReadConfig<ARFFReaderConfig> config,
        final ExecutionMonitor exec) throws IOException {
        try (final var read = new ARFFRead(path, config)) {
            final var arffSpec = read.getSpec();
            var specGuesser =
                new TableSpecGuesser<>(ARFFReadAdapterFactory.VALUE_TYPE_HIERARCHY, Function.identity());
            var spec = specGuesser.guessSpec(read, config, exec, path);
            final var specIterator = spec.iterator();
            var i = 0;
            final List<TypedReaderColumnSpec<DataType>> columnSpecs = new ArrayList<>();
            while (specIterator.hasNext()) {
                final var guessedSpec = specIterator.next();
                final var attr = arffSpec.getAttributes().get(i);
                if (guessedSpec.hasType()) {
                    columnSpecs.add(TypedReaderColumnSpec.createWithName(attr.getName(), guessedSpec.getType(), true));
                } else {
                    final var type = getDataTypeForSourceType(attr.getDefaultJavaType());
                    columnSpecs.add(TypedReaderColumnSpec.createWithName(attr.getName(), type, false));
                }
                i++;
            }
            return new TypedReaderTableSpec<>(columnSpecs);
        }
    }

    private static DataType getDataTypeForSourceType(final Class<?> clazz) {
        return JavaToDataCellConverterRegistry.getInstance() //
                .getFactoriesForSourceType(clazz) //
                .stream() //
                .findFirst() //
                .orElseThrow(() -> new IllegalStateException("There is no DataType for source type: " + clazz))
                .getDestinationType();
    }

    @SuppressWarnings("resource") // the caller must close the Read when it is no longer needed
    @Override
    public Read<String> read(final FSPath path, final TableReadConfig<ARFFReaderConfig> config) throws IOException {
        return decorateForReading(new ARFFRead(path, config), config);
    }

    /**
     * Creates a decorated {@link Read}
     *
     * @param path the path of the file to read
     * @param config the {@link TableReadConfig} used
     * @return a decorated read of type {@link Read}
     * @throws IOException if a stream can not be created from the provided file.
     */
    @SuppressWarnings("resource") // it's the responsibility of the caller to close the Read
    private static Read<String> decorateForReading(final ARFFRead read,
        final TableReadConfig<ARFFReaderConfig> config) {
        Read<String> filtered = read;
        if (config.skipRows()) {
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
