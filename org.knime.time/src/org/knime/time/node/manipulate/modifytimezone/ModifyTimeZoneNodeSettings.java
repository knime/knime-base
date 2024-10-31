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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.time.util.AppendOrReplace;
/**
 * Settings for the Time Modifier WebUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ModifyTimeZoneNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Modify type", description = "Defines the action to be performed on the selected columns.")
    @ValueSwitchWidget
    @Persist(customPersistor = BehaviourTypePersistor.class)
    @ValueReference(BehaviourTypeRef.class)
    BehaviourType m_behaviourType = BehaviourType.SET;

    @Widget(title = "Time zone", description = "A timezone to be used when saving the date.")
    @Persist(configKey = "time_zone_select")
    @Effect(predicate = BehaviourTypeIsRemove.class, type = EffectType.HIDE)
    ZoneId m_timeZone = ZoneId.of("Europe/Berlin");

    @Widget(title = "Date & time columns", description = "Only the included columns will be modified.")
    @Persist(configKey = "col_select", customPersistor = LegacyColumnFilterPersistor.class, optional = true)
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @Persist(customPersistor = AppendOrReplace.Persistor.class)
    @ValueReference(AppendOrReplace.ValueRef.class)
    AppendOrReplace m_appendOrReplace = AppendOrReplace.APPEND;

    @Widget(title = "Suffix of appended column",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name separated by a space.")
    @Effect(predicate = AppendOrReplace.IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    String m_outputColumnSuffix = "(modified time zone)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */
    enum BehaviourType {
            @Label(value = "Set", //
                description = "Changes the timezone of a date-time column, leaving the nominal time unchanged.")
            SET("Set time zone"), //
            @Label(value = "Shift",
                description = "Changes the timezone of a date-time column, changing the nominal time "
                    + "so it refers to the same instant. The date and time may change.")
            SHIFT("Shift time zone"), //
            @Label(value = "Remove", description = "Removes timezone information from date-time columns.")
            REMOVE("Remove time zone");

        private String m_oldConfigValue;

        BehaviourType(final String oldConfigValue) {
            this.m_oldConfigValue = oldConfigValue;
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
    }

    /*
     * ------------------------------------------------------------------------
     * PERSISTORS
     * ------------------------------------------------------------------------
     */

    static final class BehaviourTypePersistor implements FieldNodeSettingsPersistor<BehaviourType> {

        private static final String CONFIG_KEY = "modify_select";

        @Override
        public BehaviourType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return BehaviourType.getByOldConfigValue(settings.getString(CONFIG_KEY));
        }

        @Override
        public void save(final BehaviourType obj, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, obj.m_oldConfigValue);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{CONFIG_KEY};
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

    static final class BehaviourTypeIsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(BehaviourTypeRef.class).isOneOf(BehaviourType.SHIFT);
        }
    }



    /*
     * ------------------------------------------------------------------------
     * OTHER UTILITIES
     * ------------------------------------------------------------------------
     */
    static final class ColumnProvider implements ColumnChoicesStateProvider {

        private Supplier<BehaviourType> m_modifySelect;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesStateProvider.super.init(initializer);

            this.m_modifySelect = initializer.computeFromValueSupplier(BehaviourTypeRef.class);
        }

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            final Collection<Class<? extends DataValue>> allowedTypes = m_modifySelect.get() == BehaviourType.SET //
                ? List.of(LocalDateTimeValue.class, ZonedDateTimeValue.class) //
                : List.of(ZonedDateTimeValue.class);

            return context.getDataTableSpec(0).map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(columnSpec -> allowedTypes.stream().anyMatch(columnSpec.getType()::isCompatible)) //
                .toArray(DataColumnSpec[]::new);
        }
    }

}
