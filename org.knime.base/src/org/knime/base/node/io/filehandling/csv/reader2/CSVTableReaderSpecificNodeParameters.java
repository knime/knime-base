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
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitMemoryPerColumn;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.LimitScannedRows;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.ColumnAndDataTypeDetection.MaximumNumberOfColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstRowContainsColumnNames;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.IfRowHasLessColumns;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.CustomEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.FileEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.AutodetectFormat;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.ColumnDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CommentLineCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.CustomRowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.NumberOfCharactersForAutodetection;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuoteCharacters.QuoteEscapeCharacter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.QuotedStringsContainNoRowDelimiters;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.RowDelimiter;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.FileFormat.SkipFirstLines;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.MulitpleFileHandling.PrependFileIndexToRowId;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.DecimalSeparator;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.QuotedStrings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ReplaceEmptyQuotedStringsByMissingValues;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.Values.ThousandsSeparator;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
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
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

/**
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class CSVTableReaderSpecificNodeParameters implements NodeParameters {

    CSVTableReaderSpecificNodeParameters(final URL url) {
        if (url.toString().endsWith(".tsv")) { //NOSONAR
            m_columnDelimiter = "\\t"; // TODO correctly escaped?
        }
    }

    CSVTableReaderSpecificNodeParameters() {
        // default constructor
    }

    enum FileEncodingOption {
            @Label(value = "OS default", description = FileEncoding.DESCRIPTION_DEFAULT) //
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
            @Label(value = "ISO-8859-1", description = FileEncoding.DESCRIPTION_ISO_8859_1) //
            ISO_8859_1("ISO-8859-1"), //
            @Label(value = "US-ASCII", description = FileEncoding.DESCRIPTION_US_ASCII) //
            US_ASCII("US-ASCII"), //
            @Label(value = "UTF-8", description = FileEncoding.DESCRIPTION_UTF_8) //
            UTF_8("UTF-8"), //
            @Label(value = "UTF-16", description = FileEncoding.DESCRIPTION_UTF_16) //
            UTF_16("UTF-16"), //
            @Label(value = "UTF-16BE", description = FileEncoding.DESCRIPTION_UTF_16BE) //
            UTF_16BE("UTF-16BE"), //
            @Label(value = "UTF-16LE", description = FileEncoding.DESCRIPTION_UTF_16LE) //
            UTF_16LE("UTF-16LE"), //
            @Label(value = "Other", description = FileEncoding.DESCRIPTION_OTHER) //
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

    @Widget(title = "File encoding", description = FileEncoding.DESCRIPTION, advanced = true)
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

    @Widget(title = "Custom encoding", description = CustomEncoding.DESCRIPTION, advanced = true)
    @ValueReference(CustomEncodingRef.class)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    @Layout(CustomEncoding.class)
    String m_customEncoding = "";

    static final class SkipFirstLinesRef extends ReferenceStateProvider<Long> {
    }

    @Widget(title = "Skip first lines of file", description = SkipFirstLines.DESCRIPTION)
    @ValueReference(SkipFirstLinesRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SkipFirstLines.class)
    long m_skipFirstLines;

    static final class CommentStartRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Comment line character", description = CommentLineCharacter.DESCRIPTION)
    @ValueReference(CommentStartRef.class)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(CommentLineCharacter.class)
    String m_commentLineCharacter = "#";

    enum RowDelimiterOption {
            @Label(value = "Line break", description = RowDelimiter.DESCRIPTION_LINE_BREAK) //
            LINE_BREAK, //
            @Label(value = "Custom", description = RowDelimiter.DESCRIPTION_CUSTOM) //
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

    @Widget(title = "Row delimiter", description = RowDelimiter.DESCRIPTION)
    @ValueSwitchWidget
    @Layout(RowDelimiter.class)
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
        @Override
        protected String getPattern() {
            return ".|\\\\(t|r|n)|\\\\r\\\\n";
        }

        @Override
        public String getErrorMessage() {
            return "The value must be a single character, “\\t”, ”\\n”, “\\r”, or “\\r\\n”.";
        }
    }

    @Widget(title = "Custom row delimiter", description = CustomRowDelimiter.DESCRIPTION)
    @TextInputWidget(patternValidation = CustomRowDelimiterPatternValidation.class)
    @Layout(CustomRowDelimiter.class)
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

    @Widget(title = "Column delimiter", description = ColumnDelimiter.DESCRIPTION)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Layout(ColumnDelimiter.class)
    @ValueReference(ColumnDelimiterRef.class)
    @ValueProvider(ColumnDelimiterProvider.class)
    String m_columnDelimiter = ",";

    @Widget(title = "Quoted strings contain no row delimiters",
        description = QuotedStringsContainNoRowDelimiters.DESCRIPTION)
    @Layout(QuotedStringsContainNoRowDelimiters.class)
    boolean m_quotedStringsContainNoRowDelimiters;

    static final class QuoteCharacterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return Character.toString(getCsvFormat().getQuote());
        }
    }

    static class QuoteCharacterRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Quote character", description = QuoteCharacter.DESCRIPTION)
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

    @Widget(title = "Quote escape character", description = QuoteEscapeCharacter.DESCRIPTION)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(QuoteEscapeCharacter.class)
    @ValueReference(QuoteEscapeCharacterRef.class)
    @ValueProvider(QuoteEscapeCharacterProvider.class)
    String m_quoteEscapeCharacter = "\"";

    @Widget(title = "Number of characters for autodetection",
        description = NumberOfCharactersForAutodetection.DESCRIPTION, advanced = true)
    @ValueReference(BufferSizeRef.class)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(NumberOfCharactersForAutodetection.class)
    int m_numberOfCharactersForAutodetection = CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE;
    // TODO will be moved into a settings panel in UIEXT-1739

    static final class AutoDetectButtonRef implements ButtonReference {
    }

    @Widget(title = "Autodetect format", description = AutodetectFormat.DESCRIPTION)
    @Layout(AutodetectFormat.class)
    @SimpleButtonWidget(ref = AutoDetectButtonRef.class, icon = Icon.RELOAD)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.DISABLE)
    Void m_autoDetectButton;

    static class FirstRowContainsColumnNamesRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "First row contains column names", description = FirstRowContainsColumnNames.DESCRIPTION)
    @ValueReference(FirstRowContainsColumnNamesRef.class)
    @Layout(FirstRowContainsColumnNames.class)
    boolean m_firstRowContainsColumnNames = true;

    enum IfRowHasLessColumnsOption {
            @Label(value = "Fail", description = IfRowHasLessColumns.DESCRIPTION_FAIL) //
            FAIL, //
            @Label(value = "Insert missing", description = IfRowHasLessColumns.DESCRIPTION_INSERT_MISSING) //
            INSERT_MISSING; //
    }

    @Widget(title = "If row has fewer columns", description = IfRowHasLessColumns.DESCRIPTION)
    @ValueSwitchWidget
    @Layout(IfRowHasLessColumns.class)
    IfRowHasLessColumnsOption m_ifRowHasLessColumnsOption = IfRowHasLessColumnsOption.FAIL;

    static class DecimalSeparatorRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Decimal separator", description = DecimalSeparator.DESCRIPTION)
    @ValueReference(DecimalSeparatorRef.class)
    @TextInputWidget(patternValidation = IsSingleCharacterValidation.class)
    @Layout(DecimalSeparator.class)
    String m_decimalSeparator = ".";

    static class ThousandsSeparatorRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Thousands separator", description = ThousandsSeparator.DESCRIPTION)
    @ValueReference(ThousandsSeparatorRef.class)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Layout(ThousandsSeparator.class)
    String m_thousandsSeparator = "";

    static class ReplaceEmptyQuotedStringsByMissingValuesRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Replace empty quoted string by missing values",
        description = ReplaceEmptyQuotedStringsByMissingValues.DESCRIPTION)
    @ValueReference(ReplaceEmptyQuotedStringsByMissingValuesRef.class)
    @Layout(ReplaceEmptyQuotedStringsByMissingValues.class)
    boolean m_replaceEmptyQuotedStringsByMissingValues = true;

    enum QuotedStringsOption {
            @Label(value = "Remove quotes and trim whitespace",
                description = QuotedStrings.DESCRIPTION_REMOVE_QUOTES_AND_TRIM) //
            REMOVE_QUOTES_AND_TRIM, //
            @Label(value = "Keep quotes", description = QuotedStrings.DESCRIPTION_KEEP_QUOTES) //
            KEEP_QUOTES; //
    }

    static class QuotedStringsOptionRef extends ReferenceStateProvider<QuotedStringsOption> {
    }

    @Widget(title = "Quoted strings", description = QuotedStrings.DESCRIPTION, advanced = true)
    @ValueReference(QuotedStringsOptionRef.class)
    @RadioButtonsWidget
    @Layout(QuotedStrings.class)
    QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

    static final class MaxDataRowsScannedDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 10000L;
        }
    }

    static class MaxDataRowsScannedRef extends ReferenceStateProvider<Optional<Long>> {
    }

    @Widget(title = "Limit scanned rows", description = LimitScannedRows.DESCRIPTION)
    @ValueReference(MaxDataRowsScannedRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(LimitScannedRows.class)
    @OptionalWidget(defaultProvider = MaxDataRowsScannedDefaultProvider.class)
    Optional<Long> m_maxDataRowsScanned = Optional.of(10000L);

    static class MaximumNumberOfColumnsRef extends ReferenceStateProvider<Integer> {
    }

    @Widget(title = "Maximum number of columns", description = MaximumNumberOfColumns.DESCRIPTION)
    @ValueReference(MaximumNumberOfColumnsRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(MaximumNumberOfColumns.class)
    int m_maximumNumberOfColumns = 8192;

    static class LimitMemoryPerColumnRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Limit memory per column", description = LimitMemoryPerColumn.DESCRIPTION)
    @ValueReference(LimitMemoryPerColumnRef.class)
    @Layout(LimitMemoryPerColumn.class)
    boolean m_limitMemoryPerColumn = true;

    @Widget(title = "Prepend file index to RowID", description = PrependFileIndexToRowId.DESCRIPTION)
    @Layout(PrependFileIndexToRowId.class)
    boolean m_prependFileIndexToRowId;
    // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    public void loadFromConfig(final CSVMultiTableReadConfig config) {

        final var tableReadConfig = config.getTableReadConfig();
        final var csvConfig = tableReadConfig.getReaderSpecificConfig();

        final var charset = csvConfig.getCharSetName();
        m_fileEncoding = FileEncodingOption.fromCharsetName(charset);
        m_customEncoding = m_fileEncoding == FileEncodingOption.OTHER ? charset : "";

        m_skipFirstLines = csvConfig.skipLines() ? csvConfig.getNumLinesToSkip() : 0L;

        m_commentLineCharacter = csvConfig.getComment();

        m_rowDelimiterOption =
            csvConfig.useLineBreakRowDelimiter() ? RowDelimiterOption.LINE_BREAK : RowDelimiterOption.CUSTOM;
        m_customRowDelimiter = EscapeUtils.escape(csvConfig.getLineSeparator());

        m_columnDelimiter = EscapeUtils.escape(csvConfig.getDelimiter());

        m_quotedStringsContainNoRowDelimiters = csvConfig.noRowDelimitersInQuotes();

        m_quoteCharacter = csvConfig.getQuote();

        m_quoteEscapeCharacter = csvConfig.getQuoteEscape();

        m_numberOfCharactersForAutodetection = csvConfig.getAutoDetectionBufferSize();

        m_firstRowContainsColumnNames = tableReadConfig.useColumnHeaderIdx();

        m_ifRowHasLessColumnsOption = tableReadConfig.allowShortRows() ? IfRowHasLessColumnsOption.INSERT_MISSING
            : IfRowHasLessColumnsOption.FAIL;

        m_decimalSeparator = csvConfig.getDecimalSeparator();

        m_thousandsSeparator = csvConfig.getThousandsSeparator();

        m_replaceEmptyQuotedStringsByMissingValues = csvConfig.replaceEmptyWithMissing();

        m_quotedStringsOption = csvConfig.getQuoteOption() == QuoteOption.REMOVE_QUOTES_AND_TRIM
            ? QuotedStringsOption.REMOVE_QUOTES_AND_TRIM : QuotedStringsOption.KEEP_QUOTES;

        m_maxDataRowsScanned =
            tableReadConfig.limitRowsForSpec() ? Optional.of(tableReadConfig.getMaxRowsForSpec()) : Optional.empty();

        m_maximumNumberOfColumns = csvConfig.getMaxColumns();

        m_limitMemoryPerColumn = csvConfig.isCharsPerColumnLimited();

        m_prependFileIndexToRowId = tableReadConfig.prependSourceIdxToRowID();
    }

    static String fileEncodingToCharsetName(final FileEncodingOption encoding, final String customEncoding) {
        return encoding == FileEncodingOption.OTHER ? customEncoding : encoding.m_charsetName;
    }

    public void saveToConfig(final CSVMultiTableReadConfig config) {

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
}
