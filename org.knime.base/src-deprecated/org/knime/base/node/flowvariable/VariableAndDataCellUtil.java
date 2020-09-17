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
 *   11 Nov 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.VariableAndDataCellPair.BooleanArrayValueToBooleanListCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.BooleanVariableToBooleanCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.DoubleArrayValueToDoubleListCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.DoubleVariableToDoubleCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.IntArrayValueToIntListCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.IntVariableToIntCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.LongArrayValueToLongListCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.LongVariableToLongCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.StringArrayValueToStringListCell;
import org.knime.base.node.flowvariable.VariableAndDataCellPair.StringVariableToStringCell;
import org.knime.base.node.flowvariable.converter.celltovariable.CellToVariableConverterFactory;
import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;

/**
 * A helper class for converting {@link FlowVariable FlowVariables} to {@link DataCell DataCells} and vice versa. It
 * should not be used by ordinary nodes that work with flow variables in some other way than converting them to/from
 * data cells.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @deprecated replaced by {@link CellToVariableConverterFactory} and {@link VariableToCellConverterFactory}
 */
@Deprecated
public final class VariableAndDataCellUtil {

    private VariableAndDataCellUtil() {
    }

    private static final Map<VariableType<?>, Function<FlowVariable, VariableAndDataCellPair>> TYPES =
        new LinkedHashMap<>();
    static {
        TYPES.put(StringType.INSTANCE, StringVariableToStringCell::new);
        TYPES.put(StringArrayType.INSTANCE, StringArrayValueToStringListCell::new);
        TYPES.put(DoubleType.INSTANCE, DoubleVariableToDoubleCell::new);
        TYPES.put(DoubleArrayType.INSTANCE, DoubleArrayValueToDoubleListCell::new);
        TYPES.put(IntType.INSTANCE, IntVariableToIntCell::new);
        TYPES.put(IntArrayType.INSTANCE, IntArrayValueToIntListCell::new);
        TYPES.put(LongType.INSTANCE, LongVariableToLongCell::new);
        TYPES.put(LongArrayType.INSTANCE, LongArrayValueToLongListCell::new);
        TYPES.put(BooleanType.INSTANCE, BooleanVariableToBooleanCell::new);
        TYPES.put(BooleanArrayType.INSTANCE, BooleanArrayValueToBooleanListCell::new);
    }

    /**
     * Method that returns a list of {@link VariableType VariableTypes} that can be translated to/from a
     * {@link DataCell} by means of this utility class. Note that the list of supported types can change in between
     * releases, so do not rely on this method always returning the same result.
     *
     * @return a list of {@link VariableType VariableTypes} that can be translated to/from a {@link DataCell}
     */
    public static VariableType<?>[] getSupportedVariableTypes() {
        return TYPES.keySet().stream().toArray(VariableType<?>[]::new);
    }

    /**
     * @param type the type of a {@link DataCell} that is to be translated into a {@link FlowVariable}
     * @return true if and only if any {@link DataCell} of the given type can be translated into a {@link FlowVariable}
     */
    public static boolean isTypeCompatible(final DataType type) {
        CheckUtils.checkArgumentNotNull(type, "Type must not be null.");
        if (type.isCollectionType()) {
            final DataType elementType = type.getCollectionElementType();
            return elementType.isCompatible(BooleanValue.class) //
                || elementType.isCompatible(IntValue.class) //
                || elementType.isCompatible(LongValue.class) //
                || elementType.isCompatible(DoubleValue.class) //
                || elementType.isCompatible(StringValue.class);
        }
        return type.isCompatible(BooleanValue.class) //
            || type.isCompatible(IntValue.class) //
            || type.isCompatible(LongValue.class) //
            || type.isCompatible(DoubleValue.class) //
            || type.isCompatible(StringValue.class);
    }

