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
 *   Dec 2, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.format.datetimeformatmanager;

import static org.knime.time.node.format.StringToAlignedHTMLUtil.saveAlignment;

import java.time.DateTimeException;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataValue;
import org.knime.core.data.property.ValueFormatModel;
import org.knime.core.data.property.ValueFormatModelFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.time.node.format.StringToAlignedHTMLUtil;

/**
 *
 * @author Tobias Kampmann
 */
final class DateTimeDataValueFormatter implements ValueFormatModel {

    private static final String CONFIG_FORMAT = "dateTimeFormat";

    private static final String CONFIG_LOCALE = "usedLocale";

    private final String m_format;

    private final Locale m_locale;

    private final DateTimeFormatter m_formatter;

    private final AlignmentSuggestionOption m_alignment;

    DateTimeDataValueFormatter(final String format, final Locale locale, final AlignmentSuggestionOption alignment)
        throws InvalidSettingsException {
        m_format = format;
        m_locale = locale;
        m_alignment = alignment;

        try {
            m_formatter = DateTimeFormatter.ofPattern(format, locale).withChronology(Chronology.ofLocale(locale));
        } catch (DateTimeParseException e) {
            throw new InvalidSettingsException(
                "Invalid format string. Could not create the formatter for the column renderer: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPlaintext(final DataValue dataValue) { // NOSONAR - no good way to reduce the number of returns
        try {
            return getTemporalFromDataValue(dataValue) //
                .map(m_formatter::format) //
                .orElse("Unsupported DataValue type: " + dataValue.getClass().getName());

        } catch (DateTimeException e) { //NOSONAR
            return "Failed to format: " + dataValue.toString();
        }
    }

    @Override
    public String getHTML(final DataValue dataValue) {
        return StringToAlignedHTMLUtil.getHTML(getPlaintext(dataValue), m_alignment);

    }

    @Override
    public void save(final ConfigWO config) {
        config.addString(CONFIG_FORMAT, m_format);
        config.addString(CONFIG_LOCALE, m_locale.toLanguageTag());
        saveAlignment(config, m_alignment);

    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DateTimeDataValueFormatter fmt) {
            return m_format.equals(fmt.m_format) && m_locale.equals(fmt.m_locale) && m_alignment == fmt.m_alignment;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_format, m_locale, m_alignment);
    }

    /**
     * Factory implementation for the {@link DateTimeFormatManagerNodeFactory DateTimeFormatManager}
     */
    public static final class Factory implements ValueFormatModelFactory<DateTimeDataValueFormatter> {

        @Override
        public String getDescription() {
            return "Custom Date&Time Formatter";
        }

        @Override
        public DateTimeDataValueFormatter getFormatter(final ConfigRO config) throws InvalidSettingsException {
            final String format = config.getString(CONFIG_FORMAT);
            final Locale locale = Locale.forLanguageTag(config.getString(CONFIG_LOCALE));
            final var alignment = StringToAlignedHTMLUtil.loadAlignment(config);
            return new DateTimeDataValueFormatter(format, locale, alignment);
        }

        @Override
        public Class<DateTimeDataValueFormatter> getFormatterClass() {
            return DateTimeDataValueFormatter.class;
        }
    }

    private static Optional<Temporal> getTemporalFromDataValue(final DataValue dataValue) {
        if (dataValue instanceof LocalDateValue localDateValue) {
            return Optional.of(localDateValue.getLocalDate());
        } else if (dataValue instanceof LocalTimeValue localTimeValue) {
            return Optional.of(localTimeValue.getLocalTime());
        } else if (dataValue instanceof LocalDateTimeValue localDateTimeValue) {
            return Optional.of(localDateTimeValue.getLocalDateTime());
        } else if (dataValue instanceof ZonedDateTimeValue zonedDateTimeValue) {
            return Optional.of(zonedDateTimeValue.getZonedDateTime());
        }

        return Optional.empty();
    }
}
