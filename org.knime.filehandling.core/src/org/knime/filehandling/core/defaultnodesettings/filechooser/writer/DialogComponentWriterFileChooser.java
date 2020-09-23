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
 *   Jun 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.writer;

import java.awt.GridBagLayout;
import java.util.Enumeration;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractDialogComponentFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * File chooser dialog component for writer nodes.</br>
 * In addition to the components offered by {@link AbstractDialogComponentFileChooser}, this dialog component adds a
 * check box for specifying whether missing folders should be created, as well as radio buttons to select the desired
 * policy for existing files. </br>
 * If the settings model provided in the constructor supports fewer than two {@link FileOverwritePolicy
 * FileOverwritePolicies}, no radio buttons are provided because the user has no choice.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DialogComponentWriterFileChooser
    extends AbstractDialogComponentFileChooser<SettingsModelWriterFileChooser> {

    private JLabel m_writeOptionsLabel;

    private JCheckBox m_createMissingFolders;

    private ButtonGroup m_policyButtons;

    private boolean m_isEnabled = true;

    /**
     * Constructor using a default status message calculator implementation.
     *
     * @param model the {@link AbstractSettingsModelFileChooser} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param locationFvm the {@link FlowVariableModel} for the location
     * @param filterModes the available {@link FilterMode FilterModes} (if a none are provided, the default filter mode
     *            from <b>model</b> is used)
     */
    public DialogComponentWriterFileChooser(final SettingsModelWriterFileChooser model, final String historyID,
        final FlowVariableModel locationFvm, final FilterMode... filterModes) {
        this(model//
            , historyID//
            , locationFvm//
            , DefaultWriterStatusMessageReporter::new//
            , filterModes);
    }

    /**
     * Constructor.
     *
     * @param model the {@link AbstractSettingsModelFileChooser} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param locationFvm the {@link FlowVariableModel} for the location
     * @param statusMessageReporter function to create a {@link StatusMessageReporter} used to update the status of this
     *            component
     * @param filterModes the available {@link FilterMode FilterModes} (if a none are provided, the default filter mode
     *            from <b>model</b> is used)
     */
    public DialogComponentWriterFileChooser(final SettingsModelWriterFileChooser model, final String historyID,
        final FlowVariableModel locationFvm,
        final Function<SettingsModelWriterFileChooser, StatusMessageReporter> statusMessageReporter,
        final FilterMode... filterModes) {
        super(model//
            , historyID//
            , DialogType.SAVE_DIALOG//
            , "Write to"//
            , locationFvm//
            , statusMessageReporter//
            , filterModes);
        initComponents();
        m_createMissingFolders
            .addActionListener(e -> model.setCreateMissingFolders(m_createMissingFolders.isSelected()));
    }

    private void initComponents() {
        initCreateMissingFolders();
        initPolicyButtons();
        initWriteOptionsLabel();
    }

    private void initWriteOptionsLabel() {
        if (m_writeOptionsLabel == null) {
            m_writeOptionsLabel = new JLabel("Write options");
        }
    }

    private void initPolicyButtons() {
        if (m_policyButtons == null) {
            m_policyButtons = new ButtonGroup();
            final Set<FileOverwritePolicy> possiblePolicies = getSettingsModel().getSupportedPolicies();
            for (FileOverwritePolicy policy : possiblePolicies) {
                m_policyButtons.add(createButton(policy));
            }
        }
    }

    private void initCreateMissingFolders() {
        if (m_createMissingFolders == null) {
            m_createMissingFolders = new JCheckBox("Create missing folders");
            m_createMissingFolders.addActionListener(
                e -> getSettingsModel().setCreateMissingFolders(m_createMissingFolders.isSelected()));
        }
    }

    private JRadioButton createButton(final FileOverwritePolicy policy) {
        final JRadioButton button = new JRadioButton(policy.getText());
        button.setActionCommand(policy.getActionCommand());
        button.addActionListener(e -> updatePolicyModel());
        button.setSelected(getSettingsModel().getFileOverwritePolicy() == policy);
        return button;
    }

    private void updatePolicyModel() {
        String actionCommand = m_policyButtons.getSelection().getActionCommand();
        final FileOverwritePolicy selected = actionCommand == null ? null : FileOverwritePolicy.valueOf(actionCommand);
        getSettingsModel().setFileOverwritePolicy(selected);
    }

    @Override
    public SettingsModelWriterFileChooser getSettingsModel() {
        return (SettingsModelWriterFileChooser)getModel();
    }

    @Override
    protected void addAdditionalComponents(final JPanel panel, final GBCBuilder gbc) {
        initComponents();
        panel.add(m_writeOptionsLabel, gbc.build());
        panel.add(createWriteOptionsPanel(), gbc.incX().insetLeft(6).build());
        panel.add(new JPanel(), gbc.fillHorizontal().incX().build());
    }

    private JPanel createWriteOptionsPanel() {
        final JPanel additional = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart();
        additional.add(m_createMissingFolders, gbc.build());
        if (getSettingsModel().hasPolicyChoice()) {
            additional.add(createOverwritePolicyPanel(), gbc.incX().insetLeft(40).build());
        }
        additional.add(new JPanel(), gbc.incX().setWeightX(1).build());
        return additional;
    }

    private JPanel createOverwritePolicyPanel() {
        final GBCBuilder gbc = new GBCBuilder().anchorLineStart().resetX();
        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.add(new JLabel("If exists: "), gbc.build());
        forEachButton(b -> buttonPanel.add(b, gbc.incX().build()));
        return buttonPanel;
    }

    @Override
    protected void updateAdditionalComponents() {
        updateCreateMissingFoldersComponent();
        if (getSettingsModel().hasPolicyChoice()) {
            updatePolicyComponent();
        }
    }

    private void updatePolicyComponent() {
        final FileOverwritePolicy policy = getSettingsModel().getFileOverwritePolicy();
        forEachButton(b -> {
            final FileOverwritePolicy buttonPolicy = FileOverwritePolicy.valueOf(b.getActionCommand());
            b.setSelected(buttonPolicy == policy);
        });
    }

    private void updateCreateMissingFoldersComponent() {
        m_createMissingFolders.setSelected(getSettingsModel().isCreateMissingFoldersUI());
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        super.setEnabledComponents(enabled);
        m_isEnabled = enabled;
        updateEnabledStatus();
    }

    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        m_createMissingFolders.setToolTipText(text);
        forEachButton(b -> b.setToolTipText(text));
    }

    private void updateEnabledStatus() {
        m_writeOptionsLabel.setEnabled(enableComponents());
        m_createMissingFolders.setEnabled(enableComponents());
        forEachButton(b -> b.setEnabled(enableComponents()));
    }

    private boolean enableComponents() {
        return m_isEnabled && !isCustomURL();
    }

    private final boolean isCustomURL() {
        return getSettingsModel().isCustomURL();
    }

    private void forEachButton(final Consumer<AbstractButton> consumer) {
        final Enumeration<AbstractButton> buttons = m_policyButtons.getElements();
        while (buttons.hasMoreElements()) {
            consumer.accept(buttons.nextElement());
        }
    }

}
