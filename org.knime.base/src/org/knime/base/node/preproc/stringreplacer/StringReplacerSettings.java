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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.06.2007 (thor): created
 */
package org.knime.base.node.preproc.stringreplacer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.EnumFieldPersistor;

/**
 * This class holds the settings for the string replacer node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class StringReplacerSettings {

    /**
     * Whether to match a pattern (true) or a literal string (false)
     *
     * @deprecated since 5.4, use {@link #CFG_PATTERN_TYPE} instead
     */
    @Deprecated(since = "5.4")
    static final String CFG_FIND_PATTERN = "findPattern";

    /**
     * Whether the provided pattern is a regular expression (true) or a wildcard pattern (false). Only relevant if
     * {@link StringReplacerSettings#CFG_FIND_PATTERN} is true.
     *
     * @deprecated since 5.4, use {@link #CFG_PATTERN_TYPE} instead
     */
    @Deprecated(since = "5.4")
    static final String CFG_PATTERN_IS_REGEX = "patternIsRegex";

    /** @since 5.4 */
    static final String CFG_PATTERN_TYPE = "patternType";

    static final String CFG_ENABLE_ESCAPING = "enableEscaping";

    static final String CFG_REPLACEMENT = "replacement";

    static final String CFG_REPLACE_ALL_OCCURENCES = "replaceAllOccurences";

    static final String CFG_PATTERN = "pattern";

    static final String CFG_NEW_COL_NAME = "newColName";

    static final String CFG_CREATE_NEW_COL = "createNewCol";

    static final String CFG_COL_NAME = "colName";

    static final String CFG_CASE_SENSITIVE = "caseSensitive";

    private boolean m_caseSensitive = CaseMatching.DEFAULT == CaseMatching.CASESENSITIVE;

    private String m_colName;

    private boolean m_createNewCol;

    private String m_newColName = "";

    private String m_pattern = "";

    private boolean m_replaceAllOccurrences;

    private String m_replacement = "";

    /** @since 2.8 */
    private boolean m_enableEscaping;

    /** @since 5.1 */
    private PatternType m_patternType = PatternType.DEFAULT;

    /**
     * Returns whether the pattern is a regular expression or a simple wildcard pattern.
     *
     * @return <code>true</code> if it is a regular expression, <code>false</code> if it contains wildcards
     * @throws InvalidSettingsException
     * @since 2.8
     * @deprecated
     */
    @Deprecated(since = "5.1")
    public boolean patternIsRegex() throws InvalidSettingsException {
        return switch (m_patternType) {
            case REGEX -> true;
            case WILDCARD -> false;
            default -> throw new InvalidSettingsException(
                "Pattern type " + m_patternType.name() + " needs to be retrieved by the patternType() method.");
        };
    }

    /**
     * Sets whether the pattern is a regular expression or a simple wildcard pattern.
     *
     * @param regex <code>true</code> if it is a regular expression, <code>false</code> if it contains wildcards
     * @since 2.8
     * @deprecated
     */
    @Deprecated(since = "5.1")
    public void patternIsRegex(final boolean regex) {
        m_patternType = regex ? PatternType.REGEX : PatternType.WILDCARD;
    }

    /**
     * Returns what pattern type is configured in the settings
     *
     * @return the pattern type
     * @since 5.1
     */
    public PatternType patternType() {
        return m_patternType;
    }

    /**
     * Set what pattern type to use
     *
     * @param pt the pattern type to use
     * @since 5.1
     */
    public void patternType(final PatternType pt) {
        m_patternType = pt;
    }

    /**
     * Returns whether escaping via a backslash is enabled.
     *
     * @return <code>true</code> if the backslash is an escape character, <code>false</code> otherwise
     * @since 2.8
     */
    public boolean enableEscaping() {
        return m_enableEscaping;
    }

    /**
     * Sets whether escaping via a backslash is enabled.
     *
     * @param enable <code>true</code> if the backslash is an escape character, <code>false</code> otherwise
     * @since 2.8
     */
    public void enableEscaping(final boolean enable) {
        m_enableEscaping = enable;
    }

    /**
     * Returns if the pattern should match case sensitive or not.
     *
     * @return <code>true</code> if the matches should be case sensitive, <code>false</code> otherwise
     */
    public boolean caseSensitive() {
        return m_caseSensitive;
    }

    /**
     * Sets if the pattern should match case sensitive or not.
     *
     * @param b <code>true</code> if the matches should be case sensitive, <code>false</code> otherwise
     */
    public void caseSensitive(final boolean b) {
        m_caseSensitive = b;
    }

    /**
     * Returns the name of the column that should be processed.
     *
     * @return the column's name
     */
    public String columnName() {
        return m_colName;
    }

    /**
     * Sets the name of the column that should be processed.
     *
     * @param colName the column's name
     */
    public void columnName(final String colName) {
        m_colName = colName;
    }

    /**
     * Returns if a new column should be created instead of overriding the values in the target column.
     *
     * @return <code>true</code> if a new column should be created, <code>false</code> otherwise
     * @see #newColumnName()
     */
    public boolean createNewColumn() {
        return m_createNewCol;
    }

    /**
     * Sets if a new column should be created instead of overriding the values in the target column.
     *
     * @param b <code>true</code> if a new column should be created, <code>false</code> otherwise
     * @see #newColumnName(String)
     */
    public void createNewColumn(final boolean b) {
        m_createNewCol = b;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings node settings
     * @throws InvalidSettingsException if settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_caseSensitive = settings.getBoolean(CFG_CASE_SENSITIVE);
        m_colName = settings.getString(CFG_COL_NAME);
        m_createNewCol = settings.getBoolean(CFG_CREATE_NEW_COL);
        m_newColName = settings.getString(CFG_NEW_COL_NAME);
        m_pattern = settings.getString(CFG_PATTERN);
        m_replaceAllOccurrences = settings.getBoolean(CFG_REPLACE_ALL_OCCURENCES);
        m_replacement = settings.getString(CFG_REPLACEMENT);

        /** @since 2.8 */
        m_enableEscaping = settings.getBoolean(CFG_ENABLE_ESCAPING, false);

        /** @since 5.4 */
        m_patternType = loadPatternType(settings);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings node settings
     */
    void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_caseSensitive = settings.getBoolean(CFG_CASE_SENSITIVE, false);
        m_colName = settings.getString(CFG_COL_NAME, null);
        m_createNewCol = settings.getBoolean(CFG_CREATE_NEW_COL, false);
        m_newColName = settings.getString(CFG_NEW_COL_NAME, null);
        m_pattern = settings.getString(CFG_PATTERN, "");
        m_replaceAllOccurrences = settings.getBoolean(CFG_REPLACE_ALL_OCCURENCES, false);
        m_replacement = settings.getString(CFG_REPLACEMENT, "");
        m_enableEscaping = settings.getBoolean(CFG_ENABLE_ESCAPING, false);

        /** @since 5.4 */
        try {
            m_patternType = loadPatternType(settings);
        } catch (InvalidSettingsException e) {
            m_patternType = PatternType.DEFAULT;
        }
    }

    private static PatternType loadPatternType(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_PATTERN_TYPE)) {
            return new EnumFieldPersistor<>(CFG_PATTERN_TYPE, PatternType.class).load(settings);
        } else if (settings.getBoolean(StringReplacerSettings.CFG_FIND_PATTERN, true)) {
            final var isRegex = settings.getBoolean(StringReplacerSettings.CFG_PATTERN_IS_REGEX);
            return isRegex ? PatternType.REGEX : PatternType.WILDCARD;
        } else {
            return PatternType.LITERAL;
        }
    }

    /**
     * Returns the name of the new column.
     *
     * @return the new column's name
     * @see #createNewColumn()
     */
    public String newColumnName() {
        return m_newColName;
    }

    /**
     * Sets the name of the new column.
     *
     * @param colName the new column's name
     * @see #createNewColumn(boolean)
     */
    public void newColumnName(final String colName) {
        m_newColName = colName;
    }

    /**
     * Returns the pattern.
     *
     * @return the pattern
     */
    public String pattern() {
        return m_pattern;
    }

    /**
     * Sets the pattern.
     *
     * @param pattern the pattern
     */
    public void pattern(final String pattern) {
        m_pattern = pattern;
    }

    /**
     * Returns if the whole string or all occurrences of the pattern should be replaced.
     *
     * @return <code>true</code> if all occurrences should be replaced, <code>false</code> if the whole string should be
     *         replaced
     *
     */
    public boolean replaceAllOccurrences() {
        return m_replaceAllOccurrences;
    }

    /**
     * Sets if the whole string or all occurrences of the pattern should be replaced.
     *
     * @param b <code>true</code> if all occurrences should be replaced, <code>false</code> if the whole string should
     *            be replaced
     *
     */
    public void replaceAllOccurrences(final boolean b) {
        m_replaceAllOccurrences = b;
    }

    /**
     * Returns the replacement text.
     *
     * @return the replacement text
     */
    public String replacement() {
        return m_replacement;
    }

    /**
     * Sets the replacement text.
     *
     * @param replacement the replacement text
     */
    public void replacement(final String replacement) {
        m_replacement = replacement;
    }

    /**
     * Save the settings into the node settings object.
     *
     * @param settings node settings
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_CASE_SENSITIVE, m_caseSensitive);
        settings.addString(CFG_COL_NAME, m_colName);
        settings.addBoolean(CFG_CREATE_NEW_COL, m_createNewCol);
        settings.addString(CFG_NEW_COL_NAME, m_newColName);
        settings.addString(CFG_PATTERN, m_pattern);
        settings.addBoolean(CFG_REPLACE_ALL_OCCURENCES, m_replaceAllOccurrences);
        settings.addString(CFG_REPLACEMENT, m_replacement);
        settings.addBoolean(CFG_ENABLE_ESCAPING, m_enableEscaping);
        settings.addString(CFG_PATTERN_TYPE, m_patternType.name());
    }
}
