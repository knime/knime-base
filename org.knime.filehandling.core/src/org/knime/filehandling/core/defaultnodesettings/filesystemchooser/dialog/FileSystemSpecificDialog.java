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
 *   Apr 22, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog;

import java.awt.Color;
import java.awt.Component;

import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice;
import org.knime.filehandling.core.defaultnodesettings.FileSystemChoice.Choice;

/**
 * Interface for dialogs specific to individual file systems, e.g. the combo box in case of the mountpoint file system
 * or the timeout spinner for the custom URL file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface FileSystemSpecificDialog {

    /**
     * Returns the {@link Component} for the file system specifier.</br>
     * NOTE: Only call this method if {@link FileSystemSpecificDialog#hasSpecifierComponent()} returned {@code true}.
     *
     * @return the dialog component for the file system specifier
     */
    Component getSpecifierComponent();

    /**
     * Returns {@code true} if this dialog provides a specifier component via {@link FileSystemSpecificDialog#getSpecifierComponent()}.
     *
     * @return {@code true} if this dialog has a specifier component
     */
    boolean hasSpecifierComponent();

    /**
     * Enables/disables all components in this dialog.
     *
     * @param enabled {@code true} if the dialog should be enabled, {@code false} otherwise
     */
    void setEnabled(boolean enabled);

    /**
     * Sets the provided tooltip on the components of this dialog.
     *
     * @param tooltip to set
     */
    void setTooltip(final String tooltip);

    /**
     * Returns the file system choice represented by this dialog.
     *
     * @return the {@link FileSystemChoice}
     */
    Choice getChoice();

    /**
     * Returns the {@link Color} to display the file system name in.
     *
     * @return the color to display the file system name in
     */
    Color getTextColor();

}
