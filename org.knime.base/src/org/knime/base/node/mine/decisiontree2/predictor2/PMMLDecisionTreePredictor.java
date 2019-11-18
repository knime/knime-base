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
 *   11 Nov 2019 (Alexander): created
 */
package org.knime.base.node.mine.decisiontree2.predictor2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreeTranslator;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.util.PredictorHelper;
import org.knime.base.predict.PMMLClassificationPredictorOptions;
import org.knime.base.predict.PMMLTablePredictor;
import org.knime.base.predict.PredictorContext;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.pmml.PMMLModelType;
import org.knime.core.util.Pair;
import org.knime.core.util.UniqueNameGenerator;
import org.w3c.dom.Node;

/**
 * Class for predicting PMML decision tree models.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 */
public class PMMLDecisionTreePredictor implements PMMLTablePredictor {

    private PMMLDecisionTreePredictorOptions m_options;

    /**
     * Creates a new instance of {@code DecisionTreePredictor}.
     *
     * @param options the options determining the predictor output
     */
    public PMMLDecisionTreePredictor(final PMMLDecisionTreePredictorOptions options) {
        m_options = options;
    }

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PMMLDecisionTreePredictor.class);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable predict(final BufferedDataTable input, final PMMLPortObject model,
        final PredictorContext ctx) throws Exception {
        final ExecutionContext exec = ctx.getExecutionContext();
        exec.setMessage("Decision Tree Predictor: Loading predictor...");

        List<Node> models = model.getPMMLValue().getModels(PMMLModelType.TreeModel);
        if (models.isEmpty()) {
            String msg = "Decision Tree evaluation failed: " + "No tree model found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        PMMLDecisionTreeTranslator trans = new PMMLDecisionTreeTranslator();
        model.initializeModelTranslator(trans);
        DecisionTree decTree = trans.getDecisionTree();

        decTree.resetColorInformation();
        // get column with color information
        String colorColumn = null;
        for (DataColumnSpec s : input.getDataTableSpec()) {
            if (s.getColorHandler() != null) {
                colorColumn = s.getName();
                break;
            }
        }
        decTree.setColorColumn(colorColumn);
        exec.setMessage("Decision Tree Predictor: start execution.");
        DataTableSpec outSpec = createOutTableSpec(input.getDataTableSpec(), model.getSpec());
        BufferedDataContainer outData = exec.createDataContainer(outSpec);
        long coveredPattern = 0;
        long nrPattern = 0;
        long rowCount = 0;
        final long numberRows = input.size();
        exec.setMessage("Classifying...");
        List<String> predictionValues = getPredictionStrings(model.getSpec());
        for (DataRow thisRow : input) {
            DataCell cl = null;
            LinkedHashMap<String, Double> classDistrib = null;
            try {
                Pair<DataCell, LinkedHashMap<DataCell, Double>> pair =
                    decTree.getWinnerAndClasscounts(thisRow, input.getDataTableSpec());
                cl = pair.getFirst();
                LinkedHashMap<DataCell, Double> classCounts = pair.getSecond();

                classDistrib = getDistribution(classCounts);
                if (coveredPattern < m_options.getMaxNumCoveredPattern()) {
                    // remember this one for HiLite support
                    decTree.addCoveredPattern(thisRow, input.getDataTableSpec());
                    coveredPattern++;
                } else {
                    // too many patterns for HiLite - at least remember color
                    decTree.addCoveredColor(thisRow, input.getDataTableSpec());
                }
                nrPattern++;
            } catch (Exception e) {
                LOGGER.error("Decision Tree evaluation failed: " + e.getMessage());
                throw e;
            }
            if (cl == null) {
                LOGGER.error("Decision Tree evaluation failed: result empty");
                throw new Exception("Decision Tree evaluation failed.");
            }

            DataCell[] newCells = new DataCell[outSpec.getNumColumns()];
            int numInCells = thisRow.getNumCells();
            for (int i = 0; i < numInCells; i++) {
                newCells[i] = thisRow.getCell(i);
            }

            if (m_options.includeClassProbabilities()) {
                assert predictionValues.size() >= newCells.length - 1
                    - numInCells : "Could not determine the prediction values: " + newCells.length + "; " + numInCells
                        + "; " + predictionValues;
                for (int i = numInCells; i < newCells.length - 1; i++) {
                    String predClass = predictionValues.get(i - numInCells);
                    if (classDistrib != null && classDistrib.get(predClass) != null) {
                        newCells[i] = new DoubleCell(classDistrib.get(predClass));
                    } else {
                        newCells[i] = new DoubleCell(0.0);
                    }
                }
            }
            newCells[newCells.length - 1] = cl;

            outData.addRowToTable(new DefaultRow(thisRow.getKey(), newCells));

            rowCount++;
            if (rowCount % 100 == 0) {
                exec.setProgress(rowCount / (double)numberRows, "Classifying... Row " + rowCount + " of " + numberRows);
            }
            exec.checkCanceled();
        }
        if (coveredPattern < nrPattern) {
            // let the user know that we did not store all available pattern
            // for HiLiting.
            ctx.setWarningMessage("Tree only stored first " + m_options.getMaxNumCoveredPattern() + " (of " + nrPattern
                + ") rows for HiLiting!");
        }
        outData.close();
        exec.setMessage("Decision Tree Predictor: end execution.");
        return outData.getTable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getOutputSpec(final DataTableSpec inputSpec, final PMMLPortObjectSpec modelSpec,
        final PredictorContext ctx) throws InvalidSettingsException {
        String predCol = m_options.getPredictionColumnName();
        CheckUtils.checkSetting(
            !m_options.hasCustomPredictionColumnName() || (predCol != null && !predCol.trim().isEmpty()),
            "Prediction column name cannot be empty");

        for (String learnColName : modelSpec.getLearningFields()) {
            if (!inputSpec.containsName(learnColName)) {
                throw new InvalidSettingsException(
                    "Learning column \"" + learnColName + "\" not found in input " + "data to be predicted");
            }
        }
        return createOutTableSpec(inputSpec, modelSpec);
    }

    private static List<String> getPredictionStrings(final PMMLPortObjectSpec spec) throws InvalidSettingsException {
        List<DataCell> predictionValues = getPredictionValues(spec);
        if (predictionValues == null) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<String>(predictionValues.size());
        for (DataCell dataCell : predictionValues) {
            ret.add(dataCell.toString());
        }
        return ret;
    }

    private static LinkedHashMap<String, Double> getDistribution(final LinkedHashMap<DataCell, Double> classCounts) {
        LinkedHashMap<String, Double> dist = new LinkedHashMap<String, Double>(classCounts.size());
        Double total = 0.0;
        for (Double count : classCounts.values()) {
            total += count;
        }
        if (total == 0.0) {
            return null;
        } else {
            for (Entry<DataCell, Double> classCount : classCounts.entrySet()) {
                dist.put(classCount.getKey().toString(), classCount.getValue() / total);
            }
            return dist;
        }
    }

    private DataTableSpec createOutTableSpec(final DataTableSpec inSpec, final PMMLPortObjectSpec modelSpec)
        throws InvalidSettingsException {
        List<DataCell> predValues = null;
        if (m_options.includeClassProbabilities()) {
            predValues = getPredictionValues(modelSpec);
            if (predValues == null) {
                return null; // no out spec can be determined
            }
        }

        int numCols = (predValues == null ? 0 : predValues.size()) + 1;

        DataColumnSpec[] newCols = new DataColumnSpec[numCols];

        /* Set bar renderer and domain [0,1] as default for the double cells
         * containing the distribution */
        //    DataColumnProperties propsRendering = new DataColumnProperties(
        //            Collections.singletonMap(
        //                    DataValueRenderer.PROPERTY_PREFERRED_RENDERER,
        //                    DoubleBarRenderer.DESCRIPTION));
        DataColumnDomain domain = new DataColumnDomainCreator(new DoubleCell(0.0), new DoubleCell(1.0)).createDomain();

        PredictorHelper predictorHelper = PredictorHelper.getInstance();
        String trainingColumnName = modelSpec.getTargetFields().iterator().next();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(inSpec);
        // add all distribution columns
        for (int i = 0; i < numCols - 1; i++) {
            assert predValues != null;
            DataColumnSpecCreator colSpecCreator =
                nameGen.newCreator(predictorHelper.probabilityColumnName(trainingColumnName,
                    predValues.get(i).toString(), m_options.getPropColumnSuffix()), DoubleCell.TYPE);
            //            colSpecCreator.setProperties(propsRendering);
            colSpecCreator.setDomain(domain);
            newCols[i] = colSpecCreator.createSpec();
        }
        //add the prediction column
        String predictionColumnName = predictorHelper.computePredictionColumnName(m_options.getPredictionColumnName(),
            m_options.hasCustomPredictionColumnName(), trainingColumnName);

        newCols[numCols - 1] = nameGen.newColumn(predictionColumnName, StringCell.TYPE);
        DataTableSpec newColSpec = new DataTableSpec(newCols);
        return new DataTableSpec(inSpec, newColSpec);
    }

    private static List<DataCell> getPredictionValues(final PMMLPortObjectSpec treeSpec)
        throws InvalidSettingsException {
        String targetCol = treeSpec.getTargetFields().get(0);
        DataColumnSpec colSpec = treeSpec.getDataTableSpec().getColumnSpec(targetCol);

        if (!colSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException(
                "This predictor only supports target fields of data type string (got " + colSpec.getType() + ")");
        }

        //Replaced LinkedList because later it is used to get values by index
        ArrayList<DataCell> predValues = new ArrayList<DataCell>();
        if (colSpec.getDomain().hasValues()) {
            predValues.addAll(colSpec.getDomain().getValues());
        } else if (colSpec.getType() == BooleanCell.TYPE) {
            predValues.add(BooleanCell.FALSE);
            predValues.add(BooleanCell.TRUE);
        } else {
            return null;
        }
        return predValues;
    }

    /**
     *
     * Options for the {@code PMMLDecisionTreePredictor}.
     *
     * @author Alexander Fillbrunn, KNIME AG
     */
    public static final class PMMLDecisionTreePredictorOptions extends PMMLClassificationPredictorOptions {
        private int m_maxNumCoveredPattern;

        /**
         * Creates a new instance of {@code PMMLDecisionTreePredictorOptions}.
         *
         * @param customPredictionName the name of the prediction column or null if it should be inferred from the PMML
         * @param includeProbabilities whether to output class probabilities
         * @param propColumnSuffix column name suffix for output class probabilities
         * @param maxNumCoveredPattern number of patterns to remember for hiliting
         */
        public PMMLDecisionTreePredictorOptions(final String customPredictionName, final boolean includeProbabilities,
            final String propColumnSuffix, final int maxNumCoveredPattern) {
            super(customPredictionName, includeProbabilities, propColumnSuffix);
            m_maxNumCoveredPattern = maxNumCoveredPattern;
        }

        /**
         * Creates a new instance of {@code PMMLDecisionTreePredictorOptions} where the prediction column name is
         * inferred from the PMML.
         *
         * @param includeProbabilities whether to output class probabilities
         * @param propColumnSuffix column name suffix for output class probabilities
         * @param maxNumCoveredPattern number of patterns to remember for hiliting
         */
        public PMMLDecisionTreePredictorOptions(final boolean includeProbabilities, final String propColumnSuffix,
            final int maxNumCoveredPattern) {
            this(null, includeProbabilities, propColumnSuffix, maxNumCoveredPattern);
        }

        /**
         * Creates a new instance of {@code PMMLDecisionTreePredictorOptions}, determining that probabilities are not
         * returned and the default name is used for the prediction column.
         *
         * @param maxNumCoveredPattern number of patterns to remember for hiliting
         */
        public PMMLDecisionTreePredictorOptions(final int maxNumCoveredPattern) {
            this(null, false, null, maxNumCoveredPattern);
        }

        /**
         * @return the maxNumCoveredPattern
         */
        public int getMaxNumCoveredPattern() {
            return m_maxNumCoveredPattern;
        }
    }
}
