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
 *   Jan 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Function;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.preview.PreviewExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * Guesses the spec of a table by finding the most specific type of every column using a TypeHierarchy provided by the
 * client.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify data types
 * @param <V> the type used as values
 */
public final class TableSpecGuesser<T, V> {

    private static final int PROGRESS_UPDATE_INTERVAL = 1000;

    private final TypeHierarchy<T, V> m_typeHierarchy;

    private final Function<V, String> m_valueToString;

    /**
     * Constructor.
     *
     * @param typeHierarchy used to resolve type conflicts
     * @param columnNameExtractor used to extract column names from values
     */
    public TableSpecGuesser(final TypeHierarchy<T, V> typeHierarchy, final Function<V, String> columnNameExtractor) {
        m_typeHierarchy = CheckUtils.checkArgumentNotNull(typeHierarchy, "The typeHierarchy must not be null.");
        m_valueToString =
            CheckUtils.checkArgumentNotNull(columnNameExtractor, "The columnNameExtractor must not be null.");
    }

    /**
     * Guesses the {@link TypedReaderTableSpec} from the rows provided by the {@link Read read}.</br>
     *
     * @param read providing the rows to guess the spec from
     * @param config providing the user settings
     * @param exec the execution monitor
     * @return the guessed spec
     * @throws IOException if I/O problems occur
     */
    public TypedReaderTableSpec<T> guessSpec(final Read<V> read, final TableReadConfig<?> config,
        final ExecutionMonitor exec) throws IOException {
        try (final ExtractColumnHeaderRead<V> source = wrap(read, config)) {
            return guessSpec(source, config, exec);
        }
    }

    /**
     * Guesses the {@link TypedReaderTableSpec} from the rows provided by the {@link ExtractColumnHeaderRead read}.</br>
     * <i>Note:</i> The contract of this method is that the read obeys the settings, i.e., it is only processing the
     * proper data rows.
     *
     * @param read providing the rows to guess the spec from
     * @param config providing the user settings
     * @param exec the execution monitor
     * @return the guessed spec
     * @throws IOException if I/O problems occur
     */
    public TypedReaderTableSpec<T> guessSpec(final ExtractColumnHeaderRead<V> read, final TableReadConfig<?> config,
        final ExecutionMonitor exec) throws IOException {
        try (Read<V> filtered = filterColIdx(read, config)) {
            final TypeGuesser<T, V> typeGuesser = guessTypes(filtered, config.allowShortRows(), exec);
            final String[] headerArray = read.getColumnHeaders()//
                .map(val -> extractColumnHeaders(val, config))//
                .orElse(null);
            CheckUtils.checkArgument(headerArray != null || !config.useColumnHeaderIdx(),
                "The row containing the table headers (row number %s) was not part of the table.",
                config.getColumnHeaderIdx());
            return createTableSpec(typeGuesser, headerArray);
        }
    }

    @SuppressWarnings("resource") // the caller uses a try-with scope to ensure that the read is closed
    private ExtractColumnHeaderRead<V> wrap(final Read<V> read, final TableReadConfig<?> config) {
        final Read<V> filtered = ReadUtils.decorateForSpecGuessing(read, config);
        return new DefaultExtractColumnHeaderRead<>(filtered, config);
    }

    private Read<V> filterColIdx(final ExtractColumnHeaderRead<V> read, final TableReadConfig<?> config) {
        if (config.useRowIDIdx()) {
            return new ColumnFilterRead<>(read, config.getRowIDIdx());
        }
        return read;
    }

    private String[] extractColumnHeaders(final RandomAccessible<V> values, final TableReadConfig<?> config) {
        return convertToStringArray(filterColIdx(values, config));
    }

    private RandomAccessible<V> filterColIdx(final RandomAccessible<V> values, final TableReadConfig<?> config) {
        if (config.useRowIDIdx()) {
            final ColumnFilterRandomAccessible<V> colHeader = new ColumnFilterRandomAccessible<>(config.getRowIDIdx());
            colHeader.setDecoratee(values);
            return colHeader;
        }
        return values;
    }

