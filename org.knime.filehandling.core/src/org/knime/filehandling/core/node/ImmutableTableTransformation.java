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
 *   Dec 8, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;

/**
 * An immutable implementation of {@link TableTransformation}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external types
 */
public final class ImmutableTableTransformation<T> implements TableTransformation<T> {

    private final LinkedHashMap<TypedReaderColumnSpec<T>, ImmutableColumnTransformation<T>> m_transformations;

    private final RawSpec<T> m_rawSpec;

    private final ColumnFilterMode m_columnFilterMode;

    private final int m_unknownColumnPosition;

    private final boolean m_keepUnknownColumns;

    private final boolean m_enforceTypes;

    /**
     * Constructor.
     *
     * @param transformations a {@link Collection} of {@link ImmutableColumnTransformation}
     * @param rawSpec the {@link RawSpec}
     * @param columnFilterMode the {@link ColumnFilterMode}
     * @param unknownColumnPosition the position at which to insert unknown columns
     * @param keepUnknownColumns whether unknown columns should be kept
     * @param enforceTypes whether types should be enforced during execution
     */
    public ImmutableTableTransformation(final Collection<ImmutableColumnTransformation<T>> transformations,
        final RawSpec<T> rawSpec, final ColumnFilterMode columnFilterMode, final int unknownColumnPosition,
        final boolean keepUnknownColumns, final boolean enforceTypes) {
        m_transformations = transformations.stream().collect(//
            toMap(//
                ImmutableColumnTransformation::getExternalSpec, //
                Function.identity(), //
                (i, j) -> i, // never needed
                LinkedHashMap::new));
        m_rawSpec = rawSpec;
        m_columnFilterMode = columnFilterMode;
        m_unknownColumnPosition = unknownColumnPosition;
        m_keepUnknownColumns = keepUnknownColumns;
        m_enforceTypes = enforceTypes;
    }

    private ImmutableTableTransformation(final TableTransformation<T> toWrap) {
        m_transformations = new LinkedHashMap<>();
        for (ColumnTransformation<T> column : toWrap.stream().collect(Collectors.toList())) {
            m_transformations.put(column.getExternalSpec(), ImmutableColumnTransformation.copy(column));
        }
        m_rawSpec = toWrap.getRawSpec();
        m_columnFilterMode = toWrap.getColumnFilterMode();
        m_unknownColumnPosition = toWrap.getPositionForUnknownColumns();
        m_keepUnknownColumns = toWrap.keepUnknownColumns();
        m_enforceTypes = toWrap.enforceTypes();
    }

    /**
     * Creates an immutable copy of the provided {@link TableTransformation}.
     *
     * @param <T> the type used to identify external types
     * @param tableTransformation the {@link TableTransformation} to copy
     * @return a {@link ImmutableTableTransformation} copy of {@link TableTransformation tableTransformation}
     */
    public static <T> ImmutableTableTransformation<T> copy(final TableTransformation<T> tableTransformation) {
        return new ImmutableTableTransformation<>(tableTransformation);
    }

    @Override
    public boolean hasTransformationFor(final TypedReaderColumnSpec<T> column) {
        return m_transformations.containsKey(column);
    }

    @Override
    public ColumnTransformation<T> getTransformation(final TypedReaderColumnSpec<T> column) {
        return m_transformations.get(column);
    }

    @Override
    public Stream<ColumnTransformation<T>> stream() {
        return m_transformations.values().stream().map(Function.identity());
    }

    @Override
    public int getPositionForUnknownColumns() {
        return m_unknownColumnPosition;
    }

    @Override
    public boolean keepUnknownColumns() {
        return m_keepUnknownColumns;
    }

    @Override
    public boolean enforceTypes() {
        return m_enforceTypes;
    }

    @Override
    public ColumnFilterMode getColumnFilterMode() {
        return m_columnFilterMode;
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() == getClass()) {
            @SuppressWarnings("unchecked")
            final ImmutableTableTransformation<T> other = (ImmutableTableTransformation<T>)obj;
            return m_keepUnknownColumns == other.keepUnknownColumns() //
                && m_enforceTypes == other.enforceTypes() //
                && m_unknownColumnPosition == other.getPositionForUnknownColumns()//
                && m_columnFilterMode == other.getColumnFilterMode()//
                && m_rawSpec.equals(other.getRawSpec())//
                && m_transformations.equals(other.m_transformations);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()//
            .append(m_keepUnknownColumns)//
            .append(m_enforceTypes)//
            .append(m_unknownColumnPosition)//
            .append(m_columnFilterMode)//
            .append(m_rawSpec)//
            .append(m_transformations)//
            .toHashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder()//
            .append("{ColumnFilerMode: ")//
            .append(m_columnFilterMode)//
            .append(", keepUnknown: ")//
            .append(m_keepUnknownColumns)//
            .append(", unknownPos: ")//
            .append(m_unknownColumnPosition)//
            .append(", enforceTypes: ")//
            .append(m_enforceTypes)//
            .append(", RawSpec: ")//
            .append(m_rawSpec)//
            .append(", ColumnTransformations: ")//
            .append(m_transformations)//
            .append("}")//
            .toString();
    }
}
