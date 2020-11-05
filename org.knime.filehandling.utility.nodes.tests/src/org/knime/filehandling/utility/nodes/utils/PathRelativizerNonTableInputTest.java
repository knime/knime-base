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
package org.knime.filehandling.utility.nodes.utils;

import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.utils.PathRelativizer;
import org.knime.filehandling.utility.nodes.utils.PathRelativizerNonTableInput;

/**
 * Tests for the {@link PathRelativizer}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class PathRelativizerNonTableInputTest {

    private Path m_root;

    private Path m_fooBar;

    private Path m_fooBarFolderSubfolderFile;

    /**
     * Constructor.
     */
    public PathRelativizerNonTableInputTest() {
        @SuppressWarnings("resource") // cannot be closed anyways
        final FileSystem fs = FileSystems.getDefault();
        m_root = fs.getRootDirectories().iterator().next();
        m_fooBar = m_root.resolve("foo").resolve("bar");
        m_fooBarFolderSubfolderFile = m_fooBar.resolve("folder").resolve("subfolder").resolve("file.txt");
    }

    /**
     * Test for a file when includeParent folder is false and true.
     */
    @Test
    public void testFileMode() {
        final Path file = m_fooBarFolderSubfolderFile.getFileName();
        testFileMode(false, false, file);
        testFileMode(true, false, file);
        testFileMode(false, true, file);
        testFileMode(true, true, file);
    }

    private void testFileMode(final boolean includeParent, final boolean flattenHierarchy, final Path file) {
        final PathRelativizer pathManipulatorIncludeParentFalse = new PathRelativizerNonTableInput(
            m_fooBarFolderSubfolderFile, includeParent, FilterMode.FILE, flattenHierarchy);
        assertEquals(file.toString(), pathManipulatorIncludeParentFalse.apply(m_fooBarFolderSubfolderFile));
    }

    /**
     * Test for a folder when IncludeParentFolder is true and rootPath parent != null.
     */
    @Test
    public void testFolderModeIncludeParentFolder() {
        final PathRelativizer pathManipulator =
            new PathRelativizerNonTableInput(m_fooBar, true, FilterMode.FOLDER, false);
        final Path barFolderSubfolderFile = m_fooBar.getParent().relativize(m_fooBarFolderSubfolderFile);
        assertEquals(barFolderSubfolderFile.toString(), pathManipulator.apply(m_fooBarFolderSubfolderFile));
    }

    /**
     * Test for a folder when IncludeParentFolder is false and rootPath parent != null.
     */
    @Test
    public void testFolderModeExcludeParentFolder() {
        final PathRelativizer pathManipulator =
            new PathRelativizerNonTableInput(m_fooBar, false, FilterMode.FOLDER, false);
        final Path folderSubfolderFile = m_fooBar.relativize(m_fooBarFolderSubfolderFile);
        assertEquals(folderSubfolderFile.toString(), pathManipulator.apply(m_fooBarFolderSubfolderFile));
    }

    /**
     * Test for a folder when IncludeParentFolder is true and rootPath parent == null.
     */
    @Test
    public void testFolderModeIncludeParentFolderNull() {
        final PathRelativizer pathManipulator =
            new PathRelativizerNonTableInput(m_root, true, FilterMode.FOLDER, false);
        final Path rootFooFile = m_root.resolve("foo").resolve("file");
        assertEquals(m_root.relativize(rootFooFile).toString(), pathManipulator.apply(rootFooFile));
    }

    /**
     * Tests the flattening mode for folders.
     */
    @Test
    public void testFolderModeFlattening() {
        testFolderModeFlattening(m_fooBar, false,
            m_fooBar.relativize(m_fooBar.resolve(m_fooBarFolderSubfolderFile.getFileName())));
        testFolderModeFlattening(m_fooBar, true,
            m_fooBar.getParent().relativize(m_fooBar.resolve(m_fooBarFolderSubfolderFile.getFileName())));
        testFolderModeFlattening(m_root, true, m_fooBarFolderSubfolderFile.getFileName());
    }

    private void testFolderModeFlattening(final Path base, final boolean includeParent, final Path res) {
        final PathRelativizer pathManipulator =
            new PathRelativizerNonTableInput(base, includeParent, FilterMode.FOLDER, true);
        assertEquals(res.toString(), pathManipulator.apply(m_fooBarFolderSubfolderFile));
    }

}
