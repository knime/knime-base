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
 *   15 May 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableSupplier;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.message.Message;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;

/**
 * A settings class for a "dynamic widget" where the concrete input widget(s) depend on the selected data type.
 *
 * @author Paul Bärnreuther
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class DynamicValuesInput implements Persistable {

    private static final JavaToDataCellConverterRegistry TO_DATACELL = JavaToDataCellConverterRegistry.getInstance();

    private static final DataCellToJavaConverterRegistry FROM_DATACELL = DataCellToJavaConverterRegistry.getInstance();

    final DynamicValue[] m_values;

    final InputKind m_inputKind;

    /**
     * Needed for schema generation.
     */
    DynamicValuesInput() {
        this((DynamicValue[])null, null);
    }

    private DynamicValuesInput(final DynamicValue[] values, final InputKind kind) {
        m_values = values;
        m_inputKind = kind;
    }

    /**
     * Constructor for a single input value, given a data type and cell (of same or subtype) and an initial value. The
     * initial value can be the {@link DataType#getMissingCell()} instance.
     *
     * @param dataType target data type
     * @param initialValue data type and initial value
     * @param useStringCaseMatchingSetting {@code true} to show the case matching setting for strings, {@code false} to
     *            disable the setting regardless of data type
     */
    DynamicValuesInput(final DataType dataType, final DataCell initialValue,
        final boolean useStringCaseMatchingSetting) {
        this(new DynamicValue[]{new DynamicValue(dataType, initialValue, useStringCaseMatchingSetting)},
            InputKind.SINGLE);
    }

    /**
     * Creates an empty input, i.e. an input that does not actually have a concrete input widget. Used for boolean
     * operators {@code IS_TRUE} and {@code IS_FALSE}, that don't need an input value.
     *
     * @return new input
     */
    public static DynamicValuesInput emptySingle() {
        return new DynamicValuesInput(new DynamicValue[]{}, InputKind.SINGLE);
    }

    /**
     * Creates a single-value input that offers case-matching settings for String inputs.
     *
     * @noreference This method is not intended to be referenced by clients.
     *
     * @param dataType data type of input value
     * @return dynamic widget
     */
    public static DynamicValuesInput singleValueWithCaseMatchingForStringWithDefault(final DataType dataType) {
        final DataCell defaultValue;
        // we need to provide some default value such that correct types show up int the settings XML and the
        // flow variable popup can infer which flow variables to show
        if (IntCell.TYPE.equals(dataType)) {
            defaultValue = new IntCell(0);
        } else if (LongCell.TYPE.equals(dataType)) {
            defaultValue = new LongCell(0);
        } else if (DoubleCell.TYPE.equals(dataType)) {
            defaultValue = new DoubleCell(0);
        } else if (BooleanCell.TYPE.equals(dataType)) {
            defaultValue = BooleanCell.FALSE;
        } else {
            defaultValue = DynamicValue.readDataCellFromStringSafe(dataType, "");
        }
        return new DynamicValuesInput(dataType, defaultValue, true);
    }

    /**
     * Creates a configured single-value input. Used within K-AI
     *
     * @noreference This method is not intended to be referenced by clients.
     **/
    public static DynamicValuesInput singleValueWithInitialValue(final DataType dataType, final DataCell defaultValue) {
        // temporary method for K-AI to instantiate e.g. a row filter from model side
        return new DynamicValuesInput(dataType, defaultValue, true);
    }

    /**
     * @return a dynamic values input for a single value of RowID.
     */
    public static DynamicValuesInput forRowID() {
        return new DynamicValuesInput(StringCell.TYPE, new StringCell(""), true);
    }

    /**
     * @param dataType data type to offer widget for Row number comparisons, must be {@link StringCell#TYPE} or
     *            {@link LongCell#TYPE}
     * @return a dynamic values input for a single value of Row Number.
     */
    public static DynamicValuesInput forRowNumber(final DataType dataType) {
        if (StringCell.TYPE.equals(dataType)) {
            return new DynamicValuesInput(dataType, new StringCell("1"), false);
        } else if (LongCell.TYPE.equals(dataType)) {
            return new DynamicValuesInput(LongCell.TYPE, new LongCell(1), true);
        }
        throw new IllegalArgumentException(
            "Data type must be either String or Long type, was \"%s\"".formatted(dataType));
    }

    private enum InputKind {
            SINGLE, DOUBLE, COLLECTION
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DynamicValuesInput dvi)) {
            return false;
        }
        return m_inputKind == dvi.m_inputKind && Arrays.equals(m_values, dvi.m_values);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(m_inputKind).append(m_values).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append(m_inputKind).append(m_values)
            .toString();
    }

    /**
     * Validates input values.
     *
     * @param name name of the input to compare against, e.g. column name, "Row Number", etc.
     * @param type current type to validate against
     * @throws InvalidSettingsException
     */
    public void validate(final String name, final DataType type) throws InvalidSettingsException {
        for (final var value : m_values) {
            value.validate(name, type);
        }
    }

    /**
     * Checks if values are not missing and can be converted to their expected type.
     *
     * @throws InvalidSettingsException ...
     */
    public void checkParseError() throws InvalidSettingsException {
        for (final var value : m_values) {
            value.checkParseError();
        }
    }

    private static boolean isOfNonCollectionType(final DataType type) {
        return !type.isCollectionType();
    }

    // we need to be able to parse strings from the string input and put cells back to string for settings/json
    private static boolean supportsSerialization(final DataType type) {
        return converterToDataCell(type).isPresent() && converterFromDataCell(type).isPresent();
    }

    /**
     * Check whether types given to this input are supported.
     *
     * @param type type to check
     * @return {@code true} if the given type is supported, {@code false} otherwise
     */
    public static boolean supportsDataType(final DataType type) {
        return isOfNonCollectionType(type) && supportsSerialization(type);
    }

    /**
     * The actual dynamic value.
     *
     * @author Paul Bärnreuther
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    @Persistor(DynamicValuePersistor.class)
    static class DynamicValue implements Persistable {

        /**
         * Target type, i.e. the type of the input column at configuration time.
         */
        private DataType m_type;

        /**
         * Non-{@code null} stored value. Usually of {@link #m_type}'s {@code DataType}, but in case type-mapping fails,
         * will be of type {@link StringCell#TYPE} containing the user-entered string value.
         */
        private DataCell m_value;

        /**
         * Nullable string case matching settings.
         */
        private StringCaseMatchingSettings m_caseMatching;

        DynamicValue() {
            // For Deserialization
        }

        /**
         * Value without content. Default content will be supplied by dialog.
         *
         * @param columnType type of the content
         */
        private DynamicValue(final DataType columnType) {
            this(columnType, DataType.getMissingCell(), true);
        }

        /**
         * Value with initial content.
         *
         * @param columnType type of the content
         * @param initialValue initial value or {@link DataType#getMissingCell()} if it should be supplied by dialog
         * @param useStringCaseMatchingSetting {@code true} to show the case matching setting for strings, {@code false}
         *            to disable the setting regardless of data type
         */
        private DynamicValue(final DataType columnType, final DataCell initialValue,
            final boolean useStringCaseMatchingSetting) {
            this(columnType, initialValue, useStringCaseMatchingSetting && columnType == StringCell.TYPE
                ? new StringCaseMatchingSettings() : null);
        }

        private DynamicValue(final DataType columnType, final StringCaseMatchingSettings caseMatchingSettings) {
            this(columnType, DataType.getMissingCell(), caseMatchingSettings);
        }

        /**
         * @param columnType target data type
         * @param initial initial value or {@link DataType#getMissingCell()}
         * @param caseMatchingSettings optional settings for String case matching
         */
        private DynamicValue(final DataType columnType, final DataCell initial,
            final StringCaseMatchingSettings caseMatchingSettings) {
            m_value = Objects.requireNonNull(initial);
            if (columnType.equals(DataType.getMissingCell().getType())) {
                throw new IllegalArgumentException("DynamicValue must not be of \"MissingCell\" type");
            }
            if (columnType.isCollectionType()) {
                throw new IllegalArgumentException(
                    "Collection types are unsupported: \"%s\"".formatted(columnType.getName()));
            }
            m_type = Objects.requireNonNull(columnType);
            if (!initial.isMissing()) {
                final var cellType = initial.getType();
                CheckUtils.checkArgument(
                    // normal case (including normal StringCells)
                    columnType.isASuperTypeOf(cellType)
                        // handle adapter cells
                        || cellType.getValueClasses().stream().anyMatch(columnType::isAdaptable)
                        // error case where the column type is any type except StringCell
                        || StringCell.TYPE.equals(cellType),
                    "Value's type \"%s\" is not a subtype of target data type \"%s\" nor a StringCell.", cellType,
                    columnType);
            }
            m_caseMatching = caseMatchingSettings;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof DynamicValue dv)) {
                return false;
            }
            return m_type.equals(dv.m_type) && m_value.equals(dv.m_value)
                && Objects.equals(m_caseMatching, dv.m_caseMatching);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(13, 37).append(m_type).append(m_value).append(m_caseMatching).toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append(m_type).append(m_value)
                .append(m_caseMatching).toString();
        }

        /**
         * Validates the input field against the given data type, e.g. from a connected input table.
         *
         * @param name name that identifies the input, e.g. column name, or special such as "Row Number"
         * @param type comparison's lhs type
         * @throws InvalidSettingsException if settings are invalid given spec
         */
        private void validate(final String name, final DataType referenceType) throws InvalidSettingsException {
            checkParseError();

            // all OK if the "runtime" input column type is more specific than the type at configuration time
            if (!m_type.isASuperTypeOf(referenceType)) {
                throw Message.builder()
                    .withSummary("Type of input \"%s\" is not compatible with reference value".formatted(name))
                    .addTextIssue("Input \"%s\" has type \"%s\", but reference value is of incompatible type \"%s\""
                        .formatted(name, referenceType.toPrettyString(), m_type.toPrettyString()))
                    .addResolutions("Reconfigure the node to supply a reference value of type \"%s\"."
                        .formatted(referenceType.toPrettyString())) //
                    .addResolutions("Change the input (column) type to \"%s\".".formatted(m_type.toPrettyString())) //
                    .build().orElseThrow().toInvalidSettingsException();
            }
        }

        /**
         * Checks if the value is not missing and can be converted to the expected type.
         *
         * @throws InvalidSettingsException ...
         */
        private void checkParseError() throws InvalidSettingsException {
            CheckUtils.checkSetting(!m_value.isMissing(), "The comparison value is missing.");
            // validate that the current value is not an "error value"
            final var valueType = m_value.getType();
            // if we have a StringCell but we do not expect one, we had a conversion error and need to report it
            if (valueType.equals(StringCell.TYPE) && !m_type.equals(StringCell.TYPE)) {
                // This is the case if `readDataCellFromStringSafe` failed to type-map when closing the dialog
                throw retrieveConversionError(m_type, (StringCell)m_value);
            }
        }

        /**
         * Expects the conversion of the given string cell to the target type to fail, retrieving the conversion
         * exception.
         *
         * @param type target type for conversion
         * @param cell cell to convert to target, expected to fail
         * @return exception with parse error
         */
        private static InvalidSettingsException retrieveConversionError(final DataType type, final StringCell cell) {
            final var stringValue = cell.getStringValue();
            try {
                readDataCellFromString(type, stringValue);
                throw new IllegalStateException(
                    "Expected conversion of string \"%s\" to target type \"%s\" to fail, but it succeeded."
                        .formatted(stringValue, type));
            } catch (final ConverterException e) { // NOSONAR we unwrap our own exception
                final var msgBuilder = Message.builder() //
                    .withSummary(e.getMessage());
                final var cause = e.getCause();
                final var causeMsg = cause != null ? cause.getMessage() : null;
                if (causeMsg != null && !StringUtils.isEmpty(stringValue)) {
                    msgBuilder.addTextIssue(causeMsg);
                }
                return msgBuilder.build() //
                    .orElseThrow() // we set a summary
                    .toInvalidSettingsException();
            }
        }

        private static <E extends Exception> void writeDataCell(final DataCell dataCell,
            final FailableConsumer<Double, E> writeDouble, //
            final FailableConsumer<Integer, E> writeInteger, //
            final FailableConsumer<Long, E> writeLong, //
            final FailableConsumer<Boolean, E> writeBoolean, //
            final FailableConsumer<String, E> writeString //
        ) throws ConverterException, E {
            if (dataCell.isMissing()) {
                throw new IllegalArgumentException("Cannot write MissingCell");
            }
            if (dataCell instanceof DoubleCell doubleCell) {
                writeDouble.accept(doubleCell.getDoubleValue());
            } else if (dataCell instanceof IntCell intCell) {
                writeInteger.accept(intCell.getIntValue());
            } else if (dataCell instanceof LongCell longCell) {
                writeLong.accept(longCell.getLongValue());
            } else if (dataCell instanceof BooleanCell booleanCell) {
                writeBoolean.accept(booleanCell.getBooleanValue());
            } else {
                writeString.accept(getStringFromDataCell(dataCell));
            }
        }

        private static <E extends Throwable> DataCell readDataCell(final DataType dataType,
            final FailableSupplier<Double, E> readDouble, //
            final FailableSupplier<Integer, E> readInteger, //
            final FailableSupplier<Long, E> readLong, //
            final FailableSupplier<Boolean, E> readBoolean, //
            final FailableSupplier<String, E> readString //
        ) throws E {
            if (dataType.equals(DoubleCell.TYPE)) {
                return DoubleCellFactory.create(readDouble.get());
            } else if (dataType.equals(IntCell.TYPE)) {
                return IntCellFactory.create(readInteger.get());
            } else if (dataType.equals(LongCell.TYPE)) {
                return LongCellFactory.create(readLong.get());
            } else if (dataType.equals(BooleanCell.TYPE)) {
                return BooleanCellFactory.create(readBoolean.get());
            } else {
                return readDataCellFromStringSafe(dataType, readString.get());
            }
        }

        private static String getStringFromDataCell(final DataCell dataCell) throws ConverterException {
            // convertUnsafe does not handle missing cells
            if (dataCell.isMissing()) {
                throw new IllegalArgumentException("Cannot get string from MissingCell");
            }
            // check afterwards, since missing cells are also collection types
            final var dataType = dataCell.getType();
            if (dataType.isCollectionType()) {
                throw new IllegalArgumentException(
                    "Collection types are not supported. Given type is: %s".formatted(dataType));
            }
            final var converter = converterFromDataCell(dataType)
                .orElseThrow(() -> new ConverterException("No serializer for type %s".formatted(dataCell.getType())))
                .create();
            try {
                return converter.convertUnsafe(dataCell);
            } catch (final Exception e) {
                final var msg = e.getMessage();
                throw new ConverterException(msg != null ? msg
                    : "Unable to convert data cell of type \"%s\" to String.".formatted(dataCell.getType()), e);
            }
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
        private static DataCell readDataCellFromString(final DataType dataType, final String value)
            throws ConverterException {
            if (value == null) {
                return DataType.getMissingCell();
            }
            if (dataType.isCollectionType()) {
                throw new IllegalArgumentException(
                    "Collection types are not supported. Given type is: %s".formatted(dataType));
            }

            final var converter = converterToDataCell(dataType)
                .orElseThrow(() -> new ConverterException("No deserializer for type %s".formatted(dataType)))
                .create((ExecutionContext)null);
            try {
                return converter.convert(value);
            } catch (final Exception e) {
                final var isEmpty = StringUtils.isEmpty(value);
                final var content = isEmpty ? "empty input" : "input \"%s\"".formatted(value);
                final var message = "Could not convert %s to \"%s\"".formatted(content, dataType);
                throw new ConverterException(message, e.getCause());
            }
        }

        private static DataCell readDataCellFromStringSafe(final DataType dataType, final String value) {
            try {
                return readDataCellFromString(dataType, value);
                // We keep the user-entered value in a StringCell (the m_type remains of another type),
                // in order to parse it again for error reporting
            } catch (final ConverterException e) {
                return new StringCell(value);
            }
        }
    }

    // used in frontend to show custom string case matching settings
    private static final String USE_STRING_CASE_MATCHING = "useStringCaseMatchingSettings";

    // this contains the actual data for case matching of strings
    private static final String MATCHING_KEY = "stringCaseMatching";

    private static final String VALUE_KEY = "value";

    private static final String MISSING_CELL_ERR_KEY = "missingCellError";

    private static final String TYPE_ID_KEY = "typeIdentifier";

    static class DynamicValuePersistor implements NodeParametersPersistor<DynamicValue> {

        @Override
        public DynamicValue load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(MISSING_CELL_ERR_KEY)) {
                throw new InvalidSettingsException(settings.getString(MISSING_CELL_ERR_KEY));
            }
            final var dataType = settings.getDataType(TYPE_ID_KEY);

            final var caseMatching = settings.containsKey(MATCHING_KEY) ? NodeParametersUtil
                .loadSettings(settings.getNodeSettings(MATCHING_KEY), StringCaseMatchingSettings.class) : null;

            if (settings.containsKey(VALUE_KEY)) {
                final var dataCell = DynamicValue.<InvalidSettingsException> readDataCell(dataType, //
                    () -> settings.getDouble(VALUE_KEY), //
                    () -> settings.getInt(VALUE_KEY), //
                    () -> settings.getLong(VALUE_KEY), //
                    () -> settings.getBoolean(VALUE_KEY), //
                    () -> settings.getString(VALUE_KEY) //
                );
                return new DynamicValue(dataType, dataCell, caseMatching);
            }
            return new DynamicValue(dataType, caseMatching);
        }

        @Override
        public void save(final DynamicValue obj, final NodeSettingsWO settings) {
            throw new UnsupportedOperationException("Saving legacy dynamic value inputs is no longer supported.");
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{new String[]{}}; //all sub paths
        }
    }

    /**
     * Exception to indicate serious converter exceptions indicating a value cannot be serialized or deserialized.
     */
    private static final class ConverterException extends Exception {

        private static final long serialVersionUID = 1L;

        ConverterException(final String msg) {
            super(msg);
        }

        ConverterException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Gets the cell at the specified index, or {@link Optional#empty()} if no value exists at the specified index. If
     * the index does not exist, will throw a runtime exception.
     *
     * @param index index to get value at
     * @return value or empty optional if no value exists at the specified index
     * @throws ArrayIndexOutOfBoundsException
     */
    public Optional<DataCell> getCellAt(final int index) {
        return Optional.ofNullable(m_values[index].m_value);
    }

    /**
     * Checks case-matching for string value at given existing index.
     *
     * @noreference This method is not intended to be referenced by clients.
     *
     * @param index index to check
     * @return {@code true} if the value at the given index should be matched case-sensitive, {@code false} otherwise
     * @throws IllegalArgumentException If the value at the specified index does not use the case-matching setting
     */
    public boolean isStringMatchCaseSensitive(final int index) {
        CheckUtils.checkArgument(m_values[index].m_caseMatching != null,
            "Value at index %d does not use case-matching settings".formatted(index));
        return m_values[index].m_caseMatching.isCaseSensitive();
    }

    /**
     * Checks if the current input widget values can be reused in place of the given input values, i.e. it has the same
     * number of input values, and for each pair of values it holds that
     * <ol>
     * <li>types equal,</li>
     * <li>case matching settings are both used or not used.</li>
     * </ol>
     *
     * @param newInput starting input values
     * @return {@code true} if the current input values can be used in place of the given input values, {@code false}
     *         otherwise
     */
    public boolean isConfigurableFrom(final DynamicValuesInput newInput) {
        if (newInput.m_values.length != m_values.length) {
            return false;
        }
        for (var i = 0; i < newInput.m_values.length; i++) {
            final var current = m_values[i];
            final var newValues = newInput.m_values[i];
            if (!newValues.m_type.equals(current.m_type)) {
                return false;
            }
            if (!((newValues.m_caseMatching == null && current.m_caseMatching == null)
                || (newValues.m_caseMatching != null && current.m_caseMatching != null))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts this input to the given template type, if possible. If a conversion to the target type is not possible
     * since the given value cannot be expressed in the target type by going through its string representation, the
     * string representation will be stored. If the given value has no string representation an empty optional will be
     * returned.
     *
     * The actual parse error can be checked by validating the input.
     *
     * @param template input arity, type information, and default value
     * @return non-empty input with values converted to types according to given template, string value if the input
     *         does not represent a valid value in the target type, or {@link Optional#empty()} if conversion is not
     *         possible due to arity mismatch, input kind mismatch, or missing string representation of source value
     */
    public Optional<DynamicValuesInput> convertToType(final DynamicValuesInput template) {
        if (m_values.length != template.m_values.length || m_inputKind != template.m_inputKind) {
            return Optional.empty();
        }
        final var convertedValues = new DynamicValue[m_values.length];
        for (int i = 0; i < m_values.length; i++) {
            final var value = convert(m_values[i], template.m_values[i]);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            convertedValues[i] = value.get();
        }
        return Optional.of(new DynamicValuesInput(convertedValues, m_inputKind));
    }

    /**
     * Tries to convert the given value into the template's type. If a conversion to the target type is not possible
     * since the given value cannot be expressed in the target type by going through its string representation, the
     * string representation will be stored. If the given value has no string representation an empty optional will be
     * returned.
     *
     * @param currentValue value to convert
     * @param templateValue template to convert to
     * @return converted value or string value if conversion is not possible, empty optional if source value has no
     *         string representation
     */
    private static Optional<DynamicValue> convert(final DynamicValue value, final DynamicValue templateValue) {
        final var currentValueCell = value.m_value;
        if (currentValueCell.isMissing()) {
            return Optional.empty();
        }
        final var targetType = templateValue.m_type;
        if (currentValueCell.getType().equals(targetType)
            && !(value.m_caseMatching == null ^ templateValue.m_caseMatching == null)) {
            // we can use the provided value as-is (we cannot use DynamicValue#equals, since the template's value and
            // case matching setting can be different)
            return Optional.of(value);
        }
        final var caseMatching = getCaseMatching(templateValue.m_caseMatching, value.m_caseMatching);
        try {
            final var stringValue = currentValueCell instanceof StringCell sc ? sc.getStringValue()
                : DynamicValue.getStringFromDataCell(currentValueCell);
            final var convertedCell = DynamicValue.readDataCellFromStringSafe(targetType, stringValue);
            if (convertedCell.isMissing()) {
                // if the non-null input did not convert to a useful cell,
                // i.e. non-missing, we see the conversion as failed
                return Optional.empty();
            }
            return Optional.of(new DynamicValue(targetType, convertedCell, caseMatching));
        } catch (final ConverterException e) { // NOSONAR best effort input conversion expected to fail quite often
            return Optional.empty();
        }
    }

    /**
     * Re-uses or initializes default case matching settings, if the template specifies to use the setting.
     *
     * @param templateValue template specifying whether to use case matching settings
     * @param currentValue value to copy in case it is present
     * @return
     */
    private static StringCaseMatchingSettings getCaseMatching(final StringCaseMatchingSettings templateCaseMatching,
        final StringCaseMatchingSettings currentCaseMatching) {
        if (templateCaseMatching == null) {
            return null;
        }
        return currentCaseMatching == null ? new StringCaseMatchingSettings() : currentCaseMatching;
    }

    // can be used for testing
    static DynamicValuesInput testDummy() {
        return new DynamicValuesInput(new DynamicValue[]{//
            new DynamicValue(StringCell.TYPE), //
            new DynamicValue(DoubleCell.TYPE), //
            new DynamicValue(IntCell.TYPE), //
            new DynamicValue(BooleanCell.TYPE),//
                /** currently breaks things, since we have no validation on dialog close */
                //new DynamicValue(IntervalCell.TYPE)
        }, InputKind.SINGLE);
    }

    /* == Lookup of type-mappers. == */

    /**
     * Tries to get a converter from String to the given target data type (or a super type of it).
     *
     * @param registry converter registry to use
     * @param targetType target type
     * @return deserializing converter
     */
    private static Optional<JavaToDataCellConverterFactory<String>> converterToDataCell(final DataType targetType) {
        final var direct = TO_DATACELL.getConverterFactories(String.class, targetType);
        if (!direct.isEmpty()) {
            return Optional.of(direct.iterator().next());
        }
        // reverse-lookup types and get the converter for one of those, fitting our target type
        // e.g. targetType = SmilesAdapterCell, super type will be SmilesCell for which a converter exists
        return getSuperTypes(targetType) //
            .flatMap(type -> TO_DATACELL.getConverterFactories(String.class, type).stream()) //
            .findFirst();
    }

    /**
     * Tries to get a converter from the source type to String, including support for types that the given source type
     * can adapt to.
     *
     * @param registry converter registry to use
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
        // Example: the registry only contains converters to non-subtype cell types, e.g. String->SmilesCell.
        // When passsed a SmilesAdapterCell, the above lookup fails to identify any converters, so we have to look
        // further. Since SmilesCell and SmilesAdapterCell do not implement the exact same value interfaces
        // (there is RWAdapterValue(AdapterValue) included for the adapter one), we need to do a reverse lookup on the
        // subtype map.
        final var cellClass = type.getCellClass();
        // non-native types have no CellClass, so we cannot type-map them
        if (cellClass == null) {
            return Stream.empty();
        }
        final var dtr = DataTypeRegistry.getInstance();
        final var className = cellClass.getName();
        return dtr.availableDataTypes().stream()
            .filter(t -> dtr.getImplementationSubDataTypes(t).anyMatch(className::equals));
    }
}
