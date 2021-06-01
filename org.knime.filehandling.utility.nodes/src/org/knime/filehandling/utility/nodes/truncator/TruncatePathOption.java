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
 *   Feb 11, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.truncator;

import java.nio.file.Path;

import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.TriFunction;
import org.knime.filehandling.utility.nodes.truncator.impl.FolderPrefixPathTruncator;
import org.knime.filehandling.utility.nodes.truncator.impl.KeepPathTruncator;
import org.knime.filehandling.utility.nodes.truncator.impl.RelativePathTruncator;

/**
 * Enum encoding various options to truncate a path.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public enum TruncatePathOption {

        /** Option signaling that the resulting path has to be relative to the provided base path. */
        RELATIVE("Include only the selected file/folder",
            (path, prefix, filterMode) -> new RelativePathTruncator(path, filterMode)),

        /** Option signaling that the absolute normalized path has to be returned. */
        KEEP("Include all folders in the source path", (path, prefix, filterMode) -> new KeepPathTruncator()),

        /** Option signaling that the given folder prefix has to be removed. */
        REMOVE_FOLDER_PREFIX("Include all folders in the source path succeeding the prefix",
            FolderPrefixPathTruncator::new);

    /** The label. */
    private final String m_label;

    /** Function to create the associated {@link PathTruncator}. */
    private final TriFunction<Path, String, FilterMode, PathTruncator> m_pathTruncatorFactory;

    /**
     * Constructor.
     *
     * @param label the label
     * @param pathTruncatorFactory the factory instantiating the associated {@link PathTruncator}
     */
    private TruncatePathOption(final String label,
        final TriFunction<Path, String, FilterMode, PathTruncator> pathTruncatorFactory) {
        m_label = label;
        m_pathTruncatorFactory = pathTruncatorFactory;
    }

    String getLabel() {
        return m_label;
    }

    /**
     * Returns the default {@link TruncatePathOption}.
     *
     * @return the default {@link TruncatePathOption}
     */
    public static TruncatePathOption getDefault() {
        return RELATIVE;
    }

    /**
     * Creates the {@link PathTruncator} associated with this {@link TruncatePathOption}.
     *
     * @param basePath the base file/folder, i.e., the path specifying the prefix that has to be truncated
     * @param folderPrefix only required for {@link TruncatePathOption#REMOVE_FOLDER_PREFIX}
     * @param filterMode the {@link FilterMode} specifying the base path is a file or folder and the internals for
     *            various {@link PathTruncator}s
     * @return the associated {@link PathTruncator}
     */
    public PathTruncator createPathTruncator(final Path basePath, final String folderPrefix,
        final FilterMode filterMode) {
        return m_pathTruncatorFactory.apply(basePath, folderPrefix, filterMode);
    }

}
