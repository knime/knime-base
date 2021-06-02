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
 *   Apr 19, 2017 (marcel): created
 */
package org.knime.time.node.extract.datetime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;

import org.apache.commons.lang3.LocaleUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author Marcel Wiedenmann, KNIME.com, Konstanz, Germany
 */
final class ExtractDateTimeFieldsNodeModel extends AbstractExtractDateTimeFieldsNodeModel {

    /** Flag indicating whether the COMPAT (pre Java-9 JRE) locale provider has highest priority or not. */
    static final boolean USES_COMPAT;

    /** Mapping from COMPAT / JRE Locales without country/region to CLDR Locales with country/region. */
    static final Map<String, String> LOCALE_MAPPING;

    static {
        // init uses compat
        final String localeProviders = System.getProperty("java.locale.providers");
        USES_COMPAT = localeProviders != null && localeProviders.startsWith("COMPAT");

        // init locale mapping
        final HashMap<String, String> localeMapping = new HashMap<>();
        // ----------------------------
        // --- backwards compatible ---
        // ----------------------------
        localeMapping.put("is", "is-IS");
        localeMapping.put("el", "el-GR");
        localeMapping.put("ja", "ja-JP");
        localeMapping.put("et", "et-EE");
        // this is Bolivia and not Spain
        localeMapping.put("es", "es-BO");
        localeMapping.put("ms", "ms-SG");
        localeMapping.put("ko", "ko-KR");
        localeMapping.put("nl", "nl-NL");
        localeMapping.put("sr", "sr-RS");
        localeMapping.put("pl", "pl-PL");
        // this is ukraine and not russia
        localeMapping.put("ru", "ru-UA");
        localeMapping.put("th", "th-TH");
        localeMapping.put("sr-Latn", "sr-Latn-RS");
        localeMapping.put("fi", "fi-FI");
        localeMapping.put("ca", "ca-ES");
        localeMapping.put("hu", "hu-HU");
        localeMapping.put("fr", "fr-FR");
        localeMapping.put("da", "da-DK");
        // this is us and not gb
        localeMapping.put("en", "en-US");
        localeMapping.put("sv", "sv-SE");
        localeMapping.put("hr", "hr-HR");
        localeMapping.put("de", "de-DE");
        localeMapping.put("ar", "ar-EG");
        localeMapping.put("zh", "zh-CN");
        localeMapping.put("id", "id-ID");
        localeMapping.put("no", "no-NO");
        localeMapping.put("tr", "tr-TR");
        localeMapping.put("it", "it-IT");

        // ----------------------------
        // -- non compatible (names) --
        // ----------------------------

        localeMapping.put("uk", "uk-UA");
        localeMapping.put("he", "he-IL");
        localeMapping.put("sl", "sl-SI");
        localeMapping.put("cs", "cs-CZ");
        localeMapping.put("sk", "sk-SK");
        localeMapping.put("ro", "ro-RO");
        localeMapping.put("hi", "hi-IN");
        localeMapping.put("lt", "lt-LT");
        // this is not cv and not pt
        localeMapping.put("pt", "pt-CV");

        // ----------------------------
        // -- not compatible at all ---
        // ----------------------------

        // there is a time wise mapping to Bemba (Zambia), but we stick to Belarusian instead
        localeMapping.put("be", "be-BY");
        localeMapping.put("sq", "sq-AL");
        localeMapping.put("vi", "vi-VN");
        localeMapping.put("mt", "mt-MT");
        localeMapping.put("lv", "lv-LV");
        localeMapping.put("ga", "ga-IE");
        localeMapping.put("bg", "bg-BG");
        localeMapping.put("mk", "mk-MK");

        LOCALE_MAPPING = Collections.unmodifiableMap(localeMapping);

    }

    private final SettingsModelBoolean m_mapLocales = createMapLocalesModel();

    static SettingsModelBoolean createMapLocalesModel() {
        final SettingsModelBoolean model = new SettingsModelBoolean("map_locales", true) {
            @Override
            protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                // Backwards compatibility
                if (settings.containsKey(getConfigName())) {
                    super.validateSettingsForModel(settings);
                }
            }

            @Override
            protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (settings.containsKey(getConfigName())) {
                    super.loadSettingsForModel(settings);
                }
            }
        };
        // only enable the model in case that compat is not used
        model.setEnabled(!USES_COMPAT);
        return model;
    }

    ExtractDateTimeFieldsNodeModel() {
        super(LocaleProvider.JAVA_8.localeToString(Locale.getDefault()));
    }

    @Override
    Optional<Locale> getLocale(final String selectedLocale) {
        return getLocale(selectedLocale, !USES_COMPAT && m_mapLocales.getBooleanValue());
    }

    static Optional<Locale> getLocale(String selectedLocale, final boolean mapLocales) {
        if (mapLocales) {
            selectedLocale = LOCALE_MAPPING.getOrDefault(selectedLocale, selectedLocale);
        }
        return LocaleProvider.JAVA_8.stringToLocale(selectedLocale);
    }

    @Override
    void saveLocale(final NodeSettingsWO settings, final SettingsModelString localeModel) {
        try {
            // conversion necessary for backwards compatibility (AP-8915)
            final Locale locale = LocaleUtils.toLocale(localeModel.getStringValue());
            localeModel.setStringValue(LocaleProvider.JAVA_8.localeToString(locale));
        } catch (IllegalArgumentException e) { // NOSONAR
            // do nothing, locale is already in correct format
        }
        localeModel.saveSettingsTo(settings);
        m_mapLocales.saveSettingsTo(settings);
    }

    @Override
    void loadLocale(final NodeSettingsRO settings, final SettingsModelString localeModel)
        throws InvalidSettingsException {
        localeModel.loadSettingsFrom(settings);
        try {
            // check for backwards compatibility (AP-8915)
            LocaleUtils.toLocale(localeModel.getStringValue());
        } catch (IllegalArgumentException e) { // NOSONAR
            try {
                final String iso3Country = Locale.forLanguageTag(localeModel.getStringValue()).getISO3Country();
                final String iso3Language = Locale.forLanguageTag(localeModel.getStringValue()).getISO3Language();
                if (iso3Country.isEmpty() && iso3Language.isEmpty()) {
                    throw new InvalidSettingsException("Unsupported locale '" + localeModel.getStringValue() + "'", e);
                }
            } catch (MissingResourceException ex) {
                throw new InvalidSettingsException(
                    "Unsupported locale '" + localeModel.getStringValue() + "': " + ex.getMessage(), ex);
            }
        }
        m_mapLocales.loadSettingsFrom(settings);
    }

    @Override
    void validateLocale(final NodeSettingsRO settings, final SettingsModelString localeModel)
        throws InvalidSettingsException {
        localeModel.validateSettings(settings);
        m_mapLocales.validateSettings(settings);
    }

}
