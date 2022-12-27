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
 *   Dec 28, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.table.updater;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.table.updater.TableUpdaterNodeSettings.UnmatchedHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinTableSettings.JoinColumn;
import org.knime.core.data.join.JoinTableSettings.SpecialJoinColumn;
import org.knime.core.data.join.implementation.JoinerFactory.JoinAlgorithm;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Performs table updates.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableUpdater {

    private static final String ORDER_COL_NAME = "order";

    private static final String MATCH_DETECTION_COL_NAME = "match";

    private final ColumnMatching m_columnMatching;

    private final DataTableSpec m_inputSpec;

    private final DataTableSpec m_updateSpec;

    private final TableUpdaterNodeSettings m_settings;

    private final ColumnMatching m_canonicalizedMatching;

    private final ColumnNameCanonicalizer m_canonicalizer;

    TableUpdater(final DataTableSpec inputSpec, final DataTableSpec updateSpec, final TableUpdaterNodeSettings settings)
        throws InvalidSettingsException {
        m_columnMatching = ColumnMatching.matchSpecs(inputSpec, updateSpec);
        if (settings.m_unmatchedColumnsHandling == UnmatchedHandling.FAIL) {
            CheckUtils.checkSetting(m_columnMatching.getUnmatchedUpdateSpec().getNumColumns() == 0,
                "The columns %s present in the update table are missing from the input table.",
                Arrays.toString(m_columnMatching.getUnmatchedUpdateSpec().getColumnNames()));
        }
        m_inputSpec = inputSpec;
        m_updateSpec = updateSpec;
        m_settings = settings;
        m_canonicalizer = new ColumnNameCanonicalizer(inputSpec, updateSpec);
        m_canonicalizedMatching = m_canonicalizer.canonicalizeMatching(m_columnMatching);
    }

    /**
     * The domain might not be correct if unmatched rows are ignored because unmatched rows might contain values that
     * then are not in the output table.
     *
     * @return the updated spec (might not have the correct domain)
     */
    DataTableSpec getUpdatedSpec() {
        var updatedSpec = expandDomainsFromSpec(m_inputSpec);

        if (m_settings.m_unmatchedColumnsHandling == UnmatchedHandling.APPEND) {
            var specUpdater = new DataTableSpecCreator(updatedSpec);
            specUpdater.addColumns(m_columnMatching.getUnmatchedUpdateSpec());
            updatedSpec = specUpdater.createSpec();
        }
        // TODO should table properties also be updated

        return updatedSpec;
    }

    private DataTableSpec expandDomainsFromSpec(final DataTableSpec spec) {
        var specUpdater = new DataTableSpecCreator(spec);
        for (var col : m_columnMatching.getIntersection()) {
            int posInputSpec = m_inputSpec.findColumnIndex(col.getName());
            var inputColSpec = m_inputSpec.getColumnSpec(posInputSpec);
            var colCreator = new DataColumnSpecCreator(inputColSpec);
            // TODO discuss if this is too restrictive, as it also requires color and size handlers to match
            colCreator.merge(m_updateSpec.getColumnSpec(col.getName()));
            specUpdater.replaceColumn(posInputSpec, colCreator.createSpec());
        }
        return specUpdater.createSpec();
    }

    BufferedDataTable update(final BufferedDataTable inputTable, final BufferedDataTable updateTable,
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        CheckUtils.checkArgument(m_inputSpec.equals(inputTable.getDataTableSpec()),
            "The inputTable has a different spec than the spec provided during construction.");
        CheckUtils.checkArgument(m_updateSpec.equals(updateTable.getDataTableSpec()),
            "The updateTable has a different spec than the spec provided during construction.");

        var canonicalizedTable = m_canonicalizer.canonicalizeNames(inputTable, exec);
        var canonicalizedUpdateTable = m_canonicalizer.canonicalizeNames(updateTable, exec);

        exec.setMessage("Append order column");
        var tableWithOrder = appendOrderColumn(canonicalizedTable, exec.createSubExecutionContext(0.05));
        exec.setMessage("Append match detection column");
        var updateTableWithMatchDetection =
            appendMatchDetectionColumn(canonicalizedUpdateTable, exec.createSubExecutionContext(0.05));
        exec.setMessage("Join tables");
        var joinedTable = join(exec.createSubExecutionContext(0.75), tableWithOrder, updateTableWithMatchDetection);
        exec.setMessage("Merge columns");
        var mergedTable = mergeColumns(joinedTable, exec.createSubExecutionContext(0.05));
        exec.setMessage("Restore order");
        var orderedTable = restoreOrder(mergedTable, exec.createSubExecutionContext(0.1));
        var restoredTable = m_canonicalizer.restoreNames(orderedTable, exec);
        return expandDomains(restoredTable, exec);
    }

    private static BufferedDataTable appendOrderColumn(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        var spec = table.getDataTableSpec();
        var rearranger = new ColumnRearranger(spec);
        rearranger.append(new OrderFactory(ORDER_COL_NAME));
        return exec.createColumnRearrangeTable(table, rearranger, exec);
    }

    private static BufferedDataTable appendMatchDetectionColumn(final BufferedDataTable updateTable,
        final ExecutionContext exec) throws CanceledExecutionException {
        var rearranger = new ColumnRearranger(updateTable.getDataTableSpec());
        rearranger.append(new MatchDetectionCellFactory());
        return exec.createColumnRearrangeTable(updateTable, rearranger, exec);
    }

    private BufferedDataTable join(final ExecutionContext exec, final BufferedDataTable table,
        final BufferedDataTable updateTable) throws InvalidSettingsException, CanceledExecutionException {
        var inputSpec = table.getDataTableSpec();
        var joinOnRowID = new JoinColumn[]{new JoinColumn(SpecialJoinColumn.ROW_KEY)};
        var inputJoinTableSettings =
            new JoinTableSettings(true, joinOnRowID, inputSpec.getColumnNames(), InputTable.LEFT, table);

        var updateSpec = updateTable.getDataTableSpec();

        var updateJoinTableSettings =
            new JoinTableSettings(m_settings.m_unmatchedRowsHandling != UnmatchedHandling.IGNORE, joinOnRowID,
                getColumnsToIncludeFromUpdateTable(updateSpec).toArray(String[]::new), InputTable.RIGHT, updateTable);

        var joinSpec = new JoinSpecification.Builder(inputJoinTableSettings, updateJoinTableSettings)//
            .columnNameDisambiguator(TableUpdater::toUpdateColName)//
            .outputRowOrder(OutputRowOrder.LEFT_RIGHT)// append unmatched rows of update table (if not ignored)
            .retainMatched(true)//
            .rowKeyFactory((l, r) -> l != null ? l.getKey() : r.getKey()) // safe because we join on the row key
            .build();

        var joinImpl = JoinAlgorithm.AUTO.getFactory().create(joinSpec, exec);
        var result = joinImpl.joinOutputCombined().getResults().getTable();
        if (m_settings.m_unmatchedRowsHandling == UnmatchedHandling.FAIL) {
            CheckUtils.checkArgument(result.size() == table.size(),
                "Some rows of the update table have Row IDs that do not occur in the input table");
        }
        return result;
    }

    private Collection<String> getColumnsToIncludeFromUpdateTable(final DataTableSpec updateSpec) {
        if (m_settings.m_unmatchedColumnsHandling == UnmatchedHandling.APPEND) {
            return updateSpec.stream()//
                .map(DataColumnSpec::getName)//
                .collect(toList());
        } else {
            var colsToIncludeFromUpdateTable = m_canonicalizedMatching//
                .getIntersection()//
                .stream()//
                .map(DataColumnSpec::getName)//
                .collect(toCollection(ArrayList::new));
            colsToIncludeFromUpdateTable.add(MATCH_DETECTION_COL_NAME);
            return colsToIncludeFromUpdateTable;
        }
    }

    private static String toUpdateColName(final String dataColName) {
        return dataColName + "_";
    }

    private BufferedDataTable mergeColumns(final BufferedDataTable joinedTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        var spec = joinedTable.getDataTableSpec();
        var rearranger = new ColumnRearranger(spec);
        var updateColumns = m_canonicalizedMatching.getIntersection().stream()//
            .map(DataColumnSpec::getName)//
            .map(TableUpdater::toUpdateColName)//
            .toArray(String[]::new);
        rearranger.remove(updateColumns);
        final int matchDetectionColIndex = spec.findColumnIndex(MATCH_DETECTION_COL_NAME);
        var mergers = m_canonicalizedMatching.getIntersection().stream()//
            .map(DataColumnSpec::getName)//
            .map(n -> new ColumnMerger(spec.findColumnIndex(n), spec.findColumnIndex(toUpdateColName(n)),
                matchDetectionColIndex, m_settings.m_performUpdateWithMissingValues, spec.getColumnSpec(n)))//
            .collect(Collectors.toList());
        mergers.forEach(m -> rearranger.replace(m, m.m_dataColIndex));
        rearranger.remove(MATCH_DETECTION_COL_NAME);
        return exec.createColumnRearrangeTable(joinedTable, rearranger, exec);
    }

    private static BufferedDataTable restoreOrder(final BufferedDataTable joinedTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        // the new ArrayList stunt is necessary because BufferedDataTableSorter checks if the list contains null which
        // is not supported by the immutable list returned by List.of
        var sorter = new BufferedDataTableSorter(joinedTable, new ArrayList<>(List.of(ORDER_COL_NAME)),
            new boolean[]{true}, true);
        var sorted = sorter.sort(exec);
        var rearranger = new ColumnRearranger(sorted.getDataTableSpec());
        rearranger.remove(ORDER_COL_NAME);
        return exec.createColumnRearrangeTable(sorted, rearranger, exec.createSilentSubExecutionContext(0));
    }

    private BufferedDataTable expandDomains(final BufferedDataTable table, final ExecutionContext exec) {
        var updatedTable = exec.createSpecReplacerTable(table, expandDomainsFromSpec(table.getDataTableSpec()));
        exec.setProgress(1.0);
        return updatedTable;
    }

    private static final class OrderFactory extends SingleCellFactory {

        private long m_counter;

        public OrderFactory(final String name) {
            super(false, new DataColumnSpecCreator(name, LongCell.TYPE).createSpec());
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return new LongCell(m_counter++);//NOSONAR
        }

    }

    private static final class MatchDetectionCellFactory extends SingleCellFactory {
        MatchDetectionCellFactory() {
            super(new DataColumnSpecCreator(MATCH_DETECTION_COL_NAME, BooleanCell.TYPE).createSpec());
        }

        @Override
        public DataCell getCell(final DataRow row) {
            return BooleanCell.TRUE;
        }
    }

    private static final class ColumnMerger extends SingleCellFactory {

        private final int m_dataColIndex;

        private final int m_updateColIndex;

        private final int m_matchDetectionColIndex;

        private final boolean m_updateIfMissing;

        ColumnMerger(final int dataColIndex, final int updateColIndex, final int matchDetectionColumn,
            final boolean updateIfMissing, final DataColumnSpec spec) {
            super(spec);
            m_dataColIndex = dataColIndex;
            m_updateColIndex = updateColIndex;
            m_matchDetectionColIndex = matchDetectionColumn;
            m_updateIfMissing = updateIfMissing;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            if (row.getCell(m_matchDetectionColIndex).isMissing()) {
                // no match -> no update
                return row.getCell(m_dataColIndex);
            } else {
                // match -> update
                final var updateCell = row.getCell(m_updateColIndex);
                if (m_updateIfMissing) {
                    return updateCell;
                } else {
                    return updateCell.isMissing() ? row.getCell(m_dataColIndex) : updateCell;
                }
            }
        }

    }

    private static final class ColumnNameCanonicalizer {

        private final Map<String, String> m_oldToNewName = new HashMap<>();

        private final List<String> m_newToOldName = new ArrayList<>();

        ColumnNameCanonicalizer(final DataTableSpec inputSpec, final DataTableSpec updateSpec) {
            for (var tableSpec : List.of(inputSpec, updateSpec)) {
                for (var col : tableSpec) {
                    String name = col.getName();
                    m_oldToNewName.computeIfAbsent(name, n -> {
                        var newName = Integer.toString(m_newToOldName.size());
                        m_newToOldName.add(n);
                        return newName;
                    });
                }
            }
        }

        BufferedDataTable canonicalizeNames(final BufferedDataTable table, final ExecutionContext exec) {
            return exec.createSpecReplacerTable(table, canonicalizeNames(table.getDataTableSpec()));
        }

        BufferedDataTable restoreNames(final BufferedDataTable table, final ExecutionContext exec) {
            return exec.createSpecReplacerTable(table, restoreNames(table.getDataTableSpec()));
        }

        ColumnMatching canonicalizeMatching(final ColumnMatching matching) {
            return new ColumnMatching(canonicalizeNames(matching.getIntersection()),
                canonicalizeNames(matching.m_unmatchedUpdates));
        }

        private static DataTableSpec transformNames(final DataTableSpec spec,
            final UnaryOperator<String> nameTransformer) {
            var tableSpecCreator = new DataTableSpecCreator(spec);
            for (int i = 0; i < spec.getNumColumns(); i++) {// NOSONAR
                var columnSpec = spec.getColumnSpec(i);
                var colSpecCreator = new DataColumnSpecCreator(columnSpec);
                colSpecCreator.setName(nameTransformer.apply(columnSpec.getName()));
                tableSpecCreator.replaceColumn(i, colSpecCreator.createSpec());
            }
            return tableSpecCreator.createSpec();
        }

        private DataTableSpec canonicalizeNames(final DataTableSpec spec) {
            return transformNames(spec, m_oldToNewName::get);
        }

        private DataTableSpec restoreNames(final DataTableSpec canonicalizedSpec) {
            return transformNames(canonicalizedSpec, this::restoreIfCanonicalized);
        }

        private String restoreIfCanonicalized(final String name) {
            try {
                var parsedName = Integer.parseInt(name);
                return m_newToOldName.get(parsedName);
            } catch (NumberFormatException ex) {//NOSONAR
                // it's perfectly viable for a spec to not be an integer
                // this just means that it wasn't renamed by the renamer
                // and we keep the name it currently has
                return name;
            }
        }

    }

    private static final class ColumnMatching {
        private final DataTableSpec m_intersection;

        private final DataTableSpec m_unmatchedUpdates;

        static ColumnMatching matchSpecs(final DataTableSpec inputSpec, final DataTableSpec updateSpec)
            throws InvalidSettingsException {
            var intersection = new ColumnRearranger(inputSpec);
            var unmatchedUpdates = new ColumnRearranger(updateSpec);
            for (var col : inputSpec) {
                var name = col.getName();
                var updateCol = updateSpec.getColumnSpec(name);
                if (updateCol == null) {
                    intersection.remove(name);
                } else {
                    unmatchedUpdates.remove(name);
                    CheckUtils.checkSetting(col.getType().isASuperTypeOf(updateCol.getType()),
                        "The type of the column '%s' is incompatible between the input and the update table.", name);
                }
            }
            return new ColumnMatching(intersection.createSpec(), unmatchedUpdates.createSpec());
        }

        ColumnMatching(final DataTableSpec intersection, final DataTableSpec unmatchedUpdates) {
            m_intersection = intersection;
            m_unmatchedUpdates = unmatchedUpdates;
        }

        DataTableSpec getIntersection() {
            return m_intersection;
        }

        DataTableSpec getUnmatchedUpdateSpec() {
            return m_unmatchedUpdates;
        }
    }
}
