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
 *   Apr 3, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.knime.base.node.meta.explain.CountingDataContainer;
import org.knime.base.node.meta.explain.Explanation;
import org.knime.base.node.meta.explain.ExplanationToDataCellsConverter;
import org.knime.base.node.meta.explain.ExplanationToMultiRowConverter;
import org.knime.base.node.meta.explain.KnimePredictionVector;
import org.knime.base.node.meta.explain.PredictionVector;
import org.knime.base.node.meta.explain.feature.FeatureManager;
import org.knime.base.node.meta.explain.feature.KnimeFeatureVectorIterator;
import org.knime.base.node.meta.explain.feature.PerturberFactory;
import org.knime.base.node.meta.explain.feature.SimpleReplacingPerturberFactory;
import org.knime.base.node.meta.explain.feature.VectorEnabledPerturberFactory;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings.PredictionColumnSelectionMode;
import org.knime.base.node.meta.explain.shapley.node.ShapleyValuesSettings;
import org.knime.base.node.meta.explain.util.DefaultRowSampler;
import org.knime.base.node.meta.explain.util.MissingColumnException;
import org.knime.base.node.meta.explain.util.RowSampler;
import org.knime.base.node.meta.explain.util.TablePreparer;
import org.knime.base.node.meta.explain.util.TableSpecUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
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
 * Estimates Shapley Values based on KNIME's {@link BufferedDataTable}s. This class contains the logic for the
 * calculations of both the Shapley Values Loop Start and Loop End nodes, and is passed between the two.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class KnimeShapleyValuesEstimator {

    private final ShapleyValues m_algorithm;

    private final TablePreparer m_featureTablePreparer;

    private TablePreparer m_predictionTablePreparer;

    private final FeatureManager m_featureManager;

    private ShapleyValuesSettings m_settings;

    private RowTransformer m_rowTransformer;

    private CountingDataContainer m_loopEndContainer;

    private ExplanationToDataCellsConverter m_explanationConverter;

    private int m_numIterations = -1;

    private int m_currentIteration = -1;

    private double[] m_nullFx;

    /**
     * Creates a ShapleyValueEstimator with the provided settings
     *
     * @param settings
     */
    public KnimeShapleyValuesEstimator(final ShapleyValuesSettings settings) {
        m_algorithm = new ShapleyValues(settings.getIterationsPerFeature(), settings.getSeed());
        m_settings = settings;
        m_featureTablePreparer = new TablePreparer(settings.getFeatureCols(), "feature");
        //        m_predictionTablePreparer = new TablePreparer(settings.getPredictionCols(), "prediction");
        m_featureManager = new FeatureManager(m_settings.isTreatAllColumnsAsSingleFeature(), true);
    }

    /**
     * Configures the loop start node
     *
     * @param roiSpec {@link DataTableSpec} of the table containing the rows to explain
     * @param samplingSpec {@link DataTableSpec} of the table containing the rows for sampling
     * @param settings the current node settings
     * @return the {@link DataTableSpec} of the table that has to be predicted by the model
     * @throws InvalidSettingsException if some of the selected feature columns are not contained in the sampling table
     */
    public DataTableSpec configureLoopStart(final DataTableSpec roiSpec, final DataTableSpec samplingSpec,
        final ShapleyValuesSettings settings) throws InvalidSettingsException {
        m_settings = settings;
        m_featureTablePreparer.updateSpecs(roiSpec, settings.getFeatureCols());
        m_featureTablePreparer.checkSpec(samplingSpec);
        final DataTableSpec featureTableSpec = m_featureTablePreparer.getTableSpec();
        m_featureManager.updateWithSpec(featureTableSpec);
        return featureTableSpec;
    }

    /**
     * @param roiTable table containing the rows for which to explain the predictions
     * @param samplingTable table to use for sampling
     * @param exec {@link ExecutionContext} for reporting progress and creating tables
     * @return a table containing perturbed rows
     * @throws InvalidSettingsException if the settings are invalid
     * @throws MissingColumnException if any of the feature columns is missing in one of the tables
     * @throws CanceledExecutionException if the execution is canceled by the user
     */
    public BufferedDataTable executeLoopStart(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        checkForEmptyTables(roiTable, samplingTable);
        m_featureTablePreparer.checkSpec(samplingTable.getDataTableSpec());
        m_currentIteration++;
        if (m_rowTransformer == null) {
            // because the first iteration is on the sampling table
            m_numIterations = calculateNumberofIterations(roiTable) + 1;
            initializeRowTransformer(roiTable, samplingTable, exec);
            // the first iteration is used to estimate the mean prediction
            return m_featureTablePreparer.createTable(samplingTable, exec);
        }

        return m_rowTransformer.next(exec);

    }

    /**
     * @return the current iteration
     */
    public int getCurrentIteration() {
        CheckUtils.checkState(m_currentIteration >= 0, "The iterations have not been initialized, yet.");
        return m_currentIteration;
    }

    /**
     * @return the total number of iterations
     */
    public int getNumberOfIterations() {
        CheckUtils.checkState(m_numIterations >= 0, "The iterations have not been initialized, yet.");
        return m_numIterations;
    }

    /**
     * @param roiTable
     * @return
     */
    private int calculateNumberofIterations(final BufferedDataTable roiTable) {
        return (int)Math.ceil(roiTable.size() / ((double)m_settings.getChunkSize()));
    }

    private void initializeRowTransformer(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        final BufferedDataTable roiFeatureTable = m_featureTablePreparer.createTable(roiTable, exec);
        final BufferedDataTable samplingFeatureTable = m_featureTablePreparer.createTable(samplingTable, exec);
        updateFeatureManager(roiFeatureTable, samplingFeatureTable);
        final PerturberFactory<DataRow, Set<Integer>, DataCell[]> perturberFactory =
            createPerturberFactory(samplingFeatureTable, samplingTable.size(), exec.createSubProgress(0.1));
        final KnimeFeatureVectorIterator featureVectorIterator =
            new KnimeFeatureVectorIterator(roiFeatureTable.iterator(), perturberFactory,
                m_featureManager.getNumFeatures().orElseThrow(() -> new IllegalStateException(
                    "At this point the number of features should be known. This is a coding error.")));
        m_rowTransformer = new RowTransformer(featureVectorIterator, m_featureTablePreparer.getTableSpec(), m_algorithm,
            m_settings.getChunkSize());
    }

    private void updateExplanationConverter() {
        final DataTableSpec tableSpec = m_predictionTablePreparer.getTableSpec();
        m_explanationConverter = new ExplanationToMultiRowConverter(tableSpec.getColumnNames());
    }

    /**
     * Makes sure that all resources are released
     */
    public void reset() {
        if (m_rowTransformer != null) {
            m_rowTransformer.close();
        }
        m_rowTransformer = null;
        if (m_loopEndContainer != null) {
            m_loopEndContainer.close();
        }
        m_loopEndContainer = null;
        m_currentIteration = -1;
        m_currentIteration = -1;
        m_nullFx = null;
    }

    private static void checkForEmptyTables(final BufferedDataTable roiTable, final BufferedDataTable samplingTable) {
        CheckUtils.checkArgument(roiTable.size() > 0, "The ROI table must contain at least one row.");
        CheckUtils.checkArgument(samplingTable.size() > 0, "The sampling table must contain at least one row.");
    }

    private void updateFeatureManager(final DataTable roiFeatureTable, final DataTable samplingFeatureTable) {
        final DataRow roiRow = roiFeatureTable.iterator().next();
        m_featureManager.updateWithRow(roiRow);
        final DataRow samplingRow = samplingFeatureTable.iterator().next();
        CheckUtils.checkState(m_featureManager.hasSameNumberOfFeatures(samplingRow),
            "The rows in the sampling table contain a different number of features than the rows in the ROI table.");
    }

    private PerturberFactory<DataRow, Set<Integer>, DataCell[]> createPerturberFactory(final DataTable samplingTable,
        final long numRows, final ExecutionMonitor prog) throws CanceledExecutionException {
        final DataRow[] samplingSet = createSamplingSet(samplingTable, numRows, prog);
        final RowSampler rowSampler = new DefaultRowSampler(samplingSet);
        if (m_featureManager.containsCollection()) {
            final int[] numFeaturesPerCol = m_featureManager.getNumberOfFeaturesPerColumn();
            return new VectorEnabledPerturberFactory(rowSampler, m_featureManager.getFactories(), numFeaturesPerCol);
        } else {
            return new SimpleReplacingPerturberFactory(rowSampler);
        }

    }

    private static DataRow[] createSamplingSet(final DataTable samplingFeatureTable, final long numRows,
        final ExecutionMonitor prog) throws CanceledExecutionException {
        prog.setProgress("Create sampling dataset");
        final List<DataRow> samplingData = new ArrayList<>();
        long current = 0;
        final double total = numRows;
        for (DataRow row : samplingFeatureTable) {
            prog.checkCanceled();
            prog.setProgress(current / total, "Reading row " + row.getKey());
            current++;
            samplingData.add(row);
        }
        return samplingData.toArray(new DataRow[samplingData.size()]);
    }

    /**
     * Performs the configuration of the loop end node
     *
     * @param settings the settings of the loop end
     * @param inSpec the spec of the table containing the black-box predictions
     *
     * @return null if we don't know the number of features, otherwise
     */
    public DataTableSpec configureLoopEnd(final ExplainerLoopEndSettings settings, final DataTableSpec inSpec) {
        updateWithLoopEndSettings(settings, inSpec);
        return getLoopEndSpec();
    }

    private void updateWithLoopEndSettings(final ExplainerLoopEndSettings settings, final DataTableSpec inSpec) {
        initializePredictionTablePreparer(settings, inSpec);
        m_featureManager.setUseElementNames(settings.isUseElementNames());
        updateExplanationConverter();
    }

    private void initializePredictionTablePreparer(final ExplainerLoopEndSettings settings,
        final DataTableSpec inSpec) {
        if (settings.getPredictionColumnSelectionMode() == PredictionColumnSelectionMode.AUTOMATIC) {
            final DataTableSpec predictionCols =
                TableSpecUtil.keepOnly(TableSpecUtil.difference(inSpec, m_featureTablePreparer.getTableSpec()),
                    c -> c.getType().isCompatible(DoubleValue.class));
            m_predictionTablePreparer = new TablePreparer(predictionCols, "prediction");
        } else {
            m_predictionTablePreparer = new TablePreparer(settings.getPredictionCols(), "prediction");
            m_predictionTablePreparer.updateSpecs(inSpec, settings.getPredictionCols());
        }
    }

    /**
     * @return
     */
    private DataTableSpec getLoopEndSpec() {
        Optional<List<String>> optionalFeatureNames = m_featureManager.getFeatureNames();
        if (!optionalFeatureNames.isPresent()) {
            // without feature names, we can't configure
            // can only happen if any collection column has no element names assigned
            // and the loop start has not been executed, yet
            return null;
        }
        return m_explanationConverter.createSpec(optionalFeatureNames.get());
    }

    /**
     * Calculates the Shapley Values for the current chunk of rows
     *
     * @param predictionTable table containing the prediction for the perturbed rows
     * @param exec {@link ExecutionContext} for reporting progress and creating tables
     * @throws MissingColumnException
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    public void consumePredictions(final BufferedDataTable predictionTable, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        exec.setMessage("Calculating Shapley Values.");
        CheckUtils.checkArgument(predictionTable.size() > 0, "The prediction table must not be empty.");
        try (final CloseableRowIterator rowIter =
            m_predictionTablePreparer.createIterator(predictionTable, exec.createSilentSubExecutionContext(0))) {
            final Iterator<PredictionVector> predictionVectorIterator =
                Iterators.transform(rowIter, KnimePredictionVector::new);
            if (m_nullFx == null) {
                // the first iteration only contains the sampling set to estimate the mean predictions nullFx
                initializeNullFx(predictionVectorIterator);
                initializeLoopEndContainer(exec);
            } else {
                consumePredictions(exec, predictionVectorIterator);
            }
        }
    }

    private void initializeLoopEndContainer(final ExecutionContext exec) {
        assert m_loopEndContainer == null;
        final DataTableSpec outputSpec = getLoopEndSpec();
        m_loopEndContainer = new CountingDataContainer(exec.createDataContainer(outputSpec));
    }

    private void consumePredictions(final ExecutionMonitor exec, final Iterator<PredictionVector> rowIter) {
        final Predictor predictor = new Predictor(rowIter, m_algorithm, m_nullFx, getNumFeaturesInLoopEnd(),
            m_predictionTablePreparer.getNumColumns());
        final long totalLong = m_settings.getChunkSize();
        final double total = totalLong;
        int currentRow = 0;
        while (predictor.hasNext()) {
            exec.setProgress(currentRow / total,
                "Calculating Shapley Values for row " + currentRow + " of " + totalLong);
            final Explanation nextExplanation = predictor.next();
            m_explanationConverter.convertAndWrite(nextExplanation, m_loopEndContainer);
            currentRow++;
        }
    }

    /**
     * @return
     */
    private int getNumFeaturesInLoopEnd() {
        return m_featureManager.getNumFeatures().orElseThrow(() -> new IllegalStateException(
            "The number of features must be known during the execution of the loop end."));
    }

    private void initializeNullFx(final Iterator<PredictionVector> rowIter) {
        final int nPredictions = m_predictionTablePreparer.getNumColumns();
        m_nullFx = new double[nPredictions];
        long nRows = 0;
        while (rowIter.hasNext()) {
            nRows++;
            final PredictionVector predictions = rowIter.next();
            for (int i = 0; i < nPredictions; i++) {
                m_nullFx[i] += predictions.get(i);
            }
        }
        assert nRows > 0 : "The prediction iterator may not be empty.";
        for (int i = 0; i < nPredictions; i++) {
            m_nullFx[i] /= nRows;
        }
    }

    /**
     * @return true if there will be another iteration
     */
    public boolean hasNextIteration() {
        return m_rowTransformer.hasNext();
    }

    /**
     * @return the table containing the Shapley Values
     */
    public BufferedDataTable getLoopEndTable() {
        CheckUtils.checkState(m_loopEndContainer != null,
            "The loop end container is null, this indicates a coding error.");
        return m_loopEndContainer.getTable();
    }
}
