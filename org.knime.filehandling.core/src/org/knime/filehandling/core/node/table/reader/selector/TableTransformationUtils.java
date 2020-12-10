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
 *   Oct 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Utility class for dealing with {@link TableTransformation TransformationModels}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TableTransformationUtils {

    private TableTransformationUtils() {
        // static utility class
    }

    /**
     * Extracts the {@link ColumnTransformation Transformations} that are actually part of the output table.</br>
     * The returned {@link ColumnTransformation Transformations} are sorted according to
     * {@link ColumnTransformation#compareTo(ColumnTransformation)}.
     *
     * @param <T> the type used to identify external data types
     * @param transformationModel from which to extract the transformations
     * @return the output relevant {@link ColumnTransformation Transformations} from the provided
     *         {@link TableTransformation} (in output order)
     */
    public static <T> List<ColumnTransformation<T>>
        getOutputTransformations(final TableTransformation<T> transformationModel) {
        return getOutputTransformationStream(transformationModel).collect(toList());
    }

    /**
     * Creates the {@link DataTableSpec} corresponding to the provided {@link TableTransformation}.
     *
     * @param <T> the type used to identify external data types
     * @param transformationModel specifying the output
     * @return the {@link DataTableSpec} corresponding to {@link TableTransformation transformationModel}
     */
    public static <T> DataTableSpec toDataTableSpec(final TableTransformation<T> transformationModel) {
        return new DataTableSpec(//
            getOutputTransformationStream(transformationModel)//
                .map(TableTransformationUtils::toDataColumnSpec)//
                .toArray(DataColumnSpec[]::new));
    }

    private static <T> TypedReaderTableSpec<T> getCandidates(final TableTransformation<T> transformationModel) {
        final RawSpec<T> rawSpec = transformationModel.getRawSpec();
        final TypedReaderTableSpec<T> candidates;
        switch (transformationModel.getColumnFilterMode()) {
            case INTERSECTION:
                candidates = rawSpec.getIntersection();
                break;
            case UNION:
                candidates = rawSpec.getUnion();
                break;
            default:
                throw new IllegalArgumentException(
                    "Unsupported ColumnFilterMode: " + transformationModel.getColumnFilterMode());
        }
        if (transformationModel.skipEmptyColumns()) {
            return new TypedReaderTableSpec<>(
                candidates.stream().filter(TypedReaderColumnSpec::hasType).collect(toList()));
        }
        return candidates;
    }

    /**
     * Extracts the relevant {@link ProductionPath ProductionPaths} from the provided {@link TableTransformation} and
     * returns them in order of the output.
     *
     * @param <T> the type used to identify external data types
     * @param transformationModel specifying the output {@link ProductionPath ProductionPaths}
     * @return the {@link ProductionPath ProductionPaths} in output order
     */
    public static <T> ProductionPath[] getOutputProductionPaths(final TableTransformation<T> transformationModel) {
        return getOutputTransformationStream(transformationModel)//
            .map(ColumnTransformation::getProductionPath)//
            .toArray(ProductionPath[]::new);
    }

    private static DataColumnSpec toDataColumnSpec(final ColumnTransformation<?> transformation) {
        final DataType type = transformation.getProductionPath().getConverterFactory().getDestinationType();
        return new DataColumnSpecCreator(transformation.getName(), type).createSpec();
    }

    private static <T> Stream<ColumnTransformation<T>>
        getOutputTransformationStream(final TableTransformation<T> transformationModel) {
        return getCandidates(transformationModel).stream()//
            .map(transformationModel::getTransformation)//
            .filter(ColumnTransformation::keep)//
            .sorted();
    }

    /**
     * Retrieves the original names of the columns in the output in output order.
     *
     * @param <T> the type used to identify external data types
     * @param transformationModel from which to extract the original names
     * @return the original names of the columns in the output in output order
     */
    public static <T> List<String> getOriginalOutputNames(final TableTransformation<T> transformationModel) {
        return getOutputTransformationStream(transformationModel)//
            .map(ColumnTransformation::getOriginalName)//
            .collect(toList());
    }

}
