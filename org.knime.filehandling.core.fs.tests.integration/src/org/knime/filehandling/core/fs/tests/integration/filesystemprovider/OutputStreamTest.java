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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for output stream operations on file systems.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class OutputStreamTest extends AbstractParameterizedFSTest {

    public OutputStreamTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_output_stream() throws Exception {
        Path file = m_testInitializer.createFile("dir", "fileName");

        String contentToWrite = "This is written by an output stream!!";
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            outputStream.write(contentToWrite.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals(1, fileContent.size());
        assertEquals(contentToWrite, fileContent.get(0));
    }

    @Test
    public void test_output_stream_append() throws Exception {
        String content = "This was already there";
        Path file = m_testInitializer.createFileWithContent(content, "dir", "fileName");

        String contentToWrite = ", but this was appended!";
        try (OutputStream outputStream = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
            outputStream.write(contentToWrite.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals("This was already there, but this was appended!", fileContent.get(0));
    }

    @Test
    public void test_output_stream_create_file() throws Exception {
        Path file = m_testInitializer.createFile("dir", "file");
        Path directory = file.getParent();
        Path nonExistingFile = directory.resolve("non-existing-file");

        String contentToWrite = "The wheel is come full circle: I am here.";
        try (OutputStream outputStream = //
            Files.newOutputStream(//
                nonExistingFile, //
                StandardOpenOption.CREATE_NEW, //
                StandardOpenOption.WRITE//
            )) {
            outputStream.write(contentToWrite.getBytes());
        }

        List<String> fileContent = Files.readAllLines(nonExistingFile);
        assertEquals(contentToWrite, fileContent.get(0));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void test_output_stream_create_new_file_failure() throws Exception {
        Path file = m_testInitializer.createFile("file");
        Files.newOutputStream(//
            file, //
            StandardOpenOption.CREATE_NEW, //
            StandardOpenOption.WRITE);
    }

    @Test
    public void test_output_stream_overwrite() throws Exception {
        String content = "I burn, I pine, I perish.";
        Path file = m_testInitializer.createFileWithContent(content, "dir", "file");

        String overwriteContent = "enough Shakespeare quotes!";
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            outputStream.write(overwriteContent.getBytes());
        }

        List<String> fileContent = Files.readAllLines(file);
        assertEquals(overwriteContent, fileContent.get(0));
    }

}
