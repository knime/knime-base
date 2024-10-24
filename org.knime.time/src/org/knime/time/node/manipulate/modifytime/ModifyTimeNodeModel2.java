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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.modifytime;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.time.node.manipulate.modifytime.ModifyTimeNodeSettings.AppendOrReplace;
import org.knime.time.node.manipulate.modifytime.ModifyTimeNodeSettings.ModifySelect;

/**
 * The node model of the node which modifies time.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class ModifyTimeNodeModel2 extends SimpleStreamableFunctionNodeModel {


    private ModifyTimeNodeSettings m_settings = new ModifyTimeNodeSettings();

    private boolean m_hasValidatedConfiguration = false;


    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            throw new InvalidSettingsException("The node was not configured yet. Open the dialog.");
        }
        DataTableSpec in = inSpecs[0];
        ColumnRearranger r = createColumnRearranger(in);
        DataTableSpec out = r.createSpec();
        return new DataTableSpec[]{out};
    }

    /** {@inheritDoc} */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec) {
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);

        final String[] columnNames = m_settings.m_columnFilter.getSelectedFromFullSpec(inSpec);
        final int[] includeIndices = //
            Arrays.stream(columnNames) //
                .mapToInt(name -> inSpec.findColumnIndex(name)) //
                .toArray();

        // determine the data type of output
        DataType dataType;
        m_settings.m_modifySelect.equals(ModifySelect.REMOVE);
        switch (m_settings.m_modifySelect) {
            case REMOVE:
                dataType = LocalDateCellFactory.TYPE;
                break;
            case CHANGE:
                dataType = LocalDateTimeCellFactory.TYPE;
                break;
            default:
                if (true) {
                    dataType = ZonedDateTimeCellFactory.TYPE;
                } else {
                    dataType = LocalDateTimeCellFactory.TYPE;
                }
        }

        int i = 0;
        for (final String selectedColumn : columnNames) {

            if (inSpec.getColumnSpec(selectedColumn).getType().equals(ZonedDateTimeCellFactory.TYPE)
                && m_settings.m_modifySelect.equals(ModifySelect.CHANGE)) {
                dataType = ZonedDateTimeCellFactory.TYPE;
            }

            if (m_settings.m_appendOrReplace.equals(AppendOrReplace.REPLACE)) {
                final DataColumnSpecCreator dataColumnSpecCreator = new DataColumnSpecCreator(selectedColumn, dataType);
                final SingleCellFactory cellFac =
                    createCellFactory(dataColumnSpecCreator.createSpec(), includeIndices[i++], m_settings.m_timeZone);
                rearranger.replace(cellFac, selectedColumn);
            } else {
                final DataColumnSpec dataColSpec =
                    new UniqueNameGenerator(inSpec).newColumn(selectedColumn + m_settings.m_outputColumnSuffix, dataType);
                final SingleCellFactory cellFac =
                    createCellFactory(dataColSpec, includeIndices[i++], m_settings.m_timeZone);
                rearranger.append(cellFac);
            }
        }
        return rearranger;
    }

    private SingleCellFactory createCellFactory(final DataColumnSpec dataColSpec, final int index, final ZoneId zone) {
        switch (m_settings.m_modifySelect) {
            case APPEND:
                return new AppendTimeCellFactory(dataColSpec, index, zone);
            case CHANGE:
                return new ChangeTimeCellFactory(dataColSpec, index);
            default:
                return new RemoveTimeCellFactory(dataColSpec, index);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // passt scho
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
        m_hasValidatedConfiguration = true;
    }

    private final class AppendTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final ZoneId m_zone;

        AppendTimeCellFactory(final DataColumnSpec inSpec, final int colIndex, final ZoneId zone) {
            super(inSpec);
            m_colIndex = colIndex;
            m_zone = zone;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final LocalDateValue localDateCell = (LocalDateValue)cell;
            final LocalTime localTime = m_settings.m_timeParts;
            if (true) {
                return ZonedDateTimeCellFactory
                    .create(ZonedDateTime.of(LocalDateTime.of(localDateCell.getLocalDate(), localTime), m_zone));
            } else {
                return LocalDateTimeCellFactory.create(LocalDateTime.of(localDateCell.getLocalDate(), localTime));
            }
        }
    }

    private final class ChangeTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        ChangeTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            final LocalTime localTime = m_settings.m_timeParts;
            if (cell instanceof LocalDateTimeValue) {
                return LocalDateTimeCellFactory
                    .create(LocalDateTime.of(((LocalDateTimeValue)cell).getLocalDateTime().toLocalDate(), localTime));
            }
            return ZonedDateTimeCellFactory
                .create(ZonedDateTime.of(((ZonedDateTimeValue)cell).getZonedDateTime().toLocalDate(), localTime,
                    ((ZonedDateTimeValue)cell).getZonedDateTime().getZone()));
        }
    }

    private final class RemoveTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        RemoveTimeCellFactory(final DataColumnSpec inSpec, final int colIndex) {
            super(inSpec);
            m_colIndex = colIndex;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }
            if (cell instanceof LocalDateTimeValue) {
                return LocalDateCellFactory.create(((LocalDateTimeValue)cell).getLocalDateTime().toLocalDate());
            }
            return LocalDateCellFactory.create(((ZonedDateTimeValue)cell).getZonedDateTime().toLocalDate());
        }
    }
}
