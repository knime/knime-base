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
 *   19.12.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;

import org.apache.commons.lang3.Validate;

/**
 * Base implementation for the {@link FileStore}. This implementation does not support any FileStoreAttributeViews.
 * Calling {@link #getAttribute} will lead to an IOException.
 *
 * @author Mareike Hoeger, KNIME GmbH
 * @since 4.2
 */
public class BaseFileStore extends FileStore {

    private final String m_type;

    private final String m_name;

    private final boolean m_readOnly;

    private final long m_totalSpace;

    private final long m_usableSpace;

    /**
     * Constructs a FileStore object with the given type and name. The FileStore is set to be not read only, and have
     * Long.MAX_VALUE capacity and free space.
     *
     * @param type String indicating the type of the {@link FileStore}
     * @param name String indicating the name of the {@link FileStore}
     */
    public BaseFileStore(final String type, final String name) {
        this(type, name, false, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * Constructs a FileStore object with the given type and name. Sets the read Only flag and the total and usable
     * space.
     *
     * @param type String indicating the type of the {@link FileStore}
     * @param name String indicating the name of the {@link FileStore}
     * @param readOnly whether the {@link FileStore} is read only
     * @param totalSpace the total available space in the {@link FileStore}
     * @param usableSpace the usable space in the {@link FileStore}
     */
    public BaseFileStore(final String type, final String name, final boolean readOnly, final long totalSpace,
        final long usableSpace) {
        Validate.notNull(name, "Name must not be null.");
        Validate.notNull(type, "Type must not be null.");

        m_type = type;
        m_name = name;
        m_readOnly = readOnly;
        m_totalSpace = totalSpace;
        m_usableSpace = usableSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return m_readOnly;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalSpace() throws IOException {
        return m_totalSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsableSpace() throws IOException {
        return m_usableSpace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        return getUsableSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(final Class<? extends FileAttributeView> type) {
        return type == BasicFileAttributeView.class || type == PosixFileAttributeView.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsFileAttributeView(final String name) {
        return name.equals("basic") || name.equals("posix");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(final Class<V> type) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(final String attribute) throws IOException {
        throw new IOException(String.format("Attributes are not supported for the FileStore %s.", m_name));
    }

}
