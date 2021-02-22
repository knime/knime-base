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
package org.knime.filehandling.utility.nodes.compress.truncator;

import java.util.function.BiFunction;

import org.knime.filehandling.utility.nodes.compress.truncator.impl.KeepFullPathTruncator;
import org.knime.filehandling.utility.nodes.compress.truncator.impl.KeepSourceFolderTruncator;
import org.knime.filehandling.utility.nodes.compress.truncator.impl.RegexTruncator;
import org.knime.filehandling.utility.nodes.compress.truncator.impl.SourceFolderTruncator;

/**
 * Enum encoding various options to truncate a path.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public enum TruncatePathOption {

        /** Option signaling that no path modification is required. */
        KEEP_FULL_PATH("Keep full path", (f, r) -> new KeepFullPathTruncator(f)),

        /** Option signaling that anything before the folder has to be truncated. */
        KEEP_SRC_FOLDER("Keep source folder only", (f, r) -> new KeepSourceFolderTruncator(f)),

        /** Option signaling that the full path has to be truncated. */
        TRUNCATE_SRC_FOLDER("Truncate full path", (f, r) -> new SourceFolderTruncator(f)),

        /** Option signaling that an regular expression is used to modify the path. */
        TRUNCATE_REGEX("Truncate", RegexTruncator::new);

    /** The label used in the dialog. */
    private final String m_label;

    /** Function to create the associated {@link PathTruncator}. */
    private final BiFunction<Boolean, String, PathTruncator> m_pathTruncatorFactory;

    /**
     * Constructor.
     *
     * @param label the label used in the UI
     * @param pathTruncatorFactory the factory instantiating the associated {@link PathTruncator}
     */
    private TruncatePathOption(final String label,
        final BiFunction<Boolean, String, PathTruncator> pathTruncatorFactory) {
        m_label = label;
        m_pathTruncatorFactory = pathTruncatorFactory;
    }

    /**
     * Returns the text.
     *
     * @return the text.
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Returns the default {@link TruncatePathOption}.
     *
     * @return the default {@link TruncatePathOption}
     */
    public static TruncatePathOption getDefault() {
        return TRUNCATE_SRC_FOLDER;
    }

    /**
     * Creates the {@link PathTruncator} associated with this {@link TruncatePathOption}.
     *
     * @param flattenHierarchy flag indicating whether or not the hierarchy should be flatten
     * @param regex an regular expression that is only required for {@link TruncatePathOption#TRUNCATE_REGEX}
     * @return the associated {@link PathTruncator}
     */
    public PathTruncator createPathTruncator(final boolean flattenHierarchy, final String regex) {
        return m_pathTruncatorFactory.apply(flattenHierarchy, regex);
    }

}
