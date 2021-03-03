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
 *   Mar 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Contains unit tests for NameChecker.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NameCheckerTest {

    private NameChecker m_testInstance;

    /**
     * Initializes the test instance.
     */
    @Before
    public void init() {
        m_testInstance = new NameChecker();
    }

    /**
     * Tests adding a name.
     */
    @Test
    public void testAddingName() {
        assertEquals(false, m_testInstance.isTaken("foo"));
        m_testInstance.add("foo");
        assertEquals(true, m_testInstance.isTaken("foo"));
    }

    /**
     * Tests if rename throws an exception whenever it is called with an unknown old name.
     */
    @Test (expected = IllegalArgumentException.class)
    public void testRenameOldNameUnknown() {
        testRename(0, 0);
    }

    /**
     * Old name is free after rename, new name is unique.
     */
    @Test
    public void testAfterRenameOldFreeNewUnique() {
        testRename(1, 0);
    }

    /**
     * Old name is free after rename, new name is duplicated.
     */
    @Test
    public void testAfterRenameOldFreeNewDuplicated() {
        testRename(1, 1);
    }

    /**
     * Old and new name are unique after rename.
     */
    @Test
    public void testAfterRenameOldUniqueNewUnique() {
        testRename(2, 0);
    }

    /**
     * Old name is unique after rename, new name is duplicated.
     */
    @Test
    public void testAfterRenameOldUniqueNewDuplicated() {
        testRename(2, 1);
    }

    /**
     * Old name is duplicated after rename, new name is unique.
     */
    @Test
    public void testAfterRenameOldDuplicatedNewUnique() {
        testRename(3, 0);
    }

    /**
     * Both old and new name are duplicated after rename.
     */
    @Test
    public void testAfterRenameOldDuplicatedNewDuplicated() {
        testRename(3, 1);
    }

    private void testRename(final int oldBefore, final int newBefore) {
        for (int i = 0; i < oldBefore; i++) {
            m_testInstance.add("foo");
        }
        for (int i = 0; i < newBefore; i++) {
            m_testInstance.add("bar");
        }
        Affected affected = m_testInstance.rename("foo", "bar");
        Affected expected = oldBefore == 2 || newBefore > 0 ? Affected.ALL : Affected.THIS;
        assertEquals(expected, affected);
        assertEquals(oldBefore > 1, m_testInstance.isTaken("foo"));
        assertEquals(oldBefore > 2, m_testInstance.isDuplicated("foo"));
        assertEquals(true, m_testInstance.isTaken("bar"));
        assertEquals(newBefore > 0, m_testInstance.isDuplicated("bar"));
    }

    /**
     * Tests the clear method.
     */
     @Test
    public void testClear() {
        m_testInstance.add("foo");
        m_testInstance.add("bar");
        assertEquals(true, m_testInstance.isTaken("foo"));
        assertEquals(true, m_testInstance.isTaken("bar"));
        m_testInstance.clear();
        assertEquals(false, m_testInstance.isTaken("foo"));
        assertEquals(false, m_testInstance.isTaken("bar"));
    }

    /**
     * Tests the isDuplicated method.
     */
    @Test
    public void testIsDuplicated() {
        m_testInstance.add("foo");
        assertEquals(false, m_testInstance.isDuplicated("foo"));
        m_testInstance.add("foo");
        assertEquals(true, m_testInstance.isDuplicated("foo"));
    }

}
