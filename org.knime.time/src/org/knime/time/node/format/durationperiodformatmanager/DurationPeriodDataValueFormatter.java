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
 *   Jan 8, 2025 (david): created
 */
package org.knime.time.node.format.durationperiodformatmanager;

import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.data.DataValue;
import org.knime.core.data.property.ValueFormatModel;
import org.knime.core.data.property.ValueFormatModelFactory;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.time.node.format.StringToAlignedHTMLUtil;
import org.knime.time.util.DurationPeriodFormatUtils;
import org.knime.time.util.DurationPeriodStringFormat;

/**
 * A data value formatter for DurationPeriod values, which can be attached to a column to change how the format is
 * displayed in the table view.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class DurationPeriodDataValueFormatter implements ValueFormatModel {

    private static final String CFG_KEY_FORMAT = "format";

    private final DurationPeriodStringFormat m_format;

    private final AlignmentSuggestionOption m_alignment;

    DurationPeriodDataValueFormatter(final DurationPeriodStringFormat format,
        final AlignmentSuggestionOption alignment) {
        m_format = format;
        m_alignment = alignment;
    }

    @Override
    public String getPlaintext(final DataValue dataValue) {
        TemporalAmount value;

        if (dataValue instanceof DurationValue dv) {
            value = dv.getDuration();
        } else if (dataValue instanceof PeriodValue pv) {
            value = pv.getPeriod();
        } else {
            return "Unsupported DataValue type: " + dataValue.getClass().getName();
        }

        return switch (m_format) {
            case ISO -> dataValue.toString();
            case WORDS -> DurationPeriodFormatUtils.formatTemporalAmountLong(value);
            case LETTERS -> DurationPeriodFormatUtils.formatTemporalAmountShort(value);
        };
    }

    @Override
    public String getHTML(final DataValue dataValue) {
        var plainText = getPlaintext(dataValue);
        return StringToAlignedHTMLUtil.getHTML(plainText, m_alignment);
    }

    @Override
    public void save(final ConfigWO config) {
        config.addString(CFG_KEY_FORMAT, m_format.name());
        StringToAlignedHTMLUtil.saveAlignment(config, m_alignment);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DurationPeriodDataValueFormatter other) {
            return other.m_format == m_format && other.m_alignment == m_alignment;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_format, m_alignment);
    }

    @Override
    public String toString() {
        return "DurationPeriodDataValueFormatter{m_format=%s, m_additionalStyles=%s}" //
            .formatted(m_format, m_alignment);
    }

    /**
     * Factory used by KNIME to create instances of this data value formatter.
     */
    public static final class Factory implements ValueFormatModelFactory<DurationPeriodDataValueFormatter> {

        @Override
        public String getDescription() {
            return "Custom formatter for Duration/Period values";
        }

        @Override
        public DurationPeriodDataValueFormatter getFormatter(final ConfigRO config) throws InvalidSettingsException {
            var formatAsString = config.getString(CFG_KEY_FORMAT);
            DurationPeriodStringFormat format;
            try {
                format = DurationPeriodStringFormat.valueOf(formatAsString);
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException("Invalid format: '%s'. Valid formats are: %s".formatted( //
                    formatAsString, //
                    Arrays.stream(DurationPeriodStringFormat.values()) //
                        .map(DurationPeriodStringFormat::name) //
                        .collect(Collectors.joining(", ")) //
                ), e);
            }

            var alignment = StringToAlignedHTMLUtil.loadAlignment(config);

            return new DurationPeriodDataValueFormatter(format, alignment);
        }

        @Override
        public Class<DurationPeriodDataValueFormatter> getFormatterClass() {
            return DurationPeriodDataValueFormatter.class;
        }
    }
}
