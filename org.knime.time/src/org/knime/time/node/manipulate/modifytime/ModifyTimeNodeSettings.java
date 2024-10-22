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
 *   Oct 21, 2024 (kampmann): created
 */
package org.knime.time.node.manipulate.modifytime;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Settings for the Time Modifier WebUI node.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
class ModifyTimeNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Time setting",
        description = "Defines the action to be performed on the selected columns. "
            + "The time can be changed, appended, or removed, with different columns being eligible for each action. "
            + "For example, only local date columns can have time appended, while local date-time columns can only "
            + "be modified or have their time component removed.")
    @ValueSwitchWidget
    @Persist(customPersistor = ModifySelectPersistor.class)
    @ValueReference(ModifySelectRef.class)
    ModifySelect m_modifySelect = ModifySelect.CHANGE;

    @Widget(title = "Time",
        description = "A time value in form of HH:mm:ss.SSS.")
    @Effect(predicate = ModifySelectIsRemove.class, type = EffectType.HIDE)
    @Persist(configKey = "time")
    LocalTime m_localTime = LocalTime.of(0,0,0);

    @Widget(title = "Time zone", description = "If checked, the user can choose a time zone to append also.")
    @Persist(configKey = "time_zone")
    @ChoicesWidget(choicesProvider = ZoneIdProvider.class, optional = true)
    @Effect(predicate = ModifySelectIsAppend.class, type = EffectType.SHOW)
    @JsonInclude(Include.ALWAYS)
    ZoneId m_timeZone = ZoneId.of("Europe/Berlin");

    @Widget(title = "Date & time columns", description = "Only the included columns will be modified.")
    @Persist(configKey = "col_select", customPersistor = LegacyColumnFilterPersistor.class, optional = true)
    @ChoicesWidget(choicesProvider = ColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @Persist(configKey = "replace_or_append", customPersistor = AppendOrReplacePersistor.class)
    @ValueReference(AppendOrReplaceRef.class)
    AppendOrReplace m_appendOrReplace = AppendOrReplace.APPEND;

    @Widget(title = "Suffix of appended column",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name separated by a space.")
    @Effect(predicate = OutputColumnsIsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    String m_outputColumnSuffix = "(modified time)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */
    enum ModifySelect {
            @Label(value = "Change", //
                description = "Changes the time of local or zoned date&amp;time columns. The time zone "
                    + "will not be changed. The <i>Modify Time Zone</i> node can be used to change it.")
            CHANGE("Change time"), //
            @Label(value = "Append",
                description = "Appends a time to local date columns. Optionally a time zone can be appended too.")
            APPEND("Append time"), //
            @Label(value = "Remove", description = "Removes the time from local or zoned date&amp;time columns. "
                + "Time zones will be removed, too.")
            REMOVE("Remove time");

        private String m_oldConfigValue;

        ModifySelect(final String oldConfigValue) {
            this.m_oldConfigValue = oldConfigValue;
        }

        static ModifySelect getByOldConfigValue(final String oldValue) throws InvalidSettingsException {
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

    enum AppendOrReplace {
            @Label(value = "Replace", description = "The selected columns will be replaced by the new columns.")
            REPLACE("Replace selected columns"), //
            @Label(value = "Append with Suffix", //
                description = "The selected columns will be appended to the input table. "
                    + "The suffix of the appended columns can be provided in the text field to the right.")
            APPEND("Append selected columns"); //

        private final String m_oldConfigValue;

        AppendOrReplace(final String oldConfigValue) {
            this.m_oldConfigValue = oldConfigValue;
        }

        static AppendOrReplace getByOldConfigValue(final String oldValue) throws InvalidSettingsException {
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

    static final class AppendOrReplacePersistor extends NodeSettingsPersistorWithConfigKey<AppendOrReplace> {

        @Override
        public AppendOrReplace load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return AppendOrReplace.getByOldConfigValue(settings.getString(getConfigKey()));
        }

        @Override
        public void save(final AppendOrReplace obj, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), obj.m_oldConfigValue);
        }

    }

    static final class ModifySelectPersistor implements FieldNodeSettingsPersistor<ModifySelect> {

        private static final String CONFIG_KEY = "modify_select";

        @Override
        public ModifySelect load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ModifySelect.getByOldConfigValue(settings.getString(CONFIG_KEY));
        }

        @Override
        public void save(final ModifySelect obj, final NodeSettingsWO settings) {
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
    interface ModifySelectRef extends Reference<ModifySelect> {
    }

    static final class ModifySelectIsRemove implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ModifySelectRef.class).isOneOf(ModifySelect.REMOVE);
        }
    }

    static final class ModifySelectIsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ModifySelectRef.class).isOneOf(ModifySelect.APPEND);
        }
    }

    interface AppendOrReplaceRef extends Reference<AppendOrReplace> {
    }

    static final class OutputColumnsIsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(AppendOrReplaceRef.class).isOneOf(AppendOrReplace.APPEND);
        }
    }


    /*
     * ------------------------------------------------------------------------
     * STATE PROVIDERS
     * ------------------------------------------------------------------------
     */
    static final class ColumnProvider implements ColumnChoicesStateProvider {

        private Supplier<ModifySelect> m_modifySelect;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesStateProvider.super.init(initializer);

            this.m_modifySelect = initializer.computeFromValueSupplier(ModifySelectRef.class);
        }

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            final Collection<Class<? extends DataValue>> allowedTypes = m_modifySelect.get() == ModifySelect.APPEND //
                ? List.of(LocalDateValue.class) //
                : List.of(ZonedDateTimeValue.class, LocalDateTimeValue.class);

            return context.getDataTableSpec(0).map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(columnSpec -> allowedTypes.stream().anyMatch(columnSpec.getType()::isCompatible)) //
                .toArray(DataColumnSpec[]::new);
        }
    }

    static final class ZoneIdProvider implements StringChoicesStateProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            StringChoicesStateProvider.super.init(initializer);
        }

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return ZoneId.getAvailableZoneIds().stream().sorted().toArray(String[]::new);
        }
    }
}
