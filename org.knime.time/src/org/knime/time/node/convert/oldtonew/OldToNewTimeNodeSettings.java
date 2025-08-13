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
package org.knime.time.node.convert.oldtonew;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;
import org.knime.time.util.DateTimeType;

/**
 * Modern WebUI settings (replacement for {@code OldToNewTimeNodeDialog}).
 * <p>
 * All legacy configuration keys are preserved so that {@link OldToNewTimeNodeModel} continues to work unchanged:
 * <ul>
 * <li>"col_select" (column filter)</li>
 * <li>"replace_or_append" (append / replace choice)</li>
 * <li>"suffix" (suffix for appended columns)</li>
 * <li>"type_bool" (automatic type detection)</li>
 * <li>"zone_bool" (add time zone if possible when auto-detecting)</li>
 * <li>"time_zone_select" (chosen time zone)</li>
 * <li>"newTypeEnum" (manually selected new type enum constant)</li>
 * </ul>
 */
@SuppressWarnings("restriction")
final class OldToNewTimeNodeSettings implements NodeParameters {

    /* ============================= Sections ============================= */

    @Section(title = "Column Selection",
        description = "Select legacy Date&Time columns to convert. Only included columns are processed.")
    interface ColumnSelectionSection { }

    @Section(title = "Replace / Append Selection",
        description = "Choose whether converted columns should replace the original ones or be appended with a suffix.")
    interface ReplaceAppendSection { }

    @Section(title = "New Type Selection",
        description = "Configure how the new Date&Time type is determined: either automatically from the first non-missing row or manually by selecting the desired target type.")
    interface NewTypeSection { }

    @Section(title = "Time Zone Selection",
        description = "Control time zone enrichment. When automatic type detection is enabled you may request adding a time zone if the legacy value contains both date and time. When selecting types manually a time zone is required only for ZonedDateTime; in that case choose the zone below.")
    interface TimeZoneSection { }

    /* ============================= Column Selection ============================= */

