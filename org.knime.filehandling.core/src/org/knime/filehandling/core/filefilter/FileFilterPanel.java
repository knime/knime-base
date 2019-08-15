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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A file filter panel.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FileFilterPanel extends JPanel {

    private static final int HORIZONTAL_SPACE = 10;

    private static final int PANEL_WIDTH = 585;

    private static final long serialVersionUID = -8386119886528887209L;

    private static final String FILE_FILTER_SETTINGS_KEY = "file-filter-settings";
    private static final String FILTER_SETTINGS_KEY = "filter";
    private static final String EXTENSIONS_SETTINGS_KEY = "extensions";
    private static final String CASE_SENSITIVE_SETTINGS_KEY = "case-sensitive";

    private JComboBox<String> m_extensionField;

    private JCheckBox m_caseSensitive;

    private JRadioButton m_noFilterRadio;

    private JRadioButton m_extensionsFilterRadio;

    private JRadioButton m_regexFilterRadio;

    private JRadioButton m_wildCardsFilterRadio;

    /**
     * Creates a new File Filter Panel
     */
    public FileFilterPanel() {
        Box outerBox = Box.createVerticalBox();
        outerBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Filter:"));

        m_extensionField = new JComboBox<>();
        m_extensionField.setEditable(true);

        Box extensionBox = Box.createHorizontalBox();
        extensionBox.add(Box.createHorizontalStrut(HORIZONTAL_SPACE));
        extensionBox.add(new JLabel("Extension(s) / Expression:"));
        extensionBox.add(Box.createHorizontalStrut(HORIZONTAL_SPACE));
        extensionBox.add(m_extensionField);
        extensionBox.add(Box.createHorizontalStrut(HORIZONTAL_SPACE));

        m_caseSensitive = new JCheckBox();
        m_caseSensitive.setText("case sensitive");

        m_noFilterRadio = new JRadioButton();
        m_noFilterRadio.setText("none");
        m_noFilterRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(false);
            }
        });

        m_extensionsFilterRadio = new JRadioButton();
        m_extensionsFilterRadio.setText("file extension(s)");
        m_extensionsFilterRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        m_regexFilterRadio = new JRadioButton();
        m_regexFilterRadio.setText("regular expression");
        m_regexFilterRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        m_wildCardsFilterRadio = new JRadioButton();
        m_wildCardsFilterRadio.setText("wildcard pattern");
        m_wildCardsFilterRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                m_extensionField.setEnabled(true);
            }
        });

        ButtonGroup group = new ButtonGroup();
        group.add(m_noFilterRadio);
        group.add(m_extensionsFilterRadio);
        group.add(m_regexFilterRadio);
        group.add(m_wildCardsFilterRadio);

        JPanel filterPanel = new JPanel(new GridLayout(2, 3));
        filterPanel.add(m_noFilterRadio);
        filterPanel.add(m_extensionsFilterRadio);
        filterPanel.add(m_caseSensitive);
        filterPanel.add(m_regexFilterRadio);
        filterPanel.add(m_wildCardsFilterRadio);

        Box filterBox = Box.createHorizontalBox();
        filterBox.add(Box.createHorizontalStrut(HORIZONTAL_SPACE));
        filterBox.add(filterPanel);
        filterBox.add(Box.createHorizontalStrut(PANEL_WIDTH / 4));

        outerBox.add(extensionBox);
        outerBox.add(filterBox);

        outerBox.setMaximumSize(new Dimension(PANEL_WIDTH, 120));
        outerBox.setMinimumSize(new Dimension(PANEL_WIDTH, 120));

        this.add(outerBox);
    }

    /**
     * Load from settings.
     *
     * @param settings settings to load
     * @throws InvalidSettingsException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

        if (!settings.containsKey(FILE_FILTER_SETTINGS_KEY)) {
            // not yet configured, use defaults
        } else {
            NodeSettingsRO fileFilterSettings = settings.getNodeSettings(FILE_FILTER_SETTINGS_KEY);

            String filterName = fileFilterSettings.getString(FILTER_SETTINGS_KEY);
            FileFilter filter = FileFilter.valueOf(filterName);
            switch (filter) {
                case NONE:
                    m_noFilterRadio.setSelected(true);
                    break;
                case EXTENSION:
                    m_extensionsFilterRadio.setSelected(true);
                    break;
                case REGEX:
                    m_regexFilterRadio.setSelected(true);
                    break;
                case WILDCARD:
                    m_wildCardsFilterRadio.setSelected(true);
                    break;
            }

            String extensions = fileFilterSettings.getString(EXTENSIONS_SETTINGS_KEY);
            m_extensionField.setSelectedItem(extensions);

            boolean isCaseSensitive = fileFilterSettings.getBoolean(CASE_SENSITIVE_SETTINGS_KEY);
            m_caseSensitive.setSelected(isCaseSensitive);
        }
    }

    /**
     * Save to settings.
     *
     * @param settings settings to save to
     * @throws InvalidSettingsException
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        NodeSettingsWO fileFilterSettings = settings.addNodeSettings(FILE_FILTER_SETTINGS_KEY);

        FileFilter fileFilter = FileFilter.NONE;
        if (m_noFilterRadio.isSelected()) {
            fileFilter = FileFilter.NONE;
        } else if (m_extensionsFilterRadio.isSelected()) {
            fileFilter = FileFilter.EXTENSION;
        } else if (m_regexFilterRadio.isSelected()) {
            fileFilter = FileFilter.REGEX;
        } else if (m_wildCardsFilterRadio.isSelected()) {
            fileFilter = FileFilter.WILDCARD;
        }

        fileFilterSettings.addString(FILTER_SETTINGS_KEY, fileFilter.name());

        String extensions = (String) m_extensionField.getSelectedItem();
        fileFilterSettings.addString(EXTENSIONS_SETTINGS_KEY, extensions);

        fileFilterSettings.addBoolean(CASE_SENSITIVE_SETTINGS_KEY, m_caseSensitive.isSelected());
    }

}
