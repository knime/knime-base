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
package org.knime.time.node.manipulate.modifydate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Settings for the Time Modifier WebUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ModifyDateNodeSettings implements DefaultNodeSettings {

    ModifyDateNodeSettings() {
    }

    ModifyDateNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(spec.get().stream() //
                .filter(cspec -> m_behaviourType.isCompatibleType(cspec.getType())) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "Modification",
        description = "Defines the action to be performed on the selected columns. "
            + "The date can be changed, appended, or removed, with different columns being eligible for each action. "
            + "For example, only local time columns can have date appended, while local date-time columns can only "
            + "be modified or have their date component removed.")
    @ValueSwitchWidget
    @Persist(configKey = "modify_select", customPersistor = BehaviourTypePersistor.class)
    @ValueReference(BehaviourTypeRef.class)
    BehaviourType m_behaviourType = BehaviourType.CHANGE;

    @Widget(title = "Date", description = "A date value like YYYY-mm-dd.")
    @Persist(configKey = "date")
    @Effect(predicate = BehaviourTypeIsRemove.class, type = EffectType.HIDE)
    LocalDate m_localDate = LocalDate.now();

    @Widget(title = "Time zone", description = "A timezone to be used when saving the date.")
    @Persist(configKey = "time_zone")
    @ChoicesWidget(optional = true)
    @Effect(predicate = BehaviourTypeIsAppend.class, type = EffectType.SHOW)
    @JsonInclude(Include.ALWAYS)
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
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name separated by a space.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    String m_outputColumnSuffix = " (modified date)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */

    enum BehaviourType implements CompatibleDataValueClassesSupplier {
            @Label(value = "Change", //
                description = "Changes the date of a date-time column, leaving the timezone unchanged.")
            CHANGE("Change date", List.of(ZonedDateTimeValue.class, LocalDateTimeValue.class)), //
            @Label(value = "Append", description = "Appends a date to a time column, resulting in a date-time column. "
                + "A time zone can optionally be set.")
            APPEND("Append date", List.of(LocalTimeValue.class)), //
            @Label(value = "Remove", description = "Removes the date and timezone information from date-time columns, "
                + "leaving only the time.")
            REMOVE("Remove date", List.of(ZonedDateTimeValue.class, LocalDateTimeValue.class));

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

        @Override
        public Collection<Class<? extends DataValue>> getCompatibleDataValueClasses() {
            return m_compatibleDataValues;
        }

        boolean isCompatibleType(final DataType type) {
            return m_compatibleDataValues.stream().anyMatch(type::isCompatible);
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

    static final class BehaviourTypeIsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(BehaviourTypeRef.class).isOneOf(BehaviourType.APPEND);
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
