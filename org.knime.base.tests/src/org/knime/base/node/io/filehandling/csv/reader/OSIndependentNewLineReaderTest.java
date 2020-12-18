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
 *   Dec 17, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Tests the {@link OSIndependentNewLineReader}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class OSIndependentNewLineReaderTest extends TestCase {

    private static final String WIN_LB = "\r\n";

    private static final String LINUX_LB = "\n";

    private static final String MAC_LB = "\r";

    private static Reader createInputStream(final String string) {
        return createInputStream(string, Charset.defaultCharset());
    }

    private static Reader createInputStream(final String string, final Charset cs) {
        return new OSIndependentNewLineReader(new InputStreamReader(new ByteArrayInputStream(string.getBytes(cs)), cs));
    }

    private static String getText(final String linuxLineSepString, final String lineBreak) {
        return linuxLineSepString.replace(LINUX_LB, lineBreak);
    }

    private static void runTests(final String expected) throws IOException {
        runTests(expected, Charset.defaultCharset());
    }

    private static void runTests(final String expected, final Charset cs) throws IOException {
        try (final Reader reader = createInputStream(getText(expected, WIN_LB), cs)) {
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(getText(expected, MAC_LB), cs)) {
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(expected, cs)) {
            assertEquals(expected, IOUtils.toString(reader));
        }
    }

    /**
     * Tests a single line break.
     *
     * @throws IOException
     */
    @Test
    public void testSingleLineBreak() throws IOException {
        runTests(LINUX_LB);
    }

    /**
     * Tests a line break at the beginning of the file.
     *
     * @throws IOException
     */
    @Test
    public void testLineBreakAtTheBeginning() throws IOException {
        runTests(LINUX_LB + "aa");
    }

    /**
     * Tests a line break at the end of the file.
     *
     * @throws IOException
     */
    @Test
    public void testLineBreakAtTheEnd() throws IOException {
        runTests("aa" + LINUX_LB);
    }

    /**
     * Tests a line break in the middle of the file.
     *
     * @throws IOException
     */
    @Test
    public void testLineBreakInTheMiddle() throws IOException {
        runTests("a" + LINUX_LB + "a");
    }

    /**
     * Tests repeated line breaks.
     *
     * @throws IOException
     */
    @Test
    public void testRepeatedLineBreaks() throws IOException {
        runTests(LINUX_LB + LINUX_LB + "a" + LINUX_LB + LINUX_LB + "a" + LINUX_LB + LINUX_LB);
    }

    /**
     * Tests different line breaks in the very same file. Note that Mac followed by a Linux line break equals a Windows
     * linebreak and is therefore not tested.
     *
     * @throws IOException
     */
    @Test
    public void testDifferentOSLineBreaks() throws IOException {
        try (final Reader reader = createInputStream(
            WIN_LB + LINUX_LB + MAC_LB + "a" + LINUX_LB + LINUX_LB + WIN_LB + "a" + WIN_LB + MAC_LB + WIN_LB)) {
            assertEquals(LINUX_LB + LINUX_LB + LINUX_LB + "a" + LINUX_LB + LINUX_LB + LINUX_LB + "a" + LINUX_LB
                + LINUX_LB + LINUX_LB, IOUtils.toString(reader));
        }
    }

    /**
     * Tests random text with line breaks at different positions.
     *
     * @throws IOException
     */
    @Test
    public void testRandomTextWithVariousEncodingsLineBreak() throws IOException {
        final String expected = LINUX_LB + "Everything" + LINUX_LB + "worked" + LINUX_LB + LINUX_LB + "as" + LINUX_LB
            + "expected" + LINUX_LB;
        final Charset[] encodings = new Charset[]{Charset.defaultCharset(), StandardCharsets.ISO_8859_1,
            StandardCharsets.UTF_16, StandardCharsets.UTF_8, StandardCharsets.US_ASCII};
        for (final Charset cs : encodings) {
            runTests(expected, cs);
        }
    }

    /**
     * Tests that {@link Reader#skip(long)} works as expected.
     *
     * @throws IOException
     */
    @Test
    public void testSkip() throws IOException {
        final String expected = "\n\n";
        final String toSkip = "\n\n\n\n";
        try (final Reader reader = createInputStream(getText(toSkip, WIN_LB))) {
            reader.skip(2);
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(getText(toSkip, MAC_LB))) {
            reader.skip(2);
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(toSkip)) {
            reader.skip(2);
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(getText(toSkip, WIN_LB))) {
            reader.skip(1);
            reader.skip(1);
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(
            WIN_LB + LINUX_LB + MAC_LB + MAC_LB + WIN_LB + LINUX_LB + LINUX_LB + WIN_LB + MAC_LB + WIN_LB)) {
            for (int i = 0; i < 8; i++) {
                reader.skip(1);
            }
            assertEquals(expected, IOUtils.toString(reader));
        }
        try (final Reader reader = createInputStream(
            WIN_LB + LINUX_LB + MAC_LB + MAC_LB + WIN_LB + LINUX_LB + LINUX_LB + WIN_LB + MAC_LB + WIN_LB)) {
            reader.skip(3);
            reader.skip(3);
            reader.skip(2);
            assertEquals(expected, IOUtils.toString(reader));
        }
    }

    /**
     * Tests that {@link Reader#markSupported()} returns {@code false}.
     *
     * @throws IOException
     */
    @Test
    public void testMark() throws IOException {
        try (final Reader reader = createInputStream(
            WIN_LB + LINUX_LB + MAC_LB + MAC_LB + WIN_LB + LINUX_LB + LINUX_LB + WIN_LB + MAC_LB + WIN_LB)) {
            assertFalse(reader.markSupported());
        }
    }
}
