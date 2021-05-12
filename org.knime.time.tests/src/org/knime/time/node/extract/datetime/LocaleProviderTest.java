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
 *   May 12, 2021 (Mark Ortmann): created
 */
package org.knime.time.node.extract.datetime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 * Tests class for the {@link LocaleProvider}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public class LocaleProviderTest {

    /**
     * Tests the correctness of the {@link LocaleProvider#JAVA_11}.
     */
    @Test
    public void test_java_11_provider() {
        LocaleProvider prov = LocaleProvider.JAVA_11;
        testProvider(prov);
        for (final Locale l : prov.getLocales()) {
            assertFalse(l.getCountry().isEmpty());
        }
    }

    /**
     * Tests the correctness of the {@link LocaleProvider#JAVA_8}.
     */
    @Test
    public void test_java_8_provider() {
        LocaleProvider java8 = LocaleProvider.JAVA_8;
        testProvider(java8);
        for (final Locale l : java8.getLocales()) {
            if (l.getCountry().isEmpty()) {
                assertTrue(ExtractDateTimeFieldsNodeModel.LOCALE_MAPPING.containsKey(java8.localeToString(l)));
            } else {
                assertFalse(ExtractDateTimeFieldsNodeModel.LOCALE_MAPPING.containsKey(java8.localeToString(l)));
            }
        }
    }

    private static void testProvider(final LocaleProvider prov) {
        for (final Locale l : prov.getLocales()) {
            assertEquals(l, prov.stringToLocale(prov.localeToString(l)).orElseThrow());
        }
    }
}
