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
 *   Mar 27, 2024 (Paul Bärnreuther): created
 */

package org.knime.base.node.io.filehandling.csv.reader2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;

/**
 *
 * @author Paul Bärnreuther
 */
public class FileChooserPathAccessorTest extends LocalWorkflowContextTest {

    @TempDir
    private Path m_tempDir;

    private Path m_contentFolder;

    private Path m_contentFile;

    /**
     * Initializes the directory structure used for testing.
     *
     * @throws IOException
     */
    @BeforeEach
    void initializeTestDirectory() throws IOException {
        m_contentFolder = m_tempDir.resolve("content");
        m_contentFile = m_contentFolder.resolve("contentFile.txt");
        Files.createDirectory(m_contentFolder);
        Files.createFile(m_contentFile);
    }

    @Test
    void testGetPaths() throws IOException, InvalidSettingsException {
        final var fileChooser = toLocalFileChooser(m_contentFile.toAbsolutePath().toString());
        testGetPathsHappy(fileChooser, m_contentFile);
    }

    @Test
    void testThrowsOnEmptyPath() throws IOException, InvalidSettingsException {
        testGetPathsError(toLocalFileChooser(""), "Please specify a file");
    }

    @Test
    void testThrowsOnFolder() throws IOException, InvalidSettingsException {
        final var folderPathString = m_contentFolder.toAbsolutePath().toString();
        testGetPathsError(toLocalFileChooser(folderPathString),
            String.format("%s is a folder. Please specify a file.", folderPathString));
    }

    @Test
    void testThrowsOnMissing() throws IOException, InvalidSettingsException {
        final var missingFile = "foo";
        testGetPathsError(toLocalFileChooser(missingFile),
            String.format("The specified file %s does not exist.", missingFile));
    }

    private static FileChooser toLocalFileChooser(final String path) {
        return new FileChooser(new FSLocation(FSCategory.LOCAL, path));
    }

    private static void testGetPathsHappy(final FileChooser fileChooser, final Path... expectedPaths)
        throws IOException, InvalidSettingsException {

        try (FileChooserPathAccessor accessor = new FileChooserPathAccessor(fileChooser)) {
            final List<Path> paths = accessor.getPaths();
            String[] expectedStrings = Arrays.stream(expectedPaths).map(Path::toString).toArray(String[]::new);
            String[] actualStrings = paths.stream().map(Object::toString).toArray(String[]::new);
            assertArrayEquals(expectedStrings, actualStrings);
        }
    }

    private static void testGetPathsError(final FileChooser fileChooser, final String expectedErrorMessage)
        throws IOException, InvalidSettingsException {

        try (FileChooserPathAccessor accessor = new FileChooserPathAccessor(fileChooser)) {
            assertThat(assertThrows(InvalidSettingsException.class, () -> accessor.getPaths())).message()
                .isEqualTo(expectedErrorMessage);
        }
    }
}
