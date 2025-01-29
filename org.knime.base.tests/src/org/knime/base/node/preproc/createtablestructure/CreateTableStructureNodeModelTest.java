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
 *   Jan 29, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.createtablestructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.base.node.preproc.createtablestructure.CreateTableStructureNodeSettings.ColumnSettings;
import org.knime.base.node.preproc.createtablestructure.CreateTableStructureNodeSettings.ColumnSettingsMigration;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.util.WorkflowManagerUtil;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class CreateTableStructureNodeModelTest {

    private static final String NODE_NAME = "CreateTableStructureNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = CreateTableStructureNodeSettings.class;

    @Test
    void testIntegerMigration() throws InvalidSettingsException {
        final int colCount = 10;
        final String colType = "Integer";
        final String colPrefix = "Col ";
        final var oldNodeSettings = new NodeSettings("model");
        oldNodeSettings.addInt("colCount", colCount);
        oldNodeSettings.addString("colType", colType);
        oldNodeSettings.addString("colPrefix", colPrefix);

        final var newColumnSettings = ColumnSettingsMigration.load(oldNodeSettings);

        assertNotNull(newColumnSettings, "Should not be null");
        assertEquals(colCount, newColumnSettings.length, "Should have the same number of columns");
        for (int i = 0; i < newColumnSettings.length; i++) {
            assertEquals(colPrefix + (i + 1), newColumnSettings[i].m_columnName, "Should have the correct name");
            assertEquals(IntCellFactory.TYPE, newColumnSettings[i].getDataType(), "Should have the correct type");
        }
    }

    @Test
    void testStringMigration() throws InvalidSettingsException {
        final int colCount = 8;
        final String colType = "String";
        final String colPrefix = "Columns ";
        final var oldNodeSettings = new NodeSettings("model");
        oldNodeSettings.addInt("colCount", colCount);
        oldNodeSettings.addString("colType", colType);
        oldNodeSettings.addString("colPrefix", colPrefix);

        final var newColumnSettings = ColumnSettingsMigration.load(oldNodeSettings);

        assertNotNull(newColumnSettings, "Should not be null");
        assertEquals(colCount, newColumnSettings.length, "Should have the same number of columns");
        for (int i = 0; i < newColumnSettings.length; i++) {
            assertEquals(colPrefix + (i + 1), newColumnSettings[i].m_columnName, "Should have the correct name");
            assertEquals(StringCellFactory.TYPE, newColumnSettings[i].getDataType(), "Should have the correct type");
        }
    }

    @Test
    void testDoubleMigration() throws InvalidSettingsException {
        final int colCount = 7;
        final String colType = "Double";
        final String colPrefix = "Cols ";
        final var oldNodeSettings = new NodeSettings("model");
        oldNodeSettings.addInt("colCount", colCount);
        oldNodeSettings.addString("colType", colType);
        oldNodeSettings.addString("colPrefix", colPrefix);

        final var newColumnSettings = ColumnSettingsMigration.load(oldNodeSettings);

        assertNotNull(newColumnSettings, "Should not be null");
        assertEquals(colCount, newColumnSettings.length, "Should have the same number of columns");
        for (int i = 0; i < newColumnSettings.length; i++) {
            assertEquals(colPrefix + (i + 1), newColumnSettings[i].m_columnName, "Should have the correct name");
            assertEquals(DoubleCellFactory.TYPE, newColumnSettings[i].getDataType(), "Should have the correct type");
        }
    }

    @Test
    void testWithMultipleColumns() throws InvalidSettingsException, IOException {
        final var settings = new CreateTableStructureNodeSettings();
        settings.m_columnSettings = new ColumnSettings[]{ //
            new ColumnSettings("TestCol1", StringCellFactory.TYPE), //
            new ColumnSettings("TestCol2", IntCellFactory.TYPE), //
            new ColumnSettings("TestCol3", DoubleCellFactory.TYPE)};

        final var output = setupAndExecuteWorkflow(settings);

        assertNotNull(output, "Output table should not be null");
        assertEquals(3, output.getDataTableSpec().getNumColumns(), "Output table should have 3 columns");
        final var spec = output.getDataTableSpec();
        for (int i = 0; i < output.getDataTableSpec().getNumColumns(); i++) {
            final var colSpec = spec.getColumnSpec(i);
            assertEquals(settings.m_columnSettings[i].m_columnName, colSpec.getName(), "Wrong column name");
            assertEquals(settings.m_columnSettings[i].getDataType(), colSpec.getType(), "Wrong column type");
        }
    }

    private static BufferedDataTable setupAndExecuteWorkflow(final CreateTableStructureNodeSettings settings)
        throws InvalidSettingsException, IOException {

        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new CreateTableStructureNodeFactory());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);

        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // execute and wait...
        workflowManager.executeAllAndWaitUntilDone();
        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();
        return outputTable;
    }
}
