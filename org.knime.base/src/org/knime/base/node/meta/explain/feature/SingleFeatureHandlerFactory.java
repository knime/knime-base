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

import java.util.Collections;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.util.CheckUtils;

final class SingleFeatureHandlerFactory extends AbstractFeatureHandlerFactory<DataCell> {

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureHandler createFeatureHandler() {
        return new SingleFeatureHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handlesCollections() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsMissingValues() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFeatureNames(final DataColumnSpec columnSpec) {
        return Collections.singletonList(columnSpec.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<DataCell> getAcceptValueClass() {
        return DataCell.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNumFeatures(final DataCell value) {
        return 1;
    }

    class SingleFeatureHandler extends AbstractFeatureHandler {
        private boolean m_replace = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public void markForReplacement(final int idx) {
            CheckUtils.checkArgument(idx == 0, "Cells handled by this FeatureHandler represent only a single feature");
            m_replace = true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createReplaced() {
            CheckUtils.checkState(m_original != null && m_sampled != null, "The method createReplacement can only be "
                    + "called after both setOriginal and setSampled have been called at least once since the last reset.");
            return m_replace ? m_sampled : m_original;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resetReplaceState() {
            m_replace = false;
        }

    }

}