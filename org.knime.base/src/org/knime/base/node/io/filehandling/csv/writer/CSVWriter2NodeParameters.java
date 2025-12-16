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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.io.filehandling.csv.writer;

import java.util.List;

import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.writer.config.AdvancedConfig.QuoteMode;
import org.knime.base.node.io.filehandling.csv.writer.config.LineBreakTypes;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.LegacyPredicateInitializer;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextAreaWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;

/**
 * Node parameters for CSV Writer.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class CSVWriter2NodeParameters implements NodeParameters {

    private static final String CFG_COMPRESS_WITH_GZIP = "compress_with_gzip";

    private static final String CFG_MISSING_VALUE_PATTERN = "missing_value_pattern";

    private static final String CFG_KEEP_TRAILING_ZERO_IN_DECIMALS = "keep_trailing_zero_in_decimals";

    private static final String CFG_DECIMAL_SEPARATOR = "decimal_separator";

    private static final String CFG_QUOTE_MODE = "quote_mode";

    private static final String CFG_ADD_USER_TO_COMMENT = "add_user_to_comment";

    private static final String CFG_ADD_TIME_TO_COMMENT = "add_time_to_comment";

    private static final String CFG_ADD_TABLE_NAME_TO_COMMENT = "add_table_name_to_comment";

    private static final String CFG_ADD_CUSTOM_TEXT_TO_COMMENT = "add_custom_text_to_comment";

    private static final String CFG_CUSTOM_COMMENT_TEXT = "custom_comment_text";

    private static final String CFG_COMMENT_LINE_MARKER = "comment_line_marker";

    private static final String CFG_COMMENT_INDENTATION = "comment_indentation";

    private static final String CFG_ROW_DELIMITER = "row_delimiter";

    @Section(title = "Output File")
    interface OutputSection {
    }

    @Section(title = "File Format")
    @After(OutputSection.class)
    interface FormatSection {
        @HorizontalLayout
        interface Quotes { // NOSONAR
        }
    }

    @Section(title = "Values")
    @Advanced
    @After(FormatSection.class)
    interface ValuesSection {
    }

    @Section(title = "Comment Header")
    @Advanced
    @After(ValuesSection.class)
    interface CommentHeaderSection {
    }

    @Modification(OutputFileModification.class)
    @Persist(configKey = CSVWriter2Config.CFG_FILE_CHOOSER)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(OutputSection.class)
    @ValueReference(OutputFileRef.class)
    LegacyFileWriterWithOverwritePolicyOptions m_outputFile = new LegacyFileWriterWithOverwritePolicyOptions();

    @Layout(OutputSection.class)
    @Persist(configKey = CSVWriter2Config.CFG_ENCODING)
    @Advanced
    CsvFileEncodingParameters m_fileEncoding = new CsvFileEncodingParameters();

    @Widget(title = "Compress file (gzip)",
        description = "Check this if you want to compress the output file using gzip compression.")
    @Persist(configKey = CFG_COMPRESS_WITH_GZIP)
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(OutputSection.class)
    @Advanced
    boolean m_compressWithGzip;

    @Widget(title = "Column delimiter",
        description = "The character string delimiting columns. A single tab character can be entered as '\\t'")
    @Persistor(ColumnDelimiterPersistor.class)
    @Migrate
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(FormatSection.class)
    String m_columnDelimiter = ",";

    @Widget(title = "Row delimiter",
        description = "Select the line break variation you want to use as row delimiter while writing the CSV file.")
    @Persistor(LineBreakTypesPersistor.class)
    @Migrate
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(FormatSection.class)
    @Advanced
    LineBreakTypes m_rowDelimiter = LineBreakTypes.SYS_DEFAULT;

    @Widget(title = "Quote character",
        description = "The quote character used to enclose values with. See the \"Quote values\" option to specify "
            + "what kind of values should be enclosed within quotes.")
    @Persistor(QuoteCharPersistor.class)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Migrate
    @TextInputWidget(patternValidation = CharWithEscapeSequencesValidator.class)
    @Layout(FormatSection.Quotes.class)
    @Advanced
    String m_quoteChar = "\"";

    @Widget(title = "Quote escape character",
        description = "The character is used for escaping quotes inside an already quoted value.")
    @Persistor(QuoteEscapeCharPersistor.class)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Migrate
    @TextInputWidget(patternValidation = CharWithEscapeSequencesValidator.class)
    @Layout(FormatSection.Quotes.class)
    @Advanced
    String m_quoteEscapeChar = "\"";

    @Widget(title = "Write column headers",
        description = "If checked, the column names will be written out in the first line of the output file.")
    @Persist(configKey = CSVWriter2Config.CFG_WRITE_COLUMN_HEADER)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(FormatSection.class)
    @ValueReference(WriteColumnHeaderRef.class)
    boolean m_writeColumnHeader = true;

    @Widget(title = "Don't write column headers if file exists", description = """
            If checked, the column headers will not be written when the output is appended to the already
            existing output file. This is particularly useful when the CSV Writer is used in a loop to report
            the results of each iteration.
            """)
    @Persist(configKey = CSVWriter2Config.CFG_SKIP_COLUMN_HEADER_ON_APPEND)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(FormatSection.class)
    @Effect(predicate = OverwritePolicyIsAppend.class, type = Effect.EffectType.SHOW)
    boolean m_skipColumnHeaderOnAppend;

    @Widget(title = "Write RowIDs",
        description = "If checked, the RowIDs will be added to the output, as first item in each line.")
    @Persist(configKey = CSVWriter2Config.CFG_WRITE_ROW_HEADER)
    @PersistWithin(CSVWriter2Config.CFG_SETTINGS_TAB)
    @Layout(FormatSection.class)
    boolean m_writeRowHeader;

    @Widget(title = "Quote values", description = "Specify when to enclose values with quotes:")
    @Persist(configKey = CFG_QUOTE_MODE)
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    @ValueSwitchWidget
    @ValueReference(QuoteModeRef.class)
    QuoteMode m_quoteMode = QuoteMode.STRINGS_ONLY;

    @Widget(title = "Replace column separator with",
        description = "Specify a replacement for the value used as a column separator. Used only if the 'Never' option "
            + "is selected for Quote values.")
    @Persist(configKey = "separator_replacement")
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    @Effect(predicate = QuoteModeIsNever.class, type = Effect.EffectType.SHOW)
    String m_separatorReplacement = "";

    @Widget(title = "Replace missing values with",
        description = "Set this option to replace missing values from the input table with specific text when writing.")
    @Persist(configKey = CFG_MISSING_VALUE_PATTERN)
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    String m_missingValuePattern = "";

    @Widget(title = "Number decimal separator",
        description = "Specify a character that is used as decimal separator for floating point numbers.")
    @Persist(configKey = CFG_DECIMAL_SEPARATOR)
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    char m_decimalSeparator = '.';

    @Widget(title = "Use scientific format for very large and very small numbers",
        description = "Check this if you want very large and very small floating point numbers to be written in "
            + "scientific notation (e.g., 1.0E9 instead of 1,000,000,000).")
    @Persist(configKey = "use_scientific_format")
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    boolean m_useScientificFormat;

    @Widget(title = "Append .0 suffix for decimal numbers without fractions", description = """
            Check this if you want to write every decimal value with .0 suffix even though the value doesn't contain
            any fractional part. (e.g., the value 12 will be written as 12.0 if it is coming from a decimal column).
            Use with caution as this could lead to unnecessary increase of the file size. This doesn't affect the way
            to write values from integer columns. The option is ignored if we are using a scientific format.
            """)
    @Persist(configKey = CFG_KEEP_TRAILING_ZERO_IN_DECIMALS)
    @PersistWithin(CSVWriter2Config.CFG_ADVANCED)
    @Layout(ValuesSection.class)
    boolean m_keepTrailingZero;

    @Widget(title = "Write name of file creator (KNIME username)",
        description = "If checked, the login name of the user that created the file (probably you!) is inserted.")
    @Persist(configKey = CFG_ADD_USER_TO_COMMENT)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @ValueReference(AddUserToCommentRef.class)
    boolean m_addUserToComment;

    @Widget(title = "Write execution time",
        description = "If checked, a comment line with the current execution time and date is added.")
    @Persist(configKey = CFG_ADD_TIME_TO_COMMENT)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @ValueReference(AddTimeToCommentRef.class)
    boolean m_addTimeToComment;

    @Widget(title = "Write input table name",
        description = "If checked, a comment line is added showing the name of the table the data was read from.")
    @Persist(configKey = CFG_ADD_TABLE_NAME_TO_COMMENT)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @ValueReference(AddTableNameToCommentRef.class)
    boolean m_addTableNameToComment;

    @Widget(title = "Write custom text",
        description = "If you check this, you can enter any free text, that will be added then to the comment header.")
    @Persist(configKey = CFG_ADD_CUSTOM_TEXT_TO_COMMENT)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @ValueReference(AddCustomTextToCommentRef.class)
    boolean m_addCustomTextToComment;

    @Widget(title = "Custom text", description = "Enter the custom text to be added to the comment header.")
    @Persist(configKey = CFG_CUSTOM_COMMENT_TEXT)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @Effect(predicate = AddCustomTextEnabled.class, type = Effect.EffectType.SHOW)
    @TextAreaWidget
    String m_customCommentText = "";

    @Widget(title = "Comment line marker",
        description = "If you have checked at least one of the content options, you must provide a comment line marker "
            + "that is used as a prefix for each comment line.")
    @Persist(configKey = CFG_COMMENT_LINE_MARKER)
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @Effect(predicate = AnyCommentOptionEnabled.class, type = Effect.EffectType.SHOW)
    String m_commentLineMarker = "#";

    @Widget(title = "Comment indentation",
        description = "Specify an indentation string, i.e., a prefix for every comment line."
            + "A single tab character can be entered as '\\t'.")
    @Persistor(CommentIndentationPersistor.class)
    @Migrate
    @PersistWithin(CSVWriter2Config.CFG_COMMENT)
    @Layout(CommentHeaderSection.class)
    @Effect(predicate = AnyCommentOptionEnabled.class, type = Effect.EffectType.SHOW)
    String m_commentIndent = "\\t";

    @Migration(LoadFalseIfAbsent.class)
    // only if old nodes should also get this validation when opened again:
    @ValueProvider(LoadTrueOnOpenDialog.class)
    boolean m_performNewValidation = true;

    private static final class OutputFileModification implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {
        private static final class CsvOverwritePolicyChoicesProvider
            extends LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider {

            @Override
            protected List<LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy> getChoices() {
                return List.of(OverwritePolicy.fail, OverwritePolicy.overwrite, OverwritePolicy.append);
            }
        }

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            restrictOverwritePolicyOptions(group, CsvOverwritePolicyChoicesProvider.class);
            var fileSelection = findFileSelection(group);
            // TODO UIEXT-3116 support ".csv", ".tsv", ".txt", ".csv.gz", ".tsv.gz", ".txt.gz"
            fileSelection.modifyAnnotation(FileWriterWidget.class) //
                .withProperty("fileExtension", "csv").modify();
            fileSelection.modifyAnnotation(Widget.class) //
                .withProperty("title", "Output file") //
                .withProperty("description", "The CSV file to write the data to.") //
                .modify();
        }
    }

    private static class OutputFileRef implements ParameterReference<LegacyFileWriterWithOverwritePolicyOptions> {
    }

    private static final class OverwritePolicyIsAppend implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return ((LegacyPredicateInitializer)i).getLegacyFileWriter(OutputFileRef.class) //
                .getOverwritePolicy().isOneOf(OverwritePolicy.append);
        }
    }

    private static class WriteColumnHeaderRef implements ParameterReference<Boolean> {
    }

    private static final class LineBreakTypesPersistor implements NodeParametersPersistor<LineBreakTypes> {

        @Override
        public LineBreakTypes load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return LineBreakTypes.loadSettings(settings);
        }

        @Override
        public void save(final LineBreakTypes obj, final NodeSettingsWO settings) {
            obj.saveSettings(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ROW_DELIMITER}};
        }
    }

    /**
     * Allow a single character or an escape sequence as defined in {@link EscapeUtils}.
     */
    private static final class CharWithEscapeSequencesValidator extends PatternValidation {

        @Override
        protected String getPattern() {
            return "(\\\\t|\\\\n|\\\\r|\\\\r\\\\n|.)";
        }

        @Override
        public String getErrorMessage() {
            return "This field must contain a single character. "
                + "A limited set of escape sequences (\\t, \\n, \\r, \\r\\n) is supported.";
        }

    }

    private abstract static class CharWithEscapeSequencesPersistor implements NodeParametersPersistor<String> {

        protected abstract String getConfigKey();

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var rawCharValue = settings.getChar(getConfigKey());
            return EscapeUtils.escape(String.valueOf(rawCharValue));
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            var rawValue = EscapeUtils.unescape(param);
            if (rawValue.length() == 0) {
                settings.addChar(getConfigKey(), '\0');
            } else {
                settings.addChar(getConfigKey(), rawValue.charAt(0));
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{getConfigKey()}};
        }

    }

    private static class QuoteCharPersistor extends CharWithEscapeSequencesPersistor {

        @Override
        protected String getConfigKey() {
            return CSVWriter2Config.CFG_QUOTE_CHAR;
        }
    }

    private static class QuoteEscapeCharPersistor extends CharWithEscapeSequencesPersistor {

        @Override
        protected String getConfigKey() {
            return CSVWriter2Config.CFG_QUOTE_ESCAPE_CHAR;
        }
    }

    private abstract static class StringWithEscapeSequencesPersistor implements NodeParametersPersistor<String> {

        protected abstract String getConfigKey();

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var rawValue = settings.getString(getConfigKey());
            return EscapeUtils.escape(rawValue);
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            var rawValue = EscapeUtils.unescape(param);
            settings.addString(getConfigKey(), rawValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{getConfigKey()}};
        }
    }

    private static class ColumnDelimiterPersistor extends StringWithEscapeSequencesPersistor {

        @Override
        protected String getConfigKey() {
            return CSVWriter2Config.CFG_COLUMN_DELIMITER;
        }
    }

    private static class CommentIndentationPersistor extends StringWithEscapeSequencesPersistor {

        @Override
        protected String getConfigKey() {
            return CFG_COMMENT_INDENTATION;
        }
    }

    private static class QuoteModeRef implements ParameterReference<QuoteMode> {
    }

    private static final class QuoteModeIsNever implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(QuoteModeRef.class).isOneOf(QuoteMode.NEVER);
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_fileEncoding.validate();
    }

    private static class AddUserToCommentRef implements ParameterReference<Boolean> {
    }

    private static class AddTimeToCommentRef implements ParameterReference<Boolean> {
    }

    private static class AddTableNameToCommentRef implements ParameterReference<Boolean> {
    }

    private static class AddCustomTextToCommentRef implements ParameterReference<Boolean> {
    }

    private static final class AddCustomTextEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AddCustomTextToCommentRef.class).isTrue();
        }
    }

    private static final class AnyCommentOptionEnabled implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return (i.getBoolean(AddUserToCommentRef.class).isTrue())
                .or(i.getBoolean(AddTimeToCommentRef.class).isTrue())
                .or(i.getBoolean(AddTableNameToCommentRef.class).isTrue())
                .or(i.getBoolean(AddCustomTextToCommentRef.class).isTrue());
        }
    }

    private static final class LoadFalseIfAbsent implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return false;
        }

    }

    private static final class LoadTrueOnOpenDialog implements StateProvider<Boolean> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return true;
        }

    }

}
