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
 *   22 Aug 2023 (jasper): created
 */
package org.knime.base.node.viz.format.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.knime.base.node.viz.format.string.StringFormatter.Settings;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;

/**
 * Tests the functionality of the {@link StringFormatter}
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
class StringFormatterTest {

    @Test
    void testInstantiation() throws InvalidSettingsException {
        instantiateAndPersistFormatterWithSettings(false,
            new Settings(0, 0, false, false, false, false, Optional.empty(), false));
        instantiateAndPersistFormatterWithSettings(false,
            new Settings(10, 10, false, false, false, false, Optional.of("hi"), false));
        instantiateAndPersistFormatterWithSettings(false,
            new Settings(0, 0, true, true, true, true, Optional.empty(), true));
        instantiateAndPersistFormatterWithSettings(false,
            new Settings(10, 0, true, true, true, true, Optional.of("test"), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(10, 10, false, true, true, true, Optional.empty(), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(-10, 10, true, true, true, true, Optional.empty(), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(10, -10, true, true, true, true, Optional.empty(), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(-10, -10, true, true, true, true, Optional.empty(), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(10, 10, true, true, true, true, Optional.of(""), true));
        instantiateAndPersistFormatterWithSettings(true,
            new Settings(10, 10, true, true, true, true, Optional.of("  "), true));
    }

    static void instantiateAndPersistFormatterWithSettings(final boolean expectedToFail, final Settings s)
        throws InvalidSettingsException {
        if (expectedToFail) {
            assertThrows(InvalidSettingsException.class, () -> StringFormatter.fromSettings(s),
                "Invalid settings are detected");
        } else {
            final var f1 = StringFormatter.fromSettings(s);
            assertNotNull(f1, "StringFormatter should be instantiated");
            final var c = new ModelContent("test");
            f1.save(c);
            final var f2 = new StringFormatter.Factory().getFormatter(c);
            assertEquals(f1, f2, "Save-load cycle should yield identical formatters");
        }
    }

    @Test
    void testAbbreviate() throws InvalidSettingsException {
        for (var firstChars = 0; firstChars < 10; firstChars++) {
            for (var lastChars = 0; lastChars < 10; lastChars++) {
                final var settings =
                    new Settings(firstChars, lastChars, false, false, false, false, Optional.empty(), false);
                final var fmt = StringFormatter.fromSettings(settings);
                for (final var string : List.of("", "1", "12", "12345", "12345678", "123456789", "1234567890",
                    "1234567890abcdefghijklmnopqrstuvwxyz")) {
                    final var abbr = fmt.abbreviate(string);
                    assertTrue(StringUtils.countMatches(abbr, StringFormatter.ELLIPSIS) <= 1,
                        "At most one ellipsis should be inserted");
                    if (lastChars > 0) {
                        assertEquals(StringUtils.substring(string, -lastChars), StringUtils.substring(abbr, -lastChars),
                            "The last chars should be identical " + firstChars + " " + lastChars);
                    }
                    assertEquals(StringUtils.substring(string, 0, firstChars),
                        StringUtils.substring(abbr, 0, firstChars),
                        "The first chars should be identical " + firstChars + " " + lastChars);
                    if (firstChars + lastChars >= string.length()) {
                        assertEquals(string, abbr, "The strings are identical");
                    }
                }
            }
        }
    }

    @Test
    void testReplaceCharactersHTML() throws InvalidSettingsException {
        var settings = new Settings(500, 5, false, false, false, true, Optional.empty(), false);
        var fmt = StringFormatter.fromSettings(settings);
        final var expectedCharMappings = new LinkedHashMap<String, String>();
        expectedCharMappings.put("\u00ad", "\ufffd"); // soft hyphen
        expectedCharMappings.put("\u200D", "\ufffd"); // zero-width joiner
        expectedCharMappings.put("\u0000", "\ufffd"); // null
        expectedCharMappings.put("\u000C", "\ufffd"); // form feed
        expectedCharMappings.put(" ", " "); // normal space
        expectedCharMappings.put("\t", "\ufffd"); // tab
        expectedCharMappings.put("\r", "\r"); // cr
        expectedCharMappings.put("\n", "\n"); // lf
        expectedCharMappings.put("\u00a0", "\ufffd"); // non-break space
        expectedCharMappings.put("\u2028", "\ufffd"); // line-separator
        expectedCharMappings.put("\u2029", "\ufffd"); // paragraph-seperator
        buildStringsAndAssertEquals(fmt, expectedCharMappings, true);
        buildStringsAndAssertEquals(fmt, expectedCharMappings, false);

        // change to also replace \r \n
        settings = new Settings(500, 5, false, false, true, true, Optional.empty(), false);
        fmt = StringFormatter.fromSettings(settings);
        expectedCharMappings.put("\r", "\u240d");
        expectedCharMappings.put("\n", "\u2424");
        buildStringsAndAssertEquals(fmt, expectedCharMappings, true);
        buildStringsAndAssertEquals(fmt, expectedCharMappings, false);

        // change to only replace \r \n
        settings = new Settings(500, 5, false, false, true, false, Optional.empty(), false);
        fmt = StringFormatter.fromSettings(settings);
        for (final var entry : expectedCharMappings.entrySet()) {
            if (entry.getKey().equals("\r") || entry.getKey().equals("\n")) {
                continue;
            }
            // "reset" all other weird characters to their original form
            entry.setValue(entry.getKey());
        }
        buildStringsAndAssertEquals(fmt, expectedCharMappings, true);
        buildStringsAndAssertEquals(fmt, expectedCharMappings, false);
    }

    private static void buildStringsAndAssertEquals(final StringFormatter fmt,
        final Map<String, String> expectedCharMappings, final boolean html) {
        final var ab = new StringBuilder();
        final var bb = new StringBuilder();
        for (final var entry : expectedCharMappings.entrySet()) {
            ab.append(entry.getKey());
            if (entry.getKey().equals(entry.getValue())) {
                bb.append(entry.getValue());
            } else {
                final var cp = entry.getKey().codePointAt(0);
                if (html) {
                    bb.append("<span title=\"U+" + String.format("%04X", cp) + " " + Character.getName(cp) + "\">"
                        + entry.getValue() + "</span>");
                } else {
                    bb.append(entry.getValue());
                }
            }
        }
        final var prefix = "lalalala";
        final var suffix = "here are some very normal ascii chars that are HOPEFULLY preserved as they are";
        final var a = prefix + ab.toString() + suffix;
        final var b = prefix + bb.toString() + suffix;
        assertEquals(b, fmt.replaceCharacters(a, html), "The right characters have been replaced by \ufffd");
    }

    @Test
    void testMakeLinksClickable() throws InvalidSettingsException {
        final var settings = new Settings(500, 0, false, false, false, false, Optional.empty(), true);
        final var fmt = StringFormatter.fromSettings(settings);
        Stream.of(// some "normal" urls
            "www.knime.com", //
            "knime.com", //
            "e.com", //
            "knime.de/path.end", //
            "https://knime.com", //
            "https://knime.online", //
            "https://knime.com/path", //
            "https://knime.com/path/path", //
            "http://knime.com/path?query", //
            "http://knime.com/path/path/path", //
            "https://k.n.i.m.e.cc?query", //
            "https://k.n.i.m.e.cc?param=something%20with%20spaces&test=with+pluses&otherparam='quoted'", //
            "knime.com.with.port.coffee:1234", //
            "https://many.many.subdomains.knime.online:42/with/a/path?and=a+query", //
            "http://domain.with.weird.tld.xn--30rr7y", //
            "dOmaIIn92.0WITh.quESTIOnabLE.CAP-ITLisat10n.cool/aNd/A?qU3ry"//
        ).forEach(s -> simpleSubstitution(true, false, fmt, s));

        // test in-string substitution
        assertEquals(
            "l1\n<a href=\"https://www.knime.com/call?query\" title=\"https://www.knime.com/call?query\">"
                + "https://www.knime.com/call?query</a> is a url\nl3",
            fmt.makeLinksClickable("l1\nhttps://www.knime.com/call?query is a url\nl3"), "URL should be replaced");
        assertEquals("   aa  <a href=\"http://k.de\" title=\"http://k.de\">k.de</a>\t\r\n bb",
            fmt.makeLinksClickable("   aa  k.de\t\r\n bb"), "URL should be replaced");
        assertEquals(" (<a href=\"http://www.google.com\" title=\"http://www.google.com\">www.google.com</a>) ",
            fmt.makeLinksClickable(" (www.google.com) "), "URL should be replaced");

        Stream.of(// some emails
            "first.last@knime.com", //
            "first-last@k.org", //
            "onlyone@knime.de", //
            "1@knime.org", //
            "I+aVoId+SPAM@some.url.with.many.sub.domains.zuerich"//
        ).forEach(s -> simpleSubstitution(true, true, fmt, s));

        Stream.of(// some bad urls
            "www.knime.c", //
            "firstname.lastname", //
            "affe://elepha.nt", //
            "…nime.com", //
            "knime.co…", //
            "http://illegal.chars=before+Path+Starts", //
            "weirdprotocol://illegal.chars=before+Path+Starts", //
            "just some normal text", //
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.de", //
            ".", //
            "..", //
            ""//
        ).forEach(s -> simpleSubstitution(false, false, fmt, s));

        Stream.of(// some bad emails
            "user@knime", //
            "user@knime.c", //
            "@knime.com", //
            "@", //
            "@@", //
            "", //
            "toolongggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg@knime.com", //
            "first.last@"//
        ).forEach(s -> simpleSubstitution(false, true, fmt, s));

        // test in-string substitution
        assertEquals(
            "firstline\n<a href=\"mailto:user@knime.com\" title=\"" + StringFormatter.DEFAULT_EMAIL_TOOLTIP_PREFIX
                + "user@knime.com\">user@knime.com</a> is cool\nthirdline",
            fmt.makeLinksClickable("firstline\nuser@knime.com is cool\nthirdline"), "Mail address should be replaced");
        assertEquals(
            "<a href=\"mailto:a@knime.com\" title=\"" + StringFormatter.DEFAULT_EMAIL_TOOLTIP_PREFIX
                + "a@knime.com\">a@knime.com</a>\tnextcolumn",
            fmt.makeLinksClickable("a@knime.com\tnextcolumn"), "Mail address should be replaced");
        assertEquals(
            "some double@<a href=\"mailto:email@knime.com\" title=\"" + StringFormatter.DEFAULT_EMAIL_TOOLTIP_PREFIX
                + "email@knime.com\">email@knime.com</a> in a text",
            fmt.makeLinksClickable("some double@email@knime.com in a text"), "Mail address should be replaced");
    }

    private static void simpleSubstitution(final boolean shouldBeReplaced, final boolean isEmail,
        final StringFormatter fmt, final String string) {
        final var result = fmt.makeLinksClickable(string);
        if (shouldBeReplaced) {
            assertNotEquals(string, result, "The strings should not be equal");
            assertEquals(3, StringUtils.countMatches(result, string), // also ensures that the whole string is matched
                "the link address should appear exactly three times, "
                    + "once as the href and twice as the text (in the title in the cell): " + result);
            assertTrue(result.contains("<a href=\""), "A link should now be included in " + result);
            if (isEmail) {
                assertTrue(result.contains("mailto:"), "A mailto-link should be included");
            } else {
                assertTrue(result.contains("://"),
                    "The link must have a protocol (otherwise it'll be interpreted as a path when clicking");
                assertFalse(result.contains("mailto:"), "A \"normal\" link should've been created, not a mailto-link.");
            }
        } else {
            assertEquals(string, result, "No change should be made");
        }
    }

    @Test
    void testStyle() throws InvalidSettingsException {
        final var testString = "Taco Cabeza is just around the corner";
        final var defaultStyles = "display:inline-block;overflow:hidden;text-overflow:ellipsis;";
        var fmt = getInstanceWithStyleSettings(false, false);
        var formattedString = fmt.style(testString, testString);
        assertTrue(formattedString.startsWith("<span style="), "Start with a span definition");
        assertTrue(formattedString.endsWith("</span>"), "End with the closing of the span");
        assertEquals(0, StringUtils.countMatches(formattedString, '"') % 2, "There should be an even number of quotes");
        assertTrue(formattedString.contains(defaultStyles));

        assertTrue(formattedString.contains("white-space:pre;"), "don't wrap lines");
        assertFalse(formattedString.contains("white-space:break-spaces;"), "don't wrap lines");
        assertTrue(formattedString.contains("word-break:normal;"), "don't break words");
        assertFalse(formattedString.contains("word-break:break-all;"), "don't break words");

        fmt = getInstanceWithStyleSettings(true, false);
        formattedString = fmt.style(testString, testString);
        assertFalse(formattedString.contains("white-space:pre;"), "wrap lines");
        assertTrue(formattedString.contains("white-space:break-spaces;"), "wrap lines");
        assertTrue(formattedString.contains("word-break:normal;"), "don't break words");
        assertFalse(formattedString.contains("word-break:break-all;"), "don't break words");

        fmt = getInstanceWithStyleSettings(true, true);
        formattedString = fmt.style(testString, testString);
        assertFalse(formattedString.contains("white-space:pre;"), "wrap lines");
        assertTrue(formattedString.contains("white-space:break-spaces;"), "wrap lines");
        assertFalse(formattedString.contains("word-break:normal;"), "break words");
        assertTrue(formattedString.contains("word-break:break-all;"), "break words");

        // remember:
        assertThrows(InvalidSettingsException.class, () -> getInstanceWithStyleSettings(false, true),
            "Should throw ISE");
    }

    private static StringFormatter getInstanceWithStyleSettings(final boolean wrapLines, final boolean breakWords)
        throws InvalidSettingsException {
        final var settings = new Settings(500, 5, wrapLines, breakWords, false, false, Optional.empty(), false);
        return StringFormatter.fromSettings(settings);
    }

    @Test
    void testFormat() throws InvalidSettingsException {
        final var testString = "here's a.link and a way too long sentence\t";
        var settings = new Settings(25, 5, true, true, false, false, Optional.empty(), true);
        var fmt = StringFormatter.fromSettings(settings);
        assertEquals("""
                <span style="\
                display:inline-block;\
                overflow:hidden;\
                text-overflow:ellipsis;\
                white-space:break-spaces;\
                word-break:break-all;\
                " title="here&#39;s a.link and a way t…ence\t">\
                here&#39;s <a href="http://a.link" title="http://a.link">a.link</a> and a way t"""
            + StringFormatter.ELLIPSIS + "ence\t</span>", fmt.format(testString), "Expect the proper output");

        settings = new Settings(11, 5, true, true, false, false, Optional.empty(), true);
        fmt = StringFormatter.fromSettings(settings);
        assertFalse(fmt.format(testString).contains("<a"), "The link must not be linked to a.li");

        settings = new Settings(100, 0, false, false, false, false, Optional.empty(), true);
        fmt = StringFormatter.fromSettings(settings);
        assertEquals(2,
            StringUtils.countMatches(fmt.format("<some xml=\"tag\">with te'xt & symbols</some>"),
                "&lt;some xml=&quot;tag&quot;&gt;with te&#39;xt &amp; symbols&lt;/some&gt;"),
            "HTML is escaped properly (two times, both in title and in the span itself)");

        settings = new Settings(100, 0, false, false, true, true, Optional.empty(), true);
        fmt = StringFormatter.fromSettings(settings);
        assertTrue(
            fmt.format("<some xml=\"tag\">with te'xt & \tsymbols</some>")
                .contains("&amp; <span title=\"U+0009 CHARACTER TABULATION\">\ufffd</span>"),
            "HTML is escaped before characters are replaced");
    }

    @Test
    void testGetHTML() throws InvalidSettingsException {
        final var settings = new Settings(25, 5, true, true, true, true, Optional.empty(), true);
        final var fmt = StringFormatter.fromSettings(settings);

        final var testString = "Greatest taco place I ever knew is just around the corner";
        final var cell = StringCellFactory.create(testString);

        assertEquals(fmt.format(testString), fmt.getHTML(cell), "getHTML() should yield identical results as format()");

        final var doubleCell = DoubleCellFactory.create(42.0);
        assertThrows(IllegalArgumentException.class, () -> fmt.getHTML(doubleCell), "Non-String cells must throw");
    }

}
