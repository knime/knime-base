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
 *   Mar 25, 2020 (Sascha Wolke, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.meta;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Helper to load {@link FSDescriptorProvider}s via extension points.
 *
 * @noreference non-public API
 */
final class FSDescriptorProviderExtensionPointHelper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FSDescriptorProviderExtensionPointHelper.class);

    /** The id of the extension point where {@link FSDescriptor}s are registered . */
    private static final String EXT_POINT_ID = "org.knime.filehandling.core.FSDescriptorProvider";

    /** The attribute of the provider implementation class name in the extension point.*/
    private static final String EXT_POINT_CLASS_ATTR_DF = "class";

    private FSDescriptorProviderExtensionPointHelper() {}

    /**
     * Load all registered extension point implementations.
     */
    static synchronized Map<FSType, FSDescriptorProvider> loadRegisteredFSDescriptorProviders() {
        final Map<FSType, FSDescriptorProvider> descriptors = new HashMap<>();

        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }

            for (final IConfigurationElement descriptorExtension : point.getConfigurationElements()) {
                addDescriptorIfPossible(descriptorExtension, descriptors);
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while loading extension point " + EXT_POINT_ID, e);
        }

        return descriptors;
    }

    private static void addDescriptorIfPossible(final IConfigurationElement elem, final Map<FSType, FSDescriptorProvider> descriptors) {
        final String className = elem.getAttribute(EXT_POINT_CLASS_ATTR_DF);
        final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

        if (StringUtils.isBlank(className)) {
            LOGGER.error(String.format("The extension '%s' does not provide the required attribute '%s'.", //
                    decl, //
                    EXT_POINT_CLASS_ATTR_DF));
            LOGGER.error(String.format("Extension %s ignored.", decl));
        } else {
            try {
                final FSDescriptorProvider provider = (FSDescriptorProvider)elem.createExecutableExtension(EXT_POINT_CLASS_ATTR_DF);
                if (descriptors.containsKey(provider.getFSType())) {
                    LOGGER.error(String.format("FSDescriptorProvider with FSType %s has already been registered. Ignoring extension with id '%s'.", //
                        provider.getFSType(), //
                        decl));
                } else {
                    descriptors.put(provider.getFSType(), provider);
                }
            } catch (final Throwable t) {
                LOGGER.error(String.format("Problems during initialization of extension with id '%s'.", className), t);
                LOGGER.error(String.format("Extension %s ignored.", decl));
            }
        }
    }
}
