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
package org.knime.filehandling.core.fs.tests.integration.uriexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assume;
import org.junit.Test;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.meta.FSDescriptorRegistry;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;
import org.knime.filehandling.core.fs.tests.integration.AbstractParameterizedFSTest;
import org.knime.filehandling.core.testing.FSTestInitializer;
import org.knime.filehandling.core.util.IOESupplier;

/**
 * Test class for testing {@link URIExporter}s that generate file:// URIs.
 *
 * @author Bjoern Lohrmann, KNIME GmbH, Berlin, Germany
 */
public class FileURIExporterTest extends AbstractParameterizedFSTest {

    public FileURIExporterTest(final String fsType, final IOESupplier<FSTestInitializer> testInitializer) throws IOException {
        super(fsType, testInitializer);
    }

    @Test
    public void test_file_uri_exporter() throws IOException, URISyntaxException {
        Assume.assumeTrue(m_connection.getURIExporterIDs().contains(URIExporterIDs.KNIME_FILE));

        final FSPath path = m_testInitializer.createFileWithContent("bla", "file");

        final URIExporterFactory fileFactory = m_connection.getURIExporterFactory(URIExporterIDs.KNIME_FILE);
        assertNotNull(fileFactory);
        assertTrue(fileFactory instanceof NoConfigURIExporterFactory);

        final URI fileUri = ((NoConfigURIExporterFactory) fileFactory).getExporter().toUri(path);
        assertEquals("file", fileUri.getScheme());
        assertEquals("bla", Files.readString(Paths.get(fileUri)));
    }

    @Test
    public void test_same_file_exporter_on_fsdescriptor() {
        Assume.assumeTrue(m_connection.getURIExporterIDs().contains(URIExporterIDs.KNIME_FILE));

        final URIExporterID id = URIExporterIDs.KNIME_FILE;

        final URIExporterFactory connectionDefaultFactory = m_connection.getURIExporterFactory(id);

        final URIExporterFactory descriptorDefaultFactory = FSDescriptorRegistry.getFSDescriptor(m_connection.getFSType()) //
                .get() //
                .getURIExporterFactory(id);
        assertTrue(connectionDefaultFactory.getClass() == descriptorDefaultFactory.getClass());
    }
}
