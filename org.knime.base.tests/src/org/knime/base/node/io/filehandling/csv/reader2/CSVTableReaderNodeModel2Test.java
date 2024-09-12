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
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.TableReaderNodeModel;
import org.knime.testing.util.WorkflowManagerUtil;
import org.xml.sax.SAXException;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
class CSVTableReaderNodeModel2Test extends LocalWorkflowContextTest {

    private NativeNodeContainer m_csvReader;

    @BeforeEach
    void addCSVReaderToWorkflow() throws IOException {
        m_csvReader = WorkflowManagerUtil.createAndAddNode(m_wfm, new CSVTableReaderNodeFactory2());
    }

    @Test
    void testReadValidCSV() throws IOException, InvalidSettingsException {
        final var file = createCsvFile();

        final var settings = new CSVTableReaderNodeSettings();
        settings.m_settings.m_source = new FileChooser(new FSLocation(FSCategory.LOCAL, file.toString()));
        setSettings(settings);

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_csvReader.getNodeContainerState().isExecuted());
        assertArrayEquals(new String[]{"colName", "colName2"}, getOutputTable().getSpec().getColumnNames());
    }

    private static Path createCsvFile() throws IOException {
        final var file = Files.createTempFile(null, ".csv").toAbsolutePath();
        try (final var writer = new BufferedWriter(new FileWriter(file.toString()))) {
            writer.write("colName, colName2\n");
            writer.write("1, 2\n");
        }
        return file;
    }

    private static Path createTsvFile() throws IOException {
        final var file = Files.createTempFile(null, ".tsv").toAbsolutePath();
        try (final var writer = new BufferedWriter(new FileWriter(file.toString()))) {
            writer.write("colName\tcolName2\n");
            writer.write("1\t2\n");
        }
        return file;
    }

    @Test
    void testReadMissingFile() throws InvalidSettingsException {
        final var settings = new CSVTableReaderNodeSettings();
        final var missingFile = "foo";
        settings.m_settings.m_source = new FileChooser(new FSLocation(FSCategory.LOCAL, missingFile));
        setSettings(settings);

        assertTrue(m_csvReader.getNodeContainerState().isConfigured());
        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_csvReader.getNodeContainerState().isConfigured());
        assertEquals(String.format("ERROR: Execute failed: The specified file %s does not exist.", missingFile),
            m_csvReader.getNodeMessage().toString());
    }

    @Test
    void testThrowInvalidSettingsExceptionOnBlankCustomEncoding() throws IOException, InvalidSettingsException {
        final var settings = new CSVTableReaderNodeSettings();
        settings.m_encoding.m_charset.m_fileEncoding = FileEncodingOption.OTHER;
        settings.m_encoding.m_charset.m_customEncoding = " ";
        assertThrows(InvalidSettingsException.class, () -> setSettings(settings));
    }

    @Test
    void testWarningMessagesAfterInitialConfigure() throws Exception {
        assertTrue(m_csvReader.getNodeContainerState().isIdle());
        assertEquals("WARNING: Please specify a file", m_csvReader.getNodeMessage().toString());
    }

    @Test
    void testReadValidCSVFromURL() throws IOException, InvalidSettingsException {
        final var file = createCsvFile();

        m_wfm.removeNode(m_csvReader.getID());
        m_csvReader =
            WorkflowManagerUtil.createAndAddNode(m_wfm, new TestCSVTableReaderNodeFactory2(file.toUri().toURL()));

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_csvReader.getNodeContainerState().isExecuted());
        assertArrayEquals(new String[]{"colName", "colName2"}, getOutputTable().getSpec().getColumnNames());
    }

    @Test
    void testReadValidTSVFromURL() throws IOException, InvalidSettingsException {
        final var file = createTsvFile();

        m_wfm.removeNode(m_csvReader.getID());
        m_csvReader =
            WorkflowManagerUtil.createAndAddNode(m_wfm, new TestCSVTableReaderNodeFactory2(file.toUri().toURL()));

        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_csvReader.getNodeContainerState().isExecuted());
        assertArrayEquals(new String[]{"colName", "colName2"}, getOutputTable().getSpec().getColumnNames());
    }

    private BufferedDataTable getOutputTable() {
        return (BufferedDataTable)m_csvReader.getOutPort(1).getPortObject();
    }

    private void setSettings(final CSVTableReaderNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("CSVReader");
        m_wfm.saveNodeSettings(m_csvReader.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(CSVTableReaderNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_csvReader.getID(), nodeSettings);
    }

    private static class TestCSVTableReaderNodeFactory2
        extends ConfigurableNodeFactory<TableReaderNodeModel<FSPath, CSVTableReaderConfig, Class<?>>>
        implements NodeDialogFactory {

        private final CSVTableReaderNodeFactory2 m_delegate = new CSVTableReaderNodeFactory2();

        private final URL m_url;

        TestCSVTableReaderNodeFactory2(final URL url) {
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
        protected TableReaderNodeModel<FSPath, CSVTableReaderConfig, Class<?>>
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

        @Override
        public NodeView<TableReaderNodeModel<FSPath, CSVTableReaderConfig, Class<?>>> createNodeView(
            final int viewIndex, final TableReaderNodeModel<FSPath, CSVTableReaderConfig, Class<?>> nodeModel) {
            return m_delegate.createNodeView(viewIndex, nodeModel);
        }

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
