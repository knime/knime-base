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
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.filehandling.core.filefilter.FileFilter.FilterType;

/**
 * A file filter panel.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FileFilterPanel extends JPanel {

    /** Serial version UID */
    private static final long serialVersionUID = -8386119886528887209L;

    /** Combo box to select the filter type */
    private final JComboBox<FilterType> m_filterType;

    /** Combo box to define the suffixes, wildcard or regular expression */
    private final JComboBox<String> m_filterTextField;

    /** Check box to enable/disable case sensitive filtering */
    private final JCheckBox m_caseSensitive;

    /** Default file extension */
    private final String m_defaultExtensions;

    /** Default wildcard */
    private final String m_defaultWildcard;

    /** Label for the case sensitive check box*/
    private static final String CASE_SENSITIVE_LABEL = "Case sensitive";

    /** Label for the filter type combo box */
    private static final String FILTER_TYPE_LABEL = "Filter:";

    /**
     * Creates a new File Filter Panel
     *
     * @param defaultSuffixes default file suffixes
     */
    public FileFilterPanel(final String[] defaultSuffixes) {
        m_defaultExtensions = String.join(",", defaultSuffixes);
        m_defaultWildcard = "*." + (defaultSuffixes.length > 0 ? defaultSuffixes[0] : "*");

        m_filterType = new JComboBox<>(FilterType.values());
        m_filterType.setSelectedIndex(0);

        m_filterTextField = new JComboBox<>();
        m_filterTextField.setEditable(true);

        m_caseSensitive = new JCheckBox();
        m_caseSensitive.setText(CASE_SENSITIVE_LABEL);

        m_filterType.addActionListener(this::filterTypeSelectionChanged);
        filterTypeSelectionChanged(null);

        initLayout();
    }

    /** Method to initialize the layout of this panel */
    private void initLayout() {
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(25, 5, 0, 0);
        add(new JLabel(FILTER_TYPE_LABEL), gbc);

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

    /** FilterType Action Listener */
    private void filterTypeSelectionChanged(final ActionEvent e) {
        switch ((FilterType)m_filterType.getSelectedItem()) {
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
     * Sets the filter type to the filter type combo box.
     *
     * @param filterType the filter type to set
     */
    public void setFilterType(final FilterType filterType) {
        // TODO Check if type exists
        m_filterType.setSelectedItem(filterType);
    }

    /**
     * Returns the selected filter type.
     *
     * @return the filter type
     */
    public FilterType getSelectedFilterType() {
        return (FilterType)m_filterType.getSelectedItem();
    }

    /**
     * Sets the filter expression to the filter type combo box.
     *
     * @param filterExpression the filter expression to set
     */
    public void setFilterExpression(final String filterExpression) {
        m_filterTextField.setSelectedItem(filterExpression);
    }

    /**
     * Returns the selected filter expression.
     *
     * @return the filter expression
     */
    public String getSelectedFilterExpression() {
        return (String)m_filterTextField.getSelectedItem();
    }

    /**
     * Sets case sensitivity of file filter.
     *
     * @param caseSensitive case sensitivity
     */
    public void setCaseSensitive(final boolean caseSensitive) {
        m_caseSensitive.setSelected(caseSensitive);
    }

    /**
     * Returns if file filter is case sensitive or not.
     *
     * @return true, if file filter is case sensitive or not
     */
    public boolean getCaseSensitive() {
        return m_caseSensitive.isSelected();
    }

    /**
     * Method to enable/disable the components.
     *
     * @param enabled true, if components should be disabled
     */
    public void enableComponents(final boolean enabled) {
        m_filterType.setEnabled(enabled);
        m_filterTextField.setEnabled(enabled);
        m_caseSensitive.setEnabled(enabled);
    }

    /**
     * Adds an ActionListener to the components of this panel.
     *
     * @param listener the listener
     */
    public void addActionListener(final ActionListener listener) {
        m_filterType.addActionListener(listener);
        m_filterTextField.addActionListener(listener);
        m_caseSensitive.addActionListener(listener);
    }
}
