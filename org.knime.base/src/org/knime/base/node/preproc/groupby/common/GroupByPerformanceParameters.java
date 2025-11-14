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
 *   Nov 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.groupby.common;

import java.util.function.Supplier;

import org.knime.base.node.preproc.groupby.Sections;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;

@SuppressWarnings("javadoc")
@LoadDefaultsForAbsentFields
public final class GroupByPerformanceParameters implements NodeParameters {

    @Layout(Sections.Performance.class)
    @Widget(title = "Enable hiliting", description = """
            If enabled, the hiliting of a group row will hilite all rows of this
            group in other views. Depending on the number of rows, enabling this
            feature might consume a lot of memory.
            """)
    @Persist(configKey = "enableHilite")
    boolean m_enableHiliting;

    @Layout(Sections.Performance.class)
    @Widget(title = "Process in memory", description = """
            Process the table in the memory. Requires more memory but is faster
            since the table needs not to be sorted prior aggregation.
            The memory consumption depends on the number of unique groups and
            the chosen aggregation method. The row order of the input table is
            automatically retained.
            """)
    @ValueReference(GroupByPerformanceParameters.ProcessInMemoryRef.class)
    @Persist(configKey = "inMemory")
    boolean m_processInMemory;

    static final class ProcessInMemoryRef implements ParameterReference<Boolean> {
        // empty class used for EffectPredicateProvider
    }

    @Layout(Sections.Performance.class)
    @Widget(title = "Retain row order", description = """
            Retains the original row order of the input table.
            Could result in longer execution time.
            The row order is automatically retained if the process in memory
            option is selected.
            """)
    @Effect(predicate = GroupByPerformanceParameters.ProcessInMemoryEffect.class, type = Effect.EffectType.DISABLE)
    @ValueProvider(GroupByPerformanceParameters.ProcessInMemoryEffect.class)
    @ValueReference(GroupByPerformanceParameters.RetainOrderRef.class)
    @Persist(configKey = "retainOrder")
    boolean m_retainOrder;

    static final class RetainOrderRef implements ParameterReference<Boolean> {
    }

    static final class ProcessInMemoryEffect implements EffectPredicateProvider, StateProvider<Boolean> {

        private Supplier<Boolean> m_processInMemoryChange;

        private Supplier<Boolean> m_currentRetain;

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(GroupByPerformanceParameters.ProcessInMemoryRef.class).isTrue();
        }

        @Override
        public void init(final StateProviderInitializer init) {
            m_processInMemoryChange =
                init.computeFromValueSupplier(GroupByPerformanceParameters.ProcessInMemoryRef.class);
            m_currentRetain = init.getValueSupplier(GroupByPerformanceParameters.RetainOrderRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput input) {
            // in-memory implies retain order
            // otherwise, we keep its current value
            return m_processInMemoryChange.get() || m_currentRetain.get();
        }
    }
}
