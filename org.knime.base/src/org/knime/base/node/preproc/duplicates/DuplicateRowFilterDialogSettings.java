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
 *   Jan 4, 2023 (benjamin): created
 */
package org.knime.base.node.preproc.duplicates;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.AlwaysSaveTrueBoolean;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * {@link NodeParameters} implementation for the Duplicate Row Filter to auto-generate a Web-UI based dialog. Note that
 * this class is only used for the dialog generation and not by the node model.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class DuplicateRowFilterDialogSettings implements NodeParameters {

    @Section(title = "Duplicate detection")
    @Before(DuplicateHandlingSection.class)
    interface DuplicateDetectionSection {
    }

    @Persistor(ConsidererdColumnsPersistor.class)
    @Widget(title = "Choose columns for duplicates detection",
        description = "Allows the selection of columns identifying the duplicates. "
            + "Columns not selected are handled under \"Row selection\" in the \"Advanced\" settings.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(DuplicateDetectionSection.class)
    ColumnFilter m_consideredColumns;

    static final class ConsidererdColumnsPersistor extends LegacyColumnFilterPersistor {
        ConsidererdColumnsPersistor() {
            super(DuplicateRowFilterSettings.GROUP_COLS_KEY);
        }
    }

    @Section(title = "Duplicate handling")
    @After(DuplicateDetectionSection.class)
    @Before(PerformanceSection.class)
    interface DuplicateHandlingSection {
    }

    interface DuplicateRowHandlingRef extends ParameterReference<DuplicateRowHandling> {
    }

    @Persistor(DuplicateRowHandlingPersistor.class)
    @Widget(title = "Duplicate rows", description = "Choose how duplicate rows should be handled." //
        + "<ul>" //
        + "<li><b>Remove duplicate rows:</b> Removes duplicate rows and keeps only unique and chosen rows.</li>" //
        + "<li><b>Keep duplicate rows:</b> Appends columns with additional information to the input table.</li>" //
        + "</ul")
    @RadioButtonsWidget
    @ValueReference(DuplicateRowHandlingRef.class)
    @Layout(DuplicateHandlingSection.class)
    DuplicateRowHandling m_duplicateHandling = DuplicateRowHandling.REMOVE;

    static final class IsKeep implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(DuplicateRowHandling.isKeep(DuplicateRowHandlingRef.class));
        }
    }

    static final class AddUniqueLabel implements BooleanReference {
    }

    static final class AddChosenRowIdsColumn implements BooleanReference {
    }

    static final class KeepDuplicatesAndAddUniqueLabel implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsKeep.class).and(i.getPredicate(AddUniqueLabel.class));

        }
    }

    static final class KeepDuplicatesAndAddChosenRowIdsColumn implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsKeep.class).and(i.getPredicate(AddChosenRowIdsColumn.class));
        }
    }

    @Persist(configKey = DuplicateRowFilterSettings.ADD_ROW_DUPLICATE_FLAG_KEY)
    @Widget(title = "Add column showing the row status ('unique', 'chosen', 'duplicate') to all rows",
        description = "Appends a column with the row status:" //
            + "<ul>" //
            + "<li><i>unique:</i> There is no other row with the same values in the selected columns.</li>" //
            + "<li><i>chosen:</i> This row was chosen from a set of duplicate rows.</li>" //
            + "<li><i>duplicate:</i> This row is a duplicate and represented by another row.</li>" //
            + "</ul")
    @Effect(predicate = IsKeep.class, type = EffectType.SHOW)
    @Layout(DuplicateHandlingSection.class)
    @ValueReference(AddUniqueLabel.class)
    boolean m_addUniqueLabel = true;

    @Persist(configKey = DuplicateRowFilterSettings.UNIQUE_FLAG_COLUMN_NAME_KEY)
    @Migration(LoadLegacyUniqueStatusColumnName.class)
    @Widget(title = "Column name of row status",
        description = "Choose the column name to which the row status "
            + "('unique', 'chosen', 'duplicate') should be outputted.")
    @Effect(predicate = KeepDuplicatesAndAddUniqueLabel.class, type = EffectType.SHOW)
    @Layout(DuplicateHandlingSection.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    String m_uniqueStatusColumnName = "Duplicate Status";

    static final class LoadLegacyUniqueStatusColumnName implements DefaultProvider<String> {

        @Override
        public String getDefault() {
            return DuplicateRowFilterSettings.UNIQUE_COLUMN_NAME_LEGACY_VALUE;
        }
    }

    @Persist(configKey = DuplicateRowFilterSettings.ADD_ROW_ID_FLAG_KEY)
    @Widget(title = "Add column identifying the RowID of the chosen row for each duplicate row",
        description = "Appends a column with the RowID of the chosen row for duplicate rows. "
            + "Unique and chosen rows will not have a RowID assigned. ")
    @Effect(predicate = IsKeep.class, type = EffectType.SHOW)
    @Layout(DuplicateHandlingSection.class)
    @ValueReference(AddChosenRowIdsColumn.class)
    boolean m_addRowIdLabel;

    @Persist(configKey = DuplicateRowFilterSettings.ROW_ID_FLAG_COLUMN_NAME_KEY)
    @Migration(LoadLegacyChosenRowIdsColumnName.class)
    @Widget(title = "Column name of chosen RowIDs",
        description = "Choose the column name to which the RowID "
            + "of the chosen row for each duplicate row should be outputted.")
    @Effect(predicate = KeepDuplicatesAndAddChosenRowIdsColumn.class, type = EffectType.SHOW)
    @Layout(DuplicateHandlingSection.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    String m_chosenRowIdsColumnName = "Duplicate Chosen";

    static final class LoadLegacyChosenRowIdsColumnName implements DefaultProvider<String> {

        @Override
        public String getDefault() {
            return DuplicateRowFilterSettings.ROW_ID_COLUMN_NAME_LEGACY_VALUE;
        }
    }

    static final String DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY = "doNotAllowEmptyBlankOrPaddedColumnName";

    static final class DoNotAllowEmptyBlankOrPaddedColumnNamePersistor extends AlwaysSaveTrueBoolean {
        protected DoNotAllowEmptyBlankOrPaddedColumnNamePersistor() {
            super(DO_NOT_ALLOW_EMPTY_BLANK_PADDED_COLUMN_NAME_CFG_KEY);
        }
    }

    @Persistor(DoNotAllowEmptyBlankOrPaddedColumnNamePersistor.class)
    boolean m_doNotAllowEmptyBlankOrPaddedColumnName = true;

    interface RowSelectionRef extends ParameterReference<RowSelection> {
    }

    static final class IsFirstOrLast implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(RowSelection.isFirstOrLast(RowSelectionRef.class));
        }
    }

    @Persist(configKey = DuplicateRowFilterSettings.RowSelectionType.ROW_SELECTION_KEY)
    @Widget(title = "Row chosen in case of duplicate",
        description = "Defines which row for each set of duplicates is selected." //
            + "<ul>" //
            + "<li><b>First:</b> The first row in sequence is chosen.</li>"
            + "<li><b>Last:</b> The last row in sequence is chosen.</li>"
            + "<li><b>Minimum of:</b> The first row with the minimum value in the selected column is chosen. "
            + /* */ "In case of strings, the row will be chosen following lexicographical order. "
            + /* */ "Missing values are sorted after the maximum value.</li>"
            + "<li><b>Maximum of:</b> The first row with the maximum value in the selected column is chosen. "
            + /* */ "In case of strings, the row will be chosen following lexicographical order. "
            + /* */ "Missing values are sorted before the minimum value.</li>" //
            + "</ul>")
    @RadioButtonsWidget
    @ValueReference(RowSelectionRef.class)
    @Layout(DuplicateHandlingSection.class)
    RowSelection m_rowSelectionType = RowSelection.FIRST;

    @Persist(configKey = DuplicateRowFilterSettings.REFERENCE_COL_KEY)
    @Widget(title = "Column", description = "")
    @ChoicesProvider(AllColumnsProvider.class)
    @Effect(predicate = IsFirstOrLast.class, type = EffectType.HIDE)
    @Layout(DuplicateHandlingSection.class)
    String m_selectedColumn;

    @Section(title = "Performance")
    @Advanced
    @After(DuplicateHandlingSection.class)
    interface PerformanceSection {
    }

    @Persist(configKey = DuplicateRowFilterSettings.IN_MEMORY_KEY)
    @Widget(title = "Compute in memory",
        description = "If selected, computation is sped up by utilizing working memory (RAM). "
            + "The amount of required memory is higher than for a regular computation and also depends on the amount "
            + "of input data.")
    @Layout(PerformanceSection.class)
    boolean m_inMemory;

    @Persist(configKey = DuplicateRowFilterSettings.RETAIN_ROW_ORDER_KEY)
    @Widget(title = "Retain row order",
        description = "If selected, the rows in the output table are guaranteed to have the same "
            + "order as in the input table.")
    @Layout(PerformanceSection.class)
    boolean m_retainOrder = true;

    @Persist(configKey = DuplicateRowFilterSettings.UPDATE_DOMAINS_KEY)
    @Migrate(loadDefaultIfAbsent = true)
    @Widget( //
        title = "Update domains of all columns", //
        description = "Recompute the domains of all columns in the output tables such that the domains'" //
            + " bounds exactly match the bounds of the data in the output tables."//
    )
    @Layout(PerformanceSection.class)
    boolean m_updateDomains;

    /** Constructor for deserialization */
    DuplicateRowFilterDialogSettings() {
    }

    /** Constructor for auto-configure */
    DuplicateRowFilterDialogSettings(final NodeParametersInput context) {
        var spec = context.getInTableSpecs()[0];
        if (spec != null) {
            // Choose the first column as the selected column for the row selection
            m_selectedColumn = spec.getColumnNames()[0];
        }
    }

    /** Options for the duplicate row handling */
    enum DuplicateRowHandling {
            @Label("Remove duplicate rows")
            REMOVE, //
            @Label("Keep duplicate rows")
            KEEP;

        static EffectPredicateProvider
            isKeep(final Class<? extends ParameterReference<DuplicateRowHandling>> reference) {
            return i -> i.getEnum(reference).isOneOf(KEEP);
        }
    }

    /** Whether to add a column with duplicate status - then dislay column name selection */
    interface IsAddUniqueStatusColumn {
    }

    /** Whether to add a column with chosen RowIDs - then dislay column name selection */
    interface IsAddChosenRowIdsColumn {
    }

    /** Options for the row selection */
    enum RowSelection {
            @Label("First")
            FIRST,

            @Label("Last")
            LAST,

            @Label("Minimum of")
            MINIMUM,

            @Label("Maximum of")
            MAXIMUM;

        static EffectPredicateProvider
            isFirstOrLast(final Class<? extends ParameterReference<RowSelection>> reference) {
            return i -> i.getEnum(reference).isOneOf(FIRST, LAST);
        }

    }

    /** Custom persistor for the duplicate row handling: true for REMOVE, false for KEEP */
    private static final class DuplicateRowHandlingPersistor implements NodeParametersPersistor<DuplicateRowHandling> {

        @Override
        public DuplicateRowHandling load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(DuplicateRowFilterSettings.REMOVE_DUPLICATE_ROWS_KEY)) {
                return DuplicateRowHandling.REMOVE;
            } else {
                return DuplicateRowHandling.KEEP;
            }
        }

        @Override
        public void save(final DuplicateRowHandling obj, final NodeSettingsWO settings) {
            settings.addBoolean(DuplicateRowFilterSettings.REMOVE_DUPLICATE_ROWS_KEY,
                obj == DuplicateRowHandling.REMOVE);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DuplicateRowFilterSettings.REMOVE_DUPLICATE_ROWS_KEY}};
        }
    }

}
