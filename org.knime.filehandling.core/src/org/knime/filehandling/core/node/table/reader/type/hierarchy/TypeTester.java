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
 *   Jan 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.type.hierarchy;

import java.util.function.Predicate;

/**
 * A TypeTester evaluates if a value can be converted into the type associated with the tester.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify data types
 * @param <V> the type of value to test
 */
public interface TypeTester<T, V> extends Predicate<V> {

    /**
     * Checks if the provided value <b>value</b> is compatible with the type associated with this tester.
     */
    @Override
    boolean test(final V value);

    /**
     * Returns the type associated with this tester.
     *
     * @return the type associated with this tester
     */
    T getType();

    /**
     * Creates a {@link TypeTester} that depending on <b>allowNull</b> accepts or rejects <code>null</code> values.
     *
     * @param type the type that <b>predicate</b> tests for
     * @param predicate that tests if a value can be converted to <b>type</b>
     * @param allowNull set to <code>false</code> if <code>null</code> values should be rejected
     * @return the created TypeTester
     */
    public static <T, V> TypeTester<T, V> createTypeTester(final T type, final Predicate<V> predicate,
        final boolean allowNull) {
        return new DefaultTypeTester<>(type, predicate, allowNull);
    }

    /**
     * Creates a {@link TypeTester} that accepts <code>null</code> as value.
     *
     * @param type the type that <b>predicate</b> tests for
     * @param predicate that tests if a value can be converted to <b>type</b>
     * @return the created TypeTester
     */
    public static <T, V> TypeTester<T, V> createTypeTester(final T type, final Predicate<V> predicate) {
        return new DefaultTypeTester<>(type, predicate);
    }
}