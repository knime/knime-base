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
 *   Apr 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections;

import java.util.Arrays;

import org.knime.filehandling.core.connections.meta.FSType;

/**
 * Lists the available options for the relative to file system.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public enum RelativeTo {

        /**
         * Relative to space.
         */
        SPACE("knime.space", "Current Hub Space", FSType.RELATIVE_TO_SPACE),

        /**
         * Relative to current mountpoint.
         */
        MOUNTPOINT("knime.mountpoint", "Current mountpoint", FSType.RELATIVE_TO_MOUNTPOINT),

        /**
         * Relative to current workflow.
         */
        WORKFLOW("knime.workflow", "Current workflow", FSType.RELATIVE_TO_WORKFLOW),

        /**
         * Relative to workflow data area.
         */
        WORKFLOW_DATA("knime.workflow.data", "Current workflow data area", FSType.RELATIVE_TO_WORKFLOW_DATA_AREA);


    private final String m_settingsValue;

    private final String m_label;

    private final FSType m_fsType;

    private RelativeTo(final String settingsValue, final String label, final FSType fsType) {
        m_settingsValue = settingsValue;
        m_label = label;
        m_fsType = fsType;
    }

    @Override
    public String toString() {
        return m_label;
    }

    /**
     * Retrieves the {@link RelativeTo} corresponding to the provided string (as obtained from
     * {@link RelativeTo#getSettingsValue()}).
     *
     * @param string representation of the {@link RelativeTo} constant (as obtained from
     *            {@link RelativeTo#getSettingsValue()}).
     * @return the {@link RelativeTo} constant corresponding to <b>string</b>
     */
    public static RelativeTo fromSettingsValue(final String string) {
        return Arrays.stream(RelativeTo.values())//
            .filter(r -> r.m_settingsValue.equals(string))//
            .findFirst()//
            .orElseThrow(() -> new IllegalArgumentException(
                String.format("Unknown relative to option '%s' encountered.", string)));
    }

    /**
     * Provides a user-friendly label for display purposes.
     *
     * @return a user-friendly label for display purposes.
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * Provides the settings value.
     *
     * @return the settings value
     */
    public String getSettingsValue() {
        return m_settingsValue;
    }

    /**
     * @return the {@link FSType} of this particular type of file system.
     */
    public FSType toFSType() {
        return m_fsType;
    }
}