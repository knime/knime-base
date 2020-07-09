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
 *   Jan 30, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Enum of the available merge modes for combining multiple {@link TypedReaderTableSpec ReaderTableSpecs}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 */
public enum SpecMergeMode {

        /**
         * Fails if the specs differ in any way.
         */
        FAIL_ON_DIFFERING_SPECS {

            @Override
            <T> TypedReaderTableSpec<T> mergeSpecs(final Collection<TypedReaderTableSpec<T>> individualSpecs,
                final TypeHierarchy<T, T> typeHierarchy) {
                final Iterator<TypedReaderTableSpec<T>> iterator = individualSpecs.iterator();
                final Map<String, TypeResolver<T, T>> resolversByName = new LinkedHashMap<>();
                addAllColumnsInSpec(resolversByName, iterator.next(), typeHierarchy);
                while (iterator.hasNext()) {
                    final TypedReaderTableSpec<T> individualSpec = iterator.next();
                    CheckUtils.checkArgument(resolversByName.size() == individualSpec.size(),
                        "Specs have varying number of columns.");
                    for (TypedReaderColumnSpec<T> colSpec : individualSpec) {
                        final String name = MultiTableUtils.getNameAfterInit(colSpec);
                        final TypeResolver<T, T> resolver = resolversByName.get(name);
                        if (resolver == null) {
                            // resolver == null means that the column is not contained in the first spec
                            throw new IllegalArgumentException(
                                String.format("The column %s is not contained in all files.", name));
                        }
                        if (colSpec.hasType()) {
                            resolver.accept(colSpec.getType());
                        }
                    }
                }
                return toReaderTableSpec(resolversByName);
            }

        },

        /**
         * Takes the intersection of the specs i.e. only columns that appear in all files are present in the output.
         */
        INTERSECTION {
            @Override
            <T> TypedReaderTableSpec<T> mergeSpecs(final Collection<TypedReaderTableSpec<T>> individualSpecs,
                final TypeHierarchy<T, T> typeHierarchy) {
                final Iterator<TypedReaderTableSpec<T>> iterator = individualSpecs.iterator();
                final Map<String, TypeResolver<T, T>> resolversByName = new LinkedHashMap<>();
                addAllColumnsInSpec(resolversByName, iterator.next(), typeHierarchy);
                while (iterator.hasNext()) {
                    final TypedReaderTableSpec<T> individualSpec = iterator.next();
                    final Set<String> cols = new HashSet<>(resolversByName.keySet());
                    for (TypedReaderColumnSpec<T> colSpec : individualSpec) {
                        final String name = MultiTableUtils.getNameAfterInit(colSpec);
                        if (cols.remove(name) && colSpec.hasType()) {
                            resolversByName.get(name).accept(colSpec.getType());
                        }
                    }
                    // the columns remaining in cols were not part of the current spec
                    // and are therefore not part of the intersection
                    cols.forEach(resolversByName::remove);
                }
                CheckUtils.checkArgument(!resolversByName.isEmpty(), "The intersection of all specs is empty.");
                return toReaderTableSpec(resolversByName);
            }
        },

        /**
         * Takes the union of the specs i.e. any column that appears in any file is present in the output.
         */
        UNION {
            @Override
            <T> TypedReaderTableSpec<T> mergeSpecs(final Collection<TypedReaderTableSpec<T>> individualSpecs,
                final TypeHierarchy<T, T> typeHierarchy) {
                CheckUtils.checkArgument(!individualSpecs.isEmpty(), "No columns in input file(s).");
                final Map<String, TypeResolver<T, T>> resolversByName = new LinkedHashMap<>();
                for (TypedReaderTableSpec<T> individualSpec : individualSpecs) {
                    addAllColumnsInSpec(resolversByName, individualSpec, typeHierarchy);
                }
                return toReaderTableSpec(resolversByName);
            }
        };

    /**
     * Merges the provided {@link TypedReaderTableSpec specs} and resolves type conflicts using {@link TypeHierarchy
     * typeHierarchy}.
     *
     * @param individualSpecs the individual specs to merge
     * @param typeHierarchy to use for resolving type conflicts
     * @return the merged {@link TypedReaderTableSpec}
     */
    abstract <T> TypedReaderTableSpec<T> mergeSpecs(final Collection<TypedReaderTableSpec<T>> individualSpecs,
        TypeHierarchy<T, T> typeHierarchy);

    private static <T>
        TypedReaderTableSpec<T> toReaderTableSpec(final Map<String, TypeResolver<T, T>> resolversByName) {
        return new TypedReaderTableSpec<>(resolversByName.entrySet().stream().map(SpecMergeMode::createReaderColumnSpec)
            .collect(Collectors.toList()));
    }

    private static <T> TypedReaderColumnSpec<T>
        createReaderColumnSpec(final Entry<String, TypeResolver<T, T>> nameHierarchyEntry) {
        final T mostSpecificType = nameHierarchyEntry.getValue().getMostSpecificType();
        final boolean hasType = nameHierarchyEntry.getValue().hasType();
        return TypedReaderColumnSpec.createWithName(nameHierarchyEntry.getKey(), mostSpecificType, hasType);
    }

    private static <T> void addAllColumnsInSpec(final Map<String, TypeResolver<T, T>> resolversByName,
        final TypedReaderTableSpec<T> individualSpec, final TypeHierarchy<T, T> typeHierarchy) {
        for (TypedReaderColumnSpec<T> column : individualSpec) {
            final String name = MultiTableUtils.getNameAfterInit(column);
            final TypeResolver<T, T> resolver =
                resolversByName.computeIfAbsent(name, n -> typeHierarchy.createResolver());
            if (column.hasType()) {
                resolver.accept(column.getType());
            }
        }
    }

}