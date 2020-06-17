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
 *   May 7, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.util.Optional;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.config.ConfigRO;

/**
 * Default implementation of {@link FSLocationSpec}, to be used when there is no other object available. This class is
 * mostly only useful for file system implementations.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @since 4.2
 */
public class DefaultFSLocationSpec implements FSLocationSpec {

    /** The file system type. */
    private final String m_fsCategory;

    /** The optional file system specifier. */
    private final Optional<String> m_fileSystemSpecifier;

    /**
     * Creates a new instance.
     *
     * @param fsCategory The file system type.
     * @param fileSystemSpecifier The optional file system specifier (may be null).
     */
    public DefaultFSLocationSpec(final String fsCategory, final String fileSystemSpecifier) {
        m_fsCategory = fsCategory;
        m_fileSystemSpecifier = Optional.ofNullable(fileSystemSpecifier);
    }

    /**
     * Creates a new instance with an empty file system specifier.
     *
     * @param fsCategory The file system category.
     */
    public DefaultFSLocationSpec(final String fsCategory) {
        this(fsCategory, null);
    }

    /**
     * Creates a new instance.
     *
     * @param fsCategory The file system type as a {@link FSCategory}.
     * @param fileSystemSpecifier The optional file system specifier (may be null).
     */
    public DefaultFSLocationSpec(final FSCategory fsCategory, final String fileSystemSpecifier) {
        this(fsCategory.toString(), fileSystemSpecifier);
    }

    /**
     * Creates a new instance with an empty file system specifier.
     *
     * @param fsCategory The file system type as a {@link FSCategory}.
     */
    public DefaultFSLocationSpec(final FSCategory fsCategory) {
        this(fsCategory, null);
    }

    @Override
    public String getFileSystemCategory() {
        return m_fsCategory;
    }

    @Override
    public Optional<String> getFileSystemSpecifier() {
        return m_fileSystemSpecifier;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof DefaultFSLocationSpec) {
            return FSLocationSpec.areEqual(this, (DefaultFSLocationSpec)obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()//
                .append(m_fsCategory)//
                .append(m_fileSystemSpecifier)//
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("(").append(m_fsCategory);
        if (m_fileSystemSpecifier.isPresent()) {
            sb.append(", ").append(m_fileSystemSpecifier.get());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Default implementation of {@link FSLocationSpec.FSLocationSpecSerializer} that creates
     * {@link DefaultFSLocationSpec} instances.
     *
     * @author Bjoern Lohrmann, KNIME GmbH
     */
    public static final class DefaultFSLocationSpecSerializer extends FSLocationSpecSerializer<FSLocationSpec> {
        @Override
        public DefaultFSLocationSpec createFSLocationSpec(final String fileSystemType, final String fileSystemSpecifier,
            final ConfigRO config) {
            return new DefaultFSLocationSpec(fileSystemType, fileSystemSpecifier);
        }
    }
}
