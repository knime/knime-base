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
 *   Jul 31, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import java.util.stream.Stream;

import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * The model of a selector. </br>
 * Specifies type mapping, column filtering and reordering information.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify external types
 */
public interface TableTransformation<T> {

    /**
     * Indicates whether there exists a {@link ColumnTransformation} for the provided {@link TypedReaderColumnSpec}.
     *
     * @param column in question
     * @return {@code true} if there is a {@link ColumnTransformation} available for {@link TypedReaderColumnSpec
     *         column}
     */
    boolean hasTransformationFor(final TypedReaderColumnSpec<T> column);

    /**
     * Retrieves the {@link ColumnTransformation} for the provided {@link TypedReaderColumnSpec}.
     *
     * @param column for which to retrieve the {@link ColumnTransformation}
     * @return the {@link ColumnTransformation} for {@link TypedReaderColumnSpec column}
     */
    ColumnTransformation<T> getTransformation(final TypedReaderColumnSpec<T> column);

    /**
     * Creates a {@link Stream} of the contained {@link ColumnTransformation Transformations}.</br>
     * The stream does not provide any guarantees on the order of the transformations it contains.
     *
     * @return a {@link Stream} of the contained {@link ColumnTransformation Transformations}
     */
    Stream<ColumnTransformation<T>> stream();

    /**
     * Returns the number of {@link ColumnTransformation Transformations} stored in this model.</br>
     * Must be equal to the number of columns in the union of {@link #getRawSpec()}.
     *
     * @return the number of {@link ColumnTransformation Transformations} stored in this model
     */
    default int size() {
        return getRawSpec().getUnion().size();
    }

    /**
     * Returns the position at which new unknown columns should be inserted.
     *
     * @return the position where new unknown columns should be inserted
     */
    int getPositionForUnknownColumns();

    /**
     * Indicates whether unknown columns should be kept or ignored.
     *
     * @return {@code true} if unknown columns should be part of the output
     */
    boolean keepUnknownColumns();

    /**
     * Specifies the execution behavior in the case that a column with a known name is encountered but the external type
     * differs from the configured one.
     * If set to {@code true}, it is attempted to find an alternative production path to the configured KNIME type and if no such
     * path exists, the node fails with a corresponding exception.
     * If set to {@code false}, the default production path for the new external type is used.
     *
     * @return {@code true} if types should be enforced
     */
    boolean enforceTypes();

    /**
     * The {@link ColumnFilterMode} indicates which columns should be taken into account for the output.
     *
     * @return the {@link ColumnFilterMode}
     */
    ColumnFilterMode getColumnFilterMode();

    /**
     * Retrieves the {@link TypedReaderTableSpec} this {@link TableTransformation} operates on.
     *
     * @return the {@link TypedReaderTableSpec} underlying this {@link TableTransformation}
     */
    RawSpec<T> getRawSpec();

}
