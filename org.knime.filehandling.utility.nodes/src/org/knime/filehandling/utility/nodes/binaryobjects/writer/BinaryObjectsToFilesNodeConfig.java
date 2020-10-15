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
 *   Oct 13, 2020 (Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.binaryobjects.writer;

import java.util.EnumSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * A centralized class for encapsulating the SettingsModel Objects. The save, validate and load in NodeModel simply
 * invokes this functionality
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class BinaryObjectsToFilesNodeConfig {

    private static final String CFG_BINARY_COLUMN_NAME = "binary_object_column";

    private static final String CFG_WRITER_FILE_CHOOSER_NAME = "output_location";

    private final SettingsModelString m_binaryObjectsSelectionColumnModel;

    private final SettingsModelWriterFileChooser m_fileWriterSelectionModel;

    /**
     * Constructor for the Configuration class
     *
     * @param portsConfiguration An object of PortsConfiguration Class
     */
    BinaryObjectsToFilesNodeConfig(final PortsConfiguration portsConfiguration) {
        m_binaryObjectsSelectionColumnModel = new SettingsModelString(CFG_BINARY_COLUMN_NAME, null);
        m_fileWriterSelectionModel = new SettingsModelWriterFileChooser(CFG_WRITER_FILE_CHOOSER_NAME,
            portsConfiguration, BinaryObjectsToFilesNodeFactory.CONNECTION_INPUT_PORT_GRP_NAME, FilterMode.FOLDER,
            FileOverwritePolicy.IGNORE,
            EnumSet.of(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
    }

    SettingsModelString getBinaryObjectsSelectionColumnModel() {
        return m_binaryObjectsSelectionColumnModel;
    }

    SettingsModelWriterFileChooser getFileSettingsModelWriterFileChooser() {
        return m_fileWriterSelectionModel;
    }

    /**
     * Implements save settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsWO object from NodeModel
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        m_binaryObjectsSelectionColumnModel.saveSettingsTo(settings);
        m_fileWriterSelectionModel.saveSettingsTo(settings);
    }

    /**
     * Implements validate settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsSelectionColumnModel.validateSettings(settings);
        m_fileWriterSelectionModel.validateSettings(settings);
    }

    /**
     * Implements load validated settings on each individual settings model
     *
     * @param settings Incoming NodeSettingsRO object from NodeModel
     */
    void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_binaryObjectsSelectionColumnModel.loadSettingsFrom(settings);
        m_fileWriterSelectionModel.loadSettingsFrom(settings);
    }

}
