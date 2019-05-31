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

package org.knime.base.data.filter.row.dialog.registry;

import java.util.function.Supplier;

import org.knime.base.data.filter.row.dialog.OperatorFunction;
import org.knime.base.data.filter.row.dialog.OperatorPanel;
import org.knime.base.data.filter.row.dialog.OperatorValidation;

/**
 * A "value" entity of the {@link OperatorRegistry}.
 *
 * @author Viktor Buria
 * @param <F> the operator function type
 * @since 4.0
 */
public final class OperatorValue<F> {

    private final F m_function;

    private final OperatorValidation m_validation;

    private final Supplier<OperatorPanel> m_panelSupplier;

    /**
     * Creates the {@link OperatorValue} builder instance.
     *
     * @param function the {@link OperatorFunction} instance
     * @param <F> a type of the {@link OperatorFunction}
     * @return the {@link OperatorValue} builder
     */
    public static <F extends OperatorFunction<?, ?>> Builder<F> builder(final F function) {
        return new Builder<F>(function);
    }

    /**
     * Gets a function to execute for the current operator.
     *
     * @return the {@link OperatorFunction}
     */
    public F getFunction() {
        return m_function;
    }

    /**
     * Gets a validation function for the current operator or {@code null} if no validation required.
     *
     * @return the {@link OperatorFunction} or {@code null}
     */
    public OperatorValidation getValidation() {
        return m_validation;
    }

    /**
     * Gets a panel for the current operator or {@code null} if the operator has no panel.
     *
     * @return the {@link OperatorPanel} or {@code null}
     */
    public OperatorPanel getPanel() {
        return m_panelSupplier != null ? m_panelSupplier.get() : null;
    }

    private OperatorValue(final F function, final OperatorValidation validation,
        final Supplier<OperatorPanel> panelSupplier) {
        m_function = function;
        m_validation = validation;
        m_panelSupplier = panelSupplier;
    }

    /**
     * Builder for the {@link OperatorValue}.
     *
     * @param <F> the type of operator function
     * @author Viktor Buria
     */
    public static final class Builder<F> {
        private final F m_function;

        private OperatorValidation m_validation;

        private Supplier<OperatorPanel> m_panelSupplier;

        private Builder(final F function) {
            m_function = function;
        }

        /**
         * Adds operator validation function for the operator.
         *
         * @param validation the {@link OperatorValidation}
         * @return the current {@link org.knime.base.data.filter.row.dialog.registry.OperatorValue.Builder} instance
         */
        public Builder<F> withValidation(final OperatorValidation validation) {
            m_validation = validation;
            return this;
        }

        /**
         * Adds panel for the operator.
         *
         * @param panel the {@link OperatorPanel} {@link Supplier}
         * @return the current {@link org.knime.base.data.filter.row.dialog.registry.OperatorValue.Builder} instance
         */
        public Builder<F> withPanel(final Supplier<OperatorPanel> panel) {
            m_panelSupplier = panel;
            return this;
        }

        /**
         * Creates new {@link OperatorValue} object.
         *
         * @return the {@link OperatorValue}
         */
        public OperatorValue<F> build() {
            return new OperatorValue<F>(m_function, m_validation, m_panelSupplier);
        }

    }

}
