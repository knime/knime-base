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
 *   Apr 14, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filtermode;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.filtermode.FileAndFolderFilter.FilterType;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * Panel that contains options used by the {@link FileAndFolderFilter}.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
final class FilterOptionsPanel extends JPanel {

    /** Serial version UID */
    private static final long serialVersionUID = 1L;

    /** ButtonGroup to select the file name filter type */
    private final DialogComponentButtonGroup m_fileNameFilterType;

    /** ButtonGroup to select the folder name filter type */
    private final DialogComponentButtonGroup m_folderNameFilterType;

    /** Model for file name filter type */
    private final SettingsModelString m_fileNameFilterTypeModel;

    /** Model for folder name filter type */
    private final SettingsModelString m_folderNameFilterTypeModel;

    /** Text field to define the file suffixes */
    private final JTextField m_filterFileExtensionTextField;

    /** Text field to define the file name wildcard or regular expression */
    private final JTextField m_filterFileNameTextField;

    /** Text field to define the folder name wildcard or regular expression */
    private final JTextField m_filterFolderNameTextField;

    /** Check box to enable/disable case sensitive file extension filtering */
    private final JCheckBox m_caseSensitiveFileExtension;

    /** Check box to enable/disable case sensitive file name filtering */
    private final JCheckBox m_caseSensitiveFileName;

    /** Check box to enable/disable case sensitive folder name filtering */
    private final JCheckBox m_caseSensitiveFolderName;

    /** Check box to enable/disable hidden files inclusion */
    private final JCheckBox m_includeHiddenFiles;

    /** Check box to enable/disable hidden folders inclusion */
    private final JCheckBox m_includeHiddenFolders;

    /** Check box to enable/disable filtering by file extension */
    private final JCheckBox m_filterByFileExtension;

    /** Check box to enable/disable filtering by file name */
    private final JCheckBox m_filterByFileName;

    /** Check box to enable/disable filtering by folder name */
    private final JCheckBox m_filterByFolderName;

    /** Label for file filter panel */
    private static final String FILE_FILTER_PANEL_LABEL = "File filter options";

    /** Label for folder filter panel */
    private static final String FOLDER_FILTER_PANEL_LABEL = "Folder filter options";

    /** Label for the case sensitive check box */
    private static final String CASE_SENSITIVE_LABEL = "Case sensitive";

    /** Label for the file extension filter */
    private static final String FILTER_FILE_EXTENSIONS_LABEL = "File extension(s)";

    /** Tooltip for the file extension filter */
    private static final String FILTER_FILE_EXTENSIONS_TOOLTIP = "Enter file extensions separated by , or ; or |";

    /** Label for the file name filter */
    private static final String FILTER_FILE_NAME_LABEL = "File name";

    /** Label for the folder name filter */
    private static final String FILTER_FOLDER_NAME_LABEL = "Folder name";

    /** String used as label for the include  hidden files check box */
    private static final String INCLUDE_HIDDEN_FILES_LABEL = "Include hidden files";

    /** String used as label for the include hidden folders check box */
    private static final String INCLUDE_HIDDEN_FOLDERS_LABEL = "Include hidden folders";

    /** Key for filter type model */
    private static final String FILE_NAME_FILTER_TYPE_KEY = "file_name_filter_type";

    /** Key for filter type model */
    private static final String FOLDER_NAME_FILTER_TYPE_KEY = "folder_name_filter_type";

    /** Panel containing all the file concerning components */
    private final JPanel m_filePanel = new JPanel(new GridBagLayout());

    /** Panel containing all the folder concerning components */
    private final JPanel m_folderPanel = new JPanel(new GridBagLayout());

