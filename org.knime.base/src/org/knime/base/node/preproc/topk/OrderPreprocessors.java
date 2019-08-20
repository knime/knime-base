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
 *   Aug 20, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.topk;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.UniqueNameGenerator;

/**
 * Static factory class for OrderPreprocessor implementations.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class OrderPreprocessors {

    private OrderPreprocessors() {
        // static factory class
    }

    static OrderPreprocessor noOp() {
        return new OrderPreprocessor() {

            @Override
            public double getProgressRequired() {
                return 0;
            }

            @Override
            public BufferedDataTable preprocessSelectionTable(final BufferedDataTable table,
                final ExecutionContext exec) {
                return table;
            }

        };
    }

    static OrderPreprocessor appendInOrderColumn() {
        return new OrderColumnAppender();
    }

    private static final class OrderColumnAppender implements OrderPreprocessor {
        @Override
        public double getProgressRequired() {
            return 0.1;
        }

        @Override
        public BufferedDataTable preprocessSelectionTable(final BufferedDataTable table, final ExecutionContext exec)
            throws CanceledExecutionException {
            final DataTableSpec spec = table.getDataTableSpec();
            UniqueNameGenerator nameGen = new UniqueNameGenerator(spec);
            final DataColumnSpec orderColumn = nameGen.newColumn("order", LongCell.TYPE);
            ColumnRearranger cr = new ColumnRearranger(spec);
            cr.append(new SingleCellFactory(orderColumn) {
                long m_idx = -1;

                @Override
                public DataCell getCell(final DataRow row) {
                    m_idx++;
                    return new LongCell(m_idx);
                }
            });
            return exec.createColumnRearrangeTable(table, cr, exec);
        }
    }
}
