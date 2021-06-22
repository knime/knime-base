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
 *   Feb 13, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.read;

import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * Static utility class for modifying {@link Read} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public final class ReadUtils {

    private ReadUtils() {
        // static utility class
    }

    /**
     * Limits the number of rows readable from {@link Read read}.
     *
     * @param read to limit
     * @param limit the number of rows to read
     * @return a {@link Read} that is limited to <b>limit</b> rows
     */
    public static <V> Read<V> limit(final Read<V> read, final long limit) {
        return new IntervalRead<>(read, 0, limit);
    }

    /**
     * Skips the first <b>skip</b> rows of the provided {@link Read read}.
     *
     * @param read of which the first rows should be skipped
     * @param skip the number of rows to skip
     * @return a read that skips over the first <b>skip</b> rows
     */
    public static <V> Read<V> skip(final Read<V> read, final long skip) {
        return new IntervalRead<>(read, skip, Long.MAX_VALUE);
    }

    /**
     * Returns a {@link Read} that only returns the rows from the first index <b>from</b> to the last index <b>to</b>.
     *
     * @param read the input read
     * @param from the first index to read
     * @param to the last index to read
     * @return a read that returns only the rows of <b>read</b> starting from <b>from</b> upto <b>to</b>
     */
    public static <V> Read<V> range(final Read<V> read, final long from, final long to) {
        return new IntervalRead<>(read, from, to);
    }

    /**
     * Returns a {@link Read} that skips empty rows, i.e. rows where {@link RandomAccessible#size()} is 0.
     *
     * @param read the read to decorate
     * @return a {@link Read} that skips empty rows in {@link Read read}
     */
    public static <V> Read<V> skipEmptyRows(final Read<V> read) {
        return new SkipEmptyRead<>(read);
    }

    /**
     * Decorates the provided {@link Read} for reading actual the spec according to the provided {@link TableReadConfig}.</br>
     * Following decorators are added if necessary:</br>
     * - Skipping empty rows.</br>
     * - Validate that all rows have the same size if short rows aren't allowed.</br>
     *
     * @param read to decorated
     * @param config for reading
     * @return a decorated {@link Read} conforming to {@link TableReadConfig config}
     */
    @SuppressWarnings("resource") // that's the point
    public static <V> Read<V> decorateForSpecGuessing(final Read<V> read, final TableReadConfig<?> config) {
        Read<V> decorated = read;
        decorated = decorateSkipEmpty(decorated, config);
        decorated = decorateAllowShortRows(decorated, config);
        return decorated;
    }

    /**
     * Decorates the provided {@link Read} for reading actual rows according to the provided {@link TableReadConfig}.</br>
     * Following decorators are added if necessary:</br>
     * - Skipping empty rows.</br>
     * - Validate that all rows have the same size if short rows aren't allowed.</br>
     * - Skip the column header if it lies within the range of read rows.</br>
     *
     * @param read to decorated
     * @param config for reading
     * @return a decorated {@link Read} conforming to {@link TableReadConfig config}
     */
    @SuppressWarnings("resource") // that's the point...
    public static <V> Read<V> decorateForReading(final Read<V> read, final TableReadConfig<?> config) {
        Read<V> decorated = read;
        decorated = decorateSkipEmpty(decorated, config);
        decorated = decorateAllowShortRows(decorated, config);
        decorated = decorateSkipHeader(decorated, config);
        return decorated;
    }

    /**
     * Decorates the provided {@link Read} for reading actual rows according to the provided {@link TableReadConfig}.</br>
     * Following decorators are added if necessary:</br>
     * - Skipping empty rows.</br>
     * - Validate that all rows have the same size if short rows aren't allowed.</br>
     * - Skip the column header if it lies within the range of read rows.</br>
     *
     * @param read to decorated
     * @param config for reading
     * @return a decorated {@link Read} conforming to {@link TableReadConfig config}
     */
    @SuppressWarnings("resource") // that's the point...
    public static <V> Read<V> decorateForReading(final TableReadConfig<?> config,
        final Read<V> read) {
        Read<V> decorated = read;
        decorated = decorateSkipEmpty(decorated, config);
        decorated = decorateAllowShortRows(decorated, config);
        decorated = decorateSkipHeader(decorated, config);
        return decorated;
    }

    private static <V> Read<V> decorateSkipEmpty(final Read<V> read, final TableReadConfig<?> config) {
        if (config.skipEmptyRows()) {
            return new SkipEmptyRead<>(read);
        } else {
            return read;
        }
    }

    private static <V> Read<V> decorateAllowShortRows(final Read<V> read, final TableReadConfig<?> config) {
        if (!config.allowShortRows()) {
            return new CheckSameSizeRead<>(read);
        } else {
            return read;
        }
    }

    /**
     * Figures out if the column header needs to be skipped and decorates the read if necessary.
     *
     * @param read {@link Read} in which the column header potentially needs to be skipped
     * @param config {@link TableReadConfig} from which to figure out if the column read needs to be skipped
     * @return a {@link Read} without a column header (one way or the other)
     */
    private static <V> Read<V> decorateSkipHeader(final Read<V> read, final TableReadConfig<?> config) {
        if (needToSkipColumnHeader(config)) {
            // we only end up here if there is a column header index and it is in [readStartIdx, readEndIdx)
            final long offset = config.getColumnHeaderIdx() - getStartIdx(config);
            return new SkipIdxRead<>(read, offset);
        } else {
            return read;
        }
    }

    private static boolean needToSkipColumnHeader(final TableReadConfig<?> config) {
        if (!config.useColumnHeaderIdx()) {
            return false;
        }
        final long readStartIdx = getStartIdx(config);
        final long columnHeaderIdx = config.getColumnHeaderIdx();
        if (config.limitRows()) {
            return columnHeaderIdx >= readStartIdx && columnHeaderIdx < readStartIdx + config.getMaxRows();
        } else {
            return columnHeaderIdx >= readStartIdx;
        }
    }

    private static long getStartIdx(final TableReadConfig<?> config) {
        return config.skipRows() ? config.getNumRowsToSkip() : 0;
    }

}
