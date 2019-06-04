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
 *   May 2, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.perform;

import org.knime.base.data.statistics.TransformationMatrix;
import org.knime.base.data.statistics.calculation.PCA;
import org.knime.base.node.mine.transformation.pca.AbstractPCA2NodeModel;
import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec.TransformationType;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * The PCA node model.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PCA2NodeModel extends AbstractPCA2NodeModel {

    private final PCAApplySettings m_applySettings = new PCAApplySettings();

    PCA2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE});
    }

    @Override
    protected PortObject[] doExecute(final BufferedDataTable inTable, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException {
        final PCA pca = new PCA();
        final TransformationMatrix transformationMatrix =
            pca.calcTransformationMatrix(exec.createSubExecutionContext(0.7), inTable, getColumnNames(),
                m_computeSettings.getFailOnMissingsModel().getBooleanValue());
        final int dimToReduceTo;
        if (m_applySettings.getUseFixedDimensionModel().getBooleanValue()) {
            dimToReduceTo = m_applySettings.getDimModel().getIntValue();
        } else {
            dimToReduceTo = TransformationUtils.calcDimForGivenInfPreservation(transformationMatrix,
                m_applySettings.getInfPreservationModel().getDoubleValue());
        }
        final ColumnRearranger cr = createColumnRearranger(inTable.getDataTableSpec(), transformationMatrix,
            dimToReduceTo, m_applySettings.getRemoveUsedColsModel().getBooleanValue());
        final BufferedDataTable out = exec.createColumnRearrangeTable(inTable, cr, exec.createSubProgress(0.3));
        return new PortObject[]{out};

    }

    @Override
    protected PortObjectSpec[] doConfigure(final DataTableSpec inSpec) throws InvalidSettingsException {
        if (m_applySettings.getUseFixedDimensionModel().getBooleanValue()) {
            CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() > 0,
                "The number of dimensions to project to must be a positive integer larger than 0, %s is invalid",
                m_applySettings.getDimModel().getIntValue());
            CheckUtils.checkSetting(m_applySettings.getDimModel().getIntValue() <= getColumnNames().length,
                "The number of dimensions to project to must be less than or equal to the number of selected columns");
            return new PortObjectSpec[]{
                createColumnRearranger(inSpec, null, m_applySettings.getDimModel().getIntValue(),
                    m_applySettings.getRemoveUsedColsModel().getBooleanValue()).createSpec()};
        } else {
            // we don't know the number of resulting dimension if the user selected the information preservation option
            final double infPerservation = m_applySettings.getInfPreservationModel().getDoubleValue();
            CheckUtils.checkSetting(infPerservation > 0 && infPerservation <= 100,
                "The information perservation has to be in range [0.01,100]");
            return null;
        }
    }

    /**
     * Create a column rearranger that applies the LDA, if given
     *
     * @param inSpec the inspec of the table
     * @param transMatrix the transformation matrix or null if called from configure
     * @param k number of dimensions to reduce to (number of rows in w)
     * @param removeUsedCols whether to remove the input data
     * @return the column re-arranger
     *
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final TransformationMatrix transMatrix,
        final int k, final boolean removeUsedCols) {
        return TransformationUtils.createColumnRearranger(inSpec, transMatrix, k, removeUsedCols, getColumnNames(),
            TransformationType.PCA);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_applySettings.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_applySettings.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_applySettings.validateSettings(settings);
    }

}