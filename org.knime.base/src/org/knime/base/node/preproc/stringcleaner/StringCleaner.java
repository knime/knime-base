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
 *   7 Sept 2023 (jasper): created
 */
package org.knime.base.node.preproc.stringcleaner;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.CapitalizeAfterOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.ChangeCasingOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.PadOption;
import org.knime.base.node.preproc.stringcleaner.StringCleanerNodeSettings.ReplaceWithOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * Helper class that performs the string cleaning operations (e.g. removing whitespace / padding) for the
 * {@link StringCleanerNodeModel}. It is meant to be instantiated once (pipapo) per execution of the String Cleaner
 * node, since instantiation does need to compile some patterns, which can take up a lot of time if repeated e.g. for
 * every cell.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
final class StringCleaner {

    private static final int FLAGS = Pattern.UNICODE_CHARACTER_CLASS;

    private final UnaryOperator<String> m_cleaningFunction;

    private StringCleaner(final UnaryOperator<String> cleaningFunction) {
        m_cleaningFunction = cleaningFunction;
    }

    String clean(final String input) {
        return m_cleaningFunction.apply(input);
    }

    static StringCleaner fromSettings(final StringCleanerNodeSettings settings) throws InvalidSettingsException {
        validateSettings(settings);
        final var f = createCleaner(settings);
        return new StringCleaner(f);
    }

    private static UnaryOperator<String> createCleaner(final StringCleanerNodeSettings settings) {
        final var operations = new ArrayList<UnaryOperator<String>>();

        // cleaning
        appendOperationsToRemoveSpecialSequences(operations, settings);
        appendOperationsToRemoveCharacters(operations, settings);
        appendOperationsToHandleWhitespace(operations, settings);

        // manipulation
        appendOperationsToChangeCasing(operations, settings);
        appendOperationToPad(operations, settings);

        // combine the operations to one big function
        // shout-out to the UnaryOperator API for not providing UnaryOperator::andThen
        return operations.stream().reduce(UnaryOperator.identity(), (a, b) -> s -> b.apply(a.apply(s)));
    }

    private static void appendOperationsToHandleWhitespace(final List<UnaryOperator<String>> operations,
        final StringCleanerNodeSettings settings) {
        if (settings.m_removeAllWhitespace) {
            // just get rid of everything
            addReplacementOfPatternAsOperation(operations, "\\s", "");
        } else {
            // we don't combine these in one big regex to ensure the consecutive execution of operations
            if (settings.m_removeLeadingWhitespace) {
                addReplacementOfPatternAsOperation(operations, "^\\s+", "");
            }
            if (settings.m_removeTrailingWhitespace) {
                addReplacementOfPatternAsOperation(operations, "\\s+$", "");
            }
            if (settings.m_removeDuplicateWhitespace) {
                // replace multiple
                addReplacementOfPatternAsOperation(operations, "\\s{2,}",
                    r -> getReplacementForDuplicateWhitespace(r.group()));
            }
            // match \r\n together to only replace it by one space
            addReplacementOfPatternAsOperation(operations, "\\r?\\n|\\r", settings.m_replaceLinebreakStrategy);
            addReplacementOfPatternAsOperation(operations, "[\\s&&[^ \\r\\n]]",
                settings.m_replaceSpecialWhitespaceStrategy);
        }
    }

    private static String getReplacementForDuplicateWhitespace(final String match) {
        if (match.equals("\r\n")) {
            return match;
        } else if (match.contains("\r\n")) {
            return "\r\n";
        } else if (match.contains("\n")) {
            return "\n";
        } else {
            return " ";
        }
    }

    private static void appendOperationsToRemoveCharacters(final List<UnaryOperator<String>> operations,
        final StringCleanerNodeSettings settings) {
        final var charactersForRemoval = new StringBuilder();
        switch (settings.m_removeLettersCategory) {
            case ALL -> charactersForRemoval.append("\\p{L}");
            case LOWERCASE -> charactersForRemoval.append("\\p{Ll}");
            case UPPERCASE -> charactersForRemoval.append("\\p{Lu}");
            default -> {
                //no-op
            }
        }
        if (settings.m_removeNumbers) {
            charactersForRemoval.append("\\p{N}");
        }
        if (settings.m_removePunctuation) {
            charactersForRemoval.append("\\p{P}");
        }
        if (settings.m_removeSymbols) {
            charactersForRemoval.append("\\p{S}");
        }
        if (!settings.m_removeCustomCharactersString.isEmpty()) {
            charactersForRemoval.append(Pattern.quote(settings.m_removeCustomCharactersString));
        }

        if (!charactersForRemoval.isEmpty()) {
            addReplacementOfPatternAsOperation(operations, "[" + charactersForRemoval.toString() + "]", "");
        }
    }

