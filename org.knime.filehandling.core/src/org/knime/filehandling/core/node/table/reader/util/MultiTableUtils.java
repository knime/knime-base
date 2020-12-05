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
 *   Jan 16, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.util.UniqueNameGenerator;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Utility class for dealing with {@link TableReader TableReaders}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class MultiTableUtils {

    private MultiTableUtils() {
        // static utility class
    }

    /**
     * Extracts a string to be stored in the TableSpecConfig from a given root.
     *
     * @param <I> the type of item
     * @param rootItem the item to extract the String from
     * @return the String extracted from <b>rootItem</b>
     */
    public static <I> String extractString(final I rootItem) {
        if (rootItem instanceof FSPath) {
            return ((FSPath)rootItem).toFSLocation().getPath();
        } else {
            return rootItem.toString();
        }
    }

    /**
     * Retrieves the name from a {@link TypedReaderColumnSpec} after it is initialized i.e. its name must be present.
     *
     * @param spec to extract name from
     * @return the name of the spec
     * @throws IllegalStateException if the name is not present
     */
    public static String getNameAfterInit(final ReaderColumnSpec spec) {
        return spec.getName().orElseThrow(() -> new IllegalStateException(
            "Coding error. After initialization all column specs must be fully qualified."));
    }

    /**
     * Extracts the initialized names from a {@link ReaderTableSpec}.
     *
     * @param <T> the type of {@link ReaderColumnSpec} stored in the {@link ReaderTableSpec}
     * @param spec the {@link ReaderTableSpec}
     * @return the {@link Set} of names (actually a {@link LinkedHashSet}, so order is preserved)
     * @throws IllegalStateException if the names haven't been initialized
     */
    public static <T extends ReaderColumnSpec> Set<String> extractNamesAfterInit(final ReaderTableSpec<T> spec) {
        return spec.stream()//
            .map(MultiTableUtils::getNameAfterInit)//
            .collect(toCollection(LinkedHashSet::new));
    }

    /**
     * Assigns names to the columns in {@link TypedReaderTableSpec spec} if they don't contain a name already. The
     * naming scheme is Column0, Column1 and so on.
     *
     * @param spec {@link TypedReaderTableSpec} containing columns to assign names if they are missing
     * @return a {@link TypedReaderTableSpec} with the same types as {@link TypedReaderTableSpec spec} in which all
     *         columns are named
     */
    public static <T> TypedReaderTableSpec<T> assignNamesIfMissing(final TypedReaderTableSpec<T> spec) {
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(spec.stream()//
            .map(TypedReaderColumnSpec::getName)//
            .map(n -> n.orElse(null))//
            .filter(Objects::nonNull)//
            .collect(toSet()));
        return new TypedReaderTableSpec<>(IntStream.range(0, spec.size())
            .mapToObj(i -> assignNameIfMissing(i, spec.getColumnSpec(i), nameGen)).collect(Collectors.toList()));
    }

    private static <T> TypedReaderColumnSpec<T> assignNameIfMissing(final int idx, final TypedReaderColumnSpec<T> spec,
        final UniqueNameGenerator nameGen) {
        final Optional<String> name = spec.getName();
        if (name.isPresent()) {
            return spec;
        } else {
            return TypedReaderColumnSpec.createWithName(nameGen.newName(createDefaultColumnName(idx)), spec.getType(),
                spec.hasType());
        }
    }

    private static String createDefaultColumnName(final int iFinal) {
        return "Column" + iFinal;
    }


    public static <I> SourceGroup<String> toString(final SourceGroup<I> sourceGroup) {
        return new StringSourceGroup(sourceGroup);
    }

    private static class StringSourceGroup implements SourceGroup<String> {

        private final String m_id;

        private final List<String> m_items;

        StringSourceGroup(final SourceGroup<?> sourceGroup) {
            m_id = sourceGroup.getID();
            m_items = sourceGroup.stream().map(Object::toString).collect(toList());
        }

        @Override
        public Iterator<String> iterator() {
            return m_items.iterator();
        }

        @Override
        public String getID() {
            return m_id;
        }

        @Override
        public Stream<String> stream() {
            return m_items.stream();
        }

        @Override
        public int size() {
            return m_items.size();
        }

    }

}
