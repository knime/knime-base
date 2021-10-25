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
 *   Jun 4, 2021 (bjoern): created
 */
package org.knime.filehandling.core.fs.knime.local.relativeto.fs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;
import org.knime.filehandling.core.fs.knime.local.relativeto.fs.LocalRelativeToFileSystem;
import org.knime.filehandling.core.fs.knime.local.relativeto.fs.LocalRelativeToLegacyKNIMEUrlExporterFactory;
import org.knime.filehandling.core.fs.knime.local.workflowaware.LocalWorkflowAwarePath;

/**
 * Tests the {@link LocalRelativeToLegacyKNIMEUrlExporterFactory} which generates (relative) knime:// URLs.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class LocalRelativeToLegacyKNIMEUrlExporterFactoryTest extends LocalRelativeToFileSystemTestBase {

    private static String toKNIMEProtocolURI(final LocalWorkflowAwarePath path) throws URISyntaxException {
        return LocalRelativeToLegacyKNIMEUrlExporterFactory.getInstance().getExporter().toUri(path).toString();
    }

    @Test
    public void toUriWorkflowRelative() throws IOException, URISyntaxException {
        final LocalRelativeToFileSystem fs = getWorkflowRelativeFS();

        assertEquals("knime://knime.workflow/some-file", toKNIMEProtocolURI(fs.getPath("some-file")));
        assertEquals("knime://knime.workflow/some-file", toKNIMEProtocolURI(fs.getPath("/current-workflow/some-file")));

        assertEquals("knime://knime.workflow/some-path/some-file",
            toKNIMEProtocolURI(fs.getPath("some-path/some-file")));
        assertEquals("knime://knime.workflow/some-path/some-file",
            toKNIMEProtocolURI(fs.getPath("/current-workflow/some-path/some-file")));

        assertEquals("knime://knime.workflow/../some-file", toKNIMEProtocolURI(fs.getPath("../some-file")));

        assertEquals("knime://knime.workflow/../some-path/some-file",
            toKNIMEProtocolURI(fs.getPath("/some-path/some-file")));

        assertEquals("knime://knime.workflow/../some-file",
            toKNIMEProtocolURI(fs.getPath("/current-path/../some-file")));

        assertEquals("knime://knime.workflow/../some-file",
            toKNIMEProtocolURI(fs.getPath("../current-path/../some-file")));
    }

    @Test
    public void toUriMountpointRelative() throws IOException, URISyntaxException {
        final LocalRelativeToFileSystem fs = getMountpointRelativeFS();

        assertEquals("knime://knime.mountpoint/some-file", toKNIMEProtocolURI(fs.getPath("some-file")));

        assertEquals("knime://knime.mountpoint/some-file", toKNIMEProtocolURI(fs.getPath("/some-file")));

        assertEquals("knime://knime.mountpoint/some-path/some-file",
            toKNIMEProtocolURI(fs.getPath("some-path/some-file")));

        assertEquals("knime://knime.mountpoint/some-path/some-file",
            toKNIMEProtocolURI(fs.getPath("/some-path/some-file")));

        assertEquals("knime://knime.mountpoint/some-file",
            toKNIMEProtocolURI(fs.getPath("some-path/../some-file")));

        assertEquals("knime://knime.mountpoint/some-file",
            toKNIMEProtocolURI(fs.getPath("/some-path/../some-file")));
    }
}
