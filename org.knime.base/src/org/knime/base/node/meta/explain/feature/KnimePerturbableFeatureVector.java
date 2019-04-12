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
 *   Apr 12, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.util.CheckUtils;

/**
 *
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class KnimePerturbableFeatureVector implements PerturbableFeatureVector {

    private final Set<Integer> m_markedForPerturbance = new HashSet<>();

    private final String m_keySuffix;

    private final RowKey m_originalKey;

    private final FeatureVector m_parent;

    private final Perturber<DataRow, Set<Integer>, DataCell[]> m_perturber;

    private DataRow m_row;

    /**
     * @param parent feature vector to perturb
     * @param originalKey key of the input row
     * @param keySuffix to append to originalKey to identify this instance
     * @param perturber used to actually perform perturbation
     *
     */
    public KnimePerturbableFeatureVector(final FeatureVector parent, final RowKey originalKey,
        final String keySuffix, final Perturber<DataRow, Set<Integer>, DataCell[]> perturber) {
        m_parent = parent;
        m_originalKey = originalKey;
        m_keySuffix = keySuffix;
        m_perturber = perturber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perturb(final int idx) {
        CheckUtils.checkState(m_row == null,
            "Perturbables are only allowed to be perturbed as long as get has not been called yet.");
        checkIndex(idx);
        m_markedForPerturbance.add(idx);
    }

    /**
     * @param idx
     */
    private void checkIndex(final int idx) {
        if (idx > size()) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return m_parent.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow get() {
        if (m_row == null) {
            createPerturbedRow();
        }
        return m_row;
    }

    private void createPerturbedRow() {
        final DataRow parent = m_parent.get();
        final RowKey perturbedRowKey = createPerturbedRowKey();
        final DataCell[] cells = m_perturber.perturb(parent, m_markedForPerturbance);
        m_row = new DefaultRow(perturbedRowKey, cells);
    }

    private RowKey createPerturbedRowKey() {
        return new RowKey(m_originalKey.getString() + m_keySuffix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PerturbableFeatureVector getPerturbable(final String key) {
        return new KnimePerturbableFeatureVector(this, m_originalKey, key, m_perturber);
    }

}