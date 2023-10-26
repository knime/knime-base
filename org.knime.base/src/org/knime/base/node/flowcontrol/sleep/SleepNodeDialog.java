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
 */
package org.knime.base.node.flowcontrol.sleep;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.FileEvent;
import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.WaitMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.node.workflow.FlowVariable.Type;

/**
 * @author Tim-Oliver Buchholz, Knime.com, Zurich, Switzerland
 */
public class SleepNodeDialog extends NodeDialogPane {

    private FilesHistoryPanel m_fileChooser;

    private JRadioButton m_fileRB;

    private AbstractButton m_toRB;

    private JRadioButton m_forRB;

    private WaitMode m_selection;

    private DialogComponentButtonGroup m_events;

    private SpinnerDateModel m_waitToSpinnerModel;

    private SpinnerDateModel m_waitForSpinnerModel;

    private JSpinner m_forSpinner;

    private JSpinner m_toSpinner;

    /**
     *
     */
    public SleepNodeDialog() {
        waitForTimePanel();
        waitToTimePanel();
        waitForFile();
        final var panel = new JPanel(new GridBagLayout());
        final var constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(2, 4, 2, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;

        panel.add(m_forRB, constraints);

        constraints.gridx++;

        panel.add(m_forSpinner, constraints);

        constraints.gridy++;
        constraints.gridx = 0;

        panel.add(m_toRB, constraints);

        constraints.gridx++;

        panel.add(m_toSpinner, constraints);

        constraints.gridy++;
        constraints.gridx = 0;

        panel.add(m_fileRB, constraints);

        constraints.gridx++;

        panel.add(m_events.getComponentPanel(), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 2;

        panel.add(m_fileChooser, constraints);

        final var selection = new ButtonGroup();
        selection.add(m_forRB);
        selection.add(m_toRB);
        selection.add(m_fileRB);
        m_fileRB.doClick();
        m_toRB.doClick();
        m_forRB.doClick();

        addTab("Options", panel);
    }

    private void waitForTimePanel() {
        m_waitForSpinnerModel = new SpinnerDateModel();
        m_forSpinner = new JSpinner(m_waitForSpinnerModel);
        m_forSpinner.setEditor(new JSpinner.DateEditor(m_forSpinner, "HH:mm:ss"));

        final var dateTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(0, 0), ZoneId.systemDefault());
        m_waitForSpinnerModel.setValue(Date.from(dateTime.toInstant()));

        m_forRB = new JRadioButton("Wait for time:");
        m_forRB.doClick();
        m_forRB.addItemListener(e -> {
            if (m_forRB.isSelected()) {
                m_selection = WaitMode.WAIT_FOR_TIME;
            }
            m_forSpinner.setEnabled(m_forRB.isSelected());
        });
    }

    private void waitToTimePanel() {

        m_waitToSpinnerModel = new SpinnerDateModel();
        m_toSpinner = new JSpinner(m_waitToSpinnerModel);
        m_toSpinner.setEditor(new JSpinner.DateEditor(m_toSpinner, "HH:mm:ss"));

        final var dateTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(0, 0), ZoneId.systemDefault());
        m_waitToSpinnerModel.setValue(Date.from(dateTime.toInstant()));

        m_toRB = new JRadioButton("Wait to time:");
        m_toRB.doClick();
        m_toRB.addItemListener(e -> {
            if (m_toRB.isSelected()) {
                m_selection = WaitMode.WAIT_UNTIL_TIME;
            }
            m_toSpinner.setEnabled(m_toRB.isSelected());
        });
    }

    private void waitForFile() {
        m_events = new DialogComponentButtonGroup(
            new SettingsModelString(SleepNodeModel.CFGKEY_FILESTATUS, "Modification"), false, null,
            FileEvent.CREATION.description(), FileEvent.MODIFICATION.description(), FileEvent.DELETION.description());

        @SuppressWarnings("deprecation")
        final var fvm = createFlowVariableModel(SleepNodeModel.CFGKEY_FILEPATH, Type.STRING);

        m_fileChooser = new FilesHistoryPanel(fvm, SleepNodeModel.CFGKEY_FILEPATH, LocationValidation.None);

        m_fileRB = new JRadioButton("Wait for file.. ");
        m_fileRB.addItemListener(e -> {
            if (m_fileRB.isSelected()) {
                m_selection = WaitMode.WAIT_FILE;
            }
            m_fileChooser.setEnabled(m_fileRB.isSelected());
            m_events.getModel().setEnabled(m_fileRB.isSelected());
        });

    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) {

        final var forHours = settings.getInt(SleepNodeModel.CFGKEY_FORHOURS, 0);
        final var forMinutes = settings.getInt(SleepNodeModel.CFGKEY_FORMINUTES, 0);
        final var forSeconds = settings.getInt(SleepNodeModel.CFGKEY_FORSECONDS, 0);
        final var waitForTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(forHours, forMinutes, forSeconds),
            ZoneId.systemDefault());
        m_waitForSpinnerModel.setValue(Date.from(waitForTime.toInstant()));

        final var toHours = settings.getInt(SleepNodeModel.CFGKEY_TOHOURS, 0);
        final var toMinutes = settings.getInt(SleepNodeModel.CFGKEY_TOMINUTES, 0);
        final var toSeconds = settings.getInt(SleepNodeModel.CFGKEY_TOSECONDS, 0);
        final var waitUntilTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(toHours, toMinutes, toSeconds),
            ZoneId.systemDefault());
        m_waitToSpinnerModel.setValue(Date.from(waitUntilTime.toInstant()));

        m_fileChooser.setSelectedFile(settings.getString(SleepNodeModel.CFGKEY_FILEPATH, ""));

        try {
            m_events.loadSettingsFrom(settings, specs);
            m_fileRB.doClick();
        } catch (final NotConfigurableException e) { // NOSONAR
            // nothing
        }

        final var waitMode = settings.getInt(SleepNodeModel.CFGKEY_WAITOPTION, WaitMode.WAIT_FOR_TIME.ordinal());
        m_selection = WaitMode.values()[waitMode];
        switch (m_selection) {
            case WAIT_FOR_TIME   -> m_forRB.doClick();
            case WAIT_UNTIL_TIME -> m_toRB.doClick();
            case WAIT_FILE       -> m_fileRB.doClick();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final var localZone = ZoneId.systemDefault();

        final var waitForTime = LocalTime.ofInstant(m_waitForSpinnerModel.getDate().toInstant(), localZone);
        settings.addInt(SleepNodeModel.CFGKEY_FORHOURS, waitForTime.getHour());
        settings.addInt(SleepNodeModel.CFGKEY_FORMINUTES, waitForTime.getMinute());
        settings.addInt(SleepNodeModel.CFGKEY_FORSECONDS, waitForTime.getSecond());

        final var waitUntilTime = LocalTime.ofInstant(m_waitToSpinnerModel.getDate().toInstant(), localZone);
        settings.addInt(SleepNodeModel.CFGKEY_TOHOURS, waitUntilTime.getHour());
        settings.addInt(SleepNodeModel.CFGKEY_TOMINUTES, waitUntilTime.getMinute());
        settings.addInt(SleepNodeModel.CFGKEY_TOSECONDS, waitUntilTime.getSecond());

        settings.addString(SleepNodeModel.CFGKEY_FILEPATH, m_fileChooser.getSelectedFile());
        m_events.saveSettingsTo(settings);

        settings.addInt(SleepNodeModel.CFGKEY_WAITOPTION, m_selection.ordinal());
    }

}
