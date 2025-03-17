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
 *   Jan 27, 2025 (david): created
 */
package org.knime.time.node.manipulate.modifytimezone;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.manipulate.modifytimezone.ModifyTimeZoneNodeSettings.BehaviourType;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ModifyTimeZoneNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<ModifyTimeZoneNodeSettings> {

    ModifyTimeZoneNodeModel2(final WebUINodeConfiguration config) {
        super(config, ModifyTimeZoneNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ModifyTimeZoneNodeSettings modelSettings) throws InvalidSettingsException {

        String[] includedColumns = modelSettings.m_columnFilter.filter(spec.stream() //
            .filter(modelSettings.m_behaviourType::isCompatibleType) //
            .toList());

        var outputDataType = modelSettings.m_behaviourType == BehaviourType.REMOVE //
            ? LocalDateTimeCellFactory.TYPE //
            : ZonedDateTimeCellFactory.TYPE;

        return modelSettings.m_appendOrReplace.createRearranger(includedColumns, spec, (inCol, outColName) -> {
            var outputSpec = new DataColumnSpecCreator(outColName, outputDataType).createSpec();
            return switch (modelSettings.m_behaviourType) {
                case SET -> new SetTimeZoneCellFactory(outputSpec, inCol.index(), modelSettings.m_timeZone);
                case SHIFT -> new ShiftTimeZoneCellFactory(outputSpec, inCol.index(), modelSettings.m_timeZone);
                case REMOVE -> new RemoveTimeZoneCellFactory(outputSpec, inCol.index());
            };
        }, modelSettings.m_outputColumnSuffix, () -> {
        });
    }

    private static final class SetTimeZoneCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        private final ZoneId m_zone;

        SetTimeZoneCellFactory(final DataColumnSpec outColSpec, final int inColIndex, final ZoneId zone) {
            super(outColSpec);
            m_inColIndex = inColIndex;
            m_zone = zone;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);
            if (cell.isMissing()) {
                return cell;
            }
            if (cell instanceof LocalDateTimeValue ldt) {
                return ZonedDateTimeCellFactory.create(ZonedDateTime.of(ldt.getLocalDateTime(), m_zone));
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(zdt.getZonedDateTime().toLocalDateTime(), m_zone));
            } else {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass().getName());
            }
        }
    }

    private static final class ShiftTimeZoneCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        private final ZoneId m_zone;

        ShiftTimeZoneCellFactory(final DataColumnSpec outColSpec, final int inColIndex, final ZoneId zone) {
            super(outColSpec);

            m_inColIndex = inColIndex;
            m_zone = zone;
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            try {
                return ZonedDateTimeCellFactory
                    .create(((ZonedDateTimeValue)cell).getZonedDateTime().toInstant().atZone(m_zone));
            } catch (DateTimeException ex) { // NOSONAR we don't need to relog this exception
                // if this is thrown, it means that the time zone shift is not possible, because
                // the resulting time exeeded the range of times that java can represent.
                return new MissingCell("Time zone shift was not possible: " + ex.getMessage());
            }
        }
    }

    private static final class RemoveTimeZoneCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        RemoveTimeZoneCellFactory(final DataColumnSpec outColSpec, final int inColIndex) {
            super(outColSpec);
            m_inColIndex = inColIndex;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            return LocalDateTimeCellFactory.create(((ZonedDateTimeValue)cell).getZonedDateTime().toLocalDateTime());
        }
    }
}
