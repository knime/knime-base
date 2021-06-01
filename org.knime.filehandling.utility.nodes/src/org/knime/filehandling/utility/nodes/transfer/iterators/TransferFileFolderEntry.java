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
package org.knime.filehandling.utility.nodes.transfer.iterators;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.util.CheckedExceptionFunction;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeDialog;
import org.knime.filehandling.utility.nodes.truncator.PathToStringArrayTruncator;
import org.knime.filehandling.utility.nodes.truncator.PathToStringTruncator;

/**
 * A {@link TransferEntry} for file or folder entries. If this class is being constructed with a path to a file
 * {@link #getPathsToCopy()} will return an empty list, otherwise all files and folders located inside the provided
 * folder will be returned.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class TransferFileFolderEntry implements TransferEntry {

    private final FSPath m_source;

    private final FSPath m_destinationFolder;

    private final CheckedExceptionFunction<Path, PathToStringArrayTruncator, IOException> m_truncatorFac;

    /**
     * Constructor.
     *
     * @param source can either specify a file or a folder
     * @param destinationFolder the folder where all files have to be transfered to
     * @param truncatorFac factory allowing to create a {@link PathToStringTruncator}
     */
    public TransferFileFolderEntry(final FSPath source, final FSPath destinationFolder,
        final CheckedExceptionFunction<Path, PathToStringArrayTruncator, IOException> truncatorFac) {
        m_source = source;
        m_destinationFolder = destinationFolder;
        m_truncatorFac = truncatorFac;
    }

    @Override
    public FSPath getSource() {
        return m_source;
    }

    @Override
    public List<TransferPair> getPathsToCopy() throws IOException {
        if (!FSFiles.isDirectory(m_source)) {
            return Collections.emptyList();
        } else {
            final PathToStringArrayTruncator pathTruncator = m_truncatorFac.apply(m_source);
            return FileAndFoldersCollector.getPaths(m_source).stream()//
                .map(p -> toTransferPair(p, pathTruncator))//
                .collect(Collectors.toList());
        }
    }

    private TransferPair toTransferPair(final FSPath source, final PathToStringArrayTruncator pathTruncator) {
        return new TransferPair(source, m_destinationFolder.resolve(pathTruncator.getTruncatedStringArray(source)));
    }

    @Override
    public TransferPair getSrcDestPair() throws IOException {
        return toTransferPair(m_source, m_truncatorFac.apply(m_source));
    }

    @Override
    public void validate() throws InvalidSettingsException, IOException {
        final String[] suffix = m_truncatorFac.apply(m_source).getTruncatedStringArray(m_source);
        final FSPath absDestFolder = (FSPath)m_destinationFolder.toAbsolutePath().normalize();
        final Path dest = absDestFolder.resolve(suffix).normalize();
        if (!dest.startsWith(absDestFolder)) {
            throw new InvalidSettingsException(
                "The selected " + AbstractTransferFilesNodeDialog.DESTINATION_OPTION_TITLE.toLowerCase()
                    + " option causes files to be copied to locations outside of the selected destination folder.");
        }
    }

}
