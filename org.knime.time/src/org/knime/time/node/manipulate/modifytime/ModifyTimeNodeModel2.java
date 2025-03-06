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
package org.knime.time.node.manipulate.modifytime;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
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
final class ModifyTimeNodeModel2 extends WebUISimpleStreamableFunctionNodeModel<ModifyTimeNodeSettings> {

    public ModifyTimeNodeModel2(final WebUINodeConfiguration config) {
        super(config, ModifyTimeNodeSettings.class);
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final ModifyTimeNodeSettings modelSettings) throws InvalidSettingsException {

        final String[] includeList = modelSettings.m_columnFilter.getSelected(inSpec.stream() //
            .filter(modelSettings.m_behaviourType::isCompatibleType) //
            .map(DataColumnSpec::getName) //
            .toArray(String[]::new), inSpec);

        return modelSettings.m_appendOrReplace.createRearranger(includeList, inSpec, (inCol, outColName) -> {
            // determine the data type of output
            var dataType = switch (modelSettings.m_behaviourType) {
                case REMOVE -> LocalDateCellFactory.TYPE;
                case CHANGE -> inCol.spec().getType();
                case APPEND -> modelSettings.m_timeZone == null //
                    ? LocalDateTimeCellFactory.TYPE //
                    : ZonedDateTimeCellFactory.TYPE;
            };
            var newSpec = new DataColumnSpecCreator(outColName, dataType).createSpec();

            return createCellFactory(modelSettings, newSpec, inCol.index());
        }, modelSettings.m_outputColumnSuffix, () -> {
        });
    }

    private static SingleCellFactory createCellFactory(final ModifyTimeNodeSettings settings,
        final DataColumnSpec outputSpec, final int inputColumnIndex) {
        return switch (settings.m_behaviourType) {
            case APPEND -> new AppendTimeCellFactory(outputSpec, inputColumnIndex, settings.m_localTime,
                settings.m_timeZone);
            case CHANGE -> new ChangeTimeCellFactory(outputSpec, inputColumnIndex, settings.m_localTime);
            case REMOVE -> new RemoveTimeCellFactory(outputSpec, inputColumnIndex);
        };
    }

    private static final class AppendTimeCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        private final ZoneId m_newZone;

        private final LocalTime m_newTime;

        /**
         * @param newZone may be null if the output should be a LocalDateTime instead of a ZonedDateTime.
         */
        AppendTimeCellFactory(final DataColumnSpec outSpec, final int inColIndex, final LocalTime newTime,
            final ZoneId newZone) {
            super(outSpec);
            m_inColIndex = inColIndex;
            m_newTime = newTime;
            m_newZone = newZone;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            if (!(cell instanceof LocalDateValue)) {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
            var previousLocalDate = ((LocalDateValue)cell).getLocalDate();

            if (m_newZone == null) {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(previousLocalDate, m_newTime));
            } else {
                return ZonedDateTimeCellFactory.create(ZonedDateTime.of(previousLocalDate, m_newTime, m_newZone));
            }
        }
    }

    private static final class ChangeTimeCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        private final LocalTime m_newTime;

        ChangeTimeCellFactory(final DataColumnSpec outSpec, final int inColIndex, final LocalTime newTime) {
            super(outSpec);
            m_inColIndex = inColIndex;
            m_newTime = newTime;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_inColIndex);

            if (cell.isMissing()) {
                return cell;
            }

            if (cell instanceof LocalDateTimeValue ldt) {
                return LocalDateTimeCellFactory
                    .create(LocalDateTime.of(ldt.getLocalDateTime().toLocalDate(), m_newTime));
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return ZonedDateTimeCellFactory.create(ZonedDateTime.of(zdt.getZonedDateTime().toLocalDate(), m_newTime,
                    zdt.getZonedDateTime().getZone()));
            } else {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
        }
    }

    private static final class RemoveTimeCellFactory extends SingleCellFactory {

        private final int m_inColIndex;

        RemoveTimeCellFactory(final DataColumnSpec inSpec, final int inColIndex) {
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
                return LocalDateCellFactory.create(ldt.getLocalDateTime().toLocalDate());
            } else if (cell instanceof ZonedDateTimeValue zdt) {
                return LocalDateCellFactory.create(zdt.getZonedDateTime().toLocalDate());
            } else {
                throw new IllegalArgumentException("Unsupported cell type: " + cell.getClass());
            }
        }
    }
}
