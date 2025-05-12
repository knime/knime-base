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
 *   Mar 10, 2025 (david): created
 */
package org.knime.base.node.preproc.split3;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Model for the new WebUI version of the column splitter node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ColumnSplitNodeModel3 extends WebUINodeModel<ColumnSplitNodeSettings> {

    ColumnSplitNodeModel3(final WebUINodeConfiguration configuration) {
        super(configuration, ColumnSplitNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final ColumnSplitNodeSettings modelSettings) throws Exception {

        var selectedColumns = modelSettings.m_columnsToInclude.filterFromFullSpec(inData[0].getDataTableSpec());

        var columnRearrangers = createColumnRearrangers(inData[0].getDataTableSpec(), selectedColumns);

        var includeTable =
            exec.createColumnRearrangeTable(inData[0], columnRearrangers.included, exec.createSubProgress(0.5));
        var excludeTable =
            exec.createColumnRearrangeTable(inData[0], columnRearrangers.excluded, exec.createSubProgress(0.5));

        return new BufferedDataTable[]{includeTable, excludeTable};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final ColumnSplitNodeSettings modelSettings)
        throws InvalidSettingsException {

        var selectedColumns = modelSettings.m_columnsToInclude.filterFromFullSpec(inSpecs[0]);
        var columnRearrangers = createColumnRearrangers(inSpecs[0], selectedColumns);

        return new DataTableSpec[]{ //
            columnRearrangers.included.createSpec(), //
            columnRearrangers.excluded.createSpec() //
        };
    }

    static record ColumnRearrangers(ColumnRearranger included, ColumnRearranger excluded) {
    }

    static ColumnRearrangers createColumnRearrangers(final DataTableSpec inSpec, final String[] includedColumns) {
        ColumnRearranger includedColumnRearranger = new ColumnRearranger(inSpec);
        includedColumnRearranger.keepOnly(includedColumns);

        ColumnRearranger excludedColumnRearranger = new ColumnRearranger(inSpec);
        excludedColumnRearranger.remove(includedColumns);

        return new ColumnRearrangers(includedColumnRearranger, excludedColumnRearranger);
    }
}
