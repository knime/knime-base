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
 *   28 Aug 2022 (jasper): created
 */
package org.knime.base.node.preproc.cellreplace;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Wrapper for the settings for the Cell Replacer node
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz
 * @deprecated Replaced by "Value Lookup" node
 */
@Deprecated
public class CellReplacerNodeSettings {

    /** What should be done when no matching dictionary entry is available. */
    enum NoMatchPolicy {
            /** Keep the original input (leave cell unmodified). */
            INPUT("Input"),
            /** Set missing value. */
            MISSING("Missing");

        private String m_name;

        NoMatchPolicy(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }

        static Optional<NoMatchPolicy> getPolicy(final String s) {
            return Arrays.stream(NoMatchPolicy.values()).filter(p -> s.equals(p.toString())).findFirst();
        }
    }

    /** How Strings in the target column / dictionary lookup shall be handled */
    enum StringMatchBehaviour {
            /** Only match exact correspondence */
            EXACT("Exact"),
            /** Match if dictionary lookup is substring of target column */
            SUBSTRING("Substring"),
            /** Allow Wildcards in dictionary lookup column */
            WILDCARD("Wildcard"),
            /** Allow Regex in dictionary lookup column */
            REGEX("RegEx");

        private String m_name;

        StringMatchBehaviour(final String name) {
            m_name = name;
        }

        @Override
        public String toString() {
            return m_name;
        }

        static Optional<StringMatchBehaviour> getBehaviour(final String s) {
            return Arrays.stream(StringMatchBehaviour.values()).filter(p -> s.equals(p.toString())).findFirst();
        }
    }

    private NoMatchPolicy m_noMatchPolicy = NoMatchPolicy.MISSING;

    private StringMatchBehaviour m_stringMatchBehaviour = StringMatchBehaviour.EXACT;

    private final SettingsModelString m_targetColModel;

    private final SettingsModelString m_noMatchPolicyModel;

    private final SettingsModelColumnName m_dictInputColModel;

    private final SettingsModelColumnName m_dictOutputColModel;

    private final SettingsModelString m_stringMatchBehaviourModel;

    private final SettingsModelBoolean m_stringCaseSensitiveMatchingModel;

    private final SettingsModelBoolean m_appendColumnModel;

    private final SettingsModelString m_appendColumnNameModel;

    private final SettingsModelBoolean m_appendFoundColumnModel;

    private final SettingsModelString m_foundColumnPositiveStringModel;

    private final SettingsModelString m_foundColumnNegativeStringModel;

    /** Added in 4.1.3 as part of AP-14007 (Cell Replacer removes spec of collection columns) */
    private final SettingsModelBoolean m_retainColumnPropertiesModel;

    CellReplacerNodeSettings() {
        m_targetColModel = new SettingsModelString("targetCol", null);
        m_noMatchPolicyModel = new SettingsModelString("noMatchPolicy", m_noMatchPolicy.toString());
        m_noMatchPolicyModel.addChangeListener(
            e -> m_noMatchPolicy = NoMatchPolicy.getPolicy(m_noMatchPolicyModel.getStringValue()).get());
        m_dictInputColModel = new SettingsModelColumnName("dictInputCol", null);
        m_dictOutputColModel = new SettingsModelColumnName("dictOutputCol", null);
        m_stringMatchBehaviourModel =
            new SettingsModelString("stringMatchBehaviour", m_stringMatchBehaviour.toString());
        m_stringMatchBehaviourModel.addChangeListener(e -> m_stringMatchBehaviour =
            StringMatchBehaviour.getBehaviour(m_stringMatchBehaviourModel.getStringValue()).get());
        m_stringCaseSensitiveMatchingModel = new SettingsModelBoolean("caseSensitive", true);
        m_appendColumnModel = new SettingsModelBoolean("appendColumn", false);
        m_appendColumnNameModel = createAppendColumnNameModel(m_appendColumnModel);
        m_appendFoundColumnModel = new SettingsModelBoolean("appendFoundColumn", false);
        m_foundColumnPositiveStringModel = createFoundColumnPositiveStringModel(m_appendFoundColumnModel);
        m_foundColumnNegativeStringModel = createFoundColumnNegativeStringModel(m_appendFoundColumnModel);
        m_retainColumnPropertiesModel = createRetainColumnPropertiesModel(m_noMatchPolicyModel);
    }

    private static final SettingsModelString createAppendColumnNameModel(final SettingsModelBoolean appendColumnModel) {
        final var result = new SettingsModelString("appendColumnName", "Replacement");
        appendColumnModel.addChangeListener(e -> result.setEnabled(appendColumnModel.getBooleanValue()));
        result.setEnabled(appendColumnModel.getBooleanValue());
        return result;
    }

    private static SettingsModelString
        createFoundColumnPositiveStringModel(final SettingsModelBoolean appendFoundColumnModel) {
        final var result = new SettingsModelString("foundColumnPositiveString", "found");
        appendFoundColumnModel.addChangeListener(e -> result.setEnabled(appendFoundColumnModel.getBooleanValue()));
        result.setEnabled(appendFoundColumnModel.getBooleanValue());
        return result;
    }

