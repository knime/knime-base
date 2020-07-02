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
 *   Jul 1, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses double values from Strings.
 * Allows to specify the thousands and decimal separator.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DoubleParser {

    private String m_thousandsRegExpr = null;

    private Pattern m_thousandPattern = null;

    private char m_decimalSeparator = '.';

    DoubleParser(final CSVTableReaderConfig config) {
        final char thousandsSeparator = config.getThousandsSeparatorChar();
        if (thousandsSeparator != '\0') {
            m_thousandsRegExpr = Pattern.quote(Character.toString(thousandsSeparator));
        }
        m_decimalSeparator = config.getDecimalSeparatorChar();
        m_thousandPattern = Pattern.compile("(?i)[+-]?\\d{0,3}(?:" + m_thousandsRegExpr + "\\d{3})*(?:"
            + m_decimalSeparator + "\\d*)?(?:e[+-]?\\d+)?[fd]?");
    }

    double parse(final String value) {
        String data = value;
        // for numbers, trim data and accept empty tokens as missing
        // cells
        // remove thousands grouping
        if (m_thousandsRegExpr != null) {
            Matcher thousandMatcher = m_thousandPattern.matcher(data);
            if (thousandMatcher.matches()) {
                //Only continue processing if input is a valid number (wrong thousands separators are targeted to identify dates
                data = data.replaceAll(m_thousandsRegExpr, "");
            } else {
                throw new NumberFormatException("Double format didn't match.");
            }
        }

        // replace decimal separator with java separator '.'
        if (m_decimalSeparator != '.') {
            // we must reject tokens with a '.'.
            if (data.indexOf('.') >= 0) {
                throw new NumberFormatException(String.format(
                    "Detected '.' despite it not being the specified decimal separator ('%s').", m_decimalSeparator));
            }
            data = data.replace(m_decimalSeparator, '.');
        }
        return Double.parseDouble(data);
    }
}
