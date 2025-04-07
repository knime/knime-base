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

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.AlwaysSaveTrueBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterMigration;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.PatternValidation.ColumnNameValidationV2;

/**
 *
 * @author Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class ColCombine2NodeSettings implements DefaultNodeSettings {

    ColCombine2NodeSettings(final DefaultNodeSettingsContext context) {
        m_columnFilter = new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context)).withIncludeUnknownColumns();
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
            @Label("Only necessary")
            ONLY_NECESSARY
    }

    @Section(title = "Concatenation")
    interface Concatenation {
    }

    @Section(title = "Output")
    interface Output {
    }

    interface DelimiterInputsRef extends Reference<DelimiterInputs> {
    }

    private static final class IsQuote implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(DelimiterInputsRef.class).isOneOf(DelimiterInputs.QUOTE);
        }
    }

    @Migration(ColumnFilterMigration.class)
    @Widget(title = "Input columns", description = "Select the columns to combine in the output table.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(Concatenation.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Migration(FailIfMissingMigration.class)
    @Widget(title = "Fail if there are missing columns",
        description = "If true the node will fail when there are missing columns selected", advanced = true)
    @Layout(Concatenation.class)
    boolean m_failIfMissingColumns;

    @Widget(title = "Delimiter",
        description = "Enter a delimiter string here. This string is used to separate "
            + "the different cell contents in the new, appended column.")
    @Layout(Concatenation.class)
    String m_delimiter = ",";

    @Widget(title = "Handle delimiter inputs", description = "", advanced = true)
    @Migration(DelimiterInputMigration.class)
    @Layout(Concatenation.class)
    @ValueReference(DelimiterInputsRef.class)
    @ValueSwitchWidget
    DelimiterInputs m_delimiterInputs = DelimiterInputs.QUOTE;

    @Widget(title = "Quote character", advanced = true,
        description = "The character entered here will be used to quote the cell content "
            + "in case that it contains the delimiter string (for instance if the cell content is "
            + "<i>some;value</i>, the delimiter string is ';' (a single semicolon), and the quote character is '\"' "
            + "(single quote char) the quoted string will be <i>\"some;value\"</i> "
            + "(including quotes)). You can force quotation by checking \"Quote Always\". "
            + "Alternatively, the user can also replace the delimiter  string in the cell content string (see below).")
    @Persist(configKey = "quote_char")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(Concatenation.class)
    @Effect(predicate = IsQuote.class, type = EffectType.SHOW)
    char m_quoteCharacter = '"';

    @Widget(title = "Quote inputs", advanced = true, description = "")
    @Migration(QuoteInputMigration.class)
    @Layout(Concatenation.class)
    @Effect(predicate = IsQuote.class, type = EffectType.SHOW)
    @ValueSwitchWidget
    QuoteInputs m_quoteInputs = QuoteInputs.ONLY_NECESSARY;

    @Widget(title = "Replacement", advanced = true,
        description = "If the string representation of the cell content contains the "
            + "delimiter string, it will be replaced by the string entered here (if you entered '-' here, the output of "
            + "the above example would be <i>some-value</i>).")
    @Persist(configKey = "replace_delimiter")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(Concatenation.class)
    @Effect(predicate = IsQuote.class, type = EffectType.HIDE)
    String m_replacementDelimiter = "";

    @Widget(title = "Output column name", description = "The name of the new column.")
    @Persist(configKey = "new_column_name")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(Output.class)
    @TextInputWidget(validation = ColumnNameValidationV2.class)
    String m_outputColumnName = "Combined String";

    static final class DoNotAllowPaddedColumnNamePersistor extends AlwaysSaveTrueBoolean {
        protected DoNotAllowPaddedColumnNamePersistor() {
            super("doNotAllowPaddedColumnName");
        }
    }

    @Persistor(DoNotAllowPaddedColumnNamePersistor.class)
    boolean m_doNotAllowPaddedColumnName = true;

    @Widget(title = "Remove input columns",
        description = "If selected, removes the columns in the &quot;Include&quot; " + "list from the output.")
    @Persist(configKey = "remove_included_columns")
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(Output.class)
    boolean m_removeInputColumns;


    static final class DelimiterInputMigration implements NodeSettingsMigration<DelimiterInputs> {

        static final String CFG_KEY_BOOLEAN = "is_quoting";

        private static DelimiterInputs loadFromBoolean(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var isQuoting = settings.getBoolean(CFG_KEY_BOOLEAN);
            return isQuoting ? DelimiterInputs.QUOTE : DelimiterInputs.REPLACE;
        }

        @Override
        public List<ConfigMigration<DelimiterInputs>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(DelimiterInputMigration::loadFromBoolean)
                    .withDeprecatedConfigPath(CFG_KEY_BOOLEAN).build(), //
                ConfigMigration.builder(settings -> DelimiterInputs.REPLACE).build() //
            );
        }
    }

    static final class QuoteInputMigration implements NodeSettingsMigration<QuoteInputs> {

        static final String CFG_KEY_BOOLEAN = "is_quoting_always";

        private static QuoteInputs loadFromBoolean(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var isQuotingAlways = settings.getBoolean(CFG_KEY_BOOLEAN);
            return isQuotingAlways ? QuoteInputs.ALL : QuoteInputs.ONLY_NECESSARY;
        }

        @Override
        public List<ConfigMigration<QuoteInputs>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(QuoteInputMigration::loadFromBoolean).withDeprecatedConfigPath(CFG_KEY_BOOLEAN)
                    .build(), //
                ConfigMigration.builder(settings -> QuoteInputs.ONLY_NECESSARY).build() //
            );
        }
    }

    static final class ColumnFilterMigration extends LegacyColumnFilterMigration {

        static final String CFG_KEY_COLUMN_FILTER = "column-filter";

        ColumnFilterMigration() {
            super(CFG_KEY_COLUMN_FILTER);
        }
    }

    static final class FailIfMissingMigration implements NodeSettingsMigration<Boolean> {

        private static Boolean loadFromColumnFilter(final NodeSettingsRO settings) throws InvalidSettingsException {
            var columnFilter = settings.getNodeSettings(ColumnFilterMigration.CFG_KEY_COLUMN_FILTER);
            var enforceOptionName = columnFilter.getString("enforce_option", "");
            var enforceOption = EnforceOption.valueOf(enforceOptionName);

            return enforceOption == EnforceOption.EnforceInclusion;
        }

        @Override
        public List<ConfigMigration<Boolean>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(FailIfMissingMigration::loadFromColumnFilter)
                    .withDeprecatedConfigPath(ColumnFilterMigration.CFG_KEY_COLUMN_FILTER).build(), //
                ConfigMigration.builder(settings -> false).build() //)
            );
        }
    }
}
