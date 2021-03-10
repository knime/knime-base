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
 *   May 9, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.perform;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;

import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 * The dimensions panel allowing to selected either a fixed number of columns to reduce to, or to define the information
 * preservation percentage based on which the number of columns to reduce to is computed.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class DimensionSelectionPanel {

    private static final String ZERO_DIM_WARNING = "There are no columns selected. Please select at least one.";

    /** spinner for setting dimensions. */
    private final DialogComponentNumber m_dimensionComponent;

    /** spinner for setting preserved quality. */
    private final DialogComponentNumber m_infPreservationComponent;

    /** selection by dimension? */
    private final JRadioButton m_dimensionSelection;

    /** selection by preserved quality? */
    private final JRadioButton m_qualitySelection;

    private final SettingsModelBoolean m_dimensionSelectedModel;

    private final JLabel m_errorLbl;

    private final JLabel m_dimensionLbl;

    private final JLabel m_infPreservationLbl;

    private int m_maxDimToReduceTo;

    private final JPanel m_mainPanel;

    private final JPanel m_errorPanel;

    /**
     * Constructor.
     *
     * @param applySettings the {@code PCAApplySettings}
     */
    protected DimensionSelectionPanel(final PCAApplySettings applySettings) {
        m_dimensionComponent = new DialogComponentNumber(applySettings.getDimModel(), "", 1, 5);
        m_dimensionComponent.getModel().addChangeListener(e -> updateErrors());
        m_infPreservationComponent = new DialogComponentNumber(applySettings.getInfPreservationModel(), "", 0.1, 5);
        m_infPreservationComponent.getModel().addChangeListener(e -> updateErrors());
        m_dimensionSelectedModel = applySettings.getUseFixedDimensionModel();
        m_dimensionSelection = new JRadioButton("Dimension(s) to reduce to");
        m_qualitySelection = new JRadioButton("Minimum information fraction");
        m_dimensionLbl = new JLabel("");
        m_infPreservationLbl = new JLabel("");
        m_dimensionSelection.addItemListener(e -> {
            final boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
            m_dimensionSelectedModel.setBooleanValue(enabled);
            enableDimComps(enabled);
            updateErrors();
        });

        final ButtonGroup gb = new ButtonGroup();
        gb.add(m_dimensionSelection);
        gb.add(m_qualitySelection);
        m_errorLbl = new JLabel("");
        m_errorLbl.setForeground(Color.RED);

        m_mainPanel = createMainPanel();
        m_errorPanel = createErrorPanel();
    }

    private void enableDimComps(final boolean enabled) {
        m_dimensionComponent.getModel().setEnabled(enabled);
        m_dimensionLbl.setEnabled(enabled);
        m_infPreservationComponent.getModel().setEnabled(!enabled);
        m_infPreservationLbl.setEnabled(!enabled);
    }

    private JPanel createMainPanel() {
        final JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder("Target dimensions"));
        p.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(m_dimensionSelection, gbc);
        ++gbc.gridx;
        p.add(m_dimensionComponent.getComponentPanel(), gbc);
        ++gbc.gridx;
        p.add(m_dimensionLbl, gbc);
        gbc.gridx = 0;
        ++gbc.gridy;
        p.add(m_qualitySelection, gbc);
        ++gbc.gridx;
        p.add(m_infPreservationComponent.getComponentPanel(), gbc);
        ++gbc.gridx;
        p.add(m_infPreservationLbl, gbc);
        return p;
    }

    /**
     * Returns the main panel, i.e., the panel containing the dimension and information preservation selection button
     * group.
     *
     * @return the main panel
     */
    public final JPanel getMainPanel() {
        return m_mainPanel;
    }

    private JPanel createErrorPanel() {
        final JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        p.add(m_errorLbl, gbc);
        return p;
    }

    /**
     * Returns the error panel.
     *
     * @return the error panel
     */
    public final JPanel getErrorPanel() {
        return m_errorPanel;
    }

    /**
     * Updates the maximum dimension to reduce to and shows errors if necessary.
     *
     * @param maxDimToReduceTo the maximum number of dimensions to reduce to
     */
    protected final void updateMaxDim(final int maxDimToReduceTo) {
        m_maxDimToReduceTo = maxDimToReduceTo;
        m_dimensionComponent.setToolTipText("Maximum dimension: " + m_maxDimToReduceTo);
        updateErrors();
    }

    /**
     * Updates the errors.
     */
    protected void updateErrors() {
        final String error = getError();
        if (error != null) {
            setError(error);
        } else {
            clearError();
        }
    }

    private String getError() {
        if (m_maxDimToReduceTo <= 0) {
            updateSpinner(Color.RED, false);
            return ZERO_DIM_WARNING;
        } else if (m_maxDimToReduceTo < getDimToReduceTo()) {
            if (m_dimensionSelectedModel.getBooleanValue()) {
                updateSpinner(Color.RED, true);
                return "The current number of selected dimensions (" + getDimToReduceTo()
                    + ") is higher than the maximum allowed value of " + m_maxDimToReduceTo + ".";
            } else {
                updateSpinner(Color.RED, false);
            }
        } else {
            updateSpinner(Color.WHITE, m_dimensionComponent.getModel().isEnabled());
        }
        return null;
    }

    private void setError(final String error) {
        m_errorLbl.setText(TransformationUtils.wrapText(error));
        m_errorLbl.setVisible(true);
    }

    private void clearError() {
        m_errorLbl.setText("");
        m_errorLbl.setVisible(false);
    }

    private void updateSpinner(final Color color, final boolean enable) {
        final JSpinner spinner = (JSpinner)m_dimensionComponent.getComponentPanel().getComponent(1);
        final JFormattedTextField spinnerTextField = ((DefaultEditor)spinner.getEditor()).getTextField();
        spinnerTextField.setBackground(color);
        spinnerTextField.setEnabled(enable);
        spinner.setEnabled(enable);
    }

    private void validateBeforeSave() throws InvalidSettingsException {
        final String error = getError();
        if (error != null) {
            throw new InvalidSettingsException(error);
        }
    }

    /**
     * Sets the dimension label.
     *
     * @param text the text to be set
     */
    protected void setDimensionLblText(final String text) {
        m_dimensionLbl.setText(text);
    }

    /**
     * Sets the information preservation label.
     *
     * @param text the text to be set
     */
    protected void setInfPreservationLblText(final String text) {
        m_infPreservationLbl.setText(text);
    }

    /**
     * Returns {@code true} if the dimension selection is active, and {@code false} if the information preservation
     * selection is active.
     *
     * @return flag indicating whether or not the dimension selection is active
     */
    protected boolean isDimensionSelection() {
        return m_dimensionSelection.isSelected();
    }

    /**
     * Returns the maximum possible number of dimension to reduce to.
     *
     * @return the maximum possible number of dimension to reduce to
     */
    protected int getMaxDimToReduceTo() {
        return m_maxDimToReduceTo;
    }

    /**
     * Returns the currently selected number of dimensions to reduce to.
     *
     * @return the selected number of dimensions to reduce to
     */
    protected int getDimToReduceTo() {
        return ((SettingsModelInteger)m_dimensionComponent.getModel()).getIntValue();
    }

    /**
     * Sets the number of dimension to reduce to.
     *
     * @param dimToRecudeTo the number of dimension to reduce to
     */
    protected void setDimToReduceTo(final int dimToRecudeTo) {
        ((SettingsModelInteger)m_dimensionComponent.getModel()).setIntValue(dimToRecudeTo);
    }

    /**
     * Returns the selected informatation preservation value.
     *
     * @return the information preservation value
     */
    protected double getInfPreservationValue() {
        return ((SettingsModelDouble)m_infPreservationComponent.getModel()).getDoubleValue();
    }

    /**
     * Saves the settings.
     *
     * @param settings the settings object to write into.
     * @throws InvalidSettingsException - If the settings are not applicable to the model.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBeforeSave();
        m_dimensionSelectedModel.saveSettingsTo(settings);
        m_dimensionComponent.saveSettingsTo(settings);
        m_infPreservationComponent.saveSettingsTo(settings);
    }

    /**
     * Loads the settings.
     *
     * @param settings The settings to load into the dialog. Could be an empty object or contain invalid settings. But
     *            will never be null.
     * @param specs The input data table specs. If no spec is available for any given port (because the port is not
     *            connected or the previous node does not produce a spec) the framework will pass an empty
     *            {@link DataTableSpec} (no columns) unless the port is marked as {@link PortType#isOptional() optional}
     *            (in which case the array element is null).
     * @throws NotConfigurableException if the dialog cannot be opened because of real invalid settings or if any
     *             preconditions are not fulfilled, e.g. no predecessor node, no nominal column in input table, etc.
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_dimensionSelectedModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException ise) {
            throw new NotConfigurableException(ise.getMessage(), ise);
        }
        m_dimensionComponent.loadSettingsFrom(settings, specs);
        m_infPreservationComponent.loadSettingsFrom(settings, specs);

        // ensure that only one model is enabled at a time
        if (m_dimensionSelectedModel.getBooleanValue()) {
            m_dimensionSelection.doClick();
        } else {
            m_qualitySelection.doClick();
        }
        enableDimComps(m_dimensionSelection.isSelected());
    }

}
