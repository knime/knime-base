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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
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
 * This node splits the string representation of cells in one column into separate columns
 * or into one column containing a collection of cells, based on a specified delimiter.
 * </p>
 *
 * @author ohl, University of Konstanz
 */
@SuppressWarnings("restriction")
final class CellSplitter2NodeSettings implements NodeParameters {

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
        @Label(value = "As list", description = "If selected, the output will consist of one column containing list collection cells in which the split parts are stored. Duplicates can occur in list cells. ")
        AS_LIST,

        @Label(value = "As set (remove duplicates)", description = "If selected, the output will consist of one column containing set collection cells in which the split parts are stored. Duplicates are removed and can not occur in set cells.")
        AS_SET,

        @Label(value = "As new columns", description = "If selected, the output will consist of one or more columns, each containing a split part. ")
        AS_COLUMNS
    }

    enum SizeMode {
        @Label(value = "Set array size", description = "Check this and specify the number of columns to append. All created columns will be of type String. (See node description for what happens if the split produces a different number of parts.) ")
        FIXED_SIZE,

        @Label(value = "Guess size and column types (requires additional data table scan)", description = "If this is checked, the node performs an additional scan through the entire data table and computes the number of columns needed to hold all parts of the split. In addition it determines the column type of the new columns.")
        GUESS_SIZE
    }

    // ===== EFFECT PREDICATES =====

    static final class IsOutputAsColumns implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.AS_COLUMNS);
        }
    }

    static final class IsFixedSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SizeModeRef.class).isOneOf(SizeMode.FIXED_SIZE);
        }
    }

    static final class IsGuessSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SizeModeRef.class).isOneOf(SizeMode.GUESS_SIZE);
        }
    }

    static final class IsOutputAsColumnsAndFixedSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.AS_COLUMNS)
                .and(i.getEnum(SizeModeRef.class).isOneOf(SizeMode.FIXED_SIZE));
        }
    }

    static final class IsOutputAsColumnsAndGuessSize implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.AS_COLUMNS)
                .and(i.getEnum(SizeModeRef.class).isOneOf(SizeMode.GUESS_SIZE));
        }
    }


    static final class IsScanLimitEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ScanLimitEnabledRef.class).isTrue();
        }
    }

    static final class IsOutputAsColumnsAndGuessSizeAndScanLimitEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputModeRef.class).isOneOf(OutputMode.AS_COLUMNS)
                .and(i.getEnum(SizeModeRef.class).isOneOf(SizeMode.GUESS_SIZE)).and(i.getPredicate(ScanLimitEnabledRef.class));
        }
    }

    // ===== CUSTOM PERSISTORS =====

    static final class OutputModePersistor implements NodeParametersPersistor<OutputMode> {
        private static final String CFG_OUTPUT_AS_LIST = "outputAsList";
        private static final String CFG_OUTPUT_AS_SET = "outputAsSet";
        private static final String CFG_OUTPUT_AS_COLUMNS = "outputAsColumns";

        @Override
        public void save(final OutputMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_OUTPUT_AS_LIST, obj == OutputMode.AS_LIST);
            settings.addBoolean(CFG_OUTPUT_AS_SET, obj == OutputMode.AS_SET);
            settings.addBoolean(CFG_OUTPUT_AS_COLUMNS, obj == OutputMode.AS_COLUMNS);
        }

        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean outputAsList = settings.getBoolean(CFG_OUTPUT_AS_LIST, false);
            boolean outputAsSet = settings.getBoolean(CFG_OUTPUT_AS_SET, false);

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
            return new String[][]{{CFG_OUTPUT_AS_LIST}, {CFG_OUTPUT_AS_SET}, {CFG_OUTPUT_AS_COLUMNS}};
        }
    }

    static final class SizeModePersistor implements NodeParametersPersistor<SizeMode> {
        private static final String CFG_GUESS_NUM_OF_COLS = "guessNumOfCols";

        @Override
        public void save(final SizeMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(CFG_GUESS_NUM_OF_COLS, obj == SizeMode.GUESS_SIZE);
        }

        @Override
        public SizeMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean guessNumOfCols = settings.getBoolean(CFG_GUESS_NUM_OF_COLS, false);
            return guessNumOfCols ? SizeMode.GUESS_SIZE : SizeMode.FIXED_SIZE;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_GUESS_NUM_OF_COLS}};
        }
    }


    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Select a column",
            description = "Select the column whose values are split.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Persist(configKey = "colName")
    String columnName;

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Remove input column",
            description = "When checked, the selected input column will not be part of the output table.")
    @Persist(configKey = "removeInputColumn")
    boolean removeInputColumn = false;

    // ===== SETTINGS SECTION =====

    @Layout(SettingsSection.class)
    @Widget(title = "Enter a delimiter",
            description = "Specify the delimiter in the value, thatsplits each part.")
    @TextInputWidget(placeholder = "")
    @Persist(configKey = "delimiter")
    String delimiter = "";

    @Layout(SettingsSection.class)
    @Widget(title = "Use \\ as escape character",
            description = "If enabled, the backslash (\\) can be used to escape characters, such as \\t for tabs. " +
                         "You can use the full escape capabilities of Java.")
    @Persist(configKey = "useEscapeCharacter")
    boolean useEscapeCharacter = false;

    @Layout(SettingsSection.class)
    @Widget(title = "Enter a quotation character",
            description = "Specify the quotation character if the different parts in the value are quoted. " +
                         "(The character to escape quotes is always the backslash.) If no quotation character is needed leave it empty.")
    @TextInputWidget(placeholder = "(leave empty for none)")
    @Persist(configKey = CellSplitter2UserSettings.CFG_QUOTES)
    String quotePattern = "";


    boolean m_removeQuotes = true;

    @Layout(SettingsSection.class)
    @Widget(title = "Remove leading and trailing white space chars (trim)",
            description = "If checked, leading and trailing white spaces of each part (token) will be deleted.")
    @Persist(configKey = "removeWhitespaces")
    boolean trim = true;

    // ===== OUTPUT SECTION =====

    @Layout(OutputSection.class)
    @Widget(title = "Output format",
            description = "Select how the split results should be output: as a list collection, as a set collection (duplicates removed), or as separate columns.")
    @RadioButtonsWidget
    @ValueReference(OutputModeRef.class)
    @Persistor(OutputModePersistor.class)
    OutputMode outputMode = OutputMode.AS_COLUMNS;

    @Layout(OutputSection.class)
    @Widget(title = "Split input column name for output column names",
            description = "When outputting as new columns, check this option when the input column name can be split " +
                         "in the same manner as the column's content to obtain the names for the output columns.")
    @Effect(predicate = IsOutputAsColumns.class, type = EffectType.SHOW)
    @Persist(configKey = "splitColumnNames")
    boolean splitColumnNames = false;

    @Layout(OutputSection.class)
    @Widget(title = "Size determination",
            description = "Choose whether to specify a fixed number of output columns or to automatically determine the size.")
    @RadioButtonsWidget
    @ValueReference(SizeModeRef.class)
    @Effect(predicate = IsOutputAsColumns.class, type = EffectType.SHOW)
    @Persistor(SizeModePersistor.class)
    SizeMode sizeMode = SizeMode.FIXED_SIZE;

    @Layout(OutputSection.class)
    @Widget(title = "Number of columns",
            description = "Specify the number of columns to append. All created columns will be of type String.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsOutputAsColumnsAndFixedSize.class, type = EffectType.SHOW)
    @Persist(configKey = "numberOfCols")
    int numberOfColumns = 6;

    @Layout(OutputSection.class)
    @Widget(title = "Scan limit (number of lines to guess on)",
            description = "Maximum number of rows to scan for guessing the numberof output columns.")
    @Effect(predicate = IsOutputAsColumnsAndGuessSize.class, type = EffectType.SHOW)
    @ValueReference(ScanLimitEnabledRef.class)
    @Persist(configKey = "hasScanLimit")
    boolean hasScanLimit = false;

    @Layout(OutputSection.class)
    @Widget(title = "Maximum rows to scan",
            description = "Maximum number of rows to scan for guessing the number of output columns and their types.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = IsOutputAsColumnsAndGuessSizeAndScanLimitEnabled.class, type = EffectType.SHOW)
    @Persist(configKey = "scanLimit")
    int scanLimit = 50;

    // ===== MISSING VALUE HANDLING SECTION =====

    @Layout(MissingValueHandlingSection.class)
    @Widget(title = "Create empty string cells instead of missing string cells",
            description = "If checked, empty string cells are created for missing or short input cells instead of missing value cells.")
    @Persist(configKey = "useEmptyString")
    boolean useEmptyString = true;
}
