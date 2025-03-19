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
 *   Mar 19, 2025 (david): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.preproc.columnrenameregex.RegexRenameUtils.CaseSensitivity;
import org.knime.base.node.preproc.columnrenameregex.RegexRenameUtils.PatternType;
import org.knime.base.node.preproc.columnrenameregex.RegexRenameUtils.ReplacementStrategy;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.LongCell.LongCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.testing.util.TableTestUtil;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
final class ColumnRenameRegexNodeModel2Test {

    ColumnRenameRegexNodeSettings m_settings;

    ColumnRenameRegexNodeModel2 m_model;

    ExecutionContext m_fakeContext;

    final static DataTableSpec INPUT_TABLE_SPEC = new TableTestUtil.SpecBuilder() //
        .addColumn("column1", StringCellFactory.TYPE) //
        .addColumn("column2", LongCellFactory.TYPE) //
        .addColumn("column3", DoubleCellFactory.TYPE) //)
        .build();

    final static BufferedDataTable INPUT_TABLE = new TableTestUtil.TableBuilder(INPUT_TABLE_SPEC) //
        .addRow("a", 1L, 1.0) //
        .addRow("b", 2L, 2.0) //
        .addRow("c", 3L, 3.0) //
        .build().get();

    @BeforeEach
    void setup() throws IOException {
        m_settings = new ColumnRenameRegexNodeSettings();
        m_model = new ColumnRenameRegexNodeModel2(ColumnRenameRegexNodeFactory2.CONFIGURATION);

        @SuppressWarnings({"unchecked", "rawtypes"}) // annoying but necessary
        NodeFactory<NodeModel> factory = (NodeFactory)new ColumnRenameRegexNodeFactory2();
        m_fakeContext = new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(factory),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    @Test
    void testValidateFailsWithInvalidRegex() {
        m_settings.m_patternType = PatternType.REGEX;
        m_settings.m_pattern = "[";

        assertThrows(InvalidSettingsException.class, () -> m_model.validateSettings(m_settings));
    }

    @Test
    void testValidatePassesWithValidRegex() throws InvalidSettingsException {
        m_settings.m_patternType = PatternType.REGEX;
        m_settings.m_pattern = ".*";

        m_model.validateSettings(m_settings);
    }

    @Test
    void testWorksWithEmptyInSpec() throws Exception {
        var warningListener = new AtomicBoolean(false);

        var inSpec = new TableTestUtil.SpecBuilder().build();
        m_model.addWarningListener(w -> warningListener.set(true));
        var outSpec = m_model.configure(new DataTableSpec[]{inSpec}, m_settings);

        assertEquals(0, outSpec[0].getNumColumns(), "Expected no columns in output spec");
        assertFalse(warningListener.get(), "Expected no warning to be issued");
    }

    @Test
    void testWarnsWhenNoColumnsMatchPattern() throws Exception {
        m_settings.m_pattern = "xyz";

        var warningListener = new AtomicBoolean(false);
        m_model.addWarningListener(w -> warningListener.set(true));

        m_model.execute(new BufferedDataTable[]{INPUT_TABLE}, m_fakeContext, m_settings);

        assertTrue(warningListener.get(), "Expected warning to be issued");
    }

    @Test
    void testWarnsWhenOutputHasDuplicates() throws Exception {
        m_settings.m_pattern = ".*";
        m_settings.m_patternType = PatternType.REGEX;
        m_settings.m_replacement = "x";

        var warningListener = new AtomicBoolean(false);
        m_model.addWarningListener(w -> warningListener.set(true));

        m_model.execute(new BufferedDataTable[]{INPUT_TABLE}, m_fakeContext, m_settings);

        assertTrue(warningListener.get(), "Expected warning to be issued");
    }

    @Test
    void testErrorsWhenGroupInReplacementButNotInPattern() throws Exception {
        m_settings.m_pattern = ".*";
        m_settings.m_patternType = PatternType.REGEX;
        m_settings.m_replacement = "$1";

        var thrown = assertThrows(KNIMEException.KNIMERuntimeException.class,
            () -> m_model.execute(new BufferedDataTable[]{INPUT_TABLE}, m_fakeContext, m_settings));
        assertTrue(thrown.getMessage().contains("invalid group"), "Expected error message to contain 'invalid group'");
    }

    record TestCase(String pattern, String replacement, PatternType type, CaseSensitivity sensitivity,
        ReplacementStrategy strat, String... expectedColumns) {

        ColumnRenameRegexNodeSettings settings() {
            return settings(new ColumnRenameRegexNodeSettings());
        }

        ColumnRenameRegexNodeSettings settings(final ColumnRenameRegexNodeSettings objectToOverWrite) {
            objectToOverWrite.m_pattern = pattern;
            objectToOverWrite.m_replacement = replacement;
            objectToOverWrite.m_patternType = type;
            objectToOverWrite.m_caseSensitivity = sensitivity;
            objectToOverWrite.m_replacementStrategy = strat;
            return objectToOverWrite;
        }
    }

    /**
     * Provides test cases for the ColumnRenameRegexNodeModel2Test.
     *
     * We have 3 possible patterns, 2 possible case sensitivities, and 2 possible replacement strategies. We should also
     * test a few extra things like replacing with groups in regex/wildcard/literal, make sure column order doesn't
     * change, etc.
     */
    static Stream<Arguments> provideTestCases() {
        return Stream.of( //
            // start testing LITERAL matching
            new TestCase("column1", "x", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "x", "column2", "column3"), //
            new TestCase("COLUMN1", "x", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("COLUMN1", "x", PatternType.LITERAL, CaseSensitivity.CASE_INSENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "x", "column2", "column3"), //
            new TestCase("column(1)", "$1", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE, //
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("column1", "$1", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE, //
                ReplacementStrategy.WHOLE_STRING, "$1", "column2", "column3"), //
            new TestCase("c", "x", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("c", "x", PatternType.LITERAL, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "xolumn1", "xolumn2", "xolumn3"), //
            // test REGEX matching
            new TestCase("column(\\d)", "$1column", PatternType.REGEX, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "1column", "2column", "3column"), //
            new TestCase("COLUMN(\\d)", "$1column", PatternType.REGEX, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("column(\\d)", "$1column", PatternType.REGEX, CaseSensitivity.CASE_INSENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "1column", "2column", "3column"), //
            new TestCase("\\d", "x", PatternType.REGEX, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("\\d", "x", PatternType.REGEX, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "columnx", "columnx (#1)", "columnx (#2)"), //
            new TestCase("\\d", "&", PatternType.REGEX, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "column&", "column& (#1)", "column& (#2)"), //
            // test WILDCARD matching
            new TestCase("column?", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "x", "x (#1)", "x (#2)"), //
            new TestCase("col?", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("col*", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "x", "x (#1)", "x (#2)"), //
            new TestCase("COL*", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "column1", "column2", "column3"), //
            new TestCase("COL*", "x", PatternType.WILDCARD, CaseSensitivity.CASE_INSENSITIVE,
                ReplacementStrategy.WHOLE_STRING, "x", "x (#1)", "x (#2)"), //
            new TestCase("col?", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "xmn1", "xmn2", "xmn3"), //
            new TestCase("COL?", "x", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "column1", "column2", "column3"), //
            new TestCase("COL?", "x", PatternType.WILDCARD, CaseSensitivity.CASE_INSENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "xmn1", "xmn2", "xmn3"), //
            new TestCase("COL(?)", "$1", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "column1", "column2", "column3"), //
            new TestCase("col?", "$1", PatternType.WILDCARD, CaseSensitivity.CASE_SENSITIVE,
                ReplacementStrategy.ALL_OCCURENCES, "$1mn1", "$1mn2", "$1mn3") //
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void doSpecTests(final TestCase tc) throws Exception {
        tc.settings(m_settings);

        var outSpecs = m_model.configure(new DataTableSpec[]{INPUT_TABLE_SPEC}, m_settings);

        assertEquals(tc.expectedColumns.length, outSpecs[0].getNumColumns(),
            "Expected same number of columns before and after");

        assertTrue(Arrays.equals(tc.expectedColumns, outSpecs[0].getColumnNames()),
            "Expected columns to be in the correct order." //
                + "\n\n" //
                + "Expected: " + Arrays.toString(tc.expectedColumns) //
                + "\n\n" //
                + "But was: " + Arrays.toString(outSpecs[0].getColumnNames()) //
        );
    }
}
