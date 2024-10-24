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
 *   5 Jan 2023 (chaubold): created
 */
package org.knime.base.node.preproc.stringreplacer;

import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultPersistorWithDeprecations;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.BooleanReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * The StringReplacerNodeSettings define the WebUI dialog of the StringReplacer Node. The serialization must go via the
 * {@link StringReplacerSettings}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class StringReplacerNodeSettings implements DefaultNodeSettings {

    // Layout

    interface DialogSections {
        @Section(title = "Column Selection")
        interface ColumnSelection {
        }

        @Section(title = "Find & Replace")
        interface FindAndReplace {
        }

        @Section(title = "Output")
        interface Output {
        }
    }

    // Settings

    @Layout(DialogSections.ColumnSelection.class)
    @Persist(configKey = StringReplacerSettings.CFG_COL_NAME)
    @Widget(title = "Target column", description = "Select the column in which the strings should be replaced.")
    @ChoicesWidget(choices = StringColumnChoices.class)
    String m_colName;

    interface PatternTypeRef extends Reference<PatternType> {
    }

    /** Indicates that the "Wildcard" pattern type is selected */
    static final class IsWildcard implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(PatternTypeRef.class).isOneOf(PatternType.WILDCARD);
        }
    }

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = PatternTypePersistor.class)
    @Widget(title = PatternType.OPTION_NAME, description = PatternType.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @ValueReference(PatternTypeRef.class)
    PatternType m_patternType = PatternType.DEFAULT;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_ENABLE_ESCAPING)
    @Widget(title = "Use backslash as escape character", description = """
            If checked, the backslash character can be used to escape special characters. For instance, <tt>\\?</tt>
            will match the literal character <tt>?</tt> instead of an arbitrary character. In order to match a
            backslash you need to escape the backslash, too (<tt>\\</tt>).
            """)
    @Effect(predicate = IsWildcard.class, type = EffectType.SHOW)
    boolean m_enableEscaping;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = CaseMatching.Persistor.class)
    @Widget(title = CaseMatching.OPTION_NAME, description = CaseMatching.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    CaseMatching m_caseMatching = CaseMatching.DEFAULT;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_PATTERN)
    @Widget(title = "Pattern", description = """
            A literal string, wildcard pattern or regular expression, depending on the pattern type selected above.
            """)
    String m_pattern;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(configKey = StringReplacerSettings.CFG_REPLACEMENT)
    @Widget(title = "Replacement text", description = """
            The text that replaces the previous value in the cell if the pattern matched it. If you are using a
            regular expression, you may also use backreferences (e.g. <tt>$1</tt> to refer to the first capture group,
            named capture groups can also be used with <tt>(?&lt;group&gt;)</tt> and <tt>${group}</tt> to refer to
            them).
            """)
    String m_replacement;

    @Layout(DialogSections.FindAndReplace.class)
    @Persist(customPersistor = ReplacementStrategyPersistor.class)
    @Widget(title = ReplacementStrategy.OPTION_NAME, description = ReplacementStrategy.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.DEFAULT;

    static final class CreateNewCol implements BooleanReference {
    }

    @Layout(DialogSections.Output.class)
    @Persist(configKey = StringReplacerSettings.CFG_CREATE_NEW_COL)
    @Widget(title = "Append new column", description = """
            If enabled, the strings will not be replaced in-place but a new column is appended that contains the
            original string with the replacement applied.
            """)
    @ValueReference(CreateNewCol.class)
    boolean m_createNewCol;

    @Layout(DialogSections.Output.class)
    @Persist(configKey = StringReplacerSettings.CFG_NEW_COL_NAME)
    @Widget(title = "New column name", description = "The name of the created column with replaced strings")
    @Effect(predicate = CreateNewCol.class, type = EffectType.SHOW)
    String m_newColName = "ReplacedColumn";

    // Persistors

    @SuppressWarnings("deprecation") // we're dealing with deprecated settings here
    static final class PatternTypePersistor extends NodeSettingsPersistorWithConfigKey<PatternType>
        implements DefaultPersistorWithDeprecations<PatternType> {

        private static PatternType loadLegazy(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(StringReplacerSettings.CFG_FIND_PATTERN, true)) {
                final var isRegex = settings.getBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX);
                return isRegex ? PatternType.REGEX : PatternType.WILDCARD;
            } else {
                return PatternType.LITERAL;
            }
        }

        @Override
        public List<ConfigsDeprecation<PatternType>> getConfigsDeprecations() {

            return List.of(
                // backwards-compatibility for 5.1 <= version < 5.4:
                ConfigsDeprecation.builder(PatternTypePersistor::loadLegazy)//
                    .withMatcher(settings -> !settings.containsKey(getConfigKey()))
                    .withDeprecatedConfigPath(StringReplacerSettings.CFG_FIND_PATTERN)//
                    .withDeprecatedConfigPath(StringReplacerSettings.CFG_PATTERN_IS_REGEX)//
                    .withNewConfigPath(getConfigKey())//
                    .build());//
        }
    }

    private static final class ReplacementStrategyPersistor implements FieldNodeSettingsPersistor<ReplacementStrategy> {
        @Override
        public ReplacementStrategy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES)) {
                return ReplacementStrategy.ALL_OCCURRENCES;
            } else {
                return ReplacementStrategy.WHOLE_STRING;
            }
        }

        @Override
        public void save(final ReplacementStrategy obj, final NodeSettingsWO settings) {
            settings.addBoolean(StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES,
                obj == ReplacementStrategy.ALL_OCCURRENCES);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{StringReplacerSettings.CFG_REPLACE_ALL_OCCURENCES};
        }
    }

    private static final class StringColumnChoices implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            final DataTableSpec specs = context.getDataTableSpecs()[0];
            if (specs == null) {
                return new String[0];
            } else {
                return specs.stream() //
                    .filter(s -> s.getType().isCompatible(StringValue.class)) //
                    .map(DataColumnSpec::getName) //
                    .toArray(String[]::new);
            }
        }
    }
}
