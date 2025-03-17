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
 */
package org.knime.base.node.preproc.filter.rowref;

import java.util.function.Supplier;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;

/**
 * The Reference Row Filter node allow the filtering of row IDs based on a second reference table. Two modes are
 * possible, either the corresponding row IDs of the first table are included or excluded in the resulting output table.
 *
 * @author Christian Dietz, University of Konstanz
 * @author Paul Baernreuther, KNIME
 * @since 3.1
 */
@SuppressWarnings("restriction")
public class RowSplitRefNodeModel extends AbstractRowRefNodeModel<RowSplitRefNodeSettings> {

    RowSplitRefNodeModel(final WebUINodeConfiguration config) {
        super(config, RowSplitRefNodeSettings.class);
    }

    @Override
    DataTableSpec[] getOutputSpecs(final DataTableSpec inputSpec) {
        return new DataTableSpec[]{inputSpec, inputSpec};
    }

    @Override
    BufferedDataTable[] noopExecute(final BufferedDataTable inputTable) {
        return new BufferedDataTable[]{inputTable, inputTable};
    }

    @Override
    OutputCreator createOutputCreator(final DataTableSpec spec, final ExecutionContext exec,
        final RowSplitRefNodeSettings settings) {
        return new SplitterOutputCreator(spec, exec);
    }

    static class SplitterOutputCreator extends OutputCreator {

        private final BufferedDataContainer m_firstContainer;

        private final BufferedDataContainer m_secondContainer;

        SplitterOutputCreator(final DataTableSpec spec, final ExecutionContext exec) {
            m_firstContainer = exec.createDataContainer(spec);
            m_secondContainer = exec.createDataContainer(spec);
        }

        @Override
        void addRow(final DataRow row, final boolean isSelected) {
            if (isSelected) {
                m_firstContainer.addRowToTable(row);
            } else {
                m_secondContainer.addRowToTable(row);
            }
        }

        @Override
        BufferedDataTable[] createTables(final boolean updateDomains,
            final Supplier<ExecutionContext> domainUpdateExecSupplier) throws CanceledExecutionException {
            m_firstContainer.close();
            m_secondContainer.close();
            var firstTable = m_firstContainer.getTable();
            var secondTable = m_secondContainer.getTable();
            if (updateDomains) {
                final var domainUpdateExec = domainUpdateExecSupplier.get();
                firstTable = updateDomain(firstTable, domainUpdateExec.createSubExecutionContext(0.5));
                secondTable = updateDomain(secondTable, domainUpdateExec.createSubExecutionContext(0.5));
            }
            return new BufferedDataTable[]{firstTable, secondTable};

        }
    }

}
