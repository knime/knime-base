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
 *   Oct 16, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderParameters.CustomRowDelimiterPatternValidation.combineMultiCharExceptions;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.OSIndependentNewLineReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.ProviderFromCSVFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.ColumnAndDataTypeDetection.LimitScannedRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.ColumnAndDataTypeDetection.MaximumNumberOfColumnsAndLimitMemoryPerColumn;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.DataArea.FirstRowContainsColumnNames;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.DataArea.IfRowHasLessColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.File.FileEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.FileFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.FileFormat.AutodetectFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.FileFormat.QuoteCharactersHorizontal.QuoteCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.FileFormat.QuoteCharactersHorizontal.QuoteEscapeCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.MulitpleFileHandling.PrependFileIndexToRowId;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderLayoutAdditions.Values;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation.HasAtMaxOneCharValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

/**
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class CSVTableReaderParameters implements NodeParameters {

    CSVTableReaderParameters(final URL url) {
        if (url.toString().endsWith(".tsv")) { //NOSONAR
            m_columnDelimiter = "\\t";
        }
    }

    CSVTableReaderParameters() {
        // default constructor
    }

    enum FileEncodingOption {
            @Label(value = "OS default", description = "Uses the default decoding set by the operating system.") //
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
            @Label(value = "ISO-8859-1", description = "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.") //
            ISO_8859_1("ISO-8859-1"), //
            @Label(value = "US-ASCII", description = "Seven-bit ASCII, also referred to as US-ASCII.") //
            US_ASCII("US-ASCII"), //
            @Label(value = "UTF-8", description = "Eight-bit UCS Transformation Format.") //
            UTF_8("UTF-8"), //
            @Label(value = "UTF-16",
                description = "Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark in the file.") //
            UTF_16("UTF-16"), //
            @Label(value = "UTF-16BE", description = "Sixteen-bit UCS Transformation Format, big-endian byte order.") //
            UTF_16BE("UTF-16BE"), //
            @Label(value = "UTF-16LE", description = "Sixteen-bit UCS Transformation Format, little-endian byte order.") //
            UTF_16LE("UTF-16LE"), //
            @Label(value = "Other", description = "Enter a valid charset name supported by the Java Virtual Machine.") //
            OTHER("");

        final String m_charsetName;

        final String m_nonConstantDisplayText;

        FileEncodingOption(final String persistId) {
            this(persistId, null);
        }

        FileEncodingOption(final String charsetName, final String nonConstantDisplayText) {
            m_charsetName = charsetName;
            m_nonConstantDisplayText = nonConstantDisplayText;
        }

        static FileEncodingOption fromCharsetName(final String charsetName) {
            return Arrays.stream(FileEncodingOption.values())
                .filter(fileEncoding -> Objects.equals(fileEncoding.m_charsetName, charsetName)).findFirst()
                .orElse(OTHER);
        }

        EnumChoice<FileEncodingOption> toEnumChoice() {
            if (m_nonConstantDisplayText == null) {
                return EnumChoice.fromEnumConst(this);
            }
            return new EnumChoice<>(this, m_nonConstantDisplayText);
        }
    }

    /**
     * This provider is needed to display the non-constant display text of the default option.
     */
    static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {
        @Override
        public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
            return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
        }
    }

    static class FileEncodingRef extends ReferenceStateProvider<FileEncodingOption> {
    }

    @Widget(title = "File encoding", description = """
            Defines the character set used to read a CSV file that contains characters in a different encoding. You \
            can choose from a list of character encodings (UTF-8, UTF-16, etc.), or specify any other encoding \
            supported by your Java Virtual Machine (VM). The default value uses the default encoding of the Java VM, \
            which may depend on the locale or the Java property &quot;file.encoding&quot;.
            """, advanced = true)
    @ValueReference(FileEncodingRef.class)
    @ChoicesProvider(EncodingChoicesProvider.class)
    @Layout(FileEncoding.class)
    FileEncodingOption m_fileEncoding = FileEncodingOption.UTF_8;

    static final class IsOtherEncoding implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
        }
    }

    static class CustomEncodingRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Custom encoding", description = "A custom character set used to read a CSV file.", advanced = true)
    @ValueReference(CustomEncodingRef.class)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    @Layout(FileEncoding.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_customEncoding = "";

    static final class SkipFirstLinesRef extends ReferenceStateProvider<Long> {
    }

    @Widget(title = "Skip first lines of file", description = """
            Use this option to skip lines that do not fit in the table structure (e.g. multi-line comments).
            <br/>
            The specified number of lines are skipped in the input file before the parsing starts. Skipping lines
            prevents parallel reading of individual files.
            """)
    @ValueReference(SkipFirstLinesRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(FileFormat.class)
    long m_skipFirstLines;

    static final class CommentStartRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Comment line character", description = "Defines the character indicating line comments.")
    @ValueReference(CommentStartRef.class)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(FileFormat.class)
    String m_commentLineCharacter = "#";

    enum RowDelimiterOption {
            @Label(value = "Line break",
                description = "Uses the line break character as row delimiter. This option is platform-agnostic.") //
            LINE_BREAK, //
            @Label(value = "Custom", description = "Uses the provided string as row delimiter.") //
            CUSTOM; //
    }

    static final class RowDelimiterOptionProvider extends ProviderFromCSVFormat<RowDelimiterOption> {
        @Override
        public RowDelimiterOption computeState(final NodeParametersInput context) {
            return OSIndependentNewLineReader.isLineBreak(getCsvFormat().getLineSeparatorString())
                ? RowDelimiterOption.LINE_BREAK : RowDelimiterOption.CUSTOM;
        }
    }

    static class RowDelimiterOptionRef extends ReferenceStateProvider<RowDelimiterOption> {
    }

    static final class HasCustomRowDelimiter implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RowDelimiterOptionRef.class).isOneOf(RowDelimiterOption.CUSTOM);
        }
    }

    @Widget(title = "Row delimiter",
        description = "Defines the character string delimiting rows. Can get detected automatically.")
    @ValueSwitchWidget
    @Layout(FileFormat.class)
    @ValueReference(RowDelimiterOptionRef.class)
    @ValueProvider(RowDelimiterOptionProvider.class)
    RowDelimiterOption m_rowDelimiterOption = RowDelimiterOption.LINE_BREAK;

    static final class CustomRowDelimiterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return EscapeUtils.escape(getCsvFormat().getLineSeparatorString());
        }
    }

    static class CustomRowDelimiterRef extends ReferenceStateProvider<String> {
    }

    static final class CustomRowDelimiterPatternValidation extends PatternValidation {

        static final List<String> ALLOWED_MULTICHAR_DELIMITERS = List.of(//
            "\\t", //
            "\\n", //
            "\\r", //
            "\\r\\n" //
        );

        @Override
        protected String getPattern() {
            return ".|\\\\(t|r|n)|\\\\r\\\\n";
        }

        @Override
        public String getErrorMessage() {
            return String.format("The value must be a single character, %s.", combineMultiCharExceptions());
        }

        static String combineMultiCharExceptions() {
            final var quotedItems = ALLOWED_MULTICHAR_DELIMITERS.stream().map(s -> "“" + s + "”").toList();
            final StringBuilder sb = new StringBuilder();
            final var numItems = quotedItems.size();
            for (int i = 0; i < numItems; i++) {
                sb.append(quotedItems.get(i));
                if (i < numItems - 2) {
                    sb.append(", ");
                } else if (i == numItems - 2) {
                    sb.append(" or ");
                }
            }
            return sb.toString();
        }

    }

    private void validateCustomRowDelimiter() {
        if (CustomRowDelimiterPatternValidation.ALLOWED_MULTICHAR_DELIMITERS.contains(m_customRowDelimiter)) {
            return;
        }
        if (m_customRowDelimiter.isEmpty()) {
            throw new IllegalArgumentException("The custom row delimiter must not be empty.");
        }
        if (m_customRowDelimiter.length() != 1) {
            throw new IllegalArgumentException(String.format("The custom row delimiter must be a single character, %s.",
                combineMultiCharExceptions()));
        }

    }

    @Widget(title = "Custom row delimiter", description = "Defines the character to be used as custom row delimiter.")
    @TextInputWidget(patternValidation = CustomRowDelimiterPatternValidation.class)
    @Layout(FileFormat.class)
    @Effect(predicate = HasCustomRowDelimiter.class, type = EffectType.SHOW)
    @ValueReference(CustomRowDelimiterRef.class)
    @ValueProvider(CustomRowDelimiterProvider.class)
    String m_customRowDelimiter = "\n";

    static final class BufferSizeRef implements ParameterReference<Integer> {
    }

    static final class ColumnDelimiterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return EscapeUtils.escape(getCsvFormat().getDelimiterString());
        }
    }

    static class ColumnDelimiterRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Column delimiter", description = """
            Defines the character string delimiting columns. Use '\t' for tab characters. Can get detected
            automatically.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Layout(FileFormat.class)
    @ValueReference(ColumnDelimiterRef.class)
    @ValueProvider(ColumnDelimiterProvider.class)
    String m_columnDelimiter = ",";

    @Widget(title = "Quoted strings contain no row delimiters", description = """
            Check this box if there are no quotes that contain row delimiters inside the files. Row delimiters
            should not be inside of quotes for parallel reading of individual files.
            """)
    @Layout(FileFormat.class)
    boolean m_quotedStringsContainNoRowDelimiters;

    static final class QuoteCharacterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return Character.toString(getCsvFormat().getQuote());
        }
    }

    static class QuoteCharacterRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Quote character", description = "The character indicating quotes. Can get detected automatically.")
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(QuoteCharacter.class)
    @ValueReference(QuoteCharacterRef.class)
    @ValueProvider(QuoteCharacterProvider.class)
    String m_quoteCharacter = "\"";

    static final class QuoteEscapeCharacterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return Character.toString(getCsvFormat().getQuoteEscape());
        }
    }

    static class QuoteEscapeCharacterRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Quote escape character", description = """
            The character used for escaping quotes inside an already quoted value. Can get detected
            automatically.
            """)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(QuoteEscapeCharacter.class)
    @ValueReference(QuoteEscapeCharacterRef.class)
    @ValueProvider(QuoteEscapeCharacterProvider.class)
    String m_quoteEscapeCharacter = "\"";

    static final class AutoDetectButtonRef implements ButtonReference {
    }

    @Widget(title = "Autodetect format", description = """
            By pressing this button, the format of the file will be guessed automatically. It is not guaranteed that
            the correct values are being detected.
            """)
    @Layout(AutodetectFormat.class)
    @SimpleButtonWidget(ref = AutoDetectButtonRef.class, icon = Icon.RELOAD)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.DISABLE)
    Void m_autoDetectButton;

    @Widget(title = "Number of characters for autodetection", description = """
            Specifies on how many characters of the selected file should be used for autodetection. The
            autodetection by default is based on the first 1024 * 1024 characters.
            """, advanced = true)
    @ValueReference(BufferSizeRef.class)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(AutodetectFormat.class)
    int m_numberOfCharactersForAutodetection = CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE;

    // TODO will be moved into a settings panel in UIEXT-1739
    static class FirstRowContainsColumnNamesRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "First row contains column names",
        description = "Select this box if the first row contains column name headers.")
    @ValueReference(FirstRowContainsColumnNamesRef.class)
    @Layout(FirstRowContainsColumnNames.class)
    boolean m_firstRowContainsColumnNames = true;

    enum IfRowHasLessColumnsOption {
            @Label(value = "Fail",
                description = "if there are shorter rows in the input file the node execution fails.") //
            FAIL, //
            @Label(value = "Insert missing", description = "the shorter rows are completed with missing values.") //
            INSERT_MISSING; //
    }

    @Widget(title = "If row has fewer columns",
        description = "Specifies the behavior in case some rows are shorter than others. ")
    @ValueSwitchWidget
    @Layout(IfRowHasLessColumns.class)
    IfRowHasLessColumnsOption m_ifRowHasLessColumnsOption = IfRowHasLessColumnsOption.FAIL;

    static class DecimalSeparatorRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Decimal separator", description = """
            Specifies the decimal separator character for parsing numbers. The decimal separator is only used for
            the parsing of double values. Note that the decimal separator must differ from the thousands separator.
            You must always provide a decimal separator.
            """)
    @ValueReference(DecimalSeparatorRef.class)
    @TextInputWidget(patternValidation = IsSingleCharacterValidation.class)
    @Layout(Values.class)
    String m_decimalSeparator = ".";

    static class ThousandsSeparatorRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Thousands separator", description = """
            Specifies the thousands separator character for parsing numbers. The thousands separator is used for
            integer, long and double parsing. Note that the thousands separator must differ from the decimal
            separator. It is possible to leave the thousands separator unspecified.
            """)
    @ValueReference(ThousandsSeparatorRef.class)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(Values.class)
    String m_thousandsSeparator = "";

    static class ReplaceEmptyQuotedStringsByMissingValuesRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Replace empty quoted string by missing values",
        description = "Select this box if you want <b>quoted</b> empty strings to be replaced by missing value cells.")
    @ValueReference(ReplaceEmptyQuotedStringsByMissingValuesRef.class)
    @Layout(Values.class)
    boolean m_replaceEmptyQuotedStringsByMissingValues = true;

    enum QuotedStringsOption {
            @Label(value = "Remove quotes and trim whitespace",
                description = "Quotes will be removed from the value followed by trimming any leading/trailing whitespaces.") //
            REMOVE_QUOTES_AND_TRIM, //
            @Label(value = "Keep quotes",
                description = "Quotes of a value will be kept. Note: No trimming will be done inside the quotes.") //
            KEEP_QUOTES; //
    }

    static class QuotedStringsOptionRef extends ReferenceStateProvider<QuotedStringsOption> {
    }

    @Widget(title = "Quoted strings",
        description = "Specifies the behavior in case there are quoted strings in the input table.", advanced = true)
    @ValueReference(QuotedStringsOptionRef.class)
    @RadioButtonsWidget
    @Layout(Values.class)
    QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

    static final class MaxDataRowsScannedDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 10000L;
        }
    }

    static class MaxDataRowsScannedRef extends ReferenceStateProvider<Optional<Long>> {
    }

    @Widget(title = "Limit scanned rows", description = """
            If enabled, only the specified number of input <i>rows</i> are used to analyze the file (i.e to
            determine the column types). This option is recommended for long files where the first <i>n</i> rows are
            representative for the whole file. The "Skip first data rows" option has no effect on the scanning. Note
            also, that this option and the "Limit number of rows" option are independent from each other, i.e., if
            the value in "Limit number of rows" is smaller than the value specified here, we will still read as many
            rows as specified here.
            """)
    @ValueReference(MaxDataRowsScannedRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(LimitScannedRows.class)
    @OptionalWidget(defaultProvider = MaxDataRowsScannedDefaultProvider.class)
    Optional<Long> m_maxDataRowsScanned = Optional.of(10000L);

    static class MaximumNumberOfColumnsRef extends ReferenceStateProvider<Integer> {
    }

    @Widget(title = "Maximum number of columns", description = """
            Sets the number of allowed columns (default 8192 columns) to prevent memory exhaustion. The node will
                        fail if the number of columns exceeds the set limit.
            """)
    @ValueReference(MaximumNumberOfColumnsRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(MaximumNumberOfColumnsAndLimitMemoryPerColumn.class)
    int m_maximumNumberOfColumns = 8192;

    static class LimitMemoryPerColumnRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Limit memory per column", description = """
            If selected the memory per column is restricted to 1MB in order to prevent memory exhaustion. Uncheck
            this option to disable these memory restrictions.
            """)
    @ValueReference(LimitMemoryPerColumnRef.class)
    @Layout(MaximumNumberOfColumnsAndLimitMemoryPerColumn.class)
    boolean m_limitMemoryPerColumn = true;

    @Widget(title = "Prepend file index to RowID", description = """
            Select this box if you want to prepend a prefix that depends on the index of the source file to the
            RowIDs. The prefix for the first file is "File_0_", for the second "File_1_" and so on. This option is
            useful if the RowIDs within a single file are unique but the same RowIDs appear in multiple files.
            Prepending the file index prevents parallel reading of individual files.
            """)
    @Layout(PrependFileIndexToRowId.class)
    boolean m_prependFileIndexToRowId;
    // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    static String fileEncodingToCharsetName(final FileEncodingOption encoding, final String customEncoding) {
        return encoding == FileEncodingOption.OTHER ? customEncoding : encoding.m_charsetName;
    }

    void saveToConfig(final CSVMultiTableReadConfig config) {

        final var tableReadConfig = config.getTableReadConfig();
        final var csvConfig = tableReadConfig.getReaderSpecificConfig();

        csvConfig.setCharSetName(fileEncodingToCharsetName(m_fileEncoding, m_customEncoding));

        csvConfig.setSkipLines(m_skipFirstLines > 0);
        csvConfig.setNumLinesToSkip(m_skipFirstLines);

        csvConfig.setComment(m_commentLineCharacter);

        csvConfig.useLineBreakRowDelimiter(m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);
        csvConfig.setLineSeparator(EscapeUtils.unescape(m_customRowDelimiter));

        csvConfig.setDelimiter(EscapeUtils.unescape(m_columnDelimiter));

        csvConfig.noRowDelimitersInQuotes(m_quotedStringsContainNoRowDelimiters);

        csvConfig.setQuote(m_quoteCharacter);

        csvConfig.setQuoteEscape(m_quoteEscapeCharacter);

        csvConfig.setAutoDetectionBufferSize(m_numberOfCharactersForAutodetection);

        tableReadConfig.setColumnHeaderIdx(0L);
        tableReadConfig.setUseColumnHeaderIdx(m_firstRowContainsColumnNames);

        tableReadConfig.setAllowShortRows(m_ifRowHasLessColumnsOption == IfRowHasLessColumnsOption.INSERT_MISSING);

        csvConfig.setDecimalSeparator(m_decimalSeparator);

        csvConfig.setThousandsSeparator(m_thousandsSeparator);

        csvConfig.setReplaceEmptyWithMissing(m_replaceEmptyQuotedStringsByMissingValues);

        csvConfig.setQuoteOption(m_quotedStringsOption == QuotedStringsOption.REMOVE_QUOTES_AND_TRIM
            ? QuoteOption.REMOVE_QUOTES_AND_TRIM : QuoteOption.KEEP_QUOTES);

        tableReadConfig.setLimitRowsForSpec(m_maxDataRowsScanned.isPresent());
        tableReadConfig.setMaxRowsForSpec(m_maxDataRowsScanned.orElse(0L));

        csvConfig.setMaxColumns(m_maximumNumberOfColumns);

        csvConfig.limitCharsPerColumn(m_limitMemoryPerColumn);

        tableReadConfig.setPrependSourceIdxToRowId(m_prependFileIndexToRowId);
    }

    /** The annotations handle the validation for the dialog, we need to repeat them here for the model. */
    @Override
    public void validate() throws InvalidSettingsException {

        // m_customEncoding: IsNotBlankValidation (only when OTHER encoding is selected)
        if (m_fileEncoding == FileEncodingOption.OTHER && m_customEncoding.isBlank()) {
            throw new InvalidSettingsException("The specified custom encoding must not be empty.");
        }

        // m_skipFirstLines: IsNonNegativeValidation
        if (m_skipFirstLines < 0) {
            throw new InvalidSettingsException("The number of lines to skip must be non-negative.");
        }

        // m_commentLineCharacter: HasAtMaxOneCharValidation
        if (m_commentLineCharacter.length() > 1) {
            throw new InvalidSettingsException("The comment line character must be at most one character.");
        }

        // m_customRowDelimiter: CustomRowDelimiterPatternValidation (pattern: .|\\(t|r|n)|\\r\\n)
        // Only validated when CUSTOM row delimiter is selected
        if (m_rowDelimiterOption == RowDelimiterOption.CUSTOM) {
            validateCustomRowDelimiter();
        }

        // m_columnDelimiter: IsNotEmptyValidation
        if (m_columnDelimiter.isEmpty()) {
            throw new InvalidSettingsException("The column delimiter must not be empty.");
        }

        // m_quoteCharacter: HasAtMaxOneCharValidation
        if (m_quoteCharacter.length() > 1) {
            throw new InvalidSettingsException("The quote character must be at most one character.");
        }

        // m_quoteEscapeCharacter: HasAtMaxOneCharValidation
        if (m_quoteEscapeCharacter.length() > 1) {
            throw new InvalidSettingsException("The quote escape character must be at most one character.");
        }

        // m_numberOfCharactersForAutodetection: IsPositiveIntegerValidation
        if (m_numberOfCharactersForAutodetection <= 0) {
            throw new InvalidSettingsException("The number of characters for autodetection must be positive.");
        }

        // m_decimalSeparator: IsSingleCharacterValidation
        if (m_decimalSeparator.length() != 1) {
            throw new InvalidSettingsException("The decimal separator must be exactly one character.");
        }

        // m_thousandsSeparator: HasAtMaxOneCharValidation
        if (m_thousandsSeparator.length() > 1) {
            throw new InvalidSettingsException("The thousands separator must be at most one character.");
        }

        // m_maxDataRowsScanned: IsNonNegativeValidation (when present)
        if (m_maxDataRowsScanned.isPresent() && m_maxDataRowsScanned.get() < 0) {
            throw new InvalidSettingsException("The maximum number of data rows scanned must be non-negative.");
        }

        // m_maximumNumberOfColumns: IsNonNegativeValidation
        if (m_maximumNumberOfColumns < 0) {
            throw new InvalidSettingsException("The maximum number of columns must be non-negative.");
        }
    }

}
