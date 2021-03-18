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
 */
package org.knime.filehandling.core.fs.tests.integration.filesystemprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for byte channel operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class ByteChannelTest extends AbstractParameterizedFSTest {

    public ByteChannelTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_read_all() throws Exception {
        String testContent = "Some content to test this byte channel";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        String result;
        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            result = readFromByteChannel(byteChannel);
        }

        assertEquals(testContent, result);
    }

    @Test
    public void test_read_from_position() throws Exception {
        String testContent = "!!!This starts at byte number 3!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        String result;
        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            byteChannel.position(3);
            result = readFromByteChannel(byteChannel);
        }

        assertEquals("This starts at byte number 3!", result);
    }

    @Test
    public void test_truncate_less_than_channel_size_removes_trailing_content() throws Exception {
        String testContent = "After truncate (this is all gone)!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        String result;
        try (SeekableByteChannel byteChannel =
            Files.newByteChannel(file, EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ))) {
            byteChannel.truncate(14);
            result = readFromByteChannel(byteChannel);
        }

        assertEquals("After truncate", result);
    }

    @Test
    public void test_truncate_greater_than_channel_size_does_not_change_it() throws Exception {
        String testContent = "After truncate (this is still there)!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        String result;
        try (SeekableByteChannel byteChannel =
            Files.newByteChannel(file, EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ))) {
            byteChannel.truncate(143);
            result = readFromByteChannel(byteChannel);
        }

        assertEquals("After truncate (this is still there)!", result);
    }

    @Test
    public void test_channel_size() throws IOException {
        String testContent = "This has size 16";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            assertEquals(16, byteChannel.size());
        }
    }

    @Test
    public void test_seekable_position_can_be_set() throws IOException {
        String testContent = "Position will be changed!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            assertEquals(0, byteChannel.position());
            byteChannel.position(7);
            assertEquals(7, byteChannel.position());
        }
    }

    @Test
    public void test_byte_channel_close() throws Exception {
        String testContent = "Channel will be closed!";
        Path file = m_testInitializer.createFileWithContent(testContent, "dir", "fileName");

        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            assertEquals(true, byteChannel.isOpen());
            byteChannel.close();
            assertEquals(false, byteChannel.isOpen());
        }
    }

    @Test
    public void test_byte_channel_write() throws Exception {
        Path initiallyEmptyFile = m_testInitializer.createFile("dir", "fileName");

        String content = "A very useful sentence!";
        try (SeekableByteChannel byteChannel =
            Files.newByteChannel(initiallyEmptyFile, EnumSet.of(StandardOpenOption.WRITE))) {
            byteChannel.write(ByteBuffer.wrap(content.getBytes()));
        }

        List<String> result = Files.readAllLines(initiallyEmptyFile);
        assertEquals(1, result.size());
        assertEquals(content, result.get(0));
    }

    @Test
    public void test_byte_channel_overwrite() throws Exception {
        String content = "Initial content";
        Path file = m_testInitializer.createFileWithContent(content, "dir", "fileName");

        String overwrittenContent = "Overwritten content!";
        try (SeekableByteChannel byteChannel = Files.newByteChannel(file, EnumSet.of(StandardOpenOption.WRITE))) {
            byteChannel.write(ByteBuffer.wrap(overwrittenContent.getBytes()));
        }

        List<String> result = Files.readAllLines(file);
        assertEquals(1, result.size());
        assertEquals(overwrittenContent, result.get(0));
    }

    @Test
    public void test_create_new_file_and_write() throws Exception {
        Path directory = m_testInitializer.getTestCaseScratchDir().resolve("dir");
        Files.createDirectories(directory);

        Path nonExistingFile = directory.resolve("non-existing");

        String contentToWrite = "A very useful sentence!";
        try (SeekableByteChannel byteChannel = Files.newByteChannel(nonExistingFile,
            EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            byteChannel.write(ByteBuffer.wrap(contentToWrite.getBytes()));
        }

        List<String> result = Files.readAllLines(nonExistingFile);
        assertEquals(1, result.size());
        assertEquals(contentToWrite, result.get(0));
    }

    private String readFromByteChannel(final SeekableByteChannel byteChannel) throws IOException {
        String result = "";
        Charset charset = Charset.defaultCharset();
        ByteBuffer buffer = ByteBuffer.allocate(10);
        while (byteChannel.read(buffer) > 0) {
            buffer.flip();
            CharBuffer decode = charset.decode(buffer);
            result += decode.toString();
            buffer.clear();
        }
        return result;
    }

    @Test(expected = IOException.class)
    public void test_read_directory() throws IOException {
        Path directory = m_testInitializer.getTestCaseScratchDir().resolve("dir");
        Files.createDirectories(directory);

        Files.newByteChannel(directory, StandardOpenOption.READ).read(ByteBuffer.allocate(1));
    }

    @Test(expected = IOException.class)
    public void test_write_directory() throws IOException {
        Path directory = m_testInitializer.getTestCaseScratchDir().resolve("dir");
        Files.createDirectories(directory);
        Files.newByteChannel(directory, StandardOpenOption.WRITE);
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_write_existing_file() throws IOException {
        Path file = m_testInitializer.createFileWithContent("foobar", "dir", "file");
        Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_write_new_file_without_create_new() throws IOException {
        Path file = m_testInitializer.makePath("doesnotexist");
        Files.newByteChannel(file, StandardOpenOption.WRITE);
    }

    @Test(expected = NoSuchFileException.class)
    public void test_write_file_with_inexistent_parent_directory() throws IOException {
        Path file = m_testInitializer.makePath("dirdoesnotexist", "file");
        Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    @Test(expected = FileSystemException.class)
    public void test_write_file_where_parent_is_a_file() throws IOException {
        Path parent = m_testInitializer.createFileWithContent("content1", "file");
        Path file = parent.resolve("newfile");
        Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    @Test
    public void test_open_existing_file_read_write() throws IOException {
        Path file = m_testInitializer.createFileWithContent("content2", "file");
        try (SeekableByteChannel channel =
            Files.newByteChannel(file, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            assertEquals("content2", readFromByteChannel(channel));
            channel.write(ByteBuffer.wrap("testwrite".getBytes()));
            channel.position(0);
            assertEquals("content2testwrite", readFromByteChannel(channel));
        }
    }

    @Test
    public void test_open_new_file_read_write() throws IOException {
        Path file = m_testInitializer.makePath("newfile");
        try (SeekableByteChannel channel =
            Files.newByteChannel(file, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            channel.write(ByteBuffer.wrap("testwrite".getBytes()));
            channel.position(0);
            assertEquals("testwrite", readFromByteChannel(channel));
        }
    }

    @Test
    public void test_open_file_for_append() throws IOException {
        Path file = m_testInitializer.createFileWithContent("content3", "file");

        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.APPEND)) {
            channel.write(ByteBuffer.wrap("testwrite".getBytes()));
        }

        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            channel.position(0);
            assertEquals("content3testwrite", readFromByteChannel(channel));
        }
    }

    @Test
    public void test_open_file_for_truncation() throws IOException {
        Path file = m_testInitializer.createFileWithContent("content4", "file");

        try (SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ, StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.write(ByteBuffer.wrap("testwrite".getBytes()));
            channel.position(0);
            assertEquals("testwrite", readFromByteChannel(channel));
        }
    }

    @Test
    public void test_overwrite_updates_attribute_times() throws Exception {
        final Path file = m_testInitializer.createFileWithContent("a", "file");

        final BasicFileAttributes beforeTgtAttributes = Files.readAttributes(file, BasicFileAttributes.class);
        Thread.sleep(1000);
        try (ByteChannel out =
            Files.newByteChannel(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write(ByteBuffer.wrap(new byte[]{0}));
        }
        final BasicFileAttributes afterTgtAttributes = Files.readAttributes(file, BasicFileAttributes.class);

        assertTrue(beforeTgtAttributes.creationTime().toMillis() <= afterTgtAttributes.creationTime().toMillis());
        assertTrue(beforeTgtAttributes.lastModifiedTime().toMillis() < afterTgtAttributes.lastModifiedTime().toMillis());
    }
}
