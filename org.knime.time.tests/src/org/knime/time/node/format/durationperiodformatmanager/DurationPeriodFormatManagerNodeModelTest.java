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
 *   Jan 8, 2025 (david): created
 */
package org.knime.time.node.format.durationperiodformatmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.knime.DateTimeTestingUtil;
import org.knime.NodeModelTestRunnerUtil;
import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.testing.util.WorkflowManagerUtil;
import org.knime.time.util.DurationPeriodStringFormat;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"static-method", "restriction"})
final class DurationPeriodFormatManagerNodeModelTest {

    private static final String INPUT_COLUMN = "test_input";

    private static final NodeModelTestRunnerUtil RUNNER =
        new NodeModelTestRunnerUtil(INPUT_COLUMN, "DurationPeriodFormatManagerNode",
            DurationPeriodFormatManagerNodeSettings.class, DurationPeriodFormatManagerNodeFactory.class);

    @Test
    void testSetFormatter() throws IOException, InvalidSettingsException {
        String[] columnNamesToAddFormaterTo = { //
            DateTimeTestingUtil.COLUMN_DURATION, //
            DateTimeTestingUtil.COLUMN_PERIOD //
        };

        var wfm = WorkflowManagerUtil.createEmptyWorkflow();
        var formatManagerNode = WorkflowManagerUtil.createAndAddNode( //
            wfm, //
            new DurationPeriodFormatManagerNodeFactory() //
        );

        final var settings = new DurationPeriodFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(columnNamesToAddFormaterTo);
        settings.m_format = DurationPeriodStringFormat.WORDS;
        settings.m_alignment = AlignmentSuggestionOption.CENTER;

        final var nodeSettings = new NodeSettings("DurationPeriodFormatManagerNode");
        wfm.saveNodeSettings(formatManagerNode.getID(), nodeSettings);
        var modelSettings = nodeSettings.addNodeSettings("model");
        DefaultNodeSettings.saveSettings(DurationPeriodFormatManagerNodeSettings.class, settings, modelSettings);
        wfm.loadNodeSettings(formatManagerNode.getID(), nodeSettings);

        var inTable = DateTimeTestingUtil.createDefaultTestTable();
        DateTimeTestingUtil.addTableToNodeInputPort(wfm, inTable, formatManagerNode, 1);

        wfm.executeAllAndWaitUntilDone();

        var outputTable = (BufferedDataTable)formatManagerNode.getOutPort(1).getPortObject();

        assertTrue(outputTable.getDataTableSpec().equalStructure(inTable.get().getDataTableSpec()),
            "adding a formatter should leave the structure of the spec unchanged");

        for (var columnName : columnNamesToAddFormaterTo) {
            var outSpec = outputTable.getDataTableSpec().getColumnSpec(columnName);

            assertTrue(outSpec.getValueFormatHandler().getFormatModel() instanceof DurationPeriodDataValueFormatter,
                "the column " + columnName + " should have a formatter of the correct class");

            var formatter = (DurationPeriodDataValueFormatter)outSpec.getValueFormatHandler().getFormatModel();

            assertEquals(
                new DurationPeriodDataValueFormatter(DurationPeriodStringFormat.WORDS,
                    AlignmentSuggestionOption.CENTER),
                formatter, "Expected formatted column to have the right attached formatter");
        }
    }

    @Test
    void testThatMissingInputGivesMissingOutput() throws IOException, InvalidSettingsException {
        final var settings = new DurationPeriodFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_format = DurationPeriodStringFormat.WORDS;
        settings.m_alignment = AlignmentSuggestionOption.CENTER;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, DataType.getMissingCell());

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertTrue(testSetup.firstCell().isMissing(), "Output cell should be missing");
    }

    @Test
    void testThatEmptyInputGivesNoError() throws InvalidSettingsException, IOException {
        final var settings = new DurationPeriodFormatManagerNodeSettings();
        settings.m_columnFilter = new ColumnFilter(new String[]{INPUT_COLUMN});
        settings.m_format = DurationPeriodStringFormat.WORDS;
        settings.m_alignment = AlignmentSuggestionOption.CENTER;

        var testSetup = RUNNER.setupAndExecuteWorkflow(settings, null, LocalDateTimeCellFactory.TYPE);

        assertTrue(testSetup.nodeState().isExecuted(), "Execution should have been successful");
        assertNull(testSetup.firstCell(), "Output cell should not exists");
        assertEquals(0, testSetup.outputTable().size(), "Ouptput table should be empty");
    }
}
