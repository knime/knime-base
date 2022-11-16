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
 *   Oct 7, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.SpaceAware.Space;
import org.knime.filehandling.core.connections.base.hub.HubAccessUtil.HubAccess;
import org.knime.filehandling.core.util.Icons;

/**
 * {@link JDialog} that allows user to select Hub Space.
 *
 * @author Alexander Bondaletov
 */
final class HubSpaceSelectSubDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final HubAccess m_hubAccess; //NOSONAR not intended for serialization

    private final JTextField m_accountInput;

    private final JTable m_table;

    private final SpacesTableModel m_tableModel;

    private final JButton m_okBtn;

    private final JButton m_cancelBtn;

    private FetchSpacesWorker m_fetchWorker; //NOSONAR not intended for serialization

    private boolean m_fetchForCurrentUser;

    private Space m_selected; //NOSONAR not intended for serialization

    /**
     * @param parent The parent frame.
     * @param hubAccess The {@link HubAccess} object.
     */
    public HubSpaceSelectSubDialog(final Frame parent, final HubAccess hubAccess) {
        super(parent, "Select Hub Space", ModalityType.APPLICATION_MODAL);
        m_hubAccess = hubAccess;

        m_accountInput = new JTextField();
        m_accountInput.setColumns(30);
        m_accountInput.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                fetchSpaces();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                fetchSpaces();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                fetchSpaces();
            }
        });

        m_okBtn = new JButton("OK");
        m_okBtn.addActionListener(e -> onOk());

        m_cancelBtn = new JButton("Cancel");
        m_cancelBtn.addActionListener(e -> onCancel());

        m_tableModel = new SpacesTableModel();
        m_table = new JTable(m_tableModel);
        m_table.setDefaultRenderer(Space.class, new SpaceCellRenderer());
        m_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_table.getSelectionModel().addListSelectionListener(
            e -> m_okBtn.setEnabled(m_table.getSelectedRow() > -1 && !m_tableModel.isInStatusMode()));

        setContentPane(createContentPanel());
        setLocationRelativeTo(parent);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        KNIMEConstants.getKNIMEIcon16X16().ifPresent(i -> setIconImage(i.getImage()));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent we) {
                //handle all window closing events triggered by none of
                //the given buttons
                onCancel();
            }
        });
        pack();
    }

    private JPanel createContentPanel() {
        var panel = new JPanel(new GridBagLayout());

        final var gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.CENTER;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.insets = new Insets(10, 10, 0, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        panel.add(createModeSelectorPanel(), gc);

        gc.gridy += 1;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(10, 10, 10, 10);
        panel.add(new JScrollPane(m_table), gc);

        // buttons
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.ipadx = 20;
        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.gridy += 1;
        gc.insets = new Insets(0, 10, 10, 0);
        panel.add(m_okBtn, gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0;
        gc.ipadx = 10;
        gc.gridx = 1;
        gc.insets = new Insets(0, 5, 10, 10);
        panel.add(m_cancelBtn, gc);

        return panel;
    }

    private JComponent createModeSelectorPanel() {
        var rbCurrentUser = new JRadioButton("Current user");
        rbCurrentUser.addActionListener(e -> setFetchingMode(true));

        var rbOtherUser = new JRadioButton("Other account");
        rbOtherUser.addActionListener(e -> setFetchingMode(false));

        var group = new ButtonGroup();
        group.add(rbCurrentUser);
        group.add(rbOtherUser);
        rbCurrentUser.doClick();

        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.weightx = 0;
        gc.gridx = 0;
        gc.gridy = 0;
        panel.add(new JLabel("Show spaces for:  "), gc);

        gc.gridx += 1;
        panel.add(rbCurrentUser, gc);

        gc.gridx += 1;
        panel.add(rbOtherUser, gc);

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.gridx += 1;
        panel.add(m_accountInput, gc);
        return panel;
    }

    private void setFetchingMode(final boolean currentUser) {
        m_fetchForCurrentUser = currentUser;
        m_accountInput.setEnabled(!currentUser);
        if (!currentUser) {
            m_accountInput.requestFocus();
        }

        fetchSpaces();
    }

    private void fetchSpaces() {
        if (m_fetchWorker != null) {
            m_fetchWorker.cancel(true);
        }

        m_fetchWorker = new FetchSpacesWorker();
        m_fetchWorker.execute();
    }

    private void onOk() {
        int selectedRow = m_table.getSelectedRow();
        if (selectedRow > -1 && !m_tableModel.isInStatusMode()) {
            m_selected = m_tableModel.getItem(m_table.convertRowIndexToModel(selectedRow));
        }
        close();
    }

    private void onCancel() {
        close();
    }

    private void close() {
        setVisible(false);
        dispose();
    }

    /**
     * @return The selected space.
     */
    public Space getSelected() {
        return m_selected;
    }

    /**
     * Displays the dialog and returns the user selection.
     *
     * @param parent The parent frame.
     * @param hubAccess The {@link HubAccess} object.
     * @return The selected space, or <code>null</code> if selection was canceled.
     */
    public static Space showDialog(final Frame parent, final HubAccess hubAccess) {
        var dlg = new HubSpaceSelectSubDialog(parent, hubAccess);
        dlg.setVisible(true);
        return dlg.getSelected();
    }

    private class FetchSpacesWorker extends SwingWorkerWithContext<List<Space>, Void> {

        private final String m_account;

        FetchSpacesWorker() {
            m_account =  m_fetchForCurrentUser ? null : m_accountInput.getText();
        }

        @Override
        protected List<Space> doInBackgroundWithContext() throws Exception {
            if (m_account != null && m_account.isEmpty()) {
                throw new IllegalArgumentException("Please enter account name or ID.");
            }

            Thread.sleep(200);
            if (m_account == null) {
                return m_hubAccess.listSpaces();
            } else {
                return m_hubAccess.listSpacesForAccount(m_account);
            }
        }

        @Override
        protected void doneWithContext() {
            if (isCancelled()) {
                return;
            }

            try {
                m_tableModel.setItems(get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) { // NOSONAR
                m_tableModel.setStatus(ex.getCause().getMessage());
            }
        }
    }

    private static class SpacesTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private static final String[] COLUMNS = new String[]{"Owned by", "Space"};

        private static final int OWNER_COLUMN = 0;

        private static final int SPACE_COLUMN = 1;

        private final List<Space> m_items; //NOSONAR not intended for serialization

        private String m_status;

        public SpacesTableModel() {
            m_items = new ArrayList<>();
        }

        public void setItems(final List<Space> spaces) {
            m_items.clear();
            m_items.addAll(spaces);
            m_items.sort((l,r) -> l.getName().compareToIgnoreCase(r.getName()));
            setStatus(null);
        }

        public Space getItem(final int row) {
            return m_items.get(row);
        }

        public void setStatus(final String status) {
            m_status = status;
            fireTableStructureChanged();
        }

        public boolean isInStatusMode() {
            return m_status != null;
        }

        @Override
        public int getRowCount() {
            if (isInStatusMode()) {
                return 1;
            }

            return m_items.size();
        }

        @Override
        public int getColumnCount() {
            if (isInStatusMode()) {
                return 1;
            }
            return COLUMNS.length;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (isInStatusMode()) {
                return m_status;
            }

            final var space = m_items.get(rowIndex);
            switch (columnIndex) {
                case SPACE_COLUMN:
                    return space;
                case OWNER_COLUMN:
                    return space.getOwner();
                default:
                    throw new IllegalArgumentException("Illegal column index: " + columnIndex);
            }
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            switch (columnIndex) {
                case SPACE_COLUMN:
                    return Space.class;
                case OWNER_COLUMN:
                    return String.class;
                default:
                    throw new IllegalArgumentException("Illegal column index: " + columnIndex);
            }
        }

        @Override
        public String getColumnName(final int column) {
            if (isInStatusMode()) {
                return " ";
            }
            return COLUMNS[column];
        }

    }

    private static class SpaceCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
            final boolean hasFocus, final int row, final int column) {

            var space = (Space)value;
            super.getTableCellRendererComponent(table, space.getName(), isSelected, hasFocus, row, column);

            if (space.isPrivate()) {
                setIcon(Icons.getPrivateSpaceIcon());
            } else {
                setIcon(Icons.getPublicSpaceIcon());
            }
            setToolTipText(createToolTip(space));
            return this;
        }

        private static String createToolTip(final Space space) {
            return String.format(
                "<html><b>ID:</b> %s<br><b>Name:</b> %s<br><b>Location:</b> %s<br><b>Owner:</b> %s</html>", //
                space.getSpaceId(), //
                space.getName(), //
                space.getPath(), //
                space.getOwner());
        }
    }
}
