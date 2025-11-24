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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.knime.base.node.io.filehandling.csv.reader2.CSVFormatParameters.CustomRowDelimiterPatternValidation.combineMultiCharExceptions;

import java.net.URL;
import java.util.List;

import org.knime.base.node.io.filehandling.csv.reader.OSIndependentNewLineReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatProvider.ProviderFromCSVFormat;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation.HasAtMaxOneCharValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsSingleCharacterValidation;

/**
 * Parameters for CSV format settings (delimiters, quotes, etc.).
 *
 * @author Paul Bärnreuther
 */
@Layout(FileFormatSection.class)
final class CSVFormatParameters implements NodeParameters {

    CSVFormatParameters(final URL url) {
        if (url.toString().endsWith(".tsv")) { //NOSONAR
            m_columnDelimiter = "\\t";
        }
    }

    CSVFormatParameters() {
        // default constructor
    }

    static class CommentStartRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Comment line character", description = "Defines the character indicating line comments.")
    @ValueReference(CommentStartRef.class)
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    String m_commentLineCharacter = "#";

    /**
     * Options for row delimiter.
     */
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
    @Effect(predicate = HasCustomRowDelimiter.class, type = EffectType.SHOW)
    @ValueReference(CustomRowDelimiterRef.class)
    @ValueProvider(CustomRowDelimiterProvider.class)
    String m_customRowDelimiter = "\n";

    static final class ColumnDelimiterProvider extends ProviderFromCSVFormat<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return EscapeUtils.escape(getCsvFormat().getDelimiterString());
        }
    }

    static class ColumnDelimiterRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Column delimiter", description = """
            Defines the character string delimiting columns. Use '\\t' for tab characters. Can get detected
            automatically.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @ValueReference(ColumnDelimiterRef.class)
    @ValueProvider(ColumnDelimiterProvider.class)
    String m_columnDelimiter = ",";

    @Widget(title = "Quoted strings contain no row delimiters", description = """
            Check this box if there are no quotes that contain row delimiters inside the files. Row delimiters
            should not be inside of quotes for parallel reading of individual files.
            """)
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
    @Layout(FileFormatSection.QuoteCharactersHorizontal.QuoteCharacter.class)
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
    @Layout(FileFormatSection.QuoteCharactersHorizontal.QuoteEscapeCharacter.class)
    @ValueReference(QuoteEscapeCharacterRef.class)
    @ValueProvider(QuoteEscapeCharacterProvider.class)
    String m_quoteEscapeCharacter = "\"";

    static class DecimalSeparatorRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Decimal separator", description = """
            Specifies the decimal separator character for parsing numbers. The decimal separator is only used for
            the parsing of double values. Note that the decimal separator must differ from the thousands separator.
            You must always provide a decimal separator.
            """)
    @ValueReference(DecimalSeparatorRef.class)
    @TextInputWidget(patternValidation = IsSingleCharacterValidation.class)
    @Layout(ValuesSection.Separators.class)
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
    @Layout(ValuesSection.Separators.class)
    String m_thousandsSeparator = "";

    /**
     * Save the settings to the given config.
     *
     * @param csvConfig the config to save to
     */
    void saveToConfig(final CSVTableReaderConfig csvConfig) {
        csvConfig.setComment(m_commentLineCharacter);

        csvConfig.useLineBreakRowDelimiter(m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);
        csvConfig.setLineSeparator(EscapeUtils.unescape(m_customRowDelimiter));

        csvConfig.setDelimiter(EscapeUtils.unescape(m_columnDelimiter));

        csvConfig.noRowDelimitersInQuotes(m_quotedStringsContainNoRowDelimiters);

        csvConfig.setQuote(m_quoteCharacter);

        csvConfig.setQuoteEscape(m_quoteEscapeCharacter);

        csvConfig.setDecimalSeparator(m_decimalSeparator);

        csvConfig.setThousandsSeparator(m_thousandsSeparator);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_commentLineCharacter.length() > 1) {
            throw new InvalidSettingsException("The comment line character must be at most one character.");
        }

        if (m_rowDelimiterOption == RowDelimiterOption.CUSTOM) {
            validateCustomRowDelimiter();
        }

        if (m_columnDelimiter.isEmpty()) {
            throw new InvalidSettingsException("The column delimiter must not be empty.");
        }

        if (m_quoteCharacter.length() > 1) {
            throw new InvalidSettingsException("The quote character must be at most one character.");
        }

        if (m_quoteEscapeCharacter.length() > 1) {
            throw new InvalidSettingsException("The quote escape character must be at most one character.");
        }

        if (m_decimalSeparator.length() != 1) {
            throw new InvalidSettingsException("The decimal separator must be exactly one character.");
        }

        if (m_thousandsSeparator.length() > 1) {
            throw new InvalidSettingsException("The thousands separator must be at most one character.");
        }
    }
}
