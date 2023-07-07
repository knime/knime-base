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
package org.knime.filehandling.core.fs.knime.local.relativeto.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.core.node.workflow.NodeContext;
import org.knime.filehandling.core.fs.knime.local.workflowaware.LocalWorkflowAwarePath;
import org.knime.filehandling.core.tests.common.workflow.WorkflowTestUtil;

/**
 * Test local relative to file system specific things.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class LocalRelativeToFileSystemTest extends LocalRelativeToFileSystemTestBase {

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedServerSideExecution() throws IOException {
        // replace the current workflow manager with a server side workflow manager
        NodeContext.removeLastContext();
        final var mountpointRoot = m_tempFolder.newFolder("other-mountpoint-root").toPath();
        final var workflowName = "current-workflow";
        WorkflowTestUtil.createWorkflowDir(mountpointRoot, workflowName);
        m_workflowManager = WorkflowTestUtil.getServerSideWorkflowManager(mountpointRoot, workflowName);
        NodeContext.pushContext(m_workflowManager);

        // initialization should fail
        getMountpointRelativeFS();
    }

    @Test
    public void getRootDirectories() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        assertEquals(Collections.singletonList(fs.getPath("/")), fs.getRootDirectories());
    }

    @Test
    public void workingDirectoryWorkflowRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        assertEquals(fs.getPath("/current-workflow"), fs.getWorkingDirectory());
    }

    @Test
    public void workingDirectoryMountpointRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        assertEquals(fs.getPath("/"), fs.getWorkingDirectory());
    }

    private static void assertPathInaccessible(final LocalRelativeToFileSystem fs, final String pathStr) throws IOException {
        final var path = fs.getPath(pathStr);
        assertFalse(fs.isPathAccessible(path));
        fs.toLocalPathWithAccessibilityCheck(path); // throws exception
    }

    @Test(expected = NoSuchFileException.class)
    public void outsideMountpointWorkflowRelative() throws IOException {
        assertPathInaccessible(getWorkflowRelativeFS(), "../../../somewhere-outside");
    }

    @Test(expected = NoSuchFileException.class)
    public void outsideMountpointMountpointRelative() throws IOException {
        assertPathInaccessible(getMountpointRelativeFS(), "/../../../somewhere-outside");
    }

    @Test(expected = NoSuchFileException.class)
    public void insideCurrentWorkflowWithWorkflowRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath path = fs.getPath("../current-workflow/some-file.txt");
        assertFalse(fs.isPathAccessible(path));
        fs.toLocalPathWithAccessibilityCheck(path); // does throw an exception
    }

    @Test(expected = NoSuchFileException.class)
    public void insideCurrentWorkflowWithMountpointRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        final LocalWorkflowAwarePath path = fs.getPath("/current-workflow/some-file.txt");
        assertFalse(fs.isPathAccessible(path));
        fs.toLocalPathWithAccessibilityCheck(path); // throws exception
    }

    @Test(expected = NoSuchFileException.class)
    public void insideOtherWorkflowWithWorkflowRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath path = fs.getPath("../other-workflow/some-file.txt");
        assertFalse(fs.isPathAccessible(path));
        fs.toLocalPathWithAccessibilityCheck(path); // throws exception
    }

    @Test(expected = NoSuchFileException.class)
    public void insideOtherWorkflowWithMountpointRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        final LocalWorkflowAwarePath path = fs.getPath("/other-workflow/some-file.txt");
        assertFalse(fs.isPathAccessible(path));
        fs.toLocalPathWithAccessibilityCheck(path); // throws exception
    }

    private void assertPathIsInaccessible(final LocalWorkflowAwarePath path) throws IOException {
        @SuppressWarnings("resource")
        final var fs = path.getFileSystem();

        assertFalse(fs.isPathAccessible(path));
        try {
            fs.toLocalPathWithAccessibilityCheck(path); // throws exception
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // does not exist
        assertFalse(Files.exists(path));

        // cannot read attributes
        try {
            Files.readAttributes(path, BasicFileAttributes.class);
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) {// NOSONAR
        }

        // cannot read
        try (var stream = Files.newInputStream(path)) {
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) { // NOSONAR
        }

        // cannot write with output stream
        try (var stream = Files.newOutputStream(path)) {
            fail("Path should not exist or be accessible");
        } catch (FileSystemException e) {
            assertEquals(path.toAbsolutePath().normalize().toString(), e.getFile());
        }

        // cannot write with channel
        try (var stream = Files.newByteChannel(path, StandardOpenOption.CREATE)) {
            fail("Path should not exist or be accessible");
        } catch (FileSystemException e) {// NOSONAR
        }

        // cannot copy from
        try {
            Files.copy(path, fs.getPath("/wontbecreated"));
            fail("Path should not be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // cannot copy to
        try {
            var testFile = fs.getPath("/testfile");
            Files.writeString(testFile, "test");
            Files.copy(testFile, path, StandardCopyOption.REPLACE_EXISTING);
            fail("Path should not be accessible");
        } catch (FileSystemException e) {// NOSONAR
        }

        // cannot move from
        try {
            Files.move(path, fs.getPath("/wontbecreated"));
            fail("Path should not be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // cannot move to
        try {
            var testFile = fs.getPath("/testfile");
            Files.writeString(testFile, "test");
            Files.move(testFile, path, StandardCopyOption.REPLACE_EXISTING);
            fail("Path should not be accessible");
        } catch (FileSystemException e) {// NOSONAR
        }

        // does not show up in directory listing (1)
        var relParent = Optional.ofNullable(path.getParent()).orElse(fs.getPath(""));
        try (var stream = Files.list(relParent)) {
            var list = stream.collect(Collectors.toList());
            assertFalse(list.contains(path));
        } catch (NoSuchFileException e) {
            assertTrue(Files.notExists(relParent));
        }

        // does not show up in directory listing (2)
        var absParent = path.toAbsolutePath().normalize().getParent();
        try (var stream = Files.list(absParent)) {
            var list = stream.collect(Collectors.toList());
            assertFalse(list.contains(path.toAbsolutePath().normalize()));
        } catch (NoSuchFileException e) {
            assertTrue(Files.notExists(absParent));
        }
    }

    private void assertMetaFilePathIsInaccessible(final LocalWorkflowAwarePath path) throws IOException {
        @SuppressWarnings("resource")
        final var fs = path.getFileSystem();

        assertFalse(fs.isPathAccessible(path));
        try {
            fs.toLocalPathWithAccessibilityCheck(path); // throws exception
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // does not exist
        assertFalse(Files.exists(path));

        // cannot read attributes
        try {
            Files.readAttributes(path, BasicFileAttributes.class);
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) {// NOSONAR
        }

        // cannot read
        try (var stream = Files.newInputStream(path)) {
            fail("Path should not exist or be accessible");
        } catch (NoSuchFileException e) { // NOSONAR
        }

        // can write with output stream
        try (var stream = Files.newOutputStream(path)) {
            stream.write("Some text".getBytes(StandardCharsets.UTF_8));
        }

        // can write with channel
        try (var channel = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            ByteBuffer buff = ByteBuffer.wrap("Hello world".getBytes(StandardCharsets.UTF_8));
            channel.write(buff);
        }

        // cannot copy from
        try {
            Files.copy(path, fs.getPath("/wontbecreated"));
            fail("Path should not be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // cannot copy to
        try {
            var testFile = fs.getPath("/testfile");
            Files.writeString(testFile, "test");
            Files.copy(testFile, path, StandardCopyOption.REPLACE_EXISTING);
            fail("Path should not be accessible");
        } catch (FileSystemException e) {// NOSONAR
        }

        // cannot move from
        try {
            Files.move(path, fs.getPath("/wontbecreated"));
            fail("Path should not be accessible");
        } catch (NoSuchFileException e) {
            assertEquals(path.toString(), e.getFile());
        }

        // cannot move to
        try {
            var testFile = fs.getPath("/testfile");
            Files.writeString(testFile, "test");
            Files.move(testFile, path, StandardCopyOption.REPLACE_EXISTING);
            fail("Path should not be accessible");
        } catch (FileSystemException e) {// NOSONAR
        }

        // does not show up in directory listing (1)
        var relParent = Optional.ofNullable(path.getParent()).orElse(fs.getPath(""));
        try (var stream = Files.list(relParent)) {
            var list = stream.collect(Collectors.toList());
            assertFalse(list.contains(path));
        } catch (NoSuchFileException e) {
            assertTrue(Files.notExists(relParent));
        }

        // does not show up in directory listing (2)
        var absParent = path.toAbsolutePath().normalize().getParent();
        try (var stream = Files.list(absParent)) {
            var list = stream.collect(Collectors.toList());
            assertFalse(list.contains(path.toAbsolutePath().normalize()));
        } catch (NoSuchFileException e) {
            assertTrue(Files.notExists(absParent));
        }
    }

    @Test
    public void testMetadataFolderIsInaccessible() throws IOException {
        @SuppressWarnings("resource")
        final var fs = getMountpointRelativeFS();
        Files.createDirectory(m_mountpointRoot.resolve(".metadata"));
        Files.writeString(m_mountpointRoot.resolve(".metadata").resolve("version.ini"), "test");

        assertPathIsInaccessible(fs.getPath(".metadata"));
        assertPathIsInaccessible(fs.getPath(".metadata/version.ini"));
    }

    @Test
    public void workflowsetMetaFileWithWorkflowRelative() throws IOException {
        @SuppressWarnings("resource")
        final var fs = getWorkflowRelativeFS();
        Files.writeString(m_mountpointRoot.resolve("workflowset.meta"), "");
        Files.createDirectory(m_mountpointRoot.resolve("testfolder"));
        Files.writeString(m_mountpointRoot.resolve("testfolder").resolve("workflowset.meta"), "");

        assertMetaFilePathIsInaccessible(fs.getPath("../workflowset.meta"));
        assertMetaFilePathIsInaccessible(fs.getPath("../testfolder/workflowset.meta"));
    }

    @Test
    public void insideMetaFileWitMountpointRelative() throws IOException {
        @SuppressWarnings("resource")
        final var fs = getMountpointRelativeFS();
        Files.writeString(m_mountpointRoot.resolve("workflowset.meta"), "");
        Files.createDirectory(m_mountpointRoot.resolve("testfolder"));
        Files.writeString(m_mountpointRoot.resolve("testfolder").resolve("workflowset.meta"), "");

        assertMetaFilePathIsInaccessible(fs.getPath("workflowset.meta"));
        assertMetaFilePathIsInaccessible(fs.getPath("testfolder/workflowset.meta"));
    }

    @Test
    public void isWorkflow() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        assertFalse(fs.isWorkflow(fs.getPath("/")));
        assertTrue(fs.isWorkflow(fs.getPath("/current-workflow")));
        assertTrue(fs.isWorkflow(fs.getPath("/other-workflow")));
    }

    @Test
    public void isWorkflowRelative() throws IOException {
        assertTrue(getWorkflowRelativeFS().isWorkflowRelativeFileSystem());
        assertFalse(getMountpointRelativeFS().isWorkflowRelativeFileSystem());
    }

    @Test
    public void equalsOnDifferentFS() throws IOException {
        final String filename = "/some-dir/some-file.txt";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath mountpointPath = mountpointFS.getPath(filename);
        final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath workflowPath = workflowFS.getPath(filename);
        assertNotEquals(workflowPath, mountpointPath);
        assertEquals(mountpointPath, mountpointFS.getPath(filename));
        assertEquals(workflowPath, workflowFS.getPath(filename));

        final Path localPath = Paths.get(filename);
        assertNotEquals(mountpointPath, localPath);
        assertNotEquals(workflowPath, localPath);
        assertNotEquals(localPath, mountpointPath);
        assertNotEquals(localPath, workflowPath);
    }

    @Test
    public void testIsSame() throws IOException {
        final String filename = "/some-dir/some-file.txt";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath mountpointPath = mountpointFS.getPath(filename);
        final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath workflowPath = workflowFS.getPath(filename);

        final Path localPath = Paths.get(filename);
        assertFalse(Files.isSameFile(localPath, mountpointPath));
        assertFalse(Files.isSameFile(localPath, workflowPath));
        assertFalse(Files.isSameFile(mountpointPath, localPath));
        assertFalse(Files.isSameFile(workflowPath, localPath));
    }

    /**
     * Ensure that {@link LocalWorkflowAwarePath#toAbsoluteLocalPath()} uses separator from local filesystem.
     */
    @Test
    public void testToAbsoluteLocalPath() throws IOException {
        final String filename = "/some-dir/some-file.txt";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath mountpointPath = mountpointFS.getPath(filename);
        final Path convertedLocalPath = mountpointFS.toLocalPathWithAccessibilityCheck(mountpointPath);
        final Path realLocalPath = m_mountpointRoot.resolve("some-dir").resolve("some-file.txt");
        assertEquals(realLocalPath, convertedLocalPath);
    }

    @Test
    public void testExistsMountpointRelative() throws IOException {
        final String filename = "some-file.txt";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath mountpointPath = mountpointFS.getPath(filename);
        assertFalse(Files.exists(mountpointPath));
        Files.createFile(mountpointPath);
        assertTrue(Files.exists(mountpointPath));
    }

    @Test(expected = FileSystemException.class)
    public void testCreateFileOnRelativePath() throws IOException {
        final String filename = "some-file.txt";
        final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath relativePath = workflowFS.getPath(filename);

        assertFalse(Files.exists(relativePath));
        Files.createFile(relativePath); // throws exception
    }

    @Test(expected = FileSystemException.class)
    public void testCreateDirOnRelativePath() throws IOException {
        final String dirname = "somedir";
        final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath relativePath = workflowFS.getPath(dirname);

        assertFalse(Files.exists(relativePath));
        Files.createDirectory(relativePath); // throws exception
    }

    @Test(expected = NotDirectoryException.class)
    public void testListWorkflowDirOnRelativePath() throws IOException {
        final LocalRelativeToFileSystem workflowFS = getWorkflowRelativeFS();
        final LocalWorkflowAwarePath relativePath = workflowFS.getPath(".");
        try (final Stream<Path> stream = Files.list(relativePath)) {
        }
    }

    @Test(expected = FileSystemException.class)
    public void testCreateFileOnMountpointPath() throws IOException {
        final String filename = "current-workflow/some-file.txt";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath relativePath = mountpointFS.getPath(filename);

        assertFalse(Files.exists(relativePath));
        Files.createFile(relativePath); // throws exception
    }

    @Test(expected = FileSystemException.class)
    public void testCreateDirOnMountpointPath() throws IOException {
        final String dirname = "current-workflow/somedir";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath relativePath = mountpointFS.getPath(dirname);

        assertFalse(Files.exists(relativePath));
        Files.createDirectory(relativePath); // throws exception
    }

    @Test(expected = NotDirectoryException.class)
    public void testListWorkflowDirOnMountpointPath() throws IOException {
        final String dirname = "current-workflow";
        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath relativePath = mountpointFS.getPath(dirname);
        try (final Stream<Path> stream = Files.list(relativePath)) {
        }
    }

    @Test
    public void testListSkipMetadataFolderOnMountpointPath() throws IOException {
        final String metadataFolder = ".metadata";
        final String metadataFile = "workflowset.meta";
        WorkflowTestUtil.createWorkflowDir(m_mountpointRoot, metadataFolder);
        WorkflowTestUtil.createWorkflowDir(m_mountpointRoot, metadataFile);

        final LocalRelativeToFileSystem mountpointFS = getMountpointRelativeFS();
        final LocalWorkflowAwarePath relativePath = mountpointFS.getPath(".");
        try (final Stream<Path> stream = Files.list(relativePath)) {
            stream.forEach(p -> {
                assertNotEquals(metadataFolder, p.getFileName().toString());
                assertNotEquals(metadataFile, p.getFileName().toString());
            });
        }
        try (var stream = Files.list(relativePath)) {
            assertEquals(2, stream.count());
        }
    }

    @Test
    public void relativizeWorkflowRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        assertEquals(fs.getPath("../some-directory/some-workflow"),
            fs.getWorkingDirectory().relativize(fs.getPath("/some-directory/some-workflow")));
    }

    @Test
    public void absolutWorkflowRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();
        assertEquals(fs.getPath("/some-directory/some-workflow"),
            fs.getPath("../some-directory/some-workflow").toAbsolutePath().normalize());
    }

    @Test
    public void absolutMountpointRelative() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        assertEquals(fs.getPath("/some-directory/some-workflow"),
            fs.getPath("/some-directory/some-workflow").toAbsolutePath().normalize());
    }

    @Test(expected = IOException.class)
    public void newInputStreamOnWorkflowFails() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        final Path path = fs.getPath("/other-workflow");
        try {
            Files.newInputStream(path);
        } catch (final IOException e) {
            assertEquals(path.toString() + ": Reading a Workflow is not possible. See the Integrated Deployment extension for handling workflows.", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void newOutputStreamOnWorkflowFails() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        final Path path = fs.getPath("/other-workflow");
        try {
            Files.newOutputStream(path);
        } catch (final IOException e) {
            assertEquals(path.toString()  + ": Overwriting a Workflow is not possible. See the Integrated Deployment extension for handling workflows.", e.getMessage());
            throw e;
        }
    }

    @Test(expected = IOException.class)
    public void newByteChannelOnWorkflowFails() throws IOException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();
        final Path path = fs.getPath("/other-workflow");
        try {
            Files.newByteChannel(path);
        } catch (final IOException e) {
            assertEquals(path.toString()  + " points to/into a workflow. Workflows cannot be opened for reading/writing", e.getMessage());
            throw e;
        }
    }
}
