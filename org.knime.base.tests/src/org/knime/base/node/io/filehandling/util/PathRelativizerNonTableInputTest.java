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
 *   Sep 8, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for the {@link PathRelativizer}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public final class PathRelativizerNonTableInputTest {

    private static final String RESULT_FILE_NAME = "testFile.txt";

    private static final String RESULT_FOLDER_NAME = "subfolder2";

    private static final String RESULT_FOLDER_NAME_W_PARENT = "subfolder1/subfolder2";

    @Mock
    private Path m_rootPath;

    @Mock
    private Path m_filePath;

    @Mock
    private Path m_absoluteNormalizedRootPath;

    @Mock
    private Path m_fileNameRoot;

    /**
     * Init test setup.
     */
    @Before
    public void init() {
        final Path absoluteRootPath = Mockito.mock(Path.class);
        when(m_rootPath.toAbsolutePath()).thenReturn(absoluteRootPath);
        when(absoluteRootPath.normalize()).thenReturn(m_absoluteNormalizedRootPath);
    }

    /**
     * Test for a file when includeParent folder is false and true.
     */
    @Test
    public void testFileMode() {
        Mockito.when(m_absoluteNormalizedRootPath.getFileName()).thenReturn(m_fileNameRoot);
        Mockito.when(m_fileNameRoot.toString()).thenReturn(RESULT_FILE_NAME);

        /*
         * includeParentFolder false or true should always return the same
         * even the methode should be never called with true in file mode
         */
        final PathRelativizer pathManipulatorIncludeParentFalse =
            new PathRelativizerNonTableInput(m_rootPath, false, FilterMode.FILE);
        final PathRelativizer pathManipulatorIncludeParentTrue =
            new PathRelativizerNonTableInput(m_rootPath, true, FilterMode.FILE);

        assertEquals(RESULT_FILE_NAME, pathManipulatorIncludeParentFalse.apply(m_filePath));
        assertEquals(RESULT_FILE_NAME, pathManipulatorIncludeParentTrue.apply(m_filePath));
        assertEquals(pathManipulatorIncludeParentFalse.apply(m_filePath),
            pathManipulatorIncludeParentTrue.apply(m_filePath));

    }

    private Path makeFilePathAbsoluteNormalized() {
        final Path p = Mockito.mock(Path.class);
        final Path absoluteFilePath = Mockito.mock(Path.class);
        when(m_filePath.toAbsolutePath()).thenReturn(absoluteFilePath);
        when(absoluteFilePath.normalize()).thenReturn(p);

        return p;
    }

    /**
     * Test for a folder when IncludeParentFolder is true and rootPath parent != null.
     */
    @Test
    public void testFolderModeIncludeParentFolder() {
        final Path absoluteNormalizedSourcePath = makeFilePathAbsoluteNormalized();
        final Path relativizedPath = Mockito.mock(Path.class);
        final Path rootPathParent = Mockito.mock(Path.class);

        //Parent is not null
        when(m_absoluteNormalizedRootPath.getParent()).thenReturn(Mockito.mock(Path.class));

        when(m_absoluteNormalizedRootPath.getParent()).thenReturn(rootPathParent);
        when(rootPathParent.relativize(absoluteNormalizedSourcePath)).thenReturn(relativizedPath);
        when(relativizedPath.toString()).thenReturn(RESULT_FOLDER_NAME_W_PARENT);

        final PathRelativizer pathManipulator = new PathRelativizerNonTableInput(m_rootPath, true, FilterMode.FOLDER);

        assertEquals(RESULT_FOLDER_NAME_W_PARENT, pathManipulator.apply(m_filePath));
    }

    /**
     * Test for a folder when IncludeParentFolder is true and rootPath parent == null.
     */
    @Test
    public void testFolderModeIncludeParentFolderNull() {
        final Path absoluteNormalizedSourcePath = makeFilePathAbsoluteNormalized();
        final Path relativizedPath = Mockito.mock(Path.class);
        final Path rootPathRoot = Mockito.mock(Path.class);

        //Parent is null
        when(m_absoluteNormalizedRootPath.getParent()).thenReturn(null);

        when(m_absoluteNormalizedRootPath.getRoot()).thenReturn(rootPathRoot);
        when(rootPathRoot.relativize(absoluteNormalizedSourcePath)).thenReturn(relativizedPath);
        when(relativizedPath.toString()).thenReturn(RESULT_FOLDER_NAME);

        final PathRelativizer pathManipulator =  new PathRelativizerNonTableInput(m_rootPath, true, FilterMode.FOLDER);

        assertEquals(RESULT_FOLDER_NAME, pathManipulator.apply(m_filePath));
    }

    /**
     * Test for a folder when IncludeParentFolder is false.
     */
    @Test
    public void testFolderMode() {
        final Path relativizedPath = Mockito.mock(Path.class);
        when(m_absoluteNormalizedRootPath.relativize(makeFilePathAbsoluteNormalized())).thenReturn(relativizedPath);
        when(relativizedPath.toString()).thenReturn(RESULT_FOLDER_NAME);

        final PathRelativizer pathManipulator = new PathRelativizerNonTableInput(m_rootPath, false, FilterMode.FOLDER);

        assertEquals(RESULT_FOLDER_NAME, pathManipulator.apply(m_filePath));
    }
}
