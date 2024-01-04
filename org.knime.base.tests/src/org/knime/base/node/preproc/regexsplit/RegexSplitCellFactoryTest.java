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
 *   8 Jan 2024 (carlwitt): created
 */
package org.knime.base.node.preproc.regexsplit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputMode;
import org.knime.base.node.preproc.regexsplit.OutputSettings.SingleOutputColumnMode;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.core.ExecutionContextExtension;

/**
 * Test split with various settings.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@ExtendWith(ExecutionContextExtension.class)
class RegexSplitCellFactoryTest {

    /**
     * Creates a single column (named "value") table with a single string cell (input) and runs the node on it.
     */
    private static BufferedDataTable execute(final String input, final RegexSplitNodeSettings settings,
        final ExecutionContext exec) throws Exception {

        final var spec = new DataTableSpecCreator()
            .addColumns(new DataColumnSpecCreator("value", StringCell.TYPE).createSpec()).createSpec();

        // input table
        final var container = exec.createDataContainer(spec);
        container.addRowToTable(new DefaultRow(RowKey.createRowKey(0l), input));
        container.close();
        final var table = container.getTable();

        // run
        RegexSplitNodeModel nodeModel = new RegexSplitNodeFactory().createNodeModel();
        nodeModel.configure(new DataTableSpec[]{spec}, settings);
        return nodeModel.execute(new BufferedDataTable[]{table}, exec, settings)[0];
    }

    /**
     * @param pattern defines the splitting
     * @param input data
     * @param output groups
     * @param exec injected from {@link ExecutionContextExtension}
     */
    @ArgumentsSource(TestData.class)
    @ParameterizedTest
    void testCellFactoryRowOutput(final String pattern, final String input, final List<String> output,
        final ExecutionContext exec) throws Exception {

        final var settings = new RegexSplitNodeSettings();
        settings.m_column = "value";
        settings.m_pattern = pattern;
        settings.m_output.m_mode = OutputMode.ROWS;
        settings.m_output.m_singleOutputColumnMode = SingleOutputColumnMode.REPLACE;

        final var outputTable = execute(input, settings, exec);

        // result groups are stored in one row each
        try (final var iterator = outputTable.iterator()) {
            for (var expected : output) {
                var outputRow = iterator.next();
                assertThat(((StringCell)outputRow.getCell(0)).getStringValue()).as("row contents").isEqualTo(expected);
            }
            assertThat(iterator.hasNext()).as("no more rows").isFalse();
        }
    }


    public static class TestData implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
            return Stream.of(Arguments.of("(?<first>\\w+)\\s+(?<last>\\w+)", "carl   witt", List.of("carl", "witt")),
                Arguments.of("(\\d+).*", "123abc", List.of("123")),
                // named capture group with reluctant quantifier *? and named back reference
                Arguments.of("(?<digits>\\d+).*?\\k<digits>", "123123as11123", List.of("123")));
        }
    }

}
