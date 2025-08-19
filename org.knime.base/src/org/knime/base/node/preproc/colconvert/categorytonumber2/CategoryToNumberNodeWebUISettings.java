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
 *
 * History
 *   19.08.2025 (ai-migration): created Web UI settings replacing legacy dialog
 */
package org.knime.base.node.preproc.colconvert.categorytonumber2;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.OptionalState;
import org.knime.node.parameters.widget.OptionalWidget.OptionalWrapper;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Web UI (Modern) dialog settings for the Category to Number node.
 * Backwards compatibility: preserves all legacy keys used by {@link CategoryToNumberNodeSettings} & model.
 *
 * Legacy config keys: append_columns, column_suffix, start_index, increment, max_categories,
 * default_value, map_missing_to plus column filter root key "column-filter".
 */
@Layout(CategoryToNumberNodeWebUISettings.NumberingOptionsSection.class)
@SuppressWarnings({"restriction", "unused"})
public final class CategoryToNumberNodeWebUISettings implements NodeParameters {

    /** Empty constructor required for persistence/JSON. */
    public CategoryToNumberNodeWebUISettings() {}

    /** Context constructor populating dynamic column choices. */
    CategoryToNumberNodeWebUISettings(final NodeParametersInput context) {
        this();
        m_inclCols = new ColumnFilter(ColumnSelectionUtil.getStringColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    /* ================= Sections ================= */
    @Section(title = "Column Selection", description = "Select string columns whose categories shall be mapped to numbers.")
    interface ColumnSelectionSection {}

    @Section(title = "Numbering Options", description = "Control how categories are turned into integers.")
    @After(ColumnSelectionSection.class)
    interface NumberingOptionsSection {}

    @Section(title = "Missing / Default Handling", description = "Define numeric values for unseen or missing categories.")
    @After(NumberingOptionsSection.class)
    interface MissingHandlingSection {}

    /* ================= Column Selection ================= */
    @Persistor(InclColsPersistor.class)
    @Widget(title = "Columns", description = "Move string columns to 'Includes' to convert them.")
    @ChoicesProvider(StringColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    ColumnFilter m_inclCols = new ColumnFilter();

    /* ================= Numbering Options ================= */
    @Persistor(AppendColumnsPersistor.class)
    @Widget(title = "Append columns", description = "Append converted numeric columns (unchecked: replace originals).")
    boolean m_appendColumns = true; // legacy default

    @Persistor(ColumnSuffixPersistor.class)
    @Widget(title = "Column suffix", description = "Suffix for appended numeric columns.")
    @TextInputWidget
    String m_columnSuffix = " (to number)"; // legacy default

    @Persistor(StartIndexPersistor.class)
    @Widget(title = "Start value", description = "Integer used for the first encountered category.")
    @NumberInputWidget(min = Integer.MIN_VALUE, max = Integer.MAX_VALUE, step = 1)
    int m_startIndex = 0;

    @Persistor(IncrementPersistor.class)
    @Widget(title = "Increment", description = "Step added for each new category.")
    @NumberInputWidget(min = 1, max = Integer.MAX_VALUE, step = 1)
    int m_increment = 1;

    @Persistor(MaxCategoriesPersistor.class)
    @Widget(title = "Max. categories", description = "Maximum number of distinct categories before failing.")
    @NumberInputWidget(min = 1, max = Integer.MAX_VALUE, step = 1)
    int m_maxCategories = 100; // legacy constant DEFAULT_MAX_CATEGORIES

    /* ================= Missing / Default Handling ================= */
    @Persistor(DefaultValuePersistor.class)
    @Widget(title = "Default value", description = "Numeric value for previously unseen categories (leave empty to output missing).")
    @OptionalWidget
    OptionalWrapper<Integer> m_defaultValue = OptionalWrapper.notSet();

    @Persistor(MapMissingToPersistor.class)
    @Widget(title = "Map missing to", description = "Numeric value used instead of missing categorical cells (leave empty to keep missing).")
    @OptionalWidget
    OptionalWrapper<Integer> m_mapMissingTo = OptionalWrapper.notSet();

    /* ================= Persistors ================= */
    static final class InclColsPersistor extends LegacyColumnFilterPersistor { InclColsPersistor() { super("column-filter"); } }
    static final class AppendColumnsPersistor extends SettingsModelBooleanPersistor { AppendColumnsPersistor() { super("append_columns"); } }
    static final class ColumnSuffixPersistor extends SettingsModelStringPersistor { ColumnSuffixPersistor() { super("column_suffix"); } }
    static final class StartIndexPersistor implements NodeParametersPersistor<Integer> {
        @Override public Integer load(final NodeSettingsRO s) throws InvalidSettingsException { return s.getInt("start_index", 0); }
        @Override public void save(final Integer v, final NodeSettingsWO s) { s.addInt("start_index", v); }
        @Override public String[][] getConfigPaths() { return new String[][] {{"start_index"}}; }
    }
    static final class IncrementPersistor implements NodeParametersPersistor<Integer> {
        @Override public Integer load(final NodeSettingsRO s) throws InvalidSettingsException { return s.getInt("increment", 1); }
        @Override public void save(final Integer v, final NodeSettingsWO s) { s.addInt("increment", v); }
        @Override public String[][] getConfigPaths() { return new String[][] {{"increment"}}; }
    }
    static final class MaxCategoriesPersistor implements NodeParametersPersistor<Integer> {
        @Override public Integer load(final NodeSettingsRO s) throws InvalidSettingsException { return s.getInt("max_categories", 100); }
        @Override public void save(final Integer v, final NodeSettingsWO s) { s.addInt("max_categories", v); }
        @Override public String[][] getConfigPaths() { return new String[][] {{"max_categories"}}; }
    }
    static final class DefaultValuePersistor implements NodeParametersPersistor<OptionalWrapper<Integer>> {
        @Override public OptionalWrapper<Integer> load(final NodeSettingsRO s) throws InvalidSettingsException {
            DataCell cell = s.getDataCell("default_value", DataType.getMissingCell());
            if (cell.isMissing()) { return OptionalWrapper.notSet(); }
            return OptionalWrapper.of(((IntCell)cell).getIntValue());
        }
        @Override public void save(final OptionalWrapper<Integer> v, final NodeSettingsWO s) {
            if (v.getState() == OptionalState.SET) { s.addDataCell("default_value", new IntCell(v.get())); }
            else { s.addDataCell("default_value", DataType.getMissingCell()); }
        }
        @Override public String[][] getConfigPaths() { return new String[][] {{"default_value"}}; }
    }
    static final class MapMissingToPersistor implements NodeParametersPersistor<OptionalWrapper<Integer>> {
        @Override public OptionalWrapper<Integer> load(final NodeSettingsRO s) throws InvalidSettingsException {
            DataCell cell = s.getDataCell("map_missing_to", DataType.getMissingCell());
            if (cell.isMissing()) { return OptionalWrapper.notSet(); }
            return OptionalWrapper.of(((IntCell)cell).getIntValue());
        }
        @Override public void save(final OptionalWrapper<Integer> v, final NodeSettingsWO s) {
            if (v.getState() == OptionalState.SET) { s.addDataCell("map_missing_to", new IntCell(v.get())); }
            else { s.addDataCell("map_missing_to", DataType.getMissingCell()); }
        }
        @Override public String[][] getConfigPaths() { return new String[][] {{"map_missing_to"}}; }
    }
}
