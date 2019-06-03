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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class DuplicateFilterSettings {

    enum RowSelectionType {

            FIRST("First", false),

            LAST("Last", false),

            MINIMUM("Minimum of", true),

            MAXIMUM("Maximum of", true);

        private static final String ROW_SELECTION_KEY = "row_selection";

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
                return valueOf(settings.getString(ROW_SELECTION_KEY));
            } catch (final IllegalArgumentException | NullPointerException e) {
                throw new InvalidSettingsException(e.getMessage(), e);
            }
        }

        @Override
        public String toString() {
            return m_uiName;
        }
    }

    /** The group columns config key. */
    private static final String GROUP_COLS_KEY = "group_cols";

    /** The in-memory config key. */
    private static final String IN_MEMORY_KEY = "in_memory";

    /** The retain order config key. */
    private static final String RETAIN_ROW_ORDER_KEY = "retain_order";

    /** The remove duplicate row keys config key. */
    private static final String REMOVE_DUPLICATE_ROWS_KEY = "remove_duplicates";

    /** The add row duplicate flag config key. */
    private static final String ADD_ROW_DUPLICATE_FLAG_KEY = "add_row_duplicate_flag";

    /** The add row id flag config key/ */
    private static final String ADD_ROW_ID_FLAG_KEY = "add_row_id_flag";

    private static final String REFERENCE_COL_KEY = "reference_col";

    /** Settings model storing the selected group columns. */
    private final SettingsModelColumnFilter2 m_groupCols = new SettingsModelColumnFilter2(GROUP_COLS_KEY);

    /** Settings model storing the in-memory flag. */
    private final SettingsModelBoolean m_inMemory = new SettingsModelBoolean(IN_MEMORY_KEY, false);

    /** Settings model storing the retain row order flag. */
    private final SettingsModelBoolean m_retainOrder = new SettingsModelBoolean(RETAIN_ROW_ORDER_KEY, false);

    /** Settings model storing the remove duplicate rows flag. */
    private final SettingsModelBoolean m_removeDuplicates = new SettingsModelBoolean(REMOVE_DUPLICATE_ROWS_KEY, true);

    /** Settings model storing the add duplicate row identifier flag. */
    private final SettingsModelBoolean m_addUniqueLabel = new SettingsModelBoolean(ADD_ROW_DUPLICATE_FLAG_KEY, true);

    /** Settings model storing the add row id flag. */
    private final SettingsModelBoolean m_addRowLabel = new SettingsModelBoolean(ADD_ROW_ID_FLAG_KEY, false);

    /** Settings model storing the reference column name. */
    private final SettingsModelString m_referenceCol = new SettingsModelString(REFERENCE_COL_KEY, null);

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

    boolean addRowLabel() {
        return m_addRowLabel.getBooleanValue();
    }

    String getReferenceCol() {
        return m_referenceCol.getStringValue();
    }

    RowSelectionType getRowSelectionType() {
        return m_rowSelectionType;
    }

    /**
     * @param settings
     */
    void saveSettingsForDialog(final NodeSettingsWO settings) {
        m_removeDuplicates.saveSettingsTo(settings);
        m_rowSelectionType.saveSettingsTo(settings);
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
        m_addRowLabel.saveSettingsTo(settings);
        m_referenceCol.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);
        saveSettingsForDialog(settings);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_removeDuplicates.loadSettingsFrom(settings);
        m_rowSelectionType = RowSelectionType.loadSettingsFrom(settings);
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
        m_addRowLabel.loadSettingsFrom(settings);
        m_referenceCol.loadSettingsFrom(settings);
        m_inMemory.loadSettingsFrom(settings);
        loadSettingsForDialog(settings);
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
        m_addRowLabel.validateSettings(settings);
        m_referenceCol.validateSettings(settings);
        m_inMemory.validateSettings(settings);
    }

}
