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
 *   Feb 22, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.truncator.impl;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.knime.filehandling.utility.nodes.compress.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.compress.truncator.TruncationException;

/**
 * Truncates the source folder w.r.t. the provided regular expression. For a given regex = "o.*b", base folder =
 * "/foo/bar" and path = "/foo/bar/subfolder/file.txt" the truncated string has the form "far/subfolder/file.txt"
 * (far/file.txt flattened). In case that base folder and path are equal and the regex matches the whole base folder an
 * exception is thrown as this would result in an empty string.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class RegexTruncator extends AbstractPathTruncator {
    private final String m_regex;

    /**
     * Constructor.
     *
     * @param flattenHierarchy flag indicating whether or not to flatten the hierarchy
     * @param regex the regex string
     */
    public RegexTruncator(final boolean flattenHierarchy, final String regex) {
        super(flattenHierarchy);
        m_regex = regex;
    }

    @Override
    @SuppressWarnings("resource")
    protected Path truncatePath(final Path baseFolder, final Path path) {
        final Path truncatedBaseFolder;
        if (baseFolder == null) {
            truncatedBaseFolder = null;
        } else {
            final FileSystem fileSystem = baseFolder.getFileSystem();
            final String truncatedBaseString = baseFolder.toString().replaceFirst(m_regex, "")
                .replaceFirst("^\\" + fileSystem.getSeparator() + "+", "");
            truncatedBaseFolder = fileSystem.getPath(truncatedBaseString).normalize();
            if (isSamePath(path, baseFolder)) {
                if (truncatedBaseString.isEmpty()) {
                    throw new TruncationException(String
                        .format("The regular expression '%s' truncates the full path '%s'.", m_regex, path.toString()),
                        TruncatePathOption.TRUNCATE_REGEX);
                } else {
                    return relativizeAgainstRoot(truncatedBaseFolder);
                }
            }
        }
        Path relPath = path.normalize();
        if (flattenHierarchy()) {
            relPath = relPath.getFileName();
        } else if (baseFolder != null) {
            relPath = baseFolder.normalize().relativize(relPath);
        }
        final Path truncatedPath = truncatedBaseFolder == null ? relPath : truncatedBaseFolder.resolve(relPath);
        return relativizeAgainstRoot(truncatedPath);
    }
}