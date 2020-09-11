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

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * The model of a selector. </br>
 * Specifies type mapping, column filtering and reordering information.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify external types
 */
public interface TransformationModel<T> {

    /**
     * Retrieves the currently configured {@link ProductionPath} for the column {@link TypedReaderColumnSpec
     * columnSpec}.
     *
     * @param column one of the columns in the currently set rawSpec
     * @return the currently configured {@link ProductionPath} for {@link TypedReaderColumnSpec column}
     */
    ProductionPath getProductionPath(final TypedReaderColumnSpec<T> column);

    /**
     * Returns the name that should be used for {@link TypedReaderColumnSpec column}.
     *
     * @param column the name for which to fetch the possibly new name
     * @return the final output name for {@link TypedReaderColumnSpec column}
     */
    String getName(final TypedReaderColumnSpec<T> column);

    /**
     * Specifies whether the provided {@link TypedReaderColumnSpec} should be part of the output table.
     *
     * @param column the columnSpec in question
     * @return {@code true} if {@link TypedReaderColumnSpec column} should be kept in the output table
     */
    boolean keep(final TypedReaderColumnSpec<T> column);

    /**
     * Specifies the position of the provided {@link TypedReaderColumnSpec} in the output table.
     *
     * @param column the {@link TypedReaderColumnSpec} in question
     * @return the position of {@link TypedReaderColumnSpec column} in the output table
     */
    int getPosition(final TypedReaderColumnSpec<T> column);

    /**
     * Retrieves the {@link TypedReaderTableSpec} this {@link TransformationModel} operates on.
     *
     * @return the {@link TypedReaderTableSpec} underlying this {@link TransformationModel}
     */
    TypedReaderTableSpec<T> getRawSpec();

}
