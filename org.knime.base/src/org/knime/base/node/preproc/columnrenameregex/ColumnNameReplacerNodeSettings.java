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
 *   Mar 19, 2025 (david): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.util.List;

import org.knime.base.node.util.regex.CaseMatching;
import org.knime.base.node.util.regex.PatternType;
import org.knime.base.node.util.regex.ReplacementStrategy;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Settings for the Column Name Replacer node (formerly Column Rename (Regex)). Made public, since other column rename
 * nodes (e.g., in knime-database) use these settings as well.
 *
 * @since 5.5
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class ColumnNameReplacerNodeSettings implements DefaultNodeSettings {

    static final class PatternTypeRef implements Reference<PatternType> {
    }

    @Widget(title = PatternType.OPTION_NAME, description = PatternType.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @Migration(PatternTypeMigration.class)
    @ValueReference(PatternTypeRef.class)
    PatternType m_patternType = PatternType.LITERAL;

    static final class PatternTypeMigration implements NodeSettingsMigration<PatternType> {

        static final String IS_LITERAL_LEGACY_CFG_KEY = "isLiteral";

        @Override
        public List<ConfigMigration<PatternType>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(PatternTypeMigration::loadFromLegacy)
                .withDeprecatedConfigPath(IS_LITERAL_LEGACY_CFG_KEY).build());
        }

        private static PatternType loadFromLegacy(final NodeSettingsRO settings) {
            if (settings.getBoolean(IS_LITERAL_LEGACY_CFG_KEY, false)) {
                return PatternType.LITERAL;
            } else {
                return PatternType.REGEX;
            }

        }

    }

    static final class IsWildcard implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(PatternTypeRef.class).isOneOf(PatternType.WILDCARD);
        }

    }

    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Use backslash as escape character", description = """
            If checked, the backslash character can be used to escape special characters. For instance, <tt>\\?</tt>
            will match the literal character <tt>?</tt> instead of an arbitrary character. In order to match a
            backslash you need to escape the backslash, too (<tt>\\</tt>).
            """)
    @Effect(predicate = IsWildcard.class, type = EffectType.SHOW)
    boolean m_enableEscapingWildcard;

    @Widget(title = CaseMatching.OPTION_NAME, description = CaseMatching.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @Migration(CaseMatchingMigration.class)
    CaseMatching m_caseSensitivity = CaseMatching.CASESENSITIVE;

    static final class CaseMatchingMigration implements NodeSettingsMigration<CaseMatching> {

        static final String IS_CASE_INSENSITIVE_LEGACY_CFG_KEY = "isCaseInsensitive";

        @Override
        public List<ConfigMigration<CaseMatching>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(CaseMatchingMigration::loadFromLegacy)
                .withDeprecatedConfigPath(IS_CASE_INSENSITIVE_LEGACY_CFG_KEY).build());
        }

        private static CaseMatching loadFromLegacy(final NodeSettingsRO settings) {
            if (settings.getBoolean(IS_CASE_INSENSITIVE_LEGACY_CFG_KEY, false)) {
                return CaseMatching.CASEINSENSITIVE;
            } else {
                return CaseMatching.CASESENSITIVE;
            }
        }
    }

    @Widget(title = "Pattern", description = """
            A literal string, wildcard pattern or regular expression, depending on \
            the pattern type selected above.
            """)
    @Persist(configKey = "searchString")
    String m_pattern = "";

    @Widget(title = "Replacement text", description = """
            The replacement text for the pattern. If you are using a regular \
            expression, you may also use backreferences (e.g. $1 to refer to \
            the first capture group. Named capture groups can also be used with \
            (?&lt;group&gt;) and ${group} to refer to them).
            """)
    @Persist(configKey = "replaceString")
    String m_replacement = "";

    @Widget(title = ReplacementStrategy.OPTION_NAME, description = ReplacementStrategy.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    @Migration(AllOccurrencesDefaultProvider.class)
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;

    static final class AllOccurrencesDefaultProvider implements DefaultProvider<ReplacementStrategy> {

        @Override
        public ReplacementStrategy getDefault() {
            return ReplacementStrategy.ALL_OCCURRENCES;
        }

    }

    /**
     * @since 5.5
     *
     *        This version resolves a bug in the underlying regular expression engine that previously caused incorrect
     *        behavior when matching Unicode characters.
     *
     *        In earlier versions, case-insensitive matching did not correctly support Unicode case mappings (e.g., the
     *        capital letter ẞ was not matched by the case-insensitive pattern {@code (?i)ß}) due to the absence of the
     *        {@code UNICODE_CASE} flag in the compiled patterns.
     *
     *        Starting with version 5.5, this node uses the {@code UNICODE_CASE} flag by default for improved Unicode
     *        compatibility in case-insensitive matching.
     *
     *        To preserve backwards compatibility for existing workflows, the {@code m_properlySupportUnicodeCharacters}
     *        setting is introduced and loads {@code false} whenever the node settings have not been saved since.
     */
    @Migration(LoadFalseForOldNodes.class)
    boolean m_properlySupportUnicodeCharacters = true;

    /**
     * I.e. the only way the field can be set to {@code false} is when the node settings are loaded from a workflow that
     * was saved before version 5.5.
     */
    static final class LoadFalseForOldNodes implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return false;
        }
    }
}
