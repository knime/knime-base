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
 *   14.10.2015 (Adrian Nembach): created
 */

package org.knime.base.node.preproc.rank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.UniqueNameGenerator;

/**
 * This is the model implementation of Rank. This node ranks the input data based on the selected ranking field and
 * ranking mode
 *
 * @author Adrian Nembach, KNIME GmbH Konstanz
 */
public class RankNodeModel extends NodeModel {

    enum RankMode {
        STANDARD("Standard", StandardRankAssigner::new),
        DENSE("Dense", DenseRankAssigner::new),
        ORDINAL("Ordinal", i -> new OrdinalRankAssigner());

        private final String m_string;

        private final Function<int[], RankAssigner> m_rankAssignerFactory;

        RankMode(final String str, final Function<int[], RankAssigner> rankAssignerFactory) {
            m_string = str;
            m_rankAssignerFactory = rankAssignerFactory;
        }

        RankAssigner createRankAssigner(final int[] rankColumnIndices) {
            return m_rankAssignerFactory.apply(rankColumnIndices);
        }

        @Override
        public String toString() {
            return m_string;
        }

        static RankMode fromString(final String string) throws InvalidSettingsException {
            return Stream.of(values())//
                .filter(m -> m.m_string.equals(string))//
                .findFirst()//
                .orElseThrow(
                    () -> new InvalidSettingsException(String.format("Unknown RankMode '%s' encountered.", string)));
        }
    }

    /** initial default values. */
    static final RankMode DEFAULT_RANKMODE = RankMode.STANDARD;

    static final String DEFAULT_RANKOUTCOLNAME = "rank";

    static final boolean DEFAULT_RETAINROWORDER = false;

    static final boolean DEFAULT_RANKASLONG = false;

    // SettingsModels
    private final SettingsModelStringArray m_rankColumns = createRankColumnsModel();

    private final SettingsModelStringArray m_groupColumns = createGroupColumnsModel();

    private final SettingsModelString m_rankMode = createRankModeModel();

    private final SettingsModelStringArray m_rankOrder = createRankOrderModel();

    private final SettingsModelString m_rankOutColName = createRankOutColNameModel();

    private final SettingsModelBoolean m_retainRowOrder = createRetainRowOrderModel();

    private final SettingsModelBoolean m_rankAsLong = createRankAsLongModel();

    // static initiators for SettingsModels
    static SettingsModelStringArray createRankColumnsModel() {
        return new SettingsModelStringArray("RankingColumns", new String[]{});
    }

    static SettingsModelStringArray createGroupColumnsModel() {
        return new SettingsModelStringArray("GroupColumns", new String[]{});
    }

    static SettingsModelString createRankModeModel() {
        return new SettingsModelString("RankMode", DEFAULT_RANKMODE.toString());
    }

    static SettingsModelString createRankOutColNameModel() {
        return new SettingsModelString("RankOutFieldName", DEFAULT_RANKOUTCOLNAME);
    }

    static SettingsModelBoolean createRetainRowOrderModel() {
        return new SettingsModelBoolean("RetainRowOrder", DEFAULT_RETAINROWORDER);
    }

    static SettingsModelStringArray createRankOrderModel() {
        return new SettingsModelStringArray("RankOrder", new String[]{});
    }

    static SettingsModelBoolean createRankAsLongModel() {
        return new SettingsModelBoolean("RankAsLong", DEFAULT_RANKASLONG);
    }

    /**
     * Constructor for the node model.
     */
    protected RankNodeModel() {
        super(1, 1);
    }

    private static class OrderCellFactory extends SingleCellFactory {

        OrderCellFactory(final DataColumnSpec newColSpec) {
            super(newColSpec);
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            return new LongCell(rowIndex);
        }

    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        BufferedDataTable table = inData[0];
        if (table == null) {
            throw new IllegalArgumentException("No input table found.");
        }
        if (table.size() < 1) {
            setWarningMessage("The input table is empty.");
        }

        // calculate number of steps
        double numSteps = 2;
        if (m_retainRowOrder.getBooleanValue()) {
            numSteps += 2;
        }

        // insert extra column containing the original order of the input table
        String rowOrderColumn = null;
        if (m_retainRowOrder.getBooleanValue()) {
            rowOrderColumn = new UniqueNameGenerator(table.getDataTableSpec()).newName("rowOrder");
            table = appendOrderColumn(exec.createSubExecutionContext(1 / numSteps), table, rowOrderColumn);
        }

        var sortedTable = sortTable(exec.createSubExecutionContext(1 / numSteps), table);

        BufferedDataTable out = appendRank(exec.createSubExecutionContext(1 / numSteps), sortedTable);

        if (m_retainRowOrder.getBooleanValue()) {
            out = retainRowOrder(exec.createSubExecutionContext(1 / numSteps), rowOrderColumn, out);
        }

        return new BufferedDataTable[]{out};
    }

    /**
     * Only necessary because List.of(...) and Collectors.toList() don't support nulls but the table sorter calls
     * List.contains(null)
     */
    private static List<String> toList(final String... array) {
        return Stream.of(array).collect(Collectors.toCollection(() -> new ArrayList<>()));
    }

