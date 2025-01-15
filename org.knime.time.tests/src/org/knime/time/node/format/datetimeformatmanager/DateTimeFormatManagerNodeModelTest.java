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
 *   Nov 20, 2024 (Tobias Kampmann, TNG): created
 */
package org.knime.time.node.format.datetimeformatmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

import org.apache.commons.lang3.LocaleUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.InputTableNode;
import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.property.ValueFormatHandler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.mockito.Mockito;

@SuppressWarnings({"restriction", "squid:S5960"})
class DateTimeFormatManagerNodeModelTest {

    private NativeNodeContainer m_dateTimeFormatManagerNode;

    private WorkflowManager m_wfm;

    private static final String INPUT_COLUMN = "test_input";

    private static final String NODE_NAME = "DateTimeFormatManagerNode";

    private static final Class<? extends DefaultNodeSettings> SETTINGS_CLASS = DateTimeFormatManagerNodeSettings.class;

    @BeforeEach
    void resetWorkflow() throws IOException {
        m_wfm = WorkflowManagerUtil.createEmptyWorkflow();
        m_dateTimeFormatManagerNode =
            WorkflowManagerUtil.createAndAddNode(m_wfm, new DateTimeFormatManagerNodeFactory());
    }

    @Test
    void canBeExecuted() throws InvalidSettingsException {

        final var settings = new DateTimeFormatManagerNodeSettings();
        setSettings(settings);

        InputTableNode.addDefaultTableToNodeInputPort(m_wfm, m_dateTimeFormatManagerNode, 1);

        assertTrue(m_wfm.executeAllAndWaitUntilDone(), "The workflow should have been executed.");
        assertTrue(m_dateTimeFormatManagerNode.getNodeContainerState().isExecuted());
    }

    @Test
    void checkThatFormatsAreAddedToHistory() throws InvalidSettingsException, IOException {
        var settings = new DateTimeFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{InputTableNode.COLUMN_LOCAL_TIME});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_format = "HH";

        try (final var staticStringHistoryManagerMock =
            Mockito.mockStatic(DateTimeFormatStringHistoryManager.class, Mockito.CALLS_REAL_METHODS)) {
            connectInputTableAndExecute(settings);

            staticStringHistoryManagerMock
                .verify(() -> DateTimeFormatStringHistoryManager.addFormatToStringHistoryIfNotPresent("HH"));
        }
    }

    @Test
    void appendDateTimeFormatter() throws InvalidSettingsException {

        String[] columnNamesToAddFormatterTo = { //
            InputTableNode.COLUMN_LOCAL_TIME, //
            InputTableNode.COLUMN_LOCAL_DATE_TIME, //
            InputTableNode.COLUMN_LOCAL_DATE, //
            InputTableNode.COLUMN_ZONED_DATE_TIME};

        final var settings = new DateTimeFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(columnNamesToAddFormatterTo);
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_format = "yyyy-MM-dd HH:mm:ss [VV]";
        settings.m_alignmentSuggestion = AlignmentSuggestionOption.CENTER;

        var formatterToAppend = new ValueFormatHandler(new DateTimeDataValueFormatter( //
            settings.m_format, //
            LocaleUtils.toLocale(settings.m_locale), settings.m_alignmentSuggestion));

        var inputTable = connectInputTableAndExecute(settings);

        var outputTable = (BufferedDataTable)m_dateTimeFormatManagerNode.getOutPort(1).getPortObject();

        var specsHaveTheSameStructure =
            outputTable.getDataTableSpec().equalStructure(inputTable.get().getDataTableSpec());
        assertTrue(specsHaveTheSameStructure, "The output table should have the same structure as the input table");

        for (var columnName : columnNamesToAddFormatterTo) {
            assertColumnContainsFormatter(outputTable, columnName, formatterToAppend);
        }
    }

    @Test
    void testThatMissingInputGivesMissingOutput() throws InvalidSettingsException, IOException {

        final var settings = new DateTimeFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_locale = Locale.ENGLISH.toLanguageTag();
        settings.m_format = "yyyy-MM-dd HH:mm:ss [VV]";
        settings.m_alignmentSuggestion = AlignmentSuggestionOption.CENTER;

        var testSetup = setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.success, "Execution should have been successful");
        assertTrue(testSetup.firstCell.isMissing(), "Output cell should be missing");
    }

    private Supplier<BufferedDataTable> connectInputTableAndExecute(final DateTimeFormatManagerNodeSettings settings)
        throws InvalidSettingsException {
        setSettings(settings);

        var inputTable = InputTableNode.createDefaultTestTable();

        InputTableNode.addTableToNodeInputPort(m_wfm, inputTable, m_dateTimeFormatManagerNode, 1);
        assertTrue(m_wfm.executeAllAndWaitUntilDone(), "The workflow should have been executed");
        return inputTable;
    }

    private static void assertColumnContainsFormatter(final BufferedDataTable table, final String columnName,
        final ValueFormatHandler expectedFormatter) {

        var columnExistsInTable = Arrays.stream(table.getDataTableSpec().getColumnNames()).anyMatch(columnName::equals);
        assertTrue(columnExistsInTable, "The column " + columnName + " should exist in the output table");

        assertEquals(expectedFormatter, table.getDataTableSpec().getColumnSpec(columnName).getValueFormatHandler(),
            "The column " + columnName + " should have the correct formatter attached");
    }

    private void setSettings(final DateTimeFormatManagerNodeSettings settings) throws InvalidSettingsException {
        final var nodeSettings = new NodeSettings("DateTimeRoundNode");
        m_wfm.saveNodeSettings(m_dateTimeFormatManagerNode.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DateTimeFormatManagerNodeSettings.class, settings, modelSettings);
        m_wfm.loadNodeSettings(m_dateTimeFormatManagerNode.getID(), nodeSettings);
    }

    record TestSetup(BufferedDataTable outputTable, DataCell firstCell, boolean success) {
    }

    static TestSetup setupAndExecuteWorkflow(final DateTimeFormatManagerNodeSettings settings, final DataCell cellToAdd)
        throws InvalidSettingsException, IOException {
        var workflowManager = WorkflowManagerUtil.createEmptyWorkflow();

        var node = WorkflowManagerUtil.createAndAddNode(workflowManager, new DateTimeFormatManagerNodeFactory());

        // set the settings
        final var nodeSettings = new NodeSettings(NODE_NAME);
        workflowManager.saveNodeSettings(node.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(SETTINGS_CLASS, settings, modelSettings);
        workflowManager.loadNodeSettings(node.getID(), nodeSettings);

        // populate the input table
        var inputTableSpec = new TableTestUtil.SpecBuilder() //
            .addColumn(INPUT_COLUMN, cellToAdd.getType()) //
            .build();
        var inputTable = new TableTestUtil.TableBuilder(inputTableSpec) //
            .addRow(cellToAdd) //
            .build();
        var tableSupplierNode =
            WorkflowManagerUtil.createAndAddNode(workflowManager, new InputTableNode.InputDataNodeFactory(inputTable));

        // link the nodes
        workflowManager.addConnection(tableSupplierNode.getID(), 1, node.getID(), 1);

        // execute and wait...
        var success = workflowManager.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)node.getOutPort(1).getPortObject();

        try (var it = outputTable.iterator()) {
            return new TestSetup( //
                outputTable, //
                it.next().getCell(0), //
                success //
            );
        }
    }
}
