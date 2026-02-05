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
 *   Jan 2, 2026 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.tablecreator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.util.typemapping.TypeMappingUtils;
import org.knime.base.util.typemapping.TypeMappingUtils.ConverterException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.tablecreator.TableCreatorNodeParameters.ColumnParameters;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

@SuppressWarnings({"deprecation", "restriction"})
class TableCreator3NodeModel extends WebUINodeModel<TableCreator3NodeParameters> {

    TableCreator3NodeModel() {
        super(new PortType[0], new PortType[]{BufferedDataTable.TYPE}, TableCreator3NodeParameters.class);
    }

    @SuppressWarnings("removal")
    @Override
    protected void validateSettings(final TableCreator3NodeParameters settings) throws InvalidSettingsException {
        final var columns = settings.getColumns();
        validateNoDuplicateColumnNames(Arrays.stream(columns).map(ColumnParameters::getName).toArray(String[]::new));
        for (int colIdx = 0; colIdx < columns.length; colIdx++) {
            validateColumnParameters(columns[colIdx], colIdx);
        }

    }

    private static void validateNoDuplicateColumnNames(final String[] columnNames) throws InvalidSettingsException {
        Set<String> seen = new HashSet<>();
        for (String name : columnNames) {
            if (seen.contains(name)) {
                throw new InvalidSettingsException(
                    String.format("Duplicate column name '%s' found. Column names must be unique.", name));
            }
            seen.add(name);
        }
    }

    private static void validateColumnParameters(final ColumnParameters col, final int colIndex)
        throws InvalidSettingsException {
        ColumnNameValidationUtils.validateColumnName(col.getName(), invalidState -> { // NOSONAR complexity seems OK
            switch (invalidState) {
                case EMPTY:
                    return "The column at index %d has an empty name.".formatted(colIndex);
                case BLANK:
                    return "The column at index %d has a blank name.".formatted(colIndex);
                case NOT_TRIMMED:
                    return "The column \"%s\" at index %d has a name that starts or ends with whitespace."
                        .formatted(col.getName(), colIndex);
                default:
                    throw new IllegalStateException("Unknown invalid column name state: " + invalidState);
            }
        });
        final var values = col.getValues();
        final var type = col.getType();
        for (int rowIdx = 0; rowIdx < values.length; rowIdx++) {
            try {
                TypeMappingUtils.readDataCellFromString(type, values[rowIdx]);
            } catch (ConverterException e) {
                throw new InvalidSettingsException(createConversionExceptionMessage(col, rowIdx, e), e);
            }

        }
    }

    private static String createConversionExceptionMessage(final ColumnParameters col, final int rowIdx,
        final ConverterException e) {
        final var description = String.format("Could not convert value '%s' to type '%s' (Column: '%s', row index: %d)",
            col.getValue(rowIdx), col.getType(), col.getName(), rowIdx);
        final var message = e.getMessage();
        if (message == null) {
            return description + ".";
        }
        return description + ":\n" + message;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final TableCreator3NodeParameters modelSettings) throws InvalidSettingsException {
        return new PortObjectSpec[]{createSpec(modelSettings)};
    }

    private static DataTableSpec createSpec(final TableCreator3NodeParameters modelSettings)
        throws InvalidSettingsException {
        return new DataTableSpec(Arrays.stream(modelSettings.getColumns()).map(ColumnParameters::createColumnSpec)
            .toArray(DataColumnSpec[]::new));
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final TableCreator3NodeParameters modelSettings) throws Exception {

        final var outSpec = createSpec(modelSettings);

        final var containerSettings = DataContainerSettings.builder()//
            .withInitializedDomain(false) // no user-specified domain supported yet
            .withDomainUpdate(true) // add all the values to the domain, needed for correctness
            .withCheckDuplicateRowKeys(false) // Row keys are generated to be unique
            .build();
        BufferedDataContainer cont = exec.createDataContainer(outSpec, containerSettings);

        final var numRows = modelSettings.getNumRows();
        final var columns = modelSettings.getColumns();
        for (long i = 0; i < numRows; i++) {
            DataCell[] cells = new DataCell[columns.length];
            for (int colIdx = 0; colIdx < columns.length; colIdx++) {
                final var col = columns[colIdx];
                final var value = col.getValue(i);
                final var dataType = col.getType();
                try {
                    cells[colIdx] = TypeMappingUtils.readDataCellFromString(dataType, value);
                } catch (ConverterException e) {
                    throw new KNIMEException(createConversionExceptionMessage(col, (int)i, e), e);
                }
            }
            DataRow row = new DefaultRow(RowKey.createRowKey(i), cells);
            cont.addRowToTable(row);
            exec.checkCanceled();
        }
        cont.close();

        BufferedDataTable out = cont.getTable();
        return new BufferedDataTable[]{out};

    }

}
