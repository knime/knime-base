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
 *   Oct 29, 2024 (david): created
 */
package org.knime.time.node.manipulate.modifytimezone;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleDataValueClassesSupplier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.ReplaceOrAppend;

/**
 * Settings for the Time Modifier WebUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @author Tobias Kampmann TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ModifyTimeZoneNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Modification", description = "Defines the action to be performed on the selected columns.")
    @ValueSwitchWidget
    @Persist(configKey = "modify_select", customPersistor = BehaviourTypePersistor.class)
    @ValueReference(BehaviourTypeRef.class)
    BehaviourType m_behaviourType = BehaviourType.SET;

    @Widget(title = "Time zone", description = "A timezone to be used when saving the date.")
    @Persist(configKey = "time_zone_select")
    @Effect(predicate = BehaviourTypeIsRemove.class, type = EffectType.HIDE)
    ZoneId m_timeZone = ZoneId.systemDefault();

    @Widget(title = "Date&time columns", description = "Only the included columns will be modified.")
    @Persist(configKey = "col_select", customPersistor = LegacyColumnFilterPersistor.class)
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @Persist(customPersistor = ReplaceOrAppend.Persistor.class)
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.APPEND;

    @Widget(title = "Output column suffix",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name separated by a space.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    String m_outputColumnSuffix = " (modified time zone)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */
    enum BehaviourType implements CompatibleDataValueClassesSupplier {
            @Label(value = "Set", //
                description = "Changes the timezone of a date-time column, leaving the nominal time unchanged.")
            SET("Set time zone", List.of(LocalDateTimeValue.class, ZonedDateTimeValue.class)), //
            @Label(value = "Shift",
                description = "Changes the timezone of a date-time column, changing the nominal time "
                    + "so it refers to the same instant. The date and time may change.")
            SHIFT("Shift time zone", List.of(ZonedDateTimeValue.class)), //
            @Label(value = "Remove", description = "Removes timezone information from date-time columns.")
            REMOVE("Remove time zone", List.of(ZonedDateTimeValue.class));

        private String m_oldConfigValue;

        private List<Class<? extends DataValue>> m_compatibleDataValues;

        BehaviourType(final String oldConfigValue, final List<Class<? extends DataValue>> compatibleDataValues) {
            this.m_oldConfigValue = oldConfigValue;
            this.m_compatibleDataValues = compatibleDataValues;
        }

        static BehaviourType getByOldConfigValue(final String oldValue) throws InvalidSettingsException {
            return Arrays.stream(values()) //
                .filter(v -> v.m_oldConfigValue.equals(oldValue)) //
                .findFirst() //
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("Invalid value '%s'. Possible values: %s", oldValue, getOldConfigValues())));
        }

        static String[] getOldConfigValues() {
            return Arrays.stream(values()).map(v -> v.m_oldConfigValue).toArray(String[]::new);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<Class<? extends DataValue>> getCompatibleDataValueClasses() {
            return m_compatibleDataValues;
        }
    }

    /*
     * ------------------------------------------------------------------------
     * PERSISTORS
     * ------------------------------------------------------------------------
     */

    static final class BehaviourTypePersistor extends NodeSettingsPersistorWithConfigKey<BehaviourType> {

        @Override
        public BehaviourType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return BehaviourType.getByOldConfigValue(settings.getString(getConfigKey()));
        }

        @Override
        public void save(final BehaviourType obj, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), obj.m_oldConfigValue);
        }

    }

    /*
     * ------------------------------------------------------------------------
     * PREDICATE PROVIDERS AMD REFERENCES
     * ------------------------------------------------------------------------
     */
    interface BehaviourTypeRef extends Reference<BehaviourType> {
    }

    static final class BehaviourTypeIsRemove implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(BehaviourTypeRef.class).isOneOf(BehaviourType.REMOVE);
        }
    }

    /*
     * ------------------------------------------------------------------------
     * STATE PROVIDERS
     * ------------------------------------------------------------------------
     */
    static final class ColumnProvider extends CompatibleColumnChoicesStateProvider<BehaviourType> {

        @Override
        protected Class<? extends Reference<BehaviourType>> getReferenceClass() {
            return BehaviourTypeRef.class;
        }
    }

}
