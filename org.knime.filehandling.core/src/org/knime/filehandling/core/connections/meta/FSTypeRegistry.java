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
 *   Feb 4, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.meta;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry class that maps string
 *
 * <p>
 * In corner cases, a particular file system implementation can register itself under several types in the
 * {@link FSTypeRegistry}, but also for a given type/
 * <p>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class FSTypeRegistry {

    private static final Map<String, FSType> TYPES = new HashMap<>();

    private FSTypeRegistry() {
    }

    public static synchronized FSType getOrCreateFSType(final String typeId, final String name) {
        FSType fsType = TYPES.get(typeId); // NOSONAR computeIfAbsent causes ConcurrentModificationException due to FSType constants
        if (fsType == null) {
            fsType = new FSType(typeId, name);
            TYPES.put(typeId, fsType);
        }
        return fsType;
    }

    public static Optional<FSType> getFSType(final String key) {
        FSDescriptorRegistry.ensureInitialized();

        // in order to avoid deadlocks we synchronize AFTER the FSDescriptorRegistry
        // is initialized, because a concurrent code path initializing the
        // FSDescriptorRegistry may need to get/create FSTypes.
        synchronized (FSTypeRegistry.class) {
            return Optional.ofNullable(TYPES.get(key));
        }
    }

    public static Set<FSType> getFSTypes() {
        FSDescriptorRegistry.ensureInitialized();

        // in order to avoid deadlocks we synchronize AFTER the FSDescriptorRegistry
        // is initialized, because a concurrent code path initializing the
        // FSDescriptorRegistry may need to get/create FSTypes.
        synchronized (FSTypeRegistry.class) {
            return Collections.unmodifiableSet(new HashSet(TYPES.values()));
        }
    }
}
