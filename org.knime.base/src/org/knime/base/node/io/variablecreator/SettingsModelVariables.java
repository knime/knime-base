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
 *   06.04.2021 (jl): created
 */
package org.knime.base.node.io.variablecreator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;
import org.knime.core.util.Pair;

/**
 * A representation of the variables set by the user. The state of this table is always valid (except for unset (empty)
 * names while creating entries).
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class SettingsModelVariables extends SettingsModel {

    /** Contains the default name that is automatically used. */
    static final String DEFAULT_NAME_PREFIX = "variable";

    /** Contains the key that is used for the variables as a whole. */
    private static final String SETTINGS_KEY = "variables";

    /** Contains the key that is used for the supported variable types. */
    private static final String SETTINGS_SUPPORTED_TYPES = "supportedTypes";

    /** Contains the key that is used for the variable type column. */
    private static final String SETTINGS_COL_TYPE = "types";

    /** Contains the key that is used for the variable name column. */
    private static final String SETTINGS_COL_NAME = "names";

    /** Contains the key that is used for the variable value column. */
    private static final String SETTINGS_COL_VAL = "values";

    /** Defines the names of the columns of the table. */
    private static final String[] COL_NAMES = new String[]{"Type", "Variable Name", "Value"};

    /** The number of columns. It should be used instead of magic values. */
    static final int COL_NUMBER = COL_NAMES.length;

    /** The index of the type column. It should be used instead of magic values. */
    static final int COL_TYPE_INDEX = 0;

    /** The index of the name column. It should be used instead of magic values. */
    static final int COL_NAME_INDEX = 1;

    /** The index of the value column. It should be used instead of magic values. */
    static final int COL_VAL_INDEX = 2;

    /** Message set if a variable overrides an external variable. */
    static final String MSG_NAME_EXTERNAL_OVERRIDES = "Name overrides upstream variable";

    /** Message set if a variable has the same name as an external variable. */
    static final String MSG_NAME_EXTERNAL_CONFLICT = "Upstream name conflict; may lead to unexpected behavior";

    /**
     * Represents a type as it is saved in the table with the type object and the save representation attached.
     *
     * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
     */
    public enum Type {
            /** The representation of {@link org.knime.core.node.workflow.VariableType.StringType}. */
            STRING(VariableType.StringType.INSTANCE, "str", ""),
            /** The representation of {@link org.knime.core.node.workflow.VariableType.IntType}. */
            INTEGER(VariableType.IntType.INSTANCE, "int", "0"),
            /** The representation of {@link org.knime.core.node.workflow.VariableType.LongType}. */
            LONG(VariableType.LongType.INSTANCE, "long", "0"),
            /** The representation of {@link org.knime.core.node.workflow.VariableType.DoubleType}. */
            DOUBLE(VariableType.DoubleType.INSTANCE, "double", "0.0"),
            /** The representation of {@link org.knime.core.node.workflow.VariableType.BooleanType}. */
            BOOLEAN(VariableType.BooleanType.INSTANCE, "bool", "false");

        /** The associated variable type. */
        final VariableType<?> m_type;

        /** The representation that is used when saving. */
        final String m_strRepresentation;

        /** The default value as a string. */
        final String m_defaultStringValue;

        /**
         * Constructs a new type-string association.
         *
         * @param type the variable type
         * @param strRepresentation the string representation used in settings
         * @param defaultStringValue the string representation of the default value
         */
        Type(final VariableType<?> type, final String strRepresentation, final String defaultStringValue) {
            m_type = Objects.requireNonNull(type);
            m_strRepresentation = Objects.requireNonNull(strRepresentation);
            m_defaultStringValue = Objects.requireNonNull(defaultStringValue);
        }

        /**
         * @return the {@link VariableType} associated with this type.
         */
        VariableType<?> getVariableType() { // NOSONAR: cannot be expressed in any other way without causing an error
            return m_type;
        }

        /**
         * @return the identifier of the {@link VariableType} associated with this type.
         */
        public String getIdentifier() {
            return m_type.getIdentifier();
        }

        /**
         * @return the string representation of this type.
         */
        public String getStrRepresentation() {
            return m_strRepresentation;
        }

        /**
         * @return the string representation of the default value
         */
        public String getDefaultStringValue() {
            return m_defaultStringValue;
        }

        /**
         * Tries to get a {@link Type} from its string representation.
         *
         * @param representation the string representation of the type
         * @return the type with that representation or <code>null</code> if no such type was found.
         */
        static Type getTypeFromString(final String representation) {
            for (Type t : Type.values()) {
                if (t.m_strRepresentation.equals(representation)) {
                    return t;
                }
            }
            return null;
        }

        /**
         * Tries to get a {@link Type} from its {@link VariableType}.
         *
         * @param type the {@link VariableType} of the type
         * @return the type with that {@link VariableType} or <code>null</code> if no such type was found.
         */
        static Type getTypeFromVariableType(final VariableType<?> type) {
            for (Type t : Type.values()) {
                if (t.m_type.equals(type)) {
                    return t;
                }
            }
            return null;
        }

        /**
         * @param from the elements for which to get the types
         * @return all the {@link VariableType}s that are associated a given types.
         */
        public static VariableType<?>[] toVariableTypes(final Type[] from) { // NOSONAR: cannot be expressed in any other way without causing an error
            return Arrays.stream(from).map(Type::getVariableType).toArray(VariableType<?>[]::new);
        }

        /**
         * @return all registered variable types.
         * @implNote this is the same as {@link VariableType#getAllTypes()} so the same notices apply.
         */
        public static VariableType<?>[] getAllTypes() { // NOSONAR: cannot be expressed in any other way without causing an error
            return VariableTypeRegistry.getInstance().getAllTypes();
        }
    }

    /** The configured name. */
    private final String m_configName;

    /* The types that are that are selectable for this model. */
    private Type[] m_supportedTypes;

    /** A list of the user given variable types (as their string representation). */
    private List<Type> m_variableTypes;

    /** A list of the user given variable names. */
    private List<String> m_variableNames;

    /** A list of the user given default values. */
    private List<Object> m_variableValues;

    /** The variables that are defined over the “Variable Inport”. */
    private Map<String, FlowVariable> m_externalVariables;

    /**
     * Creates a new table to store the variables.
     *
     * @param configName the name used for this configuration.
     * @param types the types that will be presented to the user and can be saved in this model
     * @param externalVariables the external inport variables used for checking for name conflicts and overridden
     *            variables
     */
    public SettingsModelVariables(final String configName, final Type[] types,
        final Map<String, FlowVariable> externalVariables) {
        m_configName = Objects.requireNonNull(configName);
        m_externalVariables = Objects.requireNonNull(externalVariables);
        m_supportedTypes = Objects.requireNonNull(types);
        m_variableNames = new ArrayList<>();
        m_variableTypes = new ArrayList<>();
        m_variableValues = new ArrayList<>();
    }

    /**
     * Clones this {@link SettingsModelVariables}.
     *
     * @param other the object to clone
     */
    private SettingsModelVariables(final SettingsModelVariables other) {
        m_configName = other.m_configName;
        m_externalVariables = other.m_externalVariables;
        m_supportedTypes = other.m_supportedTypes;
        m_variableNames = new ArrayList<>(other.m_variableNames);
        m_variableTypes = new ArrayList<>(other.m_variableTypes);
        m_variableValues = new ArrayList<>(other.m_variableValues);

    }

    /**
     * Resets (clears) the values in the table
     */
    final void reset() {
        m_variableNames.clear();
        m_variableTypes.clear();
        m_variableValues.clear();
    }

    /**
     * @return the amount of variables that are defined in this model.
     */
    public int getRowCount() {
        return m_variableNames.size();
    }

    /**
     * @param externalVariables the variables from the “Variable Inport”
     */
    public void setExternalVariables(final Map<String, FlowVariable> externalVariables) {
        m_externalVariables = Objects.requireNonNull(externalVariables);
    }

    /**
     * @param column the number of the column
     * @return the name of the column of an empty string if that is not given
     */
    static String getColumnName(final int column) {
        if (column >= 0 && column < COL_NAMES.length) {
            return COL_NAMES[column];
        } else {
            return "";
        }
    }

    /**
     * @return the types that are that are selectable for this model.
     */
    public Type[] getSupportedTypes() {
        return m_supportedTypes;
    }

    /**
     * @param row the number of the value of retrieve
     * @return the type of the variable in row <code>row</code>.
     */
    public Type getType(final int row) {
        return m_variableTypes.get(row);
    }

    /**
     * Tries to set the type of a specific row.
     *
     * @param row the row of which to set the value
     * @param type the type to set.
     *
     * @return whether the operation was successful and an optional description of the error/warning.
     */
    Pair<Boolean, Optional<String>> setType(final int row, final Type type) {
        if (!ArrayUtils.contains(m_supportedTypes, type)) {
            return new Pair<>(Boolean.FALSE, Optional.of("Type not supported"));
        }
        final var notify = m_variableTypes.get(row) != type;
        m_variableTypes.set(row, Objects.requireNonNull(type));

        if (notify) {
            notifyChangeListeners();
        }
        return new Pair<>(Boolean.TRUE, Optional.empty());
    }

    /**
     * @param row the number of the value of retrieve
     * @return the name of the variable in row <code>row</code>.
     */
    public String getName(final int row) {
        return m_variableNames.get(row);
    }

    /**
     * Tries to set the name of a specific row. The value will not be updated if the operation fails.
     *
     * @param row the row of which to set the value
     * @param name the new name to use. A name may not be empty or only consist of white space and there may not already
     *            exist another variable with this name.
     *
     * @return whether the operation was successful and an optional description of the error/warning.
     */
    Pair<Boolean, Optional<String>> setName(final int row, final String name) {
        final Pair<Boolean, Optional<String>> desc = checkVariableName(row, name);
        if (desc.getFirst().booleanValue()) {
            final var notify = !m_variableNames.get(row).equals(name);
            m_variableNames.set(row, name);
            if (notify) {
                notifyChangeListeners();
            }
        }
        return desc;
    }

    /**
     * @param row the number of the value of retrieve
     * @return the value of the variable in row <code>row</code>.
     */
    public Object getValue(final int row) {
        return m_variableValues.get(row);
    }

    /**
     * Tries to set the value of a specific row. The value will not be updated if the operation fails.
     *
     * @param row the row of which to set the value
     * @param value the value which to set. This must be a valid string representation of the given value.
     *
     * @return whether the operation was successful and an optional description of the error/warning.
     */
    Pair<Boolean, Optional<String>> setValue(final int row, final String value) {
        final Pair<Optional<Object>, Optional<String>> result = checkVariableValueString(getType(row), value);
        final Optional<Object> parsedValue = result.getFirst();
        if (parsedValue.isPresent()) {
            final var notify = !parsedValue.get().equals(m_variableValues.get(row));
            m_variableValues.set(row, parsedValue.get());
            if (notify) {
                notifyChangeListeners();
            }
            return new Pair<>(Boolean.TRUE, result.getSecond());
        } else {
            return new Pair<>(Boolean.FALSE, result.getSecond());
        }
    }

    /**
     * Add a generic row of type string to the end of the table.
     */
    void addRow() {
        m_variableTypes.add(Type.STRING);
        m_variableNames.add("");
        m_variableValues.add("");

        notifyChangeListeners();
    }

    /**
     * Removes the specified row from the table.
     *
     * @param row the row which to remove
     */
    void removeRow(final int row) {
        m_variableTypes.remove(row);
        m_variableNames.remove(row);
        m_variableValues.remove(row);

        notifyChangeListeners();
    }

    /**
     * Swaps two rows in the table.
     *
     * @param first the first row
     * @param second the second row
     */
    void swapRows(final int first, final int second) {
        Collections.swap(m_variableTypes, first, second);
        Collections.swap(m_variableNames, first, second);
        Collections.swap(m_variableValues, first, second);

        if (first != second) {
            notifyChangeListeners();
        }
    }

    /**
     * Checks whether a variable name is valid an returns a short string describing the problem if it is not. This
     * method does only check whether the name was already defined by external variables and issues a warning, not
     * internally defined names.
     *
     * @param row the row the name should be placed in
     * @param name the name to check
     * @return whether the name is valid and an error/warning description if present
     */
    Pair<Boolean, Optional<String>> checkVariableNameExternal(final int row, final String name) {
        if (name.trim().isEmpty()) {
            return new Pair<>(Boolean.FALSE, Optional.of("Name is empty"));
        } else {
            final var externalVariable = m_externalVariables.get(name);
            if (externalVariable != null) {
                // Will only get overridden if the name and the type are the same
                if (externalVariable.getVariableType().equals(m_variableTypes.get(row).getVariableType())) {
                    return new Pair<>(Boolean.TRUE, Optional.of(MSG_NAME_EXTERNAL_OVERRIDES));
                } else {
                    return new Pair<>(Boolean.TRUE, Optional.of(MSG_NAME_EXTERNAL_CONFLICT));
                }
            }
        }
        return new Pair<>(Boolean.TRUE, Optional.empty());
    }

    /**
     * Checks whether a variable name is valid an returns a short string describing the problem if it is not.
     *
     * @param row the row the name should be placed in
     * @param name the name to check
     * @return whether the name is valid and an error/warning description if present
     */
    Pair<Boolean, Optional<String>> checkVariableName(final int row, final String name) {
        if (name.trim().isEmpty()) {
            return new Pair<>(Boolean.FALSE, Optional.of("Name is empty"));
        } else {
            final int first = m_variableNames.indexOf(name);
            if ((first >= 0 && first != row) || first != m_variableNames.lastIndexOf(name)) {
                return new Pair<>(Boolean.FALSE, Optional.of("Name is already in use"));
            }
        }
        return new Pair<>(Boolean.TRUE, Optional.empty());
    }

    /**
     * Check whether a variable value is valid and warns about edge cases that are probably not wanted by the user.
     *
     * @param type the type of the variable.
     * @param value the value to check for
     *
     * @return a pair of {@link Optional}s with the left side being the successfully parsed value if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueString(final Type type,
        final String value) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(value);

        switch (type) {
            case STRING:
                return checkVariableValueStringToString(value);
            case INTEGER:
                return checkVariableValueStringToInt(value.trim());
            case LONG:
                return checkVariableValueStringToLong(value.trim());
            case DOUBLE:
                return checkVariableValueStringToDouble(value.trim());
            case BOOLEAN:
                return checkVariableValueStringToBoolean(value.trim());
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Checks whether a value is a valid string value.
     *
     * A string should not be blank (only consisting of white space)
     *
     * @param value the string value to check
     * @return a pair of {@link Optional}s with the left side being the successfully parsed string if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueStringToString(final String value) {
        return new Pair<>(Optional.of(value),
            value.trim().isEmpty() ? Optional.of("Value is blank") : Optional.empty());
    }

    /**
     * Checks whether a value is a valid integer value.
     *
     * An integer must not empty, be too big or small, or incorrectly formatted.
     *
     * @param value the string value to check
     * @return a pair of {@link Optional}s with the left side being the successfully parsed integer if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueStringToInt(final String value) {
        if (value.isEmpty()) {
            return new Pair<>(Optional.empty(), Optional.of("Value is empty"));
        }
        try {
            final BigInteger number = new BigInteger(value);
            if (number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                return new Pair<>(Optional.empty(), Optional.of("Value too small"));
            } else if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                return new Pair<>(Optional.empty(), Optional.of("Value too big"));
            }
            return new Pair<>(Optional.of(number.intValue()), Optional.empty());

        } catch (NumberFormatException e) {
            return new Pair<>(Optional.empty(), Optional.of("Value format incorrect"));
        }
    }

    /**
     * Checks whether a value is a valid long value.
     *
     * A long must not empty, be too big or small, or incorrectly formatted.
     *
     * @param value the string value to check
     * @return a pair of {@link Optional}s with the left side being the successfully parsed long if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueStringToLong(final String value) {
        if (value.isEmpty()) {
            return new Pair<>(Optional.empty(), Optional.of("Value is empty"));
        }
        try {
            final BigInteger number = new BigInteger(value);
            if (number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
                return new Pair<>(Optional.empty(), Optional.of("Value too small"));
            } else if (number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                return new Pair<>(Optional.empty(), Optional.of("Value too big"));
            }
            return new Pair<>(Optional.of(number.longValue()), Optional.empty());

        } catch (NumberFormatException e) {
            return new Pair<>(Optional.empty(), Optional.of("Value format incorrect"));
        }
    }

    /**
     * Checks whether a value is a valid double value.
     *
     * A double must not empty or incorrectly formatted. It should not evaluate of {@link Double#POSITIVE_INFINITY},
     * {@link Double#NEGATIVE_INFINITY} or {@link Double#NaN} if these values are not used as input.
     *
     * @param value the string value to check
     * @return a pair of {@link Optional}s with the left side being the successfully parsed double if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueStringToDouble(final String value) {
        if (value.isEmpty()) {
            return new Pair<>(Optional.empty(), Optional.of("Value is empty"));
        }
        try {
            final double number = Double.parseDouble(value);
            if ((Double.isNaN(number) && !value.equals("NaN"))
                || (Double.isInfinite(number) && !value.endsWith("Infinity"))) {
                return new Pair<>(Optional.of(number),
                    Optional.of("Value is effectively “" + Double.toString(number) + "“"));

            }

            return new Pair<>(Optional.of(number), Optional.empty());
        } catch (NumberFormatException e) {
            return new Pair<>(Optional.empty(), Optional.of("Value format incorrect"));
        }
    }

    /**
     * Checks whether a value is a valid boolean value.
     *
     * @param value the string value to check
     * @return a pair of {@link Optional}s with the left side being the successfully parsed boolean if present and the
     *         right side being a string describing errors (or remarks if the left side is present)
     */
    private static Pair<Optional<Object>, Optional<String>> checkVariableValueStringToBoolean(final String value) {
        return new Pair<>(Optional.of(Boolean.valueOf(value)),
            value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") ? Optional.empty()
                : Optional.of("Value is effectively “false”"));
    }

    /**
     * Check whether the given settings are correctly formatted.
     *
     * @param settings the settings to be checked
     * @throws InvalidSettingsException
     * @throws InvalidSettingsException if the settings could not be parsed
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO tableSettings = settings.getNodeSettings(SETTINGS_KEY);
        final String[] supportedTypes = tableSettings.getStringArray(SETTINGS_SUPPORTED_TYPES,
            Arrays.stream(Type.values()).map(Type::getStrRepresentation).toArray(String[]::new)); // backwards compatibility
        final String[] typeStrings = tableSettings.getStringArray(SETTINGS_COL_TYPE);
        final String[] nameStrings = tableSettings.getStringArray(SETTINGS_COL_NAME);
        final String[] valStrings = tableSettings.getStringArray(SETTINGS_COL_VAL);
        CheckUtils.checkSetting(typeStrings.length == nameStrings.length && nameStrings.length == valStrings.length,
            "The amount of variable types (%d), names (%d) and values (%d) must be equal!", typeStrings.length,
            nameStrings.length, valStrings.length);
        final Set<String> usedNames = new HashSet<>();

        for (int i = 0; i < supportedTypes.length; i++) {
            final Type type = Type.getTypeFromString(supportedTypes[i]);
            CheckUtils.checkSetting(type != null, "Type represenation \"%s\" (number %d) is not in supported types!",
                i + 1, supportedTypes[i]);
        }

        for (int i = 0; i < typeStrings.length; i++) {
            CheckUtils.checkSetting(ArrayUtils.contains(supportedTypes, typeStrings[i]),
                "Type representation \"%s\" for variable %d is not given in supported types!", typeStrings[i], i + 1);
            final Type type = Type.getTypeFromString(typeStrings[i]);
            final String name = nameStrings[i].trim();
            CheckUtils.checkSetting(!name.isEmpty(), "Please use a (non-empty) name for variable %d!", i + 1);
            CheckUtils.checkSetting(!usedNames.contains(name),
                "Please use a name that is not already used by another variable (%s) for variable %d!", name, i + 1);
            usedNames.add(name);

            final Pair<Optional<Object>, Optional<String>> checked = checkVariableValueString(type, valStrings[i]);
            CheckUtils.checkSetting(checked.getFirst().isPresent(), "Please check variable %d (%s) for errors: %s",
                i + 1, name, checked.getSecond().orElse("(Uknown error)"));
        }
    }

    /**
     * Write the variables to the specified settings.
     *
     * @param settings the settings to write to
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final NodeSettingsWO tableSettings = settings.addNodeSettings(SETTINGS_KEY);
        tableSettings.addStringArray(SETTINGS_SUPPORTED_TYPES, Arrays.stream(m_supportedTypes).filter(Objects::nonNull)
            .distinct().map(Type::getStrRepresentation).toArray(String[]::new));
        tableSettings.addStringArray(SETTINGS_COL_TYPE,
            m_variableTypes.stream().map(Type::getStrRepresentation).toArray(String[]::new));
        tableSettings.addStringArray(SETTINGS_COL_NAME, m_variableNames.toArray(new String[0]));
        tableSettings.addStringArray(SETTINGS_COL_VAL,
            m_variableValues.stream().map(Object::toString).toArray(String[]::new));
    }

    /**
     * Tries to load defined variables from the settings. These should be checked beforehand with
     * {@link #validateSettingsForModel(NodeSettingsRO)}.
     *
     * @param settings the settings to read from
     * @throws InvalidSettingsException if the settings could not be parsed
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO tableSettings = settings.getNodeSettings(SETTINGS_KEY);
        final String[] supportedTypes = tableSettings.getStringArray(SETTINGS_SUPPORTED_TYPES,
            Arrays.stream(Type.values()).map(Type::getStrRepresentation).toArray(String[]::new)); // backwards compatibility
        final String[] typeStrings = tableSettings.getStringArray(SETTINGS_COL_TYPE);
        final String[] nameStrings = tableSettings.getStringArray(SETTINGS_COL_NAME);
        final String[] valStrings = tableSettings.getStringArray(SETTINGS_COL_VAL);

        reset();

        m_supportedTypes = Arrays.stream(supportedTypes).map(Type::getTypeFromString).toArray(Type[]::new);
        CheckUtils.checkSetting(!ArrayUtils.contains(m_supportedTypes, null),
            "Supported types contain an unknown type representation!");

        for (int i = 0; i < typeStrings.length; i++) {
            CheckUtils.checkSetting(ArrayUtils.contains(supportedTypes, typeStrings[i]),
                "Type representation \"%s\" is not given in supported types!", typeStrings[i]);
            final Type type = Type.getTypeFromString(typeStrings[i]);
            if (type == null) {
                throw new InvalidSettingsException("Unknown type represenation: \"" + typeStrings[i] + "\"");
            }
            final Pair<Optional<Object>, Optional<String>> value = checkVariableValueString(type, valStrings[i]);
            final Optional<Object> parsedValue = value.getFirst();
            if (!parsedValue.isPresent()) {
                throw new InvalidSettingsException(
                    "Variable " + i + " malformed: " + value.getSecond().orElse("(Unknown error)"));
            }

            m_variableNames.add(nameStrings[i].trim());
            m_variableTypes.add(type);
            m_variableValues.add(parsedValue.get());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelVariables createClone() {
        return new SettingsModelVariables(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMITID_variables";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            final NodeSettingsRO tableSettings = settings.getNodeSettings(SETTINGS_KEY);
            final String[] supportedTypes = tableSettings.getStringArray(SETTINGS_SUPPORTED_TYPES,
                Arrays.stream(Type.values()).map(Type::getStrRepresentation).toArray(String[]::new)); // backwards compatibility
            final String[] typeStrings = tableSettings.getStringArray(SETTINGS_COL_TYPE);
            final String[] nameStrings = tableSettings.getStringArray(SETTINGS_COL_NAME);
            final String[] valStrings = tableSettings.getStringArray(SETTINGS_COL_VAL);
            final var variableNames = new ArrayList<String>();
            final var variableTypes = new ArrayList<Type>();
            final var variableValues = new ArrayList<Object>();

            final var uncheckedSupportedTypes =
                Arrays.stream(supportedTypes).map(Type::getTypeFromString).toArray(Type[]::new);
            if (ArrayUtils.contains(uncheckedSupportedTypes, null)) {
                return; // unknown type contained
            }

            m_supportedTypes = uncheckedSupportedTypes;

            for (int i = 0; i < typeStrings.length; i++) {
                final Type type = Type.getTypeFromString(typeStrings[i]);
                if (type == null) {
                    return; // type error
                }
                final Pair<Optional<Object>, Optional<String>> value = checkVariableValueString(type, valStrings[i]);
                final Optional<Object> parsedValue = value.getFirst();
                if (!parsedValue.isPresent()) {
                    return; // format error
                }

                variableNames.add(nameStrings[i].trim());
                variableTypes.add(type);
                variableValues.add(parsedValue.get());
            }

            m_variableNames = variableNames;
            m_variableTypes = variableTypes;
            m_variableValues = variableValues;

            notifyChangeListeners();
        } catch (InvalidSettingsException e) { // NOSONAR: In case of an error we do not update the state
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }
}
