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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.DataCellComparisonMode;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.KeepRowKeysFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * This class hold the settings for the joiner node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
class Joiner3Settings {


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

        boolean isIncludeMatchingRows() { return m_includeMatchingRows; }
        boolean isIncludeLeftUnmatchedRows() { return m_includeLeftUnmatchedRows; }
        boolean isIncludeRightUnmatchedRows() { return m_includeRightUnmatchedRows; }

        @Override public String toString() { return m_uiDisplayText; }

    }

    /**
     * Conjunctive or disjunctive join mode.
     */
    enum CompositionModeButtonGroup implements ButtonGroupEnumInterface {
        MATCH_ALL("all of the following", "Join rows when all join attributes match (logical and)."),
        MATCH_ANY("any of the following", "Join rows when at least one join attribute matches (logical or).");

        private final String m_label;
        private final String m_tooltip;

        CompositionModeButtonGroup(final String label, final String tooltip) {
            m_label = label;
            m_tooltip = tooltip;
        }

        @Override public String getText() { return m_label; }
        @Override public String getActionCommand() { return name(); }
        @Override public String getToolTip() { return m_tooltip; }
        @Override public boolean isDefault() { return MATCH_ALL == this; }
    }

    /**
     * Options for comparing data cells in join columns.
     * Introduced in KNIME 4.4
     */
    enum DataCellComparisonModeButtonGroup implements ButtonGroupEnumInterface {
            STRICT(DataCellComparisonMode.STRICT, "value and type",
                "Two cells need to have the exact same value and type. "
                    + "For instance, a long and an integer cell will never match."),
            STRING(DataCellComparisonMode.AS_STRING, "string representation",
                "Convert values in join columns to string before comparing them."),
            NUMERIC(DataCellComparisonMode.NUMERIC_AS_LONG, "making integer types compatible",
                "Ignore type differences for numerical types. "
                    + "For instance, an integer cell with value 1 will match a long cell with value 1.");

        private final String m_label;
        private final String m_tooltip;
        private final DataCellComparisonMode m_mode;

        DataCellComparisonModeButtonGroup(final DataCellComparisonMode mode, final String label, final String tooltip) {
            m_mode = mode;
            m_label = label;
            m_tooltip = tooltip;
        }

        @Override public String getText() { return m_label; }
        @Override public String getActionCommand() { return name(); }
        @Override public String getToolTip() { return m_tooltip; }
        @Override public boolean isDefault() { return STRICT == this; }
        public DataCellComparisonMode getMode() { return m_mode; }
    }

    enum RowKeyFactoryButtonGroup implements ButtonGroupEnumInterface {
            /** Output rows may be provided in any order. */
            CONCATENATE("Concatenate original row keys with separator",
                "For instance, when selecting separator \"_\", "
                    + "a row joining rows with keys Row0 and Row1 is assigned key Row0_Row1.",
                JoinSpecification::createConcatRowKeysFactory),
            SEQUENTIAL("Assign new row keys sequentially",
                "Output rows are assigned sequential row keys, e.g., Row0, Row1, etc. ",
                s -> JoinSpecification.createSequenceRowKeysFactory()),
            KEEP_ROWID("Keep row keys",
                "Only available when join criteria ensure that matching rows have the same row keys.",
                s -> new KeepRowKeysFactory());

        private final String m_label;

        private final String m_tooltip;

        private final Function<String, BiFunction<DataRow, DataRow, RowKey>> m_factoryCreator;

        RowKeyFactoryButtonGroup(final String label, final String tooltip,
            final Function<String, BiFunction<DataRow, DataRow, RowKey>> factoryCreator) {
            m_label = label;
            m_tooltip = tooltip;
            m_factoryCreator = factoryCreator;
        }

        @Override public String getText() { return m_label; }
        @Override public String getActionCommand() { return name(); }
        @Override public String getToolTip() { return m_tooltip; }
        @Override public boolean isDefault() { return this == CONCATENATE; }

        public BiFunction<DataRow, DataRow, RowKey> getFactory(final String separator){
            return m_factoryCreator.apply(separator);
        }
    }

    /**
     * Duplicate column names handling options.
     */
    enum ColumnNameDisambiguationButtonGroup implements ButtonGroupEnumInterface {
            DO_NOT_EXECUTE("Do not execute", "Prevents the node from being executed if column names clash."),
//            APPEND_SUFFIX_AUTOMATIC("Append default suffix", "Appends the suffix \" (#1)\"."),
            APPEND_SUFFIX("Append custom suffix", "Appends the given suffix.");

        private String m_label;

        private String m_tooltip;

        ColumnNameDisambiguationButtonGroup(final String label, final String tooltip) {
            m_label = label;
            m_tooltip = tooltip;
        }

        @Override public String getText() { return m_label; }
        @Override public String getActionCommand() { return name(); }
        @Override public String getToolTip() { return m_tooltip; }
        @Override public boolean isDefault() { return APPEND_SUFFIX == this; }
    }

    enum OutputRowOrderButtonGroup implements ButtonGroupEnumInterface {
            /** Output rows may be provided in any order. */
            ARBITRARY("Arbitrary output order (may vary randomly)", "The output can vary depending on the currently "
                    + "available amount of main memory. This means that identical input can produce different output"
                    + " orders on consecutive executions.",
                    org.knime.core.data.join.JoinSpecification.OutputRowOrder.ARBITRARY),
            LEFT_RIGHT("Sort by row offset in left table, then right table",
                "<html>Rows are output in three blocks:        "
                    + "<ol>                                    "
                    + "<li>matched rows</li>                   "
                    + "<li>unmatched rows from left table</li> "
                    + "<li>unmatched rows from right table</li>"
                    + "</ol>                                   "
                    + "Each block is sorted by row offset in the left table, breaking ties using the "
                    + "row offset in the right table.",
                    org.knime.core.data.join.JoinSpecification.OutputRowOrder.LEFT_RIGHT);

        private final String m_label;

        private final String m_tooltip;

        private final org.knime.core.data.join.JoinSpecification.OutputRowOrder m_outputRowOrder;

        OutputRowOrderButtonGroup(final String label, final String tooltip,
            final org.knime.core.data.join.JoinSpecification.OutputRowOrder outputRowOrder) {
            m_label = label;
            m_tooltip = tooltip;
            m_outputRowOrder = outputRowOrder;
        }

        @Override public String getText() { return m_label; }
        @Override public String getActionCommand() { return name(); }
        @Override public String getToolTip() { return m_tooltip; }
        @Override public boolean isDefault() { return this == ARBITRARY; }

        org.knime.core.data.join.JoinSpecification.OutputRowOrder getOutputOrder() {
            return m_outputRowOrder;
        }
    }

    // join conditions
    final SettingsModelStringArray m_leftJoiningColumnsModel =
        new SettingsModelStringArray("leftTableJoinPredicate", new String[0]);

    final SettingsModelStringArray m_rightJoiningColumnsModel =
        new SettingsModelStringArray("rightTableJoinPredicate", new String[0]);

    final SettingsModelString m_compositionModeModel =
        new SettingsModelString("compositionMode", CompositionModeButtonGroup.MATCH_ALL.name());

    final SettingsModelString m_dataCellComparisonModeModel =
            new SettingsModelString("dataCellComparisonMode", DataCellComparisonModeButtonGroup.STRICT.name());

    // include in output: matches, left unmatched, right unmatched
    final SettingsModelBoolean m_includeMatchesModel = new SettingsModelBoolean("includeMatchesInOutput", true);

    final SettingsModelBoolean m_includeLeftUnmatchedModel =
        new SettingsModelBoolean("includeLeftUnmatchedInOutput", false);

    final SettingsModelBoolean m_includeRightUnmatchedModel =
        new SettingsModelBoolean("includeRightUnmatchedInOutput", false);

    // output options
    final SettingsModelBoolean m_mergeJoinColumnsModel = new SettingsModelBoolean("mergeJoinColumns", false);

    final SettingsModelBoolean m_outputUnmatchedRowsToSeparatePortsModel =
        new SettingsModelBoolean("outputUnmatchedRowsToSeparatePorts", false);

    final SettingsModelBoolean m_enableHilitingModel = new SettingsModelBoolean("enableHiliting", false);

    // row keys
    final SettingsModelString m_rowKeyFactoryModel =
        new SettingsModelString("rowKeyFactory", RowKeyFactoryButtonGroup.CONCATENATE.name());

    final SettingsModelString m_rowKeySeparatorModel = new SettingsModelString("rowKeySeparator", "_");

    // include columns and column name disambiguation
    final SettingsModelString m_columnDisambiguationModel =
        new SettingsModelString("duplicateHandling", ColumnNameDisambiguationButtonGroup.APPEND_SUFFIX.name());

    final SettingsModelString m_columnNameSuffixModel = new SettingsModelString("suffix", " (right)");

    // performance
    final SettingsModelString m_outputRowOrderModel =
        new SettingsModelString("outputRowOrder", OutputRowOrderButtonGroup.ARBITRARY.name());

    final SettingsModelIntegerBounded m_maxOpenFilesModel =
        new SettingsModelIntegerBounded("maxOpenFiles", 200, 3, Integer.MAX_VALUE);

    final List<SettingsModel> m_settings = new ArrayList<>();

    // column selection settings models
    private DataColumnSpecFilterConfiguration m_leftColSelectConfig =
        new DataColumnSpecFilterConfiguration("leftColumnSelectionConfig");

    private DataColumnSpecFilterConfiguration m_rightColSelectConfig =
        new DataColumnSpecFilterConfiguration("rightColumnSelectionConfig");

    public Joiner3Settings() {
        m_settings.add(m_includeMatchesModel);
        m_settings.add(m_includeLeftUnmatchedModel);
        m_settings.add(m_includeRightUnmatchedModel);
        m_settings.add(m_leftJoiningColumnsModel);
        m_settings.add(m_rightJoiningColumnsModel);
        m_settings.add(m_compositionModeModel);
        m_settings.add(m_mergeJoinColumnsModel);
        m_settings.add(m_dataCellComparisonModeModel);
        m_settings.add(m_outputUnmatchedRowsToSeparatePortsModel);
        m_settings.add(m_rowKeyFactoryModel);
        m_settings.add(m_rowKeySeparatorModel);
        m_settings.add(m_columnDisambiguationModel);
        m_settings.add(m_columnNameSuffixModel);
        m_settings.add(m_outputRowOrderModel);
        m_settings.add(m_maxOpenFilesModel);
        m_settings.add(m_enableHilitingModel);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (SettingsModel model : m_settings) {
            // Backward compatibility: data cell comparison mode was introduced in KNIME 4.4
            if (model == m_dataCellComparisonModeModel
                && !settings.containsKey(m_dataCellComparisonModeModel.getKey())) {
                // use default value for that model
            } else {
                model.loadSettingsFrom(settings);
            }
        }
    }

    /**
     * Loads the settings from the node settings object using default values if some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the input spec
     * @throws InvalidSettingsException
     */
    void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec[] spec)
        throws InvalidSettingsException {
        loadSettings(settings);
        m_leftColSelectConfig.loadConfigurationInDialog(settings, spec[0]);
        m_rightColSelectConfig.loadConfigurationInDialog(settings, spec[1]);
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    public void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSettings(settings);
        m_leftColSelectConfig.loadConfigurationInModel(settings);
        m_rightColSelectConfig.loadConfigurationInModel(settings);
    }

    public void validateSettings() throws InvalidSettingsException {
        if (getLeftJoinColumns().length == 0) {
            throw new InvalidSettingsException("Please define at least one joining column pair.");
        }

        if (getColumnNameDisambiguation() == ColumnNameDisambiguationButtonGroup.APPEND_SUFFIX
            && getDuplicateColumnSuffix().trim().isEmpty()) {
            throw new InvalidSettingsException("No suffix for duplicate columns provided");
        }
    }

    /**
     * Create the join specification for the given table specs.
     *
     * @throws InvalidSettingsException
     */
    JoinSpecification joinSpecificationForSpecs(final PortObjectSpec... portSpecs) throws InvalidSettingsException {

        // left (top port) input table
        DataTableSpec left = (DataTableSpec)portSpecs[0];
        String[] leftIncludes = getLeftColumnSelectionConfig().applyTo(left).getIncludes();
        var leftSettings = new JoinTableSettings(isIncludeLeftUnmatched(),
            getLeftJoinColumns(), leftIncludes, InputTable.LEFT, left);

        // right (bottom port) input table
        DataTableSpec right = (DataTableSpec)portSpecs[1];
        String[] rightIncludes = getRightColumnSelectionConfig().applyTo(right).getIncludes();
        var rightSettings = new JoinTableSettings(isIncludeRightUnmatched(),
            getRightJoinColumns(), rightIncludes, InputTable.RIGHT, right);

        BiFunction<DataRow, DataRow, RowKey> rowKeysFactory = getRowKeyFactory().getFactory(getRowKeySeparator());

        UnaryOperator<String> columnNameDisambiguator;
        // replace with custom
        if (getColumnNameDisambiguation() == ColumnNameDisambiguationButtonGroup.APPEND_SUFFIX) {
            columnNameDisambiguator = s -> s.concat(getDuplicateColumnSuffix());
        } else {
            columnNameDisambiguator = s -> s.concat(" (#1)");
        }

        return new JoinSpecification.Builder(leftSettings, rightSettings)
            .conjunctive(getCompositionMode() == CompositionModeButtonGroup.MATCH_ALL)
            .outputRowOrder(getOutputRowOrder())
            .retainMatched(isIncludeMatches())
            .mergeJoinColumns(isMergeJoinColumns()).columnNameDisambiguator(columnNameDisambiguator)
            .dataCellComparisonMode(getDataCellComparisonMode())
            .rowKeyFactory(rowKeysFactory)
            .build();
    }


    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.forEach(m -> m.saveSettingsTo(settings));
        // column selection
        m_leftColSelectConfig.saveConfiguration(settings);
        m_rightColSelectConfig.saveConfiguration(settings);

    }

    boolean isIncludeMatches() {
        return m_includeMatchesModel.getBooleanValue();
    }

    boolean isIncludeLeftUnmatched() {
        return m_includeLeftUnmatchedModel.getBooleanValue();
    }

    boolean isIncludeRightUnmatched() {
        return m_includeRightUnmatchedModel.getBooleanValue();
    }

    JoinMode getJoinMode() {
        return Arrays.stream(JoinMode.values())
            .filter(mode -> mode.isIncludeMatchingRows() == isIncludeMatches()
                && mode.isIncludeLeftUnmatchedRows() == isIncludeLeftUnmatched()
                && mode.isIncludeRightUnmatchedRows() == isIncludeRightUnmatched())
            .findFirst().orElseThrow(() -> new IllegalStateException("Unknown join mode selected in dialog."));
    }

    /**
     * Returns the columns of the left table used in the join predicate.
     *
     * @return the leftJoinColumns
     */
    JoinColumn[] getLeftJoinColumns() {
        String[] leftJoinColumnsString = m_leftJoiningColumnsModel.getStringArrayValue();
        return Arrays.stream(leftJoinColumnsString).map(JoinColumn::fromString).toArray(JoinColumn[]::new);
    }

    /**
     * Sets the columns of the left table used in the join predicate.
     *
     * @param leftJoinColumns the leftJoinColumns to set
     */
    void setLeftJoinColumns(final JoinColumn[] leftJoinColumns) {
        m_leftJoiningColumnsModel
            .setStringArrayValue(Arrays.stream(leftJoinColumns).map(JoinColumn::toColumnName).toArray(String[]::new));
    }

    /**
     * Returns the columns of the right table used in the join predicate.
     *
     * @return the rightJoinColumns
     */
    JoinColumn[] getRightJoinColumns() {
        String[] rightJoinColumnsString = m_rightJoiningColumnsModel.getStringArrayValue();
        return Arrays.stream(rightJoinColumnsString).map(JoinColumn::fromString).toArray(JoinColumn[]::new);
    }

    /**
     * Sets the columns of the right table used in the join predicate.
     *
     * @param rightJoinColumns the rightJoinColumns to set
     */
    void setRightJoinColumns(final JoinColumn[] rightJoinColumns) {
        m_rightJoiningColumnsModel
            .setStringArrayValue(Arrays.stream(rightJoinColumns).map(JoinColumn::toColumnName).toArray(String[]::new));
    }

    /**
     * @return the compositionMode
     */
    CompositionModeButtonGroup getCompositionMode() {
        return CompositionModeButtonGroup.valueOf(m_compositionModeModel.getStringValue());
    }

    boolean isMergeJoinColumns() {
        return m_mergeJoinColumnsModel.getBooleanValue();
    }

    DataCellComparisonMode getDataCellComparisonMode() {
        return DataCellComparisonModeButtonGroup.valueOf(m_dataCellComparisonModeModel.getStringValue()).getMode();
    }

    boolean isOutputUnmatchedRowsToSeparateOutputPort() {
        return m_outputUnmatchedRowsToSeparatePortsModel.getBooleanValue();
    }

    /**
     * Return Separator of the RowKeys in the joined table.
     *
     * @return the rowKeySeparator
     */
    String getRowKeySeparator() {
        return m_rowKeySeparatorModel.getStringValue();
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
    ColumnNameDisambiguationButtonGroup getColumnNameDisambiguation() {
        return ColumnNameDisambiguationButtonGroup.valueOf(m_columnDisambiguationModel.getStringValue());
    }

    /**
     * Returns the suffix that is appended to duplicate columns from the right table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @return the suffix
     */
    String getDuplicateColumnSuffix() {
        return m_columnNameSuffixModel.getStringValue();
    }

    JoinSpecification.OutputRowOrder getOutputRowOrder() {
        return OutputRowOrderButtonGroup.valueOf(m_outputRowOrderModel.getStringValue()).getOutputOrder();
    }

    /**
     * Return number of files that are allowed to be openend by the Joiner.
     *
     * @return the maxOpenFiles
     */
    int getMaxOpenFiles() {
        return m_maxOpenFilesModel.getIntValue();
    }

    boolean isHilitingEnabled() {
        return m_enableHilitingModel.getBooleanValue();
    }

    RowKeyFactoryButtonGroup getRowKeyFactory() {
        return RowKeyFactoryButtonGroup.valueOf(m_rowKeyFactoryModel.getStringValue());
    }

}
