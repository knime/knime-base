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
 *   Feb 1, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.iterator;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;

/**
 * A {@link CompressEntry} for file or folder entries. If this class is being constructed with a path to a file the base
 * folder will be set to the files parent, otherwise all files and empty folders within that folder will be calculated
 * and returned when invoking {@link #getPaths()}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class CompressFileFolderEntry implements CompressEntry {

    private final FSPath m_base;

    private final boolean m_includeEmptyFolders;

    /**
     * Constructor.
     *
     * @param path can either specify a file or a folder
     * @param includeEmptyFolders flag indicating whether or not empty folders should be included when compiling the
     *            {@link #getPaths()} list.
     */
    public CompressFileFolderEntry(final FSPath path, final boolean includeEmptyFolders) {
        m_base = path;
        m_includeEmptyFolders = includeEmptyFolders;
    }

    @Override
    public List<FSPath> getPaths() throws IOException {
        final List<FSPath> paths = new ArrayList<>();
        Files.walkFileTree(m_base, new FileAndEmptyFoldersCollector(paths, m_includeEmptyFolders));
        if (m_includeEmptyFolders && paths.isEmpty()) {
            paths.add(m_base);
        }
        FSFiles.sortPathsLexicographically(paths);
        return paths;
    }

    @Override
    public Optional<FSPath> getBaseFolder() throws AccessDeniedException {
        if (!FSFiles.isDirectory(m_base)) {
            return Optional.ofNullable((FSPath)m_base.getParent());
        } else {
            return Optional.of(m_base);
        }
    }

}
