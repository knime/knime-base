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
 *   25 May 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.time.format;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.knime.core.data.DataValue;
import org.knime.core.data.property.format.ValueFormatModel;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 * Defines a transformation from date&time to html.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public class ValueFormatModelDateTime implements ValueFormatModel {

    private final Locale m_locale;
    private final String m_pattern;
    private final DateTimeFormatter m_formatter;

    /**
     * @param locale
     * @param pattern
     */
    public ValueFormatModelDateTime(final Locale locale, final String pattern) {
        m_locale = locale;
        m_pattern = pattern;
        m_formatter = DateTimeFormatter.ofPattern(m_pattern, m_locale);
    }

    @Override
    public void save(final ConfigWO config) {
        Persistor.save(config, this);
    }

    public static ValueFormatModelDateTime load(final ConfigRO cfg) throws InvalidSettingsException {
        return Persistor.load(cfg);
    }

    @Override
    public String getHTML(final DataValue dv) {
        if (dv == null) {
            return "";
        }

        if (dv instanceof LocalDateValue localDate) {
            return m_formatter.format(localDate.getLocalDate());
        }
        // TODO rest of temporal types

        return "";
    }

    public class ValueFormatModelDateTimeSerializer extends ValueFormatModelSerializer<ValueFormatModelDateTime> {

        @Override
        public void save(final ValueFormatModelDateTime model, final ConfigWO config) {
            Persistor.save(config, model);

        }

        @Override
        public ValueFormatModelDateTime load(final ConfigRO config) throws InvalidSettingsException {
            return Persistor.load(config);
        }

    }

    static class Persistor {

        private static final String CONFIG_LOCALE = "locale";
        private static final String CONFIG_PATTERN = "pattern";

        public static void save(final ConfigBaseWO cfg, final ValueFormatModelDateTime fmt) {
            cfg.addString(CONFIG_LOCALE, fmt.m_locale.toLanguageTag());
            cfg.addString(CONFIG_PATTERN, fmt.m_pattern);
        }

        public static ValueFormatModelDateTime load(final ConfigBaseRO cfg) throws InvalidSettingsException {
            final var locale = Locale.forLanguageTag(cfg.getString(CONFIG_LOCALE));
            final var pattern = cfg.getString(CONFIG_PATTERN);
            return new ValueFormatModelDateTime(locale, pattern);
        }
    }

}
