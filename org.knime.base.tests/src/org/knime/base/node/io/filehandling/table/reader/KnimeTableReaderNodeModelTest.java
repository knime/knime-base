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
 *   Feb 18, 2026 (created)
 */
package org.knime.base.node.io.filehandling.table.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.FileLocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.base.node.io.filehandling.webui.testing.LocalWorkflowContextTest;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.util.FileUtil;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.DefaultFileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 * Test for the KNIME Table Reader node model.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
@SuppressWarnings("restriction")
class KnimeTableReaderNodeModelTest extends LocalWorkflowContextTest {

    private static final String TEST_FILE = "/files/test.table";

    private NativeNodeContainer m_tableReader;

    @Test
    void testReadValidTableFile() throws IOException, URISyntaxException, InvalidSettingsException {
        // given
        m_tableReader = WorkflowManagerUtil.createAndAddNode(m_wfm, new KnimeTableReaderNodeFactory());
        final var settings = new KnimeTableReaderNodeParameters();
        settings.m_knimeTableReaderParameters.m_multiFileSelectionParams.m_source =
            new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DefaultFileChooserFilters(),
                new FSLocation(FSCategory.LOCAL, getTestFilePath(TEST_FILE)));
        setSettings(settings);

        // when
        m_wfm.executeAllAndWaitUntilDone();

        // then
        assertTrue(m_tableReader.getNodeContainerState().isExecuted());
        final var outputTable = getOutputTable();
        assertThat(outputTable).isNotNull();
        assertThat(outputTable.size()).isGreaterThan(0);
        assertThat(outputTable.getDataTableSpec().getColumnNames()).containsExactly("column1", "column2");
    }

    @Test
    void testReadValidTableFileFromURL() throws IOException, URISyntaxException {
        // given
        final var fileUrl =
            FileLocator.toFileURL(KnimeTableReaderNodeModelTest.class.getResource(TEST_FILE).toURI().toURL());
        final var creationConfig = new KnimeTableReaderNodeFactory().createNodeCreationConfig();
        creationConfig.setURLConfiguration(fileUrl);

        // when
        var nodeId = m_wfm.addNodeAndApplyContext(new KnimeTableReaderNodeFactory(), creationConfig, 42);

        // then
        m_tableReader = (NativeNodeContainer)m_wfm.getNodeContainer(nodeId);
        m_wfm.executeAllAndWaitUntilDone();
        assertTrue(m_tableReader.getNodeContainerState().isExecuted());
        final var outputTable = getOutputTable();
        assertThat(outputTable).isNotNull();
        assertThat(outputTable.getDataTableSpec().getColumnNames()).containsExactly("column1", "column2");
    }

    private BufferedDataTable getOutputTable() {
        return (BufferedDataTable)m_tableReader.getOutPort(1).getPortObject();
    }

    private void setSettings(final KnimeTableReaderNodeParameters settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("TableReader");
        m_wfm.saveNodeSettings(m_tableReader.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        NodeParametersUtil.saveSettings(KnimeTableReaderNodeParameters.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_tableReader.getID(), nodeSettings);
    }

    private static String getTestFilePath(final String path) throws IOException, URISyntaxException {
        var url = FileLocator.toFileURL(KnimeTableReaderNodeModelTest.class.getResource(path).toURI().toURL());
        return FileUtil.getFileFromURL(url).toPath().toString();
    }

}
