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
 *   Apr 1, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.truncator.impl;

import java.nio.file.Path;

import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.utility.nodes.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.truncator.TruncationException;

/**
 * Depending on its initialization this truncator either removes truncates either the whole base folder or everything up
 * to the base folder itself.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class RelativePathTruncator extends AbstractPathTruncator {

    final Path m_baseFolder;

    /**
     * Constructor. The base folder defines the common prefix of all paths that need to be truncated.
     *
     * @param baseFolder the base folder, i.e., the common prefix of all paths that have to be truncated
     * @param filterMode depending on the filter mode either the base folder itself will also be truncated or everything
     *            up to the base folder itself
     */
    public RelativePathTruncator(final Path baseFolder, final FilterMode filterMode) {
        m_baseFolder = getBaseFolder(baseFolder, filterMode);
    }

    private static Path getBaseFolder(final Path baseFolder, final FilterMode filterMode) {
        final Path absNorm = makeAbsoluteNormalized(baseFolder);
        switch (filterMode) {
            case FILE:
            case FOLDER:
                return absNorm.getParent();
            case FILES_IN_FOLDERS:
                return absNorm;
            default:
                throw new IllegalArgumentException(
                    String.format("The filter mode '%s' is not supported", filterMode.getText()));
        }
    }

    @Override
    protected Path truncate(final Path path) {
        final Path absNormPath = makeAbsoluteNormalized(path);
        if (absNormPath.equals(m_baseFolder)) {
            throw new TruncationException(
                String.format("Removing the source folder from itself is prohibited ('%s')", path.toString()),
                TruncatePathOption.RELATIVE);
        }
        if (m_baseFolder != null) {
            return m_baseFolder.relativize(absNormPath);
        } else {
            return absNormPath;
        }
    }

}
