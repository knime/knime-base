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
 *   Nov 21, 2024 (kampmann): created
 */
package org.knime.time.node.manipulate.datetimeround;

import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
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
 * Settings for the Date&Time Round WebUI node.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
class DateTimeRoundNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Rounding mode", description = """
            Select the rounding mode to round either round in a date-based or a time-based manner.
            """)
    @ValueSwitchWidget
    @ValueReference(RoundingModeRef.class)
    RoundingMode m_roundingMode = RoundingMode.DATE;

    @Effect(predicate = RoundingModeIsDate.class, type = EffectType.SHOW)
    DateRoundNodeSettings m_dateRoundSettings = new DateRoundNodeSettings();

    @Effect(predicate = RoundingModeIsTime.class, type = EffectType.SHOW)
    TimeRoundNodeSettings m_timeRoundSettings = new TimeRoundNodeSettings();

    @Widget(title = "Output columns",
        description = "Depending on the selection, the selected columns will be replaced "
            + "or appended to the input table.")
    @ValueSwitchWidget
    @Persist(customPersistor = ReplaceOrAppend.Persistor.class)
    @ValueReference(ReplaceOrAppend.ValueRef.class)
    @Layout(DateTimeRoundNodeLayout.Bottom.class)
    ReplaceOrAppend m_appendOrReplace = ReplaceOrAppend.REPLACE;

    @Widget(title = "Output column suffix",
        description = "The suffix that is appended to the column name. "
            + "The suffix will be added to the original column name.")
    @Effect(predicate = ReplaceOrAppend.IsAppend.class, type = EffectType.SHOW)
    @Persist(configKey = "suffix")
    @Layout(DateTimeRoundNodeLayout.Bottom.class)
    String m_outputColumnSuffix = "(rounded)";

    /*
     * ------------------------------------------------------------------------
     * ENUMS, PREDICATE PROVIDERS AMD REFERENCES
     * ------------------------------------------------------------------------
     */

    enum RoundingMode implements CompatibleDataValueClassesSupplier {
            @Label(value = "Date", description = "Date-based rounding.")
            DATE(List.of(LocalDateValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class)), //
            @Label(value = "Time", description = "Time-based rounding.")
            TIME(List.of(LocalTimeValue.class, ZonedDateTimeValue.class, LocalDateTimeValue.class)); //

        private final Collection<Class<? extends DataValue>> m_compatibleDataValues;

        RoundingMode(final Collection<Class<? extends DataValue>> compatibleDataValues) {
            m_compatibleDataValues = compatibleDataValues;
        }

        @Override
        public Collection<Class<? extends DataValue>> getCompatibleDataValueClasses() {
            return m_compatibleDataValues;
        }
    }

    interface RoundingModeRef extends Reference<RoundingMode> {
    }

    static final class RoundingModeIsDate implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RoundingModeRef.class).isOneOf(RoundingMode.DATE);
        }
    }

    static final class RoundingModeIsTime implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RoundingModeRef.class).isOneOf(RoundingMode.TIME);
        }
    }

}
