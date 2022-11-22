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
 *   Apr 16, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FilterOptionsSettings;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.tests.common.workflow.WorkflowTestUtil;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class FileChooserPathAccessorTest {

    private Path m_tempDir;

    private Path m_links;

    private Path m_contentFolder;

    private Path m_contentFile;

    private Path m_fileLink;

    private Path m_folderLink;

    private Path m_ordinaryFile;

    private Path m_folderLinkToContentFile;

    @Mock
    private PortsConfiguration m_portsConfig;

    /**
     * Initializes the directory structure used for testing.
     *
     * @throws IOException
     */
    @Before
    public void initializeTestDirectory() throws IOException {
        m_tempDir = Files.createTempDirectory("file_chooser_path_accessor_test");
        m_links = m_tempDir.resolve("links");
        m_contentFolder = m_tempDir.resolve("content");
        m_contentFile = m_contentFolder.resolve("contentFile.txt");
        m_fileLink = m_links.resolve("fileLink");
        m_folderLink = m_links.resolve("folderLink");
        m_ordinaryFile = m_links.resolve("ordinaryFile");
        m_folderLinkToContentFile = m_folderLink.resolve("contentFile.txt");
        Files.createDirectory(m_contentFolder);
        Files.createFile(m_contentFile);
        Files.createDirectory(m_links);
        Files.createFile(m_ordinaryFile);
        Files.createSymbolicLink(m_folderLink, m_contentFolder);
        Files.createSymbolicLink(m_fileLink, m_contentFile);
    }

    /**
     * Tests whether links are correctly handled for the different filter modes.<br>
     * Links should only included if follow links is set to true i.e. links are not considered to be special files.
     *
     * @throws IOException not thrown
     * @throws InvalidSettingsException not thrown
     */
    @Test
    public void testLinkBehaviorOfGetFSPaths() throws IOException, InvalidSettingsException {
        final WorkflowManager workflowManager = WorkflowTestUtil.createAndLoadDummyWorkflow(m_tempDir);
        FilterMode filterMode = FilterMode.FILES_IN_FOLDERS;
        when(m_portsConfig.getInputPortLocation()).thenReturn(Collections.emptyMap());
        SettingsModelReaderFileChooser settingsModel = new SettingsModelReaderFileChooser("test", m_portsConfig,
            "foobar", EnumConfig.supportAll(filterMode), EnumSet.of(FSCategory.LOCAL));
        settingsModel.setLocation(new FSLocation(FSCategory.LOCAL, m_links.toAbsolutePath().toString()));
        // list files, no follow, no special files, no subfolders
        testGetFSPathsBehavior(settingsModel, new TestConfig(FilterMode.FILES_IN_FOLDERS), m_ordinaryFile);

        // list files, no follow, include special, no subfolders
        testGetFSPathsBehavior(settingsModel, new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.SPECIAL),
            m_ordinaryFile);

        // list files, follow, no special files, no subfolders
        testGetFSPathsBehavior(settingsModel, new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.FOLLOW),
            m_fileLink, m_ordinaryFile);

        // list files, follow, include special, no subfolders
        testGetFSPathsBehavior(settingsModel,
            new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.FOLLOW, FilterOption.SPECIAL), m_fileLink,
            m_ordinaryFile);

        // list files, subfolders, no follow, no special
        testGetFSPathsBehavior(settingsModel, new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.SUBFOLDERS),
            m_ordinaryFile);

        // list files, subfolders, no follow, special
        testGetFSPathsBehavior(settingsModel,
            new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.SUBFOLDERS, FilterOption.SPECIAL), m_ordinaryFile);

        // list files, follow, no special files, subfolders
        testGetFSPathsBehavior(settingsModel,
            new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.SUBFOLDERS, FilterOption.FOLLOW), m_fileLink,
            m_folderLinkToContentFile, m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(FilterMode.FILES_IN_FOLDERS, FilterOption.SUBFOLDERS,
            FilterOption.FOLLOW, FilterOption.SPECIAL), m_fileLink, m_folderLinkToContentFile, m_ordinaryFile);

        // list folders
        filterMode = FilterMode.FOLDERS;
        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode));

        // we don't follow, so this is not an actual folder
        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SPECIAL));

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.FOLLOW), m_folderLink);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.FOLLOW, FilterOption.SPECIAL),
            m_folderLink);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SUBFOLDERS));

        testGetFSPathsBehavior(settingsModel,
            new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.SPECIAL));

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.FOLLOW),
            m_folderLink);

        testGetFSPathsBehavior(settingsModel,
            new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.FOLLOW, FilterOption.SPECIAL),
            m_folderLink);

        // list files and folders
        filterMode = FilterMode.FILES_AND_FOLDERS;

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode), m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SPECIAL), m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.FOLLOW), m_fileLink, m_folderLink,
            m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.FOLLOW, FilterOption.SPECIAL),
            m_fileLink, m_folderLink, m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SUBFOLDERS), m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.SPECIAL),
            m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel, new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.FOLLOW),
            m_fileLink, m_folderLink, m_folderLinkToContentFile, m_ordinaryFile);

        testGetFSPathsBehavior(settingsModel,
            new TestConfig(filterMode, FilterOption.SUBFOLDERS, FilterOption.FOLLOW, FilterOption.SPECIAL), m_fileLink,
            m_folderLink, m_folderLinkToContentFile, m_ordinaryFile);

        WorkflowTestUtil.shutdownWorkflowManager(workflowManager);
    }

    private static void testGetFSPathsBehavior(final SettingsModelReaderFileChooser settingsModel,
        final TestConfig testConfig, final Path... expectedPaths) throws IOException, InvalidSettingsException {
        final SettingsModelFilterMode filterModel = settingsModel.getFilterModeModel();
        filterModel.setFilterMode(testConfig.m_filterMode);
        filterModel.setIncludeSubfolders(testConfig.m_options.contains(FilterOption.SUBFOLDERS));
        final FilterOptionsSettings filterOptions = filterModel.getFilterOptionsSettings();
        filterOptions.setFollowLinks(testConfig.m_options.contains(FilterOption.FOLLOW));
        filterOptions.setIncludeSpecialFiles(testConfig.m_options.contains(FilterOption.SPECIAL));

        try (FileChooserPathAccessor accessor = new FileChooserPathAccessor(settingsModel, Optional.empty())) {
            final List<FSPath> paths = accessor.getFSPaths(s -> {
            });
            String[] expectedStrings = Arrays.stream(expectedPaths).map(Path::toString).toArray(String[]::new);
            String[] actualStrings = paths.stream().map(Object::toString).toArray(String[]::new);
            assertArrayEquals(expectedStrings, actualStrings);
        }
    }

    private enum FilterOption {
            SUBFOLDERS, FOLLOW, SPECIAL;
    }

    private static final class TestConfig {

        private final EnumSet<FilterOption> m_options = EnumSet.noneOf(FilterOption.class);

        private final FilterMode m_filterMode;

        TestConfig(final FilterMode filterMode, final FilterOption... options) {
            m_filterMode = filterMode;
            m_options.addAll(Arrays.asList(options));
        }

    }

}
