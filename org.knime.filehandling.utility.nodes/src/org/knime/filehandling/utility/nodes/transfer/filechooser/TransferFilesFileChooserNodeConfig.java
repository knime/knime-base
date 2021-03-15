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
 *   Feb 25, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.filechooser;

import java.util.Arrays;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncationSettings;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeConfig;
import org.knime.filehandling.utility.nodes.transfer.policy.TransferPolicy;

/**
 * Node configuration of the Transfer Files/Folder node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFilesFileChooserNodeConfig extends AbstractTransferFilesNodeConfig {

    /** Config key for include parent folder checkbox. */
    private static final String CFG_INCLUDE_SOURCE_FOLDER = "include_source_folder";

    /** The file chooser model. */
    private final SettingsModelReaderFileChooser m_sourceFileChooserModel;

    /**
     * Constructor.
     *
     * @param sourceFilceChooserSettings the source file chooser settings model
     * @param destinationFileChooserSettings the destination file chooser settings model
     */
    TransferFilesFileChooserNodeConfig(final SettingsModelReaderFileChooser sourceFilceChooserSettings,
        final SettingsModelWriterFileChooser destinationFileChooserSettings) {
        super(destinationFileChooserSettings);
        m_sourceFileChooserModel = sourceFilceChooserSettings;
    }

    @Override
    protected boolean failIfSourceDoesNotExist() {
        return true;
    }

    /**
     * Returns the {@link SettingsModelReaderFileChooser} of the source file / folder chooser.
     *
     * @return the source file chooser model
     */
    SettingsModelReaderFileChooser getSourceFileChooserModel() {
        return m_sourceFileChooserModel;
    }

    @Override
    protected void validateAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sourceFileChooserModel.validateSettings(settings);
    }

    @Override
    protected void validateTransferPolicy(final NodeSettingsRO settings) throws InvalidSettingsException {
        // backwards compatibility see AP-16363
        if (settings.containsKey(CFG_TRANSFER_POLICY)) {
            super.validateTransferPolicy(settings);
        }
    }

    @Override
    protected void saveAdditionalSettingsForModel(final NodeSettingsWO settings) {
        m_sourceFileChooserModel.saveSettingsTo(settings);
    }

    @Override
    protected void loadSourceLocationSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_sourceFileChooserModel.loadSettingsFrom(settings);
    }

    @Override
    protected void loadTransferPolicyInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // backwards compatibility see AP-16363
        if (settings.containsKey(CFG_TRANSFER_POLICY)) {
            super.loadTransferPolicyInModel(settings);
        } else {
            loadTransferPolicyBackwardsCompatible(settings);
        }
    }

    private void loadTransferPolicyBackwardsCompatible(final NodeSettingsRO settings) throws InvalidSettingsException {
        FileOverwritePolicy fileOverwritePolicy = loadPolicyInModel(settings.getNodeSettings("destination_location"));
        final TransferPolicy transferPolicy;
        switch (fileOverwritePolicy) {
            case FAIL:
                transferPolicy = TransferPolicy.FAIL;
                break;
            case OVERWRITE:
                transferPolicy = TransferPolicy.OVERWRITE;
                break;
            case IGNORE:
                transferPolicy = TransferPolicy.IGNORE;
                break;
            default:
                throw new InvalidSettingsException(String
                    .format("The file overwrite policy '%s' is not supported by this node.", fileOverwritePolicy));
        }
        setTransferPolicy(transferPolicy);
    }

    private static FileOverwritePolicy loadPolicyInModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final String policyText = settings.getString("if_path_exists");
        return getPolicyFromText(policyText)
            .orElseThrow(() -> new InvalidSettingsException("Unable to load the transfer policy."));
    }

    private static Optional<FileOverwritePolicy> getPolicyFromText(final String policyText) {
        return Arrays.stream(FileOverwritePolicy.values())//
            .filter(p -> p.getText().equals(policyText))//
            .findAny();
    }

    @Override
    protected void validateVerboseOutput(final NodeSettingsRO settings) throws InvalidSettingsException {
        // backwards compatibility see AP-14932
        if (settings.containsKey(CFG_VERBOSE_OUTPUT)) {
            super.validateVerboseOutput(settings);
        }
    }

    @Override
    protected void loadVerboseOutputInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_VERBOSE_OUTPUT)) {
            super.loadVerboseOutputInModel(settings);
        } else {
            // backwards compatibility see AP-14932
            getVerboseOutputModel().setBooleanValue(true);
        }
    }

    @Override
    protected void validateTruncatePathOption(final NodeSettingsRO settings) throws InvalidSettingsException {
        // backwards compatibility see AP-14932
        if (settings.containsKey(CFG_INCLUDE_SOURCE_FOLDER)) {
            settings.getBoolean(CFG_INCLUDE_SOURCE_FOLDER);
        } else {
            super.validateTruncatePathOption(settings);
        }
    }

    @Override
    protected void loadTruncatePathOptionsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // backwards compatibility see AP-14932
        if (settings.containsKey(CFG_INCLUDE_SOURCE_FOLDER)) {
            final TruncationSettings truncationSettings = getTruncationSettings();
            if (m_sourceFileChooserModel.getFilterMode() != FilterMode.FILE
                && settings.getBoolean(CFG_INCLUDE_SOURCE_FOLDER)) {
                truncationSettings.setTruncatePathOption(TruncatePathOption.KEEP_SRC_FOLDER);
            } else {
                truncationSettings.setTruncatePathOption(TruncatePathOption.TRUNCATE_SRC_FOLDER);
            }
        } else {
            super.loadTruncatePathOptionsInModel(settings);
        }
    }

    @Override
    protected void loadAdditionalSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to do
    }

}
