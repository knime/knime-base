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

import java.util.EnumSet;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncatePathOption;

/**
 * Node configuration of the "Compress Files/Folder" node.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractCompressNodeConfig {

    /** The source file system connection port group name. */
    public static final String CONNECTION_INPUT_FILE_PORT_GRP_NAME = "Source File System Connection";

    /** The destination file system connection port group name. */
    public static final String CONNECTION_OUTPUT_DIR_PORT_GRP_NAME = "Destination File System Connection";

    static final String INVALID_EXTENSION_ERROR =
        "Invalid destination file extension. Please find the valid extensions in the node description.";

    private static final String CFG_OUTPUT_LOCATION = "destination_location";

    private static final String CFG_TRUNCATE_PATH_OPTION = "source_folder_truncation";

    private static final String CFG_TRUNACTE_REGEX = "truncate_regex";

    private static final String CFG_FLATTEN_HIERARCHY = "flatten_hierarchy";

    private static final String CFG_INCLUDE_EMPTY_FOLDERS = "include_empty_folders";

    private static final String CFG_COMPRESSION = "compression";

    private final SettingsModelWriterFileChooser m_destinationFileChooserModel;

    private final SettingsModelString m_compressionModel;

    private TruncatePathOption m_truncatePathOption;

    private SettingsModelString m_truncateRegex;

    private boolean m_flattenHierarchy;

    private boolean m_includeEmptyFolders;

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

    private static final String DEFAULT_REGEX = ".*";

    /**
     * Constructor
     *
     * @param portsConfig {@link PortsConfiguration} of the node
     */
    protected AbstractCompressNodeConfig(final PortsConfiguration portsConfig) {
        m_destinationFileChooserModel = new SettingsModelWriterFileChooser(CFG_OUTPUT_LOCATION, portsConfig,
            CONNECTION_OUTPUT_DIR_PORT_GRP_NAME, FilterMode.FILE, FileOverwritePolicy.FAIL,
            EnumSet.of(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE), COMPRESSIONS);
        m_compressionModel = new SettingsModelString(CFG_COMPRESSION, DEFAULT_COMPRESSION);
        m_truncatePathOption = TruncatePathOption.getDefault();
        m_truncateRegex = new SettingsModelString(CFG_TRUNACTE_REGEX, DEFAULT_REGEX) {

            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                super.validateSettingsForModel(settings);
                final String val = settings.getString(getKey());
                if (val == null || val.isEmpty()) {
                    throw new InvalidSettingsException("The truncation regular expression cannot be empty");
                }
            }
        };
        m_flattenHierarchy = false;
        m_includeEmptyFolders = true;
    }

    final void loadSettingsForDialog(final NodeSettingsRO settings) {
        setTrunacePathOption(TruncatePathOption
            .valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION, TruncatePathOption.getDefault().name())));
        includeEmptyFolders(settings.getBoolean(CFG_INCLUDE_EMPTY_FOLDERS, false));
        flattenHierarchy(settings.getBoolean(CFG_FLATTEN_HIERARCHY, false));
    }

    final void saveSettingsForDialog(final NodeSettingsWO settings) {
        saveNonSettingModelParameters(settings);
    }

    private void saveFlattenHierarchy(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_FLATTEN_HIERARCHY, flattenHierarchy());
    }

    private void saveSkipFolders(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_INCLUDE_EMPTY_FOLDERS, includeEmptyFolders());
    }

    final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_destinationFileChooserModel.validateSettings(settings);
        m_compressionModel.validateSettings(settings);
        validateTruncatePathOption(settings);
        settings.getBoolean(CFG_FLATTEN_HIERARCHY);
        validateAdditionalSettingsForModel(settings);
    }

    /**
     * Validates the truncate options.
     *
     * @param settings the settings to validate
     * @throws InvalidSettingsException - If the truncate options validation failed
     */
    protected void validateTruncatePathOption(final NodeSettingsRO settings) throws InvalidSettingsException {
        TruncatePathOption.valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION));
        settings.getBoolean(CFG_INCLUDE_EMPTY_FOLDERS);
        m_truncateRegex.validateSettings(settings);
    }

    /**
     * Validates any additional settings introduced by extending classes.
     *
     * @param settings the settings to be validated
     * @throws InvalidSettingsException - If the validation failed
     */
    protected abstract void validateAdditionalSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    final void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_destinationFileChooserModel.loadSettingsFrom(settings);
        m_compressionModel.loadSettingsFrom(settings);
        flattenHierarchy(settings.getBoolean(CFG_FLATTEN_HIERARCHY));
        loadAdditionalSettingsForModel(settings);
        // this method has to be called after #loadAdditionalSettingsForModel due to backwards compatibility
        loadTruncatePathOptionInModel(settings);
    }

    /**
     * Loads any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings storing the additional options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected abstract void loadAdditionalSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads the truncation path options in the node model. It is ensure that this method is called after invoking
     * {@link #loadAdditionalSettingsForModel(NodeSettingsRO)}.
     *
     * @param settings the setting storing the options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected void loadTruncatePathOptionInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        setTrunacePathOption(TruncatePathOption.valueOf(settings.getString(CFG_TRUNCATE_PATH_OPTION)));
        includeEmptyFolders(settings.getBoolean(CFG_INCLUDE_EMPTY_FOLDERS));
        m_truncateRegex.loadSettingsFrom(settings);
    }

    final void saveSettingsForModel(final NodeSettingsWO settings) {
        saveNonSettingModelParameters(settings);
        m_destinationFileChooserModel.saveSettingsTo(settings);
        m_compressionModel.saveSettingsTo(settings);
        m_truncateRegex.saveSettingsTo(settings);
        saveAdditionalSettingsForModel(settings);
    }

    /**
     * Stores any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings to save the options to
     */
    protected abstract void saveAdditionalSettingsForModel(NodeSettingsWO settings);

    private void saveNonSettingModelParameters(final NodeSettingsWO settings) {
        saveTruncateOptions(settings);
        saveFlattenHierarchy(settings);
        saveSkipFolders(settings);
    }

    private void saveTruncateOptions(final NodeSettingsWO settings) {
        settings.addString(CFG_TRUNCATE_PATH_OPTION, getTruncatePathOption().name());
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} used to select where to save the archive file.
     *
     * @return the {@link SettingsModelWriterFileChooser} used to select a directory
     */
    final SettingsModelWriterFileChooser getTargetFileChooserModel() {
        return m_destinationFileChooserModel;
    }

    final SettingsModelString getCompressionModel() {
        return m_compressionModel;
    }

    final SettingsModelString getTruncateRegexModel() {
        return m_truncateRegex;
    }

    /**
     * Sets the {@link TruncatePathOption}
     *
     * @param truncatePathOption the option to set
     */
    public final void setTrunacePathOption(final TruncatePathOption truncatePathOption) {
        m_truncatePathOption = truncatePathOption;
    }

    final TruncatePathOption getTruncatePathOption() {
        return m_truncatePathOption;
    }

    /**
     * Sets the flag indicating whether or not to flatten the hierarchy during compression.
     *
     * @param flattenHierarchy {@code true} if the hierarchy has to be flattened during compression and {@code false}
     *            otherwise
     */
    final void flattenHierarchy(final boolean flattenHierarchy) {
        m_flattenHierarchy = flattenHierarchy;
    }

    /**
     * Returns the flag deciding whether or not to flatten the hierarchy during compression.
     *
     * @return {code true} if the hierarchy has to be flattened during compression and {@code false} otherwise
     */
    final boolean flattenHierarchy() {
        return m_flattenHierarchy;
    }

    /**
     * Sets the include empty folders option.
     *
     * @param includeEmptyFolders {@code true} if empty folders shall be included and {@code false} otherwise
     */
    protected final void includeEmptyFolders(final boolean includeEmptyFolders) {
        m_includeEmptyFolders = includeEmptyFolders;
    }

    final boolean includeEmptyFolders() {
        return m_includeEmptyFolders;
    }

}
