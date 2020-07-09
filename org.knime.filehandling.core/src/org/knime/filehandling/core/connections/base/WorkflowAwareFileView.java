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
 *   18.11.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.knimerelativeto.BaseRelativeToFileSystem;
import org.knime.filehandling.core.connections.knimerelativeto.RelativeToPath;
import org.knime.filehandling.core.connections.knimeremote.KNIMERemotePath;
import org.knime.filehandling.core.filechooser.NioFileView;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * FileView that shows a KNIME icon for workflows
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @author Sascha Wolke, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class WorkflowAwareFileView extends NioFileView {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowAwareFileView.class);

    private Icon m_workflowIcon = null;

    @SuppressWarnings("resource")
    @Override
    public Icon getIcon(final File f) {
        try {
            final Icon workflowIcon = getWorkflowIcon();
            final Path path = f.toPath();

            if (workflowIcon != null && path instanceof RelativeToPath) {
                final RelativeToPath relativePath = (RelativeToPath)path;
                final BaseRelativeToFileSystem fs = (BaseRelativeToFileSystem)relativePath.getFileSystem();
                if (fs.isWorkflowDirectory(relativePath)) {
                    return workflowIcon;
                }

            } else if (workflowIcon != null && path instanceof KNIMERemotePath) {
                final KNIMERemotePath knimeRemotePath = (KNIMERemotePath)path;
                if (knimeRemotePath.isWorkflow()) {
                    return workflowIcon;
                }
            }

        } catch (final IOException e) {
            // something went wrong, use default icon
            LOGGER.debug(e);
            throw new UncheckedIOException(e);
        }

        return super.getIcon(f);
    }

    /**
     * Try to load the workflow icon from the bundle.
     *
     * @return Workflow icon or {@code null}
     */
    private synchronized Icon getWorkflowIcon() throws IOException {
        if (m_workflowIcon == null) {
            final Bundle currBundle = FrameworkUtil.getBundle(WorkflowAwareFileView.class);
            final IPath iconPath = new org.eclipse.core.runtime.Path("icons").append("knime_default.png");
            final URL iconUrl = FileLocator.findEntries(currBundle, iconPath)[0];
            m_workflowIcon = new ImageIcon(iconUrl);
        }

        return m_workflowIcon;
    }
}
