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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
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
import org.knime.core.data.MissingValue;
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
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;

/**
 * Factory class to create the {@link CellToVariableConverter} associated with the provided {@link DataType}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
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
     * @throws NoSuchElementException If there exists no converter for the provided cell type
     */
    public static CellToVariableConverter<?> createConverter(final DataType type) { //NOSONAR
        final DataType eleType = getType(type);
        return getSupportedTypes(type).entrySet().stream() //
            .filter(e -> eleType.isCompatible(e.getKey())) //
            .map(Map.Entry::getValue) //
            .map(Supplier::get) //
            .findFirst() //
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("There is no Cell to Variable converter associated with the provided cell type '%s'",
                    getTypeName(type))));
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

    private static String getTypeName(final DataType type) {
        var currentType = type;
        final var typeName = new StringBuilder(currentType.getName());
        while (currentType.isCollectionType()) {
            currentType = currentType.getCollectionElementType();
            typeName.append(" of ").append(currentType.getName());
        }
        return typeName.toString();
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

    private abstract static class AbstractCellToVariableConverter<T> implements CellToVariableConverter<T> {
        @Override
        public Optional<FlowVariable> createFlowVariable(final String varName, final DataCell cell,
            final MissingValueHandler handler) {
            return mayCreateVariableValue(cell, handler).map(v -> new FlowVariable(varName, getVariableType(), v));
        }

        @SuppressWarnings("unchecked")
        protected Optional<T> mayCreateVariableValue(final DataCell cell, final MissingValueHandler handler) {
            if (cell.isMissing()) {
                return Optional.ofNullable((T)handler.handle((MissingValue)cell, getVariableType()));
            }
            return Optional.of(createVariableValue(cell, handler));
        }

        protected abstract T createVariableValue(final DataCell cell, final MissingValueHandler handler);
    }

    private abstract static class AbstractCellToArrayVariableConverter<T> extends AbstractCellToVariableConverter<T[]> {

        private BiFunction<DataCell, MissingValueHandler, Optional<T>> m_conversion = null;

        private void initConversion() {
            final var elementConverter = getElementConverter();
            if (elementConverter instanceof AbstractCellToVariableConverter<?>) { // avoid unnecessary flow variable creation
                final var castConverter = (AbstractCellToVariableConverter<T>)elementConverter;
                m_conversion = castConverter::mayCreateVariableValue;
            } else { // fail save
                m_conversion = (c, h) -> elementConverter.createFlowVariable("?", c, h)
                    .map(v -> v.getValue(elementConverter.getVariableType()));
            }
        }

        @Override
        protected T[] createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            if (m_conversion == null) {
                initConversion();
            }
            return createArray(((CollectionDataValue)cell).stream() //
                .map(c -> m_conversion.apply(c, handler)) //
                .filter(Optional::isPresent) //
                .map(Optional::get));
        }

        /**
         * Turns the stream into an array of that type.<br>
         * Needed because arrays of generics cannot be instantiated.
         *
         * @param stream the stream to convert
         * @return the resulting array.
         */
        protected abstract T[] createArray(Stream<T> stream);

        /**
         * @return the {@link CellToVariableConverter} that is used to convert the cells in the cell collection.<br>
         *         This is needed because the type of empty lists or lists with only missing values cannot be reliably
         *         retrieved.
         * @apiNote this function will only be cached in the object
         */
        protected abstract CellToVariableConverter<T> getElementConverter();
    }

    private static class BooleanCellToVariableConverter extends AbstractCellToVariableConverter<Boolean> {

        @Override
        protected Boolean createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((BooleanValue)cell).getBooleanValue();
        }

        @Override
        public BooleanType getVariableType() {
            return BooleanType.INSTANCE;
        }
    }

    private static class BooleanArrayCellToVariableConverter extends AbstractCellToArrayVariableConverter<Boolean> {

        @Override
        public BooleanArrayType getVariableType() {
            return BooleanArrayType.INSTANCE;
        }

        @Override
        protected CellToVariableConverter<Boolean> getElementConverter() {
            return new BooleanCellToVariableConverter();
        }

        @Override
        protected Boolean[] createArray(final Stream<Boolean> stream) {
            return stream.toArray(Boolean[]::new);
        }
    }

    private static class IntCellToVariableConverter extends AbstractCellToVariableConverter<Integer> {

        @Override
        protected Integer createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((IntValue)cell).getIntValue();
        }

        @Override
        public IntType getVariableType() {
            return IntType.INSTANCE;
        }
    }

    private static class IntArrayCellToVariableConverter extends AbstractCellToArrayVariableConverter<Integer> {

        @Override
        protected Integer[] createArray(final Stream<Integer> stream) {
            return stream.toArray(Integer[]::new);
        }

        @Override
        protected CellToVariableConverter<Integer> getElementConverter() {
            return new IntCellToVariableConverter();
        }

        @Override
        public IntArrayType getVariableType() {
            return IntArrayType.INSTANCE;
        }
    }

    private static class LongCellToVariableConverter extends AbstractCellToVariableConverter<Long> {

        @Override
        protected Long createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((LongValue)cell).getLongValue();
        }

        @Override
        public LongType getVariableType() {
            return LongType.INSTANCE;
        }
    }

    private static class LongArrayCellToVariableConverter extends AbstractCellToArrayVariableConverter<Long> {

        @Override
        protected Long[] createArray(final Stream<Long> stream) {
            return stream.toArray(Long[]::new);
        }

        @Override
        protected CellToVariableConverter<Long> getElementConverter() {
            return new LongCellToVariableConverter();
        }

        @Override
        public LongArrayType getVariableType() {
            return LongArrayType.INSTANCE;
        }
    }

    private static class DoubleCellToVariableConverter extends AbstractCellToVariableConverter<Double> {

        @Override
        protected Double createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((DoubleValue)cell).getDoubleValue();
        }

        @Override
        public DoubleType getVariableType() {
            return DoubleType.INSTANCE;
        }
    }

    private static class DoubleArrayCellToVariableConverter extends AbstractCellToArrayVariableConverter<Double> {

        @Override
        protected Double[] createArray(final Stream<Double> stream) {
            return stream.toArray(Double[]::new);
        }

        @Override
        protected CellToVariableConverter<Double> getElementConverter() {
            return new DoubleCellToVariableConverter();
        }

        @Override
        public DoubleArrayType getVariableType() {
            return DoubleArrayType.INSTANCE;
        }
    }

    private static class StringCellToVariableConverter extends AbstractCellToVariableConverter<String> {

        @Override
        protected String createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((StringValue)cell).getStringValue();
        }

        @Override
        public StringType getVariableType() {
            return StringType.INSTANCE;
        }
    }

    private static class StringArrayCellToVariableConverter extends AbstractCellToArrayVariableConverter<String> {

        @Override
        protected String[] createArray(final Stream<String> stream) {
            return stream.toArray(String[]::new);
        }

        @Override
        protected CellToVariableConverter<String> getElementConverter() {
            return new StringCellToVariableConverter();
        }

        @Override
        public StringArrayType getVariableType() {
            return StringArrayType.INSTANCE;
        }

    }

    private static class FSLocationCellToVariableConverter extends AbstractCellToVariableConverter<FSLocation> {

        @Override
        protected FSLocation createVariableValue(final DataCell cell, final MissingValueHandler handler) {
            return ((FSLocationValue)cell).getFSLocation();
        }

        @Override
        public FSLocationVariableType getVariableType() {
            return FSLocationVariableType.INSTANCE;
        }
    }
}
