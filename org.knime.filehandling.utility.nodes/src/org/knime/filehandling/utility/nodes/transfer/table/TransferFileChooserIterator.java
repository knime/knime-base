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
 *   Feb 24, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferFileFolderEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferIterator;
import org.knime.filehandling.utility.nodes.truncator.PathTruncator;
import org.knime.filehandling.utility.nodes.truncator.TruncationSettings;
import org.knime.filehandling.utility.nodes.utils.iterators.FsCellColumnIterator;

/**
 * A {@link TransferIterator} processing an input table whose destination entries are based on a
 * {@link SettingsModelWriterFileChooser}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFileChooserIterator implements TransferIterator {

    private final FsCellColumnIterator m_iterator;

    private final WritePathAccessor m_accessor;

    private final FSPath m_destinationFolder;

    private final TruncationSettings m_truncationSettings;

    TransferFileChooserIterator(final TruncationSettings truncationSettings, final BufferedDataTable table,
        final int pathColIdx, final FSConnection connection, final SettingsModelWriterFileChooser fileChooser,
        final Consumer<StatusMessage> statusMessageConsumer) throws IOException, InvalidSettingsException {
        m_truncationSettings = truncationSettings;
        m_iterator = new FsCellColumnIterator(table, pathColIdx, connection);
        m_accessor = fileChooser.createWritePathAccessor();
        try {
            m_destinationFolder = m_accessor.getOutputPath(statusMessageConsumer);
            if (!FSFiles.exists(m_destinationFolder)) {
                if (fileChooser.isCreateMissingFolders()) {
                    Files.createDirectories(m_destinationFolder);
                } else {
                    close();
                    throw new IOException(
                        String.format("The directory '%s' does not exist and must not be created due to user settings.",
                            m_destinationFolder));
                }
            }
        } catch (final InvalidSettingsException e) {
            close();
            throw e;
        }
    }

    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    @Override
    public TransferEntry next() {
        final FSPath path = m_iterator.next();
        return new TransferFileFolderEntry(path, m_destinationFolder, this::getPathTruncator);
    }

    private PathTruncator getPathTruncator(final Path basePath) throws IOException {
        final FilterMode filterMode = FSFiles.isDirectory(basePath) ? FilterMode.FOLDER : FilterMode.FILE;
        return m_truncationSettings.getPathTruncator(basePath, filterMode);
    }

    @Override
    public void close() throws IOException {
        m_iterator.close();
        m_accessor.close();
    }

    @Override
    public long size() {
        return m_iterator.size();
    }

}
