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
 *   May 6, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.apply;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.port.TransformationPortObject;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.StreamableFunction;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.CheckUtils;

/**
 * The PCA apply node model.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PCA2ApplyNodeModel extends NodeModel {

    static final int MODEL_IN_PORT = 0;

    private static final int DATA_IN_PORT = 1;

    private final PCAApplySettings m_applySettings = new PCAApplySettings();

    PCA2ApplyNodeModel() {
        super(new PortType[]{TransformationPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        if (!(inData[MODEL_IN_PORT] instanceof TransformationPortObject)) {
            throw new IllegalArgumentException("PCAModelPortObject as first input expected");
        }
        if (!(inData[DATA_IN_PORT] instanceof BufferedDataTable)) {
            throw new IllegalArgumentException("Datatable as second input expected");
        }

        final BufferedDataTable inTable = (BufferedDataTable)inData[DATA_IN_PORT];
        final TransformationPortObject inModel = (TransformationPortObject)inData[MODEL_IN_PORT];

        final ColumnRearranger cr = createColumnRearranger(inModel, inModel.getSpec(), inTable.getDataTableSpec());

        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec);
        return new PortObject[]{out};
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec dataSpec = (DataTableSpec)inSpecs[DATA_IN_PORT];
        final TransformationPortObjectSpec modelSpec = (TransformationPortObjectSpec)inSpecs[MODEL_IN_PORT];

        // check existing column names and find out their indices
        final String[] usedColumnNames = modelSpec.getInputColumnNames();
        final int numIncludedColumns = usedColumnNames.length;
        for (int i = 0; i < numIncludedColumns; i++) {
            final String columnName = usedColumnNames[i];
            if (dataSpec.findColumnIndex(columnName) == -1) {
                throw new InvalidSettingsException(
                    "The model is expecting column \"" + columnName + "\" which is missing in the input table.");
            }
        }
        if (m_applySettings.getUseFixedDimensionModel().getBooleanValue()) {
            CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() > 0,
                "The number of dimensions to project to must be a positive integer larger than 0, %s is invalid",
                m_applySettings.getDimModel().getIntValue());
            final int maxDim = modelSpec.getMaxDimToReduceTo();
            CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() <= maxDim,
                "The number of dimensions to project to must be less than or equal %s", maxDim);
        } else if (!modelSpec.getEigenValues().isPresent()) {
            return null;
        }
        return new PortObjectSpec[]{createColumnRearranger(null, modelSpec, dataSpec).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final TransformationPortObject inModel,
        final TransformationPortObjectSpec inModelSpec, final DataTableSpec dataSpec) {
        return TransformationUtils.createColumnRearranger(dataSpec,
            inModel != null ? inModel.getTransformationMatrix() : null, getDimToReduceTo(inModelSpec),
            m_applySettings.getRemoveUsedColsModel().getBooleanValue(), inModelSpec.getInputColumnNames(),
            inModelSpec.getTransformationType());

    }

    private int getDimToReduceTo(final TransformationPortObjectSpec inModelSpec) {
        if (m_applySettings.getUseFixedDimensionModel().getBooleanValue()) {
            return m_applySettings.getDimModel().getIntValue();
        } else {
            return TransformationUtils.calcDimForGivenInfPreservation(inModelSpec.getEigenValues().get(),
                inModelSpec.getMaxDimToReduceTo(), m_applySettings.getInfPreservationModel().getDoubleValue());
        }
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {

                final TransformationPortObject inModel =
                    (TransformationPortObject)((PortObjectInput)inputs[MODEL_IN_PORT]).getPortObject();

                final TransformationPortObjectSpec inModelSpec = inModel.getSpec();

                final ColumnRearranger cr =
                    createColumnRearranger(inModel, inModelSpec, (DataTableSpec)inSpecs[DATA_IN_PORT]);

                final StreamableFunction func = cr.createStreamableFunction(DATA_IN_PORT, 0);
                func.runFinal(inputs, outputs, exec);
            }
        };
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.loadValidatedSettingsFrom(settings);

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_applySettings.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_applySettings.validateSettings(settings);
    }

    @Override
    protected void reset() {
    }

    @Override
    protected void saveInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
    }

    @Override
    protected void loadInternals(final File arg0, final ExecutionMonitor arg1)
        throws IOException, CanceledExecutionException {
    }
}