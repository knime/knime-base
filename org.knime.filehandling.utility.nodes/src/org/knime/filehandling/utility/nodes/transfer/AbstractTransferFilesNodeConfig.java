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
 *   Mar 5, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.utility.nodes.compress.truncator.PathTruncator;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncationSettings;

/**
 * Abstract node config of the Transfer Files/Folder node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public abstract class AbstractTransferFilesNodeConfig {

    /** Config key for delete source files checkbox. */
    private static final String CFG_DELETE_SOURCE_FILES = "delete_source_location";

    /** Config key for fail on deletion checkbox */
    private static final String CFG_FAIL_ON_UNSUCCESSFUL_DELETION = "fail_unsuccessful_deletion";

    /** Config key for the verbose output. */
    protected static final String CFG_VERBOSE_OUTPUT = "verbose_output";

    /** The file chooser model. */
    private final SettingsModelWriterFileChooser m_destinationFileChooserModel;

    /** The {@link TruncationSettings}. */
    private TruncationSettings m_truncationSettings;

    /** The delete source settings model. */
    private final SettingsModelBoolean m_deleteSourceFilesModel =
        new SettingsModelBoolean(CFG_DELETE_SOURCE_FILES, false);

    /** The fail on deletion settings model. */
    private final SettingsModelBoolean m_failOnDeletionModel =
        new SettingsModelBoolean(CFG_FAIL_ON_UNSUCCESSFUL_DELETION, false);

    /** The verbose output settings model. */
    private final SettingsModelBoolean m_verboseOutputModel;

    /**
     * Constructor.
     *
     * @param destinationFileChooserSettings the destination file chooser settings model
     */
    protected AbstractTransferFilesNodeConfig(final SettingsModelWriterFileChooser destinationFileChooserSettings) {
        m_destinationFileChooserModel = destinationFileChooserSettings;
        m_truncationSettings = new TruncationSettings();
        m_verboseOutputModel = new SettingsModelBoolean(CFG_VERBOSE_OUTPUT, false);
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} of the destination folder chooser.
     *
     * @return the destination file chooser model
     */
    public final SettingsModelWriterFileChooser getDestinationFileChooserModel() {
        return m_destinationFileChooserModel;
    }

    /**
     * Returns the {@link TruncationSettings}.
     *
     * @return the {@link TruncationSettings}
     */
    protected final TruncationSettings getTruncationSettings() {
        return m_truncationSettings;
    }

    /**
     * Returns the {@link PathTruncator}.
     *
     * @return the {@link PathTruncator}
     */
    public final PathTruncator getPathTruncator() {
        return m_truncationSettings.getPathTruncator(false);
    }

    /**
     * Returns the settings model storing the verbose output flag.
     *
     * @return the settings model storing the verbose output flag
     */
    protected final SettingsModelBoolean getVerboseOutputModel() {
        return m_verboseOutputModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the delete source files option.
     *
     * @return the deleteSourceFilesModel
     */
    final SettingsModelBoolean getDeleteSourceFilesModel() {
        return m_deleteSourceFilesModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the fail on deletion option.
     *
     * @return the deleteSourceFilesModel
     */
    final SettingsModelBoolean getFailOnDeletionModel() {
        return m_failOnDeletionModel;
    }

    /**
     * Validates the given settings.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if settings are invalid
     */
    final void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_destinationFileChooserModel.validateSettings(settings);
        m_deleteSourceFilesModel.validateSettings(settings);
        m_failOnDeletionModel.validateSettings(settings);
        validateTruncatePathOption(settings);
        validateVerboseOutput(settings);
        validateAdditionalSettingsForModel(settings);
    }

    /**
     * Validates the truncate options.
     *
     * @param settings the settings to validate
     * @throws InvalidSettingsException - If the truncate options validation failed
     */
    protected void validateTruncatePathOption(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_truncationSettings.validateSettingsForModel(settings);
    }

    /**
     * Validates the verbose output option.
     *
     * @param settings the settings to validate
     * @throws InvalidSettingsException - If the verbose option validation failed
     */
    protected void validateVerboseOutput(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_verboseOutputModel.validateSettings(settings);
    }

    /**
     * Validates any additional settings introduced by extending classes.
     *
     * @param settings the settings to be validated
     * @throws InvalidSettingsException - If the validation failed
     */
    protected abstract void validateAdditionalSettingsForModel(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the configuration for the {@code NodeModel}.
     *
     * @param settings the settings to save to
     */
    final void saveSettingsForModel(final NodeSettingsWO settings) {
        saveAdditionalSettingsForModel(settings);
        m_destinationFileChooserModel.saveSettingsTo(settings);
        m_truncationSettings.saveSettingsForModel(settings);
        m_deleteSourceFilesModel.saveSettingsTo(settings);
        m_failOnDeletionModel.saveSettingsTo(settings);
        m_verboseOutputModel.saveSettingsTo(settings);
    }

    /**
     * Saves all non setting model parameters in the dialog.
     *
     * @param settings the settings to write to
     * @param truncatePathOption the {@link TruncatePathOption} configured via the dialog
     */
    final void saveTruncatePathOptionsForDialog(final NodeSettingsWO settings) {
        m_truncationSettings.saveSettingsForDialog(settings);
    }

    /**
     * Stores any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings to save the options to
     */
    protected abstract void saveAdditionalSettingsForModel(NodeSettingsWO settings);

    /**
     * Load configuration in {@code NodeModel}.
     *
     * @param settings the settings to load from
     * @throws InvalidSettingsException - If loading the configuration failed
     */
    final void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        loadSourceLocationSettingsInModel(settings);
        m_destinationFileChooserModel.loadSettingsFrom(settings);
        m_deleteSourceFilesModel.loadSettingsFrom(settings);
        loadAdditionalSettingsInModel(settings);
        m_failOnDeletionModel.loadSettingsFrom(settings);
        loadVerboseOutputInModel(settings);
        loadTruncatePathOptionsInModel(settings);
    }

    /**
     * Loads any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings storing the additional options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected abstract void loadSourceLocationSettingsInModel(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads any additional settings that are specific for the concrete implementation of this class.
     *
     * @param settings the settings storing the additional options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected abstract void loadAdditionalSettingsInModel(NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Loads the verbose output options in the node model.
     *
     * @param settings the setting storing the options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected void loadVerboseOutputInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_verboseOutputModel.loadSettingsFrom(settings);
    }

    /**
     * Loads the truncation path options in the node model. It is ensure that this method is called after invoking
     * {@link #loadAdditionalSettingsInModel(NodeSettingsRO)}.
     *
     * @param settings the setting storing the options
     * @throws InvalidSettingsException - If the options cannot be loaded
     */
    protected void loadTruncatePathOptionsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_truncationSettings.loadSettingsForModel(settings);
    }

}
