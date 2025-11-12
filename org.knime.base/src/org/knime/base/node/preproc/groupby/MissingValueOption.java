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
 *   20 Oct 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.groupby;

import java.util.function.Supplier;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.ArrayPersistor;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Options for missing value handling in aggregation operators.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
enum MissingValueOption {
        @Label("Exclude")
        EXCLUDE, //
        @Label("Include")
        INCLUDE;

    /**
     * Abstract class for effect provider to show the missing value option only if the selected aggregation method
     * supports it. Annotate the {@link MissingValueOption} parameter with your derivation:
     *
     * <pre>
     * <code>
     *   &#64;Effect(type = EffectType.SHOW, predicate = MySupportsMissingValueOptions.class)
     * </code>
     * </pre>
     *
     * Then introduce a transient boolean parameter, annotated with your derivation as well:
     *
     * <pre>
     * <code>
     * &#64;ValueProvider(MySupportsMissingValueOptions.class)
     * &#64;ValueReference(MySupportsMissingValueOptions.class)
     * &#64;PersistArrayElement(MyNoPersistenceElementFieldPersistor.class) // if inside ArrayPersistor
     * // helper flag to show/hide missing value option
     * boolean m_supportsMissingValueOption;
     * </code>
     * </pre>
     *
     * If inside an {@link ArrayPersistor}, you need to derive a {@link NoPersistenceElementFieldPersistor} to make the
     * boolean field "transient".
     */
    @SuppressWarnings("restriction")
    abstract static class SupportsMissingValueOptions implements StateProvider<Boolean>, BooleanReference {

        private Supplier<String> m_methodSupplier;

        abstract Class<? extends ParameterReference<String>> getMethodReference();

        @Override
        public final void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_methodSupplier = initializer.computeFromValueSupplier(getMethodReference());
        }

        @Override
        public final Boolean computeState(final NodeParametersInput ignored) {
            final var id = m_methodSupplier.get();
            if (id == null) {
                return false;
            }
            final var method = AggregationMethods.getMethod4Id(id);
            return method.supportsMissingValueOption();
        }

    }

}
