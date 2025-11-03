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
 *   Oct 30, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.dialog.variables;

import java.util.Arrays;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.KeyValuePanel;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * A {@link AbstractTableModel} storing entries that eventually will be used to create {@link FlowVariable}s based on
 * {@link FSLocation}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class FSLocationVariableTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final int VARNAME_COL_IDX = 0;

    private static final int BASE_LOCATION_COL_IDX = 1;

    private static final int PATH_VALUE_COL_IDX = 2;

    private static final int PATH_EXTENSION_COL_IDX = 3;

    private static final String[] COL_NAMES = new String[]{"Variable name", "Base location", "Value", "File extension"};

    private final String m_cfgKey;

    private FSLocationVariables m_variables;

    private String m_pathBaseLocation;

    /**
     * Constructor.
     *
     * @param cfgKey the key of the sub-settings storing the individual entries
     */
    public FSLocationVariableTableModel(final String cfgKey) {
        m_cfgKey = cfgKey;
        initDefaults();
    }

    private void initDefaults() {
        m_variables = FSLocationVariables.empty();
    }

    /**
     * Returns the names of the variables.
     *
     * @return the names of the variables
     */
    public String[] getVarNames() {
        return m_variables.names();
    }

    /**
     * Returns the path value and extension separated by a "." if an extension has been defined.
     *
     * @return the locations
     */
    public String[] getLocations() {
        return IntStream.range(0, m_variables.paths().length)//
            .mapToObj(i -> m_variables.paths()[i] + getExtension(i))//
            .toArray(String[]::new);
    }

    private String getExtension(final int idx) {
        final String val = m_variables.extensions()[idx];
        if (!val.isEmpty() && !val.startsWith(".")) {
            return "." + val;
        } else {
            return val;
        }
    }

    /**
     * Sets the base location visualized in the corresponding column.
     *
     * @param baseLocation the base location
     */
    public void setPathBaseLocation(final String baseLocation) {
        m_pathBaseLocation = baseLocation;
        fireTableDataChanged();
    }

    /**
     * Validates the settings.
     *
     * @param settings the {@link NodeSettingsRO} storing the settings to validate
     * @param emptyAllowed if {@code true} an exception is thrown if no variables have been specified
     * @throws InvalidSettingsException - If the settings are invalid
     */
    public void validateSettingsForModel(final NodeSettingsRO settings, final boolean emptyAllowed)
        throws InvalidSettingsException {
        NodeSettingsRO varSettings = settings.getNodeSettings(m_cfgKey);
        FSLocationVariables.load(varSettings).validate(emptyAllowed);
    }

    /**
     * Loads the settings in the {@link NodeModel}.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @throws InvalidSettingsException - If the settings are invalid
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO varSettings = settings.getNodeSettings(m_cfgKey);
        m_variables = FSLocationVariables.load(varSettings);
    }

    /**
     * Loads the settings that were initially stored using the {@link KeyValuePanel}.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @param namesCfgKey the variables names config key
     * @param valuesCfgKey the variable values config key
     * @throws InvalidSettingsException - If the settings are invalid
     */
    public void loadBackwardsCompatible(final NodeSettingsRO settings, final String namesCfgKey,
        final String valuesCfgKey) throws InvalidSettingsException {
        try {
            m_variables = FSLocationVariables.loadBackwardsCompatible(settings, namesCfgKey, valuesCfgKey);
        } catch (final InvalidSettingsException e) { //NOSONAR
            throw new InvalidSettingsException(String.format("Config for key \"%s\" not found.", m_cfgKey), e);
        }
    }

    /**
     * Saves the settings in the {@link NodeModel}.
     *
     * @param settings the {@link NodeSettingsWO} to write to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        NodeSettingsWO varSettings = settings.addNodeSettings(m_cfgKey);
        m_variables.save(varSettings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        NodeSettingsRO varSettings;
        try {
            varSettings = settings.getNodeSettings(m_cfgKey);
            m_variables = FSLocationVariables.load(varSettings);
        } catch (InvalidSettingsException e) { //NOSONAR
            initDefaults();
        }
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettingsForModel(settings);
    }

    void addRow() {
        m_variables = m_variables.add("", "", "");
        fireTableRowsInserted(getRowCount(), getRowCount());
    }

    void removeRows(final int... toRemove) {
        m_variables = m_variables.remove(toRemove);
        int firstRow = Arrays.stream(toRemove).min().getAsInt();
        int lastRow = Arrays.stream(toRemove).max().getAsInt();
        fireTableRowsDeleted(firstRow, lastRow);
    }

    @Override
    public int getRowCount() {
        return m_variables.count();
    }

    @Override
    public int getColumnCount() {
        return COL_NAMES.length;
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex != BASE_LOCATION_COL_IDX;
    }

    @Override
    public String getValueAt(final int rowIndex, final int columnIndex) {
        if (columnIndex == VARNAME_COL_IDX) {
            return m_variables.names()[rowIndex];
        } else if (columnIndex == BASE_LOCATION_COL_IDX) {
            return m_pathBaseLocation;
        } else if (columnIndex == PATH_VALUE_COL_IDX) {
            return m_variables.paths()[rowIndex];
        } else if (columnIndex == PATH_EXTENSION_COL_IDX) {
            return m_variables.extensions()[rowIndex];
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final String val = (String)aValue;
        if (!val.equals(getValueAt(rowIndex, columnIndex))) {
            if (columnIndex == VARNAME_COL_IDX) {
                m_variables.names()[rowIndex] = val;
            } else if (columnIndex == BASE_LOCATION_COL_IDX) {
                throw new IllegalArgumentException("Can't set the reorder column.");
            } else if (columnIndex == PATH_VALUE_COL_IDX) {
                m_variables.paths()[rowIndex] = val;
            } else if (columnIndex == PATH_EXTENSION_COL_IDX) {
                m_variables.extensions()[rowIndex] = val;
            } else {
                throw new IndexOutOfBoundsException();
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (columnIndex < getColumnCount()) {
            return String.class;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public String getColumnName(final int columnIndex) {
        return COL_NAMES[columnIndex];
    }

    /**
     * Sets the given values.
     *
     * @param varNames the variable names
     * @param varValues the variable values
     * @param fileExtension the file extensions
     */
    public void setEntries(final String[] varNames, final String[] varValues, final String[] fileExtension) {
        m_variables = new FSLocationVariables(varNames, varValues, fileExtension);
    }

}
