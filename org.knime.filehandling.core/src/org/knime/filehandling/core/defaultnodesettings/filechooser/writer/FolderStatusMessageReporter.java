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
 *   14 Apr 2021 (Laurin): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.writer;

import java.io.IOException;
import java.nio.file.Files;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageUtils;

/**
 * {@link StatusMessageReporter} that throws an exception if the selected folder does not exist.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class FolderStatusMessageReporter implements StatusMessageReporter {

    private final SettingsModelWriterFileChooser m_settings;

    /**
     * Constructor.
     *
     * @param settings the writer file chooser settings
     */
    public FolderStatusMessageReporter(final SettingsModelWriterFileChooser settings) {
        m_settings = settings;
    }

    @Override
    public StatusMessage report() throws IOException, InvalidSettingsException {
        try (final WritePathAccessor accessor = m_settings.createWritePathAccessor()) {
            final FSPath path = accessor.getOutputPath(StatusMessageUtils.NO_OP_CONSUMER);
            if (m_settings.isCreateMissingFolders()) {
                return StatusMessageUtils.SUCCESS_MSG;
            }
            if (FSFiles.exists(path)) {
                if (!Files.isWritable(path)) {
                    throw ExceptionUtil.createAccessDeniedException(path);
                }
                return StatusMessageUtils.SUCCESS_MSG;
            } else {
                return StatusMessageUtils.MISSING_FOLDERS_MSG;
            }
        }

    }

}
