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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.expressions.datetime;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.knime.expressions.core.ExpressionUtils;
import org.knime.expressions.core.exceptions.ScriptParseException;

/**
 * TestCase to test compilation of expressions.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany
 *
 */
final public class ExpressionParseTest {

    private final String[] m_invalidIdentifiers = new String[]{"function date(){}", "date(date = 1, 1, 1)",
        "date = date(1,1,1)", "and = date(1,1,1)", "let date = 1", "function a(){let date = 1}", "1date = a", "*date",
        "date\na", "a.date date", "date(date)", "date[]", "date{}", "date=5", "date\\", "function a column",
        "a.a column", "for(column; column < 2; column++)"};

    private final String[] m_validIdentifiers =
        new String[]{"function date1(){}", "date(date1 = 1, 1, 1)", "date1 = date(1,1,1)", "and1 = date(1,1,1)",
            "let date1 = 1", "function a(){let date1 = 1}", "function Date(){}", "date(Date = 1, 1, 1)",
            "Date = date(1,1,1)", "And = date(1,1,1)", "let Date = 1", "function a(){let Date = 1}", "a.date()",
            "a.date", "date(a.date)", "a.a column(1)", "for(column(1); a < 2; a++ )", "\"column\"", "\"column a\""};


    /**
     * Tests parsing of identifiers. Identifiers that are also function names are disallowed as we don't allow
     * overwriting of these variables.
     */
    @Test
    public void parseIdentifiersTest() {
        for (String expression : m_invalidIdentifiers) {
            try {
                ExpressionUtils.parseScript(expression, null, null, null);
                fail("Test passed even though it should fail. Expression: " + expression);
            } catch (ScriptParseException e) {
                // Expected
            }
        }

        for (String expression : m_validIdentifiers) {
            try {
                ExpressionUtils.parseScript(expression, null, null, null);

            } catch (ScriptParseException e) {
                fail(e.getMessage() + " Expression: " + expression);
            }
        }
    }
}
