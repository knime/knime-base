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
package org.knime.base.node.flowvariable.converter.celltovariable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
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
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.cell.FSLocationCell;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;

/**
 * Factory class to create the {@link CellToVariableConverter} associated with the provided {@link DataType}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class CellToVariableConverterFactory {

    private static final Map<Class<? extends DataValue>, Supplier<CellToVariableConverter<?>>> SUPPORTED_CELL_TYPES;

    private static final Map<Class<? extends DataValue>, Supplier<CellToVariableConverter<?>>> SUPPORTED_COLLECTION_TYPES;

    static {
        SUPPORTED_CELL_TYPES = new LinkedHashMap<>();
        // Important: Don't change the order as this might change the created directory see DataType#isCompatible
        SUPPORTED_CELL_TYPES.put(BooleanValue.class, BooleanCellToVariableConverter::new);
        SUPPORTED_CELL_TYPES.put(IntValue.class, IntCellToVariableConverter::new);
        SUPPORTED_CELL_TYPES.put(LongValue.class, LongCellToVariableConverter::new);
        SUPPORTED_CELL_TYPES.put(DoubleValue.class, DoubleCellToVariableConverter::new);
        SUPPORTED_CELL_TYPES.put(FSLocationValue.class, FSLocationCellToVariableConverter::new);
        SUPPORTED_CELL_TYPES.put(StringValue.class, StringCellToVariableConverter::new);

        SUPPORTED_COLLECTION_TYPES = new LinkedHashMap<>();
        // Important: Don't change the order as this might change the created directory see DataType#isCompatible
        SUPPORTED_COLLECTION_TYPES.put(BooleanValue.class, BooleanArrayCellToVariableConverter::new);
        SUPPORTED_COLLECTION_TYPES.put(IntValue.class, IntArrayCellToVariableConverter::new);
        SUPPORTED_COLLECTION_TYPES.put(LongValue.class, LongArrayCellToVariableConverter::new);
        SUPPORTED_COLLECTION_TYPES.put(DoubleValue.class, DoubleArrayCellToVariableConverter::new);
        SUPPORTED_COLLECTION_TYPES.put(StringValue.class, StringArrayCellToVariableConverter::new);

    }

    private CellToVariableConverterFactory() {
        // factory class
    }

    /**
     * Creates the {@link CellToVariableConverter} associated with the given {@link DataType}.
     *
     * @param type the {@link DataType} of the column whose cells need to be converted
     * @return the converter associated with the given {@link DataType}
     */
    public static CellToVariableConverter<?> createConverter(final DataType type) { //NOSONAR
        final DataType eleType = getType(type);
        return getSupportedTypes(type).entrySet().stream() //
            .filter(e -> eleType.isCompatible(e.getKey())) //
            .map(Map.Entry::getValue) //
            .map(Supplier::get) //
            .findFirst() //
            .orElseThrow(() -> new IllegalArgumentException(String.format(
                "There is no Cell to Variable converter associated with the provided cell type '%s'", type.getName())));
    }

    /**
     * Returns {@code true} if there exists a converter for the given {@link DataType} and {@code false} otherwise.
     *
     * @param type the {@link DataType} of the column that needs to be converted
     * @return {@code true} if such a converter exists and {@code false} otherwise
     */
    public static boolean isSupported(final DataType type) {
        final DataType eleType = getType(type);
        return getSupportedTypes(type).keySet().stream() //
            .anyMatch(eleType::isCompatible);
    }

    /**
     * Returns the set of supported target {@link VariableType}s.
     *
     * @return the set of supported target variable types.
     */
    public static Set<VariableType<?>> getSupportedTargetVariableTypes() { // NOSONAR
        return Stream.concat(SUPPORTED_CELL_TYPES.values().stream(), SUPPORTED_COLLECTION_TYPES.values().stream())//
            .map(Supplier::get) //
            .map(CellToVariableConverter::getVariableType) //
            .collect(Collectors.toSet());
    }

    private static DataType getType(final DataType type) {
        return type.isCollectionType() ? type.getCollectionElementType() : type;
    }

    private static Map<Class<? extends DataValue>, Supplier<CellToVariableConverter<?>>>
        getSupportedTypes(final DataType type) {
        if (type.isCollectionType()) {
            return SUPPORTED_COLLECTION_TYPES;
        } else {
            return SUPPORTED_CELL_TYPES;
        }
    }

    private static class BooleanCellToVariableConverter implements CellToVariableConverter<BooleanType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((BooleanValue)cell).getBooleanValue());
        }

        @Override
        public BooleanType getVariableType() {
            return BooleanType.INSTANCE;
        }

    }

    private static class BooleanArrayCellToVariableConverter implements CellToVariableConverter<BooleanArrayType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, //
                getVariableType(), //
                ((CollectionDataValue)cell).stream() //
                    .map(c -> Boolean.valueOf(((BooleanValue)c).getBooleanValue())) //
                    .toArray(Boolean[]::new));
        }

        @Override
        public BooleanArrayType getVariableType() {
            return BooleanArrayType.INSTANCE;
        }

    }

    private static class IntCellToVariableConverter implements CellToVariableConverter<IntType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((IntValue)cell).getIntValue());
        }

        @Override
        public IntType getVariableType() {
            return IntType.INSTANCE;
        }

    }

    private static class IntArrayCellToVariableConverter implements CellToVariableConverter<IntArrayType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, //
                getVariableType(), //
                ((CollectionDataValue)cell).stream() //
                    .mapToInt(c -> ((IntValue)c).getIntValue()) //
                    .boxed() //
                    .toArray(Integer[]::new) //
            );
        }

        @Override
        public IntArrayType getVariableType() {
            return IntArrayType.INSTANCE;
        }

    }

    private static class LongCellToVariableConverter implements CellToVariableConverter<LongType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((LongValue)cell).getLongValue());
        }

        @Override
        public LongType getVariableType() {
            return LongType.INSTANCE;
        }

    }

    private static class LongArrayCellToVariableConverter implements CellToVariableConverter<LongArrayType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, //
                getVariableType(), //
                ((CollectionDataValue)cell).stream() //
                    .mapToLong(c -> ((LongValue)c).getLongValue()) //
                    .boxed() //
                    .toArray(Long[]::new) //
            );
        }

        @Override
        public LongArrayType getVariableType() {
            return LongArrayType.INSTANCE;
        }

    }

    private static class DoubleCellToVariableConverter implements CellToVariableConverter<DoubleType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((DoubleValue)cell).getDoubleValue());
        }

        @Override
        public DoubleType getVariableType() {
            return DoubleType.INSTANCE;
        }

    }

    private static class DoubleArrayCellToVariableConverter implements CellToVariableConverter<DoubleArrayType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, //
                getVariableType(), //
                ((CollectionDataValue)cell).stream() //
                    .mapToDouble(c -> ((DoubleValue)c).getDoubleValue()) //
                    .boxed() //
                    .toArray(Double[]::new) //
            );
        }

        @Override
        public DoubleArrayType getVariableType() {
            return DoubleArrayType.INSTANCE;
        }
    }

    private static class StringCellToVariableConverter implements CellToVariableConverter<StringType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((StringValue)cell).getStringValue());
        }

        @Override
        public StringType getVariableType() {
            return StringType.INSTANCE;
        }

    }

    private static class StringArrayCellToVariableConverter implements CellToVariableConverter<StringArrayType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, //
                getVariableType(), //
                ((CollectionDataValue)cell).stream() //
                    .map(c -> (((StringValue)c).getStringValue())) //
                    .toArray(String[]::new));
        }

        @Override
        public StringArrayType getVariableType() {
            return StringArrayType.INSTANCE;
        }

    }

    private static class FSLocationCellToVariableConverter implements CellToVariableConverter<FSLocationVariableType> {

        @Override
        public FlowVariable createFlowVariable(final String varName, final DataCell cell) {
            return new FlowVariable(varName, getVariableType(), ((FSLocationCell)cell).getFSLocation());
        }

        @Override
        public FSLocationVariableType getVariableType() {
            return FSLocationVariableType.INSTANCE;
        }

    }

}
