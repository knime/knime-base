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
 *   May 26, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer.config;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Variants of line brakes across different operating systems.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public enum LineBreakTypes {
        /** System default from System.getProperty("line.separator") */
        SYS_DEFAULT(System.getProperty("line.separator"), "System Default"),

        /** Linux and Unix line brakes, LF only. */
        UNIX_LINUX(new String(new char[]{10}), "Linux/Unix Line break"),

        /** Windows style line breaks CR + LF. */
        WINDOWS(new String(new char[]{13, 10}), "Windows Line break"),

        /** Old Mac OS style (until Mac OS9), CR only */
        MAC_OS9(new String(new char[]{13}), "Mac OS9 Line break");

    private static final String CFG_LINE_ENDING_MODE = "line_separator";

    private final String m_lineBreakType;

    private final String m_displayName;

    private LineBreakTypes(final String lBreakType, final String displayName) {
        m_lineBreakType = lBreakType;
        m_displayName = displayName;
    }

    /** @return the mode specific line ending. */
    public String getLineBreak() {
        return m_lineBreakType;
    }

    @Override
    public String toString() {
        return m_displayName;
    }

    /**
     * Saves the {@link LineBreakTypes} to the settings
     *
     * @param settings the settings
     */
    void saveSettings(final NodeSettingsWO settings) {
        final String value;
        if (this == SYS_DEFAULT) {
            value = null;
        } else {
            value = getLineBreak();
        }
        settings.addString(CFG_LINE_ENDING_MODE, value);
    }

    /**
     * Loads the settings
     *
     * @param settings the settings holding the {@link LineBreakTypes} to be loaded
     * @return the loaded {@link LineBreakTypes}
     * @throws InvalidSettingsException - If the settings are not valid
     */
    static LineBreakTypes loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String val = settings.getString(CFG_LINE_ENDING_MODE);
        if (val == null) {
            return SYS_DEFAULT;
        }
        return Arrays.stream(values())//
            .filter(lbt -> lbt != SYS_DEFAULT) // SYS_DEFAULT is written as null & handled separately above
            .filter(lbt -> lbt.getLineBreak().equals(val))//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("Unable to parse the line break type"));
    }

}
