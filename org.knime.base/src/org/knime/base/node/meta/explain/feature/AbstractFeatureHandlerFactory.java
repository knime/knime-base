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
 *   01.04.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.CheckUtils;

abstract class AbstractFeatureHandlerFactory<T extends DataValue> implements FeatureHandlerFactory {

    protected abstract Class<T> getAcceptValueClass();

    protected abstract int getNumFeatures(T value);

    /**
     * {@inheritDoc}
     * @throws MissingValueException
     */
    @Override
    public final int numFeatures(final DataCell cell) throws MissingValueException {
        final T value = getAsT(cell);
        return getNumFeatures(value);
    }

    // The compatibility is explicitly checked
    @SuppressWarnings("unchecked")
    private T getAsT(final DataCell cell) throws MissingValueException {
        if (cell.isMissing() && !supportsMissingValues()) {
            throw new MissingValueException();
        }
        CheckUtils.checkArgument(getAcceptValueClass().isInstance(cell),
            "The provided cell '%s' is not of expected class '%s'", cell, getAcceptValueClass().getCanonicalName());
        return (T)cell;
    }

    abstract class AbstractFeatureHandler implements FeatureHandler {
        protected T m_original;

        protected T m_sampled;

        /**
         * {@inheritDoc}
         * @throws MissingValueException
         */
        @Override
        public final void setOriginal(final DataCell cell) throws MissingValueException {
            m_original = getAsT(cell);
        }

        /**
         * {@inheritDoc}
         * @throws MissingValueException
         */
        @Override
        public final void setSampled(final DataCell cell) throws MissingValueException {
            m_sampled = getAsT(cell);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public final void reset() {
            m_original = null;
            m_sampled = null;
            resetReplaceState();
        }

    }

}