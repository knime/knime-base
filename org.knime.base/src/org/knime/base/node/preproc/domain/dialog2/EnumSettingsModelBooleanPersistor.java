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
 *   26 Aug 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.domain.dialog2;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.node.parameters.persistence.NodeParametersPersistor;

/**
 * Persistor for an enum with exactly two values that is stored as if it were a {@link SettingsModelBoolean}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
abstract class EnumSettingsModelBooleanPersistor<E extends Enum<E>> implements NodeParametersPersistor<E> {

    private final String m_configKey;
    private final E m_trueValue;
    private final E m_falseValue;

    /**
     * Constructor for a new two-value enum persistor that is stored as boolean in the settings.
     *
     * @param configKey the config key to use for storing the boolean value
     * @param enumClass the enum class to persist values of
     * @param trueValue the enum value that is represented by {@code true} in the settings, the other value will be
     *            represented by {@code false}
     * @throws IllegalArgumentException if the given enum class does not have exactly two values
     */
    protected EnumSettingsModelBooleanPersistor(final String configKey, final Class<E> enumClass, final E trueValue) {
        m_configKey = configKey;
        m_trueValue = trueValue;
        // to be able to represent as boolean config, it must have exactly two values
        // runtime check is better than nothing...
        final var consts = enumClass.getEnumConstants();
        if (consts.length != 2) {
            final var names =
                String.join(", ", Arrays.stream(consts).map(Enum::name).toArray(String[]::new));
            throw new IllegalArgumentException("Enum class \"%s\" must have exactly two values, has: \"%s\""
                .formatted(enumClass.getSimpleName(), names));
        }
        m_falseValue = consts[0] == m_trueValue ? consts[1] : consts[0];
    }

    @Override
    public void save(final E obj, final NodeSettingsWO settings) {
        final var isTrueValue = obj == m_trueValue;
        new SettingsModelBoolean(m_configKey, isTrueValue).saveSettingsTo(settings);
    }

    @Override
    public E load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var model = new SettingsModelBoolean(m_configKey, true);
        model.loadSettingsFrom(settings);
        return model.getBooleanValue() ? m_trueValue : m_falseValue;
    }

    @Override
    public String[][] getConfigPaths() {
        return new String[][] { { m_configKey } };
    }

}
