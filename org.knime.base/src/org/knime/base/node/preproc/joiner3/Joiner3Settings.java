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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner3;

import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the joiner node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
class Joiner3Settings {

    private static final String JOIN_MODE = "joinMode";

    private static final String LEFT_JOINING_COLUMNS = "leftTableJoinPredicate";

    private static final String RIGHT_JOINING_COLUMNS = "rightTableJoinPredicate";

    private static final String COMPOSITION_MODE = "compositionMode";

    private static final String MERGE_JOIN_COLUMNS = "mergeJoinColumns";

    private static final String OUTPUT_UNMATCHED_ROWS_TO_SEPARATE_PORTS = "outputUnmatchedRowsToSeparatePorts";

    private static final String ASSIGN_NEW_ROW_KEYS = "assignNewRowKeys";

    private static final String ROW_KEY_SEPARATOR = "rowKeySeparator";

    private static final String DUPLICATE_COLUMN_HANDLING = "duplicateHandling";

    private static final String DUPLICATE_COLUMN_SUFFIX = "suffix";

    private static final String LEFT_COL_SELECT_CONFIG = "leftColumnSelectionConfig";

    private static final String RIGHT_COL_SELECT_CONFIG = "rightColumnSelectionConfig";

    private static final String OUTPUT_ROW_ORDER = "outputRowOrder";

    private static final String MAX_OPEN_FILES = "maxOpenFiles";

    private static final String HILITING_ENABLED = "enableHiliting";

    /**
     * This enum holds all ways of handling duplicate column names in the two input tables.
     */
    enum ColumnNameDisambiguation {
            /** Append a custom suffix to the columns from the second table. */
            APPEND_SUFFIX,
            /** Don't execute the node. */
            DO_NOT_EXECUTE, APPEND_SUFFIX_AUTOMATIC;
    }

    /**
     * This enum holds all ways of joining the two tables.
     */
    enum JoinMode {
            INNER("Inner join", true, false, false), LEFT_OUTER("Left outer join", true, true, false),
            RIGHT_OUTER("Right outer join", true, false, true), FULL_OUTER("Full outer join", true, true, true),
            LEFT_ANTI("Left antijoin", false, true, false), RIGHT_ANTI("Right antijoin", false, false, true),
            FULL_ANTI("Full antijoin", false, true, true), EMPTY("No output rows", false, false, false);

        private final String m_uiDisplayText;

        private final boolean m_includeMatchingRows;

        private final boolean m_includeLeftUnmatchedRows;

        private final boolean m_includeRightUnmatchedRows;

        private JoinMode(final String uiDisplayText, final boolean includeMatchingRows,
            final boolean includeLeftUnmatchedRows, final boolean includeRightUnmatchedRows) {
            m_uiDisplayText = uiDisplayText;
            m_includeMatchingRows = includeMatchingRows;
            m_includeLeftUnmatchedRows = includeLeftUnmatchedRows;
            m_includeRightUnmatchedRows = includeRightUnmatchedRows;
        }

        boolean isIncludeMatchingRows() {
            return m_includeMatchingRows;
        }

        boolean isIncludeLeftUnmatchedRows() {
            return m_includeLeftUnmatchedRows;
        }

        boolean isIncludeRightUnmatchedRows() {
            return m_includeRightUnmatchedRows;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_uiDisplayText;
        }

    }

    /**
     * This enum holds all ways how join attributes can be combined.
     */
    enum CompositionMode {
            /** Join when all join attributes match (logical and). */
            MATCH_ALL,
            /** Join when at least one join attribute matches (logical or). */
            MATCH_ANY;
    }

    // join mode
    private JoinMode m_joinMode = JoinMode.INNER;

    // join columns
    private JoinColumn[] m_leftJoinColumns;

    private JoinColumn[] m_rightJoinColumns;

    private CompositionMode m_compositionMode = CompositionMode.MATCH_ALL;

    // output
    private boolean m_mergeJoinColumns;

    private boolean m_outputUnmatchedRowsToSeparatePorts;

    // row keys
    private boolean m_assignNewRowKeys = false;

    private String m_rowKeySeparator = "_";

    // column selection
    private DataColumnSpecFilterConfiguration m_leftColSelectConfig =
        new DataColumnSpecFilterConfiguration(LEFT_COL_SELECT_CONFIG);

    private DataColumnSpecFilterConfiguration m_rightColSelectConfig =
        new DataColumnSpecFilterConfiguration(RIGHT_COL_SELECT_CONFIG);

    private ColumnNameDisambiguation m_duplicateHandling;

    private String m_duplicateColSuffix;

    // performance
    private OutputRowOrder m_outputRowOrder = OutputRowOrder.LEFT_RIGHT;

    private int m_maxOpenFiles = 200;

    private boolean m_hilitingEnabled = true;

    void loadDefaults(final DataTableSpec[] spec) throws InvalidSettingsException {
        loadSettingsForDialog(new NodeSettings(""), spec);
    }

    /**
     * Returns the mode how the two tables should be joined.
     *
     * @return the join mode
     */
    JoinMode getJoinMode() {
        return m_joinMode;
    }

