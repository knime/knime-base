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
 *   Jun 19, 2007 (ohl): created
 */
package org.knime.base.node.preproc.cellsplit2;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
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
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Settings for the Cell Splitter node modern UI dialog.
 * <p>
 * This node splits the string representation of cells in one column into separate columns or into one column containing
 * a collection of cells, based on a specified delimiter.
 * </p>
 *
 * @author Ali Asghar Marvi
 */
@LoadDefaultsForAbsentFields
final class CellSplitter2NodeParameters implements NodeParameters {

    CellSplitter2NodeParameters() {
        m_columnName = "";
    }

    CellSplitter2NodeParameters(final NodeParametersInput input) {
        // this sensible default is currently not respected, since the node model is not migrated
        m_columnName = input.getInTableSpec(0).stream().flatMap(DataTableSpec::stream)
            .filter(cSpec -> cSpec.getType().isCompatible(StringValue.class)).findFirst().map(DataColumnSpec::getName)
            .orElse("");
    }
    // ===== SECTION DEFINITIONS =====

    @Section(title = "Column to Split")
    interface ColumnSelectionSection {
    }

    @Section(title = "Settings")
    @After(ColumnSelectionSection.class)
    interface SettingsSection {
    }

    @Section(title = "Output")
    @After(SettingsSection.class)
    interface OutputSection {
    }

    @Section(title = "Missing Value Handling")
    @After(OutputSection.class)
    interface MissingValueHandlingSection {
    }

    // ===== PARAMETER REFERENCES FOR EFFECTS =====

    interface OutputModeRef extends ParameterReference<OutputMode> {
    }

    interface SizeModeRef extends ParameterReference<SizeMode> {
    }

    static final class ScanLimitEnabledRef implements BooleanReference {

    }

    // ===== ENUMS =====

    enum OutputMode {
            @Label(value = "As new columns", description = "If selected, the output will consist of one "
                + "or more columns, each containing a split part.")
            AS_COLUMNS,

            @Label(value = "As list", description = "If selected, the output will consist of one column containing list"
                + " collection cells in which the split parts are stored." + " Duplicates can occur in list cells.")
            AS_LIST,

            @Label(value = "As set (remove duplicates)",
                description = "If selected, the output will consist of one column "
                    + "containing set collection cells in which the split parts are stored. "
                    + "Duplicates are removed and can not occur in set cells.")
            AS_SET
    }

    enum SizeMode {
            @Label(value = "Guess size and column types (requires additional data table scan)",
                description = "If this is checked, the node performs an "
                    + "additional scan through the entire data table and "
                    + "computes the number of columns needed to hold all parts of the split. "
                    + "In addition it determines the column type of the new columns.")
            GUESS_SIZE,

            @Label(value = "Set array size",
                description = "Check this and specify the number of columns to append. "
                    + "All created columns will be of type String. " + "(See node description for what happens if the "
                    + "split produces a different number of parts.)")
            FIXED_SIZE
    }

    // ===== EFFECT PREDICATES =====

