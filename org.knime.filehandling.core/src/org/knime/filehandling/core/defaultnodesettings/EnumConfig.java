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
 *   Feb 22, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Encapsulates a default value as well as the supported values from an {@link Enum}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <E> the type of {@link Enum} to use
 */
public final class EnumConfig<E extends Enum<E>> {

    private final E m_defaultValue;

    private final EnumSet<E> m_supportedValues;

    private EnumConfig(final E defaultValue, final EnumSet<E> supportedValues) {
        m_defaultValue = defaultValue;
        m_supportedValues = supportedValues;
    }

    /**
     * Creates an {@link EnumConfig} with the default value <b>defaultValue</b> that supports all values of its
     * {@link Enum}.
     *
     * @param <E> the type of {@link Enum} to use
     * @param defaultValue the default value
     * @return a {@link EnumConfig} that has default value <b>defaultValue</b> supporting all values of its {@link Enum}
     */
    public static <E extends Enum<E>> EnumConfig<E> supportAll(final E defaultValue) {
        @SuppressWarnings("unchecked") // save because enums can't be subclassed
        final Class<E> enumClass = (Class<E>)defaultValue.getClass();
        return new EnumConfig<>(defaultValue, EnumSet.allOf(enumClass));
    }

    /**
     * Creates an {@link EnumConfig} with the provided default value that also supports the provided other values.
     *
     * @param <E> the type of {@link Enum} to use
     * @param defaultValue the default value
     * @param otherValues the other values that are supported (can contain <b>defaultValue</b> but doesn't have to)
     * @return an {@link EnumConfig} with default value <b>defaultValue</b> that also supports <b>otherValues</b>
     */
    @SafeVarargs // used to instantiate an EnumSet
    public static <E extends Enum<E>> EnumConfig<E> create(final E defaultValue, final E... otherValues) {
        final EnumSet<E> supportedValues = EnumSet.of(defaultValue, otherValues);
        return new EnumConfig<>(defaultValue, supportedValues);
    }

    /**
     * Creates an {@link EnumConfig} with the provided default value and the provided {@link Set} of supported values.
     * The {@link Set supportedvalues} may contain <b>defaultValue</b> but doesn't have to as it is supported anyway.
     *
     * @param <E> the type of {@link Enum} to use
     * @param defaultValue the default value
     * @param supportedValues the supported values (can contain <b>defaultValue</b> but doesn't have to)
     * @return an {@link EnumConfig} with default value <b>defaultValue</b> and the provided {@link Set} of supported
     *         values
     */
    public static <E extends Enum<E>> EnumConfig<E> create(final E defaultValue, final Set<E> supportedValues) {
        final EnumSet<E> values = EnumSet.of(defaultValue);
        values.addAll(supportedValues);
        return new EnumConfig<>(defaultValue, values);
    }

    /**
     * Checks if the provided value is supported.
     *
     * @param value to check for support
     * @return {@code true} if <b>value</b> is supported
     */
    public boolean isSupported(final E value) {
        return m_supportedValues.contains(value);
    }

    /**
     * Returns the default value.
     *
     * @return the default value
     */
    public E getDefaultValue() {
        return m_defaultValue;
    }

    /**
     * Returns an unmodifiable {@link Set} of supported values.
     *
     * @return an unmodifiable {@link Set} of supported values
     */
    public Set<E> getSupportedValues() {
        return Collections.unmodifiableSet(m_supportedValues);
    }

    /**
     * Returns the number of supported values.
     *
     * @return the number of supported values
     */
    public int getNumberOfSupportedValues() {
        return m_supportedValues.size();
    }

    /**
     * Creates a {@link Stream} of the supported values.
     *
     * @return a {@link Stream} of the supported values
     */
    public Stream<E> stream() {
        return m_supportedValues.stream();
    }
}
