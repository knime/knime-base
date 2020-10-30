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
 *   Sep 17, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;

import javax.activation.ActivationDataFlavor;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

/**
 * Allows to reorder rows using drag-and-drop.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TransformationTableRowTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;

    private static final DataFlavor ROW_INDEX_FLAVOR = new ActivationDataFlavor(Integer.class,
        "application/x-java-Integer;class=java.lang.Integer", "Integer Row Index");

    private final JTable m_table;

    TransformationTableRowTransferHandler(final JTable table) {
        m_table = table;
    }

    @Override
    protected Transferable createTransferable(final JComponent c) {
        assert c == m_table;
        return new DataHandler(m_table.getSelectedRow(), ROW_INDEX_FLAVOR.getMimeType());
    }

    @Override
    public boolean canImport(final TransferHandler.TransferSupport info) {
        boolean b = info.getComponent() == m_table && info.isDrop() && info.isDataFlavorSupported(ROW_INDEX_FLAVOR);
        m_table.setCursor(b ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
        return b;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    public boolean importData(final TransferHandler.TransferSupport info) {
        JTable target = (JTable)info.getComponent();
        JTable.DropLocation dl = (JTable.DropLocation)info.getDropLocation();
        int index = dl.getRow();
        int max = m_table.getModel().getRowCount();
        if (index < 0 || index > max) {
            index = max;
        }
        target.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        try {
            Integer rowFrom = (Integer)info.getTransferable().getTransferData(ROW_INDEX_FLAVOR);
            if (rowFrom != -1 && rowFrom != index) {
                ((TransformationTableModel<?>)m_table.getModel()).reorder(rowFrom, index);
                if (index > rowFrom) {
                    index--;
                }
                target.getSelectionModel().addSelectionInterval(index, index);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void exportDone(final JComponent c, final Transferable t, final int act) {
        if ((act == TransferHandler.MOVE) || (act == TransferHandler.NONE)) {
            m_table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
}