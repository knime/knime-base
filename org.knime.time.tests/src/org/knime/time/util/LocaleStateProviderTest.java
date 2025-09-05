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
 *   5 Sept 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.time.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests for the {@link LocaleStateProvider}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class LocaleStateProviderTest {

    // test with a rather uncommon system default
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("en-DE");

    // our provider has some commonly used ones at the top, after the system default
    private static final Locale[] STD_LOCALES = new Locale[] { //
        Locale.US, //
        Locale.UK,  //
        Locale.GERMANY, //
        };

    // mock available locales including the ones that the state provider will also inject in another order
    private static final Locale[] AVAILABLE_LOCALES = Stream.concat(Arrays.stream(STD_LOCALES),
        Arrays.stream(new Locale[]{DEFAULT_LOCALE, Locale.forLanguageTag("nl-NL")})).toArray(Locale[]::new);

    @Test
    @SuppressWarnings("static-method")
    void testAvailableLocales() {
        final var expectedChoices = Arrays.stream(expectedLocales())
            .map(l -> new StringChoice(l.toLanguageTag(), l.getDisplayName(Locale.ENGLISH))).toList();
        try (final var mockedLSP = withMockedLocales()) {
            // provider currently does not use ctx
            final var actualChoices = new LocaleStateProvider().computeState(null);
            assertEquals(expectedChoices, actualChoices, "Expected to see the mocked locales");
        }
    }

    /**
     * @return expected locales in order of the state provider
     */
    private static Locale[] expectedLocales() {
        final var locales = new Locale[][] {
            new Locale[] { DEFAULT_LOCALE },
            STD_LOCALES,
            AVAILABLE_LOCALES
        };
        return Arrays.stream(locales).flatMap(Arrays::stream).distinct().toArray(Locale[]::new);
    }

    /**
     * Returns a provider with mocked available locales.
     *
     * @return provider with mocked available locales
     */
    // visible for other tests (mocking)
    @SuppressWarnings("resource") // ownership transferred to caller (via record)
    public static TestLocaleStateProvider withMockedLocales() {
        final var mock = Mockito.mockStatic(LocaleStateProvider.class, Mockito.CALLS_REAL_METHODS);
        mock.when(LocaleStateProvider::availableLocales).thenReturn(AVAILABLE_LOCALES);
        mock.when(LocaleStateProvider::defaultLocale).thenReturn(DEFAULT_LOCALE);
        return new TestLocaleStateProvider(mock, AVAILABLE_LOCALES);
    }

    /**
     * Helper enclosing the mocked static provider and the expected available locales.
     *
     * @param provider the mocked static provider
     * @param locales the available locales
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public record TestLocaleStateProvider(MockedStatic<LocaleStateProvider> provider, Locale[] locales)
        implements AutoCloseable {
        @Override
        public void close() {
            provider.close();
        }
    }

}
