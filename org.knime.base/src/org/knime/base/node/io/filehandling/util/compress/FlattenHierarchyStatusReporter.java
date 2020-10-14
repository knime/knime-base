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
 *   Oct 16, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.compress;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.PriorityStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class FlattenHierarchyStatusReporter implements StatusMessageReporter {

    private static final StatusMessage SUCCESS_MSG = DefaultStatusMessage.mkInfo("");

    private final SettingsModelReaderFileChooser m_readerModel;

    FlattenHierarchyStatusReporter(final SettingsModelReaderFileChooser readerModel) {
        m_readerModel = readerModel;
    }

    @Override
    public StatusMessage report() {
        return canBeFlattened().orElse(SUCCESS_MSG);
    }

    private Optional<StatusMessage> canBeFlattened() {
        final PriorityStatusConsumer consumerReader = new PriorityStatusConsumer();
        try (final ReadPathAccessor readPathAccessor = m_readerModel.createReadPathAccessor()) {
            final FSPath rootSourcePath = readPathAccessor.getRootPath(consumerReader);
            //Additional check, as an empty path is still a regular path
            if (rootSourcePath.toString().length() != 0) {
                final List<FSPath> sourcePaths = getSourcePaths(consumerReader, readPathAccessor);
                return checkNameCollisions(sourcePaths);
            } else {
                return Optional.empty();
            }
        } catch (final IOException | InvalidSettingsException e) { // NOSONAR we don't care about exceptions here
            return Optional.empty();
        }
    }

    private static Optional<StatusMessage> checkNameCollisions(final List<FSPath> sourcePaths) {
        final Map<String, String> entries = new HashMap<>();
        for (final FSPath p : sourcePaths) {
            final String fileName = p.getFileName().toString();
            if (entries.containsKey(fileName)) {
                return Optional.of(DefaultStatusMessage.mkError(CompressNodeModel.NAME_COLLISION_ERROR_TEMPLATE,
                    p.toString(), entries.get(fileName)));
            } else {
                entries.put(fileName, p.toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the source paths from the {@link DialogComponentReaderFileChooser} based on the the {@link FilterMode}.
     *
     * @param consumerReader the consumer for the status messages
     * @return the source paths of the {@link DialogComponentReaderFileChooser}
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private List<FSPath> getSourcePaths(final PriorityStatusConsumer consumerReader,
        final ReadPathAccessor readPathAccessor) throws IOException, InvalidSettingsException {
        if (m_readerModel.getFilterModeModel().getFilterMode() == FilterMode.FOLDER) {
            return FSFiles.getFilePathsFromFolder(readPathAccessor.getRootPath(consumerReader));
        } else {
            return readPathAccessor.getFSPaths(consumerReader);
        }
    }

}
