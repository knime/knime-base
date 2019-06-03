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
 *   May 27, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.duplicates;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.base.node.preproc.duplicates.DuplicateFilterSettings.RowSelectionType;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.util.CheckUtils;

/**
 * The duplicates handler node dialog.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class DuplicateFilterNodeDialog extends NodeDialogPane {

    private static final String OPTIONS_TITLE = "Choose columns for duplicates detection";

    private static final String DUPLICATE_ROWS_TITLE = "Duplicate rows";

    private static final String TIE_BREAKER_TITLE = "Row selection";

    private static final String ADDITIONAL_OPTIONS_TITLE = "Additional options";

    private static final String TIE_BREAKER_LABEL =
        "The row selection defines which row to choose for each set of duplicates";

    private static final String SELECT_TIE_BREAKER_LABEL = "Select row:";

    private static final String MISSING_SELECTION_EXCEPTION =
        "'Keep duplicate rows' requires that at least one of the two 'Add columns ...' options is checked.";

    private static final String COLUMN_NAME_CLASH_EXCEPTION =
        "The selected reference column is also used for duplicate detection.";

    private final DuplicateFilterSettings m_settings = new DuplicateFilterSettings();

    private final DialogComponentColumnFilter2 m_groupCols =
        new DialogComponentColumnFilter2(m_settings.getGroupColsModel(), DuplicateFilterNodeModel.DATA_IN_PORT);

    private final DialogComponentBoolean m_retainOrder =
        new DialogComponentBoolean(m_settings.getRetainOrderModel(), "Retain row order");

    private final DialogComponentBoolean m_inMemory =
        new DialogComponentBoolean(m_settings.getInMemoryModel(), "In-memory computation");

    private final DialogComponentBoolean m_addUniqLblCol = new DialogComponentBoolean(m_settings.getAddUniqueLblModel(),
        "Add column showing duplicates (\"" + DuplicateFilterNodeModel.UNIQUE_IDENTIFIER + "\", \""
            + DuplicateFilterNodeModel.CHOSEN_IDENTIFIER + "\", \"" + DuplicateFilterNodeModel.DUPLICATE_IDENTIFIER
            + "\") to all rows ");

    private final DialogComponentBoolean m_addRowLblCol = new DialogComponentBoolean(m_settings.getAddRowLblModel(),
        "Add column identifying the ROWID of the chosen row for each duplicate row");

    @SuppressWarnings("unchecked")
    private final DialogComponentColumnNameSelection m_referenceCol = new DialogComponentColumnNameSelection(
        m_settings.getReferenceColModel(), null, DuplicateFilterNodeModel.DATA_IN_PORT, false, false, DataValue.class);

    final JRadioButton m_remDupBtn = new JRadioButton("Remove duplicate rows");

    final JRadioButton m_keepDupBtn = new JRadioButton("Keep duplicate rows");

    final JComboBox<RowSelectionType> m_rowSelectionType = new JComboBox<>(RowSelectionType.values());

    private DataTableSpec m_curSpec;

    /**
     * Constructor.
     */
    DuplicateFilterNodeDialog() {
        addTab("Options", createMainPanel());
        addTab("Advanced", createAdvancedPanel());
    }

    private JPanel createMainPanel() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // add panel border
        p.setBorder(BorderFactory.createTitledBorder(OPTIONS_TITLE));
        p.add(m_groupCols.getComponentPanel(), gbc);
        return p;
    }

    private JPanel createAdvancedPanel() {

        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        p.add(getDuplicateRowsPanel(), gbc);

        ++gbc.gridy;
        p.add(getRowSelectionPanel(), gbc);

        ++gbc.gridy;
        p.add(getAdditionalOptionsPanel(), gbc);

        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(Box.createVerticalBox(), gbc);

        return p;
    }

    private JPanel getDuplicateRowsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        p.setBorder(BorderFactory.createTitledBorder(DUPLICATE_ROWS_TITLE));

        p.add(m_remDupBtn, gbc);

        ++gbc.gridy;
        p.add(m_keepDupBtn, gbc);

        final ButtonGroup gb = new ButtonGroup();
        gb.add(m_remDupBtn);
        gb.add(m_keepDupBtn);

        m_remDupBtn.addItemListener(e -> {
            final boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
            m_settings.getRemoveDuplicatesModel().setBooleanValue(enabled);
            m_addRowLblCol.getModel().setEnabled(!enabled);
            m_addUniqLblCol.getModel().setEnabled(!enabled);
        });
        m_remDupBtn.setSelected(!m_settings.removeDuplicates());

        ++gbc.gridy;
        gbc.insets = new Insets(0, 10, 0, 0);
        p.add(m_addUniqLblCol.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_addRowLblCol.getComponentPanel(), gbc);

        return p;
    }

    private JPanel getRowSelectionPanel() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        p.setBorder(BorderFactory.createTitledBorder(TIE_BREAKER_TITLE));
        ++gbc.gridy;
        gbc.insets = new Insets(0, 0, 0, 0);
        final JLabel lbl = new JLabel(TIE_BREAKER_LABEL);
        lbl.setFont(new Font(lbl.getFont().getName(), Font.ITALIC, lbl.getFont().getSize()));
        p.add(lbl, gbc);

        ++gbc.gridy;
        p.add(createReferencePanel(), gbc);

        return p;

    }

    private JPanel getAdditionalOptionsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        // add panel border
        p.setBorder(BorderFactory.createTitledBorder(ADDITIONAL_OPTIONS_TITLE));
        p.add(m_inMemory.getComponentPanel(), gbc);

        ++gbc.gridy;
        p.add(m_retainOrder.getComponentPanel(), gbc);

        return p;
    }

    /**
     * @return
     */
    private JPanel createReferencePanel() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;

        p.add(new JLabel(SELECT_TIE_BREAKER_LABEL), gbc);

        ++gbc.gridx;
        gbc.insets = new Insets(0, 10, 0, 0);
        p.add(m_rowSelectionType, gbc);
        m_rowSelectionType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final RowSelectionType curSelectionType =
                    m_rowSelectionType.getItemAt(m_rowSelectionType.getSelectedIndex());
                m_settings.setRowSelectionType(curSelectionType);
                m_referenceCol.getModel().setEnabled(curSelectionType.supportsRefCol());
            }
        });

        ++gbc.gridx;
        gbc.insets = new Insets(0, 0, 0, 0);
        p.add(m_referenceCol.getComponentPanel(), gbc);

        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        if (!m_settings.removeDuplicates() && !m_settings.addUniqueLabel() && !m_settings.addRowLabel()) {
            throw new InvalidSettingsException(MISSING_SELECTION_EXCEPTION);
        }
        m_groupCols.saveSettingsTo(settings);
        m_retainOrder.saveSettingsTo(settings);
        m_addUniqLblCol.saveSettingsTo(settings);
        m_addRowLblCol.saveSettingsTo(settings);
        m_referenceCol.saveSettingsTo(settings);
        m_inMemory.saveSettingsTo(settings);
        m_settings.saveSettingsForDialog(settings);

        if (m_settings.getRowSelectionType().supportsRefCol()) {
            final String refColName = m_settings.getReferenceCol();
            CheckUtils.checkSetting(refColName != null && !refColName.isEmpty(), "The reference column cannot be null");
            if (Arrays.stream(m_settings.getGroupCols(m_curSpec).getIncludes()).anyMatch(s -> s.equals(refColName))) {
                throw new InvalidSettingsException(COLUMN_NAME_CLASH_EXCEPTION);
            }
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_groupCols.loadSettingsFrom(settings, specs);
        m_retainOrder.loadSettingsFrom(settings, specs);
        m_addUniqLblCol.loadSettingsFrom(settings, specs);
        m_addRowLblCol.loadSettingsFrom(settings, specs);
        m_referenceCol.loadSettingsFrom(settings, specs);
        m_inMemory.loadSettingsFrom(settings, specs);
        try {
            m_settings.loadSettingsForDialog(settings);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException(e.getMessage(), e);
        }
        if (m_settings.removeDuplicates()) {
            m_remDupBtn.doClick();
        } else {
            m_keepDupBtn.doClick();
        }
        m_rowSelectionType.setSelectedItem(m_settings.getRowSelectionType());
        m_referenceCol.getModel().setEnabled(m_settings.getRowSelectionType().supportsRefCol());
        m_addRowLblCol.getModel().setEnabled(!m_remDupBtn.isSelected());
        m_addUniqLblCol.getModel().setEnabled(!m_remDupBtn.isSelected());
        m_curSpec = specs[DuplicateFilterNodeModel.DATA_IN_PORT];
    }
}
