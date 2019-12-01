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
 *   29.11.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.Color;
import java.awt.Component;
import java.util.Optional;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * Renderer for file system choices.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FileSystemChoiceRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    private Optional<FSConnection> m_fs = Optional.empty();

    /**
     * Creates a list renderer for the file system choice lists of the {@link DialogComponentFileChooser2}
     */
    public FileSystemChoiceRenderer() {

    }

    /**
     * Creates a list renderer for the file system choice lists of the {@link DialogComponentFileChooser2}
     *
     * @param fs optional for a connected file system connection.
     */
    public FileSystemChoiceRenderer(final Optional<FSConnection> fs) {
        m_fs = fs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {

        if (value instanceof FileSystemChoice) {
            final FileSystemChoice fsChoice = (FileSystemChoice)value;

            setForeground(getForegroundColor(fsChoice, list.getParent().getForeground(), m_fs));

            setText(fsChoice.getId());
        }
        if (!isSelected) {
            setBackground(list.getBackground());
        } else {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }

        return this;
    }

    /**
     * Returns the foreground color for the given FileSystemChoice
     *
     * @param fsChoice the FileSystemChoice
     * @param defaultColor the default color
     * @param connection optional for a connected file system connection
     * @return the foreground color for the given FileSystemChoice
     */
    protected static Color getForegroundColor(final FileSystemChoice fsChoice, final Color defaultColor,
        final Optional<FSConnection> connection) {
        if (fsChoice.getType().equals(Choice.CONNECTED_FS) && (!connection.isPresent())) {
            return Color.RED;
        } else {
            return defaultColor;
        }
    }

}
