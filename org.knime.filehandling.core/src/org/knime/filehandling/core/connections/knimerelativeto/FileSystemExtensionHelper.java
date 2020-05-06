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
package org.knime.filehandling.core.connections.knimerelativeto;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Helper to load {@link FSConnectionProvider} via extension points.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public class FileSystemExtensionHelper {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSystemExtensionHelper.class);

    /** The id of the KNIME server relative to file system provider extension point. */
    public static final String EXT_POINT_ID = "org.knime.filehandling.core.FSConnectionProvider";

    /** The attribute of the file system type in the extension point.*/
    public static final String EXT_POINT_TYPE_ATTR_DF = "type";

    /** The attribute of the provider implementation class name in the extension point.*/
    public static final String EXT_POINT_CLASS_ATTR_DF = "class";

    /** Lazy loaded map with known implementations by type */
    private static Map<String, FSConnectionProvider> CONNECTION_PROVIDER = null;

    private FileSystemExtensionHelper() {}

    /**
     * Get {@link FSConnectionProvider} by type.
     *
     * @param type type of the file system connection provider
     * @return file system connection provider
     * @throws IllegalArgumentException on unknown type / missing provider
     */
    public static synchronized FSConnectionProvider getFSConnectionProvider(final String type) {
        if (CONNECTION_PROVIDER == null) {
            CONNECTION_PROVIDER = loadConnectionProvider();
        }

        final FSConnectionProvider provider = CONNECTION_PROVIDER.get(type);

        if (provider == null) {
            throw new IllegalArgumentException("Unsupported file system type: " + type);
        } else {
            return provider;
        }
    }

    /**
     * Load all registered extension point implementations.
     */
    private static HashMap<String, FSConnectionProvider> loadConnectionProvider() {
        final HashMap<String, FSConnectionProvider> provider = new HashMap<String, FSConnectionProvider>();

        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);

            if (point == null) {
                LOGGER.error("Invalid extension point: " + EXT_POINT_ID);
                throw new IllegalStateException("ACTIVATION ERROR: --> Invalid extension point: " + EXT_POINT_ID);
            }

            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                loadExtension(provider, elem);
            }
        } catch (final Exception e) {
            LOGGER.error("Exception while registering file system provider extension", e);
        }

        if (provider.isEmpty()) {
            throw new IllegalStateException("No useable KNIME server remote relative to file system provider found.");
        } else {
            return provider;
        }
    }

    private static void loadExtension(final HashMap<String, FSConnectionProvider> provider, final IConfigurationElement elem) {
        final String type = elem.getAttribute(EXT_POINT_TYPE_ATTR_DF);
        final String className = elem.getAttribute(EXT_POINT_CLASS_ATTR_DF);
        final String decl = elem.getDeclaringExtension().getUniqueIdentifier();

        if (StringUtils.isBlank(type) || StringUtils.isBlank(className)) {
            LOGGER.error("The extension '" + decl + "' doesn't provide the required attributes '"
                    + EXT_POINT_TYPE_ATTR_DF + "' and '" + EXT_POINT_CLASS_ATTR_DF + "'.");
            LOGGER.error("Extension " + decl + " ignored.");

        } else {
            try {
                provider.put(type, (FSConnectionProvider)elem.createExecutableExtension(EXT_POINT_CLASS_ATTR_DF));
            } catch (final Throwable t) {
                LOGGER.error("Problems during initialization of provider (with id '" + className + "'.)", t);
                if (decl != null) {
                    LOGGER.error("Extension " + decl + " ignored.", t);
                }
            }
        }
    }
}
