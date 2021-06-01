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
 *   27 Aug 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.truncator.TruncationSettings;

/**
 * Abstract configuration of the "Compress Files/Folder" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @param <T> an instance of {@link TruncationSettings}
 */
public abstract class AbstractCompressNodeConfig<T extends TruncationSettings> {

    /** The source file system connection port group name. */
    public static final String CONNECTION_INPUT_FILE_PORT_GRP_NAME = "Source File System Connection";

    /** The destination file system connection port group name. */
    public static final String CONNECTION_OUTPUT_DIR_PORT_GRP_NAME = "Destination File System Connection";

    /** Config key for the TruncationSettings. */
    protected static final String CFG_TRUNCATE_OPTION = "archive_entry_path";

    /** Config key for the compress policy. */
    private static final String CFG_COMPRESS_POLICY = "if_path_exists";

    static final String INVALID_EXTENSION_ERROR =
        "Invalid destination file extension. Please find the valid extensions in the node description.";

    private static final String CFG_OUTPUT_LOCATION = "destination_location";

    private static final String CFG_INCLUDE_EMPTY_FOLDERS = "include_empty_folders";

    private static final String CFG_COMPRESSION = "compression";

    private final SettingsModelWriterFileChooser m_destinationFileChooserModel;

    private final SettingsModelString m_compressionModel;

    private final T m_truncationSettings;

    private final SettingsModelBoolean m_includeEmptyFolders;

    static final String BZ2_EXTENSION = "bz2";

    static final String GZ_EXTENSION = "gz";

    static final String[] COMPRESSIONS = new String[]{//
        ArchiveStreamFactory.ZIP, //
        ArchiveStreamFactory.JAR, //
        ArchiveStreamFactory.TAR, //
        ArchiveStreamFactory.TAR + "." + GZ_EXTENSION, //
        ArchiveStreamFactory.TAR + "." + BZ2_EXTENSION, //
        ArchiveStreamFactory.CPIO};

    /** The default compression is zip. */
    private static final String DEFAULT_COMPRESSION = COMPRESSIONS[0];

    /** The compress policy settings model. */
    private final SettingsModelString m_compressPolicyModel;

    /**
     * Constructor.
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     * @param truncationSettings the {@link TruncationSettings}
     */
    protected AbstractCompressNodeConfig(final PortsConfiguration portsConfig, final T truncationSettings) {
        m_destinationFileChooserModel =
            new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, portsConfig, CONNECTION_OUTPUT_DIR_PORT_GRP_NAME,
                EnumConfig.create(FilterMode.FILE), EnumConfig.create(FileOverwritePolicy.IGNORE), COMPRESSIONS);
        m_compressionModel = new SettingsModelString(CFG_COMPRESSION, DEFAULT_COMPRESSION);
        m_truncationSettings = truncationSettings;
        m_includeEmptyFolders = new SettingsModelBoolean(CFG_INCLUDE_EMPTY_FOLDERS, true);
        m_compressPolicyModel = new SettingsModelString(CFG_COMPRESS_POLICY, CompressPolicy.getDefault().name()) {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                // no need to catch NPE since the settings model never returns a null value
                try {
                    CompressPolicy.valueOf(settings.getString(getKey()));
                } catch (final IllegalArgumentException e) {
                    throw new InvalidSettingsException(
                        String.format("There is no compress policy associated with %s", settings.getString(getKey())),
                        e);
                }
            }

        };
    }

    final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        validateAdditionalSettingsForModel(settings);
        m_destinationFileChooserModel.validateSettings(settings);
        m_compressionModel.validateSettings(settings);
        m_truncationSettings.validateSettingsForModel(settings);
        m_includeEmptyFolders.validateSettings(settings);
        m_compressPolicyModel.validateSettings(settings);
    }

    /**
     * Validates any additional settings introduced by extending classes.
     *
     * @param settings the settings to be validated
     * @throws InvalidSettingsException - If the validation failed
     */
    protected abstract void validateAdditionalSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadAdditionalSettingsForModel(settings);
        m_destinationFileChooserModel.loadSettingsFrom(settings);
        m_compressionModel.loadSettingsFrom(settings);
        m_truncationSettings.loadSettingsForModel(settings);
        m_includeEmptyFolders.loadSettingsFrom(settings);
        m_compressPolicyModel.loadSettingsFrom(settings);
    }

    /**
     * Loads any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings storing the additional options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected abstract void loadAdditionalSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    final void saveSettingsForModel(final NodeSettingsWO settings) {
        saveAdditionalSettingsForModel(settings);
        m_destinationFileChooserModel.saveSettingsTo(settings);
        m_compressionModel.saveSettingsTo(settings);
        m_truncationSettings.saveSettingsForModel(settings);
        m_includeEmptyFolders.saveSettingsTo(settings);
        m_compressPolicyModel.saveSettingsTo(settings);
    }

    /**
     * Stores any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings to save the options to
     */
    protected abstract void saveAdditionalSettingsForModel(NodeSettingsWO settings);

    /**
     * Returns the {@link SettingsModelWriterFileChooser} used to select where to save the archive file.
     *
     * @return the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    final SettingsModelWriterFileChooser getTargetFileChooserModel() {
        return m_destinationFileChooserModel;
    }

    /**
     * Returns the {@link SettingsModelString} storing the selected compression.
     *
     * @return the {@link SettingsModelString} storing the selected compression
     */
    final SettingsModelString getCompressionModel() {
        return m_compressionModel;
    }

    /**
     * Returns the truncation settings.
     *
     * @return the truncation settings
     */
    public final T getTruncationSettings() {
        return m_truncationSettings;
    }

    /**
     * Returns the {@link SettingsModelBoolean} storing the include empty folders flag.
     *
     * @return the {@link SettingsModelBoolean} storing the include empty folders flag
     */
    final SettingsModelBoolean includeEmptyFoldersModel() {
        return m_includeEmptyFolders;
    }

    /**
     * Returns the settings model storing the selected {@link CompressPolicy}.
     *
     * @return the settings model storing the selected {@link CompressPolicy}
     */
    final SettingsModelString getCompressPolicyModel() {
        return m_compressPolicyModel;
    }

    /**
     * Returns the selected {@link CompressPolicy}.
     *
     * @return the selected {@link CompressPolicy}
     */
    final CompressPolicy getCompressPolicy() {
        return CompressPolicy.valueOf(m_compressPolicyModel.getStringValue());
    }

}
