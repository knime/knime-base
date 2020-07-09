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
 *   23.09.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;

/**
 * This converter is used to handle legacy {@link DialogComponentFileChooser} settings and adapt them based on the saved
 * path or URL.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public final class FileChooserSettingsConverter {

    /** Node logger */
    static final NodeLogger LOGGER = NodeLogger.getLogger(FileChooserSettingsConverter.class);

    /**
     * Private constructor for utility class.
     */
    private FileChooserSettingsConverter() {
        // private constructor for utility class
    }

    /**
     * Converts the settings of the given {@link SettingsModelFileChooser2} according to the saved path or URL String.
     *
     * @param settings the settings to convert
     */
    public static void convert(final SettingsModelFileChooser2 settings) {
        final String path = settings.getPathOrURL();

        if (path != null && !path.isEmpty()) {
            if (!path.contains("://") && !path.startsWith("file:/")) {
                setLocal(path, settings);
            } else {
                setCustom(path, settings);
            }
        }
    }

    private static void setCustom(final String string, final SettingsModelFileChooser2 settings) {
        settings.setFileSystem(FileSystemChoice.getCustomFsUrlChoice().getId());
        settings.setPathOrURL(string);
    }

    private static void setLocal(final String string, final SettingsModelFileChooser2 settings) {
        settings.setFileSystem(FileSystemChoice.getLocalFsChoice().getId());
        settings.setPathOrURL(string);
    }

}
