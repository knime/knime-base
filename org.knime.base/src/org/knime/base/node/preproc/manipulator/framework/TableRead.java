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
 *   Nov 14, 2020 (Tobias): created
 */
package org.knime.base.node.preproc.manipulator.framework;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.base.node.preproc.manipulator.table.Table;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.v2.RowRead;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.AbstractRandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;

import com.google.common.collect.Tables;

/**
 * {@link Read} implementation that works with {@link Tables}.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class TableRead implements Read<Table, DataValue> {

    static class RandomAccessibleDataRow extends AbstractRandomAccessible<DataValue> {


        private final RowRead m_row;
        private TableReadConfig<TableManipulatorConfig> m_config;

        RandomAccessibleDataRow(final RowRead rowRead, final TableReadConfig<TableManipulatorConfig> config) {
            m_row = rowRead;
            m_config = config;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int size() {
            if (m_config.useRowIDIdx()) {
                return m_row.getNumColumns() + 1;
            }
            return m_row.getNumColumns();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValue get(final int idx) {
            final int offsetIdx;
            if (m_config.useRowIDIdx()) {
                if (idx == 0) {
                    return new StringCell(m_row.getRowKey().getString());
                }
                offsetIdx = idx - 1;
            } else {
                offsetIdx = idx;
            }
            return m_row.isMissing(offsetIdx) ? null : m_row.getValue(offsetIdx);
        }

    }
    private Table m_input;
    private TableReadConfig<TableManipulatorConfig> m_config;
    private RowCursor m_rowCursor;

    TableRead(final Table input, final TableReadConfig<TableManipulatorConfig> config) {
        m_input = input;
        m_rowCursor = input.cursor();
        m_config = config;
    }

    @Override
    public RandomAccessible<DataValue> next() throws IOException {
        if (!m_rowCursor.canForward()) {
            return null;
        }
        return new RandomAccessibleDataRow(m_rowCursor.forward(), m_config);
    }

    @Override
    public OptionalLong getMaxProgress() {
        return OptionalLong.empty();
    }

    @Override
    public long getProgress() {
        return 0;
    }

    @Override
    public Optional<Table> getItem() {
        return Optional.of(m_input);
    }

    @Override
    public void close() throws IOException {
        //nothing to close;
    }
}
