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
 *   Apr 19, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.compress.table;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.function.Function;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.truncator.TruncatePathOption;
import org.knime.filehandling.utility.nodes.truncator.TruncationPanel;

/**
 * This class extends the {@link TruncationPanel} by an additional archive entry name selection based on a String or
 * Path column.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TableTruncationPanel extends TruncationPanel {

    private final JRadioButton m_fromColumn;

    private DialogComponentColumnNameSelection m_entryNameColSelection;

    /**
     * Constructor.
     *
     * @param borderTitle the border title, which can be null
     * @param truncationSettings the truncation settings
     * @param labels the lalbels for the {@link TruncatePathOption}s
     * @param tablePortIdx the index of the input table containing the column containing that strings/paths that contain
     *            the destination file/folder name
     */
    @SuppressWarnings("unchecked")
    public TableTruncationPanel(final String borderTitle, final TableTruncationSettings truncationSettings,
        final Function<TruncatePathOption, String> labels, final int tablePortIdx) {
        super(borderTitle, truncationSettings, labels);
        m_entryNameColSelection = new DialogComponentColumnNameSelection(truncationSettings.getEntryNameColModel(),
            null, tablePortIdx, FSLocationValue.class, StringValue.class);
        m_fromColumn = new JRadioButton("From table column");

    }

    /**
     * {@inheritDoc} In addition the dialog component to select the archive entry name is added.
     */
    @Override
    protected GBCBuilder fillPanel(final JPanel panel, final ButtonGroup buttonGroup, GBCBuilder gbc) {
        gbc = super.fillPanel(panel, buttonGroup, gbc);
        gbc = gbc.incY();
        panel.add(getFromColumnSelectionPanel(buttonGroup), gbc.build());
        return gbc;

    }

    private Component getFromColumnSelectionPanel(final ButtonGroup buttonGroup) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();

        buttonGroup.add(m_fromColumn);
        m_fromColumn.addActionListener(l -> togglePrefixField());
        m_fromColumn.setSelected(false);
        panel.add(m_fromColumn, gbc.build());
        panel.add(m_entryNameColSelection.getComponentPanel(), gbc.incX().build());
        return panel;
    }

    @Override
    protected void togglePrefixField() {
        final boolean fromCol = m_fromColumn.isSelected();
        m_entryNameColSelection.getModel().setEnabled(fromCol);
        if (fromCol) {
            enablePrefix(false);
        } else {
            super.togglePrefixField();
        }
    }

    @Override
    protected void selectOption() {
        super.selectOption();
        m_fromColumn.setSelected(((TableTruncationSettings)getSettings()).entryNameDefinedByTableColumn());
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        m_fromColumn.setEnabled(enabled);
        m_entryNameColSelection.getModel().setEnabled(enabled);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        ((TableTruncationSettings)getSettings()).entryNameDefinedByTableColumn(m_fromColumn.isSelected());
        super.saveSettingsTo(settings);
        m_entryNameColSelection.saveSettingsTo(settings);
    }

    @Override
    public void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_entryNameColSelection.loadSettingsFrom(settings, specs);
        super.loadSettings(settings, specs);
    }

}
