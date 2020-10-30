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
package org.knime.base.node.io.filehandling.util.dialogs.variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
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

    private static final String CFG_VAR_NAMES = "variable_names";

    private static final String CFG_VAR_PATH_VALUE = "path_values";

    private static final String CFG_VAR_PATH_EXTENSION = "file_extensions";

    private static final int VARNAME_COL_IDX = 0;

    private static final int BASE_LOCATION_COL_IDX = 1;

    private static final int PATH_VALUE_COL_IDX = 2;

    private static final int PATH_EXTENSION_COL_IDX = 3;

    private static final String[] COL_NAMES = new String[]{"Variable name", "Base location", "Value", "File extension"};

    private final String m_cfgKey;

    private List<String> m_varNames;

    private List<String> m_varPathValues;

    private List<String> m_varFileExtension;

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

    /**
     * Constructor that already creates a single entry.
     *
     * @param cfgKey the key of the sub-settings storing the individual entries
     * @param varName the variable name of the first entry
     * @param pathValue the path value of the first entry
     * @param fileExtension the file extension of the first entry
     */
    public FSLocationVariableTableModel(final String cfgKey, final String varName, final String pathValue,
        final String fileExtension) {
        this(cfgKey);
        m_varNames.add(varName);
        m_varPathValues.add(pathValue);
        m_varFileExtension.add(fileExtension);
    }

    private void initDefaults() {
        m_varNames = new ArrayList<>();
        m_varPathValues = new ArrayList<>();
        m_varFileExtension = new ArrayList<>();
    }

    /**
     * Returns the names of the variables.
     *
     * @return the names of the variables
     */
    public String[] getVarNames() {
        return m_varNames.toArray(new String[0]);
    }

    /**
     * Returns the path value and extension separated by a "." if an extension has been defined.
     *
     * @return the locations
     */
    public String[] getLocations() {
        return IntStream.range(0, m_varPathValues.size())//
            .mapToObj(i -> m_varPathValues.get(i) + getExtension(i))//
            .toArray(String[]::new);
    }

    private String getExtension(final int idx) {
        final String val = m_varFileExtension.get(idx);
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
        final String[] varNames = varSettings.getStringArray(CFG_VAR_NAMES);
        String[] varBody = varSettings.getStringArray(CFG_VAR_PATH_VALUE);
        String[] varExtension = varSettings.getStringArray(CFG_VAR_PATH_EXTENSION);
        for (final String varName : varNames) {
            CheckUtils.checkSetting(!varName.isEmpty(), "Please assign names to each flow variable to be created");
        }
        if (!emptyAllowed) {
            CheckUtils.checkSetting(varNames.length > 0, "Please specify at least one variable to be created");
        }
        CheckUtils.checkSetting(varNames.length * 2 == varBody.length + varExtension.length,
            "The number of variable names, body and extensions must be equal");
    }

    /**
     * Loads the settings in the {@link NodeModel}.
     *
     * @param settings the {@link NodeSettingsRO} to read from
     * @throws InvalidSettingsException - If the settings are invalid
     */
    public void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO varSettings = settings.getNodeSettings(m_cfgKey);
        m_varNames = Arrays.asList(varSettings.getStringArray(CFG_VAR_NAMES));
        m_varPathValues = Arrays.asList(varSettings.getStringArray(CFG_VAR_PATH_VALUE));
        m_varFileExtension = Arrays.asList(varSettings.getStringArray(CFG_VAR_PATH_EXTENSION));
    }

    /**
     * Saves the settings in the {@link NodeModel}.
     *
     * @param settings the {@link NodeSettingsWO} to write to
     */
    public void saveSettingsForModel(final NodeSettingsWO settings) {
        NodeSettingsWO varSettings = settings.addNodeSettings(m_cfgKey);
        varSettings.addStringArray(CFG_VAR_NAMES, m_varNames.toArray(new String[0]));
        varSettings.addStringArray(CFG_VAR_PATH_VALUE, m_varPathValues.toArray(new String[0]));
        varSettings.addStringArray(CFG_VAR_PATH_EXTENSION, m_varFileExtension.toArray(new String[0]));
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        NodeSettingsRO varSettings;
        try {
            varSettings = settings.getNodeSettings(m_cfgKey);
            m_varNames = new ArrayList<>(Arrays.asList(varSettings.getStringArray(CFG_VAR_NAMES)));
            m_varPathValues = new ArrayList<>(Arrays.asList(varSettings.getStringArray(CFG_VAR_PATH_VALUE)));
            m_varFileExtension = new ArrayList<>(Arrays.asList(varSettings.getStringArray(CFG_VAR_PATH_EXTENSION)));
        } catch (InvalidSettingsException e) { //NOSONAR
            initDefaults();
        }
    }

    void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveSettingsForModel(settings);
    }

    void addRow() {
        m_varNames.add("");
        m_varPathValues.add("");
        m_varFileExtension.add("");
        fireTableRowsInserted(getRowCount(), getRowCount());
    }

    void removeRows(final int... toRemove) {
        Arrays.sort(toRemove);
        final int lastIdx = toRemove.length - 1;
        for (int i = lastIdx; i >= 0; i--) {
            final int rowIndex = toRemove[i];
            m_varNames.remove(rowIndex);
            m_varPathValues.remove(rowIndex);
            m_varFileExtension.remove(rowIndex);
        }
        fireTableRowsDeleted(toRemove[0], toRemove[lastIdx]);
    }

    @Override
    public int getRowCount() {
        return m_varNames.size();
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
            return m_varNames.get(rowIndex);
        } else if (columnIndex == BASE_LOCATION_COL_IDX) {
            return m_pathBaseLocation;
        } else if (columnIndex == PATH_VALUE_COL_IDX) {
            return m_varPathValues.get(rowIndex);
        } else if (columnIndex == PATH_EXTENSION_COL_IDX) {
            return m_varFileExtension.get(rowIndex);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        final String val = (String)aValue;
        if (!val.equals(getValueAt(rowIndex, columnIndex))) {
            if (columnIndex == VARNAME_COL_IDX) {
                m_varNames.set(rowIndex, val);
            } else if (columnIndex == BASE_LOCATION_COL_IDX) {
                throw new IllegalArgumentException("Can't set the reorder column.");
            } else if (columnIndex == PATH_VALUE_COL_IDX) {
                m_varPathValues.set(rowIndex, val);
            } else if (columnIndex == PATH_EXTENSION_COL_IDX) {
                m_varFileExtension.set(rowIndex, val);
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
}
