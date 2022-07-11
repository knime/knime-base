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
 *   Jun 21, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.filehandling.core.connections.RelativeTo;

/**
 * Contains tests for {@link FSLocationUtil}.
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
public class FSLocationUtilTest {

    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationNull() {
        assertThrows(IllegalArgumentException.class, () -> FSLocationUtil.createFromURL(null));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationCustomURL() {
        final var url1 = "http://www.knime.com/api/test%20df.pmml";
        assertEquals(convertCustom(url1), FSLocationUtil.createFromURL(url1));

        final var url2 = "ftp://john:doe@knime:9000/test%20df.csv";
        assertEquals(convertCustom(url2), FSLocationUtil.createFromURL(url2));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationRelative() {
        final var urlPath = "/../john%20doe/test%20df.pmml";
        final var path = "../john doe/test df.pmml";

        final Set<RelativeTo> relativeHosts = Arrays.stream(RelativeTo.values()) //
                .filter(r -> r != RelativeTo.WORKFLOW) //
                .collect(Collectors.toSet());

        for (RelativeTo relativeTo : relativeHosts) {
            final var url = "knime://" + relativeTo.getSettingsValue() + urlPath;
            assertEquals(new FSLocation(FSCategory.RELATIVE, //
                relativeTo.getSettingsValue(), //
                path), //
                FSLocationUtil.createFromURL(url));
        }
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationRelativeEmptyPath() {

        final Set<RelativeTo> relativeHosts = Arrays.stream(RelativeTo.values()) //
                .filter(r -> r != RelativeTo.WORKFLOW) //
                .collect(Collectors.toSet());

        for (RelativeTo relativeTo : relativeHosts) {
            final var url = "knime://" + relativeTo.getSettingsValue() + "/";
            assertEquals(new FSLocation(FSCategory.RELATIVE, //
                relativeTo.getSettingsValue(), //
                "."), //
                FSLocationUtil.createFromURL(url));
        }

        for (RelativeTo relativeTo : relativeHosts) {
            final var url = "knime://" + relativeTo.getSettingsValue();
            assertEquals(new FSLocation(FSCategory.RELATIVE, //
                relativeTo.getSettingsValue(), //
                "."), //
                FSLocationUtil.createFromURL(url));
        }

    }


    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationLocalFile() {
        final var url = "file:///Users/john%20doe/test%20df.pmml";
        final var fsLocation = new FSLocation(FSCategory.LOCAL, "/Users/john doe/test df.pmml");

        assertEquals(fsLocation, FSLocationUtil.createFromURL(url));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testUrlToFSLocationMountpoint() {
        final var url = "knime://my-knime/Users/john%20doe/test%20df.pmml";
        final var fsLocation = new FSLocation(FSCategory.MOUNTPOINT, "my-knime", "/Users/john doe/test df.pmml");

        assertEquals(fsLocation, FSLocationUtil.createFromURL(url));
    }

    private static FSLocation convertCustom(final String url) {
        return new FSLocation(FSCategory.CUSTOM_URL, "1000", url);
    }
}
