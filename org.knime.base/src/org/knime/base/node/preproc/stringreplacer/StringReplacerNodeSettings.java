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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The StringReplacerNodeSettings define the WebUI dialog of the StringReplacer Node. The serialization must go via the
 * {@link StringReplacerSettings}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class StringReplacerNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = StringReplacerSettings.CFG_COL_NAME)
    @Widget(title = "Target column", description = "Name of the column whose cells should be processed")
    @ChoicesWidget(choices = StringColumnChoices.class)
    String m_colName;

    enum PatternType {
            @Label("Wildcard")
            WILDCARD,

            @Label("Regular expression")
            REGEX;
    }

    @Persist(customPersistor = PatternTypePersistor.class)
    @Widget(title = "Matching criteria", description = "Select the type of pattern which you want to use." + "<ul>" //
        + "<li><b>Wildcard:</b> If you select <i>wildcard</i>, " //
        + "then <b>*</b> and <b>?</b> are (the only) meta-characters. They match an arbitrary number of " //
        + "characters or a single character, respectively.</li>" //
        + "<li><b>Regular expression:</b> If you select <i>regular expression</i> you can use the full functionality" //
        + " of Java regular expressions, including backreferences in the replacement text. See the " //
        + "<a href=\"http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html\">Java API</a> "
        + "for details.</li>" //
        + "</ul>")
    PatternType m_patternType = PatternType.WILDCARD;

    @Persist(configKey = StringReplacerSettings.CFG_PATTERN)
    @Widget(title = "Find", description = "Either a wildcard pattern or a regular expression, "
        + "depending on the pattern type selected above.")
    String m_pattern;

    @Persist(configKey = StringReplacerSettings.CFG_REPLACEMENT)
    @Widget(title = "Replacement text",
        description = "The text that replaces that previous value in the cell if the pattern matched the previous "
            + "value. If you are using a regular expression, you may also use backreferences (e.g. <b>$1</b>).")
    String m_replacement;

    enum ReplacementStrategy {
            @Label("Whole string")
            WHOLE_STRING,

            @Label("All occurrences")
            ALL_OCCURRENCES;
    }

    @Persist(customPersistor = ReplacementStrategyPersistor.class)
    @Widget(title = "Replacement strategy", description = //
    "Whether the whole cell content is replaced on a match or whether each " //
        + "occurrence is replaced inside the text individually." //
        + "<ul>" //
        + "<li><b>Whole string:</b> " //
        + "The entire string (i.e. the entire cell content) is replaced when it completely " //
        + "matches the search pattern (including the meta characters <b>*</b> and <b>?</b>). </li>" //
        + "<li><b>All occurrences:</b> " //
        + "All occurrences of the entered pattern are replaced in the target column. The meta" //
        + "characters <b>*</b> and <b>?</b> are not allowed in the pattern in this case.</li>" //
        + "</ul>")
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;

    @Persist(configKey = StringReplacerSettings.CFG_CASE_SENSITIVE)
    @Widget(title = "Case sensitive", description = "Check this if the pattern should be case sensitive")
    boolean m_caseSensitive;

    @Persist(configKey = StringReplacerSettings.CFG_ENABLE_ESCAPING)
    @Widget(title = "Use backslash as escape character",
        description = "If you want to replace the wildcard characters <b>*</b> and <b>?</b> themselves, " //
            + "you need to enable this option and escape them using a backslash (<b>\\*</b> or <b>\\?</b>). " //
            + "In order to replace a backslash you need to escape the backslash, too (<b>\\\\</b>).")
    boolean m_enableEscaping;

    @Persist(configKey = StringReplacerSettings.CFG_CREATE_NEW_COL)
    @Widget(title = "Create new column",
        description = "Creates a new column with the name entered in the text field instead "
            + "of replacing the values in the original column.")
    boolean m_createNewCol;

    @Persist(configKey = StringReplacerSettings.CFG_NEW_COL_NAME)
    @Widget(title = "New column name", description = "Name of the newly created column with replaced Strings")
    String m_newColName = "ReplacedColumn";

    private static final class PatternTypePersistor implements FieldNodeSettingsPersistor<PatternType> {
        @Override
        public PatternType load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX, false)) {
                return PatternType.REGEX;
            } else {
                return PatternType.WILDCARD;
            }
        }

        @Override
        public void save(final PatternType obj, final NodeSettingsWO settings) {
            settings.addBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX, obj == PatternType.REGEX);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{StringReplacerSettings.CFG_PATTERN_IS_REGEX};
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
        public String[] choices(final SettingsCreationContext context) {
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
