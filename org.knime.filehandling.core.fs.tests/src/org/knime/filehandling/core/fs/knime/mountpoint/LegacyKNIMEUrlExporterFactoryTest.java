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
 */
package org.knime.filehandling.core.fs.knime.mountpoint;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.config.MountpointFSConnectionConfig;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;
import org.knime.filehandling.core.fs.knime.local.relativeto.fs.LocalRelativeToFileSystemTestBase;

public class LegacyKNIMEUrlExporterFactoryTest extends LocalRelativeToFileSystemTestBase {

    private FSConnection m_fsConnection;

    @Before
    public void setup() throws IllegalStateException, IOException {
        m_fsConnection = FSDescriptorRegistry.getFSDescriptor(FSType.MOUNTPOINT)
                .orElseThrow(() -> new IllegalStateException("Mountpoint file system is not registered"))
                .getConnectionFactory() //
                .createConnection(new MountpointFSConnectionConfig("LOCAL"));
    }

    @Test
    public void get_url_when_hash_sign_in_path() throws URISyntaxException, MalformedURLException {
        get_url_from_path("/somepathwith#hashsign");
    }

    @Test
    public void get_url_when_hash_signs_in_path() throws URISyntaxException, MalformedURLException {
        get_url_from_path("/some#path#with#hash#signs");
    }

    private void get_url_from_path(final String path) throws URISyntaxException, MalformedURLException {
        final FSPath fsPath = m_fsConnection.getFileSystem().getPath(path);

        final URI uri = ((NoConfigURIExporterFactory) m_fsConnection.getURIExporterFactory(URIExporterIDs.DEFAULT)) //
                .getExporter() //
                .toUri(fsPath);

        assertEquals(path, uri.getPath());
    }

}
