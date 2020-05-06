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
 *   05.02.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;

/**
 * Tests the conversion from glob to regex pattern with the {@link GlobToRegexConverter}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class GlobToRegexTests {

    /**
     * .html – Matches all strings that end in .html
     */
    @Test
    public void match_all_html_glob() {
        final String glob = "**.html";
        final String html = "abd/cd/ef.html";
        final String no_html = "abc/de/fg.somthing";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(no_html).matches());
        assertFalse(regEx.matcher("").matches());
        assertTrue(regEx.matcher(html).matches());
        assertTrue(regEx.matcher(".html").matches());
    }

    /**
     * ??? – Matches all strings with exactly three letters or digits
     */
    @Test
    public void match_three_letters_or_digits() {
        final String glob = "???";
        final String threeLetters = "abc";
        final String threeDigits = "123";
        final String moreLetters = "abcde";
        final String lessLetter = "ab";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(moreLetters).matches());
        assertFalse(regEx.matcher(lessLetter).matches());
        assertTrue(regEx.matcher(threeLetters).matches());
        assertTrue(regEx.matcher(threeDigits).matches());
    }

    /**
     * *[0-9]* – Matches all strings containing a numeric value
     */
    @Test
    public void match_numeric_values() {
        final String glob = "*[0-9]*";
        final String threeLetters = "abc";
        final String threeDigits = "123";
        final String LettersAndNumeric = "abc22de";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(threeLetters).matches());

        assertTrue(regEx.matcher(LettersAndNumeric).matches());
        assertTrue(regEx.matcher(threeDigits).matches());
    }

    /**
     * a?*.java – Matches any string beginning with a, followed by at least one letter or digit, and ending with .java
     */
    @Test
    public void match_begin_a_end_java() {
        final String glob = "a?*.java";
        final String javaString = "abc.java";
        final String noJavaString = "abc";
        final String NotStartwithA = "bc.java";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(noJavaString).matches());

        assertFalse(regEx.matcher(NotStartwithA).matches());
        assertTrue(regEx.matcher(javaString).matches());
    }

    /**
     * a\?*.java – Matches any string beginning with a? ending with .java Test escaping of meta characters
     */
    @Test
    public void test_glob_meta() {
        final String glob = "a\\?*.java";
        final String javaString = "a?bc.java";
        final String noJavaString = "abc";
        final String NotStartwithA = "bc.java";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(noJavaString).matches());

        assertFalse(regEx.matcher(NotStartwithA).matches());
        assertTrue(regEx.matcher(javaString).matches());
    }

    /**
     * {foo*,*[0-9]*} – Matches any string beginning with foo or any string containing a numeric value
     */
    @Test
    public void match_foo_or_numeric() {
        final String glob = "{foo*,*[0-9]*}";
        final String javaString = "foo.java";
        final String numeric = "12322";
        final String NotStartwithfooNotNumeric = "bc.java";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(NotStartwithfooNotNumeric).matches());

        assertTrue(regEx.matcher(numeric).matches());
        assertTrue(regEx.matcher(javaString).matches());
    }

    /**
     * .html – Matches all strings that end in .html
     */
    @Test
    public void match_file_name_html_glob() {
        final String glob = "*.html";
        final String html = "abd/cd/ef.html";
        final String no_html = "abc/de/fg.somthing";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(no_html).matches());
        assertFalse(regEx.matcher("").matches());
        assertFalse(regEx.matcher(html).matches());
        assertTrue(regEx.matcher(".html").matches());
        assertTrue(regEx.matcher("something.html").matches());
    }

    /**
     * **.{htm,html,pdf} – Matches any string ending with .htm, .html or .pdf
     */
    @Test
    public void match_file_ending_set_glob() {
        final String glob = "**.{htm,html,pdf}";

        final String html = "abd/cd/ef.html";
        final String no_html = "abc/de/fg.somthing";
        final String pdf = "abc/de/fg.pdf";
        final String htm = "abc/de/fg.htm";
        final String start_with_html = "html/de/fg.htmls";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher(no_html).matches());
        assertFalse(regEx.matcher("").matches());
        assertFalse(regEx.matcher(start_with_html).matches());
        assertTrue(regEx.matcher(html).matches());
        assertTrue(regEx.matcher(".html").matches());
        assertTrue(regEx.matcher(pdf).matches());
        assertTrue(regEx.matcher(htm).matches());
        assertTrue(regEx.matcher("something.html").matches());
    }

    /**
     * Tests escaping
     */
    @Test
    public void test_escaping_works() {
        final String glob = "\\?";
        final Pattern regEx = GlobToRegexConverter.convert(glob, '/');
        assertFalse(regEx.matcher("b").matches());
        assertTrue(regEx.matcher("?").matches());
    }

    /**
     * Tests exception for missing group end
     */
    @Test(expected = PatternSyntaxException.class)
    public void test_exception_for_missing_group_end() {
        final String glob = "ab[cde";
        GlobToRegexConverter.convert(glob, '/');
    }

    /**
     * Tests exception for escaping at end
     */
    @Test(expected = PatternSyntaxException.class)
    public void test_exception_for_escaping_at_end_of_glob() {
        final String glob = "abcde\\";
        GlobToRegexConverter.convert(glob, '/');
    }
}
