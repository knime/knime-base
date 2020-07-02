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
package org.knime.filehandling.core.fs.tests.workflow.connector;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.fs.testinitializer.FSTestConfig;
import org.knime.filehandling.core.fs.testinitializer.FSTestInitializerManager;
import org.knime.filehandling.core.fs.testinitializer.FSTestPropertiesResolver;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.testing.FSTestInitializer;

/**
 * Node model for the FS Testing node.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 *
 */
public class TestFileSystemConnectorNodeModel extends NodeModel {

    private final TestFileSystemConnectorSettings m_settings = new TestFileSystemConnectorSettings();

    private FSConnection m_fsConnection;

    private String m_fsId;

    protected TestFileSystemConnectorNodeModel() {
        super(null, new PortType[]{FileSystemPortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_fsId = FSConnectionRegistry.getInstance().getKey();

        final FSTestConfig testConfig = new FSTestConfig(FSTestPropertiesResolver.forWorkflowTests());

        final String fsType = getFSTypeToTest(testConfig);

        final FSLocationSpec fsLocationSpec =
            FSTestInitializerManager.instance().createFSLocationSpec(fsType, testConfig.getSettingsForFSType(fsType));

        return new PortObjectSpec[]{createSpec(fsType, fsLocationSpec)};
    }

    private static String getFSTypeToTest(final FSTestConfig testConfig)
        throws InvalidSettingsException {

        final Optional<String> fsToTest = testConfig.getFSTypeToTest();
        if (!fsToTest.isPresent()) {
            throw new InvalidSettingsException(String.format(
                "Missing property '%s'. Please add it to fs-test.properties and specify the file system to test.",
                FSTestConfig.KEY_TEST_FS));
        }
        return fsToTest.get();
    }

    @SuppressWarnings("resource")
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {


        final FSTestConfig testConfig = new FSTestConfig(FSTestPropertiesResolver.forWorkflowTests());
        final String fsType = getFSTypeToTest(testConfig);

        exec.getProgressMonitor().setProgress(0, String.format("Connecting to file system %s", fsType));

        final FSTestInitializer<?,?> initializer =
            FSTestInitializerManager.instance().createInitializer(fsType, testConfig.getSettingsForFSType(fsType));

        Files.createDirectories(initializer.getFileSystem().getWorkingDirectory());

        deleteWorkingDirOnConnectionClose(initializer);

        exec.getProgressMonitor().setProgress(0.4);

        if (testConfig.getFixtureDir().isPresent() && !testConfig.getFixtureDir().get().isEmpty()) {
            try {
                final Path fixtureDir = Paths.get(testConfig.getFixtureDir().get());
                copyFixturesToWorkingDir(initializer.getFileSystem(), fixtureDir, exec);
            } catch (Exception e) {
                initializer.getFSConnection().closeInBackground();
                throw e;
            }
        }

        exec.setProgress(1);

        m_fsConnection = initializer.getFSConnection();
        FSConnectionRegistry.getInstance().register(m_fsId, m_fsConnection);

        return new PortObject[]{
            new FileSystemPortObject(createSpec(fsType, m_fsConnection.getFileSystem().getFSLocationSpec()))};

    }

    private static void deleteWorkingDirOnConnectionClose(final FSTestInitializer<?, ?> initializer) {
        final FSFileSystem<FSPath> fs = (FSFileSystem<FSPath>)initializer.getFileSystem();
        initializer.getFileSystem().registerCloseable(() -> {
            FSFiles.deleteRecursively(fs.getWorkingDirectory());
        });
    }

    private void copyFixturesToWorkingDir(final FSFileSystem<?> targetFs, final Path localFixtureDir,
        final ExecutionMonitor exec) throws IOException {

        exec.setMessage("Listing fixture files to upload");

        if (!Files.isDirectory(localFixtureDir)) {
            throw new IOException(String.format("Fixture directory %s does not exist or is not a directory.",
                localFixtureDir.toString()));
        }

        @SuppressWarnings("resource")
        final PathMatcher fixtureFilter = FileSystems.getDefault().getPathMatcher(m_settings.getFixtureFilter());

        final List<Path> fixturesToUpload = getFixturesToUpload(localFixtureDir, fixtureFilter);

        exec.setProgress(0.5, "Copying fixtures");

        if (!fixturesToUpload.isEmpty()) {
            copyFixtures(targetFs, localFixtureDir, fixturesToUpload, exec.createSubProgress(0.5));
        }

        exec.setProgress(1);
    }

    private static void copyFixtures(final FSFileSystem<?> targetFs, final Path localFixtureDir,
        final List<Path> fixturesToUpload, final ExecutionMonitor exec) throws IOException {

        final double progressPerFile = 1.0 / fixturesToUpload.size();
        double currProgress = 0;
        for (final Path relSrcFile : fixturesToUpload) {
            final FSPath targetFile = toRelativeTargetFile(targetFs, relSrcFile);

            exec.setMessage(relSrcFile.toString());
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.copy(localFixtureDir.resolve(relSrcFile), targetFile);
            currProgress += progressPerFile;
            exec.setProgress(currProgress);
        }
    }

    private static FSPath toRelativeTargetFile(final FSFileSystem<?> targetFs, final Path srcFile) {

        FSPath targetPath = targetFs.getPath("fixtures");
        for (Path srcFileComp : srcFile) {
            targetPath = (FSPath)targetPath.resolve(srcFileComp.toString());
        }

        return targetPath;
    }

    private static List<Path> getFixturesToUpload(final Path localFixtureDir, final PathMatcher fixtureFilter)
        throws IOException {
        final List<Path> toReturn = new ArrayList<>();

        final FileVisitor<Path> filteringFileVisitor =
            createFilteringFileVisitor(localFixtureDir, fixtureFilter, toReturn);

        Files.walkFileTree(localFixtureDir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
            filteringFileVisitor);

        return toReturn;
    }

    private static FileVisitor<Path> createFilteringFileVisitor(final Path localFixtureDir,
        final PathMatcher fixtureFilter, final List<Path> paths) {

        final FileVisitor<Path> recursiveCopyVisitor = new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
                throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path srcFile, final BasicFileAttributes attrs) throws IOException {
                final Path relSrcFile = localFixtureDir.relativize(srcFile);
                if (fixtureFilter.matches(relSrcFile)) {
                    paths.add(relSrcFile);
                }
                return FileVisitResult.CONTINUE;
            }


            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        };
        return recursiveCopyVisitor;
    }

    private FileSystemPortObjectSpec createSpec(final String fsType, final FSLocationSpec fsLocationSpec) {
        return new FileSystemPortObjectSpec(fsType, m_fsId, fsLocationSpec);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        setWarningMessage("File system connection no longer available. Please re-execute the node.");
    }

    @Override
    protected void saveInternals(final File nodeInternDir,
        final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    protected void onDispose() {
        //close the file system also when the workflow is closed
        reset();
    }

    @Override
    protected void reset() {
        if (m_fsConnection != null) {
            m_fsConnection.closeInBackground();
            m_fsConnection = null;
        }

        m_fsId = null;
    }
}
