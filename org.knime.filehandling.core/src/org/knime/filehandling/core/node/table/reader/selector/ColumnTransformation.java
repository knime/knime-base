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
 *   Oct 8, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * A Transformation represents changes to a particular column, such as its name, its {@link ProductionPath}, whether it
 * is part of the output and its position in the output.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The type used to identify external data types
 */
public interface ColumnTransformation<T> extends Comparable<ColumnTransformation<T>> {

    /**
     * Returns the name the column should have in the output.
     *
     * @return the output name
     */
    String getName();

    /**
     * Convenience method to access the name of the {@link #getExternalSpec() external spec}.
     *
     * @return the name of the external spec
     */
    default String getOriginalName() {
        return MultiTableUtils.getNameAfterInit(getExternalSpec());
    }

    /**
     * Returns the {@link ProductionPath} for mapping the column to a KNIME type.
     *
     * @return the {@link ProductionPath}
     */
    ProductionPath getProductionPath();

    /**
     * Indicates whether the column should be part of the output or not.
     *
     * @return {@code true} if the column should be kept, otherwise {@code false}
     */
    boolean keep();

    /**
     * The position of the column in the output.</br>
     * Note that it has to be assumed that this position is with respect to all columns even those that are filtered
     * out. Hence the final output position has to be determined by taking filtered out columns into account.
     *
     * @return the position in the unfiltered output table
     */
    int getPosition();

    @Override
    default int compareTo(final ColumnTransformation<T> o) {
        return Integer.compare(getPosition(), o.getPosition());
    }

    /**
     * Returns the {@link TypedReaderColumnSpec} this instance refers to.
     *
     * @return the {@link TypedReaderColumnSpec} this instance refers to
     */
    TypedReaderColumnSpec<T> getExternalSpec();

}
