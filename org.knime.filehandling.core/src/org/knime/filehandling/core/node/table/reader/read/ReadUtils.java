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

/**
 * Static utility class for modifying {@link Read} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
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
        return new IntervalRead<>(read, skip, 0);
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

}
