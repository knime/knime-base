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

import java.util.Optional;
import java.util.regex.Pattern;

import org.knime.base.node.util.regex.RegexReplaceUtils;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalReplacementException;
import org.knime.base.node.util.regex.RegexReplaceUtils.IllegalSearchPatternException;

/**
 * Replacer dictionary implementation that has compiled RegEx patterns as lookup keys
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class PatternReplacer extends DictReplacer<Pattern> {

    /**
     * Creates a new PatternReplacer instance. Reads {@link StringReplacerDictNodeSettings#m_caseMatching} at
     * instantiation and sets RegEx flags accordingly.
     *
     * @param modelSettings the settings of the String Replacer (Dictionary) node instance
     */
    PatternReplacer(final StringReplacerDictNodeSettings modelSettings) {
        super(modelSettings);
    }

    @Override
    protected Pattern compilePattern(final String pattern) throws IllegalSearchPatternException {
        return RegexReplaceUtils.compilePattern( //
            pattern, //
            m_settings.m_patternType, //
            m_settings.m_caseMatching, //
            m_settings.m_enableEscaping //
        );
    }

    /**
     * Removes back-references from the replacement string if wildcard matching is enabled. {@inheritDoc}
     */
    @Override
    protected String prepareReplacementString(final String replacement) {
        return m_settings.m_useNewFixedWildcardBehavior
            ? RegexReplaceUtils.processReplacementString(replacement, m_settings.m_patternType) //
            : RegexReplaceUtils.processReplacementStringWithWildcardBackwardCompatibility(replacement,
                m_settings.m_patternType);
    }

    @Override
    protected Optional<String> processSingleReplacement(final Pattern pattern, final String input,
        final String replacement) throws IllegalReplacementException {
        return RegexReplaceUtils.doReplacement( //
            pattern, //
            m_settings.m_replacementStrategy, //
            m_settings.m_patternType, //
            input, //
            replacement //
        ).asOptional();
    }
}
