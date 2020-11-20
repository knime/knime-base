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
 *   Sep 3, 2020 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes.utils;

import java.nio.file.Path;

import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * The {@link PathRelativizer} for non table input nodes relativizes a path based on the {@link FilterMode} and whether
 * the includeParentFolder is checked or not and returns a relativized string representation of a file {@link Path}
 * based on a root {@link Path}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class PathRelativizerNonTableInput implements PathRelativizer {

    private final boolean m_includeParentFolder;

    private final Path m_rootPath;

    private final FilterMode m_filterMode;

    private final boolean m_flattenHierarchy;

    /**
     * Constructor.
     *
     * @param rootPath the root path
     * @param includeParentFolder flag indicating whether or not the parent folder of the rootPath has to be included
     *            when applying the operation
     * @param filterMode the {@link FilterMode}
     * @param flattenHierarchy flag indicating whether or not the hierarchy has to be flattened
     */
    public PathRelativizerNonTableInput(final Path rootPath, final boolean includeParentFolder,
        final FilterMode filterMode, final boolean flattenHierarchy) {
        m_rootPath = rootPath.toAbsolutePath().normalize();
        m_includeParentFolder = includeParentFolder;
        m_filterMode = filterMode;
        m_flattenHierarchy = flattenHierarchy;
    }

    @Override
    public String apply(final Path sourceFilePath) {
        if (m_filterMode == FilterMode.FILE) {
            return m_rootPath.getFileName().toString();
        } else {
            final Path sourcePath;
            if (m_flattenHierarchy) {
                sourcePath = m_rootPath.resolve(sourceFilePath.getFileName());
            } else {
                sourcePath = sourceFilePath.toAbsolutePath().normalize();
            }
            if (m_includeParentFolder) {
                return m_rootPath.getParent() == null ? m_rootPath.getRoot().relativize(sourcePath).toString()
                    : m_rootPath.getParent().relativize(sourcePath).toString();
            } else {
                return m_rootPath.relativize(sourcePath).toString();
            }
        }
    }
}
