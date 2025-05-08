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
package org.knime.time.node.manipulate.modifydate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * The web UI model for the Date Modifier node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ModifyDateNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<ModifyDateNodeSettings> {

    public ModifyDateNodeModel2(final WebUINodeConfiguration config) {
        super(config, ModifyDateNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final ModifyDateNodeSettings modelSettings) throws InvalidSettingsException {

        final String[] includeList = modelSettings.m_columnFilter.filter(inSpec.stream() //
            .filter(modelSettings.m_behaviourType::isCompatibleType) //
            .toList());
        return modelSettings.m_appendOrReplace.createRearranger(includeList, inSpec, (inCol, outColName) -> {
            // determine the data type of output
            var dataType = switch (modelSettings.m_behaviourType) {
                case REMOVE -> LocalTimeCellFactory.TYPE;
                case CHANGE -> inCol.spec().getType();
                case APPEND -> modelSettings.m_timeZone.isEmpty()//
                    ? LocalDateTimeCellFactory.TYPE //
                    : ZonedDateTimeCellFactory.TYPE;
            };
            var newSpec = new DataColumnSpecCreator(outColName, dataType).createSpec();

            return createCellFactory(modelSettings, newSpec, inCol.index());
        }, modelSettings.m_outputColumnSuffix, () -> {
        });
    }

    private static SingleCellFactory createCellFactory(final ModifyDateNodeSettings settings,
        final DataColumnSpec outputSpec, final int inputColumnIndex) {
        return switch (settings.m_behaviourType) {
            case APPEND -> new AppendDateCellFactory(outputSpec, inputColumnIndex, settings.m_localDate,
                settings.m_timeZone.orElse(null));
            case CHANGE -> new ChangeDateCellFactory(outputSpec, inputColumnIndex, settings.m_localDate);
            case REMOVE -> new RemoveDateCellFactory(outputSpec, inputColumnIndex);
        };
    }

    private static final class AppendDateCellFactory extends SingleCellFactory {
        private final int m_inColIndex;

        private final ZoneId m_newZone;

        private final LocalDate m_newDate;

        /**
         * @param newZone may be null if the output should be a LocalDateTime instead of a ZonedDateTime.
         */
        AppendDateCellFactory(final DataColumnSpec outSpec, final int inColIndex, final LocalDate newDate,
            final ZoneId newZone) {
            super(outSpec);
            m_inColIndex = inColIndex;
            m_newDate = newDate;
            m_newZone = newZone;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            if (!(cell instanceof LocalTimeValue)) {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
            var previouslocalTime = ((LocalTimeValue)cell).getLocalTime();

            if (m_newZone == null) {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(m_newDate, previouslocalTime));
            } else {
                return ZonedDateTimeCellFactory.create(ZonedDateTime.of(m_newDate, previouslocalTime, m_newZone));
            }
        }
    }

    private static final class ChangeDateCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        private final LocalDate m_newDate;

        ChangeDateCellFactory(final DataColumnSpec outSpec, final int inColIndex, final LocalDate newDate) {
            super(outSpec);
            m_inColIndex = inColIndex;
            m_newDate = newDate;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            if (cell instanceof LocalDateTimeValue ldt) {
                return LocalDateTimeCellFactory
                    .create(LocalDateTime.of(m_newDate, ldt.getLocalDateTime().toLocalTime()));
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return ZonedDateTimeCellFactory.create(ZonedDateTime.of(m_newDate, zdt.getZonedDateTime().toLocalTime(),
                    zdt.getZonedDateTime().getZone()));
            } else {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
        }
    }

    private static final class RemoveDateCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        RemoveDateCellFactory(final DataColumnSpec inSpec, final int inColIndex) {
            super(inSpec);
            m_inColIndex = inColIndex;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            if (cell instanceof LocalDateTimeValue ldt) {
                return LocalTimeCellFactory.create(ldt.getLocalDateTime().toLocalTime());
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return LocalTimeCellFactory.create(zdt.getZonedDateTime().toLocalTime());
            } else {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
        }
    }
}
