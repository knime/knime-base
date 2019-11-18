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
 *   Jun 11, 2019 (Perla Gjoka): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.function.Predicate;

import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Range;

/**
 * Default implementation of {@link RowPredicateFactory}.<br/>
 * Creates row predicates for ordinary columns as well as the RowID and the RowIndex.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultRowPredicateFactory implements RowPredicateFactory {

    private final Predicate<DataCell> m_cellPredicate;

    private final ColumnSpec m_columnSpec;

    private final Range<Long> m_indexRange;

    /**
     * @param cellPredicate is the predicate of the cell which holds the parameter given by the user.
     * @param columnSpec keeps the ColumnSpec of the column chosen by the user.
     * @param indexRange holds the needed range of rows to test.
     *
     */
    public DefaultRowPredicateFactory(final Predicate<DataCell> cellPredicate, final ColumnSpec columnSpec,
        final Range<Long> indexRange) {
        m_cellPredicate = cellPredicate;
        m_columnSpec = columnSpec;
        m_indexRange = indexRange;
    }

    public DefaultRowPredicateFactory(final Predicate<DataCell> cellPredicate, final ColumnSpec columnSpec) {
        this(cellPredicate, columnSpec, Range.all());
    }

    @Override
    public RowPredicate createPredicate(final DataTableSpec tableSpec) throws InvalidSettingsException {
        switch (m_columnSpec.getRole()) {
            case ORDINARY:
                return createOrdinaryPredicate(tableSpec);
            case ROW_ID:
                return createRowIdPredicate(tableSpec);
            case ROW_INDEX:
                return createRowIndexPredicate(tableSpec);
            default:
                throw new InvalidSettingsException(
                    String.format("Unknown column role '%s' encountered.", m_columnSpec.getRole()));
        }
    }

    private RowPredicate createRowIndexPredicate(final DataTableSpec tableSpec) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_columnSpec.getName().equals(ColumnRole.ROW_INDEX.createUniqueName(tableSpec)),
            "The configured column %s is not part of the table spec %s. Please reconfigure.", m_columnSpec.getName(),
            tableSpec);
        return new RowIndexPredicate(m_cellPredicate, m_indexRange);
    }

    private RowPredicate createRowIdPredicate(final DataTableSpec tableSpec) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_columnSpec.getName().equals(ColumnRole.ROW_ID.createUniqueName(tableSpec)),
            "The configured column %s is not part of the table spec %s. Please reconfigure.", m_columnSpec.getName(),
            tableSpec);
        return new RowKeyPredicate(m_cellPredicate);
    }

    private RowPredicate createOrdinaryPredicate(final DataTableSpec tableSpec) throws InvalidSettingsException {
        final int columnIdx = tableSpec.findColumnIndex(m_columnSpec.getName());
        CheckUtils.checkSetting(columnIdx >= 0, "The column '%s' is not contained in the input table.", m_columnSpec.getName());
        final DataColumnSpec columnSpec = tableSpec.getColumnSpec(columnIdx);
        CheckUtils.checkSetting(m_columnSpec.getType().isASuperTypeOf(columnSpec.getType()),
            "Expected column %s to be of type %s but it had type %s instead. Please reconfigure.",
            m_columnSpec.getName(), m_columnSpec.getType(), columnSpec.getType());
        return new ColumnRowPredicate(m_cellPredicate, columnIdx);
    }

}
