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
 *   7 Sept 2023 (jasper): created
 */
package org.knime.base.node.preproc.stringcleaner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.CapitalizeAfterOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.ChangeCasingOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.PadOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.RemoveLettersCategory;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.ReplaceWithOption;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests the implementation of the String Cleaner node.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class StringCleanerTest {

    /** TestStringThatContainsAllRelevantCharacterClasses */
    private static final String TSTCARCC =
        "\tabct\\ABC?.,!-\u00a0_.$(\n)↓ .\t?\r\nκνιμε   𝔎𝔑ℑ𝔐𝔈ÄO\u0308ø\r12Ⅴ\n\n\n";

    @Test
    void testValidateSettings() throws InvalidSettingsException {
        var settings = new StringCleanerNodeSettings();
        settings.m_pad = PadOption.NO;
        settings.m_changeCasing = ChangeCasingOption.NO;
        StringCleaner.validateSettings(settings);
        settings.m_pad = PadOption.START;
        settings.m_padFillCharacter = "";
        assertThrows(InvalidSettingsException.class, () -> StringCleaner.validateSettings(settings),
            "An empty pad fill character should throw.");
        settings.m_padFillCharacter = "_";
        StringCleaner.validateSettings(settings);
        settings.m_changeCasing = ChangeCasingOption.CAPITALIZE;
        settings.m_changeCasingCapitalizeAfter = CapitalizeAfterOption.CUSTOM;
        settings.m_changeCasingCapitalizeFirstCharacter = true;
        settings.m_changeCasingCapitalizeAfterCharacters = "";
        StringCleaner.validateSettings(settings);
        settings.m_changeCasingCapitalizeFirstCharacter = false;
        settings.m_changeCasingCapitalizeAfterCharacters = ".!";
        StringCleaner.validateSettings(settings);
        settings.m_changeCasingCapitalizeFirstCharacter = false;
        settings.m_changeCasingCapitalizeAfterCharacters = "";
        assertThrows(InvalidSettingsException.class, () -> StringCleaner.validateSettings(settings),
            "Neither capitalizing the first char nor providing any characters after which to capitalize should throw.");
    }

    @Test
    void testRemoveSpecialSequences() throws InvalidSettingsException {
        StringCleanerNodeSettings settings;
        // remove diacritics and marks
        settings = getNoopSettingsInstance();
        settings.m_removeAccents = true;
        assertThatSpecificCharactersAreHandled(settings, "\u0308", Map.of("Ä", "A"));

        // remove non-ascii
        settings = getNoopSettingsInstance();
        settings.m_removeNonASCII = true;
        assertThatSpecificCharactersAreRemoved(settings, "\u00a0↓κνιμε𝔎𝔑ℑ𝔐𝔈Ä\u0308øⅤ");
    }

    @Test
    void testRemoveCharacters() throws InvalidSettingsException {
        StringCleanerNodeSettings settings;
        // remove all letters
        settings = getNoopSettingsInstance();
        settings.m_removeLettersCategory = RemoveLettersCategory.ALL;
        assertThatSpecificCharactersAreRemoved(settings, "abctABCκνιμε𝔎𝔑ℑ𝔐𝔈ÄOø");

        // remove uppercase letters
        settings = getNoopSettingsInstance();
        settings.m_removeLettersCategory = RemoveLettersCategory.UPPERCASE;
        assertThatSpecificCharactersAreRemoved(settings, "ABC𝔎𝔑ℑ𝔐𝔈ÄO");

        // remove lowercase letters
        settings = getNoopSettingsInstance();
        settings.m_removeLettersCategory = RemoveLettersCategory.LOWERCASE;
        assertThatSpecificCharactersAreRemoved(settings, "abctκνιμεø");

        // remove numbers
        settings = getNoopSettingsInstance();
        settings.m_removeNumbers = true;
        assertThatSpecificCharactersAreRemoved(settings, "12Ⅴ");

        // remove punctuation
        settings = getNoopSettingsInstance();
        settings.m_removePunctuation = true;
        assertThatSpecificCharactersAreRemoved(settings, "?.,!-_()\\");

        // remove symbols
        settings = getNoopSettingsInstance();
        settings.m_removeSymbols = true;
        assertThatSpecificCharactersAreRemoved(settings, "$↓");
    }

    @Test
    void testRemoveCustomCharacters() throws InvalidSettingsException {
        // remove custom sets of characters (this also tests whether they are properly escaped)
        // \E is the regex end quote character, hopefully that is also escaped.
        List.of("", "\\", "\t", "\\t", "aBc", "t!?ø↓", "[(])", "\\Eha]+??llo?").forEach(charset -> {
            final var settings = getNoopSettingsInstance();
            settings.m_removeCustomCharactersString = charset;
            assertThatSpecificCharactersAreRemoved(settings, charset);
        });
    }

    @Test
    void testRemoveWhitespace() throws InvalidSettingsException {
        StringCleanerNodeSettings settings;
        // remove all whitespace
        settings = getNoopSettingsInstance();
        settings.m_removeAllWhitespace = true;
        assertThatSpecificCharactersAreRemoved(settings, "\t\u00a0\n \r");

        // remove leading whitespace
        settings = getNoopSettingsInstance();
        settings.m_removeLeadingWhitespace = true;
        assertEquals(TSTCARCC.replaceAll("^\\s+", ""), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "Leading whitespace is removed");

        // remove line breaks
        settings = getNoopSettingsInstance();
        settings.m_replaceLinebreakStrategy = ReplaceWithOption.REMOVE;
        assertEquals(TSTCARCC.replaceAll("\r|\n", ""), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "line breaks are removed");
        // replace line break with space
        settings.m_replaceLinebreakStrategy = ReplaceWithOption.REPLACE_WITH_STANDARDSPACE;
        // ensure that \r\n are replaced by only a single space
        final var map = new LinkedHashMap<String, String>(); // because the order matters in this case
        map.put("\r\n", " ");
        map.put("\r", " ");
        map.put("\n", " ");
        assertThatSpecificCharactersAreHandled(settings, "", map);

        // remove special whitespace
        settings = getNoopSettingsInstance();
        settings.m_replaceSpecialWhitespaceStrategy = ReplaceWithOption.REMOVE;
        assertEquals(TSTCARCC.replaceAll("[\\t\u00a0]", ""), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "special whitespace is removed");
        // replace weird whitespace with normal space
        settings.m_replaceSpecialWhitespaceStrategy = ReplaceWithOption.REPLACE_WITH_STANDARDSPACE;
        assertThatSpecificCharactersAreHandled(settings, "", Map.of("\t", " ", "\u00a0", " "));

        // remove duplicate whitespace --> #testRemoveDuplicateWhitespace

        // remove trailing whitespace
        settings = getNoopSettingsInstance();
        settings.m_removeTrailingWhitespace = true;
        assertEquals(TSTCARCC.replaceAll("\\s+$", ""), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "Trailing whitespace is removed");
    }

    @Test
    void testRemoveDuplicateWhitespace() throws InvalidSettingsException {
        final var settings = getNoopSettingsInstance();
        settings.m_removeDuplicateWhitespace = true;
        final var cleaner = StringCleaner.fromSettings(settings);

        // test that the right replacement is used
        assertEquals("\r\n", cleaner.clean("\r\n\r\n\r\n"), "Multiple Windows-style line breaks are combined into one");
        assertEquals("asdf\r\nasdf", cleaner.clean("asdf\r\n\r\n\r\nasdf"),
            "Multiple Windows-style line breaks are combined into one");
        assertEquals("\n", cleaner.clean("\n\n\n\n\n\n"), "Multiple Unix-style line breaks are combined into one");
        assertEquals("asdf\nasdf", cleaner.clean("asdf\n\n\n\n\n\nasdf"),
            "Multiple Unix-style line breaks are combined into one");
        assertEquals("\r\n", cleaner.clean(" \t\r\n  \r\n\t"),
            "A whitespace sequence containing a Windows-style line break is replaced by such");
        assertEquals("asdf\r\nasdf", cleaner.clean("asdf \t\r\n  \r\n\tasdf"),
            "A whitespace sequence containing a Windows-style line break is replaced by such");
        assertEquals("\n", cleaner.clean(" \t\n \n  \t"),
            "A whitespace sequence containing a Unix-style line break is replaced by such");
        assertEquals("asdf\nasdf", cleaner.clean("asdf \t\n \n  \tasdf"),
            "A whitespace sequence containing a Unix-style line break is replaced by such");

        assertEquals(" ", cleaner.clean(" \t \r \u00a0\t"),
            "A whitespace sequence not containing a line break is replaced by a standard space");
        assertEquals("asdf asdf", cleaner.clean("asdf \t \u00a0\tasdf"),
            "A whitespace sequence not containing a line break is replaced by a standard space");

        assertEquals(TSTCARCC.replaceAll("[\\s&&[^\\r\\n]]{2,}", " ").replaceAll("\\n{2,}", "\n"),
            cleaner.clean(TSTCARCC), "Duplicate whitespace is removed in a complex string");

    }

    private static void assertThatSpecificCharactersAreRemoved(final StringCleanerNodeSettings settings,
        final String charsToRemove) {
        assertThatSpecificCharactersAreHandled(settings, charsToRemove, Map.of());
    }

    private static void assertThatSpecificCharactersAreHandled(final StringCleanerNodeSettings settings,
        final String charsToRemove, final Map<String, String> charsToReplace) {
        var expectedString = TSTCARCC;
        if (charsToRemove != null && !charsToRemove.isEmpty()) {
            final var pattern = Pattern.compile("[" + Pattern.quote(charsToRemove) + "]");
            expectedString = pattern.matcher(TSTCARCC).replaceAll("");
        }
        for (var entry : charsToReplace.entrySet()) {
            expectedString = expectedString.replace(entry.getKey(), entry.getValue());
        }
        try {
            assertEquals(expectedString, StringCleaner.fromSettings(settings).clean(TSTCARCC),
                "The expected chars have been removed from the string");
        } catch (InvalidSettingsException ise) {
            // wrap to unchecked, suffices for testing (for use in lambdas we don't want to declare checked exceptions)
            throw new RuntimeException("Couldn't initialize StringCleaner", ise);
        }
    }

    @Test
    void testChangeCasing() throws InvalidSettingsException {
        StringCleanerNodeSettings settings;

        // make upper case
        settings = getNoopSettingsInstance();
        settings.m_changeCasing = ChangeCasingOption.UPPERCASE;
        assertEquals(TSTCARCC.toUpperCase(Locale.ROOT), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "String has been converted to upper case");

        // make lower case
        settings = getNoopSettingsInstance();
        settings.m_changeCasing = ChangeCasingOption.LOWERCASE;
        assertEquals(TSTCARCC.toLowerCase(Locale.ROOT), StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "String has been converted to lower case");

        // capitalize first and custom character
        settings = getNoopSettingsInstance();
        settings.m_changeCasing = ChangeCasingOption.CAPITALIZE;
        settings.m_changeCasingCapitalizeAfter = CapitalizeAfterOption.CUSTOM;
        settings.m_changeCasingCapitalizeAfterCharacters = "";
        settings.m_changeCasingCapitalizeFirstCharacter = true;
        assertEquals(TSTCARCC.substring(0, 1).toUpperCase(Locale.ROOT) + TSTCARCC.substring(1).toLowerCase(Locale.ROOT),
            StringCleaner.fromSettings(settings).clean(TSTCARCC), "String has been converted to title case");
        assertEquals("Ab", StringCleaner.fromSettings(settings).clean("aB"), "String has been converted to title case");
        settings.m_changeCasingCapitalizeAfterCharacters = "a";
        assertEquals("AB", StringCleaner.fromSettings(settings).clean("aB"), "String has capitalized after 'a'");
        settings.m_changeCasingCapitalizeFirstCharacter = false;
        assertEquals("aC", StringCleaner.fromSettings(settings).clean("ac"), "The first character isn't capitalized");
        settings.m_changeCasingCapitalizeAfterCharacters = "κ𝔎\t";
        assertEquals(TSTCARCC.toLowerCase(Locale.ROOT).replace("ν", "Ν").replace("𝔫", "𝔑").replaceFirst("a", "A"),
            StringCleaner.fromSettings(settings).clean(TSTCARCC),
            "After non-standard characters, the string is capitalized");

        // capitalize after non-letters character
        settings = getNoopSettingsInstance();
        settings.m_changeCasing = ChangeCasingOption.CAPITALIZE;
        settings.m_changeCasingCapitalizeAfter = CapitalizeAfterOption.NON_LETTERS;
        settings.m_changeCasingCapitalizeFirstCharacter = false;
        final var letterAfterNonLetterPattern = Pattern.compile("(?<=\\P{L})\\p{L}");
        assertEquals(
            letterAfterNonLetterPattern.matcher(TSTCARCC.toLowerCase(Locale.ROOT))
                .replaceAll(r -> r.group().toUpperCase(Locale.ROOT)),
            StringCleaner.fromSettings(settings).clean(TSTCARCC), "Letters after non-letters have been capitalized");

        // capitalize after whitespace character
        settings.m_changeCasingCapitalizeAfter = CapitalizeAfterOption.WHITESPACE;
        final var letterAfterWhitespacePattern = Pattern.compile("(?<=\\s)\\p{L}");
        assertEquals(
            letterAfterWhitespacePattern.matcher(TSTCARCC.toLowerCase(Locale.ROOT))
                .replaceAll(r -> r.group().toUpperCase(Locale.ROOT)),
            StringCleaner.fromSettings(settings).clean(TSTCARCC), "Letters after non-letters have been capitalized");
    }

    @Test
    void testPad() throws InvalidSettingsException {
        // Since this is handled by another method from StringUtils, we only test superficially

        StringCleanerNodeSettings settings;
        // remove all whitespace
        settings = getNoopSettingsInstance();
        settings.m_pad = PadOption.START;
        settings.m_padFillCharacter = "…";
        settings.m_padMinimumStringLength = 10;
        assertEquals(settings.m_padFillCharacter.repeat(10), StringCleaner.fromSettings(settings).clean(""),
            "The string is filled with the pad character");
        settings.m_pad = PadOption.END;
        assertEquals(settings.m_padFillCharacter.repeat(10), StringCleaner.fromSettings(settings).clean(""),
            "The string is filled with the pad character");

        settings.m_pad = PadOption.START;
        assertEquals(settings.m_padFillCharacter.repeat(5) + "abcde",
            StringCleaner.fromSettings(settings).clean("abcde"), "The string is padded at the start");
        settings.m_pad = PadOption.END;
        assertEquals("abcde" + settings.m_padFillCharacter.repeat(5),
            StringCleaner.fromSettings(settings).clean("abcde"), "The string is padded at the end");

        settings = getNoopSettingsInstance();
        settings.m_pad = PadOption.START;
        settings.m_padMinimumStringLength = 12;
        settings.m_padFillCharacter = "🅱️"; // this has length 3 because of unicode reasons™
        final var frakturKNIME = "𝔎𝔑ℑ𝔐𝔈"; // this has length 9 because of unicode reasons™
        assertEquals(settings.m_padFillCharacter + frakturKNIME,
            StringCleaner.fromSettings(settings).clean(frakturKNIME), "The padding can handle non-standard characters");
    }

    private static StringCleanerNodeSettings getNoopSettingsInstance() {
        final var settings = new StringCleanerNodeSettings();
        // a lot of these calls are redundant, but if we ever change the default we don't break the test
        settings.m_removeAccents = false;
        settings.m_removeNonASCII = false;
        settings.m_removeLettersCategory = RemoveLettersCategory.NONE;
        settings.m_removeNumbers = false;
        settings.m_removePunctuation = false;
        settings.m_removeSymbols = false;
        settings.m_removeCustomCharactersString = "";
        settings.m_removeAllWhitespace = false;
        settings.m_removeLeadingWhitespace = false;
        settings.m_replaceLinebreakStrategy = ReplaceWithOption.KEEP;
        settings.m_replaceSpecialWhitespaceStrategy = ReplaceWithOption.KEEP;
        settings.m_removeDuplicateWhitespace = false;
        settings.m_removeTrailingWhitespace = false;
        settings.m_changeCasing = ChangeCasingOption.NO;
        settings.m_pad = PadOption.NO;
        return settings;
    }

    @Test
    void testNoOpStringCleaner() throws InvalidSettingsException {
        assertEquals(TSTCARCC, StringCleaner.fromSettings(getNoopSettingsInstance()).clean(TSTCARCC),
            "The No-Op String Cleaner should not perform any operation on the string");
        assertEquals("", StringCleaner.fromSettings(getNoopSettingsInstance()).clean(""),
            "The No-Op String Cleaner should not perform any operation on the empty string");
        assertEquals(" \t", StringCleaner.fromSettings(getNoopSettingsInstance()).clean(" \t"),
            "The No-Op String Cleaner should not perform any operation on the blank string");
    }

}