    private static void appendOperationsToRemoveSpecialSequences(final List<UnaryOperator<String>> operations,
        final StringCleanerNodeSettings settings) {
        if (settings.m_removeAccents) {
            final var markPattern = Pattern.compile("\\p{M}", FLAGS);
            operations.add(s -> {
                // first decompose the string so that all accents/diacritics are separate characters
                final var decomposedString = Normalizer.normalize(s, Normalizer.Form.NFD);
                // then remove all mark characters
                return markPattern.matcher(decomposedString).replaceAll("");
            });
        }
        if (settings.m_removeNonASCII) {
            addReplacementOfPatternAsOperation(operations, "[^\\p{ASCII}]", "");
        }
        if (settings.m_removeNonPrintableChars) {
            // Remove Control, Format and Separator characters but not newlines or space.
            addReplacementOfPatternAsOperation(operations, "(?![\n\r ])[\\p{Cf}\\p{Cc}\\p{Z}]", "");
        }
    }

    private static void appendOperationsToChangeCasing(final List<UnaryOperator<String>> operations,
        final StringCleanerNodeSettings settings) {
        if (settings.m_changeCasing == ChangeCasingOption.UPPERCASE) {
            operations.add(s -> StringUtils.upperCase(s, Locale.ROOT));
        } else if (settings.m_changeCasing == ChangeCasingOption.LOWERCASE) {
            operations.add(s -> StringUtils.lowerCase(s, Locale.ROOT));
        } else if (settings.m_changeCasing == ChangeCasingOption.CAPITALIZE) {
            // first make everything lowercase, because that's our basis
            operations.add(s -> StringUtils.lowerCase(s, Locale.ROOT));
            final var capAfter = new ArrayList<String>();
            if (settings.m_changeCasingCapitalizeFirstCharacter) {
                capAfter.add("^");
            }
            switch (settings.m_changeCasingCapitalizeAfter) {
                case WHITESPACE -> capAfter.add("\\s");
                case NON_LETTERS -> capAfter.add("[^\\p{L}]");
                case CUSTOM -> {
                    if (!settings.m_changeCasingCapitalizeAfterCharacters.isEmpty()) {
                        // Add the set of characters that the user provided, but only if present
                        // (otherwise we'd get a PatternSyntaxException)
                        capAfter.add("[" + Pattern.quote(settings.m_changeCasingCapitalizeAfterCharacters) + "]");
                    }
                }
            }
            if (!capAfter.isEmpty()) { // should always be true,since we throw otherwise in #validateSettings()
                var lookbehind = "(?<=";
                lookbehind += capAfter.stream().collect(Collectors.joining("|"));
                lookbehind += ")";
                addReplacementOfPatternAsOperation(operations, lookbehind + "\\p{L}",
                    r -> r.group().toUpperCase(Locale.ROOT));
            }
        }
    }

    private static void appendOperationToPad(final List<UnaryOperator<String>> operations,
        final StringCleanerNodeSettings settings) {
        // Just delegate to StringUtils#(left|right)Pad
        switch (settings.m_pad) {
            case START -> operations
                .add(s -> StringUtils.leftPad(s, settings.m_padMinimumStringLength, settings.m_padFillCharacter));
            case END -> operations
                .add(s -> StringUtils.rightPad(s, settings.m_padMinimumStringLength, settings.m_padFillCharacter));
            default -> {
                // no-op
            }
        }
    }

    private static void addReplacementOfPatternAsOperation(final List<UnaryOperator<String>> ops, final String pattern,
        final ReplaceWithOption replacement) {
        switch (replacement) {
            case REMOVE -> addReplacementOfPatternAsOperation(ops, pattern, "");
            case REPLACE_WITH_STANDARDSPACE -> addReplacementOfPatternAsOperation(ops, pattern, " ");
            default -> {
                // no-op
            }
        }
    }

    private static void addReplacementOfPatternAsOperation(final List<UnaryOperator<String>> ops, final String pattern,
        final String replacement) {
        final var compiled = Pattern.compile(pattern, FLAGS);
        ops.add(s -> compiled.matcher(s).replaceAll(replacement));
    }

    private static void addReplacementOfPatternAsOperation(final List<UnaryOperator<String>> ops, final String pattern,
        final Function<MatchResult, String> replacement) {
        final var compiled = Pattern.compile(pattern, FLAGS);
        ops.add(s -> compiled.matcher(s).replaceAll(replacement));
    }

    /**
     * Validate the settings to that instantiation will not fail later
     *
     * @param settings
     * @throws InvalidSettingsException if the settings are not compatible with the string cleaner implementation
     */
    static void validateSettings(final StringCleanerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_pad == PadOption.START || settings.m_pad == PadOption.END) {
            CheckUtils.checkSetting(!settings.m_padFillCharacter.isEmpty(), "Fill character cannot be empty!");
        }
        if (settings.m_changeCasing == ChangeCasingOption.CAPITALIZE
            && settings.m_changeCasingCapitalizeAfter == CapitalizeAfterOption.CUSTOM) {
            CheckUtils.checkSetting(!settings.m_changeCasingCapitalizeAfterCharacters.isEmpty()
                || settings.m_changeCasingCapitalizeFirstCharacter, """
                        You chose to capitalize the string but defined no characters to capitalize after.
                        Use "lowercase" instead.
                        If you want to capitalize the output, enable \
                        "Capitalize character at the start of the string" or define some characters \
                        after which letters should be capitalized in "Capitalize after characters".""");
        }
    }

}
