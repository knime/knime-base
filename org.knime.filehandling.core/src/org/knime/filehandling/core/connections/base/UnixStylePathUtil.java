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
 *   Sep 2, 2019 (bjoern): created
 */
package org.knime.filehandling.core.connections.base;

/**
 * Utility class to implement basic logic for dealing with UNIX-style paths, in order to avoid reimplementing the logic
 * for each file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public class UnixStylePathUtil {

    /**
     * Separator character for UNIX-style paths.
     */
    public static final String SEPARATOR = "/";

    /**
     * Parses a UNIX-style path String into its components. Does not do any normalization (e.g. removing "..", ".").
     * Everything between two separator characters will become a path component.
     *
     * @param path The path to parse.
     * @return array containing the path components.
     */
    public static String[] toPathComponentsArray(String path) {
        if(path == null) {
            return new String[0];
        }
        // remove leading slash if present (this may leave us with an empty string)
        if (path.startsWith(SEPARATOR)) {
            path = path.substring(1);
        }

        // remove trailing slash if present (this may leave us with an empty string)
        if (path.endsWith(SEPARATOR)) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.isEmpty()) {
            return new String[0];
        } else {
            return path.split(SEPARATOR);
        }
    }

    public static boolean hasRootComponent(final String path) {
        return path != null && path.startsWith(SEPARATOR);
    }

    public static String resolve(final String[] baseComponents, final String[] toResolveComponents,
        final boolean resolveToAbsolute) {

        final String[] resolvedPathComponents = new String[baseComponents.length + toResolveComponents.length];

        System.arraycopy(baseComponents, 0, resolvedPathComponents, 0, baseComponents.length);
        System.arraycopy(toResolveComponents, 0, resolvedPathComponents, baseComponents.length,
            toResolveComponents.length);

        final String resolvedPath =
            (resolveToAbsolute ? SEPARATOR : "") + String.join(SEPARATOR, resolvedPathComponents);
        return resolvedPath;
    }

    /**
     * Replaces all backward slash (Windows separator) in the input path with forward slash (UNIX separator).
     *
     * @param path the input path
     * @return a unix style path
     */
    public static String asUnixStylePath(final String path) {
        return path.replaceAll("\\\\", "/");
    }
}