    private static SettingsModelString
        createFoundColumnNegativeStringModel(final SettingsModelBoolean appendFoundColumnModel) {
        final var result = new SettingsModelString("foundColumnNegativeString", "not found");
        appendFoundColumnModel.addChangeListener(e -> result.setEnabled(appendFoundColumnModel.getBooleanValue()));
        result.setEnabled(appendFoundColumnModel.getBooleanValue());
        return result;
    }

    private static final SettingsModelBoolean
        createRetainColumnPropertiesModel(final SettingsModelString noMatchPolicyModel) {
        final var result = new SettingsModelBoolean("retainColumnProperties", true);
        noMatchPolicyModel.addChangeListener(e -> result
            .setEnabled(Objects.equals(noMatchPolicyModel.getStringValue(), NoMatchPolicy.MISSING.toString())));
        result.setEnabled(Objects.equals(noMatchPolicyModel.getStringValue(), NoMatchPolicy.MISSING.toString()));
        return result;
    }

    /**
     * Get {@code SettingsModel} where the user inputs a target column
     *
     * @return
     */
    SettingsModelString getTargetColNameModel() {
        return m_targetColModel;
    }

    /**
     * Get the name of the target column, i.e. the column that will be searched for matching values
     *
     * @return
     */
    String getTargetColName() {
        return m_targetColModel.getStringValue();
    }

    /**
     * Get the {@code SettingsModel} that let's the user pick between different {@code NoMatchPolicy}s
     *
     * @return
     */
    SettingsModelString getNoMatchPolicyModel() {
        return m_noMatchPolicyModel;
    }

    /**
     * Get the policy of what to do when there's no match on a cell in the target column. See {@link NoMatchPolicy}.
     *
     * @return
     */
    NoMatchPolicy getNoMatchPolicy() {
        return m_noMatchPolicy;
    }

    /**
     * Get the {@code SettingsModel} where the user selects an input (lookup) column from the dictionary table
     *
     * @return
     */
    SettingsModelColumnName getDictInputColModel() {
        return m_dictInputColModel;
    }

    /**
     * Whether the user has selected the RowID as the input (lookup) column
     *
     * @return
     */
    boolean isDictInputRowID() {
        return m_dictInputColModel.useRowID();
    }

    /**
     * Get the name of the column that the user selected as the input (lookup) column from the dictionary table
     *
     * @return
     */
    String getDictInputColName() {
        return m_dictInputColModel.getColumnName();
    }

    /**
     * Get the {@code SettingsModel} where the user selects an output (replacement) column from the dictionary table
     *
     * @return
     */
    SettingsModelColumnName getDictOutputColModel() {
        return m_dictOutputColModel;
    }

    /**
     * Whether the user has selected the RowID as the output (replacement) column
     *
     * @return
     */
    boolean isDictOutputRowID() {
        return m_dictOutputColModel.useRowID();
    }

    /**
     * Get the name of the column that the user selected as the output (replacement) column from the dictionary table
     *
     * @return
     */
    String getDictOutputColName() {
        return m_dictOutputColModel.getColumnName();
    }

    /**
     * Get the {@link SettingsModel} that lets the user pick a {@link StringMatchBehaviour}
     *
     * @return
     */
    SettingsModelString getStringMatchBehaviourModel() {
        return m_stringMatchBehaviourModel;
    }

    /**
     * Get the {@link StringMatchBehaviour} that the user selected. This is only relevant if the input (lookup) column
     * is string-compatible
     *
     * @return
     */
    StringMatchBehaviour getStringMatchBehaviour() {
        return m_stringMatchBehaviour;
    }

    /**
     * Get the {@link SettingsModel} that lets the user decide whether string matching should be case sensitive or not
     *
     * @return
     */
    SettingsModelBoolean getStringCaseSensitiveMatchingModel() {
        return m_stringCaseSensitiveMatchingModel;
    }

