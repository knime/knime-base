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
 *   Jan 10, 2024 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.io.filehandling.webui.LocalWorkflowContextTest;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.TableReaderNodeModel;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.SourceSettings;
import org.knime.testing.util.WorkflowManagerUtil;
import org.xml.sax.SAXException;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
class TableReaderNodeModel2Test extends LocalWorkflowContextTest {

    private NativeNodeContainer m_tableReader;

    @BeforeEach
    void addTableReaderToWorkflow() throws IOException {
        m_tableReader = WorkflowManagerUtil.createAndAddNode(m_wfm, new TableReaderNodeFactory2());
    }

    @Test
    void testReadValidTable() throws IOException, InvalidSettingsException {
        final var file = createTableFile();

        final var settings = new TableReaderNodeSettings();
        settings.m_settings.m_source = new FileSelection(new FSLocation(FSCategory.LOCAL, file.toString()));
        setSettings(settings);

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_tableReader.getNodeContainerState().isExecuted());
        assertArrayEquals(new String[]{"intCol", "stringCol"}, getOutputTable().getSpec().getColumnNames());
    }

    private static Path createTableFile() throws IOException {
        final var file = Files.createTempFile(null, ".table").toAbsolutePath();
        TableReaderTransformationSettingsStateProvidersTest.createTableFile(file.toString());
        return file;
    }

    @Test
    void testReadMissingFile() throws InvalidSettingsException {
        final var settings = new TableReaderNodeSettings();
        final var missingFile = "foo";
        settings.m_settings.m_source = new FileSelection(new FSLocation(FSCategory.LOCAL, missingFile));
        setSettings(settings);

        assertTrue(m_tableReader.getNodeContainerState().isConfigured());
        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_tableReader.getNodeContainerState().isConfigured());
        assertEquals(String.format("ERROR: Execute failed: The specified file %s does not exist.", missingFile),
            m_tableReader.getNodeMessage().toString());
    }

    @Test
    void testWarningMessagesAfterInitialConfigure() throws Exception {
        assertTrue(m_tableReader.getNodeContainerState().isIdle());
        assertEquals("WARNING: Please specify a file", m_tableReader.getNodeMessage().toString());
    }

    @Test
    void testReadValidTableFromURL() throws IOException, InvalidSettingsException {
        final var file = createTableFile();

        m_wfm.removeNode(m_tableReader.getID());
        m_tableReader =
            WorkflowManagerUtil.createAndAddNode(m_wfm, new TestTableReaderNodeFactory2(file.toUri().toURL()));

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_tableReader.getNodeContainerState().isExecuted());
        assertArrayEquals(new String[]{"intCol", "stringCol"}, getOutputTable().getSpec().getColumnNames());
    }

    private BufferedDataTable getOutputTable() {
        return (BufferedDataTable)m_tableReader.getOutPort(1).getPortObject();
    }

    private void setSettings(final TableReaderNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("TableReader");
        m_wfm.saveNodeSettings(m_tableReader.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        NodeParametersUtil.saveSettings(TableReaderNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_tableReader.getID(), nodeSettings);
    }

    private static class TestTableReaderNodeFactory2
        extends ConfigurableNodeFactory<TableReaderNodeModel<FSPath, SourceSettings<FSPath>, TableManipulatorConfig, DataType, StorableMultiTableReadConfig<TableManipulatorConfig, DataType>>>
        implements NodeDialogFactory {

        private final TableReaderNodeFactory2 m_delegate = new TableReaderNodeFactory2();

        private final URL m_url;

        TestTableReaderNodeFactory2(final URL url) {
            super(true);
            m_url = url;
        }

        @Override
        public NodeDialog createNodeDialog() {
            return m_delegate.createNodeDialog();
        }

        @Override
        protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
            return m_delegate.createPortsConfigBuilder();
        }

        @Override
        protected
            TableReaderNodeModel<FSPath, SourceSettings<FSPath>, TableManipulatorConfig, DataType, StorableMultiTableReadConfig<TableManipulatorConfig, DataType>>
            createNodeModel(final NodeCreationConfiguration creationConfig) {
            final var modifiableCreationConfig = (ModifiableNodeCreationConfiguration)creationConfig;
            modifiableCreationConfig.setURLConfiguration(m_url);
            return m_delegate.createNodeModel(modifiableCreationConfig);
        }

        @Override
        protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
            return m_delegate.createNodeDialogPane(creationConfig);
        }

        @Override
        protected int getNrNodeViews() {
            return 0;
        }

        @Deprecated
        @Override
        public
            NodeView<TableReaderNodeModel<FSPath, SourceSettings<FSPath>, TableManipulatorConfig, DataType, StorableMultiTableReadConfig<TableManipulatorConfig, DataType>>>
            createNodeView(final int viewIndex,
                final TableReaderNodeModel<FSPath, SourceSettings<FSPath>, TableManipulatorConfig, DataType, StorableMultiTableReadConfig<TableManipulatorConfig, DataType>> nodeModel) {
            return m_delegate.createNodeView(viewIndex, nodeModel);
        }

        @Deprecated
        @Override
        protected boolean hasDialog() {
            return true;
        }

        @Override
        protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
            return m_delegate.createNodeDescription();
        }
    }
}
