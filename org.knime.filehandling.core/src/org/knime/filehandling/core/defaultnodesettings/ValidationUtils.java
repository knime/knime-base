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
 *   May 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.net.URI;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.FSPluginConfig;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * Utility class that contains methods for validating {@link FSPath} objects.</br>
 * This class only exists temporarily since the validation should eventually be done by the individual file systems.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // static utility class
    }

    /**
     * Validates the provided {@link FSLocation}.
     *
     * @param customUrlLocation The {@link FSLocation} to validate.
     * @throws InvalidSettingsException if the custom url is invalid
     */
    public static void validateCustomURLLocation(final FSLocation customUrlLocation) throws InvalidSettingsException {

        CheckUtils.checkArgument(customUrlLocation.getFSCategory() == FSCategory.CUSTOM_URL,
            "Expected custom url location instead received '%s'.", customUrlLocation);
        CheckUtils.checkSetting(customUrlLocation.getFileSystemSpecifier().isPresent(),
            "No timeout specified for custom URL file system.");

        validateCustomURL(customUrlLocation.getPath()); // in a CUSTOM_URL FSLocation the path is actually a URL
    }

    /**
     * Turns the provided string into a path valid for the custom url file system.
     *
     * @param customUrl The custom URL to transform into a path string suited for the Custom URL file system.
     * @return a path string suited for the Custom URL file system (contains path, query and fragment, but not scheme
     *         and authority).
     */
    public static String toCustomURLPathString(final String customUrl) {
        final URI uri = URI.create(customUrl.replace(" ", "%20"));
        return getURIPathQueryAndFragment(uri);
    }

    private static String getURIPathQueryAndFragment(final URI uri) {
        final StringBuilder toReturn = new StringBuilder(uri.getPath());

        if (uri.getQuery() != null) {
            toReturn.append("?");
            toReturn.append(uri.getRawQuery());
        }

        if (uri.getFragment() != null) {
            toReturn.append("#");
            toReturn.append(uri.getRawFragment());
        }
        return toReturn.toString();
    }

    public static void validateCustomURL(final String customUrl) throws InvalidSettingsException {
        final URI uri = URI.create(customUrl.replace(" ", "%20"));

        // validate scheme
        if (!uri.isAbsolute()) {
            throw new InvalidSettingsException("URL must start with a scheme, e.g. http:");
        }

        if (uri.getScheme().equals("file")) {
            validateLocalFsAccess();
        }

        if (uri.isOpaque()) {
            throw new InvalidSettingsException("URL must have forward slash ('/') after scheme, e.g. http://");
        }

        if (uri.getPath() == null) {
            throw new InvalidSettingsException("URL must specify a path, as in https://host/path/to/file");
        }
    }

    /**
     * Validates a path for the relative to file system.
     *
     * @param knimeFSPath to validate
     * @throws InvalidSettingsException if not relative
     */
    public static void validateKnimeFSPath(final FSPath knimeFSPath) throws InvalidSettingsException {
        CheckUtils.checkSetting(!knimeFSPath.isAbsolute(), "The path must be relative.");
    }


    /**
     * Checks if local file system access is valid.
     *
     * @throws InvalidSettingsException if local file system access is invalid
     */
    public static void validateLocalFsAccess() throws InvalidSettingsException {
        if (WorkflowContextUtil.isServerContext() && !FSPluginConfig.load().allowLocalFsAccessOnServer()) {
            throw new InvalidSettingsException(
                "Direct access to the local file system is not allowed on KNIME Server.");
        }
    }
}
