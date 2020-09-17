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
 *   Sep 7, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.converter.variabletocell;

import static org.knime.core.data.collection.ListCell.getCollectionType;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;

/**
 * Abstract super class for all converters that convert simple {@link FlowVariable}s, i.e., flow variable that can be
 * transformed to cells without requiring additional objects.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
abstract class SimpleVarToCellConverter implements VariableToCellConverter {

    private final FlowVariable m_var;

    private final DataType m_cellType;

    SimpleVarToCellConverter(final FlowVariable var, final DataType cellType) {
        m_var = var;
        m_cellType = cellType;
    }

    @Override
    public DataColumnSpec createSpec(final String columnName) {
        return new DataColumnSpecCreator(columnName, m_cellType).createSpec();
    }

    final FlowVariable getVariable() {
        return m_var;
    }

    static class StringVariableConverter extends SimpleVarToCellConverter {
        StringVariableConverter(final FlowVariable var) {
            super(var, StringCell.TYPE);
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            // For reasons of backwards compatibility, String flow variables with a value of null shall be converted
            // to empty String cells
            return new StringCell(getVariable().getStringValue() == null ? "" : getVariable().getStringValue());
        }
    }

    static final class StringArrayVariableConverter extends SimpleVarToCellConverter {

        StringArrayVariableConverter(final FlowVariable var) {
            super(var, getCollectionType(StringCell.TYPE));
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return CollectionCellFactory.createListCell(//
                Arrays.stream(getVariable().getValue(StringArrayType.INSTANCE))//
                    .map(StringCell::new)//
                    .collect(Collectors.toList())//
            );
        }
    }

    static final class DoubleVariableConverter extends SimpleVarToCellConverter {

        DoubleVariableConverter(final FlowVariable var) {
            super(var, DoubleCell.TYPE);
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return new DoubleCell(getVariable().getDoubleValue());
        }
    }

    static final class DoubleArrayVariableConverter extends SimpleVarToCellConverter {

        DoubleArrayVariableConverter(final FlowVariable var) {
            super(var, getCollectionType(DoubleCell.TYPE));
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return CollectionCellFactory.createListCell(//
                Arrays.stream(getVariable().getValue(DoubleArrayType.INSTANCE))//
                    .map(DoubleCell::new)//
                    .collect(Collectors.toList())//
            );
        }
    }

    static final class IntVariableConverter extends SimpleVarToCellConverter {

        IntVariableConverter(final FlowVariable var) {
            super(var, IntCell.TYPE);
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return new IntCell(getVariable().getIntValue());
        }
    }

    static final class IntArrayVariableConverter extends SimpleVarToCellConverter {

        IntArrayVariableConverter(final FlowVariable var) {
            super(var, getCollectionType(IntCell.TYPE));
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return CollectionCellFactory.createListCell(//
                Arrays.stream(getVariable().getValue(IntArrayType.INSTANCE))//
                    .map(IntCell::new)//
                    .collect(Collectors.toList())//
            );
        }
    }

    static final class LongVariableConverter extends SimpleVarToCellConverter {

        LongVariableConverter(final FlowVariable var) {
            super(var, LongCell.TYPE);
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return new LongCell(getVariable().getValue(LongType.INSTANCE));
        }
    }

    static final class LongArrayVariableConverter extends SimpleVarToCellConverter {

        LongArrayVariableConverter(final FlowVariable var) {
            super(var, getCollectionType(LongCell.TYPE));
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return CollectionCellFactory.createListCell(//
                Arrays.stream(getVariable().getValue(LongArrayType.INSTANCE))//
                    .map(LongCell::new)//
                    .collect(Collectors.toList())//
            );
        }
    }

    static final class BooleanVariableConverter extends SimpleVarToCellConverter {

        BooleanVariableConverter(final FlowVariable var) {
            super(var, BooleanCell.TYPE);
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return BooleanCellFactory.create(getVariable().getValue(BooleanType.INSTANCE));
        }
    }

    static final class BooleanArrayVariableConverter extends SimpleVarToCellConverter {

        BooleanArrayVariableConverter(final FlowVariable var) {
            super(var, getCollectionType(BooleanCell.TYPE));
        }

        @Override
        public DataCell getDataCell(final ExecutionContext exec) {
            return CollectionCellFactory.createListCell(//
                Arrays.stream(getVariable().getValue(BooleanArrayType.INSTANCE))//
                    .map(BooleanCellFactory::create)//
                    .collect(Collectors.toList())//
            );
        }
    }

}
