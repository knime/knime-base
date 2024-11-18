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
 *   Nov 18, 2024 (david): created
 */
package org.knime.time.node.convert.durationtonumber;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.convert.durationtonumber.DurationToNumberNodeSettings.AllowedUnits;
import org.knime.time.node.convert.durationtonumber.DurationToNumberNodeSettings.RoundingBehaviour;
import org.knime.time.util.ReplaceOrAppend;

/**
 * New node model for the node that converts durations to numbers.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DurationToNumberNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<DurationToNumberNodeSettings> {

    /**
     * @param configuration
     */
    public DurationToNumberNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, DurationToNumberNodeSettings.class);
    }

    /**
     * Get the column names that are to be processed during node execution.
     *
     * @param inputSpec
     * @param settings
     * @return
     */
    private static List<String> getInputColumnNames(final DataTableSpec inputSpec,
        final DurationToNumberNodeSettings settings) {

        var compatibleColumns = inputSpec.stream() //
            .filter(colSpec -> colSpec.getType().isCompatible(DurationValue.class)) //
            .map(DataColumnSpec::getName) //
            .toArray(String[]::new);

        return Arrays.asList(settings.m_filter.getSelected(compatibleColumns, inputSpec));
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inputSpec,
        final DurationToNumberNodeSettings modelSettings) throws InvalidSettingsException {

        var columnRearranger = new ColumnRearranger(inputSpec);
        var inputColumnNames = getInputColumnNames(inputSpec, modelSettings);
        var uniqueNameGenerator = new UniqueNameGenerator(inputSpec);

        if (modelSettings.m_appendOrReplaceColumn == ReplaceOrAppend.APPEND) {
            for (String inputName : inputColumnNames) {
                columnRearranger.append(new DurationToNumberCellFactory( //
                    inputSpec.findColumnIndex(inputName), //
                    modelSettings.m_roundingBehaviour, //
                    uniqueNameGenerator.newName(inputName + modelSettings.m_suffix), //
                    modelSettings.m_unit //
                ));
            }
        } else {
            for (String inputName : inputColumnNames) {
                columnRearranger.replace(new DurationToNumberCellFactory( //
                    inputSpec.findColumnIndex(inputName), //
                    modelSettings.m_roundingBehaviour, //
                    inputName, //
                    modelSettings.m_unit //
                ), inputName);
            }
        }

        return columnRearranger;
    }

    static final class DurationToNumberCellFactory extends SingleCellFactory {

        private final RoundingBehaviour m_roundingBehaviour;

        private final AllowedUnits m_unit;

        private final int m_inputColumnIndex;

        DurationToNumberCellFactory( //
            final int inputColumnIndex, //
            final RoundingBehaviour roundingBehaviour, //
            final String newColumnName, //
            final AllowedUnits unit //
        ) {
            super(createNewColumnSpec(newColumnName, roundingBehaviour));

            this.m_roundingBehaviour = roundingBehaviour;
            this.m_inputColumnIndex = inputColumnIndex;
            this.m_unit = unit;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var cell = row.getCell(m_inputColumnIndex);
            if (cell.isMissing()) {
                return cell;
            }

            var durationCell = (DurationValue)cell;

            return switch (m_roundingBehaviour) {
                case DOUBLE -> new DoubleCell(m_unit.getConversionExact(durationCell.getDuration()));
                case INTEGER -> new LongCell(m_unit.getConversionFloored(durationCell.getDuration()));
            };
        }

        private static DataColumnSpec createNewColumnSpec(final String newColumnName,
            final RoundingBehaviour roundingBehaviour) {

            return new DataColumnSpecCreator( //
                newColumnName, roundingBehaviour == RoundingBehaviour.DOUBLE ? DoubleCell.TYPE : LongCell.TYPE //
            ).createSpec();
        }
    }
}
