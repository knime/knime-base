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
 *   Feb 7, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.NumberMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputColumn;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.RoundingMethod;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.RoundingMethod.Advanced;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;

/**
 * Persistors for backwards compatibility
 *
 * @author Kai Franze, KNIME GmbH, Germany
 */
@SuppressWarnings("restriction")
final class RoundDoublePersistors {

    private RoundDoublePersistors() {
        // Utility class
    }

    static final class OutputColumnPersistor extends NodeSettingsPersistorWithConfigKey<OutputColumn> {
        @Override
        public OutputColumn load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var isAppendColumns = settings.getBoolean(getConfigKey());
            return isAppendColumns ? OutputColumn.APPEND : OutputColumn.REPLACE;
        }

        @Override
        public void save(final OutputColumn outputColumn, final NodeSettingsWO settings) {
            settings.addBoolean(getConfigKey(), outputColumn == OutputColumn.APPEND);
        }
    }

    static final class RoundingMethodPersistor extends NodeSettingsPersistorWithConfigKey<RoundingMethod> {
        @Override
        public RoundingMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var roundingModeString = settings.getString(getConfigKey());
            return getRoundingMethodFromRoundingModeString(roundingModeString);
        }

        @Override
        public void save(final RoundingMethod roundingMethod, final NodeSettingsWO settings) {
            final var roundingMode = RoundDoubleNodeSettings.getRoundingModeFromMethod(roundingMethod);
            settings.addString(getConfigKey(), roundingMode.toString());
        }

        private static RoundingMethod getRoundingMethodFromRoundingModeString(final String roundingModeString)
            throws InvalidSettingsException {
            final var roundingMode = valueByT(RoundingMode.class, Enum::name, roundingModeString);
            return switch (roundingMode) {
                case CEILING -> new RoundingMethod(Advanced.TO_LARGER);
                case DOWN -> new RoundingMethod(Advanced.TOWARDS_ZERO);
                case FLOOR -> new RoundingMethod(Advanced.TO_SMALLER);
                case HALF_DOWN -> new RoundingMethod(Advanced.HALF_TOWARDS_ZERO);
                case HALF_EVEN -> new RoundingMethod(Advanced.HALF_TO_EVEN_DIGIT);
                case HALF_UP -> new RoundingMethod();
                case UP -> new RoundingMethod(Advanced.AWAY_FROM_ZERO);
                case UNNECESSARY -> throw new InvalidSettingsException("Will not round unnecessarily");
            };
        }
    }

    static final class OutputModePersistor extends NodeSettingsPersistorWithConfigKey<OutputMode> {
        @Override
        public OutputMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // For backwards compatibility with AP versions prior to 2.8
            if (!settings.containsKey(getConfigKey())) {
                final var isOutputAsString = settings.getBoolean("OutputAsString", false);
                return isOutputAsString ? OutputMode.STANDARD_STRING : OutputMode.DOUBLE;
            }
            return valueByT(OutputMode.class, OutputMode::getPersistKey, settings.getString(getConfigKey()));
        }

        @Override
        public void save(final OutputMode outputMode, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), outputMode.getPersistKey());
        }
    }

    static final class NumberModePersistor extends NodeSettingsPersistorWithConfigKey<NumberMode> {
        @Override
        public NumberMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return valueByT(NumberMode.class, NumberMode::getPersistKey, settings.getString(getConfigKey()));
        }

        @Override
        public void save(final NumberMode numberMode, final NodeSettingsWO settings) {
            settings.addString(getConfigKey(), numberMode.getPersistKey());
        }
    }

    private static <T, E extends Enum<E>> E valueByT(final Class<E> enumType, final Function<E, T> func, final T t)
        throws InvalidSettingsException {
        for (E e : enumType.getEnumConstants()) {
            if (func.apply(e).equals(t)) {
                return e;
            }
        }
        final var values = Arrays.stream(enumType.getEnumConstants()).map(func).map(Object::toString)
            .collect(Collectors.joining(", "));
        throw new InvalidSettingsException(
            String.format("Invalid value '%s'. Possible values: %s", t.toString(), values));
    }
}
