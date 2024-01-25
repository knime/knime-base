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
 *   22 Jan 2024 (knime): created
 */
package org.knime.base.node.preproc.colcombine2;

import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.EnumFieldPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class ColCombine2NodeSettings implements DefaultNodeSettings {

    ColCombine2NodeSettings(final DefaultNodeSettingsContext context) {
        m_columnFilter = ColumnFilter.createDefault(AllColumns.class, context);
    }

    /**
     * Constructor for persistence and conversion to JSON.
     */
    ColCombine2NodeSettings() {
    }

    enum DelimiterInputs {
        /**
         * Delimiter in input data are quoted
         */
        @Label("Quote inputs")
        QUOTE,
        /**
         * delimiters in input data are replaced
         */
        @Label("Replace delimiters")
        REPLACE
    }

    enum QuoteInputs {
        /**
         * Delimiter in input data are quoted
         */
        @Label("All")
        ALL,
        /**
         * delimiters in input data are replaced
         */
        @Label("Only Necessary")
        ONLY_NECESSARY
    }

    @Section(title = "Concatenation")
    interface Concatenation {
    }

    @Section(title = "Output")
    interface Output {
    }

    private static final class IsQuote extends OneOfEnumCondition<DelimiterInputs> {
        @Override
        public DelimiterInputs[] oneOf() {
            return new DelimiterInputs[] { DelimiterInputs.QUOTE } ;
        }
    }

    @Persist(configKey = "column-filter", customPersistor = LegacyColumnFilterPersistor.class)
    @Widget(title = "Input columns", description = "Select the columns to combine in the output table.")
    @ChoicesWidget(choices = AllColumns.class)
    @Layout(Concatenation.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Persist(customPersistor = FailIfMissingPersistor.class)
    @Widget(title = "Fail if there are missing columns", description = "If true the node will fail when there are missing columns selected", advanced = true)
    @Layout(Concatenation.class)
    boolean m_failIfMissingColumns = false;

    @Widget(title = "Delimiter", description = "Enter a delimiter string here. This string is used to separate "
        + "the different cell contents in the new, appended column.")
    @Layout(Concatenation.class)
    String m_delimiter = ",";

    @Widget(title= "Handle delimiter inputs", description = "", advanced = true)
    @Persist(customPersistor = DelimiterInputPersistor.class)
    @Layout(Concatenation.class)
    @Signal(condition = IsQuote.class)
    @ValueSwitchWidget
    DelimiterInputs m_delimiterInputs = DelimiterInputs.QUOTE;

    @Widget(title = "Quote character", advanced = true, description = "The character entered here will be used to quote the cell content "
        + "in case that it contains the delimiter string (for instance if the cell content is "
        + "<i>some;value</i>, the delimiter string is ';' (a single semicolon), and the quote character is '\"' "
        + "(single quote char) the quoted string will be <i>\"some;value\"</i> "
        + "(including quotes)). You can force quotation by checking \"Quote Always\". "
        + "Alternatively, the user can also replace the delimiter  string in the cell content string (see below).")
    @Persist(configKey = "quote_char", customPersistor = QuoteCharacterPersistor.class)
    @Layout(Concatenation.class)
    @Effect(signals = IsQuote.class, type = EffectType.SHOW)
    Character m_quoteCharacter = '"';

    @Widget(title = "Quote inputs", advanced = true, description = "")
    @Persist(configKey = "is_quoting_always", customPersistor = QuoteInputPersistor.class)
    @Layout(Concatenation.class)
    @Effect(signals = IsQuote.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    QuoteInputs m_quoteInputs = QuoteInputs.ONLY_NECESSARY;

    @Widget(title = "Replacement", advanced = true, description = "If the string representation of the cell content contains the "
        + "delimiter string, it will be replaced by the string entered here (if you entered '-' here, the output of "
        + "the above example would be <i>some-value</i>).")
    @Persist(configKey = "replace_delimiter", customPersistor = ReplaceDelimiterPersistor.class)
    @Layout(Concatenation.class)
    @Effect(signals = IsQuote.class, type = EffectType.HIDE)
    String m_replacementDelimiter = "";

    @Widget(title = "Output column name", description = "The name of the new column.")
    @Persist(configKey = "new_column_name", customPersistor = OutputColumnNamePersistor.class)
    @Layout(Output.class)
    String m_outputColumnName = "Combined String";

    @Widget(title = "Remove input columns", description = "If selected, removes the columns in the &quot;Include&quot; "
        + "list from the output.")
    @Persist(configKey = "remove_included_columns", customPersistor = RemoveIncludedColumnsPersitor.class)
    @Layout(Output.class)
    boolean m_removeInputColumns = false;

    static final class AllColumns implements ColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .toArray(DataColumnSpec[]::new);
        }

    }

    static final class QuoteCharacterPersistor extends NodeSettingsPersistorWithConfigKey<Character> {

        @Override
        public Character load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return settings.getChar(getConfigKey());
            }
            if (settings.containsKey("quote_char")) {
                return settings.getChar("quote_char");
            }
            return '"';
        }

        @Override
        public void save(final Character obj, final NodeSettingsWO settings) {
            settings.addChar(getConfigKey(), obj);
        }
    }

    static final class RemoveIncludedColumnsPersitor extends NodeSettingsPersistorWithConfigKey<Boolean> {

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return settings.getBoolean(getConfigKey());
            }
            if (settings.containsKey("remove_included_columns")) {
                return settings.getBoolean("remove_included_columns");
            }
            return false;
        }

        @Override
        public void save(final Boolean obj, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), obj);
        }
    }

    static final class OutputColumnNamePersistor extends NodeSettingsPersistorWithConfigKey<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return settings.getString(getConfigKey());
            }
            if (settings.containsKey("new_column_name")) {
                return settings.getString("new_column_name");
            }
            return "Combined String";
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), obj);
        }
    }

    static final class DelimiterInputPersistor extends NodeSettingsPersistorWithConfigKey<DelimiterInputs> {

        private EnumFieldPersistor<DelimiterInputs> persistor;

        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
            persistor = new EnumFieldPersistor<>(configKey, DelimiterInputs.class);
        }

        @Override
        public DelimiterInputs load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return persistor.load(settings);
            }
            if (settings.containsKey("is_quoting")) {
                if (settings.getBoolean("is_quoting")) {
                return DelimiterInputs.QUOTE;
                } else {
                    return DelimiterInputs.REPLACE;
                }
            }
            return DelimiterInputs.REPLACE;
        }

        @Override
        public void save(final DelimiterInputs obj, final NodeSettingsWO settings) {
            persistor.save(obj, settings);
        }
    }

    static final class QuoteInputPersistor extends NodeSettingsPersistorWithConfigKey<QuoteInputs> {

        @Override
        public QuoteInputs load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey("is_quoting_always")) {
                if(settings.getBoolean("is_quoting_always")) {
                    return QuoteInputs.ALL;
                } else {
                    return QuoteInputs.ONLY_NECESSARY;
                }
            }
            return QuoteInputs.ONLY_NECESSARY;
        }

        @Override
        public void save(final QuoteInputs obj, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), obj == QuoteInputs.ALL);
        }
    }

    static final class ReplaceDelimiterPersistor extends NodeSettingsPersistorWithConfigKey<String> {

        @Override
        public void setConfigKey(final String configKey) {
            super.setConfigKey(configKey);
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return settings.getString(getConfigKey());
            }
            if (settings.containsKey("replace_delimiter")) {
                return settings.getString("replace_delimiter");
            }
            return "";
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), obj);
        }
    }

    static final class FailIfMissingPersistor extends NodeSettingsPersistorWithConfigKey<Boolean> {

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(getConfigKey())) {
                return settings.getBoolean(getConfigKey());
            }
            if (settings.containsKey("column-filter")) {
                var columnFilter = settings.getNodeSettings("column-filter");
                var enforceOptionName = columnFilter.getString("enforce_option", "");
                var enforceOption = EnforceOption.valueOf(enforceOptionName);

                return enforceOption == EnforceOption.EnforceInclusion;
            }

            return false;
        }

        @Override
        public void save(final Boolean obj, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), obj);
        }
    }
}