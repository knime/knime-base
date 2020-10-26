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
package org.knime.base.node.io.filehandling.util.copymovefiles;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;

/**
 * Node config of the Copy/Move Files node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class CopyMoveFilesNodeConfig {

    /** Config key for delete source files checkbox. */
    private static final String CFG_DELETE_SOURCE_FILES = "delete_source_files";

    /** Config key for fail on deletion checkbox */
    private static final String CFG_FAIL_ON_DELETION = "fail_on_deletion";

    /** Config key for include parent folder checkbox. */
    private static final String CFG_INCLUDE_SOURCE_FOLDER = "include_source_folder";

    /** The file chooser model. */
    private final SettingsModelReaderFileChooser m_sourceFileChooserModel;

    /** The file chooser model. */
    private final SettingsModelWriterFileChooser m_destinationFileChooserModel;

    /** The delete source settings model. */
    private final SettingsModelBoolean m_deleteSourceFilesModel =
        new SettingsModelBoolean(CFG_DELETE_SOURCE_FILES, false);

    /** The fail on deletion settings model. */
    private final SettingsModelBoolean m_failOnDeletionModel = new SettingsModelBoolean(CFG_FAIL_ON_DELETION, false);

    /** The include parent folder settings model */
    private final SettingsModelBoolean m_includeSourcetFolderModel =
        new SettingsModelBoolean(CFG_INCLUDE_SOURCE_FOLDER, false);

    CopyMoveFilesNodeConfig(final SettingsModelReaderFileChooser sourceFilceChooserSettings,
        final SettingsModelWriterFileChooser destinationFileChooserSettings) {
        m_sourceFileChooserModel = sourceFilceChooserSettings;
        m_destinationFileChooserModel = destinationFileChooserSettings;
    }

    /**
     * Returns the {@link SettingsModelReaderFileChooser} of the source file / folder chooser.
     *
     * @return the source file chooser model
     */
    SettingsModelReaderFileChooser getSourceFileChooserModel() {
        return m_sourceFileChooserModel;
    }

    /**
     * Returns the {@link SettingsModelWriterFileChooser} of the destination folder chooser.
     *
     * @return the destination file chooser model
     */
    SettingsModelWriterFileChooser getDestinationFileChooserModel() {
        return m_destinationFileChooserModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the delete source files option.
     *
     * @return the deleteSourceFilesModel
     */
    SettingsModelBoolean getDeleteSourceFilesModel() {
        return m_deleteSourceFilesModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the fail on deletion option.
     *
     * @return the deleteSourceFilesModel
     */
    SettingsModelBoolean getFailOnDeletionModel() {
        return m_failOnDeletionModel;
    }

    /**
     * Returns the {@link SettingsModelBoolean} for the include parent folder option.
     *
     * @return the includeParentFolderModel
     */
    SettingsModelBoolean getSettingsModelIncludeSourceFolder() {
        return m_includeSourcetFolderModel;
    }

    /**
     * Validates the given settings.
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if settings are invalid
     */
    void validateConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sourceFileChooserModel.validateSettings(settings);
        m_destinationFileChooserModel.validateSettings(settings);
        m_deleteSourceFilesModel.validateSettings(settings);
        m_includeSourcetFolderModel.validateSettings(settings);
        m_failOnDeletionModel.validateSettings(settings);
    }

    /**
     * Saves the configuration for the {@code NodeModel}.
     *
     * @param settings the settings to save to
     */
    void saveConfigurationForModel(final NodeSettingsWO settings) {
        m_sourceFileChooserModel.saveSettingsTo(settings);
        m_destinationFileChooserModel.saveSettingsTo(settings);
        m_deleteSourceFilesModel.saveSettingsTo(settings);
        m_includeSourcetFolderModel.saveSettingsTo(settings);
        m_failOnDeletionModel.saveSettingsTo(settings);
    }

    /**
     * Load configuration in {@code NodeModel}.
     *
     * @param settings the settings to load from
     * @throws InvalidSettingsException - If loading the configuration failed
     */
    void loadConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sourceFileChooserModel.loadSettingsFrom(settings);
        m_destinationFileChooserModel.loadSettingsFrom(settings);
        m_deleteSourceFilesModel.loadSettingsFrom(settings);
        m_includeSourcetFolderModel.loadSettingsFrom(settings);
        m_failOnDeletionModel.loadSettingsFrom(settings);
    }
}
