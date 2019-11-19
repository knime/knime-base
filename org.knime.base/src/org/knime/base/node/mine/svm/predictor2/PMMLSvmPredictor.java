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
 *   12 Nov 2019 (Alexander): created
 */
package org.knime.base.node.mine.svm.predictor2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.knime.base.node.mine.svm.PMMLSVMTranslator;
import org.knime.base.node.mine.svm.Svm;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.predict.PMMLClassificationPredictorOptions;
import org.knime.base.predict.PMMLTablePredictor;
import org.knime.base.predict.PredictorContext;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * Predictor for applying SVM PMML models to a table.
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public class PMMLSvmPredictor implements PMMLTablePredictor {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLSvmPredictor.class);

    private static final String SVM_EVALUATION_FAILED_MSG =
            "SVM evaluation failed: No support vector machine model found.";

    private PMMLClassificationPredictorOptions m_options;

    /**
     * Creates a new instance of {@code PMMLSvnPredictor}.
     * @param options the options determining the output of the predictor
     */
    public PMMLSvmPredictor(final PMMLClassificationPredictorOptions options) {
        m_options = options;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable predict(final BufferedDataTable input, final PMMLPortObject model,
        final PredictorContext ctx) throws Exception {
        ColumnRearranger colre = createColumnRearranger(model, input.getDataTableSpec());
        BufferedDataTable result =
                ctx.getExecutionContext().createColumnRearrangeTable(input, colre, ctx.getExecutionContext());
        return result;
    }

    private ColumnRearranger createColumnRearranger(final PMMLPortObject pmmlModel, final DataTableSpec inSpec)
            throws InvalidSettingsException {
        List<Node> models = pmmlModel.getPMMLValue().getModels(PMMLModelType.SupportVectorMachineModel);
        if (models.isEmpty()) {
            LOGGER.error(SVM_EVALUATION_FAILED_MSG);
            throw new RuntimeException(SVM_EVALUATION_FAILED_MSG);
        }
        PMMLSVMTranslator trans = new PMMLSVMTranslator();
        pmmlModel.initializeModelTranslator(trans);

        List<Svm> svms = trans.getSVMs();
        Svm[] svmsArr = svms.toArray(new Svm[svms.size()]);
        if (m_options.includeClassProbabilities() == pmmlModel.getSpec().getTargetCols().size() > 0) {
            adjustOrder(pmmlModel.getSpec().getTargetCols().get(0), svmsArr);
        }
        DataTableSpec testSpec = inSpec;
        PMMLPortObjectSpec pmmlSpec = pmmlModel.getSpec();
        DataTableSpec trainingSpec = pmmlSpec.getDataTableSpec();
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : trainingSpec) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = testSpec.findColumnIndex(colspec.getName());
                if (colindex < 0) {
                    throw new InvalidSettingsException(
                        "Column " + "\'" + colspec.getName() + "\' not found" + " in test data");
                }
                colindices.add(colindex);
            }
        }
        int[] colindicesArr = new int[colindices.size()];
        for (int i = 0; i < colindicesArr.length; i++) {
            colindicesArr[i] = colindices.get(i);
        }
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final String targetCol = pmmlSpec.getTargetFields().iterator().next();
        SVMPredictor svmpredict = new SVMPredictor(targetCol, svmsArr, colindicesArr,
            predictorHelper.computePredictionColumnName(m_options.getPredictionColumnName(),
                m_options.hasCustomPredictionColumnName(), targetCol),
            m_options.includeClassProbabilities(), m_options.getPropColumnSuffix());
        ColumnRearranger colre = new ColumnRearranger(testSpec);
        colre.append(svmpredict);
        return colre;
    }

    /**
     * @param targetSpec The target column from the model.
     */
    private static void adjustOrder(final DataColumnSpec targetSpec, final Svm[] svms) {
        if (targetSpec.getDomain() != null) {
            Map<String, Svm> map = new LinkedHashMap<>();
            for (Svm svm : svms) {
                map.put(svm.getPositive(), svm);
            }
            int i = 0;
            for (DataCell v : targetSpec.getDomain().getValues()) {
                String key = v.toString();
                Svm svm = map.get(key);
                if (svm != null) {
                    svms[i++] = svm;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getOutputSpec(final DataTableSpec inputSpec, final PMMLPortObjectSpec modelSpec,
        final PredictorContext ctx) throws InvalidSettingsException {
        // try to find all columns (except the class column)
        Vector<Integer> colindices = new Vector<Integer>();
        for (DataColumnSpec colspec : modelSpec.getLearningCols()) {
            if (colspec.getType().isCompatible(DoubleValue.class)) {
                int colindex = inputSpec.findColumnIndex(colspec.getName());
                if (colindex < 0) {
                    throw new InvalidSettingsException("Column " + "\'" + colspec.getName() + "\' not found"
                        + " in test data");
                }
                colindices.add(colindex);
            }
        }
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        return predictorHelper.createOutTableSpec(inputSpec, modelSpec,
            m_options.includeClassProbabilities(), m_options.getPredictionColumnName(),
            m_options.hasCustomPredictionColumnName(), m_options.getPropColumnSuffix());
    }
}
