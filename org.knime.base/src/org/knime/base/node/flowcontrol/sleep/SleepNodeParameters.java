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
 * ------------------------------------------------------------------------
 */
    
package org.knime.base.node.flowcontrol.sleep;

import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.FileEvent;
import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.WaitMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Wait....
 * 
 * @author GitHub Copilot
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class SleepNodeParameters implements NodeParameters {

    /**
     * The wait mode enumeration for the UI
     */
    enum WaitModeUI {
        @Label("Wait for time")
        WAIT_FOR_TIME,
        @Label("Wait to time") 
        WAIT_UNTIL_TIME,
        @Label("Wait for file")
        WAIT_FILE
    }

    /**
     * The file event enumeration for the UI
     */
    enum FileEventUI {
        @Label("Creation")
        CREATION,
        @Label("Modification")
        MODIFICATION,
        @Label("Deletion")
        DELETION
    }

    @Section(title = "Wait Configuration")
    interface WaitConfigSection {
    }

    // Main wait mode selection
    @Widget(title = "Wait Mode", 
            description = "Select how the node should wait: for a duration, until a specific time, or for a file event.")
    @RadioButtonsWidget
    @Layout(WaitConfigSection.class)
    @Persistor(WaitModePersistor.class)
    @Effect(signals = WaitModeUI.class, operation = EffectType.SHOW)
    WaitModeUI waitMode = WaitModeUI.WAIT_FOR_TIME;

    // Wait for time duration fields
    @Widget(title = "Hours", 
            description = "Number of hours to wait")
    @NumberInputWidget(min = 0)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_FOR_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_FORHOURS)
    int forHours = 0;

    @Widget(title = "Minutes", 
            description = "Number of minutes to wait (0-59)")
    @NumberInputWidget(min = 0, max = 59)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_FOR_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_FORMINUTES)
    int forMinutes = 0;

    @Widget(title = "Seconds", 
            description = "Number of seconds to wait (0-59)")
    @NumberInputWidget(min = 0, max = 59)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_FOR_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_FORSECONDS)
    int forSeconds = 0;

    // Wait until time of day fields
    @Widget(title = "Hours", 
            description = "Hour of day to wait until (0-23)")
    @NumberInputWidget(min = 0, max = 23)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_UNTIL_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_TOHOURS)
    int toHours = 0;

    @Widget(title = "Minutes", 
            description = "Minute of hour to wait until (0-59)")
    @NumberInputWidget(min = 0, max = 59)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_UNTIL_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_TOMINUTES)
    int toMinutes = 0;

    @Widget(title = "Seconds", 
            description = "Second of minute to wait until (0-59)")
    @NumberInputWidget(min = 0, max = 59)
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_UNTIL_TIME)
    @Persist(configKey = SleepNodeModel.CFGKEY_TOSECONDS)
    int toSeconds = 0;

    // Wait for file fields
    @Widget(title = "File path", 
            description = "Path to the file to monitor for events. Can be a local file or URL pointing to a local file.")
    @TextInputWidget
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_FILE)
    @Persist(configKey = SleepNodeModel.CFGKEY_FILEPATH)
    String filePath = "";

    @Widget(title = "File event", 
            description = "Type of file event to wait for.")
    @RadioButtonsWidget
    @Layout(WaitConfigSection.class)
    @ValueSwitchWidget(WaitModeUI.WAIT_FILE)
    @Persistor(FileEventPersistor.class)
    FileEventUI fileEvent = FileEventUI.MODIFICATION;

    /**
     * Custom persistor for WaitMode to handle conversion between UI enum and model enum
     */
    static class WaitModePersistor implements NodeParametersPersistor {
        
        @Override
        public void save(Object obj, NodeSettingsWO settings) {
            if (obj instanceof WaitModeUI uiMode) {
                WaitMode modelMode = switch (uiMode) {
                    case WAIT_FOR_TIME -> WaitMode.WAIT_FOR_TIME;
                    case WAIT_UNTIL_TIME -> WaitMode.WAIT_UNTIL_TIME;
                    case WAIT_FILE -> WaitMode.WAIT_FILE;
                };
                settings.addInt(SleepNodeModel.CFGKEY_WAITOPTION, modelMode.ordinal());
            }
        }

        @Override
        public WaitModeUI load(NodeSettingsRO settings) throws InvalidSettingsException {
            int waitOption = settings.getInt(SleepNodeModel.CFGKEY_WAITOPTION, WaitMode.WAIT_FOR_TIME.ordinal());
            WaitMode[] modes = WaitMode.values();
            if (waitOption >= 0 && waitOption < modes.length) {
                WaitMode modelMode = modes[waitOption];
                return switch (modelMode) {
                    case WAIT_FOR_TIME -> WaitModeUI.WAIT_FOR_TIME;
                    case WAIT_UNTIL_TIME -> WaitModeUI.WAIT_UNTIL_TIME;
                    case WAIT_FILE -> WaitModeUI.WAIT_FILE;
                };
            }
            return WaitModeUI.WAIT_FOR_TIME;
        }

        @Override
        public String[] getConfigPaths() {
            return new String[]{SleepNodeModel.CFGKEY_WAITOPTION};
        }
    }

    /**
     * Custom persistor for FileEvent to handle conversion between UI enum and model enum
     */
    static class FileEventPersistor implements NodeParametersPersistor {
        
        @Override
        public void save(Object obj, NodeSettingsWO settings) {
            if (obj instanceof FileEventUI uiEvent) {
                FileEvent modelEvent = switch (uiEvent) {
                    case CREATION -> FileEvent.CREATION;
                    case MODIFICATION -> FileEvent.MODIFICATION;
                    case DELETION -> FileEvent.DELETION;
                };
                // The model saves this as a string description, not ordinal
                SettingsModelString sms = new SettingsModelString(SleepNodeModel.CFGKEY_FILESTATUS, 
                                                                  modelEvent.description());
                sms.setStringValue(modelEvent.description());
                sms.saveSettingsTo(settings);
            }
        }

        @Override
        public FileEventUI load(NodeSettingsRO settings) throws InvalidSettingsException {
            SettingsModelString sms = new SettingsModelString(SleepNodeModel.CFGKEY_FILESTATUS, 
                                                              FileEvent.MODIFICATION.description());
            try {
                sms.loadSettingsFrom(settings);
                String eventDesc = sms.getStringValue();
                
                for (FileEvent event : FileEvent.values()) {
                    if (event.description().equals(eventDesc)) {
                        return switch (event) {
                            case CREATION -> FileEventUI.CREATION;
                            case MODIFICATION -> FileEventUI.MODIFICATION;
                            case DELETION -> FileEventUI.DELETION;
                        };
                    }
                }
            } catch (Exception e) {
                // Fallback to default
            }
            return FileEventUI.MODIFICATION;
        }

        @Override
        public String[] getConfigPaths() {
            return new String[]{SleepNodeModel.CFGKEY_FILESTATUS};
        }
    }
}
