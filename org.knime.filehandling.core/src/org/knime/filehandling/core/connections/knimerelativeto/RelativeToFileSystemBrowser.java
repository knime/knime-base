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
 *   Feb 11, 2020 (Sascha Wolke, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.knimerelativeto;

import javax.swing.filechooser.FileView;

import org.knime.filehandling.core.connections.FSFileSystem;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.base.WorkflowAwareFileView;
import org.knime.filehandling.core.defaultnodesettings.FilesHistoryPanel;
import org.knime.filehandling.core.filechooser.NioFileSystemBrowser;
import org.knime.filehandling.core.filechooser.NioFileSystemView;

/**
 * A KNIME File System Browser allowing the {@link FilesHistoryPanel} to browse a local KNIME relative-to file system.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class RelativeToFileSystemBrowser extends NioFileSystemBrowser {

    private final FSFileSystem<?> m_fileSystem;

    /**
     * Creates a new local KNIME relative-to file system browser with a view and base location.
     *
     * @param fileSystem the file system to use
     */
    public RelativeToFileSystemBrowser(final FSFileSystem<?> fileSystem) {
        this(fileSystem, fileSystem.getWorkingDirectory(), fileSystem.getWorkingDirectory());
    }

    public RelativeToFileSystemBrowser(final FSFileSystem<?> fileSystem, final FSPath homeDir,
        final FSPath defaultDir) {
        super(new NioFileSystemView(fileSystem, homeDir, defaultDir));
        m_fileSystem = fileSystem;
    }

    @Override
    protected FileView getFileView() {
        return new WorkflowAwareFileView();
    }

    /**
     * Convert the selected file to a relative path in workflow relative mode.
     */
    @Override
    protected String postprocessSelectedFilePath(final String selectedFile) {
        return m_fileSystem.getWorkingDirectory().relativize(m_fileSystem.getPath(selectedFile)).toString();
    }
}
