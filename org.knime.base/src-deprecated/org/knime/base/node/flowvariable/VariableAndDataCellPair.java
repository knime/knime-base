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
 *   28 Oct 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable;

import static org.knime.core.data.collection.CollectionCellFactory.createListCell;
import static org.knime.core.data.collection.ListCell.getCollectionType;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;

/**
 * A class that wraps a {@link FlowVariable} and a corresponding {@link DataCell}.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@Deprecated
public abstract class VariableAndDataCellPair {

    private final FlowVariable m_var;

    private final DataCell m_cell;

    private final DataType m_cellType;

    private VariableAndDataCellPair(final FlowVariable var, final DataCell cell, final DataType type) {
        m_var = var;
        m_cell = cell;
        m_cellType = type;
    }

    /**
     * @return the name of the {@link FlowVariable} to convert from
     */
    public final String getName() {
        return m_var.getName();
    }

    /**
     * @return the {@link FlowVariable} to convert from
     */
    public FlowVariable getFlowVariable() {
        return m_var;
    }

    /**
     * @return the {@link DataCell} to convert to
     */
    public final DataCell getDataCell() {
        return m_cell;
    }

    /**
     * @return the type of the {@link DataCell} to convert to
     */
    public final DataType getCellType() {
        return m_cellType;
    }

    static final class StringVariableToStringCell extends VariableAndDataCellPair {
        StringVariableToStringCell(final FlowVariable var) {
            // For reasons of backwards compatibility, String flow variables with a value of null shall be converted
            // to empty String cells
            super(var, new StringCell(var.getStringValue() == null ? "" : var.getStringValue()), StringCell.TYPE);
        }
    }

    static final class StringArrayValueToStringListCell extends VariableAndDataCellPair {
        StringArrayValueToStringListCell(final FlowVariable var) {
            super(var, createListCell(Arrays.stream(var.getValue(StringArrayType.INSTANCE)).map(s -> new StringCell(s))
                .collect(Collectors.toList())), getCollectionType(StringCell.TYPE));
        }
    }

    static final class DoubleVariableToDoubleCell extends VariableAndDataCellPair {
        DoubleVariableToDoubleCell(final FlowVariable var) {
            super(var, new DoubleCell(var.getDoubleValue()), DoubleCell.TYPE);
        }
    }

    static final class DoubleArrayValueToDoubleListCell extends VariableAndDataCellPair {
        DoubleArrayValueToDoubleListCell(final FlowVariable var) {
            super(var, createListCell(Arrays.stream(var.getValue(DoubleArrayType.INSTANCE)).map(d -> new DoubleCell(d))
                .collect(Collectors.toList())), getCollectionType(DoubleCell.TYPE));
        }
    }

    static final class IntVariableToIntCell extends VariableAndDataCellPair {
        IntVariableToIntCell(final FlowVariable var) {
            super(var, new IntCell(var.getIntValue()), IntCell.TYPE);
        }
    }

    static final class IntArrayValueToIntListCell extends VariableAndDataCellPair {
        IntArrayValueToIntListCell(final FlowVariable var) {
            super(var, createListCell(Arrays.stream(var.getValue(IntArrayType.INSTANCE)).map(i -> new IntCell(i))
                .collect(Collectors.toList())), getCollectionType(IntCell.TYPE));
        }
    }

    static final class LongVariableToLongCell extends VariableAndDataCellPair {
        LongVariableToLongCell(final FlowVariable var) {
            super(var, new LongCell(var.getValue(LongType.INSTANCE)), LongCell.TYPE);
        }
    }

    static final class LongArrayValueToLongListCell extends VariableAndDataCellPair {
        LongArrayValueToLongListCell(final FlowVariable var) {
            super(var, createListCell(Arrays.stream(var.getValue(LongArrayType.INSTANCE)).map(l -> new LongCell(l))
                .collect(Collectors.toList())), getCollectionType(LongCell.TYPE));
        }
    }

    static final class BooleanVariableToBooleanCell extends VariableAndDataCellPair {
        BooleanVariableToBooleanCell(final FlowVariable var) {
            super(var, BooleanCellFactory.create(var.getValue(BooleanType.INSTANCE)), BooleanCell.TYPE);
        }
    }

    static final class BooleanArrayValueToBooleanListCell extends VariableAndDataCellPair {
        BooleanArrayValueToBooleanListCell(final FlowVariable var) {
            super(var,
                createListCell(Arrays.stream(var.getValue(BooleanArrayType.INSTANCE))
                    .map(b -> BooleanCellFactory.create(b)).collect(Collectors.toList())),
                getCollectionType(BooleanCell.TYPE));
        }
    }

}
