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
 *   Nov 2, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.dialogs.variables;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.SystemUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;

/**
 * A {@link ChangeListener} that updates the {@link FSLocationVariableTableModel#setPathBaseLocation(String)} whenever
 * the location of the {@link AbstractSettingsModelFileChooser} changes.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class BaseLocationListener implements ChangeListener {

    private final FSLocationVariableTableModel m_tableModel;

    private final AbstractSettingsModelFileChooser<?> m_baseLocationModel;

    private String m_pathSeparator;

    private FSLocation m_lastCategory;

    /**
     * Constructor.
     *
     * @param varTableModel the {@link FSLocationVariableTableModel}
     * @param baseLocationModel the {@link AbstractSettingsModelFileChooser}
     */
    public BaseLocationListener(final FSLocationVariableTableModel varTableModel,
        final AbstractSettingsModelFileChooser<?> baseLocationModel) {
        m_tableModel = varTableModel;
        m_baseLocationModel = baseLocationModel;

        // This is a work around and needs to be replaced once AP-14001 has been implemented
        // corresponding ticket AP-15353
        m_lastCategory = m_baseLocationModel.getLocation();
        assignSeparator(m_lastCategory.getFSCategory());
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
        m_tableModel.setPathBaseLocation(getBaseLocation());
    }

    private String getBaseLocation() {
        updateSeperator();
        final String base = m_baseLocationModel.getLocation().getPath();
        return base.endsWith(m_pathSeparator) ? base : (base + m_pathSeparator);
    }

    private void updateSeperator() {
        final FSLocation curLocation = m_baseLocationModel.getLocation();
        final FSCategory curCategory = curLocation.getFSCategory();
        if (m_lastCategory.getFSCategory() != curCategory) {
            assignSeparator(curCategory);
        }
        m_lastCategory = curLocation;
    }

    private void assignSeparator(final FSCategory curCategory) {
        if ((curCategory == FSCategory.LOCAL || curCategory == FSCategory.RELATIVE) && SystemUtils.IS_OS_WINDOWS) {
            m_pathSeparator = "\\";
        } else {
            m_pathSeparator = "/";
        }
    }

}
