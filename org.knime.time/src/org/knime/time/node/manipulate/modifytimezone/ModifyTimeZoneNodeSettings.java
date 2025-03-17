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
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleColumnChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.CompatibleDataValueClassesSupplier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
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
final class ModifyTimeZoneNodeSettings implements DefaultNodeSettings {

    ModifyTimeZoneNodeSettings() {
    }

    ModifyTimeZoneNodeSettings(final DefaultNodeSettingsContext context) {
        var spec = context.getDataTableSpec(0);

        if (spec.isPresent()) {
            m_columnFilter = new ColumnFilter(spec.get().stream() //
                .filter(m_behaviourType::isCompatibleType) //
                .map(DataColumnSpec::getName) //
                .toArray(String[]::new) //
            );
        }
    }

    @Widget(title = "Modification", description = "Defines the action to be performed on the selected columns.")
    @ValueSwitchWidget
    @ValueReference(BehaviourTypeRef.class)
    BehaviourType m_behaviourType = BehaviourType.SET;

    @Widget(title = "Time zone", description = "The time zone to use when modifying the date&amp;time.")
    @Effect(predicate = BehaviourTypeIsRemove.class, type = EffectType.HIDE)
    ZoneId m_timeZone = ZoneId.systemDefault();

    @Widget(title = "Date&time columns", description = "The date&amp;time columns whose values to modify.")
    @ChoicesProvider(ColumnProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Output columns", description = """
            Depending on this setting, the output columns will either replace the modified columns, or be \
            appended to the table with a suffix.
            """)
    @ValueSwitchWidget
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix to append to the column names of the new columns.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    String m_outputColumnSuffix = " (Modified time zone)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS
     * ------------------------------------------------------------------------
     */
    enum BehaviourType implements CompatibleDataValueClassesSupplier {
            @Label(value = "Set", description = """
                    Changes the time zone of a date&amp;time column, leaving the wall time \
                    unchanged.
                    """)
            SET(List.of(LocalDateTimeValue.class, ZonedDateTimeValue.class)), //
            @Label(value = "Shift", description = """
                    Changes the time zone of a date&amp;time column, changing the wall time \
                    so it refers to the same instant. Both, the nominal date and time may \
                    change.
                    """)
            SHIFT(List.of(ZonedDateTimeValue.class)), //
            @Label(value = "Remove", description = "Removes time zone information from zoned date&amp;time columns.")
            REMOVE(List.of(ZonedDateTimeValue.class));

        private final Collection<Class<? extends DataValue>> m_compatibleDataValues;

        BehaviourType(final Collection<Class<? extends DataValue>> compatibleDataValues) {
            this.m_compatibleDataValues = compatibleDataValues;
        }

        @Override
        public Collection<Class<? extends DataValue>> getCompatibleDataValueClasses() {
            return m_compatibleDataValues;
        }

        boolean isCompatibleType(final DataType type) {
            return m_compatibleDataValues.stream().anyMatch(type::isCompatible);
        }

        boolean isCompatibleType(final DataColumnSpec spec) {
            return isCompatibleType(spec.getType());
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
