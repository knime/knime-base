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
 *   Dec 1, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.connections.base.auth;

import org.knime.core.node.util.CheckUtils;

/**
 * Value class to describe an authentication scheme.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public final class AuthType {

    private final String m_settingsKey;

    private final String m_text;

    private final String m_tooltip;

    /**
     * @param settingsKey
     * @param text
     * @param tooltip
     */
    public AuthType(final String settingsKey, final String text, final String tooltip) {
        CheckUtils.checkArgumentNotNull(settingsKey, "Settings key must not be null");
        CheckUtils.checkArgumentNotNull(text, "Text must not be null");
        CheckUtils.checkArgumentNotNull(tooltip, "Tooltip key must not be null");
        m_settingsKey = settingsKey;
        m_text = text;
        m_tooltip = tooltip;
    }

    /**
     * @return the settingsKey
     */
    public String getSettingsKey() {
        return m_settingsKey;
    }

    /**
     * @return the text
     */
    public String getText() {
        return m_text;
    }

    /**
     * @return the tooltip
     */
    public String getTooltip() {
        return m_tooltip;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_settingsKey == null) ? 0 : m_settingsKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AuthType other = (AuthType)obj;
        return m_settingsKey.equals(other.m_settingsKey);
    }

    @Override
    public String toString() {
        return m_settingsKey;
    }
}
