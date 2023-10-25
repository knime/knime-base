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
 *   May 29, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.reader;

import java.util.Optional;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageUtils;

/**
 * Computes the status message for dialogs of type "open".
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DefaultReaderStatusMessageReporter implements StatusMessageReporter {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultReaderStatusMessageReporter.class);

    private final SettingsModelReaderFileChooser m_settings;

    /**
     * Constructor.
     *
     * @param settings the reader file chooser settings
     */
    public DefaultReaderStatusMessageReporter(final SettingsModelReaderFileChooser settings) {
        m_settings = settings;
    }

    @Override
    public StatusMessage report() {
        try (final ReadPathAccessor accessor = m_settings.createReadPathAccessor()) {
            final PriorityStatusConsumer consumer = new PriorityStatusConsumer();
            accessor.getFSPaths(consumer);
            final Optional<StatusMessage> scanningStatus = consumer.get();
            if (scanningStatus.isPresent()) {
                return scanningStatus.get();
            } else if (getFilterMode() == FilterMode.FILE || getFilterMode() == FilterMode.FOLDER) {
                return StatusMessageUtils.SUCCESS_MSG;
            } else {
                final FileFilterStatistic stats = accessor.getFileFilterStatistic();
                return createFilterMessage(stats);
            }
        } catch (Exception ex) {
            LOGGER.debug("Exception encountered while reporting status message.", ex);
            return DefaultStatusMessage.mkError("%s", ex.getMessage());
        }
    }

    private StatusMessage createFilterMessage(final FileFilterStatistic stat) {
        if (stat.getIncludedFiles() == 0
            && (getFilterMode() == FilterMode.FILES_IN_FOLDERS || stat.getIncludedFolders() == 0)) {
            return createNoMatchesError();
        } else {
            return createScanSuccessMessage(stat);
        }
    }

    private StatusMessage createScanSuccessMessage(final FileFilterStatistic stat) {
        return DefaultStatusMessage.mkInfo(createScanSuccessMessageText(stat));
    }

    private String createScanSuccessMessageText(final FileFilterStatistic stat) {
        switch (getFilterMode()) {
            case FILES_AND_FOLDERS:
                return String.format("Selected %d of %d files and %d of %d folders", stat.getIncludedFiles(),
                    stat.getVisitedFiles(), stat.getIncludedFolders(), stat.getVisitedFolders());
            case FILES_IN_FOLDERS:
                return String.format("Selected %d of %d files", stat.getIncludedFiles(), stat.getVisitedFiles());
            case FOLDERS:
                return String.format("Selected %d of %d folders", stat.getIncludedFolders(), stat.getVisitedFolders());
            case FILE:
            case FOLDER:
            default:
                throw new IllegalStateException("Unsupported filter mode while scanning: " + getFilterMode());
        }
    }

    private FilterMode getFilterMode() {
        return m_settings.getFilterModeModel().getFilterMode();
    }

    private StatusMessage createNoMatchesError() {
        return DefaultStatusMessage.mkError("No %s matched the filters", getFilterEntity());
    }

    private String getFilterEntity() {
        final FilterMode filterMode = getFilterMode();
        switch (filterMode) {
            case FILES_IN_FOLDERS:
                return "files";
            case FOLDERS:
                return "folders";
            case FILES_AND_FOLDERS:
                return "files or folders";
            case FOLDER:
            case FILE:
            default:
                // doesn't happen
                throw new IllegalStateException("Unsupported filter mode when scanning: " + filterMode);
        }
    }

}
