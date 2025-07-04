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
 *   22 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.missingvaluecolfilter;

import org.knime.base.node.util.LegacyColumnFilterMigration;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TwinlistWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MaxValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Settings for Missing Value Column Filter node
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class MissingValueColumnFilterNodeSettings implements DefaultNodeSettings {

    static final class ColumnFilterMigration extends LegacyColumnFilterMigration {
        ColumnFilterMigration() {
            super("column-filter");
        }
    }

    @Migration(ColumnFilterMigration.class)
    @Widget(title = "Input columns", description = "Select the columns to test for missing values.")
    @ChoicesProvider(AllColumnsProvider.class)
    @TwinlistWidget(excludedLabel = "Retained columns", includedLabel = "Columns to test")
    ColumnFilter m_columnFilter;

    enum RemovalCriterion {
            @Label(value = "With at least one missing value",
                description = "Remove the column if it contains at least one missing value.")
            SOME, //
            @Label(value = "With only missing values",
                description = "Remove the column if it contains only missing values.")
            ONLY, //
            @Label(value = "By percentage of missing values",
                description = "Remove the column if it contains at least the configured percentage of missing values.")
            PERCENTAGE, //
            @Label(value = "By number of missing values",
                description = "Remove the column if it contains at least the configured number of missing values.")
            NUMBER
    }

    interface RemovalCriterionRef extends Reference<RemovalCriterion> {
    }

    static final class ByPercentage implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RemovalCriterionRef.class).isOneOf(RemovalCriterion.PERCENTAGE);
        }
    }

    static final class ByNumber implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RemovalCriterionRef.class).isOneOf(RemovalCriterion.NUMBER);
        }
    }

    private static final class RemovalCriterionDefaultProvider implements DefaultProvider<RemovalCriterion> {
        @Override
        public RemovalCriterion getDefault() {
            // Swing-based node did only have percentage setting,
            // so if the key for the mode is missing, it must be an old instance
            return RemovalCriterion.PERCENTAGE;
        }
    }

    @RadioButtonsWidget
    @Widget(title = "Remove columns", description = "Specify the threshold for the removal of selected columns.")
    @ValueReference(RemovalCriterionRef.class)
    @Migration(RemovalCriterionDefaultProvider.class) // added during webui transition
    RemovalCriterion m_removeColumnsBy = RemovalCriterion.ONLY;

    private static final class PercentageMaxValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 100;
        }
    }

    // "more-than-or-equal" is legacy behavior
    @Widget(title = "Threshold percentage (equal or more than %)", //
        description = "Selected columns with at least this percentage of missing values are filtered out.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = PercentageMaxValidation.class)
    @Persist(configKey = "missing_value_percentage")
    @Effect(type = EffectType.SHOW, predicate = ByPercentage.class)
    double m_percentage = 100.0;

    @Widget(title = "Threshold number (equal or more than)", //
        description = "Selected columns with at least this number of missing values are filtered out.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Effect(type = EffectType.SHOW, predicate = ByNumber.class)
    @Migrate(loadDefaultIfAbsent = true)
    long m_number = 1;

    MissingValueColumnFilterNodeSettings() {
        this((DataTableSpec)null);
    }

    MissingValueColumnFilterNodeSettings(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpec(0).orElse(null));
    }

    MissingValueColumnFilterNodeSettings(final DataTableSpec spec) {
        m_columnFilter =
            new ColumnFilter(spec == null ? new String[0] : spec.getColumnNames()).withExcludeUnknownColumns();
    }
}
