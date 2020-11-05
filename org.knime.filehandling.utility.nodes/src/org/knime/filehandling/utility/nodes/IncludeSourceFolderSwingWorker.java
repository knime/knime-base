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
 *   Aug 27, 2020 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes;

import java.io.IOException;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessageUtils;

/**
 * Swingworker to check whether a path ends with ".",  "..", has no parent or is empty and returns a corresponding {@link StatusMessage}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */

public final class IncludeSourceFolderSwingWorker implements StatusMessageReporter {

    private final SettingsModelReaderFileChooser m_readerModel;

    /**
     * Constructor.
     *
     * @param readerModel the {@link SettingsModelReaderFileChooser}
     */
    public IncludeSourceFolderSwingWorker(final SettingsModelReaderFileChooser readerModel) {
        m_readerModel = readerModel;
    }

    @Override
    public StatusMessage report() throws IOException, InvalidSettingsException {
        return hasParentFolder().orElse(DefaultStatusMessage.SUCCESS_MSG);
    }

    private Optional<StatusMessage> hasParentFolder() throws IOException, InvalidSettingsException {
        try (final ReadPathAccessor readPathAccessor = m_readerModel.createReadPathAccessor()) {
            final FSPath rootPath = readPathAccessor.getRootPath(StatusMessageUtils.NO_OP_CONSUMER);

            return PathHandlingUtils.isIncludeSourceFolderAvailable(rootPath)
                ? Optional.of(DefaultStatusMessage.SUCCESS_MSG)
                : Optional.of(DefaultStatusMessage.mkError(PathHandlingUtils.createErrorMessage(rootPath)));
        } catch (final IOException | InvalidSettingsException e) { // NOSONAR we don't care about exceptions here
            return Optional.empty();
        }
    }
}
