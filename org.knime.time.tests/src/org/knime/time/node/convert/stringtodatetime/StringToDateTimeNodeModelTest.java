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
 *   28 Mar 2023 (carlwitt): created
 */
package org.knime.time.node.convert.stringtodatetime;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class StringToDateTimeNodeModelTest {

    static final String[] INPUTS =  new String[]{
        // as output by ZonedDateTime::toOffsetDatetime::toString which can be used to generate ISO 8601 strings
        "2023-03-21T11:29:17.123456+02:00",
        "2023-03-21T11:29:17.123456Z",
        // standard examples
        "19990322+0100",
        "19990322",
        "1999-03-22+01:00",
        "1999-03-22T05:06:07.000[Europe/Paris]",
        "1999-03-22T05:06:07,000[Europe/Paris]",
        "1999-03-22 05:06:07.000[Europe/Paris]",
        "1999-03-22 05:06:07,000[Europe/Paris]",
        "1999-03-22T05:06:07.000",
        "1999-03-22T05:06:07,000",
        "1999-03-22 05:06:07.000",
        "1999-03-22 05:06:07,000",
        "1999-03-22T05:06:07",
        "1999-03-22T05:06:07Z",
        "1999-03-22T05:06:07Z",
        "1999-03-22 05:06:07Z",
        "1999-03-22 05:06:07Z",
        "1999-03-22T05:06:07.000Z",
        "1999-03-22T05:06:07,000Z",
        "1999-03-22 05:06:07.000Z",
        "1999-03-22 05:06:07,000Z",
        "1999-03-22T05:06:07.000+01:00",
        "1999-03-22T05:06:07,000+01:00",
        "1999-03-22 05:06:07.000+01:00",
        "1999-03-22 05:06:07,000+01:00",
        "1999-03-22T05:06:07+01",
        "1999-03-22 05:06:07+01",
        "1999-03-22T05:06:07+01:00",
        "1999-03-22 05:06:07+01:00",
        "1999-081+01:00",
        "1999-03-22T05:06:07.000+01:00[Europe/Paris]",
        "1999-03-22T05:06:07,000+01:00[Europe/Paris]",
        "1999-03-22 05:06:07.000+01:00[Europe/Paris]",
        "1999-03-22 05:06:07,000+01:00[Europe/Paris]",
        "1999-03-22T05:06:07+01:00[Europe/Paris]",
        "1999-03-22 05:06:07+01:00[Europe/Paris]",
    };

    /**
     * Make sure the node can guess some standard date formats.
     */
    @Test
    public void testGuessFormat() {
        for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.CHINA, Locale.US, Locale.ROOT}) {
            for (String input : INPUTS) {
                var format = StringToDateTimeNodeModel.guessFormat(input, locale);
                assertTrue("Should be able to guess format for input: %s under locale %s ".formatted(input, locale),
                    format.isPresent());
            }
        }
    }

}
