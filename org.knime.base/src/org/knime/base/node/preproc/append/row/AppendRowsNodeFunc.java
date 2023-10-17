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
 *   Oct 17, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.append.row;

import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.AbstractNodeFunc;
import org.knime.core.node.func.ArgumentDefinition;
import org.knime.core.node.func.DefaultPortDefinition;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.func.PortDefinition;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class AppendRowsNodeFunc extends AbstractNodeFunc {

    /**
     * Constructor used by the framework to instantiate the NodeFunc.
     */
    public AppendRowsNodeFunc() {
        super(API, AppendedRowsNodeFactory.class.getName());
    }

    @Override
    public String getName() {
        return "append_rows";
    }

    private static final NodeFuncApi API = NodeFuncApi.builder("append_rows")//
        .withDescription("""
            Returns a new DataFrame that consists of the DataFrames a and be concatenated along the row dimension.
            Columns are matched by names and a common super type is determined by the framework.
                """
            )//
        .withInputTable("a", "The first table")//
        .withInputTable("b", "The second table")//
        .withOutputTable("concatenated", "The concatenated table")//
        .build();

    @Override
    public void saveSettings(final String[] arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) {
        settings.addBoolean(AppendedRowsNodeModel.CFG_FAIL_ON_DUPLICATES, false);
        settings.addBoolean(AppendedRowsNodeModel.CFG_APPEND_SUFFIX, true);
        settings.addBoolean(AppendedRowsNodeModel.CFG_INTERSECT_COLUMNS, false);
        settings.addString(AppendedRowsNodeModel.CFG_SUFFIX, "_dup");
        settings.addBoolean(AppendedRowsNodeModel.CFG_HILITING, false);
    }

    @Override
    public String getDescription() {
        return "Concatenates the tables a and b along the row dimension.";
    }

    @Override
    public PortDefinition[] getInputs() {
        return new PortDefinition[]{new DefaultPortDefinition("a", "The first table"),
            new DefaultPortDefinition("b", "The second table")};
    }

    @Override
    public ArgumentDefinition[] getArguments() {
        return new ArgumentDefinition[0];
    }

    @Override
    public PortDefinition[] getOutputs() {
        return new PortDefinition[]{new DefaultPortDefinition("out", "The concatenated table")};
    }

    @Override
    public String getNodeFactoryClassName() {
        return AppendedRowsNodeFactory.class.getName();
    }

}
