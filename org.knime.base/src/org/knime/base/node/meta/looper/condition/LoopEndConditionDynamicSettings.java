/*
 * ------------------------------------------------------------------------
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
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import java.util.Arrays;
import java.util.function.IntPredicate;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableTypeRegistry;

/**
 * This class holds the settings for the condition loop tail node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndConditionSettings} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopEndConditionDynamicSettings {
    /** All comparison operators the user can choose from in the dialog. */
    enum Operator {
            /** Numeric greater than. */
            GT(">", i -> i > 0),
            /** Numeric lower than. */
            LT("<", i -> i < 0),
            /** Numeric greater than or equal. */
            GE(">=", i -> i >= 0),
            /** Numeric lower than or equal. */
            LE("<=", i -> i <= 0),
            /** Numeric or string unequal. */
            NE("!=", i -> i != 0),
            /** Numeric or string equal. */
            EQ("=", i -> i == 0);

        private final String m_represent;
        private final IntPredicate m_operation;

        private Operator(final String represent, final IntPredicate operation) {
            m_represent = represent;
            m_operation = operation;
        }

        private Operator() {
            m_represent = null;
            m_operation = null;
        }

        <T extends Comparable<T>> boolean test(final T left, final T right) {
            return m_operation.test(left.compareTo(right));
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (m_represent != null) {
                return m_represent;
            }
            return super.toString();
        }
    }

    private final int m_numberofPorts;

    private String m_variableName;

    private VariableType<?> m_variableType;

    private Operator m_operator;

    private String m_value;

    private boolean[] m_addLastRows;

    private boolean[] m_addLastRowsOnly;

    private boolean[] m_addIterationColumn;

    /** @since 4.4 */
    private boolean m_propagateLoopVariables = false;

    LoopEndConditionDynamicSettings(final int numberOfPorts) {
        m_numberofPorts = numberOfPorts;
        m_addLastRows = new boolean[numberOfPorts];
        m_addLastRowsOnly = new boolean[numberOfPorts];
        m_addIterationColumn = new boolean[numberOfPorts];

        Arrays.fill(m_addLastRows, true);
        Arrays.fill(m_addIterationColumn, true);
    }

    /**
     * Returns if the rows from the loop's last iteration should be added to the output table or not.
     *
     * @param index the index of the table
     * @return <code>true</code> if the rows should be added, <code>false</code> otherwise
     */
    public boolean addLastRows(final int index) {
        return m_addLastRows[index];
    }

    /**
     * Sets if only the rows from the loop's last iteration should be added to the output table or not.
     *
     * @param index the index of the table
     * @param b <code>true</code> if the only the last rows should be added, <code>false</code> otherwise
     */
    public void addLastRowsOnly(final int index, final boolean b) {
        m_addLastRowsOnly[index] = b;
    }

    /**
     * Returns if only the rows from the loop's last iteration should be added to the output table or not.
     *
     * @param index the index of the table
     * @return <code>true</code> if the only the last rows should be added, <code>false</code> otherwise
     */
    public boolean addLastRowsOnly(final int index) {
        return m_addLastRowsOnly[index];
    }

    /**
     * Sets if the rows from the loop's last iteration should be added to the output table or not.
     *
     * @param index the index of the table
     * @param b <code>true</code> if the rows should be added, <code>false</code> otherwise
     */
    public void addLastRows(final int index, final boolean b) {
        m_addLastRows[index] = b;
    }

    /**
     * Sets if a column containing the iteration number should be appended to the output table.
     *
     * @param index the index of the table
     * @param add <code>true</code> if a column should be added, <code>false</code> otherwise
     */
    public void addIterationColumn(final int index, final boolean add) {
        m_addIterationColumn[index] = add;
    }

    /**
     * Returns if a column containing the iteration number should be appended to the output table.
     *
     * @param index the index of the table
     * @return <code>true</code> if a column should be added, <code>false</code> otherwise
     */
    public boolean addIterationColumn(final int index) {
        return m_addIterationColumn[index];
    }

    /**
     * Returns the flow variable's name which is checked in each iteration.
     *
     * @return the variable's name
     */
    public String variableName() {
        return m_variableName;
    }

    /**
     * Returns the flow variable's type which is checked in each iteration.
     *
     * @return the variable's type
     */
    public VariableType<?> variableType() {//NOSONAR: no other way to return this
        return m_variableType;
    }

    /**
     * Sets the flow variable which is checked in each iteration.
     *
     * @param variable the flow variable
     */
    public void variable(final FlowVariable variable) {
        if (variable == null) {
            m_variableName = null;
            m_variableType = null;
        } else {
            m_variableName = variable.getName();
            m_variableType = variable.getVariableType();
        }
    }

    /**
     * Returns the operator that should be used in the comparison between variable and value.
     *
     * @return the comparison operator
     */
    public Operator operator() {
        return m_operator;
    }

    /**
     * Sets the operator that should be used in the comparison between variable and value.
     *
     * @param op the comparison operator
     */
    public void operator(final Operator op) {
        m_operator = op;
    }

    /**
     * Returns the value the flow variable should be compared with. It can either be a number or a string, depending on
     * the flow variables type.
     *
     * @return the value
     */
    public String value() {
        return m_value;
    }

    /**
     * Sets the value the flow variable should be compared with. It can either be a number or a string, depending on the
     * flow variables type.
     *
     * @param s the value
     */
    public void value(final String s) {
        m_value = s;
    }

    /**
     * Whether to propagate modification of variables in subsequent loop iterations and in the output of the end node.
     *
     * @return the propagateLoopVariables that property.
     * @since 4.4
     */
    public boolean propagateLoopVariables() {
        return m_propagateLoopVariables;
    }

    /**
     * Set the {@link #propagateLoopVariables()} property.
     *
     * @param value the propagateLoopVariables to set
     * @since 4.4
     */
    public void propagateLoopVariables(final boolean value) {
        m_propagateLoopVariables = value;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_variableName = settings.getString("variableName");
        final var type = settings.getString("variableType");
        if (type == null) {
            m_variableType = null;
        } else {
            m_variableType = Arrays.stream(VariableTypeRegistry.getInstance().getAllTypes())//
                .filter(t -> type.equals(t.getIdentifier()))//
                .findAny()//
                .orElseThrow(() -> new InvalidSettingsException("Unknown variable type: " + type));
        }
        m_value = settings.getString("value");

        final var op = settings.getString("operator");
        if (op == null) {
            m_operator = null;
        } else {
            m_operator = Operator.valueOf(op);
        }

        m_addLastRows = read(settings, "addLastRows", true);
        m_addLastRowsOnly = read(settings, "addLastRowsOnly", false);
        m_addIterationColumn = readWithDefaults(settings, "addIterationColumn", true);
        m_propagateLoopVariables = settings.getBoolean("propagateLoopVariables", false);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_variableName = settings.getString("variableName", null);
        m_value = settings.getString("value", "");

        final var type = settings.getString("variableType", null);
        if (type == null) {
            m_variableType = null;
        } else {
            m_variableType = Arrays.stream(VariableTypeRegistry.getInstance().getAllTypes())//
                .filter(t -> type.equals(t.getIdentifier()))//
                .findAny()//
                .orElse(null);
        }

        final var op = settings.getString("operator", Operator.EQ.name());
        if (op == null) {
            m_operator = null;
        } else {
            try {
                m_operator = Operator.valueOf(op);
            } catch (IllegalArgumentException e) {// NOSONAR: silently load default
                m_operator = null;
            }
        }

        m_addLastRows = readWithDefaults(settings, "addLastRows", true);
        m_addLastRowsOnly = readWithDefaults(settings, "addLastRowsOnly", false);
        m_addIterationColumn = readWithDefaults(settings, "addIterationColumn", true);
        m_propagateLoopVariables = settings.getBoolean("propagateLoopVariables", false); // added in 4.4
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("variableName", m_variableName);
        if (m_variableType == null) {
            settings.addString("variableType", null);
        } else {
            settings.addString("variableType", m_variableType.getIdentifier());
        }
        settings.addString("value", m_value);
        if (m_operator == null) {
            settings.addString("operator", null);
        } else {
            settings.addString("operator", m_operator.name());
        }
        settings.addBooleanArray("addLastRows", m_addLastRows);
        settings.addBooleanArray("addLastRowsOnly", m_addLastRowsOnly);
        settings.addBooleanArray("addIterationColumn", m_addIterationColumn);
        settings.addBoolean("propagateLoopVariables", m_propagateLoopVariables);
    }

    private boolean[] read(final NodeSettingsRO settings, final String key, final boolean def)
        throws InvalidSettingsException {
        final var result = new boolean[m_numberofPorts];
        Arrays.fill(result, def);
        final var read = settings.getBooleanArray(key);
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_numberofPorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }

    private boolean[] readWithDefaults(final NodeSettingsRO settings, final String key, final boolean def) {
        final var result = new boolean[m_numberofPorts];
        Arrays.fill(result, def);
        final var read = settings.getBooleanArray(key, new boolean[0]); // NOSONAR: has to call the varargs
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_numberofPorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }
}
