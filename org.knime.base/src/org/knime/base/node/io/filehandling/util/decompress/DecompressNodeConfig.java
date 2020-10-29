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
 *   24 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.util.decompress;

import java.util.EnumSet;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Settings configuration for "Decompress Files" node
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class DecompressNodeConfig {

    private static final String CFG_INPUT_FILE = "source_location";

    private static final String CFG_OUTPUT_LOCATION = "destination_location";

    private SettingsModelReaderFileChooser m_inputFileChooserModel;

    private SettingsModelWriterFileChooser m_outputDirChooserModel;

    private static final String BZ2_EXTENSION = "bz2";

    private static final String GZ_EXTENSION = "gz";

    private static final String[] FILE_EXTENSIONS = new String[]{//
        "." + ArchiveStreamFactory.ZIP, //
        "." + ArchiveStreamFactory.JAR, //
        "." + ArchiveStreamFactory.TAR, //
        "." + ArchiveStreamFactory.TAR + "." + GZ_EXTENSION, //
        "." + ArchiveStreamFactory.TAR + "." + BZ2_EXTENSION, //
        "." + ArchiveStreamFactory.CPIO, //
        "." + ArchiveStreamFactory.AR};

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    public DecompressNodeConfig(final PortsConfiguration portsConfig) {
        m_inputFileChooserModel = new SettingsModelReaderFileChooser(CFG_INPUT_FILE, portsConfig,
            DecompressNodeFactory.CONNECTION_INPUT_FILE_PORT_GRP_NAME, FilterMode.FILE, FILE_EXTENSIONS);

        m_outputDirChooserModel = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, portsConfig,
            DecompressNodeFactory.CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, FilterMode.FOLDER, FileOverwritePolicy.IGNORE,
            EnumSet.of(FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.IGNORE, FileOverwritePolicy.FAIL),
            EnumSet.of(FSCategory.LOCAL, FSCategory.MOUNTPOINT, FSCategory.RELATIVE));
    }

    void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputFileChooserModel.validateSettings(settings);
        m_outputDirChooserModel.validateSettings(settings);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_inputFileChooserModel.loadSettingsFrom(settings);
        m_outputDirChooserModel.loadSettingsFrom(settings);
    }

    void saveSettingsForModel(final NodeSettingsWO settings) {
        m_inputFileChooserModel.saveSettingsTo(settings);
        m_outputDirChooserModel.saveSettingsTo(settings);
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} used to select a directory where the files are extracted to.
     *
     * @return the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    SettingsModelWriterFileChooser getOutputDirChooserModel() {
        return m_outputDirChooserModel;
    }

    /**
     * Returns the {@link SettingsModelReaderFileChooser} used to select a directory where the files are extracted to.
     *
     * @return the {@link SettingsModelReaderFileChooser} used to select a directory
     */
    SettingsModelReaderFileChooser getInputFileChooserModel() {
        return m_inputFileChooserModel;
    }
}
