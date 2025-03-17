/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.nominal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.NominalValueRowSplitterNodeSettings;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * This is the model implementation of PossibleValueRowFilter. For a nominal column one or more possible values can be
 * selected. If the value in the selected column of a row matches the included possible values the row is added to the
 * included rows at first out port, else to the excluded at second outport.
 *
 *
 * @author KNIME GmbH
 * @since 5.3
 */
@SuppressWarnings("restriction")
final class NominalValueRowSplitterNodeModel extends WebUINodeModel<NominalValueRowSplitterNodeSettings> {

    private int m_selectedColIdx;

    private final Set<String> m_selectedAttr = new HashSet<String>();

    NominalValueRowSplitterNodeModel(final WebUINodeConfiguration config) {
        super(config, NominalValueRowSplitterNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final NominalValueRowSplitterNodeSettings settings) throws Exception {
        // include data container
        DataContainer positive = exec.createDataContainer(inData[0].getDataTableSpec());
        DataContainer negative = exec.createDataContainer(inData[0].getDataTableSpec());
        long currentRow = 0;
        for (DataRow row : inData[0]) {
            // if row matches to included...
            if (matches(row, settings)) {
                positive.addRowToTable(row);
            } else {
                negative.addRowToTable(row);
            }
            exec.setProgress(currentRow / (double)inData[0].size(), "filtering row # " + currentRow);
            currentRow++;
            exec.checkCanceled();
        }
        positive.close();
        BufferedDataTable positiveTable = exec.createBufferedDataTable(positive.getTable(), exec);
        if (positiveTable.size() <= 0) {
            String warning = "No rows matched! Input mirrored at out-port 1 (excluded)";
            setWarningMessage(warning);
        }
        negative.close();
        BufferedDataTable negativeTable = exec.createBufferedDataTable(negative.getTable(), exec);
        if (negativeTable.size() <= 0) {
            setWarningMessage("All rows matched! Input mirrored at out-port 0 (included)");
        }
        return new BufferedDataTable[]{positiveTable, negativeTable};
    }

    /*
     * Check if the value in the selected column is in the selected possible
     * values.
     */
    private boolean matches(final DataRow row, final NominalValueRowSplitterNodeSettings settings) {
        DataCell dc = row.getCell(m_selectedColIdx);
        if (dc.isMissing()) {
            return settings.m_missingValueHandling == NominalValueRowSplitterNodeSettings.MissingValueHandling.UPPER;
        } else {
            return m_selectedAttr.contains(dc.toString());
        }
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs, final NominalValueRowSplitterNodeSettings settings) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {

                RowInput in = (RowInput)inputs[0];
                RowOutput match = (RowOutput)outputs[0];
                RowOutput miss = (RowOutput)outputs[1];
                try {
                    long rowIdx = -1;
                    DataRow row;
                    while ((row = in.poll()) != null) {
                        rowIdx++;
                        exec.setProgress("Adding row " + rowIdx + ".");
                        exec.checkCanceled();

                        if (matches(row, settings)) {
                            match.push(row);
                        } else {
                            miss.push(row);
                        }
                    }
                } finally {
                    match.close();
                    miss.close();
                }
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED, OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, // NOSONAR method is old and well-tested. Complexity is OK.
        final NominalValueRowSplitterNodeSettings settings) throws InvalidSettingsException {
        // check if possible values are available
        int nrValidCols = 0;
        for (DataColumnSpec colSpec : inSpecs[0]) {
            if (colSpec.getType().isCompatible(NominalValue.class) && colSpec.getDomain().hasValues()) {
                nrValidCols++;
            }
        }
        // are there some valid columns (nominal and with possible values)
        if (nrValidCols == 0) {
            throw new InvalidSettingsException("No nominal columns with possible values found. "
                + "To make nominal columns usable by this node, their domain must be calculated."
                + "For columns with few values (less than 60) this is done automatically. "
                + "For more values, use the Domain Calculator node or specify the possible values manually "
                + "using the Edit Nominal Domain node.");
        }
        m_selectedAttr.clear();
        var selectedColumn = settings.m_selectedColumn;
        if (selectedColumn != null && selectedColumn.length() > 0) {
            m_selectedColIdx = inSpecs[0].findColumnIndex(selectedColumn);
            // selected attribute not found in possible values
            if (m_selectedColIdx < 0) {
                throw new InvalidSettingsException(
                    "The selected column \"" + selectedColumn + "\" is not present in the input table.");
            }
            final var domainValues =
                Optional.ofNullable(inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain().getValues()) //
                    .map(values -> values.stream().map(DataCell::toString).toArray(String[]::new)) //
                    .orElse(new String[0]);
            m_selectedAttr.addAll(Arrays.asList(settings.m_nominalValueSelection.filter(domainValues)));
            // all values excluded?
            var includeMissing =
                settings.m_missingValueHandling == NominalValueRowSplitterNodeSettings.MissingValueHandling.UPPER;
            if (selectedColumn != null && m_selectedAttr.isEmpty() && !includeMissing) {
                setWarningMessage("All values are excluded! Input will be mirrored at out-port 1 (excluded)");
            }
            // all values included?
            boolean validAttrVal = false;
            if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain().hasValues()) {
                if (inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain().getValues().size() == m_selectedAttr.size()
                    && includeMissing) {
                    setWarningMessage("All values are included! Input will be " + "mirrored at out-port 0 (included)");
                }
                // if attribute value isn't found in domain also throw exception
                for (DataCell dc : inSpecs[0].getColumnSpec(m_selectedColIdx).getDomain().getValues()) {
                    if (m_selectedAttr.contains(dc.toString())) {
                        validAttrVal = true;
                        break;
                    }
                }
            }

            if (!validAttrVal && !m_selectedAttr.isEmpty()) {
                throw new InvalidSettingsException("Selected attribute value \"" + m_selectedAttr
                    + "\" was not found in column \"" + selectedColumn + "\".");
            }

            // return original spec,
            // only the rows are affected
        }
        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
    }
}
