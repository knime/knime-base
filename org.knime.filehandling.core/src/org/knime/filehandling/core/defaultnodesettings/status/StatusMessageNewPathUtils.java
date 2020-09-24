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
 *   Sep 24, 2020 (lars.schweikardt): created
 */
package org.knime.filehandling.core.defaultnodesettings.status;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;

/**
 * Utility class to handle the {@link StatusMessage} within the {@link DialogComponentWriterFileChooser}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public final class StatusMessageNewPathUtils {

    private static final StatusMessage MISSING_FOLDERS_MSG =
        DefaultStatusMessage.mkError("Some folders in the specified path are missing.");

    private StatusMessageNewPathUtils() {
        // static utility class
    }

    /**
     * Handler in case the {@link DialogComponentWriterFileChooser} gets a path which does not already exist.
     *
     * @param path the destination {@link Path}
     * @param isCreateMissingFolders flag whether the option to create new folders is checked or not
     * @return the corresponding {@link StatusMessage}
     * @throws AccessDeniedException
     */
    public static StatusMessage handleNewPath(final FSPath path, final boolean isCreateMissingFolders)
        throws AccessDeniedException {
        if (isCreateMissingFolders) {
            return DefaultStatusMessage.SUCCESS_MSG;
        } else {
            return checkIfParentFoldersAreMissing(path);
        }
    }

    /**
     * Returns a {@link StatusMessage} based on an yet non existent path.
     *
     * @param path the destination {@link Path}
     * @return a {@link StatusMessage}
     * @throws AccessDeniedException
     */
    public static StatusMessage checkIfParentFoldersAreMissing(final FSPath path) throws AccessDeniedException {
        final Path parent = path.getParent();
        if (parent == null) {
            return DefaultStatusMessage.SUCCESS_MSG;
        }
        if (FSFiles.exists(parent)) {
            if (!Files.isWritable(parent)) {
                throw ExceptionUtil.createAccessDeniedException(parent);
            }
            return DefaultStatusMessage.SUCCESS_MSG;
        } else {
            // TODO recursively go through ancestors until an existing one is found and check if we can write into it
            return MISSING_FOLDERS_MSG;
        }
    }
}
