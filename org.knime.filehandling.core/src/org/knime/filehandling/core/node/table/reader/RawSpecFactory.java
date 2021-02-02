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
 *   Sep 18, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Utility class that creates a {@link RawSpec} from multiple {@link TypedReaderTableSpec specs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The type used to identify external data types
 */
final class RawSpecFactory<T> {

    private final TypeHierarchy<T, T> m_typeHierarchy;

    /**
     * Constructor.
     *
     * @param typeHierarchy for merging specs
     */
    RawSpecFactory(final TypeHierarchy<T, T> typeHierarchy) {
        m_typeHierarchy = typeHierarchy;
    }

    RawSpec<T> create(final Collection<TypedReaderTableSpec<T>> specs) {
        final LinkedHashMap<String, TypeResolver<T, T>> typeResolvers = new LinkedHashMap<>();
        final Set<String> commonNames = specs.iterator().next().stream()//
                .map(MultiTableUtils::getNameAfterInit)//
                .collect(toSet());
        for (TypedReaderTableSpec<T> spec : specs) {
            final Set<String> currentIntersection = new HashSet<>(commonNames);
            for (TypedReaderColumnSpec<T> col : spec) {
                final String name = getNameAfterInit(col);
                currentIntersection.remove(name);
                final TypeResolver<T, T> typeResolver = typeResolvers
                    .computeIfAbsent(name, n -> m_typeHierarchy.createResolver());
                if (col.hasType()) {
                    typeResolver.accept(col.getType());
                }
            }
            // the columns remaining in currentIntersection were not part of the current spec
            // and therefore aren't part of the intersection of all columns
            commonNames.removeAll(currentIntersection);
        }
        final Collection<TypedReaderColumnSpec<T>> cols =
            typeResolvers.entrySet().stream().map(e -> TypedReaderColumnSpec.createWithName(e.getKey(),
                e.getValue().getMostSpecificType(), e.getValue().hasType())).collect(toList());
        final TypedReaderTableSpec<T> union = new TypedReaderTableSpec<>(cols);
        final TypedReaderTableSpec<T> intersection = new TypedReaderTableSpec<>(//
                cols.stream()//
                .filter(c -> commonNames.contains(getNameAfterInit(c)))//
                .collect(toList()));
        return new RawSpec<>(union, intersection);
    }

}
