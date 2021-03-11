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
package org.knime.filehandling.utility.nodes.transfer.filechooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.WritePathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.utility.nodes.compress.truncator.PathTruncator;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferFileFolderEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferIterator;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferPair;

/**
 * A {@link TransferIterator} with exactly one entry provided by the {@link SettingsModelReaderFileChooser}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFileChooserIterator implements TransferIterator {

    private final ReadPathAccessor m_readAccessor;

    private final WritePathAccessor m_writeAccessor;

    private final Iterator<? extends TransferEntry> m_entryIter;

    private final long m_size;

    TransferFileChooserIterator(final PathTruncator pathTruncator, final SettingsModelReaderFileChooser readFileChooser,
        final SettingsModelWriterFileChooser writeFileChooser, final Consumer<StatusMessage> statusMessageConsumer)
        throws IOException, InvalidSettingsException {
        m_readAccessor = readFileChooser.createReadPathAccessor();
        final FSPath source;
        final List<FSPath> sourcePaths;
        try {
            source = m_readAccessor.getRootPath(statusMessageConsumer);
            sourcePaths = m_readAccessor.getFSPaths(statusMessageConsumer);
        } catch (final IOException | InvalidSettingsException e) {
            m_readAccessor.close();
            throw e;
        }

        m_writeAccessor = writeFileChooser.createWritePathAccessor();
        final FSPath destinationFolder;
        try {
            destinationFolder = m_writeAccessor.getOutputPath(statusMessageConsumer);
            final Path parent = destinationFolder.getParent();
            if (parent != null && !FSFiles.exists(parent)) {
                if (writeFileChooser.isCreateMissingFolders()) {
                    Files.createDirectories(parent);
                } else {
                    close();
                    throw new IOException(String.format(
                        "The directory '%s' does not exist and must not be created due to user settings.", parent));
                }
            }
        } catch (final InvalidSettingsException e) {
            close();
            throw e;
        }

        if (readFileChooser.getFilterMode() == FilterMode.FOLDER) {
            m_entryIter = Collections
                .singletonList(new TransferFileFolderEntry(source, destinationFolder, pathTruncator)).iterator();
        } else {
            final FSPath parent;
            if (readFileChooser.getFilterMode() == FilterMode.FILE) {
                parent = (FSPath)source.getParent();
            } else {
                parent = source;
            }
            m_entryIter = sourcePaths.stream()//
                .map(p -> new TransferFileEntry(p,
                    destinationFolder.resolve(pathTruncator.getTruncatedStringArray(parent, p))))//
                .iterator();
        }
        m_size = sourcePaths.size();
    }

    @Override
    public boolean hasNext() {
        return m_entryIter.hasNext();
    }

    @Override
    public TransferEntry next() {
        return m_entryIter.next();
    }

    @Override
    public void close() throws IOException {
        try {
            m_readAccessor.close();
        } finally {
            m_writeAccessor.close();
        }
    }

    @Override
    public long size() {
        return m_size;
    }

    private static class TransferFileEntry implements TransferEntry {

        private final TransferPair m_transferPair;

        TransferFileEntry(final FSPath source, final FSPath destination) {
            m_transferPair = new TransferPair(source, destination);
        }

        @Override
        public FSPath getSource() {
            return m_transferPair.getSource();
        }

        @Override
        public TransferPair getSrcDestPair() throws IOException {
            return m_transferPair;
        }

        @Override
        public List<TransferPair> getPathsToCopy() throws IOException {
            return Collections.emptyList();
        }

    }
}
