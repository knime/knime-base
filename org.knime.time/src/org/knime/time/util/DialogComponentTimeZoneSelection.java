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
 */
package org.knime.time.util;

import java.awt.FlowLayout;
import java.time.ZoneId;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Provide a standard component for a dialog that allows to select a time zone. This is a reduced version of
 * {@link DialogComponentDateTimeSelection}.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @since 4.2
 */
public final class DialogComponentTimeZoneSelection extends DialogComponent {

    private final JLabel m_zoneLabel;

    private final JComboBox<String> m_zoneComboBox;

    /**
     * Constructor puts for the date, time and time zone a checkbox and chooser into the panel according to display
     * option.
     *
     * @param model the model that stores the values for this component
     * @param title label for the border of the dialog, if <code>null</code> no border is created
     * @param label label in front of the combo box, if <code>null</code> no label is created
     */
    public DialogComponentTimeZoneSelection(final SettingsModelTimeZone model, final String title, final String label) {
        super(model);

        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        if (label != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        } else {
            // Remove border
            getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        }

        if (!StringUtils.isBlank(label)) {
            m_zoneLabel = new JLabel(label);
            panel.add(m_zoneLabel);
        } else {
            m_zoneLabel = new JLabel("");
        }

        m_zoneComboBox = new JComboBox<>();
        for (final String id : new TreeSet<String>(ZoneId.getAvailableZoneIds())) {
            m_zoneComboBox.addItem(StringUtils.abbreviateMiddle(id, "..", 29));
        }
        final ZoneId zone = model.getZone();
        if (zone != null) {
            m_zoneComboBox.setSelectedItem(zone.getId());
        }
        m_zoneComboBox.setEnabled(model.isEnabled());
        panel.add(m_zoneComboBox);

        m_zoneComboBox.addActionListener(e -> {
            model.setZone(ZoneId.of((String)m_zoneComboBox.getSelectedItem()));
        });
        model.addChangeListener(e -> updateComponent());

        getComponentPanel().add(panel);
    }

    @Override
    protected void updateComponent() {
        final SettingsModelTimeZone model = (SettingsModelTimeZone)getModel();
        m_zoneComboBox.setSelectedItem(model.getZone().getId());
        setEnabledComponents(model.isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     */
    private void updateModel() {
        final SettingsModelTimeZone model = (SettingsModelTimeZone)getModel();
        model.setZone(ZoneId.of((String)m_zoneComboBox.getSelectedItem()));
    }

    @Override
    public void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        //  nothing to do
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_zoneLabel.setEnabled(enabled);
        m_zoneComboBox.setEnabled(enabled);
    }

    @Override
    public void setToolTipText(final String text) {
        getComponentPanel().setToolTipText(text);
    }
}
