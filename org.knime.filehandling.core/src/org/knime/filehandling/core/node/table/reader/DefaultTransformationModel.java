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
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Default implementation of a {@link TransformationModel} that holds the information for each column in a separate
 * tuple object.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external types
 */
public final class DefaultTransformationModel<T> implements TransformationModel<T> {

    private final Map<TypedReaderColumnSpec<T>, ColumnTransformation> m_transformations;

    private final TypedReaderTableSpec<T> m_rawSpec;

    /**
     * Constructor.
     *
     * @param rawSpec the raw {@link TypedReaderTableSpec}
     * @param productionPaths the {@link ProductionPath ProductionPaths} for type mapping
     * @param names contains the name for each column in the output
     * @param positions the position of each column in the output table
     * @param kept which columns are to be kept in the output
     */
    public DefaultTransformationModel(final TypedReaderTableSpec<T> rawSpec, final ProductionPath[] productionPaths,
        final String[] names, final Map<TypedReaderColumnSpec<T>, Integer> positions,
        final Set<TypedReaderColumnSpec<T>> kept) {
        m_rawSpec = rawSpec;
        m_transformations = new HashMap<>(rawSpec.size());
        for (int i = 0; i < rawSpec.size(); i++) {
            TypedReaderColumnSpec<T> columnSpec = rawSpec.getColumnSpec(i);
            m_transformations.put(columnSpec, new ColumnTransformation(productionPaths[i], kept.contains(columnSpec),
                positions.get(columnSpec), names[i]));
        }
    }

    /**
     * Constructor that keeps the column order and doesn't remove or rename any columns.
     *
     * @param rawSpec the raw {@link TypedReaderTableSpec}
     * @param productionPaths the {@link ProductionPath ProductionPaths} for type mapping
     */
    public DefaultTransformationModel(final TypedReaderTableSpec<T> rawSpec, final ProductionPath[] productionPaths) {
        this(rawSpec, //
            productionPaths, //
            rawSpec.stream().map(MultiTableUtils::getNameAfterInit).toArray(String[]::new), //
            IntStream.range(0, rawSpec.size())//
                .boxed()//
                .collect(toMap(rawSpec::getColumnSpec, Function.identity())), //
            rawSpec.stream().collect(toSet()));
    }

    @Override
    public TypedReaderTableSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    private ColumnTransformation getTransformation(final TypedReaderColumnSpec<T> column) {
        final ColumnTransformation transformation = m_transformations.get(column);
        CheckUtils.checkArgument(transformation != null, "No transformation for unknown column '%s' found.", column);
        return transformation;
    }

    @Override
    public ProductionPath getProductionPath(final TypedReaderColumnSpec<T> column) {
        return getTransformation(column).m_productionPath;
    }

    @Override
    public String getName(final TypedReaderColumnSpec<T> column) {
        return getTransformation(column).m_name;
    }

    @Override
    public boolean keep(final TypedReaderColumnSpec<T> column) {
        return getTransformation(column).m_keep;
    }

    @Override
    public int getPosition(final TypedReaderColumnSpec<T> column) {
        return getTransformation(column).m_position;
    }

    /**
     * Tuple holding the transformation for an individual column.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static final class ColumnTransformation {

        private final ProductionPath m_productionPath;

        private final boolean m_keep;

        private final int m_position;

        private final String m_name;

        ColumnTransformation(final ProductionPath productionPath, final boolean keep, final int position,
            final String name) {
            m_productionPath = productionPath;
            m_keep = keep;
            m_position = position;
            m_name = name;
        }
    }

}