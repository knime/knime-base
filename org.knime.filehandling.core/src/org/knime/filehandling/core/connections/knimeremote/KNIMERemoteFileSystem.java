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
 *   Nov 11, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.knimeremote;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;

import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.base.BaseFileSystem;
import org.knime.filehandling.core.connections.base.UnixStylePathUtil;
import org.knime.filehandling.core.connections.meta.FSType;
import org.knime.filehandling.core.util.MountPointFileSystemAccessService;

/**
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
final class KNIMERemoteFileSystem extends BaseFileSystem<KNIMERemotePath> {

    static final FSType FS_TYPE = FSType.MOUNTPOINT;

    static final String SEPARATOR = "/";

    private final KNIMERemoteFSConnectionConfig m_config;

    KNIMERemoteFileSystem(final KNIMERemoteFSConnectionConfig config) {
        super(new KNIMERemoteFileSystemProvider(), //
            0, //
            config.getWorkingDirectory(), //
            createFSLocationSpec(config));

        m_config = config;
    }

    private static FSLocationSpec createFSLocationSpec(final KNIMERemoteFSConnectionConfig config) {
        final FSCategory category;
        final String specifier;

        if (config.isConnectedFileSystem()) {
            category = FSCategory.CONNECTED;
            specifier = String.format("%s:%s", FS_TYPE.getTypeId(), config.getMountpoint());
        } else {
            category = FSCategory.MOUNTPOINT;
            specifier = config.getMountpoint();
        }
        return new DefaultFSLocationSpec(category, specifier);
    }

    @Override
    public String getSeparator() {
        return SEPARATOR;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singletonList(new KNIMERemotePath(this, URI.create(UnixStylePathUtil.SEPARATOR)));
    }

    @Override
    public KNIMERemotePath getPath(final String first, final String... more) {
        return new KNIMERemotePath(this, first, more);
    }

    /**
     * Returns the mount point of this remote KNIME file system.
     *
     * @return the mount point of this remote KNIME file system
     */
    String getMountpoint() {
        return m_config.getMountpoint();
    }

    URI getKNIMEProtocolURL() {
        try {
            return new URI("knime", m_config.getMountpoint(), null, null);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Illegal mountpoint: " + m_config.getMountpoint(), ex);
        }
    }

    /**
     * Returns the default directory of this file system.
     *
     * @return the default directory of this file system
     */
    public KNIMERemotePath getDefaultDirectory() {
        return new KNIMERemotePath(this,
            MountPointFileSystemAccessService.instance().getDefaultDirectory(getKNIMEProtocolURL()));
    }

    @Override
    public void prepareClose() {
        //Nothing to do.
    }
}
