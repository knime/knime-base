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

import java.time.LocalTime;
import java.util.Arrays;

import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.FileEvent;
import org.knime.base.node.flowcontrol.sleep.SleepNodeModel.WaitMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LocalFileReaderWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Node parameters for Wait....
 *
 * @author Ali Asghar Marvi, KNIME AG, Zurich, Switzerland
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SleepNodeParameters implements NodeParameters {

    // ===== PARAMETER REFERENCES FOR EFFECTS =====

    interface WaitModeRef extends ParameterReference<WaitMode> {
    }

    // ===== EFFECT PREDICATES =====

    static final class IsWaitForTime implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(WaitModeRef.class).isOneOf(WaitMode.WAIT_FOR_TIME);
        }
    }

    static final class IsWaitToTime implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(WaitModeRef.class).isOneOf(WaitMode.WAIT_UNTIL_TIME);
        }
    }

    static final class IsWaitForFile implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(WaitModeRef.class).isOneOf(WaitMode.WAIT_FILE);
        }
    }

    // Main wait mode selection
    @Widget(title = "Wait",
        description = "Select how the node should wait: for a duration, until a specific time, or for a file event.")
    @RadioButtonsWidget
    @ValueReference(WaitModeRef.class)
    @Persistor(WaitModePersistor.class)
    WaitMode m_waitMode = WaitMode.WAIT_FOR_TIME;

    // Wait for time duration fields
    @Widget(title = "Duration", description = "The duration to wait for.")
    //create a custom persistor for wait For time
    @Persistor(WaitForTimePersistor.class)
    @Effect(predicate = IsWaitForTime.class, type = EffectType.SHOW)
    LocalTime m_forTime = LocalTime.ofSecondOfDay(1);

    // Wait to time duration fields
    @Widget(title = "Time", description = "The time of day to wait until.")
    //create a custom persistor for wait to Time
    @Persistor(WaitToTimePersistor.class)
    @Effect(predicate = IsWaitToTime.class, type = EffectType.SHOW)
    LocalTime m_toTime = LocalTime.MIDNIGHT;

    static abstract class WaitPersistor implements NodeParametersPersistor<LocalTime> {

        private final String m_cfgKeyHours;

        private final String m_cfgKeyMinutes;

        private final String m_cfgKeySeconds;

        private final String m_cfgKeyMilliseconds;

        protected WaitPersistor(final String cfgKeyHours, final String cfgKeyMinutes, final String cfgKeySeconds,
            final String cfgKeyMilliseconds) {
            m_cfgKeyHours = cfgKeyHours;
            m_cfgKeyMinutes = cfgKeyMinutes;
            m_cfgKeySeconds = cfgKeySeconds;
            m_cfgKeyMilliseconds = cfgKeyMilliseconds;
        }

        @Override
        public void save(final LocalTime time, final NodeSettingsWO settings) {
            settings.addInt(m_cfgKeyHours, time.getHour());
            settings.addInt(m_cfgKeyMinutes, time.getMinute());
            settings.addInt(m_cfgKeySeconds, time.getSecond());
            settings.addInt(m_cfgKeyMilliseconds, time.getNano() / 1_000_000);
        }

        @Override
        public LocalTime load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int hours = settings.getInt(m_cfgKeyHours);
            int minutes = settings.getInt(m_cfgKeyMinutes);
            int seconds = settings.getInt(m_cfgKeySeconds);
            int nanoseconds = settings.getInt(m_cfgKeyMilliseconds, 0) * 1_000_000; // introduced with 5.8
            return LocalTime.of(hours, minutes, seconds, nanoseconds);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_cfgKeyHours}, {m_cfgKeyMinutes}, {m_cfgKeySeconds}, {m_cfgKeyMilliseconds}};
        }
    }

    //Wait for Time Persistor
    static class WaitForTimePersistor extends WaitPersistor {
        public WaitForTimePersistor() {
            super(SleepNodeModel.CFGKEY_FORHOURS, SleepNodeModel.CFGKEY_FORMINUTES, SleepNodeModel.CFGKEY_FORSECONDS,
                SleepNodeModel.CFGKEY_FORMILLISECONDS);
        }
    }

    static class WaitToTimePersistor extends WaitPersistor {
        public WaitToTimePersistor() {
            super(SleepNodeModel.CFGKEY_TOHOURS, SleepNodeModel.CFGKEY_TOMINUTES, SleepNodeModel.CFGKEY_TOSECONDS,
                SleepNodeModel.CFGKEY_TOMILLISECONDS);
        }
    }

    // Wait for file fields

    @Widget(title = "File",
        description = "Path to the file to monitor for events. Can be a local file or URL pointing to a local file.")
    @Persist(configKey = SleepNodeModel.CFGKEY_FILEPATH)
    @LocalFileReaderWidget
    @Effect(predicate = IsWaitForFile.class, type = EffectType.SHOW)
    String m_filePath = "";

    @Widget(title = "File change", description = "Type of file event to wait for.")
    @ValueSwitchWidget
    @Persistor(FileEventPersistor.class)
    @Effect(predicate = IsWaitForFile.class, type = EffectType.SHOW)
    FileEvent m_fileEvent = FileEvent.MODIFICATION;

    /**
     * Custom persistor for WaitMode to handle conversion between UI enum and model enum
     */
    static class WaitModePersistor implements NodeParametersPersistor<WaitMode> {

        @Override
        public void save(final WaitMode obj, final NodeSettingsWO settings) {
            settings.addInt(SleepNodeModel.CFGKEY_WAITOPTION, obj.ordinal());
        }

        @Override
        public WaitMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return SleepNodeModel.getWaitMode(settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SleepNodeModel.CFGKEY_WAITOPTION}};
        }
    }

    /**
     * Custom persistor for FileEvent to handle conversion between UI enum and model enum
     */
    static class FileEventPersistor implements NodeParametersPersistor<FileEvent> {

        @Override
        public void save(final FileEvent obj, final NodeSettingsWO settings) {
            settings.addString(SleepNodeModel.CFGKEY_FILESTATUS, obj == null ? null : obj.description());

        }

        @Override
        public FileEvent load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var fileEventDesc = settings.getString(SleepNodeModel.CFGKEY_FILESTATUS);
            if (fileEventDesc == null) {
                return FileEvent.MODIFICATION;
            }
            return Arrays.stream(FileEvent.values()) //
                .filter(evt -> evt.description().equals(fileEventDesc)) //
                .findAny() //
                .orElseThrow(() -> new InvalidSettingsException("Unknown file event: '" + fileEventDesc + "'"));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SleepNodeModel.CFGKEY_FILESTATUS}};
        }
    }
}
