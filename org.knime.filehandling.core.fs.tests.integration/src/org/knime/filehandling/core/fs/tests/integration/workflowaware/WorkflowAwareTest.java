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
 *   May 17, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.fs.tests.integration.workflowaware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.workflowaware.Entity;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAware;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling.Operation;
import org.knime.filehandling.core.connections.workflowaware.WorkflowAwareErrorHandling.WorkflowAwareFSException;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.WorkflowAwareFSTestInitializer;
import org.knime.filehandling.core.util.CheckedExceptionBiConsumer;
import org.knime.filehandling.core.util.CheckedExceptionConsumer;
import org.knime.filehandling.core.util.CheckedExceptionSupplier;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Contains test for {@link WorkflowAware} file systems.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class WorkflowAwareTest extends AbstractParameterizedFSTest {

    /**
     * Constructor.
     *
     * @param fsType the type of file system as string
     * @param testInitializer a supplier for the {@link FSTestInitializer}
     * @throws IOException
     */
    public WorkflowAwareTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer)
        throws IOException {
        super(fsType, testInitializer);
    }

    private Entity getExpectedEntityForComponent() {
        return canDistinguishBetweenComponentsAndMetaNodes() ? Entity.COMPONENT : Entity.WORKFLOW_TEMPLATE;
    }

    private Entity getExpectedEntityForMetanode() {
        return canDistinguishBetweenComponentsAndMetaNodes() ? Entity.METANODE : Entity.WORKFLOW_TEMPLATE;
    }

    private boolean canDistinguishBetweenComponentsAndMetaNodes() {
        return getWfAwareInitializer().canDistinguishBetweenComponentsAndMetanodes();
    }

    private WorkflowAwareFSTestInitializer getWfAwareInitializer() {
        return (WorkflowAwareFSTestInitializer)m_testInitializer;
    }

    private boolean isWorkflowAware() {
        return m_testInitializer instanceof WorkflowAwareFSTestInitializer;
    }

    private static void assertEntityType(final Entity expected, final FSPath path) throws IOException {
        assertEquals(expected, path.getFileSystem().getWorkflowAware().orElseThrow().getEntityOf(path));
    }

    @Test
    public void test_open_input_stream_on_workflow() throws IOException {
        test_failing_operation_on_knime_object(this::createWorkflow, FSFiles::newInputStream, Entity.WORKFLOW,
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_input_stream_on_component() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_failing_operation_on_knime_object(this::createComponent, FSFiles::newInputStream, getExpectedEntityForComponent(),
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_input_stream_on_metanode() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_failing_operation_on_knime_object(this::createMetanode, FSFiles::newInputStream, getExpectedEntityForMetanode(),
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_workflow() throws IOException {
        test_failing_operation_on_knime_object(this::createWorkflow, FSFiles::newOutputStream, Entity.WORKFLOW,
            Operation.NEW_OUTPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_component() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_failing_operation_on_knime_object(this::createComponent, FSFiles::newOutputStream, getExpectedEntityForComponent(),
            Operation.NEW_OUTPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_metanode() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_failing_operation_on_knime_object(this::createMetanode, FSFiles::newOutputStream, getExpectedEntityForMetanode(),
            Operation.NEW_OUTPUT_STREAM);
    }

    @Test
    public void test_delete_workflow() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createWorkflow, Files::delete,
            p -> assertFalse(Files.exists(p)));
    }

    @Test
    public void test_delete_component() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createComponent, Files::delete,
            p -> assertFalse(Files.exists(p)));
    }

    @Test
    public void test_delete_metanode() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_successful_operation_on_knime_object(this::createMetanode, Files::delete,
            p -> assertFalse(Files.exists(p)));
    }

    @Test
    public void test_copy_workflow() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createWorkflow, //
            p -> Files.copy(p, p.getFileSystem().getPath(p.toString() + "_copy")),
            p -> {
                assertTrue(Files.exists(p));
                assertEntityType(Entity.WORKFLOW, p);
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_copy");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.WORKFLOW, copyPath);
            });
    }

    @Test
    public void test_copy_component() throws IOException {
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_MOUNTPOINT);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_WORKFLOW);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST);

        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createComponent, //
            p -> Files.copy(p, p.getFileSystem().getPath(p.toString() + "_copy")),
            p -> {
                assertTrue(Files.exists(p));
                assertEntityType(Entity.COMPONENT, p);
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_copy");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.COMPONENT, copyPath);
            });
    }

    @Test
    public void test_copy_component_server() throws IOException {
        // Server REST API does not distinguish metanodes and components
        ignoreAllExcept(KNIME_REST, KNIME_REST_RELATIVE_MOUNTPOINT, KNIME_REST_RELATIVE_WORKFLOW);

        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createComponent, //
            p -> Files.copy(p, p.getFileSystem().getPath(p.toString() + "_copy")),
            p -> {
                assertTrue(Files.exists(p));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, p);
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_copy");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, copyPath);
            });
    }


    @Test
    public void test_copy_metanode() throws IOException {
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_MOUNTPOINT);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_WORKFLOW);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST);

        if (!isWorkflowAware()) {
            return;
        }
        test_successful_operation_on_knime_object(this::createMetanode, //
            p -> Files.copy(p, p.getFileSystem().getPath(p.toString() + "_copy")),
            p -> {
                assertTrue(Files.exists(p));
                assertEntityType(Entity.METANODE, p);
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_copy");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.METANODE, copyPath);
            });
    }

    @Test
    public void test_copy_metanode_server() throws IOException {
        // Server REST API does not distinguish metanodes and components
        ignoreAllExcept(KNIME_REST, KNIME_REST_RELATIVE_MOUNTPOINT, KNIME_REST_RELATIVE_WORKFLOW);

        if (!isWorkflowAware()) {
            return;
        }
        test_successful_operation_on_knime_object(this::createMetanode, //
            p -> Files.copy(p, p.getFileSystem().getPath(p.toString() + "_copy")),
            p -> {
                assertTrue(Files.exists(p));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, p);
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_copy");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, copyPath);
            });
    }


    @Test
    public void test_copy_workflow_to_existing_target_fails() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        final var copySource = createWorkflow();
        final var workflowPath2 = createWorkflow();
        final var existingFile = m_testInitializer.createFile("testfile");

        try {
            Files.copy(copySource, workflowPath2);
            fail("The operation was expected to fail.");
        } catch (FileAlreadyExistsException e) {
            assertTrue(e.getFile().contains(workflowPath2.toString()));
        }

        try {
            Files.copy(copySource, existingFile);
            fail("The operation was expected to fail.");
        } catch (FileAlreadyExistsException e) {
            assertTrue(e.getFile().contains(existingFile.toString()));
        }
    }

    @Test
    public void test_copy_workflow_to_existing_target_with_replace_succeeds() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        final var copySource = createWorkflow();
        final var workflowPath2 = createWorkflow();
        final var existingFile = m_testInitializer.createFile("testfile");

        assertEntityType(Entity.WORKFLOW, workflowPath2);
        Files.copy(copySource, workflowPath2, StandardCopyOption.REPLACE_EXISTING);
        assertEntityType(Entity.WORKFLOW, workflowPath2);

        assertEntityType(Entity.DATA, existingFile);
        Files.copy(copySource, existingFile, StandardCopyOption.REPLACE_EXISTING);
        assertEntityType(Entity.WORKFLOW, existingFile);
    }

    @Test
    public void test_move_workflow() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createWorkflow, //
            p -> Files.move(p, p.getFileSystem().getPath(p.toString() + "_moved")), p -> {
                assertFalse(Files.exists(p));
                final var movedPath = p.getFileSystem().getPath(p.toString() + "_moved");
                assertTrue(Files.exists(movedPath));
                assertEntityType(Entity.WORKFLOW, movedPath);
            });
    }

    @Test
    public void test_move_component() throws IOException {
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_MOUNTPOINT);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_WORKFLOW);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST);

        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createComponent, //
            p -> Files.move(p, p.getFileSystem().getPath(p.toString() + "_moved")), p -> {
                assertFalse(Files.exists(p));
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_moved");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.COMPONENT, copyPath);
            });
    }

    public void test_move_component_server() throws IOException {
        // Server REST API does not distinguish metanodes and components
        ignoreAllExcept(KNIME_REST, KNIME_REST_RELATIVE_MOUNTPOINT, KNIME_REST_RELATIVE_WORKFLOW);

        if (!isWorkflowAware()) {
            return;
        }

        test_successful_operation_on_knime_object(this::createComponent, //
            p -> Files.move(p, p.getFileSystem().getPath(p.toString() + "_moved")), p -> {
                assertFalse(Files.exists(p));
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_moved");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, copyPath);
            });
    }


    @Test
    public void test_move_metanode() throws IOException {
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_MOUNTPOINT);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST_RELATIVE_WORKFLOW);
        ignoreWithReason("Server REST API does not distinguish metanodes and components", KNIME_REST);

        if (!isWorkflowAware()) {
            return;
        }
        test_successful_operation_on_knime_object(this::createMetanode, //
            p -> Files.move(p, p.getFileSystem().getPath(p.toString() + "_moved")), p -> {
                assertFalse(Files.exists(p));
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_moved");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.METANODE, copyPath);
            });
    }

    @Test
    public void test_move_metanode_server() throws IOException {
        // Server REST API does not distinguish metanodes and components
        ignoreAllExcept(KNIME_REST, KNIME_REST_RELATIVE_MOUNTPOINT, KNIME_REST_RELATIVE_WORKFLOW);

        if (!isWorkflowAware()) {
            return;
        }
        test_successful_operation_on_knime_object(this::createMetanode, //
            p -> Files.move(p, p.getFileSystem().getPath(p.toString() + "_moved")), p -> {
                assertFalse(Files.exists(p));
                final var copyPath = p.getFileSystem().getPath(p.toString() + "_moved");
                assertTrue(Files.exists(copyPath));
                assertEntityType(Entity.WORKFLOW_TEMPLATE, copyPath);
            });
    }


    @Test
    public void test_move_workflow_to_existing_target_fails() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        final var moveSource = createWorkflow();
        final var workflowPath2 = createWorkflow();
        final var existingFile = m_testInitializer.createFile("testfile");

        try {
            Files.move(moveSource, workflowPath2);
            fail("The operation was expected to fail.");
        } catch (FileAlreadyExistsException e) {
            assertTrue(e.getFile().contains(workflowPath2.toString()));
            assertTrue(Files.exists(moveSource));
            assertTrue(Files.exists(workflowPath2));
        }

        try {
            Files.move(moveSource, existingFile);
            fail("The operation was expected to fail.");
        } catch (FileAlreadyExistsException e) {
            assertTrue(e.getFile().contains(existingFile.toString()));
            assertTrue(Files.exists(moveSource));
            assertTrue(Files.exists(existingFile));
        }
    }

    @Test
    public void test_move_workflow_to_existing_target_with_replace_succeeds() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }

        final var moveSource1 = createWorkflow();
        final var workflowPath2 = createWorkflow();

        assertEntityType(Entity.WORKFLOW, workflowPath2);
        Files.move(moveSource1, workflowPath2, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Files.exists(moveSource1));
        assertEntityType(Entity.WORKFLOW, workflowPath2);

        final var moveSource2 = createWorkflow();
        final var existingFile = m_testInitializer.createFile("testfile");
        assertEntityType(Entity.DATA, existingFile);
        Files.move(moveSource2, existingFile, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Files.exists(moveSource2));
        assertEntityType(Entity.WORKFLOW, existingFile);
    }

    private void test_failing_operation_on_knime_object(final CheckedExceptionSupplier<FSPath, IOException> objectCreator,
        final CheckedExceptionConsumer<FSPath, IOException> operation, final Entity expectedEntity,
        final Operation expectedOperation) throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        FSPath path = objectCreator.get();
        try {
            operation.accept(path);
            fail("The operation was expected to fail.");
        } catch (WorkflowAwareFSException wfEx) {
            assertEquals(expectedEntity, wfEx.getEntity());
            assertEquals(expectedOperation, wfEx.getOperation());
        }
    }

    private void test_successful_operation_on_knime_object(final CheckedExceptionSupplier<FSPath, IOException> objectCreator, //
        final CheckedExceptionConsumer<FSPath, IOException> operation, //
        final CheckedExceptionConsumer<FSPath, IOException> successChecker) throws IOException {

        if (!isWorkflowAware()) {
            return;
        }

        FSPath path = objectCreator.get();
        try {
            operation.accept(path);
            successChecker.accept(path);
        } catch (WorkflowAwareFSException wfEx) {
            fail("The operation was expected to succeed.");
        }
    }

    private FSPath createWorkflow() throws IOException {
        return uploadWorkflowResource("Workflow", this::loadWfConfigIntoTmpWf);
    }

    private FSPath createComponent() throws IOException {
        return uploadWorkflowResource("Component", this::loadTemplateConfigIntoTmpWf);
    }

    private FSPath createMetanode() throws IOException {
        return uploadWorkflowResource("Metanode", this::loadTemplateConfigIntoTmpWf);
    }

    private FSPath uploadWorkflowResource(final String resourceName,
        final CheckedExceptionBiConsumer<String, Path, IOException> resourceLoader) throws IOException {
        Path tmpWf = Files.createTempDirectory(resourceName);
        resourceLoader.accept(resourceName, tmpWf);
        return getWfAwareInitializer().deployWorkflow(tmpWf, resourceName + UUID.randomUUID().toString().substring(20));
    }

    private void loadWfConfigIntoTmpWf(final String resourceName, final Path tmpWf) throws IOException {
        copyResourceIntoTmpWf(tmpWf, resourceName, "workflow.knime");
    }

    private void loadTemplateConfigIntoTmpWf(final String resourceName, final Path tmpWf) throws IOException {
        loadWfConfigIntoTmpWf(resourceName, tmpWf);
        copyResourceIntoTmpWf(tmpWf, resourceName, "template.knime");
    }

    private void copyResourceIntoTmpWf(final Path tmpWf, final String resourceParent, final String resourceChild)
            throws IOException {
            try (InputStream wfStream = getClass().getResourceAsStream("/" + resourceParent + "/" + resourceChild)) {
                Files.copy(wfStream, tmpWf.resolve(resourceChild));
            }
        }

}
