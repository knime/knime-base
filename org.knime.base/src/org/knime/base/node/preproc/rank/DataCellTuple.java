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
 *   06.10.2015 (Adrian Nembach): created
 */
package org.knime.base.node.preproc.rank;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell.StringCellFactory;

/**
 * This class wraps an array of DataCells and provides equals and hashCode for the array
 *
 * @author Adrian Nembach, KNIME GmbH Konstanz
 */
final class DataCellTuple {
    private final DataCell[] m_elements;

    public DataCellTuple(final DataRow row, final int[] colIndices) {
        m_elements = IntStream.of(colIndices)//
            .mapToObj(i -> i == -1 ? StringCellFactory.create(row.getKey().getString()) : row.getCell(i))//
            .toArray(DataCell[]::new);
    }

    public DataCellTuple(final int[] colIndices) {
        m_elements = new DataCell[colIndices.length];
    }

    public int getLength() {
        return m_elements.length;
    }

    public DataCell getElement(final int index) {
        if (index >= m_elements.length) {
            throw new IllegalArgumentException("Index too large");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index must be positive");
        }

        return m_elements[index];
    }

    /*
     * Two DataCellTuples are equal if they are of the same size and contain exactly the same elements
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof DataCellTuple) {
            DataCellTuple instance = (DataCellTuple)obj;
            return Arrays.equals(m_elements, instance.m_elements);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(m_elements);
    }

}
