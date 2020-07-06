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
 *   May 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.reader;

/**
 * Statistics on the number of files/folders visited and filtered.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class FileFilterStatistic {

    private final int m_filteredFiles;

    private final int m_visitedFiles;

    private final int m_filteredFolders;

    private final int m_visitedFolders;

    /**
     * Constructor.
     *
     * @param filteredFiles the number of filtered files
     * @param filteredHiddenFiles the number of filtered hidden files
     * @param visitedFiles the number of visited files
     * @param filteredFolders the number of filtered folders
     * @param filteredHiddenFolders the number of filtered hidden folders
     * @param visitedFolders the number of visited folders
     */
    public FileFilterStatistic(final int filteredFiles, final int filteredHiddenFiles, final int visitedFiles,
        final int filteredFolders, final int filteredHiddenFolders, final int visitedFolders) {
        m_filteredFiles = filteredFiles;
        m_visitedFiles = visitedFiles - filteredHiddenFiles;
        m_filteredFolders = filteredFolders;
        m_visitedFolders = visitedFolders - filteredHiddenFolders;
    }

    /**
     * Returns the number of filtered out files.
     *
     * @return the number of filtered out files
     */
    public int getFilteredFiles() {
        return m_filteredFiles;
    }

    /**
     * Returns the number of included files.
     *
     * @return the number of included files
     */
    public int getIncludedFiles() {
        return m_visitedFiles - m_filteredFiles;
    }

    /**
     * Returns the number of visited files.
     *
     * @return the number of visited files
     */
    public int getVisitedFiles() {
        return m_visitedFiles;
    }

    /**
     * Returns the number of filtered out folders.
     *
     * @return the number of filtered out folders
     */
    public int getFilteredFolders() {
        return m_filteredFolders;
    }

    /**
     * Returns the number of included folders.
     *
     * @return the number of included folders
     */
    public int getIncludedFolders() {
        return m_visitedFolders - m_filteredFolders;
    }

    /**
     * Returns the number of visited folders.
     *
     * @return the number of visited folders
     */
    public int getVisitedFolders() {
        return m_visitedFolders;
    }
}