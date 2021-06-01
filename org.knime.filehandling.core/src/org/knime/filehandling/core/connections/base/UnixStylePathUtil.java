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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class to implement basic logic for dealing with UNIX-style paths, in order to avoid reimplementing the logic
 * for each file system.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class UnixStylePathUtil {

    /**
     * Separator character for UNIX-style paths.
     */
    public static final String SEPARATOR = "/";

    private UnixStylePathUtil() {

    }

    /**
     * Parses a UNIX-style path String into its components. Does not do any normalization (e.g. removing "..", ".").
     * Everything between two separator characters will become a path component.
     *
     * @param path The path to parse.
     * @return array containing the path components.
     */
    public static String[] toPathComponentsArray(String path) {
        if (path == null) {
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

    /**
     * @param pathParts the path parts to normalize
     * @param isAbsolute <code>true</code> if the input path is absolute otherwise <code>false</code>
     * @return the normalized path parts
     */
    public static LinkedList<String> getNormalizedPathParts(final List<String> pathParts,
        final boolean isAbsolute) {
        final LinkedList<String> normalized = new LinkedList<>();
        boolean stepUp = true;
        for (final String pathComponent : pathParts) {
            if (pathComponent.equals(".")) {
                continue;
            } else if (pathComponent.equals(UnixStylePath.TO_PARENT)) {
                if (normalized.isEmpty() || !stepUp) {
                    if (!isAbsolute) {
                        normalized.add(pathComponent);
                        stepUp = false;
                    }
                } else {
                    normalized.removeLast();
                }
            } else {
                normalized.add(pathComponent);
                stepUp = true;
            }
        }
        return normalized;
    }

    /**
     * Concatenates the given strings into a path string, introducing the path separator between parts where necessary.
     * Empty strings will be ignored.
     *
     * @param separator the file system specific separator
     * @param first first part of the path
     * @param more subsequent parts of the path
     * @return the concatenated string
     */
    public static String concatenatePathSegments(final String separator, final String first, final String... more) {
        final StringBuilder sb = new StringBuilder(first);

        String previousPart = first;
        for (final String currentPart : more) {
            if (currentPart.length() > 0) {
                if (sb.length() > 0 && !previousPart.endsWith(separator)) {
                    sb.append(separator);
                }
                sb.append(currentPart);
                previousPart = currentPart;
            }
        }
        return sb.toString();
    }

    /**
     * Uses the given separator to split the path string into its components.
     *
     * @param separator the file system separator
     * @param pathString the path string
     * @return the path parts
     */
    public static List<String> getPathSplits(final String separator, final String pathString) {
        final ArrayList<String> splitList = new ArrayList<>();
        if (pathString.isEmpty()) {
            // special case: the empty path
            splitList.add("");
        } else {

            Arrays.stream(pathString.split(Pattern.quote(separator))) //
                .filter(c -> !c.isEmpty()) //
                .forEach(splitList::add);
        }
        return splitList;
    }

    /**
     * The parent path name.
     *
     * @param isAbsolute <code>true</code> if the input path is absolute
     * @param pathSeparator the path separator to use
     * @param pathParts the path parts
     * @return the parent path name or <code>null</code> if the given path parts are empty
     */
    public static String getParentPathName(final boolean isAbsolute, final String pathSeparator,
        final List<String> pathParts) {
        if ((isAbsolute && pathParts.isEmpty()) || (!isAbsolute && pathParts.size() <= 1)) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        if (isAbsolute) {
            sb.append(pathSeparator);
        }
        for (int i = 0; i < pathParts.size() - 1; i++) {
            sb.append(pathParts.get(i));
            sb.append(pathSeparator);
        }
        return sb.toString();
    }

}
