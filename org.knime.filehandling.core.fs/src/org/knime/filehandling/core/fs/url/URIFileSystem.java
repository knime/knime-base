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
package org.knime.filehandling.core.fs.url;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.config.URIFSConnectionConfig;

/**
 * Pseudo-filesystem that allows to read user-provided URLs.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class URIFileSystem extends BaseFileSystem<URIPath> {

    static final String PATH_SEPARATOR = "/";

    private final String m_baseUri;

    URIFileSystem(final URIFSConnectionConfig config) {
        super(new URIFileSystemProvider(config.getTimeout()), //
            0L, //
            PATH_SEPARATOR, //
            createFSLocationSpec(config));

        m_baseUri = toBaseURI(config.getURI());
    }

    /**
     * Creates an {@link FSLocationSpec} for the given configuration.
     *
     * @param config The file system configuration.
     * @return an {@link FSLocationSpec} for the given configuration.
     */
    static FSLocationSpec createFSLocationSpec(final URIFSConnectionConfig config) {
        return new DefaultFSLocationSpec(FSCategory.CUSTOM_URL, Long.toString(config.getTimeout().toMillis()));
    }

    /**
     * Retains everything to the left of the URI's path.
     *
     * @return the base URL (same scheme and authority, but no path, query or fragment.
     */
    private static String toBaseURI(final URI uri) {
        final String uriPath = CheckUtils.checkNotNull(uri.getRawPath(), "URL must specify a path");
        final String ssp = uri.getRawSchemeSpecificPart();
        final String nonPathSSP = ssp.substring(0, ssp.lastIndexOf(uriPath));
        return String.format("%s:%s", uri.getScheme(), nonPathSSP);
    }

    String getBaseURI() {
        return m_baseUri;
    }

    private boolean isRelativeKNIMEProtocol() {
        return m_baseUri.equalsIgnoreCase("knime://knime.mountpoint") //
            || m_baseUri.equalsIgnoreCase("knime://knime.workflow") //
            || m_baseUri.equalsIgnoreCase("knime://knime.node");
    }

    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(getPath(PATH_SEPARATOR));
    }

    @Override
    public URIPath getPath(final FSLocation fsLocation) {
        final URI uri = URI.create(fsLocation.getPath().replace(" ", "%20"));

        String pathString = uri.getPath();
        if (isRelativeKNIMEProtocol()) {
            // for a knime:// URL, the URL path comes absolute but we need to translate it to
            // a relative path by deleting the leading slash
            pathString = pathString.substring(1);
        }

        return new URIPath(this, pathString, uri);
    }

    @Override
    public URIPath getPath(final String first, final String... more) {
        return new URIPath(this, first, more);
    }

    @Override
    public void prepareClose() {
        // Nothing to do
    }
}
