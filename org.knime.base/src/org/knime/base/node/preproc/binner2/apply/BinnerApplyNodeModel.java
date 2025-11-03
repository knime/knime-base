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
package org.knime.base.node.preproc.binner2.apply;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.util.binning.BinningPMMLApplyUtil;

/**
 * The Model for the Binner (Apply) Node.
 *
 * @author Paul Baernreuther
 */
final class BinnerApplyNodeModel extends NodeModel {

    /** Creates a new instance. */
    BinnerApplyNodeModel() {
        super(new PortType[]{PMMLPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * Feature flag added with 5.9 (UIEXT-3029) since the Binner node did not output valid PMML before.
     */
    private boolean m_validatePMMLPortObject = true;

    private static final String VALIDATE_PMML_CFG_KEY = "validate_pmml_port_object";

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{null};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        PMMLPortObject pmmlPort = (PMMLPortObject)inObjects[0];
        BufferedDataTable inTable = (BufferedDataTable)inObjects[1];
        final var inSpec = inTable.getSpec();
        validatePMMLPortObject(inSpec, pmmlPort);
        final var rearranger = BinningPMMLApplyUtil.createColumnRearranger(inSpec, pmmlPort);
        final var binnedData = exec.createColumnRearrangeTable(inTable, rearranger, exec);
        return new PortObject[]{binnedData};
    }

    private void validatePMMLPortObject(final DataTableSpec inSpec, final PMMLPortObject pmmlPortObject)
        throws InvalidSettingsException {
        if (m_validatePMMLPortObject) {
            final DataTableSpec pmmlInputSpec = pmmlPortObject.getSpec().getDataTableSpec();
            for (final var derivedField : pmmlPortObject.getDerivedFields()) {
                final String fieldName = derivedField.getDiscretize().getField();
                validateFieldName(inSpec, pmmlInputSpec, fieldName);
            }

        }

    }

    private static void validateFieldName(final DataTableSpec dataTableSpec, final DataTableSpec pmmlInputSpec,
        final String fieldName) throws InvalidSettingsException {
        final DataColumnSpec colSpec = dataTableSpec.getColumnSpec(fieldName);
        if (colSpec == null) {
            throw new InvalidSettingsException("Column '" + fieldName + "' not found in input table");
        }

        final DataColumnSpec pmmlInputColSpec = pmmlInputSpec.getColumnSpec(fieldName);
        assert (pmmlInputColSpec != null) : "Column '" + fieldName
            + "' from derived fields not found in PMML model spec";

        final DataType knimeType = pmmlInputColSpec.getType();
        if (!colSpec.getType().isCompatible(knimeType.getPreferredValueClass())) {
            throw new InvalidSettingsException(
                "Date type of column '" + fieldName + "' is not compatible with PMML model: expected '" + knimeType
                    + "' but is '" + colSpec.getType() + "'");
        }
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            private DataTableSpec m_outSpec;

            @Override
            public StreamableOperatorInternals saveInternals() {
                if (m_outSpec != null) {
                    SimpleStreamableOperatorInternals internals = new SimpleStreamableOperatorInternals();
                    m_outSpec.save(internals.getConfig().addConfig("OUT_SPEC"));
                    return internals;
                } else {
                    return null;
                }
            }

            @Override
            public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
                //actually only one node is sufficient to calc the outspec
                //we choose here the first remote node to do so
                if (partitionInfo.getPartitionIndex() == 0) {
                    PMMLPortObject pmmlPort = (PMMLPortObject)((PortObjectInput)inputs[0]).getPortObject();
                    final var columnRearranger =
                        BinningPMMLApplyUtil.createColumnRearranger((DataTableSpec)inSpecs[0], pmmlPort);
                    m_outSpec = columnRearranger.createSpec();
                }
            }

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                PMMLPortObject pmmlPort = (PMMLPortObject)((PortObjectInput)inputs[0]).getPortObject();
                final var columnRearranger =
                    BinningPMMLApplyUtil.createColumnRearranger((DataTableSpec)inSpecs[0], pmmlPort);
                StreamableFunction func = columnRearranger.createStreamableFunction(1, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
    }

    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        //one iteration to determine the out spec, if out spec already present, no more iteration necessary
        return !((SimpleStreamableOperatorInternals)internals).getConfig().containsKey("OUT_SPEC");
    }

    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec out =
            DataTableSpec.load(((SimpleStreamableOperatorInternals)internals).getConfig().getConfig("OUT_SPEC"));
        return new PortObjectSpec[]{out};
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE};
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.DISTRIBUTED};
    }

    @Override
    protected void reset() {
        // nothing to reset
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // added with 5.9 to be able to disable PMML validation in legacy workflows
        m_validatePMMLPortObject = settings.getBoolean(VALIDATE_PMML_CFG_KEY, false);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // added with 5.9 to be able to disable PMML validation in legacy workflows
        settings.addBoolean(VALIDATE_PMML_CFG_KEY, m_validatePMMLPortObject);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // node has no settings
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // node has no internal data
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // node has no internal data
    }
}
