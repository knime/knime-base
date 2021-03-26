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
 *   Nov 27, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.pathtostring;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterIDs;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;

/**
 * Utility class for the Path to String nodes.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class PathToStringUtils {

    private PathToStringUtils() {
        // hide constructor for utility class
    }

    /**
     * Creates and returns a KNIME URL using the {@link URIExporter} with id {@link URIExporterIDs#LEGACY_KNIME_URL}.
     *
     * @param fsLocation the {@link FSLocation} to convert, must not be {@code null}
     * @param factory the {@link FSPathProviderFactory}, must not be {@code null}
     * @return the converted {@link FSLocation} string
     */
    @SuppressWarnings("resource") // we don't want to close FSConnection here
    public static String fsLocationToString(final FSLocation fsLocation, final FSPathProviderFactory factory) {
        try (final FSPathProvider pathProvider = factory.create(fsLocation)) {
            final FSConnection fsConnection = pathProvider.getFSConnection();
            final Map<URIExporterID, URIExporterFactory> uriExporters = fsConnection.getURIExporterFactories();
            final URIExporter uriExporter =
                ((NoConfigURIExporterFactory)uriExporters.get(URIExporterIDs.LEGACY_KNIME_URL)).getExporter();
            final FSPath path = pathProvider.getPath();
            return uriExporter.toUri(path).toString();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(String.format("The path '%s' could not be converted to a KNIME URL: %s",
                fsLocation.getPath(), e.getMessage()), e);
        }
    }
}