    /**
     * Creates a new File Filter Panel
     */
    FilterOptionsPanel() {
        super(new GridBagLayout());
        m_fileNameFilterTypeModel = new SettingsModelString(FILE_NAME_FILTER_TYPE_KEY, FilterType.WILDCARD.name());
        m_fileNameFilterType =
            new DialogComponentButtonGroup(m_fileNameFilterTypeModel, null, false, FilterType.values());
        m_fileNameFilterTypeModel.addChangeListener(e -> handleFileNameFilterTypeUpdate());

        m_folderNameFilterTypeModel = new SettingsModelString(FOLDER_NAME_FILTER_TYPE_KEY, FilterType.WILDCARD.name());
        m_folderNameFilterType =
            new DialogComponentButtonGroup(m_folderNameFilterTypeModel, null, false, FilterType.values());
        m_folderNameFilterTypeModel.addChangeListener(e -> handleFolderNameFilterTypeUpdate());

        m_filterFileNameTextField = new JTextField();
        m_filterFolderNameTextField = new JTextField();

        m_caseSensitiveFileName = new JCheckBox(CASE_SENSITIVE_LABEL);
        m_caseSensitiveFolderName = new JCheckBox(CASE_SENSITIVE_LABEL);

        m_filterByFileName = new JCheckBox(FILTER_FILE_NAME_LABEL);
        m_filterByFileName.addChangeListener(e -> handleFilterFileNameCheckBoxUpdate());
        m_filterByFolderName = new JCheckBox(FILTER_FOLDER_NAME_LABEL);
        m_filterByFolderName.addChangeListener(e -> handleFilterFolderNameCheckBoxUpdate());

        m_filterFileExtensionTextField = new JTextField();
        m_filterFileExtensionTextField.setToolTipText(FILTER_FILE_EXTENSIONS_TOOLTIP);

        m_caseSensitiveFileExtension = new JCheckBox(CASE_SENSITIVE_LABEL);

        m_filterByFileExtension = new JCheckBox(FILTER_FILE_EXTENSIONS_LABEL);
        m_filterByFileExtension.addChangeListener(e -> handleFilterFileExtensionCheckBoxUpdate());

        m_includeHiddenFiles = new JCheckBox(INCLUDE_HIDDEN_FILES_LABEL);
        m_includeHiddenFiles.setSelected(true);
        m_includeHiddenFolders = new JCheckBox(INCLUDE_HIDDEN_FOLDERS_LABEL);
        m_includeHiddenFolders.setSelected(true);

        handleFilterFileExtensionCheckBoxUpdate();
        handleFilterFileNameCheckBoxUpdate();
        handleFilterFolderNameCheckBoxUpdate();
        handleFileNameFilterTypeUpdate();
        handleFolderNameFilterTypeUpdate();

        initLayout();
    }

    private void handleFilterFileNameCheckBoxUpdate() {
        final boolean filterName = m_filterByFileName.isSelected();
        m_filterFileNameTextField.setEnabled(filterName);
        m_caseSensitiveFileName.setEnabled(filterName);
        m_fileNameFilterTypeModel.setEnabled(filterName);
    }

    private void handleFilterFolderNameCheckBoxUpdate() {
        final boolean filterName = m_filterByFolderName.isSelected();
        m_filterFolderNameTextField.setEnabled(filterName);
        m_caseSensitiveFolderName.setEnabled(filterName);
        m_folderNameFilterTypeModel.setEnabled(filterName);
    }

    private void handleFilterFileExtensionCheckBoxUpdate() {
        final boolean filterExtension = m_filterByFileExtension.isSelected();
        m_filterFileExtensionTextField.setEnabled(filterExtension);
        m_caseSensitiveFileExtension.setEnabled(filterExtension);
    }

    private void handleFileNameFilterTypeUpdate() {
        final FilterType filterType = FilterType.valueOf(m_fileNameFilterTypeModel.getStringValue());
        m_filterFileNameTextField.setToolTipText(filterType.getInputTooltip());
    }

    private void handleFolderNameFilterTypeUpdate() {
        final FilterType filterType = FilterType.valueOf(m_folderNameFilterTypeModel.getStringValue());
        m_filterFolderNameTextField.setToolTipText(filterType.getInputTooltip());
    }

    /** Method to initialize the layout of this panel */
    private void initLayout() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        // very small number so that the file panel will not (noticeably) be moved if both file and folder panels are
        // visible; still > 0 in case folder panel is not visible
        gbc.weighty = 0.0001;
        add(createFileCompsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 0.9999;
        gbc.insets = new Insets(5, 0, 0, 0);
        add(createFolderCompsPanel(), gbc);
    }

