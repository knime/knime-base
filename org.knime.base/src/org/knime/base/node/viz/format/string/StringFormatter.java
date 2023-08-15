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
import java.util.stream.Collectors;

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
                str = m_settings.emptyStringReplacement().orElse(str);
            }
            return format(str);
        }
        throw new IllegalArgumentException("Not a StringValue: " + dataValue.toString());
    }

    /** Applies the actual transformations / the formatting to the string by calling subroutines */
    String format(String s) {
        s = abbreviate(s);
        s = HtmlEscapers.htmlEscaper().escape(s);
        s = replaceCharacters(s);
        s = makeLinksClickable(s);
        s = style(s);
        return s;
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
    String replaceCharacters(String string) {
        if (m_settings.replaceNonPrintableCharacters()) {
            string = StringFormatterPatterns.NON_PRINTABLE_CHARS.matcher(string).replaceAll(mr -> mr.group()
                .codePoints().mapToObj(StringFormatter::getCharacterReplacementSpan).collect(Collectors.joining()));
        }
        if (m_settings.replaceNewlinesAndCarriageReturn()) {
            // U+2424 is SYMBOL FOR NEWLINE ␤
            string = string.replace("\n", getCharacterReplacementSpan("\n".codePointAt(0), '\u2424'));
            // U+240d is SYMBOL FOR CARRIAGE RETURN ␍
            string = string.replace("\r", getCharacterReplacementSpan("\r".codePointAt(0), '\u240d'));
        }
        return string;
    }

    private static String getCharacterReplacementSpan(final int codepoint) {
        // Use the  REPLACEMENT CHARACTER � (U-FFFD)
        return getCharacterReplacementSpan(codepoint, '\ufffd');
    }

    private static String getCharacterReplacementSpan(final int codepoint, final char replacementChar) {
        return "<span title=\"U+" + String.format("%04X", codepoint) + " " + Character.getName(codepoint) + "\">"
            + replacementChar + "</span>";
    }

    /** If the settings say so, replace URLs and emails by a link to themselves */
    String makeLinksClickable(String s) {
        if (m_settings.linkHrefsAndEmails) {
            s = StringFormatterPatterns.URL.matcher(s).replaceAll("<a href=\"$1\">$1</a>");
            s = StringFormatterPatterns.EMAIL.matcher(s).replaceAll("<a href=\"mailto:$1\">$1</a>");
        }
        return s;
    }

    /** Add CSS styles */
    String style(final String s) {
        final var sb = new StringBuilder("<span style=\"");
        sb.append("display: inline-block;");
        if (m_settings.wrapLines()) {
            sb.append("white-space: break-spaces;");
        } else {
            sb.append("white-space: pre;");
        }
        if (m_settings.breakWords()) {
            sb.append("word-break: break-all;");
        } else {
            sb.append("word-break: normal;");
        }
        sb.append("\">").append(s).append("</span>");
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
     * @param replaceNewlinesAndCarriageReturn
     * @param replaceNonPrintableCharacters
     * @param linkHrefsAndEmails
     */
    static record Settings(int nFirstChars, int nLastChars, boolean wrapLines, boolean breakWords,
        boolean replaceNewlinesAndCarriageReturn, boolean replaceNonPrintableCharacters,
        Optional<String> emptyStringReplacement, boolean linkHrefsAndEmails) {
        /**
         * Validate the settings that are saved in this record instance
         *
         * @throws InvalidSettingsException
         */
        void validate() throws InvalidSettingsException {
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
                config.getBoolean(CONFIG_REPLACE_NEWLINE_WITH_SYMBOLS), //
                config.getBoolean(CONFIG_DISPLAY_NONPRINTABLE_CHARS), //
                Optional.ofNullable(config.getString(CONFIG_EMPTYSTRING_REPLACEMENT)),
                config.getBoolean(CONFIG_LINK_HREF_AND_EMAIL));
            return StringFormatter.fromSettings(s);
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
