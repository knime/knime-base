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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyStringFilter;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Node parameters for CAIM Binner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class CAIMDiscretization2NodeParameters implements NodeParameters {

    @Widget(title = "Class column", description = """
            The class column. According to this column the binning is optimized.
            """)
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueProvider(ClassColumnProvider.class)
    @ValueReference(ClassColumnRef.class)
    @Persist(configKey = CAIMDiscretizationNodeModel.CFG_CLASS_COLUMN)
    String m_classColumn;

    @Widget(title = "Column selection", description = """
            Allows to include those columns which should be included in the discretization. Just the included
            columns are discretized and changed to "String" type.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @Persist(configKey = CAIMDiscretizationNodeModel.CFG_INCLUDED_COLUMNS)
    @ValueReference(IncludedColumnsRef.class)
    @ValueProvider(IncludedColumnsProvider.class)
    @Modification(IncludedColumnsModification.class)
    LegacyStringFilter m_includedColumns = new LegacyStringFilter(new String[0], new String[0]);

    static final class IncludedColumnsModification extends LegacyStringFilter.LegacyStringFilterModification {

        IncludedColumnsModification() {
            super(false, null, """
                    Allows to include those columns which should be included in the discretization. Just the included
                    columns are discretized and changed to "String" type.
                    """, null, null, null, null);
        }

    }

    @Advanced
    @Widget(title = "Sort in memory", description = """
            To increase performance, select this option to sort the data in memory during the discretization process
            and select the memory policy 'Keep all in memory' at the PREVIOUS node (if possible)
            This can significantly improve performance but requires more memory.
            """)
    @Persist(configKey = CAIMDiscretizationNodeModel.CFG_SORT_IN_MEMORY)
    boolean m_sortInMemory = true;

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    static final class IncludedColumnsRef implements ParameterReference<LegacyStringFilter> {
    }

    static final class ClassColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ClassColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, StringValue.class);
            return compatibleColumns.isEmpty() ?
                Optional.empty() : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class IncludedColumnsProvider implements StateProvider<LegacyStringFilter> {

        Supplier<LegacyStringFilter> m_includedColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_includedColumnsSupplier = initializer.getValueSupplier(IncludedColumnsRef.class);
        }

        @Override
        public LegacyStringFilter computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var columnSelection = m_includedColumnsSupplier.get();
            if (columnSelection != null) {
                final var twinlist = columnSelection.m_twinList;
                if (twinlist.m_inclList.length != 0 || twinlist.m_exclList.length != 0) {
                    throw new StateComputationFailureException();
                }
            }
            return new LegacyStringFilter(parametersInput.getInTableSpec(0).stream()
                .map(spec -> ColumnSelectionUtil.getCompatibleColumns(spec, DoubleValue.class)).flatMap(List::stream)
                .map(DataColumnSpec::getName).toArray(String[]::new), new String[0]);
        }

    }

}
