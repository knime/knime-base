/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.connections.base.auth;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.FlowVariableListCellRenderer;
import org.knime.core.node.util.FlowVariableListCellRenderer.FlowVariableCell;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Provides a standard component for a dialog that allows to select a credential provided by a
 * {@link CredentialsProvider}.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class DialogComponentCredentialSelection extends DialogComponent {

    private final JComboBox<FlowVariableCell> m_jcomboBox;

    private final ItemListener m_listener;

    private final boolean m_hasNone;

    private final Supplier<CredentialsProvider> m_credentialsProvider;

    private boolean m_selectionIsValid = false;

    /**
     * Constructor creates a label and a combo box and adds them to the component panel. The credentials of the given
     * {@link CredentialsProvider} are added as items to the combo box.
     *
     * @param model The {@link SettingsModelString} to store the name of the selected credential.
     * @param label The title of the label to show. May be null to not show any label at all.
     * @param credentialsProvider The {@link CredentialsProvider} that supplies the credentials to choose from.
     */
    public DialogComponentCredentialSelection(final SettingsModelString model, final String label,
        final Supplier<CredentialsProvider> credentialsProvider) {
        this(model, label, credentialsProvider, false);
    }

    /**
     * Constructor creates a label and a combo box and adds them to the component panel. The credentials of the given
     * {@link CredentialsProvider} are added as items to the combo box.
     *
     * @param model The {@link SettingsModelString} to store the name of the selected credential.
     * @param label The title of the label to show. May be null to not show any label at all.
     * @param credentialsProvider The {@link CredentialsProvider} that supplies the credentials to choose from.
     * @param hasNone if true the field is optional and can be set to "NONE"
     */
    public DialogComponentCredentialSelection(final SettingsModelString model, final String label,
        final Supplier<CredentialsProvider> credentialsProvider, final boolean hasNone) {
        super(model);

        m_credentialsProvider = CheckUtils.checkArgumentNotNull(credentialsProvider);
        m_hasNone = hasNone;

        if (label != null) {
            getComponentPanel().add(new JLabel(label));
        }

        m_listener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // if a new item is selected update the model
                updateModel();
            }
        };

        m_jcomboBox = new JComboBox<>();
        m_jcomboBox.setRenderer(new FlowVariableListCellRenderer());
        m_jcomboBox.setEditable(false);
        m_jcomboBox.addItemListener(m_listener);
        getComponentPanel().add(m_jcomboBox);

        getModel().addChangeListener(e -> updateComponent());

        updateComponent();
    }

    private void updateModel() {
        if (m_jcomboBox.getSelectedItem() == null) {
            ((SettingsModelString)getModel()).setStringValue(null);
        } else {
            // save the value of the flow variable into the model
            ((SettingsModelString)getModel())
                .setStringValue(((FlowVariableCell)m_jcomboBox.getSelectedItem()).getName());
        }
    }

    @Override
    protected void updateComponent() {
        final var selection = ((SettingsModelString)getModel()).getStringValue();

        final var newVars = Optional.ofNullable(m_credentialsProvider.get())//
            .map(CredentialsProvider::listNames)//
            .orElse(Collections.emptyList())//
            .stream()//
            .map(DialogComponentCredentialSelection::mockCredentialsFlowVariable)//
            .map(FlowVariableCell::new)//
            .collect(Collectors.toList());

        if (m_hasNone) {
            newVars.add(new FlowVariableCell(new FlowVariable("NONE", "")));
        }

        m_jcomboBox.removeItemListener(m_listener);
        m_jcomboBox.removeAllItems();
        m_selectionIsValid = false;

        for (var flowVarCell : newVars) {
            m_jcomboBox.addItem(flowVarCell);
            if (flowVarCell.getName().equals(selection)) {
                m_jcomboBox.setSelectedItem(flowVarCell);
                m_selectionIsValid = true;
            }
        }

        if (!m_selectionIsValid) {
            if (selection != null && selection.length() > 0) {
                final var selectedVar = new FlowVariableCell(selection);
                m_jcomboBox.addItem(selectedVar);
                m_jcomboBox.setSelectedItem(selectedVar);
            } else {
                m_jcomboBox.setSelectedIndex(-1);
            }
        }
        m_jcomboBox.addItemListener(m_listener);

        setEnabledComponents(getModel().isEnabled());
    }

    private static FlowVariable mockCredentialsFlowVariable(final String name) {
        try {
            return CredentialsStore.newCredentialsFlowVariable(name,//
                "", "", false, false);
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("Invalid credentials " + ex);
        }
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        if (!m_selectionIsValid) {
            throw new InvalidSettingsException("No valid credential selected.");
        }
        updateModel();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // nothing to do here
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_jcomboBox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_jcomboBox.setToolTipText(text);
    }
}
