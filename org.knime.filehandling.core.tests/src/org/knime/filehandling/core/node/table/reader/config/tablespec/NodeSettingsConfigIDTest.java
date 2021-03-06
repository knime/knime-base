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
 *   Feb 11, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.mockito.Mockito;

/**
 * Contains unit tests for {@link NodeSettingsConfigID}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class NodeSettingsConfigIDTest {

    /**
     * Tests the save method.
     *
     * @throws InvalidSettingsException never thrown
     */
    @Test
    public void testSave() throws InvalidSettingsException {
        final NodeSettings toSave = new NodeSettings("test");
        toSave.addString("key", "value");

        final NodeSettingsConfigID configID = new NodeSettingsConfigID(toSave);

        final NodeSettings saved = new NodeSettings("settings");
        configID.save(saved);

        assertEquals(toSave, saved.getNodeSettings("test"));
    }

    /**
     * Tests equals and hashCode.
     */
    @Test
    public void testEqualsHashCode() {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("key", "value");

        final NodeSettingsConfigID id = new NodeSettingsConfigID(settings);
        assertEquals(id, id);

        final NodeSettingsConfigID otherIDSameContent = new NodeSettingsConfigID(settings);
        assertEquals(id, otherIDSameContent);
        assertEquals(id.hashCode(), otherIDSameContent.hashCode());

        final NodeSettings otherKeySettings = new NodeSettings("bar");
        settings.addString("key", "value");
        NodeSettingsConfigID otherKeySettingsID = new NodeSettingsConfigID(otherKeySettings);
        assertNotEquals(id, otherKeySettingsID);

        final NodeSettings otherContentSettings = new NodeSettings("test");
        settings.addString("key", "otherValue");
        NodeSettingsConfigID otherContentSettingsID = new NodeSettingsConfigID(otherContentSettings);
        assertNotEquals(id, otherContentSettingsID);

        assertNotEquals(id, "foo");

        assertNotEquals(id, null);
    }

    /**
     * Tests the {@link ConfigID#isCompatible(ConfigID)} implementation.
     */
    @Test
    public void testIsCompatible() {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("key", "value");

        final NodeSettingsConfigID id = new NodeSettingsConfigID(settings);
        assertEquals(true, id.isCompatible(id));

        final NodeSettingsConfigID otherIDSameContent = new NodeSettingsConfigID(settings);
        assertEquals(true, id.isCompatible(otherIDSameContent));

        final NodeSettings otherKeySettings = new NodeSettings("bar");
        settings.addString("key", "value");
        NodeSettingsConfigID otherKeySettingsID = new NodeSettingsConfigID(otherKeySettings);
        assertEquals(false, id.isCompatible(otherKeySettingsID));

        final NodeSettings otherContentSettings = new NodeSettings("test");
        settings.addString("key", "otherValue");
        NodeSettingsConfigID otherContentSettingsID = new NodeSettingsConfigID(otherContentSettings);
        assertEquals(false, id.isCompatible(otherContentSettingsID));

        assertEquals(false, id.isCompatible(Mockito.mock(ConfigID.class)));
    }

}
