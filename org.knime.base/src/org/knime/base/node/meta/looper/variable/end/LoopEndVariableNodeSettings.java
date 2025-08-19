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
 *   Aug 19, 2025 (AI Migration): created
 */
package org.knime.base.node.meta.looper.variable.end;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory.ConvertibleFlowVariablesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilter;
import org.knime.node.parameters.widget.choices.filter.FlowVariableFilterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.withtypes.TypedStringFilterMode;

/**
 * Settings for the "Variable Loop End" node (modern Web UI dialog).
 * <p>
 * The node collects (per loop iteration) the selected flow variables and outputs a table containing one row per
 * iteration with a column for each chosen variable. Optionally, modified variables inside the loop body can be
 * propagated back to the flow variable stack after loop termination.
 * </p>
 *
 * @author AI (migration)
 * @since 5.7
 */
@SuppressWarnings("restriction")
final class LoopEndVariableNodeSettings implements NodeParameters {

    /** Custom persistor writing/reading the legacy NameFilterConfiguration structure under key 'variable_filter'. */
    static final class FilterPersistor implements NodeParametersPersistor<FlowVariableFilter> {

        private static final String KEY = "variable_filter";

        @Override
        public FlowVariableFilter load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var filter = new FlowVariableFilter();
            if (!settings.containsKey(KEY)) { // nothing persisted yet
                return filter;
            }
            final var cfg = settings.getNodeSettings(KEY);
            // Included names
            String[] included = readStringArray(cfg, "included_names");
            String[] excluded = readStringArray(cfg, "excluded_names");
            // enforce option
            boolean includeUnknown = "EnforceInclusion".equals(cfg.getString("enforce_option", "EnforceExclusion"));
            filter.m_manualFilter.m_manuallySelected = included;
            filter.m_manualFilter.m_manuallyDeselected = excluded;
            filter.m_manualFilter.m_includeUnknownColumns = includeUnknown;
            // pattern settings
            if (cfg.containsKey("name_pattern")) {
                final var p = cfg.getNodeSettings("name_pattern");
                filter.m_patternFilter.m_pattern = p.getString("pattern", "");
                filter.m_patternFilter.m_isCaseSensitive = p.getBoolean("caseSensitive", false);
                filter.m_patternFilter.m_isInverted = p.getBoolean("excludeMatching", false);
                final var type = p.getString("type", "Wildcard");
                switch (type) {
                case "Regex":
                case "REGEX":
                    filter.m_mode = TypedStringFilterMode.REGEX; break;
                case "Wildcard":
                case "WILDCARD":
                    filter.m_mode = TypedStringFilterMode.WILDCARD; break;
                default:
                    filter.m_mode = TypedStringFilterMode.MANUAL;
                }
            }
            return filter;
        }

        private static String[] readStringArray(final NodeSettingsRO parent, final String key)
                throws InvalidSettingsException {
            if (!parent.containsKey(key)) { return new String[0]; }
            final var c = parent.getNodeSettings(key);
            final int size = c.getInt("array-size", 0);
            final String[] result = new String[size];
            for (int i = 0; i < size; i++) {
                final String entryKey = Integer.toString(i);
                result[i] = c.containsKey(entryKey) ? c.getString(entryKey) : "";
            }
            return result;
        }

        @Override
        public void save(final FlowVariableFilter obj, final NodeSettingsWO settings) {
            final var cfg = settings.addConfig(KEY);
            cfg.addString("filter-type", "STANDARD");
            // manual lists
            writeStringArray(cfg.addConfig("included_names"), obj.m_manualFilter.m_manuallySelected);
            writeStringArray(cfg.addConfig("excluded_names"), obj.m_manualFilter.m_manuallyDeselected);
            cfg.addString("enforce_option", obj.m_manualFilter.m_includeUnknownColumns ? "EnforceInclusion" : "EnforceExclusion");
            // pattern
            final var p = cfg.addConfig("name_pattern");
            p.addString("pattern", obj.m_patternFilter.m_pattern == null ? "" : obj.m_patternFilter.m_pattern);
            final String type = switch (obj.m_mode) {
                case REGEX -> "Regex";
                case WILDCARD -> "Wildcard";
                default -> "Wildcard"; // legacy default
            };
            p.addString("type", type);
            p.addBoolean("caseSensitive", obj.m_patternFilter.m_isCaseSensitive);
            p.addBoolean("excludeMatching", obj.m_patternFilter.m_isInverted);
        }

        private static void writeStringArray(final NodeSettingsWO cfg, final String[] values) {
            final String[] arr = values == null ? new String[0] : values;
            cfg.addInt("array-size", arr.length);
            for (int i = 0; i < arr.length; i++) {
                cfg.addString(Integer.toString(i), arr[i]);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] { {KEY, "filter-type"}, {KEY, "included_names"}, {KEY, "excluded_names"},
                {KEY, "enforce_option"}, {KEY, "name_pattern", "pattern"}, {KEY, "name_pattern", "type"},
                {KEY, "name_pattern", "caseSensitive"}, {KEY, "name_pattern", "excludeMatching"} };
        }
    }

    /**
     * Filter specifying which flow variables are converted into columns of the result table. The include list defines
     * the variables to output; excluded variables are ignored. Pattern, type, and manual selection modes are supported.
     */
    @Widget(title = "Variable selection", description = "Select the flow variables that should be collected at the end "
        + "of each loop iteration. Included variables become columns in the output table; excluded variables are ignored. "
        + "You can switch between manual selection, name pattern based selection, or filtering by variable type. New "
        + "variables encountered during execution are handled according to the chosen mode.")
    @FlowVariableFilterWidget(choicesProvider = ConvertibleFlowVariablesProvider.class)
    @Persistor(FilterPersistor.class)
    FlowVariableFilter m_filter = new FlowVariableFilter();

    /** Persistor for the propagate-loop-variables boolean using a SettingsModel to stay backwards compatible. */
    static final class PropagateLoopVariablesPersistor extends SettingsModelBooleanPersistor {
        PropagateLoopVariablesPersistor() { super("propagateLoopVariables"); }
    }

    /**
     * Whether modified loop variables (whose values changed inside the loop body) should be written back onto the flow
     * variable stack after the loop finishes. Disable to keep outer-scope values unchanged.
     */
    @Widget(title = "Propagate modified loop variables", description = "If enabled, any flow variable whose value was "
        + "changed inside the loop is propagated (its final value after the last iteration). Disable to leave the "
        + "values from before entering the loop untouched.")
    @Persistor(PropagateLoopVariablesPersistor.class)
    boolean m_propagateLoopVariables = false; // default matches old SettingsModelBoolean default
}
