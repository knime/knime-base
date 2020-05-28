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
 *   May 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.fileselection;

import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.knime.core.node.util.StringHistory;

/**
 * {@link ComboBoxModel} based on {@link StringHistory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class HistoryComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {

    private static final long serialVersionUID = 1L;

    private final transient StringHistory m_history;

    private String m_selected;

    HistoryComboBoxModel(final String id, final int length) {
        m_history = StringHistory.getInstance(id, length);
    }

    @Override
    public int getSize() {
        return m_history.getHistory().length;
    }

    @Override
    public String getElementAt(final int index) {
        return m_history.getHistory()[index];
    }

    @Override
    public void setSelectedItem(final Object anItem) {
        if (!Objects.equals(m_selected, anItem)) {
            m_selected = (String)anItem;
            fireContentsChanged(this, -1, -1);
        }
    }

    void addCurrentSelectionToHistory() {
        if (m_selected != null && m_selected.trim().length() > 0) {
            m_history.add(m_selected);
        }
    }

    @Override
    public String getSelectedItem() {
        return m_selected;
    }

}