    private String[] convertToStringArray(final RandomAccessible<V> randomAccessible) {
        return randomAccessible.stream()//
            .map(val -> val != null //
                ? m_valueToString.apply(val) //
                : null)//
            .toArray(String[]::new);
    }

    private TypedReaderTableSpec<T> createTableSpec(final TypeGuesser<T, V> typeGuesser, final String[] columnNames) {
        if (columnNames != null) {
            String[] headerArray = uniquify(columnNames);
            // make sure that we have at least as many types as names
            final List<T> types = typeGuesser.getMostSpecificTypes(headerArray.length);
            final List<Boolean> hasTypes = typeGuesser.getHasTypes(headerArray.length);
            if (types.size() != headerArray.length) {
                // make sure that we have the same number of names as types
                headerArray = Arrays.copyOf(headerArray, types.size());
            }
            return TypedReaderTableSpec.create(Arrays.asList(headerArray), types, hasTypes);
        } else {
            return TypedReaderTableSpec.create(typeGuesser.getMostSpecificTypes(0), typeGuesser.getHasTypes(0));
        }
    }

    private static String[] uniquify(final String[] columnNames) {
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(Collections.emptySet());
        return Arrays.stream(columnNames)//
            .map(n -> n == null ? null : nameGen.newName(n))//
            .toArray(String[]::new);
    }

    private TypeGuesser<T, V> guessTypes(final Read<V> source, final boolean allowShortRows,
        final ExecutionMonitor exec) throws IOException {
        final TypeGuesser<T, V> typeGuesser = new TypeGuesser<>(m_typeHierarchy, !allowShortRows);
        final PreviewExecutionMonitor previewExec;
        final OptionalLong estimatedSizeInBytes = source.getEstimatedSizeInBytes();
        final long estimatedSize;
        if (estimatedSizeInBytes.isPresent()) {
            estimatedSize = estimatedSizeInBytes.getAsLong();
        } else {
            estimatedSize = -1L;
        }
        previewExec = getPreviewExecutionMonitor(source, exec, estimatedSizeInBytes);
        RandomAccessible<V> row;
        long rowCount = 0;
        try {
            while (!typeGuesser.canStop() && (row = source.next()) != null) {
                exec.checkCanceled();
                typeGuesser.update(row);
                rowCount++;
                final double progress =
                    estimatedSizeInBytes.isPresent() ? ((double)source.readBytes() / estimatedSize) : 0;
                setProgress(exec, previewExec, rowCount, progress);

            }
        } catch (CanceledExecutionException es) {
            // do nothing, just stop
        } catch (Exception e) {
            if (previewExec != null) {
                previewExec.setSpecGuessingError(rowCount, e.getMessage());
            } else {
                throw e;
            }
        }
        return typeGuesser;
    }

    private static void setProgress(final ExecutionMonitor exec, final PreviewExecutionMonitor previewExec,
        final long rowCount, final double progress) {
        if (rowCount == 1 || rowCount % PROGRESS_UPDATE_INTERVAL == 0) {
            if (previewExec != null) {
                previewExec.setProgress(progress, rowCount);
            } else {
                final long finalRowCount = rowCount; // variable needs to be effective final
                exec.setProgress(progress, () -> "Analyzed " + finalRowCount + " rows");
            }
        }
    }

    private PreviewExecutionMonitor getPreviewExecutionMonitor(final Read<V> source, final ExecutionMonitor exec,
        final OptionalLong estimatedSizeInBytes) {
        final PreviewExecutionMonitor previewExec;
        if (exec instanceof PreviewExecutionMonitor) {
            previewExec = (PreviewExecutionMonitor)exec;
            previewExec.setSizeAssessable(estimatedSizeInBytes.isPresent());
            previewExec.setCurrentPath(source.getPath());
            previewExec.incrementCurrentlyReadingPathIdx();
        } else {
            previewExec = null;
        }
        return previewExec;
    }

}
