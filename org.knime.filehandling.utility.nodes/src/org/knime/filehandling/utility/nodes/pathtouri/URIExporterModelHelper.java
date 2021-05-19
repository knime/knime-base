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
 *   Mar 23, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.utility.nodes.pathtouri;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterConfig;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;

/**
 * Concrete implementation of the {@link AbstractURIExporterHelper} to assist in the node model.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 * @author Bjoern Lohrmann, KNIME GmbH
 */
@SuppressWarnings("javadoc")
final class URIExporterModelHelper extends AbstractURIExporterHelper {

    private NodeSettingsRO m_nodeSettings;

    URIExporterModelHelper(final SettingsModelString selectedColumn, //
        final SettingsModelString selectedUriExporterModel, //
        final int fileSystemPortIndex, //
        final int dataTablePortIndex) {

        super(selectedColumn, selectedUriExporterModel, fileSystemPortIndex, dataTablePortIndex);
    }

    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_nodeSettings != null) {
            m_nodeSettings.copyTo(settings);
        }
    }

    @Override
    void loadSettingsFrom(final NodeSettingsRO exporterConfig) throws InvalidSettingsException {
        //store a local copy so that settings can be loaded
        m_nodeSettings = exporterConfig;
    }

    URIExporter createExporter(final FSConnection connection) throws InvalidSettingsException {
        final URIExporterFactory factory = connection.getURIExporterFactory(getSelectedExporterId());
        final URIExporterConfig config = factory.initConfig();
        config.loadSettingsForExporter(m_nodeSettings);
        return factory.createExporter(config);
    }

    FSLocationSpec getPathColumnFSLocationSpec() {
        return getPathColumnSpec().getMetaDataOfType(FSLocationValueMetaData.class) //
            .orElseThrow(IllegalStateException::new);
    }
}