    /**
     * Sets the mode how the two tables should be joined.
     *
     * @param joinMode the join mode
     */
    void setJoinMode(final JoinMode joinMode) {
        m_joinMode = joinMode;
    }

    /**
     * Returns the columns of the left table used in the join predicate.
     *
     * @return the leftJoinColumns
     */
    JoinColumn[] getLeftJoinColumns() {
        return m_leftJoinColumns;
    }

    /**
     * Sets the columns of the left table used in the join predicate.
     *
     * @param leftJoinColumns the leftJoinColumns to set
     */
    void setLeftJoinColumns(final JoinColumn[] leftJoinColumns) {
        m_leftJoinColumns = leftJoinColumns;
    }

    /**
     * Returns the columns of the right table used in the join predicate.
     *
     * @return the rightJoinColumns
     */
    JoinColumn[] getRightJoinColumns() {
        return m_rightJoinColumns;
    }

    /**
     * Sets the columns of the right table used in the join predicate.
     *
     * @param rightJoinColumns the rightJoinColumns to set
     */
    void setRightJoinColumns(final JoinColumn[] rightJoinColumns) {
        m_rightJoinColumns = rightJoinColumns;
    }

    /**
     * @return the compositionMode
     */
    CompositionMode getCompositionMode() {
        return m_compositionMode;
    }

    /**
     * @param compositionMode the compositionMode to set
     */
    void setCompositionMode(final CompositionMode compositionMode) {
        m_compositionMode = compositionMode;
    }

    boolean isMergeJoinColumns() {
        return m_mergeJoinColumns;
    }

    void setMergeJoinColumns(final boolean value) {
        m_mergeJoinColumns = value;
    }

    boolean isOutputUnmatchedRowsToSeparateOutputPort() {
        return m_outputUnmatchedRowsToSeparatePorts;
    }

    void setOutputUnmatchedRowsToSeparateOutputPort(final boolean value) {
        m_outputUnmatchedRowsToSeparatePorts = value;
    }

    boolean isAssignNewRowKeys() {
        return m_assignNewRowKeys;
    }

    void setAssignNewRowKeys(final boolean value) {
        m_assignNewRowKeys = value;
    }

    /**
     * Return Separator of the RowKeys in the joined table.
     *
     * @return the rowKeySeparator
     */
    String getRowKeySeparator() {
        return m_rowKeySeparator;
    }

    /**
     * Set Separator of the RowKeys in the joined table.
     *
     * @param rowKeySeparator the rowKeySeparator to set
     */
    void setRowKeySeparator(final String rowKeySeparator) {
        m_rowKeySeparator = rowKeySeparator;
    }

    DataColumnSpecFilterConfiguration getLeftColumnSelectionConfig() {
        return m_leftColSelectConfig;
    }

    DataColumnSpecFilterConfiguration getRightColumnSelectionConfig() {
        return m_rightColSelectConfig;
    }

    /**
     * Returns how duplicate column names should be handled.
     *
     * @return the duplicate handling method
     */
    ColumnNameDisambiguation getDuplicateHandling() {
        return m_duplicateHandling;
    }

    /**
     * Sets how duplicate column names should be handled.
     *
     * @param duplicateHandling the duplicate handling method
     */
    void setDuplicateHandling(final ColumnNameDisambiguation duplicateHandling) {
        m_duplicateHandling = duplicateHandling;
    }

    /**
     * Returns the suffix that is appended to duplicate columns from the right table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @return the suffix
     */
    String getDuplicateColumnSuffix() {
        return m_duplicateColSuffix;
    }

    /**
     * Sets the suffix that is appended to duplicate columns from the right table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @param suffix the suffix
     */
    void setDuplicateColumnSuffix(final String suffix) {
        m_duplicateColSuffix = suffix;
    }

    OutputRowOrder getOutputRowOrder() {
        return m_outputRowOrder;
    }

    void setOutputRowOrder(final OutputRowOrder outputRowOrder) {
        m_outputRowOrder = outputRowOrder;
    }

    /**
     * Return number of files that are allowed to be openend by the Joiner.
     *
     * @return the maxOpenFiles
     */
    int getMaxOpenFiles() {
        return m_maxOpenFiles;
    }

    /**
     * Set number of files that are allowed to be openend by the Joiner.
     *
     * @param maxOpenFiles the maxOpenFiles to set
     */
    void setMaxOpenFiles(final int maxOpenFiles) {
        m_maxOpenFiles = maxOpenFiles;
    }

    boolean isHilitingEnabled() {
        return m_hilitingEnabled;
    }

