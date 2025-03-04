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
 *   Feb 4, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.util;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.WorkflowManagerUtil;
/**
 * Some helpers for providing input data to a node in a workflow, for testing purposes.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
public final class InputTableNode {
    /**
     * The name of the column containing integer values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_INT = "int";
    /**
     * The name of the column containing long values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_LONG = "long";
    /**
     * The name of the column containing string values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_STRING = "string";
    /**
     * The name of the column containing double values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_DOUBLE = "double";
    /**
     * The name of the column containing boolean values in the test table. See {@link #createDefaultTestTable()}.
     */
    public static final String COLUMN_BOOLEAN = "boolean";
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
        NamedCell[] cells =
            {new NamedCell(COLUMN_INT, new IntCell(0)), new NamedCell(COLUMN_STRING, new StringCell("Test")), //
                new NamedCell(COLUMN_INT, new IntCell(5)), //
                new NamedCell(COLUMN_LONG, new LongCell(0)), //
                new NamedCell(COLUMN_DOUBLE, new DoubleCell(5.4)), //
                new NamedCell(COLUMN_BOOLEAN, BooleanCellFactory.create(true))}; //
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
    /**
     * A simple node that provides a table to its output port. The table is provided by a supplier that is passed to the
     * constructor.
     */
    public static class InputDataNodeModel extends NodeModel {
        Supplier<BufferedDataTable> m_tableSupplier;
        /**
         * Creates a new InputDataNodeModel that will provide the table from the given supplier.
         *
         * @param tableSupplier the supplier that will provide the table.
         */
        public InputDataNodeModel(final Supplier<BufferedDataTable> tableSupplier) {
            super(0, 1);
            m_tableSupplier = tableSupplier;
        }
        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
            var table = m_tableSupplier.get();
            if (table == null) {
                throw new IllegalStateException("Table supplier returned null");
            }
            return new DataTableSpec[]{m_tableSupplier.get().getSpec()};
        }
        @Override
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) {
            var table = m_tableSupplier.get();
            if (table == null) {
                throw new IllegalStateException("Table supplier returned null");
            }
            return new BufferedDataTable[]{m_tableSupplier.get()};
        }
        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // does nothing
        }
        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // does nothing
        }
        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
            // does nothing
        }
        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            // does nothing
        }
        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            // does nothing
        }
        @Override
        protected void reset() {
            // does nothing
        }
    }
    /**
     * A simple factory to create the node with the model {@link InputDataNodeModel}. You might pass this to
     * {@link WorkflowManagerUtil#createAndAddNode(WorkflowManager, NodeFactory)} to add a general input node to a test
     * workflow.
     */
    public static class InputDataNodeFactory extends NodeFactory<InputDataNodeModel> {
        private final Supplier<BufferedDataTable> m_tableSupplier;
        /**
         * Creates a new factory that will create a node with the given table supplier.
         *
         * @param tableSupplier the supplier that will provide the table.
         */
        public InputDataNodeFactory(final Supplier<BufferedDataTable> tableSupplier) {
            m_tableSupplier = tableSupplier;
        }
        @Override
        public InputDataNodeModel createNodeModel() {
            return new InputDataNodeModel(m_tableSupplier);
        }
        @Override
        protected int getNrNodeViews() {
            return 0;
        }
        @Override
        public NodeView<InputDataNodeModel> createNodeView(final int viewIndex, final InputDataNodeModel nodeModel) {
            return null;
        }
        @Override
        protected boolean hasDialog() {
            return false;
        }
        @Override
        protected NodeDialogPane createNodeDialogPane() {
            return null;
        }
    }
}