    /**
     * Whether the user selected case-sensitive matching or not
     *
     * @return {@code true} iff the user selected case-sensitive matching
     */
    boolean isCaseSensitive() {
        return m_stringCaseSensitiveMatchingModel.getBooleanValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user pick whether the result column (in which all the matches have
     * been replaced) should be appended to the table instead of replacing the target column
     *
     * @return
     */
    SettingsModelBoolean getAppendColumnModel() {
        return m_appendColumnModel;
    }

    /**
     * Whether the user selected the option to append (instead of replacing the target column) the result column
     *
     * @return
     */
    boolean isAppendColumn() {
        return m_appendColumnModel.getBooleanValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user enter a name for the result column (if not replacing the target
     * column)
     *
     * @return
     */
    SettingsModelString getAppendColumnNameModel() {
        return m_appendColumnNameModel;
    }

    /**
     * Relevant if {@code isAppendColumn()} is {@code true}: get the user-specified name of the result column
     *
     * @return
     */
    String getAppendColumnName() {
        return m_appendColumnNameModel.getStringValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user choose to also append a "found" / "not found" column
     *
     * @return
     */
    SettingsModelBoolean getAppendFoundColumnModel() {
        return m_appendFoundColumnModel;
    }

    /**
     * Whether the user checked the option to also append a "found" / "not found" column
     *
     * @return
     */
    boolean isAppendFoundColumn() {
        return m_appendFoundColumnModel.getBooleanValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user define a string that will be entered into the "found" / "not
     * found" column if this row was matched against a dictionary entry (and therefore replaced)
     *
     * @return
     */
    SettingsModelString getFoundColumnPositiveStringModel() {
        return m_foundColumnPositiveStringModel;
    }

    /**
     * Get the string that the user defined to represent a match in the "found" / "not found" column
     *
     * @return
     */
    String getFoundColumnPositiveString() {
        return m_foundColumnPositiveStringModel.getStringValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user define a string that will be entered into the "found" / "not
     * found" column if this row was NOT matched against a dictionary entry (and therefore replaced)
     *
     * @return
     */
    SettingsModelString getFoundColumnNegativeStringModel() {
        return m_foundColumnNegativeStringModel;
    }

    /**
     * Get the string that the user defined to represent NO match in the "found" / "not found" column
     *
     * @return
     */
    String getFoundColumnNegativeString() {
        return m_foundColumnNegativeStringModel.getStringValue();
    }

    /**
     * Get the {@link SettingsModel} that lets the user decide whether to retain column properties from the target
     * column in the output column
     *
     * @return
     */
    SettingsModelBoolean getRetainColumnPropertiesModel() {
        return m_retainColumnPropertiesModel;
    }

    /**
     * Whether the user checked the option to retain column properties from the target column in the output column
     *
     * @return
     */
    boolean isRetainColumnProperties() {
        return m_retainColumnPropertiesModel.getBooleanValue();
    }

    /**
     * Save the settings represented by this instance to a {@link NodeSettingsWO} instance
     *
     * @param settings
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        m_targetColModel.saveSettingsTo(settings);
        m_noMatchPolicyModel.saveSettingsTo(settings);
        m_dictInputColModel.saveSettingsTo(settings);
        m_dictOutputColModel.saveSettingsTo(settings);
        m_stringMatchBehaviourModel.saveSettingsTo(settings);
        m_stringCaseSensitiveMatchingModel.saveSettingsTo(settings);
        m_appendColumnModel.saveSettingsTo(settings);
        m_appendColumnNameModel.saveSettingsTo(settings);
        m_appendFoundColumnModel.saveSettingsTo(settings);
        m_foundColumnPositiveStringModel.saveSettingsTo(settings);
        m_foundColumnNegativeStringModel.saveSettingsTo(settings);
        m_retainColumnPropertiesModel.saveSettingsTo(settings);
    }

    /**
     * Load the settings from a {@link NodeSettingsRO} instance into this instance
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_targetColModel.loadSettingsFrom(settings);
        m_noMatchPolicyModel.loadSettingsFrom(settings);
        m_dictInputColModel.loadSettingsFrom(settings);
        m_dictOutputColModel.loadSettingsFrom(settings);
        m_appendColumnModel.loadSettingsFrom(settings);
        m_appendColumnNameModel.loadSettingsFrom(settings);

        // Added with 4.1.3: Option to retain the column properties from the target column to the output column
        if (settings.containsKey(m_retainColumnPropertiesModel.getConfigName())) {
            m_retainColumnPropertiesModel.loadSettingsFrom(settings);
        } else { // Pre-4.1.3 workflow -- don't retain column properties
            m_retainColumnPropertiesModel.setBooleanValue(false);
        }

        // Added with 4.7: More flexible string matching and options for a "found" / "not found" column
        // See AP-13269: https://knime-com.atlassian.net/browse/AP-13269
        if (settings.containsKey(m_stringMatchBehaviourModel.getKey())) {
            m_stringMatchBehaviourModel.loadSettingsFrom(settings);
            m_stringCaseSensitiveMatchingModel.loadSettingsFrom(settings);
            m_appendFoundColumnModel.loadSettingsFrom(settings);
            m_foundColumnPositiveStringModel.loadSettingsFrom(settings);
            m_foundColumnNegativeStringModel.loadSettingsFrom(settings);
        } else { // Pre-4.7 workflow -- load default behaviour (so that the node behaves like before the update)
            m_stringMatchBehaviourModel.setStringValue(StringMatchBehaviour.EXACT.toString());
            m_stringCaseSensitiveMatchingModel.setBooleanValue(true);
            m_appendFoundColumnModel.setBooleanValue(false);
            m_foundColumnPositiveStringModel.setStringValue("found");
            m_foundColumnNegativeStringModel.setStringValue("not found");
        }
    }
}
