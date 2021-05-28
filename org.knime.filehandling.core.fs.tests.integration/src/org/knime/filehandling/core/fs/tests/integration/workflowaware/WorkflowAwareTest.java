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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.WorkflowAware;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.Entity;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.Operation;
import org.knime.filehandling.core.connections.WorkflowAwareErrorHandling.WorkflowAwareFSException;
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

    @Test
    public void test_open_input_stream_on_workflow() throws IOException {
        test_operation_on_knime_object(this::createWorkflow, FSFiles::newInputStream, Entity.WORKFLOW,
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_input_stream_on_component() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_operation_on_knime_object(this::createComponent, FSFiles::newInputStream, getExpectedEntityForComponent(),
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_input_stream_on_metanode() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_operation_on_knime_object(this::createMetanode, FSFiles::newInputStream, getExpectedEntityForMetanode(),
            Operation.NEW_INPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_workflow() throws IOException {
        test_operation_on_knime_object(this::createWorkflow, FSFiles::newOutputStream, Entity.WORKFLOW,
            Operation.NEW_OUTPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_component() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_operation_on_knime_object(this::createComponent, FSFiles::newOutputStream, getExpectedEntityForComponent(),
            Operation.NEW_OUTPUT_STREAM);
    }

    @Test
    public void test_open_output_stream_on_metanode() throws IOException {
        if (!isWorkflowAware()) {
            return;
        }
        test_operation_on_knime_object(this::createMetanode, FSFiles::newOutputStream, getExpectedEntityForMetanode(),
            Operation.NEW_OUTPUT_STREAM);
    }

    private void test_operation_on_knime_object(final CheckedExceptionSupplier<FSPath, IOException> objectCreator,
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
        return getWfAwareInitializer().deployWorkflow(tmpWf, resourceName);
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
