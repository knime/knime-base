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
 *   8 Oct 2025 (Generated): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;
import org.knime.node.parameters.Widget;

/**
 * Parameters for Double data type filter operators.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class DoubleParameters implements SingleCellValueParameters<DoubleCell> {

    @Widget(title = FILTER_VALUE_TITLE, description = FILTER_VALUE_DESCRIPTION)
    double m_value;

    @Override
    public DoubleCell createCell() {
        return new DoubleCell(m_value);
    }

    @Override
    public DataType getSpecificType() {
        return DoubleCellFactory.TYPE;
    }

    @Override
    public void loadFrom(final DoubleCell cellFromStash) {
        m_value = cellFromStash.getDoubleValue();
    }

    @Override
    @SuppressWarnings("java:S1244")
        // we specifically want to test for doubles that can be interpreted as integers without losing precision
    public DataValue[] stash() {
        // avoid stashing as double if it is an integer value, because that can be unstashed as Int or Long easily
        if (Math.rint(m_value) == m_value) {
            return new DataValue[]{new IntCell((int)m_value)};
        }
        return new DataValue[]{new DoubleCell(m_value)};
    }

    @Override
    public void applyStash(final DataValue[] stashedValues) {
        if (stashedValues.length == 0) {
            return;
        }
        final var first = stashedValues[0];
        if (first instanceof DoubleCell doubleCell) {
            loadFrom(doubleCell);
        } else if (first instanceof IntCell intCell) {
            m_value = intCell.getIntValue();
        } else if (first instanceof LongCell longCell) {
            m_value = longCell.getLongValue();
        } else if (first instanceof StringValue stringValue) {
            try {
                m_value = Double.parseDouble(stringValue.getStringValue());
            } catch (final NumberFormatException e) {// NOSONAR
                // ignore stash
            }
        }
    }
}
