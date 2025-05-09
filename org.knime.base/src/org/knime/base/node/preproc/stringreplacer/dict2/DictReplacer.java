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
 *   2 May 2023 (jasper): created
 */
package org.knime.base.node.preproc.stringreplacer.dict2;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.knime.base.node.preproc.stringreplacer.dict2.StringReplacerDictNodeSettings.MultipleMatchHandling;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;
import org.knime.core.data.DataRow;
import org.knime.core.data.StringValue;

/**
 * Wrapper of a pattern-replacement dictionary for the String Replacer (Dictionary)
 *
 * @param <K> the type of the patterns stored in the dictionary, e.g. {@link Pattern} or {@link String}
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
public abstract sealed class DictReplacer<K> permits StringReplacer, PatternReplacer {

    /**
     * A linked-list representation of the Pattern-Replacement pairs from the dictionary
     */
    private final List<Map.Entry<K, String>> m_dict;

    /**
     * The node settings of the String Replacer (Dictionary) node
     */
    protected final StringReplacerDictNodeSettings m_settings;

    DictReplacer(final StringReplacerDictNodeSettings modelSettings) {
        m_dict = new LinkedList<>();
        m_settings = modelSettings;
    }

    /**
     * Insert a new row containing a pattern and a replacement into the internal dictionary. If the pattern column is a
     * collection, all elements are inserted separately with the same replacement string.
     *
     * @param row the {@link DataRow} that contains the relevant cells
     * @param patternIndex the column index of the pattern cell
     * @param replacementIndex the column index of the replacement cell
     * @throws IllegalSearchPatternCellException if the pattern cell is flawed
     * @throws IllegalSearchPatternException if the pattern could not be compiled or is malformed
     * @throws IllegalReplacementCellException if the replacement cell is flawed
     * @throws IllegalReplacementException if the replacement could not be executed
     */
    void insertDictRow(final DataRow row, final int patternIndex, final int replacementIndex)
        throws IllegalSearchPatternCellException, IllegalSearchPatternException, IllegalReplacementCellException,
        IllegalReplacementException {
        var patternCol = row.getCell(patternIndex);
        if (patternCol.isMissing()) {
            throw new IllegalSearchPatternCellException("The pattern column contains a missing cell");
        }
        var replacement = row.getCell(replacementIndex);
        if (replacement.isMissing()) {
            throw new IllegalReplacementCellException("The replacement column contains a missing cell");
        }
        if (!patternCol.getType().isCompatible(StringValue.class)) {
            throw new IllegalSearchPatternCellException("The pattern column is not string-compatible.");
        }
        this.addToDictionary(((StringValue)patternCol).getStringValue(), replacement.toString());
    }

    static final class IllegalSearchPatternCellException extends Exception {

        private static final long serialVersionUID = -6741857197590713069L;

        IllegalSearchPatternCellException(final String message) {
            super(message);
        }

    }

    static final class IllegalReplacementCellException extends Exception {

        private static final long serialVersionUID = -6741857197590713069L;

        IllegalReplacementCellException(final String message) {
            super(message);
        }

    }

    /**
     * Process a string cell, i.e. search for the patterns in it and replace them
     *
     * @param input the string to process
     * @return the resulting string after all replacements have been made
     * @throws IllegalReplacementException if a replacement could not be executed
     */
    String process(final String input) throws IllegalReplacementException {
        var currentString = input;
        for (var pair : m_dict) {
            var newString = processSingleReplacement(pair.getKey(), currentString, pair.getValue());
            if (newString.isPresent()) {
                if (m_settings.m_multipleMatchHandling == MultipleMatchHandling.REPLACEFIRST) {
                    return newString.get();
                }
                currentString = newString.get();
            }
        }
        return currentString;
    }

    /**
     * Add a pattern-replacement pair to the dictionary
     *
     * @param patternAsString the pattern cell as a string
     * @param replacement the replacement string
     * @throws IllegalSearchPatternException if the pattern could not be compiled or is malformed
     */
    protected void addToDictionary(final String patternAsString, final String replacement)
        throws IllegalSearchPatternException {
        var pattern = compilePattern(patternAsString);
        var escapedReplacement = prepareReplacementString(replacement);
        m_dict.add(Map.entry(pattern, escapedReplacement));
    }

    /**
     * Prepare the replacement string to be inserted into the internal dictionary.
     *
     * @param replacement the replacement string
     * @return the processed replacement string
     */
    protected abstract String prepareReplacementString(String replacement);

    /**
     * Compile a pattern that can be used as a key in the internal dictionary from a string
     *
     * @param pattern The pattern as a string
     * @return the compiled pattern
     * @throws IllegalSearchPatternException if the pattern could not be compiled or is malformed
     */
    protected abstract K compilePattern(String pattern) throws IllegalSearchPatternException;

    /**
     * Find the pattern in the input string and replace it with the replacement. It depends on the node settings whether
     * all occurrences are replaced or just the whole string, case sensitivity, etc...
     *
     * @param pattern the pattern to search for
     * @param input the input string
     * @param replacement the replacement string
     * @return {@code Optional.of(String)} if a replacement took place, otherwise {@code Optional.empty()}
     * @throws IllegalReplacementException
     */
    protected abstract Optional<String> processSingleReplacement(final K pattern, final String input,
        final String replacement) throws IllegalReplacementException;
}
