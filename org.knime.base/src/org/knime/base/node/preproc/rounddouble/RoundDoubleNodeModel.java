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
 *   Jan 24, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.NumberMode;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputColumn;
import org.knime.base.node.preproc.rounddouble.RoundDoubleNodeSettings.OutputMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Model for the 'Number Rounder' node
 *
 * @author Kai Franze, KNIME GmbH, Germany
 */
@SuppressWarnings("restriction")
final class RoundDoubleNodeModel extends WebUISimpleStreamableFunctionNodeModel<RoundDoubleNodeSettings> {

    RoundDoubleNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, RoundDoubleNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final RoundDoubleNodeSettings modelSettings) throws InvalidSettingsException {
        // Configure
        final var numberColumns = ColumnSelectionUtil.getDoubleColumns(inSpec);
        final var targetColumns = Set.of(modelSettings.m_columnsToFormat.filter(numberColumns));
        final var isAppend = modelSettings.m_outputColumn == OutputColumn.APPEND;
        final var newColSpecs = createNewColSpecs(inSpec, targetColumns, isAppend, modelSettings.m_numberModeV2,
            modelSettings.m_outputMode, modelSettings.m_suffix);

        // Create cell factory
        final var roundingMode = RoundDoubleNodeSettings.getRoundingModeFromMethod(modelSettings.m_roundingMethod);
        final var colIndexToRound = getColIndexToRound(inSpec, targetColumns);
        final var cellFactory = new RoundDoubleCellFactory(modelSettings.m_precision, modelSettings.m_numberModeV2,
            roundingMode, modelSettings.m_outputMode, colIndexToRound, newColSpecs);

        // Create column re-arranger
        final var columnRearranger = new ColumnRearranger(inSpec);
        if (isAppend) {
            columnRearranger.append(cellFactory);
        } else {
            columnRearranger.replace(cellFactory, colIndexToRound);
        }
        return columnRearranger;
    }

    private static DataColumnSpec[] createNewColSpecs(final DataTableSpec inSpec, final Set<String> targetColumns,
        final boolean isAppend, final NumberMode numberMode, final OutputMode outputMode, final String suffix) {
        final Function<DataColumnSpec, String> specToName = inColumnSpec -> {
            if (isAppend) {
                final var columnName = (suffix != null) ? (inColumnSpec.getName() + suffix) : inColumnSpec.getName();
                return DataTableSpec.getUniqueColumnName(inSpec, columnName);
            }
            return inColumnSpec.getName();
        };
        return inSpec.stream() //
            .filter(Objects::nonNull) //
            .filter(inColumnSpec -> targetColumns.contains(inColumnSpec.getName())) //
            .map(inColumnSpec -> createNewColSpec(inColumnSpec, numberMode, outputMode, specToName)) //
            .toArray(DataColumnSpec[]::new);
    }

    private static DataColumnSpec createNewColSpec(final DataColumnSpec inColumnSpec, final NumberMode numberMode,
        final OutputMode outputMode, final Function<DataColumnSpec, String> specToName) {
        final var type = switch (outputMode) {
            case AUTO -> getDataTypeForAuto(inColumnSpec.getType(), numberMode);
            case DOUBLE -> DoubleCell.TYPE;
            case STANDARD_STRING, PLAIN_STRING, ENGINEERING_STRING -> StringCell.TYPE;
        };
        final var name = specToName.apply(inColumnSpec);
        return new DataColumnSpecCreator(name, type).createSpec();
    }

    /**
     * Automatically determines an output {@link DataType}. This has to be in sync with
     * {@link RoundDoubleCellFactory#inferOutputTypeFromInputType}.
     *
     * @return Defaults to {@code DoubleCell.TYPE} unless the input data type is {@code IntCell.TYPE},
     *         {@code LongCell.TYPE}, {@code BooleanCell.TYPE}.
     */
    private static DataType getDataTypeForAuto(final DataType inputType, final NumberMode numberMode) {
        if (numberMode == NumberMode.INTEGER || inputType.equals(IntCell.TYPE)) {
            return IntCell.TYPE;
        }
        if (inputType.equals(LongCell.TYPE)) {
            return LongCell.TYPE;
        }
        if (inputType.equals(BooleanCell.TYPE)) {
            return BooleanCell.TYPE;
        }
        return DoubleCell.TYPE;
    }

    private static int[] getColIndexToRound(final DataTableSpec inSpec, final Set<String> targetColumns) {
        return IntStream.range(0, inSpec.getNumColumns()) //
            .filter(idx -> targetColumns.contains(inSpec.getColumnSpec(idx).getName())) //
            .toArray();
    }

}
