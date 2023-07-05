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
 *   Jun 1, 2023 (adrian.nembach): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for the {@link ChunkReader}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class ChunkReaderTest {

    private static final Charset UTF_8 = Charset.forName("utf8");

    @ParameterizedTest
    @MethodSource("provideCharsets")
    void testReadingInsideOfTheChunk(final Charset charset) throws Exception {
        var text = "Some text to be read by the ChunkReader for testing.";
        int chunkSize = charset.encode(text).limit() + 3;
        try (var reader = createReader(text, charset, chunkSize, ".")) {
            var cbuf = new char[3];
            int numRead = reader.read(cbuf);
            assertEquals(3, numRead, "It should be possible to read three chars.");
            assertArrayEquals("Som".toCharArray(), cbuf, "Unexpected chars read.");
        }
    }

    @ParameterizedTest
    @MethodSource("provideCharsets")
    void testEndSequenceTerminationOutsideOfChunk(final Charset charset) throws Exception {
        var whatTheReaderShouldRead = "Foo bar baz.";
        int chunkSize = charset.encode(whatTheReaderShouldRead).limit() - (int)charset.newEncoder().maxBytesPerChar();
        var text = whatTheReaderShouldRead + " There is more here!";
        try (var reader = createReader(text, charset, chunkSize, ".")) {
            var cbuf = new char[100];
            int numRead = reader.read(cbuf);
            char[] expectedChars = whatTheReaderShouldRead.toCharArray();
            assertEquals(expectedChars.length, numRead, "The reader should have terminated at '.'.");
            assertArrayEquals(expectedChars, Arrays.copyOf(cbuf, numRead), "Unexpected chars read.");
        }
    }

    @ParameterizedTest
    @MethodSource("provideCharsets")
    void testEndSequenceAtChunkEnd(final Charset charset) throws Exception {
        var textAligningWithChunkSize = "Foo!";
        var text = textAligningWithChunkSize + " There is more here!";
        try (var reader = createReader(text, charset, charset.encode(textAligningWithChunkSize).limit(), "!")) {
            var cbuf = new char[text.length()];
            int numRead = reader.read(cbuf);
            var expectedChars = text.toCharArray();
            assertEquals(expectedChars.length, numRead, "The reader should read the entire text.");
            assertArrayEquals(expectedChars, cbuf, "Unexpected characters read.");
        }
    }

    @ParameterizedTest
    @MethodSource("provideCharsets")
    void testEndSequenceAtChunkEndAndReadLen(final Charset charset) throws Exception {
        var textAligningWithChunkSize = "Foo!";
        var more = " There is more here!";
        var text = textAligningWithChunkSize + more;
        try (var reader = createReader(text, charset, charset.encode(textAligningWithChunkSize).limit(), "!")) {
            var cbuf = new char[textAligningWithChunkSize.length()];
            int numRead = reader.read(cbuf);
            var expectedChars = textAligningWithChunkSize.toCharArray();
            assertEquals(expectedChars.length, numRead, "The first read should read up until chunksize.");
            assertArrayEquals(expectedChars, cbuf, "Unexpected characters read.");
            cbuf = new char[more.length()];
            numRead = reader.read(cbuf);
            expectedChars = more.toCharArray();
            assertEquals(expectedChars.length, numRead,
                "The reader should continue reading until it hits the end sequence.");
            assertArrayEquals(expectedChars, cbuf, "Unexpected characters read.");
        }
    }

    private static Stream<Arguments> provideCharsets() {
        return Stream.of(new Charset[]{//
            StandardCharsets.ISO_8859_1, //
            StandardCharsets.US_ASCII, //
            StandardCharsets.UTF_16, //
            StandardCharsets.UTF_16BE, //
            StandardCharsets.UTF_16LE, //
            StandardCharsets.UTF_8//
        }).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("provideCharsets")
    void testNoMoreReadsPossibleAfterEndSequence(final Charset charset) throws Exception {
        var text = "Foo!Bar and a lot more stuff that could be read.";
        var chunkSize = charset.encode("Foo").limit();
        try (var reader = createReader(text, charset, chunkSize, "!")) {
            var cbuf = new char[10];
            int n = reader.read(cbuf);
            assertEquals("Foo!", String.valueOf(cbuf, 0, n), "The reader should read 'Foo!'");
            assertEquals(-1, reader.read(cbuf),
                "No more reading should be possible after detection of the end seqeunce.");
        }
    }

    @Test
    void testGracefullyHandleClosedChannel() throws Exception {
        @SuppressWarnings("resource")
        var channel = createChannel("foo bar baz", UTF_8);
        try (var reader = new ChunkReader(channel, UTF_8, 5, ".", 3)) {
            channel.close();
            assertEquals(-1, reader.read(), "A closed channel should result in EOF being returned.");
        }
    }

    @Test
    void testCloseClosesUnderlyingChannel() throws Exception {
        @SuppressWarnings("resource")
        var channel = createChannel("foo", UTF_8);
        try (var reader = new ChunkReader(channel, UTF_8, 4, ".", 3)) {
            // do nothing, we just want to test if close is correctly invoked
        }
        assertFalse(channel.isOpen(), "The reader should close the channel.");
    }

    @Test
    void testReplaceMalformedInput() throws Exception {
        byte[] malformedBytes = {(byte)0xC3, (byte)0x28}; // Invalid 2 Octet Sequence
        try (var channel = Channels.newChannel(new ByteArrayInputStream(malformedBytes));
                var reader = new ChunkReader(channel, StandardCharsets.UTF_8, 10, "!", 5)) {
            assertEquals('\uFFFD', reader.read(),
                "Malformed input should be replaced with the unicode replacement character \\uFFFD.");
        }
    }

    @SuppressWarnings("resource") // channel is closed by the ChunkReader
    private static ChunkReader createReader(final String text, final Charset charset, final long chunkSize,
        final String endSequence) {
        return new ChunkReader(createChannel(text, charset), charset, chunkSize, endSequence, 5);
    }

    private static ReadableByteChannel createChannel(final String text, final Charset charset) {
        var byteBuffer = charset.encode(text);
        var bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        var channel = Channels.newChannel(new ByteArrayInputStream(bytes));
        return channel;
    }
}
