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
 *   Jun 20, 2025 (david): created
 */
package org.knime.base.node.util.binning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.knime.base.node.util.InputTableNode;
import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.testing.util.TableTestUtil;

/**
 *
 * @author david
 */
final class AutoBinningUtilsTest {

    ExecutionContext m_exec;

    @Test
    void testExtractAndSortSingleColumn() throws CanceledExecutionException {
        var data = createDataTable( //
            new Column("col1", new double[]{3.0, 2.0, 1.0, 4.0, 5.0}), //
            new Column("col2", new double[]{10.0, 20.0, 30.0, 40.0, 50.0}) //
        );
        var exec = createExecutionContext(data);

        var sortedSingleColumn = AutoBinningUtils.AutoBinner.extractAndSortSingleColumn(data.get(), exec, "col2");

        var extractedData = StreamSupport.stream(sortedSingleColumn.spliterator(), false) //
            .map(row -> row.getCell(0)) //
            .map(DoubleValue.class::cast) //
            .mapToDouble(DoubleValue::getDoubleValue) //;
            .toArray();

        // assert that the data is sorted and contains same values as col2
        var expectedData = new double[]{10.0, 20.0, 30.0, 40.0, 50.0};

        assertEquals(expectedData.length, extractedData.length, "Data length mismatch");
        for (int i = 0; i < expectedData.length; i++) {
            assertEquals(expectedData[i], extractedData[i], "Data value mismatch at index " + i);
        }
    }

    @Nested
    class TestsForEdgeCreation {

        @Nested
        class TestsForEqualWidthEdgeCreation {

            @Test
            void testNoBoundsAndNoIntegerForcing() {
                var data = createDataTable( //
                    new Column("col1", new double[]{1, 2, 3, 4}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualWidth( //
                    data.get().getDataTableSpec().getColumnSpec(0), //
                    OptionalDouble.empty(), //
                    OptionalDouble.empty(), //
                    false, //
                    2 //
                );

                assertEquals(3, edges.size(), "Expected 3 edges for equal width binning with no bounds");
                assertEquals(List.of(1.0, 2.5, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }

            @Test
            void testBoundsAndNoIntegerForcing() {
                var data = createDataTable( //
                    new Column("col1", new double[]{0, 1, 2, 3, 4, 5}) //
                );

                var edges = AutoBinningUtils.createEdgesForEqualWidth( //
                    data.get().getDataTableSpec().getColumnSpec(0), //
                    OptionalDouble.of(1.0), // lower bound
                    OptionalDouble.of(4.0), // upper bound
                    false, // no integer forcing
                    2 // number of bins
                );

                assertEquals(3, edges.size(), "Expected 3 edges for equal width binning with bounds");
                assertEquals(List.of(1.0, 2.5, 4.0),
                    edges.stream().map(AutoBinningSettings.BinBoundary::value).toList(), "Edges should be correct");

                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(0).exactMatchBehaviour(),
                    "First boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, edges.get(1).exactMatchBehaviour(),
                    "Second boundary should match to upper bin");
                assertEquals(BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, edges.get(2).exactMatchBehaviour(),
                    "Last boundary should match to lower bin");
            }
        }

        @Test
        void testCreateEdgesForEqualFrequency() {

        }

        @Test
        void testCreateEdgesForCustomQuantiles() {

        }
    }

    static Supplier<BufferedDataTable> createDataTable(final Column... columns) {
        // first make sure all columns have the same length
        if (1 != Arrays.stream(columns).mapToInt(a -> a.values.length).distinct().count()) {
            throw new IllegalArgumentException("All columns must have the same length");
        }

        var specBuilder = new TableTestUtil.SpecBuilder();
        for (Column column : columns) {
            specBuilder.addColumn(column.name, DoubleCellFactory.TYPE);
        }
        var tableBuilder = new TableTestUtil.TableBuilder(specBuilder.build());

        for (int i = 0; i < columns[0].values.length; i++) {
            var rowData = new Object[columns.length];

            for (int j = 0; j < columns.length; j++) {
                rowData[j] = new DoubleCell(columns[j].values[i]);
            }

            tableBuilder.addRow(rowData);
        }

        var table = tableBuilder.build().get();

        return () -> table;
    }

    record Column(String name, double[] values) {
    }

    ExecutionContext createExecutionContext(final Supplier<BufferedDataTable> table) {
        var monitor = new DefaultNodeProgressMonitor();
        NodeFactory<NodeModel> factory = (NodeFactory)new InputTableNode.InputDataNodeFactory(table);
        var node = new Node(factory);
        var memoryPolicy = SingleNodeContainer.MemoryPolicy.CacheInMemory;
        var thing = NotInWorkflowDataRepository.newInstance();

        return new ExecutionContext(monitor, node, memoryPolicy, thing);
    }
}
