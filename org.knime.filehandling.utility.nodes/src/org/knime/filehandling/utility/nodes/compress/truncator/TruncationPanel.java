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
package org.knime.filehandling.utility.nodes.compress.truncator;

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
public final class TruncationPanel extends JPanel {

    private static final long serialVersionUID = 2344967748302207348L;

    private final transient EnumMap<TruncatePathOption, JRadioButton> m_truncateMap;

    private final transient DialogComponentString m_truncateRegex;

    private final transient TruncationSettings m_truncationSettings;

    /**
     * Constructor.
     *
     * @param titleBorder the title border
     * @param truncationSettings the {@link TruncationSettings}
     */
    public TruncationPanel(final String titleBorder, final TruncationSettings truncationSettings) {
        super(new GridBagLayout());
        m_truncationSettings = truncationSettings;
        m_truncateRegex = new DialogComponentString(truncationSettings.getTruncateRegexModel(), null, true, 15);
        m_truncateMap = new EnumMap<>(TruncatePathOption.class);
        final ButtonGroup grp = new ButtonGroup();
        for (final TruncatePathOption opt : TruncatePathOption.values()) {
            final JRadioButton btn = new JRadioButton(opt.getLabel());
            btn.addActionListener(l -> toggleRegexField());
            m_truncateMap.put(opt, btn);
            grp.add(btn);
        }

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), titleBorder));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        add(m_truncateMap.get(TruncatePathOption.KEEP_FULL_PATH), gbc.build());
        add(m_truncateMap.get(TruncatePathOption.KEEP_SRC_FOLDER), gbc.incX().build());
        add(m_truncateMap.get(TruncatePathOption.TRUNCATE_SRC_FOLDER), gbc.resetX().incY().build());
        add(getRegexPanel(), gbc.incX().build());
        add(new JPanel(), gbc.setWeightX(1).setWidth(2).fillHorizontal().build());
    }

    private void toggleRegexField() {
        m_truncateRegex.getModel().setEnabled(m_truncateMap.get(TruncatePathOption.TRUNCATE_REGEX).isSelected());
    }

    private Component getRegexPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().setWeightX(0).fillNone();
        panel.add(m_truncateMap.get(TruncatePathOption.TRUNCATE_REGEX), gbc.build());
        panel.add(m_truncateRegex.getComponentPanel(), gbc.incX().build());
        return panel;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        m_truncateMap.values()//
            .forEach(b -> b.setEnabled(enabled));
        m_truncateRegex.getModel().setEnabled(enabled);
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
        m_truncateRegex.saveSettingsTo(settings);
    }

    /**
     * Returns the selected {@link TruncatePathOption}.
     *
     * @return the selected {@link TruncationException}
     * @throws InvalidSettingsException - If the user has entered wrong values
     */
    private TruncatePathOption getTruncateOption() throws InvalidSettingsException {
        return m_truncateMap.entrySet().stream()//
            .filter(e -> e.getValue().isSelected())//
            .map(Entry::getKey)//
            .findFirst()//
            .orElseThrow(() -> new InvalidSettingsException("Please select one of the truncate options"));
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
        m_truncateRegex.loadSettingsFrom(settings, specs);
        selectTruncateOption();
        toggleRegexField();
    }

    private void selectTruncateOption() {
        final TruncatePathOption truncatePathOption = m_truncationSettings.getTruncatePathOption();
        m_truncateMap.entrySet().stream()//
            .forEachOrdered(e -> e.getValue().setSelected(e.getKey() == truncatePathOption));
        toggleRegexField();
    }

}
