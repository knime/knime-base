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
package org.knime.filehandling.core.node.table.reader.read;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 * Represents a single read of a table stored on disk. The API is similar to {@link Supplier Suppliers} with the
 * distinction that a read is {@link AutoCloseable} and {@link Read#next()} can throw {@link IOException IOExceptions}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of tokens making up a row in the read
 */
public interface Read<V> extends AutoCloseable {

    /**
     * Returns the next row or null if the end is reached.</br>
     *
     * <b>NOTE:</b> The returned object may be a proxy i.e. the same object may be returned whose state is altered.
     * Therefore <code>read.next().equals(read.next())</code> may return true even though the actual values changed.
     *
     * @return the next row or null if the end is reached
     * @throws IOException if an I/O related problem is encountered
     */
    RandomAccessible<V> next() throws IOException;

    /**
     * Returns the estimated number of bytes stored in the file that this {@code Read} reads from.
     *
     * @return the estimated number of bytes this Read is going to read or {@link OptionalLong#empty()} if the size
     *         can't be estimated
     */
    OptionalLong getEstimatedSizeInBytes();

    /**
     * Returns the number of bytes this Read already read.
     *
     * @return the number of bytes this Read already read
     */
    long readBytes();

    /**
     * Returns the path of the underlying source.
     *
     * @return the path of the underlying source
     */
    Optional<Path> getPath();

    @Override
    void close() throws IOException;

}
