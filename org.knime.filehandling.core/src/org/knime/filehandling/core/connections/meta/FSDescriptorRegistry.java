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
 *   May 2, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author bjoern
 */
public final class FSDescriptorRegistry {

    private static final NodeLogger LOG = NodeLogger.getLogger(FSDescriptorRegistry.class);

    private static boolean extensionPointInitialized = false;

    private static final Map<FSType, FSDescriptorProvider> PROVIDERS = new HashMap<>();

    private FSDescriptorRegistry() {
    }

    static synchronized void ensureInitialized() {
        if (!extensionPointInitialized) {
            FSDescriptorProviderExtensionPointHelper.loadRegisteredFSDescriptorProviders() //
                .forEach(FSDescriptorRegistry::registerFSDescriptorProvider);
            extensionPointInitialized = true;
        }
    }

    private static synchronized void registerFSDescriptorProvider(final FSType fstype,
        final FSDescriptorProvider provider) {
        Validate.notNull(fstype, "FSType not allowed to be null");
        Validate.notNull(provider, "FSDescriptorProvider not allowed to be null");

        if (PROVIDERS.containsKey(fstype)) {
            throw new IllegalArgumentException(
                String.format("FSDescriptorProvider for %s has already been registered", fstype));
        } else {
            PROVIDERS.put(fstype, provider);
        }
    }

    public static synchronized Optional<FSDescriptor> getFSDescriptor(final FSType fsType) {
        ensureInitialized();

        Optional<FSDescriptor> toReturn = Optional.empty();
        if (PROVIDERS.containsKey(fsType)) {
            try {
                toReturn = Optional.of(PROVIDERS.get(fsType).getFSDescriptor());
            } catch (RuntimeException e) { // NOSONAR
                LOG.warnWithFormat("Failed to create FSDescriptor for FSType %s: %s", //
                    fsType.getTypeId(), //
                    e.getMessage());
            }
        }

        return toReturn;
    }
}
