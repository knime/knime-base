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
 *   Nov 20, 2024 (Tobias Kampmann): created
 */
package org.knime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.xmlbeans.XmlException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.time.duration.DurationCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.period.PeriodCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NoDescriptionProxy;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.util.InputTableNode.InputDataNodeFactory;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
import org.xml.sax.SAXException;

/**
 * Some helpers for providing input data to a node in a workflow, for testing purposes.
 *
 * @author Tobias Kampmann
 */
public final class DateTimeTestingUtil {

    /**
     * The name of the column containing integer values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_INT = "int";

    /**
     * The name of the column containing duration values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_DURATION = "duration";

    /**
     * The name of the column containing period values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_PERIOD = "period";

    /**
     * The name of the column containing long values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_LONG = "long";

    /**
     * The name of the column containing string values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_STRING = "string";

    /**
     * The name of the column containing local date values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_LOCAL_DATE = "localDate";

    /**
     * The value of the column containing local date values in the test table. See {@link #createDefaultTestTable()}.
     */
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 12, 1);

    /**
     * The name of the column containing local time values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_LOCAL_TIME = "localTime";

    /**
     * The value of the column containing local date time values in the test table. See
     * {@link #createDefaultTestTable()}.
     */
    private static final LocalTime TEST_TIME = LocalTime.of(12, 11, 10);

    /**
     * The name of the column containing local date time values in the test table. See
     * {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_LOCAL_DATE_TIME = "localDateTime";

    /**
     * The value of the column containing local date time values in the test table. See
     * {@link #createDefaultTestTable()}.
     */
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(TEST_DATE, TEST_TIME);

    /**
     * The name of the column containing zoned date time values in the test table. See
     * {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_ZONED_DATE_TIME = "zoneDateTime";

    /**
     * The value of the column containing zoned date time values in the test table. See
     * {@link #createDefaultTestTable()}.
     */
    private static final ZonedDateTime TEST_ZONED_DATE_TIME = TEST_DATE_TIME.atZone(ZoneId.of("Australia/Lord_Howe"));

    /**
     * Adds a table to the input port of the provided node.
     *
     * @param wfm the workflow manager that handles adding the table
     * @param testTableSupplier the supplier of the table to add
     * @param nodeToConnectTo the node to connect the table to, that should accept the table as input
     * @param portIndex the index of the input port of the node to connect the table to
     */
    public static void addTableToNodeInputPort( //
        final WorkflowManager wfm, //
        final Supplier<BufferedDataTable> testTableSupplier, //
        final NodeContainer nodeToConnectTo, //
        final int portIndex) {

        var inputDataTableNode = WorkflowManagerUtil //
            .createAndAddNode(wfm, new InputDataNodeFactory(testTableSupplier));

        wfm.addConnection( //
            inputDataTableNode.getID(), 1, //
            nodeToConnectTo.getID(), portIndex //
        );
    }

    /**
     * Adds a default table (see {@link #createDefaultTestTable()} to the input port of the provided node.
     *
     * @param wfm the workflow manager that handles adding the table
     * @param nodeToConnectTo the node to connect the table to, that should accept the table as input
     * @param portIndex the index of the input port of the node to connect the table to
     * @return
     */
    public static Supplier<BufferedDataTable> addDefaultTableToNodeInputPort(final WorkflowManager wfm,
        final NodeContainer nodeToConnectTo, final int portIndex) {

        var table = createDefaultTestTable();

        addTableToNodeInputPort(wfm, table, nodeToConnectTo, portIndex);

        return table;
    }

    /**
     * Extract the first row from the table.
     *
     * @param table the table to extract the row from
     * @return the first row of the table
     */
    public static DataRow getFirstRow(final BufferedDataTable table) {
        try (var it = table.iterator()) {
            return it.next();
        }
    }

    /**
     * Extract the last row from the table.
     *
     * @param table the table to extract the row from
     * @return the last row of the table
     */
    public static DataRow getLastRow(final BufferedDataTable table) {
        try (var it = table.iterator()) {
            DataRow row = null;
            while (it.hasNext()) {
                row = it.next();
            }
            return row;
        }
    }

    /**
     * Creates a TableSupplier, with a single row, which can be used for unit testing implementations of view nodes.
     *
     * @return a TableSupplier object containing the created table
     */
    public static Supplier<BufferedDataTable> createDefaultTestTable() {
        NamedCell[] cells = {new NamedCell(COLUMN_INT, new IntCell(0)),
            new NamedCell(COLUMN_DURATION, new DurationCellFactory().createCell("PT1H")),
            new NamedCell(COLUMN_PERIOD, new PeriodCellFactory().createCell("P1D")),
            new NamedCell(COLUMN_LONG, new LongCell(0)), new NamedCell(COLUMN_STRING, new StringCell("i")),
            new NamedCell(COLUMN_LOCAL_DATE,
                new LocalDateCellFactory().createCell(TEST_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE))),
            new NamedCell(COLUMN_LOCAL_TIME,
                new LocalTimeCellFactory().createCell(TEST_TIME.format(DateTimeFormatter.ISO_LOCAL_TIME))),
            new NamedCell(COLUMN_LOCAL_DATE_TIME,
                new LocalDateTimeCellFactory()
                    .createCell(TEST_DATE_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))),
            new NamedCell(COLUMN_ZONED_DATE_TIME, new ZonedDateTimeCellFactory()
                .createCell(TEST_ZONED_DATE_TIME.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)))};

        return createTestTableSupplier(cells);
    }

    /**
     * Creates a {@link Supplier} that will return a table with one single row, and columns as specified by the
     * {@link NamedCell} objects.
     *
     * @param cells the list of cells to create the table with
     * @return a {@link Supplier} that will return the table
     */
    public static Supplier<BufferedDataTable> createTestTableSupplier(final NamedCell... cells) {

        var testSpec = new TableTestUtil.SpecBuilder();
        Arrays.stream(cells).forEach(c -> testSpec.addColumn(c.columnName, c.cell.getType()));

        final var builder = new TableTestUtil.TableBuilder(testSpec.build());

        var dataCells = Arrays.stream(cells).map(c -> c.cell).toArray(DataCell[]::new);
        builder.addRow((Object[])dataCells);

        return builder.build();
    }

    /**
     * A simple record to hold a column name and a cell value. Used by {@link #createTestTableSupplier(NamedCell...)} to
     * create a test table with a single row.
     *
     * @param columnName the name of the column
     * @param cell the value that should be in the column
     */
    public record NamedCell(String columnName, DataCell cell) {
    }
}
