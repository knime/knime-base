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
 *   Oct 30, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.dialogs.variables;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Optional;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSLocation;

/**
 * A {@link JTable} allowing to edit entries that eventually will be used to create {@link FlowVariable}s based on
 * {@link FSLocation}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class FSLocationVariableTable extends JTable {

    private static final float[] columnWidthPercentage = {0.15f, 0.4f, 0.3f, 0.15f};

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param model the {@link FSLocationVariableTableModel}
     */
    public FSLocationVariableTable(final FSLocationVariableTableModel model) {
        super(model);
        setupTable();
    }

    @Override
    public FSLocationVariableTableModel getModel() {
        return (FSLocationVariableTableModel)super.getModel();
    }

    private void setupTable() {
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        setRowSelectionAllowed(true);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                resizeColumns();
            }
        });

    }

    private void resizeColumns() {
        final TableColumnModel colModel = getColumnModel();
        int tW = colModel.getTotalColumnWidth();
        TableColumn column;
        for (int i = 0; i < getColumnCount(); i++) {
            column = colModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    void loadSettings(final NodeSettingsRO settings) {
        getModel().loadSettingsForDialog(settings);
    }

    void saveSettings(final NodeSettingsWO settings) {
        Optional.ofNullable(getCellEditor()).ifPresent(TableCellEditor::stopCellEditing);
        getModel().saveSettingsForDialog(settings);
    }

    void addEntry() {
        getModel().addRow();
    }

    void removeEntries() {
        getModel().removeRows(getSelectedRows());
    }

    /**
     * Cancels any open editors when the dialog is closed.
     */
    void onClose() {
        Optional.ofNullable(getCellEditor()).ifPresent(TableCellEditor::cancelCellEditing);
    }

}