    static final class IsOutputAsColumns implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.AS_COLUMNS);
        }
    }

    static final class IsOutputAsColumnsAndFixedSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsOutputAsColumns.class)
                .and(i.getEnum(SizeModeRef.class).isOneOf(SizeMode.FIXED_SIZE));
        }
    }

    static final class IsOutputAsColumnsAndGuessSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsOutputAsColumns.class)
                .and(i.getEnum(SizeModeRef.class).isOneOf(SizeMode.GUESS_SIZE));
        }
    }

    static final class IsOutputAsColumnsAndGuessSizeAndScanLimitEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsOutputAsColumnsAndGuessSize.class).and(i.getPredicate(ScanLimitEnabledRef.class));
        }
    }

    // ===== CUSTOM PERSISTORS =====

    static final class OutputModePersistor implements NodeParametersPersistor<OutputMode> {

        @Override
        public void save(final OutputMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(CellSplitter2UserSettings.CFG_OUTPUTASLIST, obj == OutputMode.AS_LIST);
            settings.addBoolean(CellSplitter2UserSettings.CFG_OUTPUTASSET, obj == OutputMode.AS_SET);
            settings.addBoolean(CellSplitter2UserSettings.CFG_OUTPUTASCOLS, obj == OutputMode.AS_COLUMNS);
        }

        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean outputAsList = settings.getBoolean(CellSplitter2UserSettings.CFG_OUTPUTASLIST, false);
            boolean outputAsSet = settings.getBoolean(CellSplitter2UserSettings.CFG_OUTPUTASSET, false);

            if (outputAsList) {
                return OutputMode.AS_LIST;
            } else if (outputAsSet) {
                return OutputMode.AS_SET;
            } else {
                return OutputMode.AS_COLUMNS;
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CellSplitter2UserSettings.CFG_OUTPUTASLIST},
                {CellSplitter2UserSettings.CFG_OUTPUTASSET}, {CellSplitter2UserSettings.CFG_OUTPUTASCOLS}};
        }
    }

    static final class SizeModePersistor extends EnumBooleanPersistor<SizeMode> {
        SizeModePersistor() {
            super(CellSplitter2UserSettings.CFG_GUESSCOLS, SizeMode.class, SizeMode.GUESS_SIZE);
        }
    }

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Select a column", description = "Select the column whose values are split.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Persist(configKey = CellSplitter2UserSettings.CFG_COLNAME)
    String m_columnName;

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Remove input column",
        description = "When checked, the selected input column will not be part of the output table.")
    boolean m_removeInputColumn;

    // ===== SETTINGS SECTION =====

    @Layout(SettingsSection.class)
    @Widget(title = "Enter a delimiter", description = "Specify the delimiter in the value, that splits each part.")
    String m_delimiter = ",";

    @Layout(SettingsSection.class)
    @Widget(title = "Use \\ as escape character",
        description = "If enabled, the backslash (\\) can be used to escape characters, "
            + "such as \\t for tabs. You can use the full escape capabilities of Java.")
    boolean m_useEscapeCharacter;

    @Layout(SettingsSection.class)
    @Widget(title = "Enter a quotation character",
        description = "Specify the quotation character if the different parts in the value are quoted. "
            + "(The character to escape quotes is always the backslash.) "
            + "If no quotation character is needed leave it empty.")
    @TextInputWidget(placeholder = "(leave empty for none)")
    String m_quotePattern = "\"";

    // this setting is not visible but is used by CellSplitter User settings class
    boolean m_removeQuotes = true;

    @Layout(SettingsSection.class)
    @Widget(title = "Remove leading and trailing white space chars (trim)",
        description = "If checked, leading and trailing white spaces of each part (token) will be deleted.")
    @Persist(configKey = CellSplitter2UserSettings.CFG_TRIM)
    boolean m_trim = true;

    // ===== OUTPUT SECTION =====

    @Layout(OutputSection.class)
    @Widget(title = "Output format",
        description = "Select how the split results should be output: as a list collection, "
            + "as a set collection (duplicates removed), or as separate columns.")
    @RadioButtonsWidget
    @ValueReference(OutputModeRef.class)
    @Persistor(OutputModePersistor.class)
    OutputMode m_outputMode = OutputMode.AS_COLUMNS;

    @Layout(OutputSection.class)
    @Widget(title = "Split input column name for output column names",
        description = "When outputting as new columns, check this option when the input column name can be split "
            + "in the same manner as the column's content to obtain the names for the output columns.")
    @Effect(predicate = IsOutputAsColumns.class, type = EffectType.SHOW)
    boolean m_splitColumnNames;

    @Layout(OutputSection.class)
    @Widget(title = "Size determination", description = """
            Choose whether to specify a fixed number of output columns or to automatically determine the size.
            """)
    @RadioButtonsWidget
    @ValueReference(SizeModeRef.class)
    @Effect(predicate = IsOutputAsColumns.class, type = EffectType.SHOW)
    @Persistor(SizeModePersistor.class)
    // legacy dialog default guessed number of columns
    SizeMode m_sizeMode = SizeMode.GUESS_SIZE;

    @Layout(OutputSection.class)
    @Widget(title = "Number of columns",
        description = "Specify the number of columns to append. All created columns will be of type String.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsOutputAsColumnsAndFixedSize.class, type = EffectType.SHOW)
    @Persist(configKey = CellSplitter2UserSettings.CFG_NUMOFCOLS)
    int m_numberOfColumns = 6;

    @Layout(OutputSection.class)
    @Widget(title = "Scan limit (number of lines to guess on)",
        description = "Maximum number of rows to scan for guessing the number of output columns.")
    @Effect(predicate = IsOutputAsColumnsAndGuessSize.class, type = EffectType.SHOW)
    @ValueReference(ScanLimitEnabledRef.class)
    boolean m_hasScanLimit;

    @Layout(OutputSection.class)
    @Widget(title = "Maximum rows to scan",
        description = "Maximum number of rows to scan for guessing the number of output columns and their types.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsOutputAsColumnsAndGuessSizeAndScanLimitEnabled.class, type = EffectType.SHOW)
    int m_scanLimit = 50;

    // ===== MISSING VALUE HANDLING SECTION =====

    @Layout(MissingValueHandlingSection.class)
    @Widget(title = "Create empty string cells", description = """
            If checked, empty string cells are created for missing or short input cells instead of missing value cells.
            """)
    boolean m_useEmptyString = true;
}
