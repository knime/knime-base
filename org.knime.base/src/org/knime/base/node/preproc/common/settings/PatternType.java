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
 *   23 Jun 2023 (carlwitt): created
 */
package org.knime.base.node.preproc.common.settings;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;

/**
 * Defines how to interpret a search pattern.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.3
 */
@SuppressWarnings("restriction")
public enum PatternType {
        /** No interpolation. */
        @Label("Literal")
        LITERAL,

        /** Supports meta characters {@code *} and {@code ?}. */
        @Label("Wildcard")
        WILDCARD,

        /** Java regular expressions. */
        @Label("Regular expression")
        REGEX;

    /** Recommended default setting. */
    public static final PatternType DEFAULT = LITERAL;

    /** Displayed in dialogs as title for controls. */
    public static final String OPTION_NAME = "Pattern type";

    /** Displayed in dialogs as help text on controls. */
    public static final String OPTION_DESCRIPTION = """
            Select the type of pattern which you want to use.
            <ul>
                <li><i>Literal</i> matches the pattern as is.</li>
                <li>
                    <i>Wildcard</i> matches <tt>*</tt> to zero or more arbitrary characters and matches
                    <tt>?</tt> to any single character.
                </li>
                <li>
                    <i>Regular expression</i>
                    matches using the full functionality of Java regular expressions, including back references
                    in the replacement text. See the
                    <a href="http://docs.oracle.com/javase/6/docs/api/java/util/regex/Pattern.html">Java API
                    </a> for details.
                </li>
            </ul>
            """;


    static Optional<PatternType> get(final String name) {
        if (LITERAL.name().equals(name)) {
            return Optional.of(LITERAL);
        } else if (WILDCARD.name().equals(name)) {
            return Optional.of(WILDCARD);
        } else if (REGEX.name().equals(name)) {
            return Optional.of(REGEX);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Condition to test whether the pattern type {@code LITERAL} is selected
     */
    public static class IsLiteralCondition extends OneOfEnumCondition<PatternType> {
        @Override
        public PatternType[] oneOf() {
            return new PatternType[]{LITERAL};
        }
    }

    /**
     * Condition to test whether the pattern type {@code WILDCARD} is selected
     */
    public static class IsWildcardCondition extends OneOfEnumCondition<PatternType> {
        @Override
        public PatternType[] oneOf() {
            return new PatternType[]{WILDCARD};
        }
    }

    /**
     * Condition to test whether the pattern type {@code REGEX} is selected
     */
    public static class IsRegexCondition extends OneOfEnumCondition<PatternType> {
        @Override
        public PatternType[] oneOf() {
            return new PatternType[]{REGEX};
        }
    }
}
