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
 *   Aug 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;

/**
 * Default implementation of a {@link TableTransformation}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external types
 */
public final class DefaultTableTransformation<T> implements TableTransformation<T> {

    private final Map<TypedReaderColumnSpec<T>, ColumnTransformation<T>> m_transformations;

    private final RawSpec<T> m_rawSpec;

    private final ColumnFilterMode m_columnFilterMode;

    private final int m_unknownColumnPosition;

    private final boolean m_includeUnknownColumns;

    private final boolean m_enforceTypes;

    private final boolean m_skipEmptyColumns;

    /**
     * Constructor.
     *
     * @param rawSpec the {@link RawSpec}
     * @param transformations the transformations
     * @param columnFilterMode indicating which columns should be included
     * @param includeUnknownColumns flag indicating if new columns should be included or not
     * @param unknownColumnPosition the positions at which new columns should be inserted
     * @param enforceTypes indicates whether configured types should be enforced
     * @param skipEmptyColumns whether empty columns should be skipped
     */
    public DefaultTableTransformation(final RawSpec<T> rawSpec,
        final Collection<ColumnTransformation<T>> transformations, final ColumnFilterMode columnFilterMode,
        final boolean includeUnknownColumns, final int unknownColumnPosition, final boolean enforceTypes,
        final boolean skipEmptyColumns) {
        m_rawSpec = rawSpec;
        m_transformations = transformations.stream().collect(
            toMap(ColumnTransformation::getExternalSpec, Function.identity(), (l, r) -> l, LinkedHashMap::new));
        m_columnFilterMode = columnFilterMode;
        m_includeUnknownColumns = includeUnknownColumns;
        m_unknownColumnPosition = unknownColumnPosition;
        m_enforceTypes = enforceTypes;
        m_skipEmptyColumns = skipEmptyColumns;
    }

    @Override
    public RawSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean hasTransformationFor(final TypedReaderColumnSpec<T> column) {
        return m_transformations.containsKey(column);
    }

    @Override
    public ColumnTransformation<T> getTransformation(final TypedReaderColumnSpec<T> column) {
        final ColumnTransformation<T> transformation = m_transformations.get(column);
        CheckUtils.checkArgument(transformation != null, "No transformation for unknown column '%s' found.", column);
        return transformation;
    }

    @Override
    public int getPositionForUnknownColumns() {
        return m_unknownColumnPosition;
    }

    @Override
    public ColumnFilterMode getColumnFilterMode() {
        return m_columnFilterMode;
    }

    @Override
    public boolean keepUnknownColumns() {
        return m_includeUnknownColumns;
    }

    @Override
    public Stream<ColumnTransformation<T>> stream() {
        return m_transformations.values().stream().map(Function.identity());
    }

    @Override
    public Iterator<ColumnTransformation<T>> iterator() {
        return stream().iterator();
    }

    @Override
    public boolean enforceTypes() {
        return m_enforceTypes;
    }

    @Override
    public boolean skipEmptyColumns() {
        return m_skipEmptyColumns;
    }

}