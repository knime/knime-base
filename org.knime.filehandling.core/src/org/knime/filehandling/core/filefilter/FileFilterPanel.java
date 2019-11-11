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

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.filefilter.FileFilter.FilterType;

/**
 * A panel for the configuration of file filters.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FileFilterPanel extends JPanel {

    /** Serial version UID */
    private static final long serialVersionUID = -8386119886528887209L;

    /** ButtonGroup to select the filter type */
    private final DialogComponentButtonGroup m_filterType;

    /** Model for filter type */
    private final SettingsModelString m_filterTypeModel;

    /** Text field to define the suffixes */
    private final JTextField m_filterExtensionTextField;

    /** Text field to define the wildcard or regular expression */
    private final JTextField m_filterNameTextField;

    /** Check box to enable/disable case sensitive file extension filtering */
    private final JCheckBox m_caseSensitiveExtension;

    /** Check box to enable/disable case sensitive file name filtering */
    private final JCheckBox m_caseSensitiveName;

    /** Check box to enable/disable hidden files filtering */
    private final JCheckBox m_filterHiddenFiles;

    private final JCheckBox m_filterExtension;

    private final JCheckBox m_filterName;

    /** Label for the case sensitive check box */
    private static final String CASE_SENSITIVE_LABEL = "Case sensitive";

    /** Label for the file extension filter */
    private static final String FILTER_EXTENSIONS_LABEL = "File extension(s):";

    /** Tooltip for the file extension filter */
    private static final String FILTER_EXTENSIONS_TOOLTIP = "Enter file extension seperatet by ;";

    /** Label for the file name filter */
    private static final String FILTER_NAME_LABEL = "File name:";

    /** String used as label for the filter hidden files check box */
    private static final String FILTER_HIDDEN_FILES_LABLE = "Filter hidden files";

    /** Key for filter type model */
    private static final String FILTER_TYPE_KEY = "filterType";

    /**
     * Creates a new File Filter Panel
     */
    public FileFilterPanel() {

        m_filterTypeModel = new SettingsModelString(FILTER_TYPE_KEY, FilterType.WILDCARD.name());
        m_filterType = new DialogComponentButtonGroup(m_filterTypeModel, null, false, FilterType.values());
        m_filterTypeModel.addChangeListener(e -> handleFilterTypeUpdate());

        m_filterNameTextField = new JTextField();

        m_caseSensitiveName = new JCheckBox(CASE_SENSITIVE_LABEL);

        m_filterName = new JCheckBox(FILTER_NAME_LABEL);
        m_filterName.addChangeListener(e -> handleFilterNameCheckBoxUpdate());

        m_filterExtensionTextField = new JTextField();
        m_filterExtensionTextField.setToolTipText(FILTER_EXTENSIONS_TOOLTIP);

        m_caseSensitiveExtension = new JCheckBox(CASE_SENSITIVE_LABEL);

        m_filterExtension = new JCheckBox(FILTER_EXTENSIONS_LABEL);
        m_filterExtension.addChangeListener(e -> handleFilterExtensionCheckBoxUpdate());

        m_filterHiddenFiles = new JCheckBox(FILTER_HIDDEN_FILES_LABLE);
        m_filterHiddenFiles.setSelected(true);

        handleFilterExtensionCheckBoxUpdate();
        handleFilterNameCheckBoxUpdate();
        handleFilterTypeUpdate();

        initLayout();
    }

    private void handleFilterNameCheckBoxUpdate() {
        final boolean filterName = m_filterName.isSelected();
        m_filterNameTextField.setEnabled(filterName);
        m_caseSensitiveName.setEnabled(filterName);
        m_filterTypeModel.setEnabled(filterName);
    }

    private void handleFilterExtensionCheckBoxUpdate() {
        final boolean filterExtension = m_filterExtension.isSelected();
        m_filterExtensionTextField.setEnabled(filterExtension);
        m_caseSensitiveExtension.setEnabled(filterExtension);
    }

    private void handleFilterTypeUpdate() {
        final FilterType filterType = FilterType.valueOf(m_filterTypeModel.getStringValue());
        m_filterNameTextField.setToolTipText(filterType.getInputTooltip());
    }

    /** Method to initialize the layout of this panel */
    private void initLayout() {
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        // Extension Filter settings
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 5, 0, 0);
        add(m_filterExtension, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_filterExtensionTextField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 25, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        add(m_caseSensitiveExtension, gbc);

        // Name Filter settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 5, 0, 0);
        add(m_filterName, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_filterNameTextField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 0, 0);
        add(m_caseSensitiveName, gbc);
        gbc.gridx++;
        add(m_filterType.getComponentPanel(), gbc);

        //Hidden Files settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 5, 0, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_filterHiddenFiles, gbc);
    }

    /**
     * Method to enable/disable the components.
     *
     * @param enabled true, if components should be disabled
     */
    public void enableComponents(final boolean enabled) {
        m_filterExtension.setEnabled(enabled);
        m_filterExtensionTextField.setEnabled(enabled);
        m_caseSensitiveExtension.setEnabled(enabled);

        m_filterName.setEnabled(enabled);
        m_filterNameTextField.setEnabled(enabled);
        m_caseSensitiveName.setEnabled(enabled);
        m_filterTypeModel.setEnabled(enabled);

        m_filterHiddenFiles.setEnabled(enabled);
    }

    /**
     * Returns the current state of the panel as {@link FileFilterSettings}.
     *
     * @return the current state of the panel as {@link FileFilterSettings}
     */
    public FileFilterSettings getFileFilterSettings() {
        final FileFilterSettings fileFilterSettings = new FileFilterSettings();

        fileFilterSettings.setFilterFilesByExtension(m_filterExtension.isSelected());
        fileFilterSettings.setFilterExpressionExtension(m_filterExtensionTextField.getText());
        fileFilterSettings.setFilterCaseSensitiveExtension(m_caseSensitiveExtension.isSelected());

        fileFilterSettings.setFilterFilesByName(m_filterName.isSelected());
        fileFilterSettings.setFilterExpressionName(m_filterNameTextField.getText());
        fileFilterSettings.setFilterType(getSelectedFilterType());
        fileFilterSettings.setFilterCaseSensitiveName(m_caseSensitiveName.isSelected());

        fileFilterSettings.setFilterHiddenFiles(m_filterHiddenFiles.isSelected());
        return fileFilterSettings;
    }

    /**
     * Returns the selected filter type.
     *
     * @return the filter type
     */
    private FilterType getSelectedFilterType() {
        return FilterType.valueOf(m_filterTypeModel.getStringValue());
    }

    /**
     * Sets the state of the panel based on the given {@link FileFilterSettings}.
     *
     * @param fileFilterSettings the {@link FileFilterSettings} to apply
     */
    public void setFileFilterSettings(final FileFilterSettings fileFilterSettings) {
        m_filterExtension.setSelected(fileFilterSettings.filterFilesByExtension());
        m_filterExtensionTextField.setText(fileFilterSettings.getFilterExpressionExtension());
        m_caseSensitiveExtension.setSelected(fileFilterSettings.isFilterCaseSensitiveExtension());

        m_filterName.setSelected(fileFilterSettings.filterFilesByName());
        m_filterNameTextField.setText(fileFilterSettings.getFilterExpressionName());
        m_filterTypeModel.setStringValue(fileFilterSettings.getFilterType().toString());
        m_caseSensitiveName.setSelected(fileFilterSettings.isFilterCaseSensitiveName());

        m_filterHiddenFiles.setSelected(fileFilterSettings.filterHiddenFiles());
    }
}
