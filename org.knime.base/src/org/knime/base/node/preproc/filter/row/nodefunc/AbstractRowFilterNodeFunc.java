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
 *   Nov 6, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row.nodefunc;

import org.knime.base.node.preproc.filter.row.RowFilterNodeFactory;
import org.knime.base.node.preproc.filter.row.RowFilterNodeModel;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Base class for Row Filter NodeFuncs.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractRowFilterNodeFunc implements NodeFunc {

    private static final String INCLUDE = "include";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var tableSpec = (DataTableSpec)inputSpecs[0];
        var include = arguments.getBoolean(INCLUDE);
        var rowFilter = createFilter(arguments, tableSpec, include);
        rowFilter.configure(tableSpec);
        var rowFilterSettings = settings.addNodeSettings(RowFilterNodeModel.CFGFILTER);
        rowFilter.saveSettingsTo(rowFilterSettings);
    }

    abstract IRowFilter createFilter(final NodeSettingsRO arguments, final DataTableSpec tableSpec, boolean include)
        throws InvalidSettingsException;

    @Override
    public String getNodeFactoryClassName() {
        return RowFilterNodeFactory.class.getName();
    }

    @Override
    public final NodeFuncApi getApi() {
        var builder = NodeFuncApi.builder(getName());
        builder.withInputTable("table", "The table to filter.")//
            .withOutputTable("filtered_table", "The filtered table.")//
            .withBooleanArgument(INCLUDE,
                "Whether rows matching the filter should be included in the output table or not.");
        extendApi(builder);
        return builder.build();
    }

    abstract void extendApi(final NodeFuncApi.Builder builder);

    abstract String getName();

}
