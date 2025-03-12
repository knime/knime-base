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
package org.knime.time.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Enumeration to represent the different ways to format a duration/period to a string.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public enum DurationPeriodStringFormat {
        @Label(value = "ISO 8601", description = """
                Formats the durations using the ISO-8601 representation, e.g. \
                'P2Y3M5D'.
                """)
        ISO("ISO-8601 representation"), //
        @Label(value = "Whole words", description = """
                Formats the durations using words to represent them, e.g. '2 \
                years 3 months 5 days'.
                """)
        WORDS("Long representation"), //
        @Label(value = "Single letters", description = """
                Formats the durations using letters to represent them, e.g. \
                '2y 3M 5d' (Date-based duration: y: years, M: months, d: days; Time-based \
                duration: H: hours, m: minutes, s: seconds).
                """)
        LETTERS("Short representation");

    private final String m_oldConfigValue;

    DurationPeriodStringFormat(final String oldConfigValue) {
        this.m_oldConfigValue = oldConfigValue;
    }

    private static DurationPeriodStringFormat getByOldConfigValue(final String oldConfigValue) {
        return Arrays.stream(values()) //
            .filter(v -> v.m_oldConfigValue.equals(oldConfigValue)) //
            .findFirst() //
            .orElseThrow(() -> new IllegalArgumentException("No OutputFormat for old config value: " + oldConfigValue));
    }

    /**
     * A reference to a {@link DurationPeriodStringFormat} object that can be used in the {@link DefaultNodeSettings
     * settings class} for a given node.
     *
     * @see ValueReference
     */
    public interface Ref extends Reference<DurationPeriodStringFormat> {
    }

    /**
     * A persistor for {@link DurationPeriodStringFormat} objects that can be used in the {@link DefaultNodeSettings}
     * and can load legacy settings in a backwards-compatible way. Not recommended unless you actually need the
     * backwards compatibility.
     */
    public static final class LegacyPersistor implements NodeSettingsPersistor<DurationPeriodStringFormat> {

        private static final String CONFIG_KEY = "format";

        @Override
        public DurationPeriodStringFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var oldConfigValue = settings.getString(CONFIG_KEY);

            try {
                return DurationPeriodStringFormat.getByOldConfigValue(oldConfigValue);
            } catch (final IllegalArgumentException e) {
                var validValuesJoinedByComma = Arrays.stream(DurationPeriodStringFormat.values()) //
                    .map(v -> v.m_oldConfigValue) //
                    .collect(Collectors.joining(", "));

                throw new InvalidSettingsException("Invalid output format setting '%s'. Valid values are: %s"
                    .formatted(oldConfigValue, validValuesJoinedByComma), e);
            }
        }

        @Override
        public void save(final DurationPeriodStringFormat obj, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, obj.m_oldConfigValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }
}
