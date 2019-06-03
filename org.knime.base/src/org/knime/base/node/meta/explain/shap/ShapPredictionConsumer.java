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
 *   May 24, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import java.util.Iterator;
import java.util.List;

import org.knime.base.node.meta.explain.CountingDataContainer;
import org.knime.base.node.meta.explain.DefaultExplanation.DefaultExplanationBuilder;
import org.knime.base.node.meta.explain.ExplanationToDataCellsConverter;
import org.knime.base.node.meta.explain.ExplanationToMultiRowConverter;
import org.knime.base.node.meta.explain.KnimePredictionVector;
import org.knime.base.node.meta.explain.PredictionVector;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings.PredictionColumnSelectionMode;
import org.knime.base.node.meta.explain.util.MissingColumnException;
import org.knime.base.node.meta.explain.util.TablePreparer;
import org.knime.base.node.meta.explain.util.TableSpecUtil;
import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.knime.base.node.meta.explain.util.iter.DoubleIterator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Iterators;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ShapPredictionConsumer {

    private List<String> m_featureNames;

    private TablePreparer m_predictionTablePreparer;

    private ShapWLS m_shapWLS;

    private CountingDataContainer m_loopEndContainer;

    private ExplanationToDataCellsConverter m_explanationConverter;

    private DoubleIterable m_samplingWeights;

    private double[] m_nullFx;

    private int m_samplingSetSize;

    private ExplainerLoopEndSettings m_settings;

    /**
     *
     */
    public ShapPredictionConsumer() {
        // the members are later initialized based on the users configuration and data
    }

    void consumePredictions(final BufferedDataTable predictedTable, final ShapIteration shapIteration,
        final ExecutionContext exec) throws Exception {
        try (CloseableRowIterator iter =
            m_predictionTablePreparer.createIterator(predictedTable, exec.createSilentSubExecutionContext(0))) {
            final Iterator<PredictionVector> predictionIterator = Iterators.transform(iter, KnimePredictionVector::new);
            if (m_loopEndContainer == null) {
                // in the first iteration we only predicted the sampling table in order to obtain nullFx
                m_loopEndContainer = new CountingDataContainer(
                    exec.createDataContainer(m_explanationConverter.createSpec(m_featureNames)));
                m_samplingSetSize = getAsInt(predictedTable.size());
                initializeNullPredictions(predictionIterator, exec);
            } else {
                CheckUtils.checkArgument(shapIteration != null,
                    "The shapIteration must not be null from the second iteration on.");
                explainCurrentRoi(predictionIterator, shapIteration, exec);
            }
        }
    }

    DataTableSpec getExplanationSpec() {
        if (m_featureNames != null) {
            return m_explanationConverter.createSpec(m_featureNames);
        } else {
            // without feature names, we can't configure
            // can only happen if any collection column has no element names assigned
            // and the loop start has not been executed, yet
            return null;
        }
    }

    void setSamplingWeights(final DoubleIterable samplingWeights) {
        m_samplingWeights = samplingWeights;
    }

    BufferedDataTable getExplanationTable() {
        CheckUtils.checkState(m_loopEndContainer != null,
            "The loop end container is null, this indicates a coding error.");
        return m_loopEndContainer.getTable();
    }

    void updateSettings(final DataTableSpec tableSpec, final ExplainerLoopEndSettings settings,
        final List<String> featureNames, final DataTableSpec featureSpec) {
        m_settings = settings;
        m_featureNames = featureNames;
        updatePredictionTablePreparer(tableSpec, settings, featureSpec);
        m_explanationConverter =
            new ExplanationToMultiRowConverter(m_predictionTablePreparer.getTableSpec().getColumnNames());
    }

    void reset() {
        if (m_loopEndContainer != null) {
            m_loopEndContainer.close();
        }
        m_loopEndContainer = null;
        m_explanationConverter = null;
    }

    private void explainCurrentRoi(final Iterator<PredictionVector> predictionIterator,
        final ShapIteration shapIteration, final ExecutionMonitor monitor) {
        final PredictionVector currentFx = predictionIterator.next();
        monitor.setMessage("Aggregating predictions");
        final double[][] aggregatedPredictions =
            aggregatePredictionsPerSample(predictionIterator, shapIteration.getNumberOfSamples());
        final double[] shapWeights = shapIteration.getWeights();
        final Mask[] masks = shapIteration.getMasks();
        final int numPredictions = currentFx.size();
        final int nFeatures = masks[0].getNumberOfFeatures();
        final DefaultExplanationBuilder explanationBuilder =
            new DefaultExplanationBuilder(shapIteration.getRoiKey(), numPredictions, nFeatures);
        for (int i = 0; i < numPredictions; i++) {
            final double fx = currentFx.get(i);
            explanationBuilder.setActualPredictionAndDeviation(i, fx, m_nullFx[i]);
            monitor.setMessage("Calculating explanation for target " + (i + 1));
            final double[] wlsCoefficients =
                m_shapWLS.getWLSCoefficients(masks, aggregatedPredictions[i], i, fx, shapWeights);
            for (int j = 0; j < nFeatures; j++) {
                explanationBuilder.setExplanationValue(i, j, wlsCoefficients[j]);
            }
        }
        m_explanationConverter.convertAndWrite(explanationBuilder.build(), m_loopEndContainer);
    }

    private double[][] aggregatePredictionsPerSample(final Iterator<PredictionVector> iter, final int nSamples) {
        CheckUtils.checkState(m_samplingWeights != null, "No sampling weights set.");
        final int numPredCols = m_predictionTablePreparer.getNumColumns();
        final double[][] aggregatedPredictions = new double[numPredCols][nSamples];
        int sampleIdx = -1;
        int i = 0;
        DoubleIterator weightIter = m_samplingWeights.iterator();
        while (iter.hasNext()) {
            final PredictionVector row = iter.next();
            if (i % m_samplingSetSize == 0) {
                weightIter = m_samplingWeights.iterator();
                sampleIdx++;
            }
            assert weightIter.hasNext() : "The number of weights does not match the size of the sampling table.";
            final double weight = weightIter.next();
            for (int j = 0; j < numPredCols; j++) {
                aggregatedPredictions[j][sampleIdx] += row.get(j) * weight;
            }
            i++;
        }

        return aggregatedPredictions;
    }

    private void updatePredictionTablePreparer(final DataTableSpec predictionSpec,
        final ExplainerLoopEndSettings settings, final DataTableSpec featureSpec) {
        if (settings.getPredictionColumnSelectionMode() == PredictionColumnSelectionMode.AUTOMATIC) {
            final DataTableSpec predictionCols =
                TableSpecUtil.keepOnly(TableSpecUtil.difference(predictionSpec, featureSpec),
                    c -> c.getType().isCompatible(DoubleValue.class));
            m_predictionTablePreparer = new TablePreparer(predictionCols, "prediction");
        } else {
            m_predictionTablePreparer = new TablePreparer(settings.getPredictionCols(), "prediction");
            m_predictionTablePreparer.updateSpecs(predictionSpec, settings.getPredictionCols());
        }
    }

    private static int getAsInt(final long val) {
        CheckUtils.checkArgument(val <= Integer.MAX_VALUE, "The provided value %s exceeds Integer.MAX_VALUE", val);
        CheckUtils.checkArgument(val >= Integer.MIN_VALUE, "The provided value %s is smaller than Integer.MIN_VALUE",
            val);
        return (int)val;
    }

    private void initializeNullPredictions(final Iterator<PredictionVector> predictionIterator,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        final double size = m_samplingSetSize;
        long rowIdx = 1;
        final DoubleIterator weightIter = m_samplingWeights.iterator();
        m_nullFx = new double[m_predictionTablePreparer.getNumColumns()];
        while (predictionIterator.hasNext()) {
            exec.checkCanceled();
            final PredictionVector row = predictionIterator.next();
            assert weightIter.hasNext();
            final double weight = weightIter.next();
            for (int i = 0; i < m_nullFx.length; i++) {
                m_nullFx[i] += row.get(i) * weight;
            }
            exec.setProgress(rowIdx / size);
        }
        // TODO allow different link functions e.g. logit
        // TODO get the number of active features and alpha from user
        m_shapWLS = new ShapWLS(m_nullFx, d -> d, m_settings.getAlpha(),
            m_settings.isRegularizeExplanations() ? m_settings.getMaxActiveFeatures() : Integer.MAX_VALUE);
    }

}
