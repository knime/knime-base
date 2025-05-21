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
 *   15 Aug 2023 (jasper): created
 */
package org.knime.base.node.viz.format.string;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.property.ValueFormatModel;
import org.knime.core.data.property.ValueFormatModelFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.util.CheckUtils;

import com.google.common.html.HtmlEscapers;

/**
 * Formatter that shortens strings, adds CSS styles to it, normalises special characters and makes links clickable
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class StringFormatter implements ValueFormatModel {

    /** If a string is too long, it will be abbreviated using this ellipsis. */
    static final String ELLIPSIS = "…";

    /** If a URL has no protocol, this protocol will be prepended to the link */
    static final String DEFAULT_PROTOCOL = "http://";

    /** When creating a mailto: link, this text will appear in the tooltip when hovering over the email address */
    static final String DEFAULT_EMAIL_TOOLTIP_PREFIX = "Send email to ";

    /** When creating a mailto: link, this text will appear in the tooltip when hovering over the email address */
    static final String EMPTY_STRING_TOOLTIP = "<empty string>";

    private static final char DEFAULT_REPLACEMENT_CHAR = '\ufffd'; // REPLACEMENT CHARACTER � (U-FFFD)

    private static final char LINE_FEED_REPLACEMENT_CHAR = '\u2424'; // SYMBOL FOR NEWLINE ␤ (U+2424)

    private static final char CARRIAGE_RETURN_REPLACEMENT_CHAR = '\u240d'; // SYMBOL FOR CARRIAGE RETURN ␍ (U+240D)

    private final Settings m_settings;

    /** Only to be called by {@link #fromSettings(Settings)} */
    private StringFormatter(final Settings settings) {
        m_settings = settings;
    }

    @Override
    public String getHTML(final DataValue dataValue) {
        if (dataValue instanceof StringValue sv) {
            var str = sv.getStringValue();
            if (str.isEmpty()) {
                return formatEmptyString();
            }
            return format(str);
        }
        throw notAStringValue(dataValue);
    }

    @Override
    public String getPlaintext(final DataValue dataValue) {
        if (dataValue instanceof StringValue sv) {
            return makePlaintext(sv.getStringValue());
        }
        throw notAStringValue(dataValue);
    }

    private static IllegalArgumentException notAStringValue(final DataValue dataValue) {
        return new IllegalArgumentException("Not a StringValue: " + dataValue.toString());
    }

    /** Format the empty string */
    String formatEmptyString() {
        final var replacementString = HtmlEscapers.htmlEscaper().escape(m_settings.emptyStringReplacement().orElse(""));
        final var tooltipString = HtmlEscapers.htmlEscaper().escape(EMPTY_STRING_TOOLTIP);
        return style(replacementString, tooltipString);
    }

    /** Applies the actual transformations / the formatting to the string by calling subroutines */
    String format(String s) {
        s = abbreviate(s);
        s = HtmlEscapers.htmlEscaper().escape(s);
        final var plaintext = replaceCharacters(s, false);
        var html = replaceCharacters(s, true);
        html = makeLinksClickable(html);
        return style(html, plaintext);
    }

    String makePlaintext(String s) {
        s = abbreviate(s);
        return replaceCharacters(s, false);
    }

    /** Truncate the string, if necessary */
    String abbreviate(final String string) {
        if (string.length() <= m_settings.nFirstChars() + m_settings.nLastChars()) {
            return string;
        }
        if (m_settings.nFirstChars() + m_settings.nLastChars() == 0) {
            return ELLIPSIS;
        }
        return new StringBuilder()//
            .append(string, 0, m_settings.nFirstChars()) // 0 <= first < length, so no IOOB
            .append(ELLIPSIS) // we definitely need an ellipsis, otherwise we would've returned the string earlier
            .append(string, string.length() - m_settings.nLastChars(), string.length()) // 0 <= last < length
            .toString();
    }

    /** Depending on the settings, replace control characters with symbols */
    String replaceCharacters(String string, final boolean html) {
        if (m_settings.replaceNonPrintableCharacters()) {
            if (html) {
                string = StringFormatterPatterns.NON_PRINTABLE_CHARS.matcher(string)//
                    .replaceAll(mr -> mr.group()//
                        .codePoints()//
                        .mapToObj(cp -> getCharacterReplacementSpan(cp, DEFAULT_REPLACEMENT_CHAR))//
                        .collect(Collectors.joining()));
            } else {
                string = StringFormatterPatterns.NON_PRINTABLE_CHARS.matcher(string)
                    .replaceAll(String.valueOf(DEFAULT_REPLACEMENT_CHAR));
            }
        }
        if (m_settings.replaceNewlinesAndCarriageReturn()) {
            if (html) {
                string =
                    string.replace("\n", getCharacterReplacementSpan("\n".codePointAt(0), LINE_FEED_REPLACEMENT_CHAR));
                string = string.replace("\r",
                    getCharacterReplacementSpan("\r".codePointAt(0), CARRIAGE_RETURN_REPLACEMENT_CHAR));
            } else {
                string = string.replace("\n", String.valueOf(LINE_FEED_REPLACEMENT_CHAR));
                string = string.replace("\r", String.valueOf(CARRIAGE_RETURN_REPLACEMENT_CHAR));

            }
        }
        return string;
    }

    private static String getCharacterReplacementSpan(final int codepoint, final char replacementChar) {
        return "<span title=\"U+" + String.format("%04X", codepoint) + " " + Character.getName(codepoint) + "\">"
            + replacementChar + "</span>";
    }

    /** If the settings say so, replace URLs and emails by a link to themselves */
    String makeLinksClickable(String s) {
        if (m_settings.linkHrefsAndEmails) {
            s = StringFormatterPatterns.URL.matcher(s).replaceAll(mr -> {
                /* The matching url may contain '$' characters, which need to be escaped since they have a special
                 * meaning in {@link Matcher#replaceAll(String)}. */
                var url = Matcher.quoteReplacement(mr.group());
                if (!StringUtils.startsWithIgnoreCase(url, "http://") // NOSONAR keep string explicit here
                    && !StringUtils.startsWithIgnoreCase(url, "https://")) {
                    // otherwise, e.g. "knime.com" is interpreted as a file path when clicking it
                    url = DEFAULT_PROTOCOL + url;
                }
                return "<a href=\"" + url + "\" title=\"" + url + "\" target=\"_blank\">$1</a>";
            });
            s = StringFormatterPatterns.EMAIL.matcher(s).replaceAll(
                "<a href=\"mailto:$1\" title=\"" + DEFAULT_EMAIL_TOOLTIP_PREFIX + "$1\" target=\"_blank\">$1</a>");
        }
        return s;
    }

    /** Add CSS styles */
    String style(final String html, final String plaintext) {
        final var sb = new StringBuilder("<span style=\"");
        sb.append("display:inline-block;overflow:hidden;text-overflow:ellipsis;width:100%;");
        if (m_settings.wrapLines()) {
            sb.append("white-space:break-spaces;");
        } else {
            sb.append("white-space:pre;");
        }
        if (m_settings.breakWords()) {
            sb.append("word-break:break-all;");
        } else {
            sb.append("word-break:normal;");
        }
        sb.append(m_settings.alignment().getCSSAttribute());
        sb.append("\" title=\"").append(plaintext).append("\">").append(html).append("</span>");
        return sb.toString();
    }

    @Override
    public void save(final ConfigWO config) {
        Persistor.save(config, this);
    }

    /**
     * Instantiate a new instance from a settings record. This method also validates the settings.
     *
     * @param settings
     * @return A new {@linkplain StringFormatter} instance
     * @throws InvalidSettingsException
     */
    static StringFormatter fromSettings(final Settings settings) throws InvalidSettingsException {
        settings.validate();
        return new StringFormatter(settings);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StringFormatter fmt) {
            return m_settings.equals(fmt.m_settings);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return m_settings.hashCode();
    }

    /**
     * Settings record for the {@linkplain StringFormatter}
     *
     * @param nFirstChars
     * @param nLastChars
     * @param wrapLines
     * @param breakWords
     * @param alignment
     * @param replaceNewlinesAndCarriageReturn
     * @param replaceNonPrintableCharacters
     * @param emptyStringReplacement
     * @param linkHrefsAndEmails
     */
    static record Settings(int nFirstChars, int nLastChars, boolean wrapLines, boolean breakWords,
        AlignmentSuggestionOption alignment, boolean replaceNewlinesAndCarriageReturn,
        boolean replaceNonPrintableCharacters, Optional<String> emptyStringReplacement, boolean linkHrefsAndEmails) {
        /**
         * Validate the settings that are saved in this record instance
         *
         * @throws InvalidSettingsException
         */
        void validate() throws InvalidSettingsException {
            CheckUtils.checkSettingNotNull(alignment, "Alignment suggestion cannot be null");
            CheckUtils.checkSettingNotNull(emptyStringReplacement,
                "String replacement cannot be null (use empty optional instead)");
            CheckUtils.checkSetting(nFirstChars >= 0, "Cannot truncate to a negative number of characters.");
            CheckUtils.checkSetting(nLastChars >= 0, "Cannot keep a negative number of last characters.");
            CheckUtils.checkSetting(!breakWords || wrapLines, "Breaking words is only possible when wrapping lines.");
            CheckUtils.checkSetting(emptyStringReplacement.isEmpty() || !emptyStringReplacement.get().isBlank(),
                "Replacement string for the empty string cannot be blank");
        }
    }

    private static final class Persistor {

        private static final String CONFIG_FIRST_CHARS = "keepFirstChars";

        private static final String CONFIG_LAST_CHARS = "keepLastChars";

        private static final String CONFIG_WRAP_LINES = "wrapLines";

        private static final String CONFIG_BREAK_WORDS = "breakWords";

        private static final String CONFIG_ALIGNMENT_SUGGESTION = "alignmentSuggestion";

        private static final String CONFIG_DISPLAY_NONPRINTABLE_CHARS = "replaceNonPrintableCharacters";

        private static final String CONFIG_REPLACE_NEWLINE_WITH_SYMBOLS = "replaceNewlineAndCarriageReturn";

        private static final String CONFIG_EMPTYSTRING_REPLACEMENT = "emptyStringReplacement";

        private static final String CONFIG_LINK_HREF_AND_EMAIL = "linkHrefAndEmail";

        static void save(final ConfigBaseWO config, final StringFormatter formatter) {
            final var s = formatter.m_settings;
            config.addInt(CONFIG_FIRST_CHARS, s.nFirstChars());
            config.addInt(CONFIG_LAST_CHARS, s.nLastChars());
            config.addBoolean(CONFIG_WRAP_LINES, s.wrapLines());
            config.addBoolean(CONFIG_BREAK_WORDS, s.breakWords());
            config.addString(CONFIG_ALIGNMENT_SUGGESTION, s.alignment().name());
            config.addBoolean(CONFIG_REPLACE_NEWLINE_WITH_SYMBOLS, s.replaceNewlinesAndCarriageReturn());
            config.addBoolean(CONFIG_DISPLAY_NONPRINTABLE_CHARS, s.replaceNonPrintableCharacters());
            config.addString(CONFIG_EMPTYSTRING_REPLACEMENT, s.emptyStringReplacement().orElse(null));
            config.addBoolean(CONFIG_LINK_HREF_AND_EMAIL, s.linkHrefsAndEmails());
        }

        static StringFormatter load(final ConfigBaseRO config) throws InvalidSettingsException {
            final var s = new Settings(//
                config.getInt(CONFIG_FIRST_CHARS), //
                config.getInt(CONFIG_LAST_CHARS), //
                config.getBoolean(CONFIG_WRAP_LINES), //
                config.getBoolean(CONFIG_BREAK_WORDS), //
                getAlignmentSuggestion(config), //
                config.getBoolean(CONFIG_REPLACE_NEWLINE_WITH_SYMBOLS), //
                config.getBoolean(CONFIG_DISPLAY_NONPRINTABLE_CHARS), //
                Optional.ofNullable(config.getString(CONFIG_EMPTYSTRING_REPLACEMENT)),
                config.getBoolean(CONFIG_LINK_HREF_AND_EMAIL));
            return StringFormatter.fromSettings(s);
        }

        static AlignmentSuggestionOption getAlignmentSuggestion(final ConfigBaseRO config)
            throws InvalidSettingsException {
            var v = config.getString(CONFIG_ALIGNMENT_SUGGESTION, AlignmentSuggestionOption.LEFT.name());
            try {
                return AlignmentSuggestionOption.valueOf(v);
            } catch (IllegalArgumentException iae) {
                throw new InvalidSettingsException("Invalid alignment suggestion", iae);
            }

        }
    }

    /**
     * Factory implementation for the {@link StringFormatter}
     *
     * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
     */
    public static final class Factory implements ValueFormatModelFactory<StringFormatter> {

        @Override
        public String getDescription() {
            return "String Formatter";
        }

        @Override
        public StringFormatter getFormatter(final ConfigRO config) throws InvalidSettingsException {
            return Persistor.load(config);
        }

        @Override
        public Class<StringFormatter> getFormatterClass() {
            return StringFormatter.class;
        }
    }

}
