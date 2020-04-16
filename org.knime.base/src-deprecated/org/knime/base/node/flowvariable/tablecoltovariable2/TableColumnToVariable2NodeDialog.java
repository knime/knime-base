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
package org.knime.base.node.flowvariable.tablecoltovariable2;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.base.node.flowvariable.VariableAndDataCellUtil;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "TableColumnToVariable2" Node. Converts the values from a table column to flow
 * variables with the row ids as their variable name.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 *
 * @author Gabor Bakos
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @deprecated replaced by {@code TableColumnToVariable3NodeDialog}
 */
@Deprecated
class TableColumnToVariable2NodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the TableColumnToVariable2 node.
     */
    TableColumnToVariable2NodeDialog() {
        final ColumnFilter filter = new ColumnFilter() {
            @Override
            public boolean includeColumn(final DataColumnSpec colSpec) {
                return VariableAndDataCellUtil.isTypeCompatible(colSpec.getType());
            }

            @Override
            public String allFilteredMsg() {
                return "No columns compatible with any of the types "
                    + Arrays.stream(VariableAndDataCellUtil.getSupportedVariableTypes())
                        .map(t -> t.getIdentifier().toLowerCase()).collect(Collectors.joining(", "))
                    + ".";
            }
        };
        addDialogComponent(new DialogComponentColumnNameSelection(
            TableColumnToVariable2NodeModel.createColumnSettings(), "Column name", 0, true, filter));

        final DialogComponentBoolean ignoreMissing =
            new DialogComponentBoolean(TableColumnToVariable2NodeModel.createIgnoreMissing(), "Skip missing values");
        ignoreMissing
            .setToolTipText("When unchecked, the execution fails when a missing value is in the input column.");
        addDialogComponent(ignoreMissing);
    }

}
