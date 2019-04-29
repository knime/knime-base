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
 *   Apr 25, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class KnimePerturbableFeatureVectorTest {

    /**
     *
     */
    private static final String KEY_SUFFIX = "test";

    @Mock
    private DataRow m_parentRow;

    @Mock
    private FeatureVector m_parent;

    private RowKey m_originalKey = new RowKey("originalKey");

    @Mock
    private Perturber<DataRow, Set<Integer>, DataCell[]> m_perturber;

    @Mock
    private DataCell m_cell;

    private KnimePerturbableFeatureVector createTestingVector() {
        return new KnimePerturbableFeatureVector(m_parent, m_originalKey, KEY_SUFFIX, m_perturber);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testPerturbIndexOutOfBounds() throws Exception {
        Mockito.when(m_parent.size()).thenReturn(2);
        createTestingVector().perturb(2);
    }

    private DataCell[] createMockCells(final int size) {
        DataCell[] cells = new DataCell[size];
        for (int i = 0; i < size; i++) {
            cells[i] = m_cell;
        }
        return cells;
    }


    @Test
    public void testPerturb() throws Exception {
        setupPerturb(5);
        final KnimePerturbableFeatureVector vec = createTestingVector();
        vec.perturb(1);
        vec.perturb(3);
        final DataRow row = vec.get();
        assertEquals("originalKeytest", row.getKey().getString());
        assertEquals(5, row.getNumCells());
        for (int i = 0; i < row.getNumCells(); i++) {
            assertEquals(m_cell, row.getCell(i));
        }
        Mockito.verify(m_perturber).perturb(m_parentRow, Sets.newHashSet(1, 3));
        // test if the instance tries to create the row again (which it shouldn't)
        final DataRow secondCall = vec.get();
        Mockito.verify(m_perturber, times(1)).perturb(any(), any());
        assertEquals(row, secondCall);
    }

    /**
     *
     */
    private void setupPerturb(final int size) {
        when(m_parent.size()).thenReturn(size);
        when(m_parent.get()).thenReturn(m_parentRow);
        final DataCell[] mockCells = createMockCells(size);
        Mockito.when(m_perturber.perturb(any(), any())).thenReturn(mockCells);
    }

    @Test (expected = IllegalStateException.class)
    public void testPerturbAfterGet() throws Exception {
        setupPerturb(5);
        final KnimePerturbableFeatureVector vec = createTestingVector();
        vec.perturb(1);
        vec.get();
        vec.perturb(5);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetPerturbableDetectsDuplicateKey() throws Exception {
        final KnimePerturbableFeatureVector vec = createTestingVector();
        vec.getPerturbable(KEY_SUFFIX);
    }

    @Test
    public void testGetPerturbable() throws Exception {
        setupPerturb(5);
        final KnimePerturbableFeatureVector parent = createTestingVector();
        final PerturbableFeatureVector child = parent.getPerturbable("child");
        child.perturb(2);
        child.get();
        Mockito.verify(m_perturber, times(2)).perturb(any(), any());
    }

}
