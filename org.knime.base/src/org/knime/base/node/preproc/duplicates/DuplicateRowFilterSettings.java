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
 *   May 27, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.duplicates;

import static org.knime.base.node.preproc.duplicates.DuplicateRowFilterDialogSettings.DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY;
import static org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.validateColumnName;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;

/**
 * The duplicate row filter node settings.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class DuplicateRowFilterSettings {

    enum RowSelectionType {

            FIRST("First", false),

            LAST("Last", false),

            MINIMUM("Minimum of", true),

            MAXIMUM("Maximum of", true);

        static final String ROW_SELECTION_KEY = "row_selection";

        private final String m_uiName;

        private final boolean m_supportsRefCol;

        private RowSelectionType(final String uiName, final boolean supportsRefCol) {
            m_uiName = uiName;
            m_supportsRefCol = supportsRefCol;
        }

        boolean supportsRefCol() {
            return m_supportsRefCol;
        }

        void saveSettingsTo(final NodeSettingsWO settings) {
            settings.addString(ROW_SELECTION_KEY, name());
        }

        static RowSelectionType loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            try {
                final var selectionString = settings.getString(ROW_SELECTION_KEY);
                return valueOf(selectionString != null ? selectionString : "<not found>");
            } catch (final IllegalArgumentException e) {
                throw new InvalidSettingsException(
                    "Could not read the row selection type (\"chosen\" rows) from the dialog settings. "
                        + "Make sure it is set.",
                    e);
            }
        }

        @Override
        public String toString() {
            return m_uiName;
        }
    }

    /** The group columns config key. */
    static final String GROUP_COLS_KEY = "group_cols";

    /** The in-memory config key. */
    static final String IN_MEMORY_KEY = "in_memory";

    /** The retain order config key. */
    static final String RETAIN_ROW_ORDER_KEY = "retain_order";

    /** The remove duplicate row keys config key. */
    static final String REMOVE_DUPLICATE_ROWS_KEY = "remove_duplicates";

    /** The add row duplicate flag config key. */
    static final String ADD_ROW_DUPLICATE_FLAG_KEY = "add_row_duplicate_flag";

    /** The column name of row duplicates flag config key. */
    static final String UNIQUE_FLAG_COLUMN_NAME_KEY = "unique_flag_column_name";

    /** The add row id flag config key/ */
    static final String ADD_ROW_ID_FLAG_KEY = "add_row_id_flag";

    /** The column name of row id flag config key. */
    static final String ROW_ID_FLAG_COLUMN_NAME_KEY = "row_id_flag_column_name";

    static final String DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY_INTERNAL =
        DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY + SettingsModel.CFGKEY_INTERNAL;

    static final String REFERENCE_COL_KEY = "reference_col";

    static final String UPDATE_DOMAINS_KEY = "update_domains";

    /** Settings model storing the selected group columns. */
    private final SettingsModelColumnFilter2 m_groupCols = new SettingsModelColumnFilter2(GROUP_COLS_KEY);

    /** Settings model storing the in-memory flag. */
    private final SettingsModelBoolean m_inMemory = new SettingsModelBoolean(IN_MEMORY_KEY, false);

    /** Settings model storing the retain row order flag. */
    private final SettingsModelBoolean m_retainOrder = new SettingsModelBoolean(RETAIN_ROW_ORDER_KEY, true);

    /** Settings model storing the remove duplicate rows flag. */
    private final SettingsModelBoolean m_removeDuplicates = new SettingsModelBoolean(REMOVE_DUPLICATE_ROWS_KEY, true);

    /** Settings model storing the add duplicate row identifier flag. */
    private final SettingsModelBoolean m_addUniqueLabel = new SettingsModelBoolean(ADD_ROW_DUPLICATE_FLAG_KEY, true);

    /** Settings model storing the column name of the duplicate row status. */
    private final SettingsModelString m_uniqueStatusColumnName =
        new SettingsModelString(UNIQUE_FLAG_COLUMN_NAME_KEY, "Duplicate Status");

    /** Settings model storing the add row id flag. */
    private final SettingsModelBoolean m_addRowLabel = new SettingsModelBoolean(ADD_ROW_ID_FLAG_KEY, false);

    /** Settings model storing the column name of the chosen row ids. */
    private final SettingsModelString m_chosenRowIdsColumnName =
        new SettingsModelString(ROW_ID_FLAG_COLUMN_NAME_KEY, "Duplicate Chosen");

    /** Settings model storing the reference column name. */
    private final SettingsModelString m_referenceCol = new SettingsModelString(REFERENCE_COL_KEY, null);

    /** If domains should be updated. This element is only shown in the modern UI, defaults to "false" otherwise. */
    private final SettingsModelBoolean m_updateDomains = new SettingsModelBoolean(UPDATE_DOMAINS_KEY, false);

    private RowSelectionType m_rowSelectionType = RowSelectionType.FIRST;

    SettingsModelColumnFilter2 getGroupColsModel() {
        return m_groupCols;
    }

    SettingsModelBoolean getInMemoryModel() {
        return m_inMemory;
    }

    SettingsModelBoolean getRetainOrderModel() {
        return m_retainOrder;
    }

    SettingsModelBoolean getRemoveDuplicatesModel() {
        return m_removeDuplicates;
    }

    SettingsModelBoolean getAddUniqueLblModel() {
        return m_addUniqueLabel;
    }

    SettingsModelBoolean getAddRowLblModel() {
        return m_addRowLabel;
    }

    SettingsModelString getReferenceColModel() {
        return m_referenceCol;
    }

    void setRowSelectionType(final RowSelectionType type) {
        m_rowSelectionType = type;
    }

    FilterResult getGroupCols(final DataTableSpec spec) {
        return m_groupCols.applyTo(spec);
    }

    boolean inMemory() {
        return m_inMemory.getBooleanValue();
    }

    boolean retainOrder() {
        return m_retainOrder.getBooleanValue();
    }

    boolean removeDuplicates() {
        return m_removeDuplicates.getBooleanValue();
    }

    boolean addUniqueLabel() {
        return m_addUniqueLabel.getBooleanValue();
    }

    String getUniqueStatusColumnName() {
        return m_uniqueStatusColumnName.getStringValue();
    }

    boolean addRowLabel() {
        return m_addRowLabel.getBooleanValue();
    }

    String getChosenRowIdsColumnName() {
        return m_chosenRowIdsColumnName.getStringValue();
    }

    String getReferenceCol() {
        return m_referenceCol.getStringValue();
    }

    RowSelectionType getRowSelectionType() {
        return m_rowSelectionType;
    }

    boolean updateDomains() {
        return m_updateDomains.getBooleanValue();
    }

    /**
     * @param settings
     */
    void saveSettingsForDialog(final NodeSettingsWO settings) {
        m_removeDuplicates.saveSettingsTo(settings);
        m_rowSelectionType.saveSettingsTo(settings);
        m_referenceCol.saveSettingsTo(settings);
    }

    /**
     * Saves the settings
     *
     * @param settings settings to save to
     */
    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_groupCols.saveSettingsTo(settings);
        m_retainOrder.saveSettingsTo(settings);
        m_addUniqueLabel.saveSettingsTo(settings);
        m_uniqueStatusColumnName.saveSettingsTo(settings);
        m_addRowLabel.saveSettingsTo(settings);
        m_chosenRowIdsColumnName.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);
        saveSettingsForDialog(settings);
        m_updateDomains.saveSettingsTo(settings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeDuplicates.loadSettingsFrom(settings);
        m_rowSelectionType = RowSelectionType.loadSettingsFrom(settings);
        m_referenceCol.loadSettingsFrom(settings);
    }

    /**
     * Loads the validated settings
     *
     * @param settings the settings to load
     * @throws InvalidSettingsException - If loading failed
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_groupCols.loadSettingsFrom(settings);
        m_retainOrder.loadSettingsFrom(settings);
        m_addUniqueLabel.loadSettingsFrom(settings);
        if (settings.containsKey(UNIQUE_FLAG_COLUMN_NAME_KEY)) {
            // Added in 5.2, defaults to old column name
            m_uniqueStatusColumnName.loadSettingsFrom(settings);

        } else {
            m_uniqueStatusColumnName.setStringValue("duplicate-type-classifier");
        }
        m_addRowLabel.loadSettingsFrom(settings);
        if (settings.containsKey(ROW_ID_FLAG_COLUMN_NAME_KEY)) {
            // Added in 5.2, defaults to old column name
            m_chosenRowIdsColumnName.loadSettingsFrom(settings);
        } else {
            m_chosenRowIdsColumnName.setStringValue("duplicate-row-identifier");
        }
        m_inMemory.loadSettingsFrom(settings);
        loadSettingsForDialog(settings);

        if (settings.containsKey(UPDATE_DOMAINS_KEY)) {
            // Added in 5.0, defaults to false
            m_updateDomains.loadSettingsFrom(settings);
        }
    }

    /**
     * Validates the settings.
     *
     * @param settings the settings to validate
     * @throws InvalidSettingsException - If the validation failed
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_groupCols.validateSettings(settings);
        m_retainOrder.validateSettings(settings);
        m_removeDuplicates.validateSettings(settings);
        m_addUniqueLabel.validateSettings(settings);
        validateColumnNameSetting(settings, m_uniqueStatusColumnName, UNIQUE_FLAG_COLUMN_NAME_KEY,
            "row status column name");
        m_addRowLabel.validateSettings(settings);
        validateColumnNameSetting(settings, m_chosenRowIdsColumnName, ROW_ID_FLAG_COLUMN_NAME_KEY,
            "chosen RowIDs column name");
        m_referenceCol.validateSettings(settings);
        m_inMemory.validateSettings(settings);
        if (settings.containsKey(UPDATE_DOMAINS_KEY)) {
            // Added in 5.0
            m_updateDomains.validateSettings(settings);
        }
        final var keepDuplicates = !settings.getBoolean(REMOVE_DUPLICATE_ROWS_KEY);
        final var addRowStatusColumn = settings.getBoolean(ADD_ROW_DUPLICATE_FLAG_KEY);
        final var addChosenRowIDColumn = settings.getBoolean(ADD_ROW_ID_FLAG_KEY);
        if (keepDuplicates && !addRowStatusColumn && !addChosenRowIDColumn) {
            throw new InvalidSettingsException(
                "'Keep duplicate rows' requires that at least one of the two 'Add column...' options is checked.");
        }
    }

    private static void validateColumnNameSetting(final NodeSettingsRO settings, final SettingsModelString setting,
        final String settingConfigKey, final String settingIdentifier) throws InvalidSettingsException {
        if (settings.containsKey(settingConfigKey)) {
            if (settings.getBoolean(DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY_INTERNAL, false)) {
                // Added in 5.5
                final var invalidColNameToErrorMessage =
                    new ColumnNameValidationMessageBuilder(settingIdentifier).build();
                validateColumnName(settings.getString(settingConfigKey), invalidColNameToErrorMessage);
            } else {
                // Added in 5.2
                setting.validateSettings(settings);
            }
        }
    }
}
