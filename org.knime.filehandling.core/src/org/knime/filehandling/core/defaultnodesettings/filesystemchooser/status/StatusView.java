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
 *   May 8, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.defaultnodesettings.WordWrapJLabel;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.status.StatusMessage.MessageType;

/**
 * A view that displays status messages.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class StatusView {

    private final WordWrapJLabel m_statusLabel;

    /**
     * Constructor.
     *
     * @param widthInPixel status label width in pixels
     */
    public StatusView(final int widthInPixel) {
        m_statusLabel = new WordWrapJLabel(" ", widthInPixel);
    }

    /**
     * Sets a new status message.
     *
     * @param message the {@link StatusMessage} to set
     */
    public void setStatus(final StatusMessage message) {
        m_statusLabel.setText(message.getMessage());
        m_statusLabel.setIcon(getIcon(message.getType()));
        m_statusLabel.setForeground(getColor(message.getType()));
    }

    private static Icon getIcon(final MessageType type) {
        switch (type) {
            case ERROR:
                return SharedIcons.ERROR.get();
            case WARNING:
                return SharedIcons.WARNING_YELLOW.get();
            case INFO:
            default:
                return null;

        }
    }

    private static Color getColor(final MessageType type) {
        switch (type) {
            case ERROR:
                return Color.RED;
            default:
                return Color.BLACK;
        }
    }

    /**
     * Clears the status.
     */
    public void clearStatus() {
        m_statusLabel.setText(" ");
        m_statusLabel.setIcon(null);
    }

    /**
     * Returns the label that is used to display the status.
     *
     * @return the label containing the status message
     */
    public JLabel getLabel() {
        return m_statusLabel;
    }



}
