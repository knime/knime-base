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
 *   Aug 15, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.filefilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A file filter panel.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FileFilterPanel extends JPanel {

    private static final long serialVersionUID = -8386119886528887209L;

    private static final String FILE_FILTER_SETTINGS_KEY = "file-filter-settings";
    private static final String FILTER_SETTINGS_KEY = "filter";
    private static final String EXTENSIONS_SETTINGS_KEY = "extensions";
    private static final String CASE_SENSITIVE_SETTINGS_KEY = "case-sensitive";

    private final String[] m_defaultFileSuffixes;

    private final JComboBox<String> m_filterType;

    private final JComboBox<String> m_filterTextField;

    private final JCheckBox m_caseSensitive;

    private final String m_defaultExtensions;

    private final String m_defaultWildcard;

    /**
     * Creates a new File Filter Panel
     */
    public FileFilterPanel(final String[] defaultSuffixes) {
        m_defaultFileSuffixes = defaultSuffixes;

        m_defaultExtensions =  String.join(",", defaultSuffixes);
        m_defaultWildcard = "*." + (defaultSuffixes.length > 0 ? defaultSuffixes[0] : "*");

        m_filterType =
            new JComboBox<>(Arrays.stream(FileFilter.values()).map(FileFilter::getDisplayText).toArray(String[]::new));
        m_filterType.setSelectedIndex(0);

        m_filterTextField = new JComboBox<>();
        m_filterTextField.setEditable(true);

        m_caseSensitive = new JCheckBox();
        m_caseSensitive.setText("Case sensitive");

        m_filterType.addActionListener(this::filterTypeSelectionChanged);
        filterTypeSelectionChanged(null);

        initLayout();
    }

    private void initLayout() {
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(25, 5, 0, 0);
        add(new JLabel("Filter:"), gbc);

        gbc.gridx++;
        add(m_filterType, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_filterTextField, gbc);

        gbc.gridx++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        add(m_caseSensitive, gbc);
    }

    private void filterTypeSelectionChanged(final ActionEvent e) {
        switch (FileFilter.fromDisplayText((String)m_filterType.getSelectedItem())) {
            case EXTENSIONS:
                m_filterTextField.setSelectedItem(m_defaultExtensions);
                break;
            case WILDCARD:
                m_filterTextField.setSelectedItem(m_defaultWildcard);
                break;
            case REGEX:
                m_filterTextField.setSelectedItem("");
                break;
        }
    }

    /**
     * Load from settings.
     *
     * @param settings settings to load
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO
    }

    /**
     * Save to settings.
     *
     * @param settings settings to save to
     * @throws InvalidSettingsException
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        // TODO
    }

}
