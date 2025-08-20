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
package org.knime.base.node.preproc.colautotypecast;

import static org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil.getStringColumnsOfFirstPort;

import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Settings for the Column Auto Type Cast node.
 *
 * Replaces the legacy dialog by exposing the same parameters using the modern UI framework.
 *
 * - Column filter (string columns only), persisted under "column-filter".
 * - Date format (config key: "dateFormat").
 * - Missing value pattern (config key: "missingValuePattern").
 * - Quickscan toggle (config key: "doAQuickScan") and number of rows (config key: "numberOfRowsForQuickScan").
 * - Use legacy type names (config key: "useLegacyTypeNames").
 *
 * @since 5.x
 */
@SuppressWarnings("restriction")
final class ColumnAutoTypeCasterNodeSettings implements NodeParameters {

    ColumnAutoTypeCasterNodeSettings(final NodeParametersInput context) {
        // Default the column filter to available string columns and include unknown columns by default
        m_columnFilter = new ColumnFilter(getStringColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    /** Constructor for persistence. */
    ColumnAutoTypeCasterNodeSettings() {
    }

    // Sections
    @Section(title = "Column selection")
    interface ColumnsSection {
    }

    @Section(title = "Options")
    @After(ColumnsSection.class)
    interface OptionsSection {
    }

    // Column filter with legacy persistor for DataColumnSpecFilterConfiguration (key: "column-filter")
    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super("column-filter");
        }
    }

    @Persistor(ColumnFilterPersistor.class)
    @Widget(title = "Column filter",
        description = "Select the string columns to consider for automatic type casting. "
            + "Only columns compatible with String are offered. The filter supports manual selection and wildcard/regex.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Layout(ColumnsSection.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    // Options

    @Persist(configKey = ColumnAutoTypeCasterNodeModel.CFGKEY_DATEFORMAT)
    @Widget(title = "Choose a date format",
        description = "Choose or enter a date pattern used to detect dates in the selected columns. "
            + "Examples: 'dd.MM.yy', 'dd.MM.yy HH:mm:ss', 'dd.MM.yy HH:mm:ss:SSS', 'HH:mm:ss'. "
            + "Pattern symbols: y=Year, M=Month, d=Day, H=Hour, m=Minute, s=Second, S=Millisecond.")
    @Layout(OptionsSection.class)
    String m_dateFormat = "dd.MM.yy";

    @Persist(configKey = ColumnAutoTypeCasterNodeModel.CFGKEY_MISSVALPAT)
    @Widget(title = "Missing value pattern",
        description = "Enter a missing value pattern applied to all included columns. "
            + "Use '<none>' for no pattern (default) or '<empty>' for the empty string. Any other string will be treated as the pattern.")
    @Layout(OptionsSection.class)
    String m_missingValuePattern = ColumnAutoTypeCasterNodeModel.MISSVALDESC_NONE; // "<none>"

    interface QuickScanRef extends ParameterReference<Boolean> {
    }

    static final class QuickScanIsEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(QuickScanRef.class).isTrue();
        }
    }

    @Persist(configKey = ColumnAutoTypeCasterNodeModel.CFGKEY_QUICKSANBOOLEAN)
    @Widget(title = "Quickscan",
        description = "Speed up by determining the most specific type based only on the first N rows. "
            + "Note: With quickscan enabled this node may fail during execution if later rows contradict the inferred type.")
    @ValueSwitchWidget
    @ValueReference(QuickScanRef.class)
    @Layout(OptionsSection.class)
    boolean m_quickScan;

    @Persist(configKey = ColumnAutoTypeCasterNodeModel.CFGKEY_QUICKSCANROWS)
    @Widget(title = "Number of rows to consider",
        description = "Number of initial rows used when quickscan is enabled. Minimum is 1. Default is 1000.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Effect(predicate = QuickScanIsEnabled.class, type = EffectType.SHOW)
    @Layout(OptionsSection.class)
    int m_quickScanRows = 1000;

    @Persist(configKey = ColumnAutoTypeCasterNodeModel.CFGKEY_USELEGACYTYPENAMES)
    @Widget(title = "Use legacy type names instead of identifiers",
        description = "Output legacy type names like 'Number (double)' on the second port instead of identifiers "
            + "like 'org.knime.core.data.def.DoubleCell'. This resembles the old behavior but is discouraged as "
            + "type names may change in future versions.")
    @ValueSwitchWidget
    @Layout(OptionsSection.class)
    boolean m_useLegacyTypeNames = true;
}
