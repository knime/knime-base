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
 *   15 Dec 2023 (jasper): created
 */
package org.knime.base.node.preproc.regexsplit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.util.CheckUtils;

/**
 * Parses capture groups (duh..), given a previously-compiled regular expression. This exists for the purpose of
 * extracting the number and names of the capture groups for the String Splitter (Regex) node.
 *
 * The parser works by reading the regular expression character by character and counting unescaped opening parentheses,
 * while accounting for an optional capture group name and ignoring constructs like look-aheads. This parser doesn't
 * count closing parentheses because the pattern must've been compiled before (e.g. with
 * {@link Pattern#compile(String)}), and this class relies on that the input pattern is a valid RegEx pattern.
 *
 * This parser is implemented w.r.t. the Java {@link Pattern} implementation of Regexs. For example, this implementation
 * does not implement conditionals, therefore they are not considered here. See
 * https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for the specifications of what is (and what
 * isn't) a capture group.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class CaptureGroupParser {

    /**
     * Abstracts a capture group in a regular expression
     *
     * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
     */
    record CaptureGroup(int index, Optional<String> name) {
        boolean isNamed() {
            return name.isPresent();
        }
    }

    /** The pattern in question */
    private final String m_patternString;

    /** The expected number of groups (See {@link Matcher#groupCount()}) */
    private final int m_expectedGroupCount;

    /** A list of parsed capture groups */
    private final List<CaptureGroup> m_captureGroups;

    /** The number of parsed capture groups */
    private int m_groupCount;

    /** State of the parser */
    private ParsingState m_state;

    /** Accumulates characters of a capture group name (only relevant if the cursor is within that name) */
    private StringBuilder m_currentCaptureGroupName;

    /** The cursor position in the pattern string */
    private int m_cursorPos;

    /** The character at the cursor position in the pattern string */
    private char m_cursor;

    private CaptureGroupParser(final Pattern p) {
        m_patternString = p.pattern();
        m_expectedGroupCount = p.matcher("").groupCount(); // dummy matcher to check num of groups
        m_captureGroups = new ArrayList<>(m_expectedGroupCount);
    }

    /**
     * Execute the parser on the given pre-compiled pattern
     *
     * @param p pattern in question
     * @return A list of capture groups, in order of occurrence. The group index starts at one.
     */
    static List<CaptureGroup> parse(final Pattern p) {
        return new CaptureGroupParser(p).doParse();
    }

    private enum ParsingState {
            START, // default state
            ESCAPE, // next character is escaped
            OP, // string reads …(  (Open Parenthesis)
            OP_QM, // string reads …(?  (Open Parenthesis _ Question Mark)
            OP_QM_LT, // string reads …(?<  (Open Parenthesis _ Question Mark _ Less Than)
            NAMED_CAPTURE; // we are in the name definition of a named capture
    }

    /** Perform the actual parsing */
    private List<CaptureGroup> doParse() {
        m_groupCount = 0;
        m_state = ParsingState.START;
        m_currentCaptureGroupName = null;
        for (m_cursorPos = 0; m_cursorPos < m_patternString.length(); ++m_cursorPos) {
            m_cursor = m_patternString.charAt(m_cursorPos);
            switch (m_state) {
                case START -> readStart();
                case ESCAPE -> readEscaped();
                case OP -> readOP();
                case OP_QM -> readOPQM();
                case OP_QM_LT -> readOPQMLT();
                case NAMED_CAPTURE -> readNamedCapture();
                default -> throw new IllegalStateException("Unexpected state: " + m_state);
            }
        }
        CheckUtils.checkState(m_groupCount == m_expectedGroupCount,
            "The number of groups is not the same as counted by the java implementation");
        CheckUtils.checkState(m_groupCount == m_captureGroups.size(),
            "The number of capture groups and the counter don't match");
        return m_captureGroups;
    }

    private void readStart() {
        if (m_cursor == '(') {
            // This might lead to a capture group definition
            m_state = ParsingState.OP;
        } else {
            m_state = defaultState();
        }
    }

    private void readEscaped() {
        // Between any special sequence (e.g. "(?:"), there can't be any extra characters, so this suffices
        m_state = ParsingState.START; // regardless of cursor
    }

    private void readOP() {
        if (m_cursor == '?') {
            // Transition from "(" to "(?"
            m_state = ParsingState.OP_QM;
        } else {
            // We have a capture group without a name
            m_groupCount++;
            m_captureGroups.add(new CaptureGroup(m_groupCount, Optional.empty()));
            m_state = defaultState();
        }
    }

    private void readOPQM() {
        if (m_cursor == '<') {
            // Transition from "(?" to "(?<"
            m_state = ParsingState.OP_QM_LT;
        } else {
            // Give up on capture group-parsing
            m_state = defaultState();
        }
    }

    private void readOPQMLT() {
        if (isAsciiAlpha(m_cursor)) {
            // Only if it starts with a letter, we've got a named capture group!
            m_currentCaptureGroupName = new StringBuilder(String.valueOf(m_cursor));
            m_state = ParsingState.NAMED_CAPTURE;
        } else {
            // This includes situations like "(?<!" or "(?<=" (look-behinds), but also any group names which
            // don't match [A-Za-z0-9]+
            m_state = defaultState();
        }
    }

    /**
     * Read the name of a named capture group, and validate that it only contains alphanumeric characters and the
     * definition ends with '>'
     */
    private void readNamedCapture() throws PatternSyntaxException {
        // currentNamedCaptureGroup should be initialised, since the only way here is via state OP_QM_LT
        CheckUtils.checkNotNull(m_currentCaptureGroupName,
            "Parsing Error: currentNamedCaptureGroup is not initialized");
        m_groupCount++;
        while (isAsciiAlphaNumeric(m_cursor)) {
            // Collect characters of capture group name
            Objects.requireNonNull(m_currentCaptureGroupName).append(m_cursor);
            m_cursorPos++; // Since the pattern has to have been successfully compiled before we enter this method, we
            // can't get any IOOB error here (because there has to be at least one closing parenthesis left)
            m_cursor = m_patternString.charAt(m_cursorPos); // advance cursor
        }
        if (m_cursor == '>') {
            // named capture definition complete
            // We need not worry about multiple capture groups with the same name because that is not valid in a Regex.
            m_captureGroups.add(new CaptureGroup(m_groupCount,
                Optional.of(Objects.requireNonNull(m_currentCaptureGroupName).toString())));
        } else {
            // This should never happen, since in this case we don't have a named capture group but then the
            // '?' in "(?<" would be a quantifier on the non-quantifiable opening parenthesis '(' (which is
            // not escaped). This means the Pattern should've never been successfully compiled.
            throw new PatternSyntaxException("The named capture group contains illegal characters.", m_patternString,
                m_cursorPos);
        }
        m_currentCaptureGroupName = null; // "Reset" StringBuilder so that there's no already registered CG stored
        m_state = defaultState();
    }

    /** Returns to either START or ESCAPE, depending on if the cursor is on a backslash (escape) character */
    private ParsingState defaultState() {
        return (m_cursor == '\\') ? ParsingState.ESCAPE : ParsingState.START;
    }

    private static boolean isAsciiAlphaNumeric(final char c) {
        final var isAlpha = isAsciiAlpha(c);
        final var isNumber = c >= '0' && c <= '9';
        return isAlpha || isNumber;
    }

    private static boolean isAsciiAlpha(final char c) {
        final var isUpper = c >= 'A' && c <= 'Z';
        final var isLower = c >= 'a' && c <= 'z';
        return isUpper || isLower;
    }

}
