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
 *   Jun 28, 2020 (bjoern): created
 */
package org.knime.filehandling.core.testing.integrationtests.path;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.StringTokenizer;

import org.junit.Test;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.testing.integrationtests.AbstractParameterizedFSTest;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Tests the Path.toUri() method.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class ToUriTest extends AbstractParameterizedFSTest {

    public ToUriTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    private static void assertEqualNameComponents(final String uriPath, final Path p) {
        assertEquals("/", Character.toString(uriPath.charAt(0)));
        StringTokenizer tok = new StringTokenizer(uriPath.substring(1), "/");
        int currNameComponent = 0;
        while (tok.hasMoreTokens()) {
            final String currToken = tok.nextToken();
            String currNc = p.getName(currNameComponent).toString();
            if (currNc.endsWith("/")) {
                currNc = currNc.substring(0, currNc.length() - 1);
            }
            assertEquals(currNc, currToken);
            currNameComponent++;
        }
    }

    @Test
    public void test_toUri_simple() {
        final Path p = m_testInitializer.makePath("bla").normalize();
        final URI uri = p.toUri();

        assertEqualNameComponents(uri.getPath(), p);
    }

    @Test
    public void test_toUri_with_spaces() {
        final Path p = m_testInitializer.makePath("with some spaces", "bla");
        final URI uri = p.toUri();

        assertEqualNameComponents(uri.getPath(), p);
    }

    @Test
    public void test_toUri_with_hash_signs() {
        final Path p = m_testInitializer.makePath("with#some#hashsigns", "bla");
        final URI uri = p.toUri();

        assertEqualNameComponents(uri.getPath(), p);
    }

    @Test
    public void test_toUri_with_question_marks() {
        final Path p = m_testInitializer.makePath("with?some?questionmarks", "bla");
        final URI uri = p.toUri();

        assertEqualNameComponents(uri.getPath(), p);
    }

    @Test
    public void test_toUri_with_relative_path() {
        final Path p = getFileSystem().getPath("bla");
        final URI uri = p.toUri();

        assertEqualNameComponents(uri.getPath(), p.toAbsolutePath());
    }
}
