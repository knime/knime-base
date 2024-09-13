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
 *   Aug 12, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeFactory2;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSConnectionRegistry;
import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.testing.util.WorkflowManagerUtil;

@SuppressWarnings("restriction")
class FileSystemPortConnectionUtilTest extends LocalWorkflowContextTest {

    private static final String dummyFileSystemId = "myDummyFileSystem";

    private static final FSConnection dummyFSConnection = mock(FSConnection.class);

    @BeforeAll
    static void registerDummyConnection() {
        FSConnectionRegistry.getInstance().register(dummyFileSystemId, dummyFSConnection);
    }

    static void deregisterDummyConnection() throws IOException {
        FSConnectionRegistry.getInstance().deregister(dummyFileSystemId).close();
    }

    private NativeNodeContainer m_csvReader;

    @BeforeEach
    void addCSVReaderToWorkflow() throws IOException {
        m_csvReader = WorkflowManagerUtil.createAndAddNode(m_wfm, new CSVTableReaderNodeFactory2());

    }

    void addPort() {
        final var creationConfigCopy = m_csvReader.getNode().getCopyOfCreationConfig().orElseThrow();
        ((ExtendablePortGroup)creationConfigCopy.getPortConfig().orElseThrow().getGroup("File System Connection"))
            .addPort(FileSystemPortObject.TYPE);
        m_wfm.replaceNode(m_csvReader.getID(), creationConfigCopy);
        m_csvReader = (NativeNodeContainer)m_wfm.getNodeContainer(m_csvReader.getID());
    }

    DefaultNodeSettingsContext getDefaultNodeSettingsContextWithoutConnection() {

        NodeContext.pushContext(m_csvReader);
        return DefaultNodeSettings.createDefaultNodeSettingsContext(new PortObjectSpec[]{});
    }

    DefaultNodeSettingsContext getContextForEmptySpec() {
        return getContextForConnection(null);
    }

    DefaultNodeSettingsContext getContextForEmptyConnection() {
        return getContextForConnection(new FileSystemPortObjectSpec());
    }

    DefaultNodeSettingsContext getContextForDummyConnection() {

        return getContextForConnection(new FileSystemPortObjectSpec(null, dummyFileSystemId, null));
    }

    private DefaultNodeSettingsContext getContextForConnection(final FileSystemPortObjectSpec spec) {

        NodeContext.pushContext(m_csvReader);
        return DefaultNodeSettings.createDefaultNodeSettingsContext(new PortObjectSpec[]{spec});
    }

    static final class DummyFSConnection implements FSConnection {

        @Override
        public FSFileSystem<?> getFileSystem() {
            return null;
        }

        @Override
        public FileSystemBrowser getFileSystemBrowser() {
            return null;
        }

    }

    @AfterEach
    void clearNodeContext() {
        NodeContext.removeLastContext();
    }

    @Test
    void testDoesNotApplyWithoutPort() {
        assertFalse(new ConnectedWithoutFileSystemSpec().applies(getDefaultNodeSettingsContextWithoutConnection()));
    }

    @Test
    void testAppliesWithoutSpec() {
        addPort();
        assertTrue(new ConnectedWithoutFileSystemSpec().applies(getContextForEmptySpec()));
    }

    @Test
    void testAppliesWithoutConnection() {
        addPort();
        assertTrue(new ConnectedWithoutFileSystemSpec().applies(getContextForEmptyConnection()));
    }

    @SuppressWarnings("resource")
    @Test
    void testDoesNotApplyWithConnection() {
        addPort();
        assertFalse(new ConnectedWithoutFileSystemSpec().applies(getContextForDummyConnection()));
    }

}
