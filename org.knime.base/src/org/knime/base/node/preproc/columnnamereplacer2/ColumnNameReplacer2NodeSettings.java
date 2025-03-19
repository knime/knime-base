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
package org.knime.base.node.preproc.columnnamereplacer2;

import org.knime.base.node.util.regex.CaseMatching;
import org.knime.base.node.util.regex.PatternType;
import org.knime.base.node.util.regex.ReplacementStrategy;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings for the Column Name Replacer node (formerly Column Rename (Regex)).
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ColumnNameReplacer2NodeSettings implements DefaultNodeSettings {

    @Widget(title = PatternType.OPTION_NAME, description = PatternType.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    PatternType m_patternType = PatternType.LITERAL;

    @Widget(title = CaseMatching.OPTION_NAME, description = CaseMatching.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    CaseMatching m_caseSensitivity = CaseMatching.CASESENSITIVE;

    @Widget(title = "Pattern", description = """
            A literal string, wildcard pattern or regular expression, depending on \
            the pattern type selected above.
            """)
    String m_pattern = "";

    @Widget(title = "Replacement text", description = """
            The replacement text for the pattern. If you are using a regular \
            expression, you may also use backreferences (e.g. $1 to refer to \
            the first capture group. Named capture groups can also be used with \
            (?&lt;group&gt;) and ${group} to refer to them).
            """)
    String m_replacement = "";

    @Widget(title = ReplacementStrategy.OPTION_NAME, description = ReplacementStrategy.OPTION_DESCRIPTION)
    @ValueSwitchWidget
    ReplacementStrategy m_replacementStrategy = ReplacementStrategy.WHOLE_STRING;
}
