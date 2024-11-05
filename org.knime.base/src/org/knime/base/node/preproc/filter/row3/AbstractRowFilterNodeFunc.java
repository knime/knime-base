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
 *   14 Oct 2024 (Alexander Jauch-Walser): created
 */
package org.knime.base.node.preproc.filter.row3;

import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterCriterion;
import org.knime.base.node.preproc.filter.row3.AbstractRowFilterNodeSettings.FilterMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;

/**
 *
 * Base class for Row Filter NodeFuncs.
 *
 * @author Alexander Jauch-Walser, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractRowFilterNodeFunc implements NodeFunc {

    // Whether to include or exclude the rows
    static final String INCLUDE = "include";
    static final String COLUMN = "column";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        boolean include = arguments.getBoolean(INCLUDE);

        var criterion = getSpecificCriterion(arguments);

        // NodeFunc will always create a row filter with exactly one filter criterion
        var rowFilterSettings = new RowFilterNodeSettings();
        rowFilterSettings.m_predicates = new FilterCriterion[] {criterion};
        rowFilterSettings.m_outputMode = include ? FilterMode.MATCHING : FilterMode.NON_MATCHING;

        DefaultNodeSettings.saveSettings(RowFilterNodeSettings.class, rowFilterSettings, settings);
    }

    @Override
    public final NodeFuncApi getApi() {
        var builder = NodeFuncApi.builder(getName());
        builder.withInputTable("table", "The table to filter.")//
            .withOutputTable("filtered_table", "The filtered table.")//
            .withBooleanArgument(INCLUDE,
                "Whether rows matching the filter for a specific column should be "
                + "included in the output table or not.")//
            .withStringArgument(COLUMN,
                "The column on which the criterion will be applied to.");
        extendApi(builder);
        return builder.build();
    }

    /**
     * Each NodeFunc will produce a row filter with a specific configured criterion
     */
    abstract FilterCriterion getSpecificCriterion(final NodeSettingsRO arguments) throws InvalidSettingsException;

    @Override
    public String getNodeFactoryClassName() {
        return RowFilterNodeFactory.class.getName();
    }

    /**
     * Derived NodeFuncs can set a function description add additional parameters to the API builder here
     */
    abstract void extendApi(final NodeFuncApi.Builder builder);

    abstract String getName();

}
