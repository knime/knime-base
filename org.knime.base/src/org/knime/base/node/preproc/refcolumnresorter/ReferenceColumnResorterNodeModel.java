/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   15.10.2015 (ferry.abt): created
 */
package org.knime.base.node.preproc.refcolumnresorter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;

/**
 * The node model of the reference column resorter node, re sorting columns based on order defined by second input.
 *
 * @author Ferry Abt, KNIME AG, Konstanz, Germany
 */
final class ReferenceColumnResorterNodeModel extends NodeModel {

    static final int ORDER_INPORT = 1;

    static final String CFGKEY_STRATEGY = "Strategy";

    static final String CFGKEY_ORDERCOL = "OrderCol";

    static final String DEFAULT_STRATEGY = "Last";

    static final String[] DEFAULT_ORDER = new String[]{};

    static final String DEFAULT_ORDERCOL = null;

    private final SettingsModelString m_strategy = new SettingsModelString(CFGKEY_STRATEGY, DEFAULT_STRATEGY);

    private final SettingsModelString m_orderCol = new SettingsModelString(CFGKEY_ORDERCOL, DEFAULT_ORDERCOL);

    static final String[] AVAILABLE_STRATEGIES = new String[]{"Last", "First", "Drop"};

    /**
     * Creates new instance of <code>ReferenceColumnResorterNodeModel</code>.
     */
    ReferenceColumnResorterNodeModel() {
        super(2, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.checkSetting(
            Arrays.stream(AVAILABLE_STRATEGIES).anyMatch(s -> s.equals(m_strategy.getStringValue())),
            "Unknown strategy: " + m_strategy.getStringValue());
        final String orderColName = m_orderCol.getStringValue();
        final DataColumnSpec orderCol = inSpecs[1].getColumnSpec(orderColName);
        CheckUtils.checkSettingNotNull(orderCol, "Order column \"%s\" not present in 2nd input table", orderColName);
        CheckUtils.checkSetting(orderCol.getType().isCompatible(StringValue.class),
            "Order column \"%s\" " + "not string compatible (type \"%s\"), i.e. does not contain column names",
            orderColName, orderCol.getType().getName());
        return new DataTableSpec[1];
    }

    /**
     * {@inheritDoc}
     *
     * @throws CanceledExecutionException
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws CanceledExecutionException {
        final String[] order = readOrderFromTable(inData[1]);
        final ColumnRearranger rearranger = createColumnRearranger(inData[0].getDataTableSpec(), order);
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(inData[0], rearranger, exec)};
    }

    /**
     * @param orderTable
     * @return
     */
    private String[] readOrderFromTable(final BufferedDataTable orderTable) {
        final int orderCol = orderTable.getSpec().findColumnIndex(m_orderCol.getStringValue());

        //get order from input[1]
        final List<String> orderList = new ArrayList<>();
        final Set<String> duplicateCheck = new HashSet<>();
        try (CloseableRowIterator rows = orderTable.iterator()) {
            while (rows.hasNext()) {
                final String colName = rows.next().getCell(orderCol).toString();
                if (!duplicateCheck.add(colName)) {
                    throw new IllegalArgumentException("Duplicate column names in argument array: " + colName);
                }
                orderList.add(colName);
            }
        }
        return orderList.toArray(new String[orderList.size()]);
    }

    /**
     * Creates and returns an instance of the column rearranger which re sorts the input columns in a user specified
     * way.
     *
     * @param original The data table spec of the original input table.
     * @param order Ordered data array - retrieved from first input
     * @return The rearranger to resort the columns.
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec original, final String[] order) {
        ColumnRearranger rearranger = new ColumnRearranger(original);
        final String[] newColOder = getNewOrder(Arrays.asList(original.getColumnNames()), Arrays.asList(order));
        rearranger.permute(newColOder);
        if (m_strategy.getStringValue().equals("Drop")) {
            rearranger.keepOnly(newColOder);
        }
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    /** {@inheritDoc} */
    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        String[] order = readOrderFromStreamableOperatorInternals(internals);
        return order == null;
    }

    /**
     * @param internals
     * @return
     */
    private static String[] readOrderFromStreamableOperatorInternals(final StreamableOperatorInternals internals) {
        SimpleStreamableOperatorInternals i = (SimpleStreamableOperatorInternals)internals;
        String[] order = i.getConfig().getStringArray("order", (String[])null);
        return order;
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new SimpleStreamableOperatorInternals();
    }

