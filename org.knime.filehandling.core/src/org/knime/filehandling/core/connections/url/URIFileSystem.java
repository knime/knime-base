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
 *   Aug 28, 2019 (bjoern): created
 */
package org.knime.filehandling.core.connections.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class URIFileSystem extends BaseFileSystem<URIPath> {

    private static final String PATH_SEPARATOR = "/";

    private final URI m_baseUri;

    private final URIPath m_workingDirectory;

    URIFileSystem(final URIFileSystemProvider provider, final URI uri, final boolean isConnectedFs) {
        super(provider,//
            toBaseURI(uri),//
            getName(uri),//
            getName(uri),//
            0L,//
            isConnectedFs ? Choice.CONNECTED_FS : Choice.CUSTOM_URL_FS,//
            Optional.empty());

        m_baseUri = toBaseURI(uri);
        m_workingDirectory = getPath(PATH_SEPARATOR);
    }

    /**
     * Retains the scheme and authority from the URL and removes everything else. The path of the returned base URI is
     * simply "/".
     *
     * @return the base URI (same scheme and authority, but no path, query or fragment.
     */
    private static URI toBaseURI(final URI uri) {
        try {
            // special case: file:/path/to/file
            // here authority is null and the scheme-specific-part is /path/to/file
            if (uri.getAuthority() == null) {
                return new URI(uri.getScheme() + ":///");
            } else {
                return new URI(uri.getScheme(), uri.getAuthority(), "/", null, null);
            }
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public URI getBaseURI() {
        return m_baseUri;
    }

    private static String getName(final URI uri) {

        if (uri.getScheme() == null && uri.getAuthority() == null) {
            return "";
        } else {
            return String.format("%s://%s", uri.getScheme(), uri.getAuthority());
        }
    }

    public boolean isRelativeKNIMEProtocol() {
        final String hostString = getHostString();
        return getSchemeString().equalsIgnoreCase("knime")
                && (hostString.equalsIgnoreCase("knime.mountpoint") //
                        || getHostString().equalsIgnoreCase("knime.workflow") //
                        || getHostString().equalsIgnoreCase("knime.node"));
    }


    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public URIPath getWorkingDirectory() {
        return m_workingDirectory;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.singleton("basic");
    }

    @Override
    public URIPath getPath(final String first, final String... more) {
        return new URIPath(this, first, more);
    }

    @Override
    public void prepareClose() {
        // Nothing to do
    }

    @Override
    public String getSchemeString() {
        return m_baseUri.getScheme();
    }

    @Override
    public String getHostString() {
        return m_baseUri.getAuthority();
    }
}
