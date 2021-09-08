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
 *   Jun 17, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.meta;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;

/**
 * Instances of this class uniquely identify a particular type of file system, e.g. the "local file system", or the "SSH
 * file system". The "type" of a file system is almost always synonymous with its implementation, but does not have to
 * be (see {@link FSTypeRegistry}).
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class FSType {

    /**
     * The file system type for the local file system.
     */
    public static final FSType LOCAL_FS = FSTypeRegistry.getOrCreateFSType("local", "Local File System");

    /**
     * The file system type for the Mountpoint file system.
     */
    public static final FSType MOUNTPOINT = FSTypeRegistry.getOrCreateFSType("knime-mountpoint", "Mountpoint");

    public static final FSType RELATIVE_TO_WORKFLOW =
        FSTypeRegistry.getOrCreateFSType("knime-relative-workflow", "Relative to current workflow");

    public static final FSType RELATIVE_TO_MOUNTPOINT =
        FSTypeRegistry.getOrCreateFSType("knime-relative-mountpoint", "Relative to current mountpoint");

    public static final FSType RELATIVE_TO_WORKFLOW_DATA_AREA =
        FSTypeRegistry.getOrCreateFSType("knime-relative-workflow-data", "Current workflow data area");

    /**
     * The file system type for the (convenience) Custom/KNIME URL file system.
     */
    public static final FSType CUSTOM_URL = FSTypeRegistry.getOrCreateFSType("knime-url", "Custom/KNIME URL");

    private final String m_typeId;

    private final String m_name;

    /**
     * Creates a new instance.
     *
     * @param typeId The globally unique type identifier of the file system type.
     * @param name A human-readable name for this type of file system
     */
    FSType(final String typeId, final String name) {
        CheckUtils.checkArgument(StringUtils.isNotBlank(typeId), "ID of FSType must not be blank");
        CheckUtils.checkArgument(StringUtils.isNotBlank(name), "Name of FSType must not be blank");
        m_typeId = typeId.trim();
        m_name = name;
    }

    /**
     * Returns the globally unique type identifier of the file system type. Note that a file system implementation may
     * register several {@link FSType}s in different {@link FSCategory categories}. This is for example true for most of
     * the convenience file systems (local, etc). In this case these {@link FSType}s share the same type identifier, but
     * only differ in their category.
     *
     * @return the globally unique type identifier of the file system.
     */
    public String getTypeId() {
        return m_typeId;
    }

    /**
     * @return a nice-looking human readable name for this type of file system (only to be used for display purposes).
     */
    public String getName() {
        return m_name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_typeId == null) ? 0 : m_typeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) { // NOSONAR generated equals method
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FSType other = (FSType)obj;
        if (m_typeId == null) {
            if (other.m_typeId != null) {
                return false;
            }
        } else if (!m_typeId.equals(other.m_typeId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getTypeId();
    }
}