    /** {@inheritDoc} */
    @Override
    public MergeOperator createMergeOperator() {
        return new MergeOperator() {

            /** {@inheritDoc} */
            @Override
            public StreamableOperatorInternals mergeIntermediate(final StreamableOperatorInternals[] operators) {
                return mergeFinal(operators);
            }

            @Override
            public StreamableOperatorInternals mergeFinal(final StreamableOperatorInternals[] operators) {
                StreamableOperatorInternals result = operators[0];
                String[] refOrder = readOrderFromStreamableOperatorInternals(result);
                for (int i = 1; i < operators.length; i++) {
                    final String[] curOrder = readOrderFromStreamableOperatorInternals(operators[i]);
                    if (!Arrays.equals(refOrder, curOrder)) {
                        StringBuilder b = new StringBuilder();
                        b.append("Inconsistent internals -- all workers should see same dictionary table; ");
                        b.append("index ").append(i).append("is different to position 0: ");
                        b.append(refOrder).append(" vs. ").append(curOrder);
                    }
                }
                return result;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{
            createColumnRearranger((DataTableSpec)inSpecs[0], readOrderFromStreamableOperatorInternals(internals))
                .createSpec()};
    }

    /** {@inheritDoc} */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            private String[] m_streamableOperatorOrder;

            /** {@inheritDoc} */
            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                BufferedDataTable orderTable = (BufferedDataTable)((PortObjectInput)inputs[1]).getPortObject();
                m_streamableOperatorOrder = readOrderFromTable(orderTable);
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                RowInput dataInput = (RowInput)inputs[0];
                DataTableSpec dataInputSpec = dataInput.getDataTableSpec();
                ColumnRearranger rearranger = createColumnRearranger(dataInputSpec, m_streamableOperatorOrder);
                StreamableFunction streamableFunction = rearranger.createStreamableFunction();
                streamableFunction.runFinal(new PortInput[]{dataInput}, outputs, exec);
            }

            /** {@inheritDoc} */
            @Override
            public StreamableOperatorInternals saveInternals() {
                final SimpleStreamableOperatorInternals i = new SimpleStreamableOperatorInternals();
                i.getConfig().addStringArray("order", m_streamableOperatorOrder);
                return i;
            }

            /** {@inheritDoc} */
            @Override
            public void loadInternals(final StreamableOperatorInternals internals) {
                m_streamableOperatorOrder = readOrderFromStreamableOperatorInternals(internals);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public void finishStreamableExecution(final StreamableOperatorInternals internals, final ExecutionContext exec,
        final PortOutput[] output) throws Exception {
        assert output[0] == null : "Output should be computed distributed";
    }

    /**
     * Creates and returns arrays of column names in new order, based on the dialog settings.
     *
     * @param input the columns to be sorted
     * @param order ordered col names
     * @return Array of column names in new order.
     */
    private String[] getNewOrder(final List<String> input, final List<String> order) {

        final List<String> newOrder = new ArrayList<>(input);

        final boolean appendNew;
        switch (m_strategy.getStringValue()) {
            case "First":
                appendNew = false;
                break;
            case "Last":
                appendNew = true;
                break;
            case "Drop":
                appendNew = false;
                newOrder.retainAll(order);
                break;
            default:
                throw new IllegalStateException("Unknown strategy");
        }

        newOrder.sort(new Comparator<String>() {

            @Override
            public int compare(final String col1, final String col2) {
                if (order.contains(col1) && order.contains(col2)) {
                    //the order of the two columns is defined by the reference table
                    return Integer.compare(order.indexOf(col1), order.indexOf(col2));
                } else if (order.contains(col1)) {
                    //col2 is unknown, but col1 has a place according to reference
                    if (appendNew) {
                        //unknown columns shall be appended -> col1 definitely comes before col2
                        return -1;
                    } else {
                        //unknown columns shall be prepended -> col1 definitely comes after col2
                        return 1;
                    }
                } else if (order.contains(col2)) {
                    //col1 is unknown but col2 has a place according to reference
                    if (appendNew) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    //both columns are unknown, order them according to the input (i.e. leave order as is)
                    return Integer.compare(input.indexOf(col1), input.indexOf(col2));
                }
            }
        });

        return newOrder.toArray(new String[newOrder.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_strategy.saveSettingsTo(settings);
        m_orderCol.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_strategy.validateSettings(settings);
        m_orderCol.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_strategy.loadSettingsFrom(settings);
        m_orderCol.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do ...
    }
}
