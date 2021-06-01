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
 *   Mar 2, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.truncator;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A {@link JPanel} allowing the user to choose from the different {@link TruncatePathOption}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class TruncationPanel {

    private final EnumMap<TruncatePathOption, JRadioButton> m_truncationMap;

    private final DialogComponentString m_folderTruncationPrefix;

    private final TruncationSettings m_truncationSettings;

    private final String m_borderTitle;

    private JPanel m_panel;

    /**
     * Creates the panel with a title border if the provided string is not {@code null}.
     *
     * @param borderTitle the title border, null if no border should be created
     * @param truncationSettings the {@link TruncationSettings}
     */
    public TruncationPanel(final String borderTitle, final TruncationSettings truncationSettings) {
        m_truncationSettings = truncationSettings;
        m_folderTruncationPrefix =
            new DialogComponentString(truncationSettings.getFolderTruncateModel(), null, true, 15);
        m_truncationMap = new EnumMap<>(TruncatePathOption.class);
        m_borderTitle = borderTitle;
        for (final TruncatePathOption opt : TruncatePathOption.values()) {
            final JRadioButton btn = new JRadioButton(opt.getLabel());
            m_truncationMap.put(opt, btn);
        }
    }

    /**
     * Returns the panel.
     *
     * @return the panel
     */
    public JPanel getPanel() {
        if (m_panel == null) {
            m_panel = new JPanel(new GridBagLayout());
            if (m_borderTitle != null) {
                m_panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), m_borderTitle));
            }
            final GBCBuilder gbc = fillPanel(m_panel, new ButtonGroup(),
                new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone());
            m_panel.add(new JPanel(), gbc.incY().setWeightX(1).setWidth(2).fillHorizontal().build());

        }
        return m_panel;
    }

    /**
     * Adds the components allowing to select the {@link TruncatePathOption}s.
     *
     * @param panel the panel to be filled
     * @param buttonGroup the button group
     * @param gbc the {@link GBCBuilder}
     * @return the final state of the {@link GBCBuilder}
     */
    protected GBCBuilder fillPanel(final JPanel panel, final ButtonGroup buttonGroup, GBCBuilder gbc) {
        for (JRadioButton btn : m_truncationMap.values()) {
            btn.addActionListener(l -> togglePrefixField());
            buttonGroup.add(btn);
        }
        panel.add(m_truncationMap.get(TruncatePathOption.RELATIVE), gbc.build());
        gbc = gbc.incY();
        panel.add(m_truncationMap.get(TruncatePathOption.KEEP), gbc.build());
        gbc = gbc.incY();
        panel.add(getPrefixPanel(), gbc.build());
        return gbc;
    }

    /**
     * Toggles the prefix field.
     */
    protected void togglePrefixField() {
        enablePrefix(m_truncationMap.get(TruncatePathOption.REMOVE_FOLDER_PREFIX).isSelected());
    }

    /**
     * Returns the truncation settings.
     *
     * @return the {@link TruncationSettings}
     */
    protected final TruncationSettings getSettings() {
        return m_truncationSettings;
    }

    /**
     * Sets the enabled status of the folder truncation prefix model.
     *
     * @param enable {@code true} if the folder truncation prefix model should be enabled, false otherwise
     */
    protected final void enablePrefix(final boolean enable) {
        m_folderTruncationPrefix.getModel().setEnabled(enable);
    }

    private Component getPrefixPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_truncationMap.get(TruncatePathOption.REMOVE_FOLDER_PREFIX), gbc.build());
        panel.add(m_folderTruncationPrefix.getComponentPanel(), gbc.incX().build());
        return panel;
    }

    /**
     * Sets the enable status of this panel.
     *
     * @param enabled {@code true} if the panel should be enabled, false otherwise
     */
    public void setEnabled(final boolean enabled) {
        m_truncationMap.values()//
            .forEach(b -> b.setEnabled(enabled));
        m_folderTruncationPrefix.getModel().setEnabled(enabled);
    }

    /**
     * Saves the truncation settings to the given settings.
     *
     * @param settings the settings to write to
     * @throws InvalidSettingsException - If the user has entered wrong values
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_truncationSettings.setTruncatePathOption(getTruncateOption());
        m_truncationSettings.saveSettingsForDialog(settings);
        m_folderTruncationPrefix.saveSettingsTo(settings);
    }

    /**
     * Returns the selected {@link TruncatePathOption}.
     *
     * @return the selected {@link TruncationException}
     */
    private TruncatePathOption getTruncateOption() {
        return m_truncationMap.entrySet().stream()//
            .filter(e -> e.getValue().isSelected())//
            .map(Entry::getKey)//
            .findFirst()//
            .orElse(TruncatePathOption.getDefault());
    }

    /**
     * Loads the settings into the panel.
     *
     * @param settings the <code>NodeSettings</code> to read from
     * @param specs the input specs
     * @throws NotConfigurableException If there is no chance for the dialog component to be valid (i.e. the settings
     *             are valid), e.g. if the given specs lack some important columns or column types.
     */
    public void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_truncationSettings.loadSettingsForDialog(settings);
        m_folderTruncationPrefix.loadSettingsFrom(settings, specs);
        selectOption();
        togglePrefixField();
    }

    /**
     * Sets the truncation path option according to the stored value.
     */
    protected void selectOption() {
        final TruncatePathOption truncatePathOption = m_truncationSettings.getTruncatePathOption();
        m_truncationMap.entrySet().stream()//
            .forEachOrdered(e -> e.getValue().setSelected(e.getKey() == truncatePathOption));
    }

}
