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
 *   Oct 21, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.GenericMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Takes a {@link RawSpec} and a {@link MultiTableReadConfig} and creates a {@link TableTransformation} compatible with
 * the {@link RawSpec} by either incorporating an existing {@link TableTransformation} or generating a new one.</br>
 * </br>
 * NOTE: If the old TransformationModel has {@link ColumnFilterMode#INTERSECTION} then all columns that are new to the
 * new intersection are considered to be new even if they were already part of the old union. Example:
 *
 * <pre>
 * Incoming TransformationModel:
 *      ColumnFilterMode: Intersection
 *      Old RawSpec: Union: [A, B, C, D], Intersection [B, C]
 *      Positions: [A:0, B:1,<unknown>, C:2, D:3]
 *      keep unknown: true
 *      unknown position: 2
 *
 * New RawSpec: Union: [A, B, C, D], Intersection: [A, B, C]
 *
 * New TransformationModel:
 *      Positions: [B:0, A:1, D:2, C:3]
 * </pre>
 *
 * Note how all positions change.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableTransformationFactory<I, T> {

    private static final String ENFORCE_TYPES_ERROR =
        "The column '%s' can't be converted to the configured data type '%s'. "
            + "Change the target type or uncheck the enforce types option to map it to a compatible type.";

    private final ProductionPathProvider<T> m_prodPathProvider;

    TableTransformationFactory(final ProductionPathProvider<T> productionPathProvider) {
        m_prodPathProvider = productionPathProvider;
    }

    TableTransformation<T> create(final RawSpec<T> rawSpec, final GenericMultiTableReadConfig<I, ?> config) {
        if (config.hasTableSpecConfig()) {
            final TableTransformation<T> configuredTransformationModel =
                config.getTableSpecConfig().getTransformationModel();
            return createFromExisting(rawSpec, configuredTransformationModel);
        } else {
            return createDefaultTransformationModel(rawSpec, config);
        }
    }

    private TableTransformation<T> createDefaultTransformationModel(final RawSpec<T> rawSpec,
        final GenericMultiTableReadConfig<I, ?> config) {
        // there is no TableSpecConfig (e.g. when the dialog was saved with a then invalid path)
        // so we need to fallback to the old SpecMergeMode if available or default to UNION
        @SuppressWarnings("deprecation")
        final ColumnFilterMode columnFilterMode = config.getSpecMergeMode() == SpecMergeMode.INTERSECTION
            ? ColumnFilterMode.INTERSECTION : ColumnFilterMode.UNION;
        if (columnFilterMode == ColumnFilterMode.INTERSECTION) {
            CheckUtils.checkArgument(rawSpec.getIntersection().size() > 0, "The intersection of all specs is empty.");
        }
        final TypedReaderTableSpec<T> union = rawSpec.getUnion();
        final List<ColumnTransformation<T>> transformations = new ArrayList<>(union.size());
        int idx = 0;
        final ColumnTransformationFactory transformationFactory = new ColumnTransformationFactory(true, true);
        for (TypedReaderColumnSpec<T> column : union) {
            transformations.add(transformationFactory.createNew(column, idx));
            idx++;
        }
        // defaulting enforceTypes to true is save because this transformation is only stored for the Table Manipulator
        // which is a new node in 4.3. Reader nodes don't store the transformation created here.
        return new DefaultTableTransformation<>(rawSpec, transformations, columnFilterMode, true,
            transformations.size(), true);
    }

    private int calculateNewPosForUnknown(final Collection<ColumnTransformation<T>> relevantTransformations,
        final int storedPositionForUnknown) {
        // relevantTransformations are sorted by position so we can iterate over them to find the new insert position
        int idx = 0;
        for (ColumnTransformation<T> transformation : relevantTransformations) {
            if (transformation.getPosition() >= storedPositionForUnknown) {
                return idx;
            }
            idx++;
        }
        return idx;
    }

    private TableTransformation<T> createFromExisting(final RawSpec<T> newRawSpec,
        final TableTransformation<T> existingModel) {
        final ColumnFilterMode colFilterMode = existingModel.getColumnFilterMode();
        // The columns that are potentially in the output i.e. the union or intersection of all columns
        // depending on the ColumnFilterMode
        final LinkedHashMap<String, TypedReaderColumnSpec<T>> relevantColumns =
            colFilterMode.getRelevantSpec(newRawSpec).stream().collect(//
                toMap(MultiTableUtils::getNameAfterInit, //
                    Function.identity(), //
                    (c1, c2) -> c1, // never used because the spec doesn't contain duplicates
                    LinkedHashMap::new));
        // The transformations for which a column is present in the new input
        // IMPORTANT: For ColumnFilterMode.INTERSECTION a transformation is also dropped if it is no longer
        // in the intersection but still in the union.
        final LinkedHashMap<String, ColumnTransformation<T>> relevantTransformations = colFilterMode
            .getRelevantSpec(existingModel.getRawSpec()).stream().map(existingModel::getTransformation)//
            .filter(t -> relevantColumns.containsKey(t.getOriginalName()))//
            .sorted()// sort by position in output
            .collect(
                toMap(ColumnTransformation::getOriginalName, Function.identity(), (t1, t2) -> t1, LinkedHashMap::new));

        // The position at which all unknown columns are inserted
        final int insertUnknownsAt =
            calculateNewPosForUnknown(relevantTransformations.values(), existingModel.getPositionForUnknownColumns());

        // All columns that are either new or are no longer relevant
        // (i.e. they dropped out of the intersection if ColumnFilterMode==INTERSECTION)
        final List<TypedReaderColumnSpec<T>> unknowns = newRawSpec.getUnion().stream()//
            .filter(e -> !relevantTransformations.containsKey(getNameAfterInit(e)))//
            .collect(toList());

        final boolean keepUnknownColumns = existingModel.keepUnknownColumns();
        final boolean enforceTypes = existingModel.enforceTypes();
        final ColumnTransformationFactory transformationFactory = new ColumnTransformationFactory(enforceTypes, keepUnknownColumns);
        final List<ColumnTransformation<T>> newTransformations = createColumnTransformations(relevantColumns,
            relevantTransformations, insertUnknownsAt, unknowns, transformationFactory);
        return new DefaultTableTransformation<>(newRawSpec, newTransformations, colFilterMode, keepUnknownColumns,
            insertUnknownsAt + unknowns.size(), enforceTypes);
    }

    private List<ColumnTransformation<T>> createColumnTransformations(
        final LinkedHashMap<String, TypedReaderColumnSpec<T>> relevantColumns,
        final LinkedHashMap<String, ColumnTransformation<T>> relevantTransformations, final int insertUnknownsAt,
        final List<TypedReaderColumnSpec<T>> unknowns, final ColumnTransformationFactory transformationFactory) {
        final List<ColumnTransformation<T>> newTransformations = new ArrayList<>();
        int idx = 0;
        final Iterator<ColumnTransformation<T>> existingTransformationIterator =
            relevantTransformations.values().iterator();
        // fill the new transformations up with the (updated) existing ones until we reach insertUnknownsAt
        for (; idx < insertUnknownsAt; idx++) {
            assert existingTransformationIterator.hasNext();
            final ColumnTransformation<T> existingTransformation = existingTransformationIterator.next();
            final TypedReaderColumnSpec<T> newSpec = relevantColumns.get(existingTransformation.getOriginalName());
            newTransformations.add(transformationFactory.adaptExisting(newSpec, existingTransformation, idx));
        }
        // insert all new ColumnTransformations
        for (TypedReaderColumnSpec<T> unknownColumn : unknowns) {
            newTransformations.add(transformationFactory.createNew(unknownColumn, idx));
            idx++;
        }
        // add the remaining (updated) old transformations
        for (; existingTransformationIterator.hasNext(); idx++) {
            final ColumnTransformation<T> existingTransformation = existingTransformationIterator.next();
            TypedReaderColumnSpec<T> newSpec = relevantColumns.get(existingTransformation.getOriginalName());
            newTransformations.add(transformationFactory.adaptExisting(newSpec, existingTransformation, idx));
        }
        return newTransformations;
    }

    private class ColumnTransformationFactory {

        private boolean m_enforceTypes;

        private boolean m_keepUnknownColumns;

        ColumnTransformationFactory(final boolean enforceTypes, final boolean keepUnknownColumns) {
            m_enforceTypes = enforceTypes;
            m_keepUnknownColumns = keepUnknownColumns;
        }

        ColumnTransformation<T> createNew(final TypedReaderColumnSpec<T> column, final int idx) {
            return new ImmutableColumnTransformation<>(column,
                m_prodPathProvider.getDefaultProductionPath(column.getType()), m_keepUnknownColumns, idx,
                getNameAfterInit(column));
        }

        ColumnTransformation<T> adaptExisting(final TypedReaderColumnSpec<T> column,
            final ColumnTransformation<T> transformation, final int idx) {
            final ProductionPath prodPath = determineProductionPath(column, transformation);
            return new ImmutableColumnTransformation<>(column, prodPath, transformation.keep(), idx,
                transformation.getName());
        }

        private ProductionPath determineProductionPath(final TypedReaderColumnSpec<T> column,
            final ColumnTransformation<T> transformation) {
            final T externalType = column.getType();
            if (externalType.equals(transformation.getExternalSpec().getType())) {
                // same external type, so it's save to use the configured production path
                return transformation.getProductionPath();
            } else if (m_enforceTypes) {
                final DataType configuredKnimeType =
                    transformation.getProductionPath().getConverterFactory().getDestinationType();
                // check if we can convert from the new external type to the configured knime type
                // if that's not possible fall back onto the default
                return m_prodPathProvider.getAvailableProductionPaths(externalType).stream()//
                    .filter(p -> p.getConverterFactory().getDestinationType().equals(configuredKnimeType))//
                    .findFirst()//
                    .orElseThrow(() -> new IllegalArgumentException(
                        String.format(ENFORCE_TYPES_ERROR, getNameAfterInit(column), configuredKnimeType)));
            } else {
                return m_prodPathProvider.getDefaultProductionPath(externalType);
            }
        }
    }

}