    void enabledHiliting(final boolean enabled) {
        m_hilitingEnabled = enabled;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettingsCommon(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_joinMode = JoinMode.valueOf(settings.getString(JOIN_MODE, JoinMode.INNER.name()));

        String[] leftJoinColumnsString = settings.getStringArray(LEFT_JOINING_COLUMNS, new String[0]);
        m_leftJoinColumns = Arrays.stream(leftJoinColumnsString).map(JoinColumn::fromString).toArray(JoinColumn[]::new);

        String[] rightJoinColumnsString = settings.getStringArray(RIGHT_JOINING_COLUMNS, new String[0]);
        m_rightJoinColumns =
            Arrays.stream(rightJoinColumnsString).map(JoinColumn::fromString).toArray(JoinColumn[]::new);

        m_compositionMode =
            CompositionMode.valueOf(settings.getString(COMPOSITION_MODE, CompositionMode.MATCH_ALL.name()));

        m_mergeJoinColumns = settings.getBoolean(MERGE_JOIN_COLUMNS, true);
        m_outputUnmatchedRowsToSeparatePorts = settings.getBoolean(OUTPUT_UNMATCHED_ROWS_TO_SEPARATE_PORTS, false);

        m_assignNewRowKeys = settings.getBoolean(ASSIGN_NEW_ROW_KEYS, false);
        m_rowKeySeparator = settings.getString(ROW_KEY_SEPARATOR, "_");

        m_duplicateHandling = ColumnNameDisambiguation.valueOf(
            settings.getString(DUPLICATE_COLUMN_HANDLING, ColumnNameDisambiguation.APPEND_SUFFIX_AUTOMATIC.name()));
        m_duplicateColSuffix = settings.getString(DUPLICATE_COLUMN_SUFFIX, " (right)");

        m_outputRowOrder =
            OutputRowOrder.valueOf(settings.getString(OUTPUT_ROW_ORDER, OutputRowOrder.ARBITRARY.name()));
        m_maxOpenFiles = settings.getInt(MAX_OPEN_FILES, 200);
        m_hilitingEnabled = settings.getBoolean(HILITING_ENABLED, false);
    }

    /**
     * Loads the settings from the node settings object using default values if some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the input spec
     * @throws InvalidSettingsException
     */
    void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec[] spec)
        throws InvalidSettingsException {

        loadSettingsCommon(settings);

        m_leftColSelectConfig.loadConfigurationInDialog(settings, spec[0]);
        m_rightColSelectConfig.loadConfigurationInDialog(settings, spec[1]);
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {

        loadSettingsCommon(settings);

        m_leftColSelectConfig.loadConfigurationInModel(settings);
        m_rightColSelectConfig.loadConfigurationInModel(settings);
    }

    public void validateSettings() throws InvalidSettingsException {
        if (getDuplicateHandling() == null) {
            throw new InvalidSettingsException("No duplicate handling method selected");
        }
        if (getJoinMode() == null) {
            throw new InvalidSettingsException("No join mode selected");
        }
        if ((getLeftJoinColumns() == null) || getLeftJoinColumns().length < 1 || getRightJoinColumns() == null
            || getRightJoinColumns().length < 1) {
            throw new InvalidSettingsException("Please define at least one joining column pair.");
        }
        if (getLeftJoinColumns() != null && getRightJoinColumns() != null
            && getLeftJoinColumns().length != getRightJoinColumns().length) {
            throw new InvalidSettingsException(
                "Number of columns selected from the top table and from " + "the bottom table do not match");
        }
        if (getDuplicateHandling() == ColumnNameDisambiguation.APPEND_SUFFIX
            && (getDuplicateColumnSuffix() == null || getDuplicateColumnSuffix().isEmpty())) {
            throw new InvalidSettingsException("No suffix for duplicate columns provided");
        }
        if (getMaxOpenFiles() < 3) {
            throw new InvalidSettingsException("Maximum number of open files must be at least 3.");
        }
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        // join mode
        settings.addString(JOIN_MODE, m_joinMode.name());

        // join columns
        settings.addString(COMPOSITION_MODE, m_compositionMode.toString());
        settings.addStringArray(LEFT_JOINING_COLUMNS,
            Arrays.stream(m_leftJoinColumns).map(JoinColumn::toColumnName).toArray(String[]::new));
        settings.addStringArray(RIGHT_JOINING_COLUMNS,
            Arrays.stream(m_rightJoinColumns).map(JoinColumn::toColumnName).toArray(String[]::new));

        // output
        settings.addBoolean(MERGE_JOIN_COLUMNS, m_mergeJoinColumns);
        settings.addBoolean(OUTPUT_UNMATCHED_ROWS_TO_SEPARATE_PORTS, m_outputUnmatchedRowsToSeparatePorts);

        // row keys
        settings.addBoolean(ASSIGN_NEW_ROW_KEYS, m_assignNewRowKeys);
        settings.addString(ROW_KEY_SEPARATOR, m_rowKeySeparator);

        // column selection
        m_leftColSelectConfig.saveConfiguration(settings);
        m_rightColSelectConfig.saveConfiguration(settings);
        settings.addString(DUPLICATE_COLUMN_HANDLING, m_duplicateHandling.toString());
        settings.addString(DUPLICATE_COLUMN_SUFFIX, m_duplicateColSuffix);

        // performance
        settings.addString(OUTPUT_ROW_ORDER, m_outputRowOrder.name());
        settings.addInt(MAX_OPEN_FILES, m_maxOpenFiles);
        settings.addBoolean(HILITING_ENABLED, m_hilitingEnabled);

    }

}