    /**
     * A method that translates a {@link DataCell} into a {@link FlowVariable} and pushes it onto the stack.
     *
     * @param type the type of the to-be-translated {@link DataCell}
     * @param cell the cell that is to be translated into a {@link FlowVariable} and pushed onto the stack
     * @param push a reference to the {@link NodeModel NodeModel's} method to push a {@link FlowVariable} of any
     *            {@link VariableType} onto the stack
     */
    @SuppressWarnings("unchecked")
    public static <T> void pushVariable(final DataType type, final DataCell cell,
        final BiConsumer<VariableType<T>, T> push) {
        CheckUtils.checkArgumentNotNull(type, "Type must not be null.");
        CheckUtils.checkArgumentNotNull(cell, "Data cell must not be null.");
        CheckUtils.checkArgumentNotNull(push, "Method reference must not be null.");
        if (type.isCollectionType()) {
            final DataType elementType = type.getCollectionElementType();
            if (elementType.isCompatible(BooleanValue.class)) {
                push.accept((VariableType<T>)BooleanArrayType.INSTANCE, (T)((CollectionDataValue)cell).stream()
                    .map(c -> Boolean.valueOf(((BooleanValue)c).getBooleanValue())).toArray(Boolean[]::new));
            } else if (elementType.isCompatible(IntValue.class)) {
                push.accept((VariableType<T>)IntArrayType.INSTANCE, (T)((CollectionDataValue)cell).stream()
                    .mapToInt(c -> ((IntValue)c).getIntValue()).boxed().toArray(Integer[]::new));
            } else if (elementType.isCompatible(LongValue.class)) {
                push.accept((VariableType<T>)LongArrayType.INSTANCE, (T)((CollectionDataValue)cell).stream()
                    .mapToLong(c -> ((LongValue)c).getLongValue()).boxed().toArray(Long[]::new));
            } else if (elementType.isCompatible(DoubleValue.class)) {
                push.accept((VariableType<T>)DoubleArrayType.INSTANCE, (T)((CollectionDataValue)cell).stream()
                    .mapToDouble(c -> ((DoubleValue)c).getDoubleValue()).boxed().toArray(Double[]::new));
            } else if (elementType.isCompatible(StringValue.class)) {
                push.accept((VariableType<T>)StringArrayType.INSTANCE, (T)((CollectionDataValue)cell).stream()
                    .map(c -> ((StringValue)c).getStringValue()).toArray(String[]::new));
            }
        } else if (type.isCompatible(BooleanValue.class)) {
            push.accept((VariableType<T>)BooleanType.INSTANCE, (T)new Boolean(((BooleanValue)cell).getBooleanValue()));
        } else if (type.isCompatible(IntValue.class)) {
            push.accept((VariableType<T>)IntType.INSTANCE, (T)new Integer(((IntValue)cell).getIntValue()));
        } else if (type.isCompatible(LongValue.class)) {
            push.accept((VariableType<T>)LongType.INSTANCE, (T)new Long(((LongValue)cell).getLongValue()));
        } else if (type.isCompatible(DoubleValue.class)) {
            push.accept((VariableType<T>)DoubleType.INSTANCE, (T)new Double(((DoubleValue)cell).getDoubleValue()));
        } else if (type.isCompatible(StringValue.class)) {
            push.accept((VariableType<T>)StringType.INSTANCE, (T)((StringValue)cell).getStringValue());
        }
    }

    /**
     * A method that converts {@link FlowVariable FlowVariables} supported by this class (see
     * {@link #getSupportedVariableTypes()}) from the stack and converts them into {@link DataCell DataCells}.
     *
     * @param vars a map of {@link FlowVariable FlowVariables}, identified by their name
     * @return a list of {@link FlowVariable FlowVariables} {@link DataCell DataCells}
     */
    public static List<VariableAndDataCellPair> getVariablesAsDataCells(final Map<String, FlowVariable> vars) {
        CheckUtils.checkArgumentNotNull(vars, "Flow variable map must not be null.");
        return vars.values().stream().filter(v -> v != null).filter(v -> TYPES.get(v.getVariableType()) != null)
            .map(v -> TYPES.get(v.getVariableType()).apply(v)).collect(Collectors.toList());
    }

}
