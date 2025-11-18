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
 *   Sep 3, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.node.ExecutionContext;

/**
 * Utility class for mapping between String representations and DataCells.
 *
 * @author Paul Bärnreuther
 */
final class TypeMappingUtils {

    private static final JavaToDataCellConverterRegistry TO_DATACELL = JavaToDataCellConverterRegistry.getInstance();

    private static final DataCellToJavaConverterRegistry FROM_DATACELL = DataCellToJavaConverterRegistry.getInstance();

    private TypeMappingUtils() {
        // Utility class
    }

    /**
     * Converts the given string value to a cell of the given data type. In case the value was {@code null}, a plain
     * missing cell without an error message is returned.
     *
     * @param dataType target data type
     * @param value string value to deserialize
     * @return data cell for the string representation
     * @throws ConverterException in case the string value could not be converted into the given data type
     */
    static DataCell readDataCellFromString(final DataType dataType, final String value)
        throws ConverterException {
        if (value == null) {
            return DataType.getMissingCell();
        }
        if (dataType.isCollectionType()) {
            throw new IllegalArgumentException(
                "Collection types are not supported. Given type is: %s".formatted(dataType));
        }

        final var converter = converterToDataCell(dataType)
            .orElseThrow(() -> new ConverterException("No deserializer registered for type \"%s\"".formatted(dataType)))
            .create((ExecutionContext)null);
        try {
            return converter.convert(value);
        } catch (final Exception e) {
            throw toUnpacked(e);
        }
    }

    /**
     * Throws a {@link ConverterException} with the top-most non-empty message from the given exception chain,
     * or an empty message if no such message could be found.
     *
     * @param e exception to unpack
     * @returns ConverterException exception with top-most non-empty message or empty message
     */
    private static ConverterException toUnpacked(final Exception e) {
        // find first cause with message to be a bit more helpful
        Throwable cause = e;
        while (cause != null) {
            if (StringUtils.isNotBlank(cause.getMessage())) {
                return new ConverterException(cause.getMessage(), cause.getCause());
            }
            cause = cause.getCause();
        }
        // we don't have any details, so we just signal that conversion failed (no need to create
        // a "could not convert" message, callers will do that)
        return new ConverterException(null, cause);
    }

    /**
     * Converts a non-missing DataCell to its String representation.
     *
     * @param dataCell the cell to convert
     * @return string representation of the cell
     * @throws ConverterException if conversion fails
     */
    static String getStringFromDataCell(final DataCell dataCell) throws ConverterException {
        if (dataCell.isMissing()) {
            throw new IllegalArgumentException("Cannot get string from MissingCell");
        }
        final var dataType = dataCell.getType();
        if (dataType.isCollectionType()) {
            throw new IllegalArgumentException(
                "Collection types are not supported. Given type is: %s".formatted(dataType));
        }
        final var converter = converterFromDataCell(dataType)
            .orElseThrow(
                () -> new ConverterException("No serializer registered for type \"%s\"".formatted(dataCell.getType())))
            .create();
        try {
            return converter.convertUnsafe(dataCell);
        } catch (final Exception e) {
            throw toUnpacked(e);
        }
    }

    /**
     * Check whether types given to this input are supported.
     *
     * @param type type to check
     * @return {@code true} if the given type is supported, {@code false} otherwise
     */
    static boolean supportsDataType(final DataType type) {
        return isOfNonCollectionType(type) && supportsSerialization(type);
    }

    private static boolean isOfNonCollectionType(final DataType type) {
        return !type.isCollectionType();
    }

    private static boolean supportsSerialization(final DataType type) {
        return converterToDataCell(type).isPresent() && converterFromDataCell(type).isPresent();
    }

    /**
     * Tries to get a converter from String to the given target data type (or a super type of it).
     *
     * @param targetType target type
     * @return deserializing converter
     */
    private static Optional<JavaToDataCellConverterFactory<String>> converterToDataCell(final DataType targetType) {
        final var direct = TO_DATACELL.getConverterFactories(String.class, targetType);
        if (!direct.isEmpty()) {
            return Optional.of(direct.iterator().next());
        }
        return getSuperTypes(targetType) //
            .flatMap(type -> TO_DATACELL.getConverterFactories(String.class, type).stream()) //
            .findFirst();
    }

    /**
     * Tries to get a converter from the source type to String, including support for types that the given source type
     * can adapt to.
     *
     * @param srcType source type
     * @return serializing converter
     */
    private static Optional<DataCellToJavaConverterFactory<? extends DataValue, String>>
        converterFromDataCell(final DataType srcType) {
        final var direct = FROM_DATACELL.getConverterFactories(srcType, String.class);
        if (!direct.isEmpty()) {
            return Optional.of(direct.iterator().next());
        }
        return getSuperTypes(srcType) //
            .flatMap(type -> FROM_DATACELL.getConverterFactories(type, String.class).stream()) //
            .findFirst();
    }

    /**
     * Gets all super types of the given type to use in type-mapping.
     *
     * @param type type which maybe is a subtype of any other super types
     * @return stream of types which the given type is a subtype of
     */
    private static Stream<DataType> getSuperTypes(final DataType type) {
        final var cellClass = type.getCellClass();
        if (cellClass == null) {
            return Stream.empty();
        }
        final var dtr = DataTypeRegistry.getInstance();
        final var className = cellClass.getName();
        return dtr.availableDataTypes().stream()
            .filter(t -> dtr.getImplementationSubDataTypes(t).anyMatch(className::equals));
    }

    /**
     * Exception to indicate serious converter exceptions indicating a value cannot be serialized or deserialized.
     */
    static final class ConverterException extends Exception {

        private static final long serialVersionUID = 1L;

        public ConverterException(final String msg) {
            super(msg);
        }

        public ConverterException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}
