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
 *   Jun 14, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.CheckUtils;

/**
 * Utility class for {@link FSLocation} objects.
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
public final class FSLocationUtil {

    private static final String URL_SCHEME_KNIME = "knime";

    private static final String URL_SCHEME_FILE = "file";

    private static final String DEFAULT_URL_TIMEOUT = "1000";

    private static final Set<String> RELATIVE_HOSTS = Arrays.stream(RelativeTo.values()) //
        .map(RelativeTo::getSettingsValue) //
        .collect(Collectors.toSet());

    private FSLocationUtil() {
        // static utility class
    }

    /**
     * Creates an {@link FSLocation} from a URL. This method provides special handling of file:// and knime:// URLs so
     * that they get converted to {@link FSLocation}s with category {@link FSCategory#LOCAL},
     * {@link FSCategory#RELATIVE} and {@link FSCategory#MOUNTPOINT} respectively. Other URLs are converted to
     * {@link FSCategory#CUSTOM_URL}.
     *
     * @param url the {@link URL} to convert, must not be {@code null}
     * @return the {@link FSLocation}
     * @throws IllegalArgumentException when the provided URL was invalid.
     */
    public static FSLocation createFromURL(final String url) {
        try {
            CheckUtils.checkArgument(!StringUtils.isBlank(url), "URL must not be null or empty");

            final FSLocation toReturn;

            // replace spaces with %20 for backwards compatibility (FileUtil is doing this too)
            final var uri = new URI(url.replace(" ", "%20"));
            final var scheme = uri.getScheme();

            if (scheme.equals(URL_SCHEME_FILE)) {
                toReturn = new FSLocation(FSCategory.LOCAL, Paths.get(uri).toString());

            } else if (scheme.equals(URL_SCHEME_KNIME)) {
                final var host = uri.getHost();
                CheckUtils.checkArgument(!StringUtils.isBlank(host), "Host of knime:// URL must not be null or empty");

                if (RELATIVE_HOSTS.contains(host)) {
                    toReturn = toRelativeFSLocation(RelativeTo.fromSettingsValue(host), //
                        uri.getPath());
                } else {
                    toReturn = new FSLocation(FSCategory.MOUNTPOINT, host, uri.getPath());
                }
            } else {
                toReturn = new FSLocation(FSCategory.CUSTOM_URL, DEFAULT_URL_TIMEOUT, url);
            }

            return toReturn;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static FSLocation toRelativeFSLocation(final RelativeTo type, final String urlPath) {
        var fsLocationPath = urlPath;

        // strip away the leading slash we usually get from the URL's path
        if (fsLocationPath.startsWith("/")) {
            fsLocationPath = fsLocationPath.substring(1);
        }

        // this maps knime://knime.workflow and knime://knime.workflow/ to "."
        if (fsLocationPath.isEmpty()) {
            fsLocationPath = ".";
        }

        return new FSLocation(FSCategory.RELATIVE, type.getSettingsValue(), fsLocationPath);
    }
}
