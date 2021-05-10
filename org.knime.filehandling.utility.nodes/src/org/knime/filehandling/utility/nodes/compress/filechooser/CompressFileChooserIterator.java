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
 *   Feb 2, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.filechooser;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressFileFolderEntry;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressIterator;
import org.knime.filehandling.utility.nodes.compress.iterator.CompressPair;
import org.knime.filehandling.utility.nodes.truncator.PathTruncator;
import org.knime.filehandling.utility.nodes.truncator.TruncationSettings;

/**
 * A {@link CompressIterator} with exactly one entry provided by the {@link SettingsModelReaderFileChooser}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class CompressFileChooserIterator implements CompressIterator {

    private final ReadPathAccessor m_accessor;

    private final Iterator<? extends CompressEntry> m_entryIter;

    private final long m_size;

    CompressFileChooserIterator(final TruncationSettings truncationSettings,
        final SettingsModelReaderFileChooser fileChooser, final Consumer<StatusMessage> statusMessageConsumer,
        final boolean includeEmptyFolders) throws IOException, InvalidSettingsException {
        m_accessor = fileChooser.createReadPathAccessor();
        final FSPath source;
        final List<FSPath> sourcePaths;
        try {
            source = m_accessor.getRootPath(statusMessageConsumer);
            sourcePaths = m_accessor.getFSPaths(statusMessageConsumer);
        } catch (final IOException | InvalidSettingsException e) {
            m_accessor.close();
            throw e;
        }

        m_entryIter =
            initEntryIter(truncationSettings, includeEmptyFolders, source, sourcePaths, fileChooser.getFilterMode());
        m_size = sourcePaths.size();
    }

    private static Iterator<? extends CompressEntry> initEntryIter(final TruncationSettings truncationSettings,
        final boolean includeEmptyFolders, final FSPath source, final List<FSPath> sourcePaths,
        final FilterMode filterMode) {
        if (filterMode == FilterMode.FILES_IN_FOLDERS) {
            final PathTruncator truncator = truncationSettings.getPathTruncator(source, filterMode);
            return sourcePaths.stream()//
                .map(p -> new CompressFileEntry(truncator.getTruncatedString(p), p))//
                .iterator();
        } else if (filterMode == FilterMode.FILE || filterMode == FilterMode.FOLDER) {
            return Collections.singletonList(new CompressFileFolderEntry(source, includeEmptyFolders,
                p -> truncationSettings.getPathTruncator(p, filterMode))).iterator();
        } else {
            throw new IllegalArgumentException(
                String.format("The selected filter mode %s is not supported.", filterMode.getText()));
        }
    }

    @Override
    public boolean hasNext() {
        return m_entryIter.hasNext();
    }

    @Override
    public CompressEntry next() {
        return m_entryIter.next();
    }

    @Override
    public long size() {
        return m_size;
    }

    @Override
    public void close() throws IOException {
        m_accessor.close();
    }

    private static class CompressFileEntry implements CompressEntry {

        private final CompressPair m_compressPair;

        CompressFileEntry(final String archiveEntryName, final FSPath pathToCompress) {
            m_compressPair = new CompressPair(archiveEntryName, pathToCompress);
        }

        @Override
        public List<CompressPair> getPaths() throws IOException {
            return Collections.singletonList(m_compressPair);
        }

    }
}