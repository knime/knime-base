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
 *   Mar 3, 2021 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.connections.uriexport;

import org.knime.filehandling.core.connections.uriexport.noconfig.EmptyURIExporterConfig;

/**
 * Factory class to create {@link URIExporterConfig}, {@link URIExporter} and {@link URIExporterPanel} instances.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @since 4.3
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface URIExporterFactory {

    /**
     * Creates a new {@link URIExporterPanel} instance that loads and saves its settings through the given config.
     *
     * @param config The {@link URIExporterConfig} instance that the panel will use to load and save settings.
     * @return URIExporterPanel
     */
    URIExporterPanel createPanel(URIExporterConfig config);

    /**
     * Creates an new {@link URIExporter} instance that consumes the settings of the given config.
     *
     * @param config A {@link URIExporterConfig} instance that holds the exporter's settings. It is the responsibililty
     *            of the caller to properly initialize the config (see Javadoc of {@link URIExporterConfig}).
     * @return URIExporter A new {@link URIExporter} that operates based on the given config.
     */
    URIExporter createExporter(URIExporterConfig config);

    /**
     * Provides a {@link URIExporterConfig} instance that is initialized with defaults values. Note that implementations
     * are allowed to always return the same instance if the concrete {@link URIExporterConfig} implementation is
     * stateless and immutable (see {@link EmptyURIExporterConfig}). Otherwise implementations must always return a new
     * instance.
     *
     * @return a new {@link URIExporterConfig} instance that is initialized with defaults values.
     */
    URIExporterConfig initConfig();

    /**
     * @return meta information about this exporter, such as a description.
     */
    URIExporterMetaInfo getMetaInfo();
}
