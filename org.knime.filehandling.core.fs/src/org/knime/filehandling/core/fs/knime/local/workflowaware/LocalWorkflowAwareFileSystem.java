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
 *   Feb 11, 2020 (Sascha Wolke, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knime.local.workflowaware;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.workflowalizer.MetadataConfig;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.Entity;
import org.knime.filehandling.core.connections.base.BaseFileSystem;

/**
 * A workflow-aware file system implementation that is backed by a folder in the local file system. The contents of this
 * root folder are accessible through this file system. This file system is workflow-aware, because it will detect which
 * folders in the local file system represent workflows/components, and which represent workflow groups.
 * Workflows/components are represented as atomic files.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class LocalWorkflowAwareFileSystem extends BaseFileSystem<LocalWorkflowAwarePath> {

    private static final long CACHE_TTL = 0; // = disabled

    /**
     * Separator used between names in paths.
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * Where this file system is rooted in the local (platform default) file system.
     */
    private final Path m_localRoot;

    /**
     * Default constructor.
     *
     * @param provider The file system provider.
     * @param localRoot Folder in the local file system, whose contents should be accessible through this new file
     *            system.
     * @param workingDir The working directory of this new file system.
     * @param fsLocationSpec The {@link FSLocationSpec} to use for this new file system.
     */
    @SuppressWarnings("resource")
    protected LocalWorkflowAwareFileSystem(final LocalWorkflowAwareFileSystemProvider<?> provider, final Path localRoot, //
        final String workingDir, //
        final FSLocationSpec fsLocationSpec) {
        super(provider, //
            CACHE_TTL, //
            workingDir, //
            fsLocationSpec);

        CheckUtils.checkArgument(localRoot.getFileSystem() == FileSystems.getDefault(),
            "The local root of a LocalWorkflowAwareFileSystem must be a path in the platform default file system");
        m_localRoot = localRoot.toAbsolutePath().normalize();
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    /**
     * @return the path to the one and only root directory in this file system.
     */
    public LocalWorkflowAwarePath getRoot() {
        return getPath(LocalWorkflowAwareFileSystem.PATH_SEPARATOR);
    }

    /**
     * @return the folder in the local (platform default) file system where this file system is rooted.
     */
    public Path getLocalRoot() {
        return m_localRoot;
    }

    /**
     * @return where this file system's working directory is at in the local (platform default) file system.
     */
    public Path getLocalWorkingDir() {
        return toLocalPath(getWorkingDirectory()).toAbsolutePath().normalize();
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getRoot());
    }

    /**
     * Utility method whether the given path is accessible, e.g. files outside the local root are not accessible.
     *
     * @param path A path (from the workflow-aware FS).
     * @return true when the given path can be accessed with the workflow-aware file system, false otherwise.
     * @throws IOException
     */
    public boolean isPathAccessible(final LocalWorkflowAwarePath path) throws IOException {
        // we must not access files outside of the mountpoint
        if (!isInMountPoint(path)) {
            return false;
        }

        // we must be able to view workflows (whether we display them as files or folders)
        if (isWorkflow(path)) {
            return true;
        }

        // we must never be able to see files inside workflows
        if (isPartOfWorkflow(path)) {
            return false;
        }

        return true;
    }

    /**
     * Validate if a given workflow-aware path is a workflow, meta node or component directory.
     *
     * @param path path to check
     * @return {@code true} if the path represents a workflow, meta node or component directory.
     * @throws IOException
     */
    public boolean isWorkflow(final LocalWorkflowAwarePath path) throws IOException {
        return isLocalWorkflowDirectory(toLocalPath(path));
    }

    /**
     * Maps a path from the workflow-aware file system to a path in the local file system.
     *
     * @param path A path from the workflow-aware file system
     * @return an absolute path in the local file system (default FS provider) that corresponds to this path.
     */
    public Path toLocalPath(final LocalWorkflowAwarePath path) {
        final var absolutePath = (LocalWorkflowAwarePath)path.toAbsolutePath().normalize();
        return Paths.get(m_localRoot.toString(), absolutePath.stringStream().toArray(String[]::new));
    }

    /**
     * Returns which kind of entity (workflow, component, workflow group, meta node or data) the provided path points
     * to.
     *
     * @param path for which to get the type of entity
     * @return the type of entity at the provided path or {@link Optional#empty()} if the path doesn't point to anything
     * @throws IOException if I/O problems occur
     */
    protected Optional<Entity> getEntity(final LocalWorkflowAwarePath path) throws IOException {
        // throws NoSuchFileException if it points into a workflow
        final Path localPath = toLocalPathWithAccessibilityCheck(path);
        if (!Files.exists(localPath)) {
            return Optional.empty();
        } else if (!Files.isDirectory(localPath)) {
            return Optional.of(Entity.DATA);
        } else {
            // directories can be either workflows, meta nodes, components or workflow groups
            if (hasWorkflowFile(localPath)) {
                if (hasTemplateFile(localPath)) {
                    final Entity entity = getTemplateEntity(localPath);
                    return Optional.of(entity);
                } else {
                    return Optional.of(Entity.WORKFLOW);
                }
            } else {
                return Optional.of(Entity.WORKFLOW_GROUP);
            }
        }
    }

    private static boolean hasTemplateFile(final Path localPath) {
        return Files.exists(localPath.resolve(WorkflowPersistor.TEMPLATE_FILE));
    }

    private static boolean hasWorkflowFile(final Path localPath) {
        return Files.exists(localPath.resolve(WorkflowPersistor.WORKFLOW_FILE));
    }

    private static Entity getTemplateEntity(final Path localPath) throws IOException {
        if (isComponent(localPath.resolve(WorkflowPersistor.TEMPLATE_FILE))) {
            return Entity.COMPONENT;
        } else {
            return Entity.METANODE;
        }
    }

    private static boolean isComponent(final Path pathToTemplateFile) throws IOException {
        assert pathToTemplateFile.endsWith(WorkflowPersistor.TEMPLATE_FILE);
        final MetadataConfig c = new MetadataConfig("ignored");
        try (final InputStream s = Files.newInputStream(pathToTemplateFile)) {
            c.load(s);
            return c.getConfigBase("workflow_template_information").getString("templateType")
                .equals(MetaNodeTemplateInformation.TemplateType.SubNode.toString());
        } catch (InvalidSettingsException ex) {
            throw new IOException("Invalid template.knime file.", ex);
        }
    }

    /**
     * Validates if the given local file system path is a workflow, component or meta node directory.
     *
     * @param localPath a path in the local file system to validate
     * @return {@code true} when the path contains a workflow
     */
    protected boolean isLocalWorkflowDirectory(final Path localPath) {
        if (!Files.exists(localPath)) {
            return false;
        }

        return Files.exists(localPath.resolve(WorkflowPersistor.TEMPLATE_FILE))
            || Files.exists(localPath.resolve(WorkflowPersistor.WORKFLOW_FILE));
    }

    /**
     * Validate recursive if given path (from the workflow-aware FS) or a parent is part of a workflow.
     *
     * @param path workflow-aware file system path to check
     * @return {@code true} if given path or a parent path is part of a workflow
     * @throws IOException
     */
    public boolean isPartOfWorkflow(final LocalWorkflowAwarePath path) throws IOException {
        LocalWorkflowAwarePath current = (LocalWorkflowAwarePath)path.toAbsolutePath().normalize();

        while (isInMountPoint(current)) {
            if (isWorkflow(current)) {
                return true;
            } else {
                current = (LocalWorkflowAwarePath)current.getParent();
            }
        }

        return false;
    }

    /**
     * Test if given path is a child of the mount point directory.
     *
     * @param path A workflow-aware file system path to test
     * @return {@code} if given path is a child of the mount point directory
     */
    private static boolean isInMountPoint(final LocalWorkflowAwarePath path) {
        // Note: Java's Path.normalize converts outside of the root "/../bla" into "/bla".
        // The Apache commons filename utils return null if the path is outside of the root directory.
        return path != null && FilenameUtils.normalize(path.toAbsolutePath().toString(), true) != null;
    }

    /**
     * Validates that a given workflow-aware file system path is accessible and maps it to a absolute and normalized path
     * in the real file system.
     *
     * @param path a workflow-aware path inside workflow-aware file system
     * @return an absolute path in the local file system (default FS provider) that corresponds to this path.
     * @throws NoSuchFileException if given path is not accessible
     * @throws IOException on other failures
     */
    public Path toLocalPathWithAccessibilityCheck(final LocalWorkflowAwarePath path) throws IOException {
        if (!isPathAccessible(path)) {
            throw new NoSuchFileException(path.toString());
        } else {
            return toLocalPath(path);
        }
    }

    /**
     * Check if the given workflow-aware path exists and is accessible.
     *
     * @param path workflow-aware path to check
     * @return {@code true} if path exists and is accessible
     * @throws IOException
     */
    protected boolean existsWithAccessibilityCheck(final LocalWorkflowAwarePath path) throws IOException {
        final Path localPath = toLocalPath(path);
        return isPathAccessible(path) && Files.exists(localPath);
    }

    @Override
    protected void prepareClose() {
        // Nothing to do
    }
}