    @Widget(title = "Columns to convert", description = "Move columns between the lists to specify which legacy Date&Time columns (old type) should be converted to the new KNIME Date&Time types. Only columns compatible with the legacy Date&Time data type are shown.")
    @Layout(ColumnSelectionSection.class)
    @ChoicesProvider(DateTimeColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Included columns", excludedLabel = "Available columns")
    @Persistor(ColSelectPersistor.class)
    ColumnFilter m_columns = new ColumnFilter();

    /* ============================= Replace / Append ============================= */

    /** Replace or append behaviour (enum for UI; persisted as original legacy strings). */
    enum ReplaceAppendMode {
        @Label("Append selected columns") APPEND("Append selected columns"),
        @Label("Replace selected columns") REPLACE("Replace selected columns");
        private final String m_legacy;
        ReplaceAppendMode(final String legacy) { m_legacy = legacy; }
        String legacy() { return m_legacy; }
        static ReplaceAppendMode fromLegacy(final String v) {
            return Arrays.stream(values()).filter(e -> e.m_legacy.equals(v)).findFirst().orElse(REPLACE);
        }
    }

    @Widget(title = "Output mode", description = "Select how converted columns should appear in the output: <b>Replace selected columns</b> overwrites the originals, while <b>Append selected columns</b> keeps originals and adds new columns with a configurable suffix.")
    @Layout(ReplaceAppendSection.class)
    @RadioButtonsWidget
    @ValueReference(ReplaceAppendRef.class)
    @Persistor(ReplaceAppendPersistor.class)
    ReplaceAppendMode m_replaceAppend = ReplaceAppendMode.REPLACE;

    @Widget(title = "Suffix of appended columns",
        description = "Suffix appended to each converted column when <b>Append selected columns</b> is chosen. Must yield unique column names. Ignored in replace mode.")
    @Layout(ReplaceAppendSection.class)
    @Persist(configKey = "suffix")
    @Effect(predicate = ShowSuffixPredicate.class, type = EffectType.SHOW)
    String m_suffix = "(new Date&Time)";

    /* ============================= Type Selection ============================= */

    @Widget(title = "Automatic type detection",
        description = "If enabled the new type for each included column is inferred from the first non-missing cell: the legacy value's components (date / time / both) decide whether LocalDate, LocalTime, LocalDateTime or (optionally) ZonedDateTime is chosen.")
    @Layout(NewTypeSection.class)
    @Persist(configKey = "type_bool")
    @ValueReference(AutoTypeRef.class)
    boolean m_autoType = true;

    @Widget(title = "Manual new type",
        description = "Choose the target Date&Time type to which all included columns will be converted when automatic type detection is disabled. Available options: LocalDate, LocalTime, LocalDateTime, ZonedDateTime.")
    @Layout(NewTypeSection.class)
    @Persistor(NewTypeEnumPersistor.class)
    @Effect(predicate = ShowManualTypePredicate.class, type = EffectType.SHOW)
    @ValueReference(ManualTypeRef.class) // added so predicates can reference the manual type
    DateTimeType m_manualType = DateTimeType.LOCAL_DATE_TIME;

    /* ============================= Time Zone Handling ============================= */

    @Widget(title = "Add time zone (auto mode)",
        description = "When automatic type detection is enabled this option attempts to upgrade legacy values containing both date and time components to ZonedDateTime by attaching the selected time zone. If a value contains date only or time only no time zone is added.")
    @Layout(TimeZoneSection.class)
    @Persist(configKey = "zone_bool")
    @ValueReference(AddZoneRef.class)
    @Effect(predicate = ShowZoneBoolPredicate.class, type = EffectType.SHOW)
    boolean m_addZoneIfPossible = false;

    @Widget(title = "Time zone",
        description = "The IANA time zone identifier used either (a) when automatic type detection is enabled and 'Add time zone' is checked, or (b) when manual type selection is active and the chosen type is ZonedDateTime.")
    @Layout(TimeZoneSection.class)
    @ChoicesProvider(TimeZoneChoices.class)
    @Persist(configKey = "time_zone_select")
    @Effect(predicate = ShowTimeZonePredicate.class, type = EffectType.SHOW)
    String m_timeZoneId = ZoneId.systemDefault().getId();

    /* ============================= Constructors ============================= */

    OldToNewTimeNodeSettings() { }

    OldToNewTimeNodeSettings(final NodeParametersInput context) { }

    /* ============================= Predicate & Reference Classes ============================= */

    static final class ReplaceAppendRef implements ParameterReference<ReplaceAppendMode> { }
    static final class AutoTypeRef implements ParameterReference<Boolean> { }
    static final class AddZoneRef implements ParameterReference<Boolean> { }
    static final class ManualTypeRef implements ParameterReference<DateTimeType> { }

    /** Show suffix only in append mode. */
    static final class ShowSuffixPredicate implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ReplaceAppendRef.class).isOneOf(ReplaceAppendMode.APPEND);
        }
    }
    /** Show manual type chooser only if auto detection disabled. */
    static final class ShowManualTypePredicate implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AutoTypeRef.class).isFalse();
        }
    }
    /** Show zone bool only if auto detection enabled. */
    static final class ShowZoneBoolPredicate implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(AutoTypeRef.class).isTrue();
        }
    }
    /** Show time zone selection if (auto && addZone) OR (!auto && manualType == ZONED_DATE_TIME). */
    static final class ShowTimeZonePredicate implements EffectPredicateProvider {
        @Override public EffectPredicate init(final PredicateInitializer i) {
            var auto = i.getBoolean(AutoTypeRef.class).isTrue();
            var addZone = i.getBoolean(AddZoneRef.class).isTrue();
            var manualZoned = i.getEnum(ManualTypeRef.class).isOneOf(DateTimeType.ZONED_DATE_TIME);
            // (auto && addZone) || (!auto && manualType == ZONED_DATE_TIME)
            return or(and(auto, addZone), and(not(auto), manualZoned));
        }
    }

    /* ============================= Choices Providers ============================= */

    /** Provides legacy Date&Time columns. */
    static final class DateTimeColumnsProvider implements FilteredInputTableColumnsProvider {
        @Override public boolean isIncluded(final DataColumnSpec col) { return col.getType().isCompatible(DateAndTimeValue.class); }
        @Override public int getInputTableIndex() { return 0; }
    }

    /** Time zone identifiers. */
    static final class TimeZoneChoices implements StringChoicesProvider { // was ChoicesProvider (annotation) -> implement provider interface
        @Override public java.util.List<String> choices(final org.knime.node.parameters.NodeParametersInput in) {
            return ZoneId.getAvailableZoneIds().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        }
    }

    /* ============================= Persistors ============================= */

    /** Persistor mapping enum ReplaceAppendMode to legacy string key replace_or_append. */
    static final class ReplaceAppendPersistor implements NodeParametersPersistor<ReplaceAppendMode> {
        @Override public void save(final ReplaceAppendMode obj, final org.knime.core.node.NodeSettingsWO settings) { settings.addString("replace_or_append", obj.legacy()); }
        @Override public ReplaceAppendMode load(final org.knime.core.node.NodeSettingsRO settings) throws org.knime.core.node.InvalidSettingsException { return ReplaceAppendMode.fromLegacy(settings.getString("replace_or_append")); }
        @Override public String[][] getConfigPaths() { return new String[][] {{"replace_or_append"}}; }
    }

    /** Persistor for manual type enum under key newTypeEnum. */
    static final class NewTypeEnumPersistor implements NodeParametersPersistor<DateTimeType> {
        @Override public void save(final DateTimeType obj, final org.knime.core.node.NodeSettingsWO settings) { settings.addString("newTypeEnum", obj.name()); }
        @Override public DateTimeType load(final org.knime.core.node.NodeSettingsRO settings) throws org.knime.core.node.InvalidSettingsException { return DateTimeType.valueOf(settings.getString("newTypeEnum", DateTimeType.LOCAL_DATE_TIME.name())); }
        @Override public String[][] getConfigPaths() { return new String[][] {{"newTypeEnum"}}; }
    }

    /** Column filter persistor for legacy key col_select. */
    static final class ColSelectPersistor extends LegacyColumnFilterPersistor { ColSelectPersistor() { super("col_select"); } }
}
