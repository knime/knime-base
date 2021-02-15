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
 *   Feb 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec.TypedReaderTableSpecBuilder;

/**
 * Serializer for {@link TableTransformation TableTransformations}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TableTransformationSerializer<T> {

    private static final String CFG_COLUMNS = "columns";

    private static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode";

    private static final String CFG_ENFORCE_TYPES = "enforce_types";

    private static final String CFG_KEEP_UNKNOWN_COLUMNS = "keep_unknown";

    private static final String CFG_POSITION_FOR_UNKNOWN_COLUMNS = "position_for_unknown";

    private static final String CFG_INTERSECTION_INDICES = "intersection_indices";

    private static final String CFG_NUM_COLUMNS = "num_columns";

    private final ColumnTransformationSerializer<T> m_columnTransformationSerializer;

    TableTransformationSerializer(final ColumnTransformationSerializer<T> columnTransformationSerializer) {
        m_columnTransformationSerializer = columnTransformationSerializer;
    }

    void save(final TableTransformation<T> tableTransformation, final NodeSettingsWO settings) {
        saveColumns(tableTransformation, settings.addNodeSettings(CFG_COLUMNS));
        settings.addBoolean(CFG_KEEP_UNKNOWN_COLUMNS, tableTransformation.keepUnknownColumns());
        settings.addInt(CFG_POSITION_FOR_UNKNOWN_COLUMNS, tableTransformation.getPositionForUnknownColumns());
        settings.addBoolean(CFG_ENFORCE_TYPES, tableTransformation.enforceTypes());
        settings.addString(CFG_COLUMN_FILTER_MODE, tableTransformation.getColumnFilterMode().name());

    }

    private void saveColumns(final TableTransformation<T> tableTransformation, final NodeSettingsWO settings) {
        final Set<TypedReaderColumnSpec<T>> intersection = tableTransformation.getRawSpec().getIntersection()//
            .stream()//
            .collect(toSet());
        final List<Integer> intersectionIndices = new ArrayList<>();
        int i = 0;
        for (ColumnTransformation<T> columnTransformation : tableTransformation) {
            if (intersection.contains(columnTransformation.getExternalSpec())) {
                intersectionIndices.add(i);
            }
            m_columnTransformationSerializer.save(columnTransformation, settings.addNodeSettings("" + i));
            i++;
        }
        settings.addInt(CFG_NUM_COLUMNS, tableTransformation.size());
        settings.addIntArray(CFG_INTERSECTION_INDICES, intersectionIndices.stream()//
            .mapToInt(Integer::intValue)//
            .toArray());
    }

    TableTransformation<T> load(final NodeSettingsRO settings, final boolean skipEmptyColumns)
        throws InvalidSettingsException {
        NodeSettingsRO columnSettings = settings.getNodeSettings(CFG_COLUMNS);
        final List<ImmutableColumnTransformation<T>> columns = loadColumnTransformations(columnSettings);
        final RawSpec<T> rawSpec = loadRawSpec(columns, columnSettings);
        final boolean keepUnknown = settings.getBoolean(CFG_KEEP_UNKNOWN_COLUMNS);
        final int positionForUnknown = settings.getInt(CFG_POSITION_FOR_UNKNOWN_COLUMNS);
        final boolean enforceTypes = settings.getBoolean(CFG_ENFORCE_TYPES);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings);
        return new ImmutableTableTransformation<>(columns, rawSpec, columnFilterMode, positionForUnknown, keepUnknown,
            enforceTypes, skipEmptyColumns);
    }

    private List<ImmutableColumnTransformation<T>> loadColumnTransformations(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final int num_columns = settings.getInt(CFG_NUM_COLUMNS);
        final List<ImmutableColumnTransformation<T>> columns = new ArrayList<>(num_columns);
        for (int i = 0; i < num_columns; i++) {
            columns.add(m_columnTransformationSerializer.load(settings.getNodeSettings("" + i)));
        }
        return columns;
    }

    private RawSpec<T> loadRawSpec(final List<ImmutableColumnTransformation<T>> columnTransformations,
        final NodeSettingsRO settings) throws InvalidSettingsException {
        final int[] intersectionIndices = settings.getIntArray(CFG_INTERSECTION_INDICES);
        final Set<Integer> intersection = Arrays.stream(intersectionIndices).boxed().collect(toSet());
        final TypedReaderTableSpecBuilder<T> unionBuilder = new TypedReaderTableSpecBuilder<>();
        final TypedReaderTableSpecBuilder<T> intersectionBuilder = new TypedReaderTableSpecBuilder<>();
        int i = 0;
        for (ColumnTransformation<T> columnTransformation : columnTransformations) {
            final TypedReaderColumnSpec<T> column = columnTransformation.getExternalSpec();
            unionBuilder.addColumn(column);
            if (intersection.contains(i)) {
                intersectionBuilder.addColumn(column);
            }
            i++;
        }
        return new RawSpec<>(unionBuilder.build(), intersectionBuilder.build());
    }

    private static ColumnFilterMode loadColumnFilterMode(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return ColumnFilterMode.valueOf(settings.getString(CFG_COLUMN_FILTER_MODE));
    }

}
