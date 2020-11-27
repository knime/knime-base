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
 *   Nov 27, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.uriexport;

import org.knime.filehandling.core.connections.FSConnection;

/**
 * Declares IDs for of {@link URIExporter}s which are used by various file system implementations.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class URIExporterIDs {

    /**
     * ID of the default {@link URIExporter}. Every {@link FSConnection} provides this exporter.
     */
    public static final URIExporterID DEFAULT = new URIExporterID("default");

    /**
     * ID of an {@link URIExporter} that maps to a URI, which contains the path in URI-compatible notation. Please see
     * {@link PathURIExporter}. Every {@link FSConnection} provides this exporter.
     */
    public static final URIExporterID PATH = new URIExporterID("knime-path");

    /**
     * ID of an {@link URIExporter} that maps to legacy knime:// URLs (for example knime://knime.workflow/...). This
     * {@link URIExporter} is available on several, but not all {@link FSConnection}s. This exporter is only available
     * on certaub {@link FSConnection}s.
     */
    public static final URIExporterID LEGACY_KNIME_URL = new URIExporterID("knime-legacy-url");

    /**
     * ID of an {@link URIExporter} that provides a URI which is compatible with the Apache Hadoop file system API. This
     * exporter is available for all file systems provided by KNIME big data extensions but potentially also by others.
     * All connector nodes that are supposes to be connected to big data nodes must provide this {@link URIExporter}.
     */
    public static final URIExporterID DEFAULT_HADOOP = new URIExporterID("default_hadoop");

    private URIExporterIDs() {
    }
}
