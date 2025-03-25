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
 *   Nov 28, 2024 (Tobias Kampmann): created
 */
package org.knime.time.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoice;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;

/**
 * A state provider that provides a list of all available locales as choices. The list is sorted by the English display
 * name of the locales, except for a few commonly-used locales (including the system default) that are moved to the top
 * of the list.
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public final class LocaleStateProvider implements StringChoicesProvider {

    private static final List<Locale> LOCALES_TO_SHOW_FIRST = List.of(Locale.getDefault());

    private static final List<Locale> LOCALES_TO_SHOW_SECOND = List.of( //
        Locale.US, //
        Locale.UK, //
        Locale.GERMANY //
    );

    @Override
    public void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();
    }

    @Override
    public List<StringChoice> computeState(final DefaultNodeSettingsContext context) throws WidgetHandlerException {
        List<Locale> sortedLocales = Arrays.stream(Locale.getAvailableLocales()) //
            .sorted(LocaleStateProvider::compareByEnglishTextRepresentation) //
            .collect(Collectors.toCollection(ArrayList::new)); // modifiable list

        // Move special locales to the front
        for (List<Locale> locales : List.of(LOCALES_TO_SHOW_SECOND, LOCALES_TO_SHOW_FIRST)) {
            sortedLocales.removeAll(locales);
            sortedLocales.addAll(0, locales);
        }

        return sortedLocales.stream() //
            .map(LocaleStateProvider::localeToStringChoice) //
            .toList();
    }

    private static int compareByEnglishTextRepresentation(final Locale l1, final Locale l2) {
        return l1.getDisplayName(Locale.ENGLISH).compareTo(l2.getDisplayName(Locale.ENGLISH));
    }

    private static StringChoice localeToStringChoice(final Locale locale) {
        return new StringChoice(locale.toLanguageTag(), locale.getDisplayName(Locale.ENGLISH));
    }
}
