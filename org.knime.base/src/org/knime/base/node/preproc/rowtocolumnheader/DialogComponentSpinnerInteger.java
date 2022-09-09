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
 *   Sep 8, 2022 (leonard.woerteler): created
 */
package org.knime.base.node.preproc.rowtocolumnheader;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Dialog component for entering an integer using a spinner interface.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public final class DialogComponentSpinnerInteger extends DialogComponent {
    /** Spinner model using integers. */
    private final SpinnerNumberModel m_spinnerModel;
    /** Spinner component. */
    private final JSpinner m_spinner;

    /**
     * Creates a new dialog component.
     *
     * @param itegerModel underlying integer model
     * @param label label for the spinner
     */
    public DialogComponentSpinnerInteger(final SettingsModelInteger itegerModel, final String label) {
        super(itegerModel);

        m_spinnerModel = new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1);
        m_spinner = new JSpinner(m_spinnerModel);
        m_spinnerModel.setValue(itegerModel.getIntValue());
        // update the model, if the user changes the component

        m_spinnerModel.addChangeListener(e -> {
            final SettingsModelInteger model = (SettingsModelInteger) getModel();
            model.setIntValue((Integer) m_spinnerModel.getNumber());
        });

        // update the checkbox, whenever the model changes - make sure we get notified first.
        getModel().addChangeListener(e -> updateComponent());
        final var panel = Box.createHorizontalBox();
        panel.add(Box.createHorizontalGlue());
        panel.add(new JLabel(label));
        panel.add(Box.createHorizontalGlue());
        panel.add(m_spinner);
        getComponentPanel().add(panel);
        //call this method to be in sync with the settings model
        updateComponent();
    }

    @Override
    protected void updateComponent() {
        // only update component if values are off
        final SettingsModelInteger model = (SettingsModelInteger) getModel();
        setEnabledComponents(model.isEnabled());
        if (!m_spinnerModel.getNumber().equals(model.getIntValue())) {
            m_spinnerModel.setValue(model.getIntValue());
        }
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        // nothing to do.
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // we're always good.
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_spinner.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        m_spinner.setToolTipText(text);
    }
}