    private static BufferedDataTable retainRowOrder(final ExecutionContext exec, final String rowOrderColumn,
        BufferedDataTable out) throws CanceledExecutionException {
        // recover row order
        out = new BufferedDataTableSorter(out, toList(rowOrderColumn), new boolean[]{true})
            .sort(exec);
        // remove order column
        var cr = new ColumnRearranger(out.getDataTableSpec());
        cr.remove(rowOrderColumn);
        out = exec.createColumnRearrangeTable(out, cr, exec.createSilentSubExecutionContext(0));
        return out;
    }

    private BufferedDataTable appendRank(final ExecutionContext exec, final BufferedDataTable sortedTable)
        throws CanceledExecutionException, InvalidSettingsException {
        var spec = sortedTable.getDataTableSpec();
        var columnRearranger = new ColumnRearranger(spec);
        int[] groupColIndices = getIndicesFromColNameList(m_groupColumns.getStringArrayValue(), spec);
        int[] rankColIndices = getIndicesFromColNameList(m_rankColumns.getStringArrayValue(), spec);
        // append rank column
        columnRearranger.append(new RankCellFactory(createRankColSpec(), groupColIndices, rankColIndices,
            RankMode.fromString(m_rankMode.getStringValue()), m_rankAsLong.getBooleanValue()));
        return exec.createColumnRearrangeTable(sortedTable, columnRearranger, exec);
    }

    private BufferedDataTable sortTable(final ExecutionContext exec, final BufferedDataTable table)
        throws CanceledExecutionException {
        // set boolean array to indicate ascending ranking columns
        String[] orderRank = m_rankOrder.getStringArrayValue();
        var ascRank = new boolean[orderRank.length];
        for (int i = 0; i < ascRank.length; i++) {//NOSONAR
            ascRank[i] = orderRank[i].equals("Ascending");
        }
        return new BufferedDataTableSorter(table, toList(m_rankColumns.getStringArrayValue()), ascRank).sort(exec);
    }

    private static BufferedDataTable appendOrderColumn(final ExecutionContext exec, final BufferedDataTable table,
        final String rowOrderColumn) throws CanceledExecutionException {
        var cr = new ColumnRearranger(table.getDataTableSpec());
        DataColumnSpec rowOrderSpec = new DataColumnSpecCreator(rowOrderColumn, LongCell.TYPE).createSpec();
        var cellFac = new OrderCellFactory(rowOrderSpec);
        cr.append(cellFac);
        return exec.createColumnRearrangeTable(table, cr, exec);
    }

    private static int[] getIndicesFromColNameList(final String[] colNames, final DataTableSpec inSpec) {
        return Stream.of(colNames)//
                .mapToInt(inSpec::findColumnIndex)//
                .toArray();
    }

    @Override
    protected void reset() {
        // nothing to reset
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        String[] rankCols = m_rankColumns.getStringArrayValue();
        // check if at least one ranking column is selected
        if (rankCols.length == 0) {
            throw new InvalidSettingsException("No ranking column is selected.");
        }
        var tableSpec = inSpecs[0];
        checkAllContained(rankCols, "ranking", tableSpec);
        checkAllContained(m_groupColumns.getStringArrayValue(), "grouping", tableSpec);

        // check if a name for the column that will contain the ranks in the outputs is provided.
        if (m_rankOutColName.getStringValue().isEmpty()) {
            throw new InvalidSettingsException("There is no name for the output rank column provided.");
        }

        return new DataTableSpec[]{createOutSpec(tableSpec)};
    }

    private static void checkAllContained(final String[] columnNames, final String columnPurpose,
        final DataTableSpec tableSpec) throws InvalidSettingsException {
        for (String colName : columnNames) {
            CheckUtils.checkSetting(tableSpec.containsName(colName),
                "The selected %s column '%s' is not contained in the input table.", columnPurpose, colName);
        }
    }

    // create the DataTableSpec for the output table
    private DataTableSpec createOutSpec(final DataTableSpec inSpec) {
        return new DataTableSpec(inSpec, new DataTableSpec(createRankColSpec()));
    }

    private DataColumnSpec createRankColSpec() {
        var outputType = m_rankAsLong.getBooleanValue() ? LongCell.TYPE : IntCell.TYPE;
        return new DataColumnSpecCreator(m_rankOutColName.getStringValue(), outputType).createSpec();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        m_groupColumns.saveSettingsTo(settings);
        m_rankColumns.saveSettingsTo(settings);
        m_rankMode.saveSettingsTo(settings);
        m_rankOrder.saveSettingsTo(settings);
        m_rankOutColName.saveSettingsTo(settings);
        m_retainRowOrder.saveSettingsTo(settings);
        m_rankAsLong.saveSettingsTo(settings);

    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_groupColumns.loadSettingsFrom(settings);
        m_rankColumns.loadSettingsFrom(settings);
        m_rankMode.loadSettingsFrom(settings);
        m_rankOrder.loadSettingsFrom(settings);
        m_rankOutColName.loadSettingsFrom(settings);
        m_retainRowOrder.loadSettingsFrom(settings);
        m_rankAsLong.loadSettingsFrom(settings);

    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_groupColumns.validateSettings(settings);
        m_rankColumns.validateSettings(settings);
        m_rankMode.validateSettings(settings);
        m_rankOrder.validateSettings(settings);
        m_rankOutColName.validateSettings(settings);
        m_retainRowOrder.validateSettings(settings);
        m_rankAsLong.validateSettings(settings);
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do here
    }

}
