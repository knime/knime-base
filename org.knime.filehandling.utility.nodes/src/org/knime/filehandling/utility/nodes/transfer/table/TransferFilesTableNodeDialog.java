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
 *   Mar 3, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.table;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.utility.nodes.transfer.AbstractTransferFilesNodeDialog;

/**
 * Node dialog of the Transfer Files/Folder (Table input) node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class TransferFilesTableNodeDialog extends AbstractTransferFilesNodeDialog<TransferFilesTableNodeConfig> {

    private static final int LEFT_INSET = 10;

    private final DialogComponentColumnNameSelection m_srcColSelection;

    private final DialogComponentColumnNameSelection m_destColSelection;

    private final JRadioButton m_tableSelection;

    private final JRadioButton m_fileSelection;

    private final DialogComponentBoolean m_failIfSrcDoesNotExist;

    /**
     * Constructor.
     *
     * @param config the {@link TransferFilesTableNodeConfig}
     * @param inputTableIdx the table input port index
     */
    @SuppressWarnings("unchecked")
    TransferFilesTableNodeDialog(final TransferFilesTableNodeConfig config, final int inputTableIdx) {
        super(config);
        m_srcColSelection = new DialogComponentColumnNameSelection(config.getSrcPathColModel(), "Source column",
            inputTableIdx, FSLocationValue.class);
        m_destColSelection = new DialogComponentColumnNameSelection(config.getDestPathColModel(), "Destination column",
            inputTableIdx, FSLocationValue.class);
        m_failIfSrcDoesNotExist =
            new DialogComponentBoolean(config.getFailIfSourceDoesNotExistsModel(), "Fail if source does not exist");
        m_tableSelection = new JRadioButton("From table");
        m_fileSelection = new JRadioButton("From file chooser");
        createPanel();
    }

    @Override
    protected Component getSourceLocationPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc =
            new GBCBuilder().anchorFirstLineStart().resetPos().weight(0, 0).fillNone().insetLeft(LEFT_INSET);

        p.add(m_srcColSelection.getComponentPanel(), gbc.build());

        gbc.incX().setWeightX(1).fillHorizontal();
        p.add(Box.createHorizontalBox(), gbc.build());

        return p;
    }

    @Override
    protected JPanel getDestinationPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().anchorFirstLineStart().resetPos().weight(1, 0).fillHorizontal();

        final ButtonGroup btnGrp = new ButtonGroup();
        m_tableSelection.setSelected(true);
        m_tableSelection.addActionListener(l -> toggleSelection());
        btnGrp.add(m_tableSelection);
        p.add(m_tableSelection, gbc.build());

        gbc.incY().insetLeft(LEFT_INSET);
        p.add(createDestColPanel(), gbc.build());

        m_fileSelection.setSelected(false);
        m_fileSelection.addActionListener(l -> toggleSelection());
        btnGrp.add(m_fileSelection);
        gbc.insetLeft(0).incY();
        p.add(m_fileSelection, gbc.build());

        gbc.setWeightX(1).fillHorizontal().incY().insetLeft(LEFT_INSET + 3);
        p.add(super.getDestinationPanel(), gbc.build());

        return p;
    }

    @Override
    protected void addAdditionalOptions(final JPanel panel, final GridBagConstraints gbc) {
        panel.add(m_failIfSrcDoesNotExist.getComponentPanel(), gbc);
    }

    private JPanel createDestColPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().anchorFirstLineStart().resetPos().weight(0, 0).fillNone();
        p.add(m_destColSelection.getComponentPanel(), gbc.build());
        gbc.incX().setWeightX(1).fillHorizontal();
        p.add(Box.createHorizontalBox(), gbc.build());
        return p;
    }

    private void toggleSelection() {
        final boolean enabled = m_fileSelection.isSelected();
        enableDestFileChooserPanel(enabled);
        m_destColSelection.getModel().setEnabled(!enabled);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_srcColSelection.saveSettingsTo(settings);
        getConfig().destDefinedByTableColumn(m_tableSelection.isSelected());
        getConfig().saveDestDefinedByTableColInDialog(settings);
        m_destColSelection.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
        m_failIfSrcDoesNotExist.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_srcColSelection.loadSettingsFrom(settings, specs);
        getConfig().loadDestDefinedByTableColInDialog(settings);
        m_destColSelection.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);
        m_failIfSrcDoesNotExist.loadSettingsFrom(settings, specs);
        updateSelection();
    }

    private void updateSelection() {
        final boolean selected = getConfig().destDefinedByTableColumn();
        m_tableSelection.setSelected(selected);
        m_fileSelection.setSelected(!selected);
        toggleSelection();
    }

}
