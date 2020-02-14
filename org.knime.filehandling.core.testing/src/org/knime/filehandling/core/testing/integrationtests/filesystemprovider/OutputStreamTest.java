package org.knime.filehandling.core.testing.integrationtests.filesystemprovider;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;

/**
 * Test class for output stream operations on file systems.
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class OutputStreamTest extends AbstractParameterizedFSTest {

	public OutputStreamTest(String fsType, FSTestInitializer testInitializer) {
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
		try (OutputStream outputStream = 
			Files.newOutputStream(
				nonExistingFile,
				StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE
			)
		) {
			outputStream.write(contentToWrite.getBytes());
		}
		
		List<String> fileContent = Files.readAllLines(nonExistingFile);
		assertEquals(contentToWrite, fileContent.get(0));
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
