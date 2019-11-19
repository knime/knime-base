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
 *   15 Nov 2019 (Alexander): created
 */
package org.knime.base.node.mine.bayes.naivebayes.predictor4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.mine.bayes.naivebayes.datamodel3.NaiveBayesModel;
import org.knime.base.node.mine.bayes.naivebayes.datamodel3.PMMLNaiveBayesModelTranslator;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.predict.PMMLClassificationPredictorOptions;
import org.knime.base.predict.PMMLTablePredictor;
import org.knime.base.predict.PredictorContext;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * Predictor for naive bayes models.
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public class PMMLNaiveBayesPredictor implements PMMLTablePredictor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PMMLNaiveBayesPredictor.class);

    private PMMLClassificationPredictorOptions m_options;

    /**
     * Creates a new instance of {@code PMMLNaiveBayesPredictor}.
     * @param options options determining the predictor's output
     */
    public PMMLNaiveBayesPredictor(final PMMLClassificationPredictorOptions options) {
        m_options = options;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable predict(final BufferedDataTable input, final PMMLPortObject model,
        final PredictorContext ctx) throws Exception {
        LOGGER.debug("Entering execute(inData, exec) of class NaiveBayesPredictorNodeModel.");
        final Collection<Node> models = model.getPMMLValue().getModels(PMMLModelType.NaiveBayesModel);
        if (models == null || models.isEmpty()) {
            throw new Exception("Node not properly configured. No Naive Bayes Model available.");
        }
        if (models.size() > 1) {
            throw new Exception("Node supports only one Naive Bayes Model at a time.");
        }
        ctx.getExecutionContext().setMessage("Classifying rows...");
        ColumnRearranger rearranger = createColumnRearranger(model, input.getDataTableSpec(), ctx);
        final BufferedDataTable returnVal = ctx.getExecutionContext()
                .createColumnRearrangeTable(input, rearranger, ctx.getExecutionContext());
        LOGGER.debug("Exiting execute(inData, exec) of class NaiveBayesPredictorNodeModel.");
        return returnVal;
    }

    /* Helper to create the column rearranger that does the actual work */
    private ColumnRearranger createColumnRearranger(final PMMLPortObject pmmlPortObj,
        final DataTableSpec inSpec, final PredictorContext ctx) {
        final PMMLNaiveBayesModelTranslator translator = new PMMLNaiveBayesModelTranslator();
        pmmlPortObj.initializeModelTranslator(translator);
        final NaiveBayesModel model = translator.getModel();
        if (!model.hasPMMLThreshold()) {
            ctx.setWarningMessage("The provided PMML model misses a proper default probability threshold, "
                + NaiveBayesModel.DEFAULT_MIN_PROB_THRESHOLD + " is used instead.");

        }
        if (!model.hasStablePMMLThreshold()) {
            ctx.setWarningMessage("The PMML model's default probability threshold is zero, which "
                + "might cause numerical problems.");
        }
        PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final String classColumnName = model.getClassColumnName();
        final String predictionColName = m_options.hasCustomPredictionColumnName() ? m_options.getPredictionColumnName()
            : predictorHelper.computePredictionDefault(classColumnName);
        final NaiveBayesCellFactory appender = new NaiveBayesCellFactory(model, predictionColName, inSpec,
            m_options.includeClassProbabilities(), m_options.getPropColumnSuffix());
        final ColumnRearranger rearranger = new ColumnRearranger(inSpec);
        rearranger.append(appender);
        return rearranger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getOutputSpec(final DataTableSpec inputSpec, final PMMLPortObjectSpec modelSpec,
        final PredictorContext ctx) throws InvalidSettingsException {
        final DataTableSpec trainingSpec = modelSpec.getDataTableSpec();
        if (trainingSpec == null) {
            throw new InvalidSettingsException("No model spec available");
        }
        final List<DataColumnSpec> targetCols = modelSpec.getTargetCols();
        if (targetCols.size() != 1) {
            throw new InvalidSettingsException("No valid class column found");
        }
        final DataColumnSpec classColumn = targetCols.get(0);
        //check the input data for columns with the wrong name or wrong type
        final List<String> unknownCols = check4UnknownCols(trainingSpec, inputSpec);
        warningMessage("The following input columns are unknown and will be skipped: ", unknownCols, ctx);
        //check if the learned model contains columns which are not in the
        //input data
        final List<String> missingInputCols = check4MissingCols(trainingSpec, classColumn.getName(), inputSpec);
        warningMessage("The following attributes are missing in the input data: ", missingInputCols, ctx);
        final PredictorHelper predictorHelper = PredictorHelper.getInstance();
        final DataColumnSpec resultColSpecs = NaiveBayesCellFactory.createResultColSpecs(
            predictorHelper.checkedComputePredictionColumnName(m_options.getPredictionColumnName(),
                m_options.hasCustomPredictionColumnName(), classColumn.getName()),
            classColumn.getType(), inputSpec, m_options.includeClassProbabilities());
        if (resultColSpecs != null) {
            return AppendedColumnTable.getTableSpec(inputSpec, resultColSpecs);
        }
        return null;
    }

    private static void warningMessage(final String message, final List<String> colNames, final PredictorContext ctx) {
        if (!colNames.isEmpty()) {
            final StringBuilder buf = new StringBuilder();
            buf.append(message);
            for (int i = 0, length = colNames.size(); i < length; i++) {
                if (i != 0) {
                    buf.append(", ");
                }
                if (i == 4) {
                    ctx.setWarningMessage(buf.toString() + "... (see log file for details)");
                }
                buf.append(colNames.get(i));

            }
            if (colNames.size() < 4) {
                ctx.setWarningMessage(buf.toString());
            } else {
                LOGGER.info(buf.toString());
            }
        }
    }

    private static List<String> check4MissingCols(final DataTableSpec trainingSpec, final String classCol,
        final DataTableSpec spec) {
        final List<String> missingInputCols = new ArrayList<>();
        for (final DataColumnSpec trainColSpec : trainingSpec) {
            if (!trainColSpec.getName().equals(classCol)) {
                //check only for none class value columns
                if (spec.getColumnSpec(trainColSpec.getName()) == null) {
                    missingInputCols.add(trainColSpec.getName());
                }
            }
        }
        return missingInputCols;
    }

    private static List<String> check4UnknownCols(final DataTableSpec trainingSpec, final DataTableSpec spec) {
        if (spec == null) {
            throw new NullPointerException("TableSpec must not be null");
        }
        final List<String> unknownCols = new ArrayList<>();
        for (final DataColumnSpec colSpec : spec) {
            final DataColumnSpec trainColSpec = trainingSpec.getColumnSpec(colSpec.getName());
            if (trainColSpec == null || !colSpec.getType().equals(trainColSpec.getType())) {
                unknownCols.add(colSpec.getName());
            }
        }
        return unknownCols;
    }

}
