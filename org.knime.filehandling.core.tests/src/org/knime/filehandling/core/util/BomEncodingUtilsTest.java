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
 *   2 Apr 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.Test;

/**
 * Contains tests for {@link BomEncodingUtils}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 *
 */
public final class BomEncodingUtilsTest {

    private static final String TEST_STRING = "Test";

    @Test
    public void defaultEncoding() throws IOException {
        testEncoding(Charset.defaultCharset());
    }

    @Test
    public void testASCIIEncoding() throws IOException {
        testEncoding(StandardCharsets.US_ASCII);
    }

    @Test
    public void testISO8859Encoding() throws IOException {
        testEncoding(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void testUTF8() throws IOException {
        testEncoding(StandardCharsets.UTF_8);
        testEncoding(StandardCharsets.UTF_8, ByteOrderMark.UTF_8);
    }

    @Test
    public void testUTF16LE() throws IOException {
        testEncoding(StandardCharsets.UTF_16LE);
        testEncoding(StandardCharsets.UTF_16LE, ByteOrderMark.UTF_16LE);
    }

    @Test
    public void testUTF16BE() throws IOException {
        testEncoding(StandardCharsets.UTF_16BE);
        testEncoding(StandardCharsets.UTF_16BE, ByteOrderMark.UTF_16BE);
    }

    @Test
    public void testUTF32LE() throws IOException {
        testEncoding(Charset.forName("UTF-32LE"));
        testEncoding(Charset.forName("UTF-32LE"), ByteOrderMark.UTF_32LE);
    }

    @Test
    public void testUTF32BE() throws IOException {
        testEncoding(Charset.forName("UTF-32BE"));
        testEncoding(Charset.forName("UTF-32BE"), ByteOrderMark.UTF_32BE);

    }

    @Test
    public void testSkipBom() throws Exception {
        var charsets = List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE,
            Charset.forName("UTF-32LE"), Charset.forName("UTF-32BE")).iterator();
        var boms = List.of(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE,
            ByteOrderMark.UTF_32BE).iterator();
        while (charsets.hasNext()) {
            var charset = charsets.next();
            testSkipBom(charset, null);
            testSkipBom(charset, boms.next());
        }
    }

    @Test
    public void testSkipBomChannelHasLessBytesThanBomSize() throws Exception {
        var encodedString = StandardCharsets.UTF_8.encode("f").array();
        try (var channel = new SeekableInMemoryByteChannel(encodedString)) {
            assertEquals("The channel has more bytes than expected.", 1, channel.size());
            BomEncodingUtils.skipBom(channel, StandardCharsets.UTF_8);
            assertEquals("The channel should be at position 0 because it contains fewer bytes than the bom has.", 0,
                channel.position());
        }
    }

    private static void testSkipBom(final Charset charset, final ByteOrderMark bom) throws IOException {
        final byte[] encodedString = createEncodedString(charset, bom);
        try (var channel = new SeekableInMemoryByteChannel(encodedString)) {
            BomEncodingUtils.skipBom(channel, charset);
            if (bom == null) {
                assertEquals("No BOM, so no bytes should have been skipped.", 0, channel.position());
            } else {
                assertEquals("The BOM should have been skipped.", bom.length(), channel.position());
            }
        }
    }


    private void testEncoding(final Charset charset) throws IOException {
        testEncoding(charset, null);
    }

    private void testEncoding(final Charset charset, final ByteOrderMark bom) throws IOException {
        final byte[] encodedString = createEncodedString(charset, bom);
        try (final Reader reader = BomEncodingUtils.createReader(new ByteArrayInputStream(encodedString), charset)) {
            assertArrayEquals(TEST_STRING.toCharArray(), IOUtils.toCharArray(reader));
        }
    }

    private static byte[] createEncodedString(final Charset charset, final ByteOrderMark bom) throws IOException {
        final byte[] encodedString = charset.encode(TEST_STRING).array();
        try (final BOMInputStream bomInput =
            new BOMInputStream(new ByteArrayInputStream(encodedString), ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE,
                ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_8)) {
            assertTrue(!bomInput.hasBOM());
        }
        if (bom == null) {
            return encodedString;
        }

        // add BOM
        final byte[] bomBytes = bom.getBytes();
        final byte[] bomEncodedString = new byte[bomBytes.length + encodedString.length];
        System.arraycopy(bomBytes, 0, bomEncodedString, 0, bomBytes.length);
        System.arraycopy(encodedString, 0, bomEncodedString, bomBytes.length, encodedString.length);
        // test that new string contains
        try (final BOMInputStream bomInput = new BOMInputStream(new ByteArrayInputStream(bomEncodedString), bom)) {
            assertTrue(bomInput.hasBOM());
        }
        return bomEncodedString;
    }

}
