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
 *   Mar 10 2024 (david): created
 */
package org.knime.base.node.preproc.split3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.testing.util.TableTestUtil;

/**
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ColumnSplitNodeModel3Test {

    ColumnSplitNodeModel3 m_model;

    DataTableSpec m_inputSpec;

    BufferedDataTable m_inputTable;

    ExecutionContext m_fakeContext;

    @BeforeEach
    void setup() {
        m_model = new ColumnSplitNodeModel3(ColumnSplitNodeFactory3.CONFIGURATION);

        m_inputSpec = new DataTableSpecCreator() //
            .addColumns(new DataColumnSpecCreator("A", IntCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("B", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("C", DoubleCell.TYPE).createSpec()) //
            .createSpec();

        m_inputTable = new TableTestUtil.TableBuilder(m_inputSpec) //
            .addRow(1, "a", 1.0) //
            .addRow(2, "b", 2.0) //
            .addRow(3, "c", 3.0) //
            .build().get();

        @SuppressWarnings({"unchecked", "rawtypes"}) // annoying but necessary
        NodeFactory<NodeModel> factory = (NodeFactory)new ColumnSplitNodeFactory3();

        m_fakeContext = new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(factory),
            SingleNodeContainer.MemoryPolicy.CacheSmallInMemory, NotInWorkflowDataRepository.newInstance());
    }

    @Test
    void testOutputSpecIsCorrect() throws Exception {
        var settings = new ColumnSplitNodeSettings();
        settings.m_columnsToInclude = new ColumnFilter(new String[]{"A", "C"});

        var warningMessageReceived = new AtomicBoolean();

        m_model.addWarningListener(m -> warningMessageReceived.set(true));
        var result = m_model.execute(new BufferedDataTable[]{m_inputTable}, m_fakeContext, settings);

        assertEquals(2, result.length, "Expected two output tables");

        var includedTableSpec = result[0].getDataTableSpec();
        var excludedTableSpec = result[1].getDataTableSpec();

        assertTrue(includedTableSpec.containsName("A"), "Expected column A in included table");
        assertTrue(includedTableSpec.containsName("C"), "Expected column C in included table");
        assertEquals(2, includedTableSpec.getNumColumns(), "Expected two columns in included table");

        assertTrue(excludedTableSpec.containsName("B"), "Expected column B in excluded table");
        assertEquals(1, excludedTableSpec.getNumColumns(), "Expected one column in excluded table");

        // also check included table order
        assertEquals("A", includedTableSpec.getColumnSpec(0).getName(), "Expected column A first in included table");
        assertEquals("C", includedTableSpec.getColumnSpec(1).getName(), "Expected column C second in included table");

        // expect no warning
        assertFalse(warningMessageReceived.get(), "Expected no warning");
    }

    @Test
    void testWorksIfAllColumnsIncluded() throws Exception {
        var settings = new ColumnSplitNodeSettings();
        settings.m_columnsToInclude = new ColumnFilter(new String[]{"A", "B", "C"});

        var warningMessageReceived = new AtomicBoolean();

        m_model.addWarningListener(m -> warningMessageReceived.set(true));
        var result = m_model.execute(new BufferedDataTable[]{m_inputTable}, m_fakeContext, settings);

        assertEquals(2, result.length, "Expected two output tables");

        var includedTableSpec = result[0].getDataTableSpec();
        var excludedTableSpec = result[1].getDataTableSpec();

        assertEquals(3, includedTableSpec.getNumColumns(), "Expected three columns in included table");
        assertEquals(0, excludedTableSpec.getNumColumns(), "Expected no columns in excluded table");

        // also check order
        assertEquals("A", includedTableSpec.getColumnSpec(0).getName(), "Expected column A first in included table");
        assertEquals("B", includedTableSpec.getColumnSpec(1).getName(), "Expected column B second in included table");
        assertEquals("C", includedTableSpec.getColumnSpec(2).getName(), "Expected column C third in included table");

        // expect no warning
        assertFalse(warningMessageReceived.get(), "Expected no warning");
    }

    @Test
    void testWorksIfNoColumnsIncluded() throws Exception {
        var settings = new ColumnSplitNodeSettings();
        settings.m_columnsToInclude = new ColumnFilter(new String[]{});

        var warningMessageReceived = new AtomicBoolean();

        m_model.addWarningListener(m -> warningMessageReceived.set(true));
        var result = m_model.execute(new BufferedDataTable[]{m_inputTable}, m_fakeContext, settings);

        assertEquals(2, result.length, "Expected two output tables");

        var includedTableSpec = result[0].getDataTableSpec();
        var excludedTableSpec = result[1].getDataTableSpec();

        assertEquals(0, includedTableSpec.getNumColumns(), "Expected no columns in included table");
        assertEquals(3, excludedTableSpec.getNumColumns(), "Expected three columns in excluded table");

        // also check order
        assertEquals("A", excludedTableSpec.getColumnSpec(0).getName(), "Expected column A first in excluded table");
        assertEquals("B", excludedTableSpec.getColumnSpec(1).getName(), "Expected column B second in excluded table");
        assertEquals("C", excludedTableSpec.getColumnSpec(2).getName(), "Expected column C third in excluded table");

        // expect no warning
        assertFalse(warningMessageReceived.get(), "Expected no warning");
    }

    @Test
    void testWarnsIfColumnNoLongerExists() throws Exception {
        var nonExistentColumn = "how much wood would a woodchuck chuck";

        var settings = new ColumnSplitNodeSettings();
        settings.m_columnsToInclude = new ColumnFilter(new String[]{"A", nonExistentColumn});

        AtomicReference<String> warningMessage = new AtomicReference<>();
        m_model.addWarningListener(m -> warningMessage.set(m.getSummary()));

        m_model.configure(new DataTableSpec[]{m_inputSpec}, settings);

        var warningMessageString = warningMessage.get();
        assertNotNull(warningMessageString, "expected warning");
        assertTrue(warningMessageString.contains(nonExistentColumn));
    }

    @Test
    void testWarnsIfMultipleColumnNoLongerExist() throws Exception {
        var nonExistentColumn1 = "how much wood would a woodchuck chuck";
        var nonExistentColumn2 = "if a woodchuck could chuck wood";

        var settings = new ColumnSplitNodeSettings();
        settings.m_columnsToInclude = new ColumnFilter(new String[]{"A", nonExistentColumn1, nonExistentColumn2});

        AtomicReference<String> warningMessage = new AtomicReference<>();
        m_model.addWarningListener(m -> warningMessage.set(m.getSummary()));

        m_model.configure(new DataTableSpec[]{m_inputSpec}, settings);

        var warningMessageString = warningMessage.get();
        assertNotNull(warningMessageString, "expected warning");
        assertTrue(warningMessageString.contains(nonExistentColumn1));
        assertTrue(warningMessageString.contains(nonExistentColumn2));
    }
}