    private JPanel createFileCompsPanel() {
        m_filePanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), FILE_FILTER_PANEL_LABEL));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        // File extension filter settings
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        m_filePanel.add(m_filterByFileExtension, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 5, 0, 5);
        m_filePanel.add(m_filterFileExtensionTextField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 25, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        m_filePanel.add(m_caseSensitiveFileExtension, gbc);

        // File name filter settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 5, 0, 5);
        m_filePanel.add(m_filterByFileName, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        m_filePanel.add(m_filterFileNameTextField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        m_filePanel.add(m_caseSensitiveFileName, gbc);
        gbc.gridx++;
        m_filePanel.add(m_fileNameFilterType.getComponentPanel(), gbc);

        // Hidden files settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_filePanel.add(m_includeHiddenFiles, gbc);
        gbc.gridy++;
        gbc.weighty = 1;

        // Dummy label to keep other components at the top
        m_filePanel.add(new JLabel(), gbc);
        return m_filePanel;
    }

    private JPanel createFolderCompsPanel() {
        m_folderPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), FOLDER_FILTER_PANEL_LABEL));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        // Folder name filter settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 5, 0, 5);
        m_folderPanel.add(m_filterByFolderName, gbc);
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        m_folderPanel.add(m_filterFolderNameTextField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 25, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        m_folderPanel.add(m_caseSensitiveFolderName, gbc);
        gbc.gridx++;
        m_folderPanel.add(m_folderNameFilterType.getComponentPanel(), gbc);

        // Hidden folders settings
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        m_folderPanel.add(m_includeHiddenFolders, gbc);
        gbc.gridy++;
        gbc.weighty = 1;

        // Dummy label to keep other components at the top
        m_folderPanel.add(new JLabel(), gbc);
        return m_folderPanel;
    }

    void visibleComponents(final FilterMode filterOption) {
        final boolean visibleFileComps = filterOption == FilterMode.FILE || filterOption == FilterMode.FILES_IN_FOLDERS
            || filterOption == FilterMode.FILES_AND_FOLDERS;
        final boolean visibleFolderComps = filterOption == FilterMode.FOLDER || filterOption == FilterMode.FOLDERS
            || filterOption == FilterMode.FILES_AND_FOLDERS || filterOption == FilterMode.FILES_IN_FOLDERS;
        m_filePanel.setVisible(visibleFileComps);
        m_folderPanel.setVisible(visibleFolderComps);
    }

    /**
     * Returns the current state of the panel as {@link FilterOptionsSettings}.
     *
     * @return the current state of the panel as {@link FilterOptionsSettings}
     */
    FilterOptionsSettings getFilterConfigSettings() {
        final FilterOptionsSettings fileFilterSettings = new FilterOptionsSettings();

        fileFilterSettings.setFilterFilesByExtension(m_filterByFileExtension.isSelected());
        fileFilterSettings.setFilesExtensionExpression(m_filterFileExtensionTextField.getText());
        fileFilterSettings.setFilesExtensionCaseSensitive(m_caseSensitiveFileExtension.isSelected());
        fileFilterSettings.setFilterFilesByName(m_filterByFileName.isSelected());
        fileFilterSettings.setFilesNameExpression(m_filterFileNameTextField.getText());
        fileFilterSettings.setFilesNameFilterType(FilterType.valueOf(m_fileNameFilterTypeModel.getStringValue()));
        fileFilterSettings.setFilesNameCaseSensitive(m_caseSensitiveFileName.isSelected());
        fileFilterSettings.setIncludeHiddenFiles(m_includeHiddenFiles.isSelected());

        fileFilterSettings.setFilterFoldersByName(m_filterByFolderName.isSelected());
        fileFilterSettings.setFoldersNameExpression(m_filterFolderNameTextField.getText());
        fileFilterSettings.setFoldersNameFilterMode(FilterType.valueOf(m_folderNameFilterTypeModel.getStringValue()));
        fileFilterSettings.setFoldersNameCaseSensitive(m_caseSensitiveFolderName.isSelected());
        fileFilterSettings.setIncludeHiddenFolders(m_includeHiddenFolders.isSelected());

        return fileFilterSettings;
    }

    /**
     * Sets the state of the panel based on the given {@link FilterOptionsSettings}.
     *
     * @param fileFilterSettings the {@link FilterOptionsSettings} to apply
     */
    void setFilterConfigSettings(final FilterOptionsSettings fileFilterSettings) {
        m_filterByFileExtension.setSelected(fileFilterSettings.isFilterFilesByExtension());
        m_filterFileExtensionTextField.setText(fileFilterSettings.getFilesExtensionExpression());
        m_caseSensitiveFileExtension.setSelected(fileFilterSettings.isFilesExtensionCaseSensitive());
        m_filterByFileName.setSelected(fileFilterSettings.isFilterFilesByName());
        m_filterFileNameTextField.setText(fileFilterSettings.getFilesNameExpression());
        m_fileNameFilterTypeModel.setStringValue(fileFilterSettings.getFilesNameFilterType().toString());
        m_caseSensitiveFileName.setSelected(fileFilterSettings.isFilesNameCaseSensitive());
        m_includeHiddenFiles.setSelected(fileFilterSettings.isIncludeHiddenFiles());

        m_filterByFolderName.setSelected(fileFilterSettings.isFilterFoldersByName());
        m_filterFolderNameTextField.setText(fileFilterSettings.getFoldersNameExpression());
        m_folderNameFilterTypeModel.setStringValue(fileFilterSettings.getFoldersNameFilterType().toString());
        m_caseSensitiveFolderName.setSelected(fileFilterSettings.isFoldersNameCaseSensitive());
        m_includeHiddenFolders.setSelected(fileFilterSettings.isIncludeHiddenFolders());
    }
}
