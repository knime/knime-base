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

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * This dialog component consists of two panels. The first contains a button group that allows to select the
 * {@link FilterMode} for files and folders and is always displayed. The second one is optional and contains a button to
 * open a dialog with further filter options and a checkbox to include subfolders. Depending on construction of this
 * class, this panel will be displayed underneath the first one or not. Its visibility and content also depends on the
 * selected {@link FilterMode}. Furthermore, by calling {@link #getFilterConfigPanel()} it can be retrieved and placed
 * detached from the first panel.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DialogComponentFilterMode extends DialogComponent {

    /** Panel that is opened in a new dialog and contains filtering options */
    private final FilterOptionsPanel m_filterOptionsPanel = new FilterOptionsPanel();

    /** Button to open the dialog that contains options for file filtering */
    private final JButton m_filterOptionsButton = new JButton("Filter options");

    private final JCheckBox m_includeSubfoldersCheckBox = new JCheckBox("Include subfolders");

    private final DialogComponentButtonGroup m_filterModeButtonGroup;

    /** This model is not used for saving/loading settings but only to control the {@code m_filterModeButtonGroup}. */
    private final SettingsModelString m_filterModeSettingsModel;

    private final JPanel m_filterModePanel;

    private final JPanel m_filterConfigPanel;

    private final FilterMode[] m_filterModes;

    /**
     * Constructor. If {@code showFilterOptionsPanel} is disabled, the panel can still be retrieved by calling
     * {@link #getFilterConfigPanel()}.
     *
     * @param model the settings model
     * @param showFilterOptionsPanel if the panel that allows setting filter options is shown or not
     *
     * @throws IllegalArgumentException if the list of filter modes contains duplicates
     */
    public DialogComponentFilterMode(final SettingsModelFilterMode model, final boolean showFilterOptionsPanel) {
        super(model);
        m_filterModes = model.getSupportedFilterModes();
        // does not need a meaningful config name since it will never be saved
        m_filterModeSettingsModel = new SettingsModelString("dummy", model.getFilterMode().name());
        m_filterModeButtonGroup =
            new DialogComponentButtonGroup(m_filterModeSettingsModel, null, false, model.getSupportedFilterModes());
        m_filterOptionsButton.addActionListener(e -> showFileFilterConfigurationDialog());

        m_filterModePanel = createFilterModePanel();
        m_filterConfigPanel = createFilterPanel();

        m_filterModeSettingsModel.addChangeListener(l -> {
            final FilterMode filterMode = FilterMode.valueOf(m_filterModeSettingsModel.getStringValue());
            model.setFilterMode(filterMode);
            setFilterModeBasedVisibility(filterMode);
        });
        m_includeSubfoldersCheckBox
            .addActionListener(l -> model.setIncludeSubfolders(m_includeSubfoldersCheckBox.isSelected()));

        initComponent(showFilterOptionsPanel);
        getModel().addChangeListener(e -> updateComponent());
        updateComponent();
    }

    private void initComponent(final boolean showFilterOptionsPanel) {
        getComponentPanel().setLayout(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.weightx = 1;
        gbc.weighty = showFilterOptionsPanel ? 0 : 1;
        getComponentPanel().add(m_filterModePanel, gbc);
        if (showFilterOptionsPanel) {
            gbc.gridy++;
            gbc.weighty = 1;
            getComponentPanel().add(m_filterConfigPanel, gbc);
        }
    }

    private JPanel createFilterModePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(m_filterModeButtonGroup.getComponentPanel(), gbc);
        return panel;
    }

    private JPanel createFilterPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        panel.add(m_filterOptionsButton, gbc);
        gbc.gridx++;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(m_includeSubfoldersCheckBox, gbc);
        return panel;
    }

    private static GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        return gbc;
    }

    /**
     * This component consists of either one or two panels depending on the construction. The first one contains the
     * button group to select the filter mode. The second one contains filter configurations. Independently of the
     * construction, the second panel can always be retrieved by calling {@link #getFilterConfigPanel()} and separated
     * from the first panel by placing it somewhere else.
     *
     * @return the panel in which all sub-components of this component are arranged. This panel can be added to the
     *         dialog pane.
     */
    @Override
    public JPanel getComponentPanel() {//NOSONAR
        return super.getComponentPanel();
    }

    /**
     * Enables or disables the specified filter mode button. If the selected button is disabled, the first enabled
     * button starting from left will be selected instead. Returns the selected {@link FilterMode} after
     * enabling/disabling.
     *
     * @param filterMode the filter mode of the button to enable or disable
     * @param enabled if the button should be enabled or not
     * @return the selected filter mode after the call of this function
     */
    public FilterMode setEnabledFilterModeButton(final FilterMode filterMode, final boolean enabled) {
        if (!isSupported(filterMode)) {
            return FilterMode.valueOf(m_filterModeSettingsModel.getStringValue());
        }
        final JRadioButton button = (JRadioButton)m_filterModeButtonGroup.getButton(filterMode.name());

        // if the button should be disabled and was the selected one, select a different one
        if (!enabled && button.isEnabled() && button.isSelected()) {
            for (final FilterMode fm : m_filterModes) {
                if (fm == filterMode) {
                    continue;
                }
                final AbstractButton buttonToSelect = m_filterModeButtonGroup.getButton(fm.name());
                // select the button only if it is not disabled
                if (buttonToSelect.isEnabled()) {
                    // button will be selected when we update the settings model
                    m_filterModeSettingsModel.setStringValue(fm.name());
                    button.setEnabled(false);
                    return fm;
                }
            }
        }
        button.setEnabled(enabled);
        return FilterMode.valueOf(m_filterModeSettingsModel.getStringValue());
    }

    private SettingsModelFilterMode getSM() {
        return (SettingsModelFilterMode)getModel();
    }

    /**
     * Returns {@code true} if the provided {@link FilterMode} is supported. TODO remove/deprecate once we know the
     * supported filter modes in the settings model.
     *
     * @param filterMode the filter mode to check for compatibility
     * @return {@code true} if the provided {@link FilterMode} is supported.
     */
    public boolean isSupported(final FilterMode filterMode) {
        return getSM().isSupported(filterMode);
    }

    /**
     * Returns if the specified filter mode button is enabled.
     *
     * @param filterMode the filter mode of the button to check
     * @return {@code true} if the button is enabled, otherwise {@code false}
     */
    public boolean isFilterModeEnabled(final FilterMode filterMode) {
        return m_filterModeButtonGroup.getButton(filterMode.name()).isEnabled();
    }

    /**
     * Returns the panel that contains the the button to open the dialog with filter options and the check box that
     * enables/disables subfolders inclusion.
     *
     * @return the panel containing filter options button and include subfolders check box
     */
    public JPanel getFilterConfigPanel() {
        return m_filterConfigPanel;
    }

    /** Method called if filter options button is clicked */
    private void showFileFilterConfigurationDialog() {
        final Container c = getComponentPanel().getParent();
        Frame parentFrame = null;
        Container parent = c;
        while (parent != null) {
            if (parent instanceof Frame) {
                parentFrame = (Frame)parent;
                break;
            }
            parent = parent.getParent();
        }
        final FilterOptionsDialog filterOptionsDialog = new FilterOptionsDialog(parentFrame, m_filterOptionsPanel);
        filterOptionsDialog.setLocationRelativeTo(c);
        filterOptionsDialog.setVisible(true);

        if (filterOptionsDialog.getResultStatus() == JOptionPane.OK_OPTION) {
            // update the settings model
            ((SettingsModelFilterMode)getModel())
                .setFilterOptionsSettings(m_filterOptionsPanel.getFilterConfigSettings());
        } else {
            // overwrite the values in the filter options panel with the saved ones
            m_filterOptionsPanel
                .setFilterConfigSettings(((SettingsModelFilterMode)getModel()).getFilterOptionsSettings());
        }
    }

    @Override
    protected void updateComponent() {
        final SettingsModelFilterMode model = (SettingsModelFilterMode)getModel();
        m_filterModeSettingsModel.setStringValue(model.getFilterMode().name());
        m_includeSubfoldersCheckBox.setSelected(model.isIncludeSubfolders());
        m_filterOptionsPanel.setFilterConfigSettings(model.getFilterOptionsSettings());
        setFilterModeBasedVisibility(model.getFilterMode());
        setEnabledComponents(model.isEnabled());
    }

    private void setFilterModeBasedVisibility(final FilterMode filterMode) {
        m_filterOptionsPanel.visibleComponents(filterMode);
        m_filterConfigPanel.setVisible(filterMode == FilterMode.FILES_IN_FOLDERS
            || filterMode == FilterMode.FILES_AND_FOLDERS || filterMode == FilterMode.FOLDERS);
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // nothing to do
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to do
    }

    @Override
    public void setEnabledComponents(final boolean enabled) {
        m_filterModeButtonGroup.getModel().setEnabled(enabled);
        m_filterOptionsButton.setEnabled(enabled);
        m_includeSubfoldersCheckBox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        // nothing to do
    }
}
