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
 *   Sep 3, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knime;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.defaultnodesettings.KNIMEConnection;

/**
 *  File System implementation for the KNIME "relative to" paths.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class KNIMEFileSystem extends BaseFileSystem {

    private static final String SEPERATOR = UnixStylePathUtil.SEPARATOR;

    private final URI m_key;
    private final KNIMEConnection.Type m_connectionType;

    /**
     * Constructs a KNIME file system for relative paths.
     *
     * @param fileSystemProvider the file system provider
     * @param baseLocation the base location of the file system
     * @param connectionType the connection type
     */
    public KNIMEFileSystem(
            final KNIMEFileSystemProvider fileSystemProvider,
            final URI baseLocation,
            final KNIMEConnection.Type connectionType) {
        super(fileSystemProvider, baseLocation, "KNIME File Store", "KNIME File Store", 0);
        m_key = baseLocation;
        m_connectionType = connectionType;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeparator() {
        return SEPERATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        return Paths.get(m_key).getFileSystem().getRootDirectories();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPath(final String first, final String... more) {
        return new KNIMEPath(this, first, more);
    }

    /**
     * Returns the base location path of this file system. Can be a workflow, node or local mount point location.
     *
     * @return the base location path of this file system
     */
    public Path getBasePath() {
        return m_key != null ? Paths.get(m_key) : null;
    }

    /**
     * Returns the URI key of this file system.
     *
     * @return the URI key of this file system.
     */
    public URI getKey() {
        return m_key;
    }

    /**
     * @return the connection type
     */
    public KNIMEConnection.Type getConnectionType() {
        return m_connectionType;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareClose() {
        //Nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSchemeString() {
        return m_key.getScheme();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostString() {
        return m_key.getHost();
    }
}
