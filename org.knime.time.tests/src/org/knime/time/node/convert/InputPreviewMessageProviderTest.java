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
 *   Jan 22, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.time.node.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
public class InputPreviewMessageProviderTest {

    class DefaultTestInputPreviewMessageProvider implements InputPreviewMessageProvider {
        private Optional<String> m_cellContent;

        private Optional<ColumnFilter> m_columnFilter;

        DefaultTestInputPreviewMessageProvider() {
            this.m_cellContent = Optional.empty();
            this.m_columnFilter = Optional.empty();
        }

        DefaultTestInputPreviewMessageProvider(final Optional<String> cellContent,
            final Optional<ColumnFilter> columnFilter) {
            this.m_cellContent = cellContent;
            this.m_columnFilter = columnFilter;
        }

        @Override
        public Optional<ColumnFilter> getFilter() {
            return this.m_columnFilter;
        }

        @Override
        public Optional<String> getFirstDataCellPreview(final DataTable dt, final String[] selectedCols) {
            return this.m_cellContent;
        }

    }

    @Test
    void testDefaultDescriptionWithNoTable() {
        var provider = new DefaultTestInputPreviewMessageProvider();
        final var mockContext = mock(NodeParametersInput.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{null});

        var description = provider.description(mockContext);

        assertEquals("No input data available. Connect a node to the input port.", description);
    }

    @Test
    void testDefaultDescriptionWithTableNotExecuted() {
        var provider = new DefaultTestInputPreviewMessageProvider();
        final var mockContext = mock(NodeParametersInput.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{new DataTableSpec()});
        when(mockContext.getInPortObjects()).thenReturn(new PortObject[1]);
        when(mockContext.getInTable(0)).thenReturn(Optional.empty());

        var description = provider.description(mockContext);

        assertEquals("No input data available. Execute upstream nodes.", description);
    }

    @Test
    void testDefaultDescriptionWithNoFilterAndColumn() {
        var provider = new DefaultTestInputPreviewMessageProvider();
        final var mockContext = mock(NodeParametersInput.class);
        final var mockDataTable = mock(BufferedDataTable.class);
        final var mockSpec = mock(DataTableSpec.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{mockSpec});
        when(mockContext.getInPortObjects()).thenReturn(new PortObject[1]);
        when(mockContext.getInTable(0)).thenReturn(Optional.of(mockDataTable));
        when(mockDataTable.getDataTableSpec()).thenReturn(mockSpec);
        when(mockSpec.getColumnNames()).thenReturn(new String[0]);

        var description = provider.description(mockContext);

        assertEquals("Select a column to see a preview", description);
    }

    @Test
    void testDefaultDescriptionWithFilter() {
        final var mockFilter = mock(ColumnFilter.class);
        when(mockFilter.filterFromFullSpec(any())).thenReturn(new String[0]);
        var provider = new DefaultTestInputPreviewMessageProvider(Optional.empty(), Optional.of(mockFilter));
        final var mockContext = mock(NodeParametersInput.class);
        final var mockDataTable = mock(BufferedDataTable.class);
        final var mockSpec = mock(DataTableSpec.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{mockSpec});
        when(mockContext.getInPortObjects()).thenReturn(new PortObject[1]);
        when(mockContext.getInTable(0)).thenReturn(Optional.of(mockDataTable));
        when(mockDataTable.getDataTableSpec()).thenReturn(mockSpec);
        when(mockSpec.getColumnNames()).thenReturn(new String[1]);

        var description = provider.description(mockContext);

        assertEquals("Select a column to see a preview", description);
            verify(mockFilter).filterFromFullSpec(any());
    }

    @Test
    void testDefaultDescriptionWithNoData() {
        var provider = new DefaultTestInputPreviewMessageProvider();
        final var mockContext = mock(NodeParametersInput.class);
        final var mockDataTable = mock(BufferedDataTable.class);
        final var mockSpec = mock(DataTableSpec.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{mockSpec});
        when(mockContext.getInPortObjects()).thenReturn(new PortObject[1]);
        when(mockContext.getInTable(0)).thenReturn(Optional.of(mockDataTable));
        when(mockDataTable.getDataTableSpec()).thenReturn(mockSpec);
        when(mockSpec.getColumnNames()).thenReturn(new String[1]);

        var description = provider.description(mockContext);

        assertEquals("No valid data available", description);
    }

    @Test
    void testDefaultDescriptionWithData() {
        var provider = new DefaultTestInputPreviewMessageProvider(Optional.of("TestData"), Optional.empty());
        final var mockContext = mock(NodeParametersInput.class);
        final var mockDataTable = mock(BufferedDataTable.class);
        final var mockSpec = mock(DataTableSpec.class);
        when(mockContext.getInPortSpecs()).thenReturn(new PortObjectSpec[]{mockSpec});
        when(mockContext.getInPortObjects()).thenReturn(new PortObject[1]);
        when(mockContext.getInTable(0)).thenReturn(Optional.of(mockDataTable));
        when(mockDataTable.getDataTableSpec()).thenReturn(mockSpec);
        when(mockSpec.getColumnNames()).thenReturn(new String[1]);

        var description = provider.description(mockContext);

        assertEquals("TestData", description);
    }
}
