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
 *   Sep 14, 2022 (leonard.woerteler): created
 */
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.util.Arrays;
import java.util.function.Consumer;

import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;

/**
 * Hierarchy of supported types for CSV type guessing.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public enum CSVGuessableType {

    /** String is the root of the type hierarchy. */
    STRING(null, String.class) {
        @Override
        protected Consumer<String> getTester(final CSVTableReaderConfig cfg) {
            return str -> {};
        }
    },

    /** Double-precision floating-point numbers. */
    DOUBLE(String.class, Double.class) {
        @Override
        protected Consumer<String> getTester(final CSVTableReaderConfig cfg) {
            final var parser = new DoubleParser(cfg);
            return parser::parse;
        }
    },

    /** 64-bit integer values. */
    LONG(Double.class, Long.class) {
        @Override
        protected Consumer<String> getTester(final CSVTableReaderConfig cfg) {
            final var parser = new IntegerParser(cfg);
            return parser::parseLong;
        }
    },

    /** 32-bit integer values. */
    INTEGER(Long.class, Integer.class) {
        @Override
        protected Consumer<String> getTester(final CSVTableReaderConfig cfg) {
            final var parser = new IntegerParser(cfg);
            return parser::parseInt;
        }
    };

    /** Type hierarchy that only contains {@link #STRING}, used to skip type guessing. */
    private static final TreeTypeHierarchy<Class<?>, String> ONLY_STRING =
            TreeTypeHierarchy.builder(CSVGuessableType.STRING.createTypeTester(null)).build();

    /**
     * Creates a type hierarchy for type guessing using the number formats specified in the given configuration.
     *
     * @param cfg configuration
     * @return configured type hierarchy
     */
    public static TreeTypeHierarchy<Class<?>, String> createHierarchy(final CSVTableReaderConfig cfg) {
        final var builder = TreeTypeHierarchy.builder(CSVGuessableType.STRING.createTypeTester(cfg));
        Arrays.stream(CSVGuessableType.values())
                .skip(1)
                .forEach(tp -> builder.addType(tp.m_parent, tp.createTypeTester(cfg)));
        return builder.build();
    }

    /**
     * Singleton type hierarchy only containing {@link #STRING}.
     *
     * @return singleton type hierarchy
     */
    public static TreeTypeHierarchy<Class<?>, String> stringOnlyHierarchy() {
        return ONLY_STRING;
    }

    /** Parent in the type hierarchy. */
    private final Class<?> m_parent;
    /** Java class of the column type. */
    private final Class<?> m_javaClass;

    /**
     * Creates a detectable column type.
     *
     * @param parent parent in the type hierarchy
     * @param javaClass java class object
     */
    CSVGuessableType(final Class<?> parent, final Class<?> javaClass) {
        m_parent = parent;
        m_javaClass = javaClass;
    }

    /**
     * Creates a tester for this type that throws a {@link NumberFormatException} if the string can't be converted.
     *
     * @param cfg configuration for value formats
     * @return tester
     */
    protected abstract Consumer<String> getTester(CSVTableReaderConfig cfg);

    /**
     * Creates a type tester for this guessable type.
     *
     * @param cfg configuration for value formats
     * @return type tester
     */
    private TypeTester<Class<?>, String> createTypeTester(final CSVTableReaderConfig cfg) {
        final Consumer<String> tester = getTester(cfg);
        return TypeTester.createTypeTester(m_javaClass, s -> {
            try {
                tester.accept(s);
                return true;
            } catch (NumberFormatException ex) {
                return false;
            }
        });
    }
}
