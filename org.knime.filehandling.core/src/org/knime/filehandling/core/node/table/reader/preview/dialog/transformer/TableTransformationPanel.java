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
 *   Sep 11, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Panel consisting of a table allowing to manipulate how the table headers should be transformed as well as a number of
 * buttons for changing the filter behavior and resetting the table.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TableTransformationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTable m_transformationTable;

    private final TableTransformationTableModel<?> m_tableModel;

    private final JButton m_resetMenuBtn = new JButton("Reset actions");

    private final JPopupMenu m_resetPopup = new JPopupMenu();

    private final JMenuItem m_resetAllBtn = new JMenuItem("Reset all");

    private final JMenuItem m_resetNames = new JMenuItem("Reset names");

    private final JMenuItem m_resetPositions = new JMenuItem("Reset order");

    private final JMenuItem m_resetTypes = new JMenuItem("Reset types");

    private final JMenuItem m_includeAll = new JMenuItem("Reset filter");

    private final JButton m_moveUp = new JButton("Move up", SharedIcons.MOVE_UP.get());

    private final JButton m_moveDown = new JButton("Move down", SharedIcons.MOVE_DOWN.get());

    private final JCheckBox m_enforceTypes = new JCheckBox("Enforce types");

    private final JLabel m_colFilterModeLabel = new JLabel("Take columns from:");

    private final ColumnFilterModePanel m_columnFilterModePanel;
    /**
     * Constructor.
     *
     * @param model the underlying {@link TableTransformationTableModel}
     * @param productionPathProvider provides a list of {@link ProductionPath} for a given external type
     * @param includeColumnFilterButtons {@code true} if the column filter buttons should be included
     */
    public TableTransformationPanel(final TableTransformationTableModel<?> model,
        final Function<Object, List<ProductionPath>> productionPathProvider, final boolean includeColumnFilterButtons) {
        this(model, productionPathProvider, includeColumnFilterButtons, false);
    }
    /**
     * Constructor.
     *
     * @param model the underlying {@link TableTransformationTableModel}
     * @param productionPathProvider provides a list of {@link ProductionPath} for a given external type
     * @param includeColumnFilterButtons {@code true} if the column filter buttons should be included
     * @param showFullProductionPath <code>true</code> if the full production path should be displayed
     * in the transformation table otherwise only the target DataType is displayed
     */
    public TableTransformationPanel(final TableTransformationTableModel<?> model,
        final Function<Object, List<ProductionPath>> productionPathProvider, final boolean includeColumnFilterButtons,
        final boolean showFullProductionPath) {
        super(new GridBagLayout());
        if (includeColumnFilterButtons) {
            m_columnFilterModePanel = new ColumnFilterModePanel(model.getColumnFilterModeModel());
        } else {
            m_columnFilterModePanel = null;
        }
        m_transformationTable = new JTable(model);
        m_tableModel = model;
        setupTable(model, productionPathProvider, showFullProductionPath);

        m_enforceTypes.setModel(model.getEnforceTypesModel());

        m_resetPopup.add(m_resetPositions);
        m_resetPopup.add(m_includeAll);
        m_resetPopup.add(m_resetNames);
        m_resetPopup.add(m_resetTypes);
        m_resetPopup.add(m_resetAllBtn);

        m_resetMenuBtn.addActionListener(l -> displayResetPopup());

        m_resetAllBtn.addActionListener(l -> m_tableModel.resetAll());
        m_resetNames.addActionListener(l -> m_tableModel.resetNames());
        m_resetPositions.addActionListener(l -> m_tableModel.resetPositions());
        m_resetTypes.addActionListener(l -> m_tableModel.resetProductionPaths());
        m_includeAll.addActionListener(l -> m_tableModel.resetKeep());

        m_moveUp.addActionListener(l -> move(MoveDirection.UP));
        m_moveDown.addActionListener(l -> move(MoveDirection.DOWN));
        m_moveUp.setEnabled(false);
        m_moveDown.setEnabled(false);

        m_transformationTable.getSelectionModel().addListSelectionListener(l -> updateMoveButtons());

        setBorder(BorderFactory.createTitledBorder("Transformations"));

        final GBCBuilder gbc = new GBCBuilder()//
            .resetPos()//
            .anchorPageStart().insets(0, 0, 5, 5);
        add(m_resetMenuBtn, gbc.build());
        add(m_moveUp, gbc.incX().build());
        add(m_moveDown, gbc.incX().build());
        add(m_enforceTypes, gbc.incX().build());
        if (includeColumnFilterButtons) {
            add(m_colFilterModeLabel, gbc.incX().insetTop(3).build());
            add(m_columnFilterModePanel, gbc.incX().insetTop(0).build());
        }
        add(new JPanel(), gbc.incX().fillBoth().setWeightX(1.0).build());
        add(new JScrollPane(m_transformationTable), gbc.resetX().incY().widthRemainder().setWeightY(1.0).build());
    }

    private void displayResetPopup() {
        final int height = m_resetMenuBtn.getSize().height;
        m_resetPopup.show(m_resetMenuBtn, 0, height);
    }

    private void updateMoveButtons() {
        final int row = m_transformationTable.getSelectedRow();
        m_moveUp.setEnabled(row > 0);
        m_moveDown.setEnabled(row >= 0 && row < m_tableModel.getRowCount() - 1);
    }

    private void move(final MoveDirection direction) {
        final int row = m_transformationTable.getSelectedRow();
        if (row == -1) {
            // happens e.g. when the dialog is opened
            return;
        }
        m_tableModel.reorder(row, direction);
        m_transformationTable.getSelectionModel().clearSelection();
        final int newSelection = direction == MoveDirection.UP ? (row - 1) : (row + 1);
        m_transformationTable.getSelectionModel().setSelectionInterval(newSelection, newSelection);
    }

    private void setupTable(final TableTransformationTableModel<?> model,
        final Function<Object, List<ProductionPath>> productionPathProvider, final boolean showFullProductionPath) {
        TableColumnModel columnModel = m_transformationTable.getColumnModel();
        columnModel.getColumn(0).setMaxWidth(30);
        columnModel.getColumn(1).setMaxWidth(30);
        final TransformationTableHeader header = new TransformationTableHeader(columnModel);
        header.setReorderingAllowed(false);
        m_transformationTable.setRowSelectionAllowed(true);
        m_transformationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_transformationTable.setTableHeader(header);
        m_transformationTable.setDefaultEditor(String.class, new ColumnNameCellEditor(model));
        // this property ensures that currently edited values are committed to the model if the table loses focus
        m_transformationTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        if (showFullProductionPath) {
            m_transformationTable.setDefaultEditor(ProductionPath.class,
                new ProductionPathCellEditor(productionPathProvider,
                    new KnimeTypeFullProductionPathListCellRenderer()));
            m_transformationTable.setDefaultRenderer(ProductionPath.class,
                new KnimeTypeFullProductionPathTableCellRenderer());
        } else {
            m_transformationTable.setDefaultEditor(ProductionPath.class,
                new ProductionPathCellEditor(productionPathProvider, new KnimeTypeProductionPathListCellRenderer()));
            m_transformationTable.setDefaultRenderer(ProductionPath.class,
                new KnimeTypeProductionPathTableCellRenderer());
        }
        m_transformationTable.setDefaultRenderer(String.class, new ColumnNameCellRenderer());
        m_transformationTable.setDefaultRenderer(DataColumnSpec.class, new SpecCellRenderer());
        m_transformationTable.setRowHeight(25);
        // enable drag-and-drop
        m_transformationTable.setDragEnabled(true);
        m_transformationTable.setDropMode(DropMode.INSERT_ROWS);
        m_transformationTable.setTransferHandler(new TransformationTableRowTransferHandler(m_transformationTable));
    }

    /**
     * Sets whether the radio buttons for the table transformation panel are enabled or disabled.
     *
     * @param enabled {@code true} if the buttons should be enabled
     */
    public void setColumnFilterModeEnabled(final boolean enabled) {
        if (m_columnFilterModePanel != null) {
            m_colFilterModeLabel.setEnabled(enabled);
            m_columnFilterModePanel.setEnabled(enabled);
        }
    }

    /**
     * Commits any open changes to the underlying model and throws an {@link InvalidSettingsException} if any names are
     * invalid.
     *
     * @throws InvalidSettingsException if the names are invalid i.e. contains empty or duplicate names
     */
    public void commitChanges() throws InvalidSettingsException {
        final CellEditor editor = m_transformationTable.getCellEditor();
        if (editor != null) {
            editor.stopCellEditing();
        }
        CheckUtils.checkSetting(IntStream.range(0, m_tableModel.getRowCount()).noneMatch(i -> !m_tableModel.isValid(i)),
            "Some names are invalid.");
    }

    /**
     * Cancels any open editors when the dialog is closed.
     */
    public void onClose() {
        if (m_transformationTable.isEditing()) {
            // if the user closes the dialog while editing
            // the editor will commit its value when the dialog is reopened
            m_transformationTable.getCellEditor().cancelCellEditing();
        }
    }

}
