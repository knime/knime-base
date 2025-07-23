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
 *   Nov 13, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.unpivot2;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.ArgumentDefinition.PrimitiveArgumentType;
import org.knime.core.node.func.ListArgumentType;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.SimpleNodeFunc;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class UnpivotNodeFunc implements SimpleNodeFunc {

    private static final String SKIP_MISSING_VALUES = "skip_missing_values";

    private static final String RETAINED_COLUMNS = "retained_columns";

    private static final String VALUE_COLUMNS = "value_columns";

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var unpivotSettings = new Unpivot2NodeSettings();
        unpivotSettings.m_valueColumns = new ColumnFilter(arguments.getStringArray(VALUE_COLUMNS));
        unpivotSettings.m_retainedColumns = new ColumnFilter(arguments.getStringArray(RETAINED_COLUMNS));
        unpivotSettings.m_missingValues = arguments.getBoolean(SKIP_MISSING_VALUES);
        NodeParametersUtil.saveSettings(Unpivot2NodeSettings.class, unpivotSettings, settings);
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("unpivot")//
            .withInputTable("table", "The table to unpivot.")//
            .withOutputTable("unpivotted_table", "The unpivotted table.")//
            .withDescription("""
                    Rotates the selected columns from the input table to rows
                    and duplicates at the same time the remaining input columns
                    by appending them to each corresponding output row. Example:
                    Input:
                        Value1 Value2 Value3 Extra1 Extra2 Extra3
                    Output:
                        Value1 Extra1 Extra2 Extra3
                        Value2 Extra1 Extra2 Extra3
                        Value3 Extra1 Extra2 Extra3""")
            .withArgument(VALUE_COLUMNS, "The columns that are rotated into one single column.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))//
            .withArgument(RETAINED_COLUMNS, "The columns whose values are duplicated by the number of selected value columns.",
                ListArgumentType.create(PrimitiveArgumentType.STRING, false))
            .withArgument(SKIP_MISSING_VALUES,
                "Skip all rows containing missing cells in the selected value columns.", PrimitiveArgumentType.BOOLEAN)
            .build();
    }

    @Override
    public Class<? extends NodeFactory<?>> getNodeFactoryClass() {
        return Unpivot2NodeFactory.class;
    }

}
