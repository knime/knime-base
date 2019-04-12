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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.base.node.meta.explain.feature.FeatureManager;
import org.knime.base.node.meta.explain.feature.KnimeFeatureVectorIterator;
import org.knime.base.node.meta.explain.feature.PerturberFactory;
import org.knime.base.node.meta.explain.feature.SimpleReplacingPerturberFactory;
import org.knime.base.node.meta.explain.feature.VectorEnabledPerturberFactory;
import org.knime.base.node.meta.explain.shapley.node.ShapleyValuesSettings;
import org.knime.base.node.meta.explain.util.DefaultRowSampler;
import org.knime.base.node.meta.explain.util.RowSampler;
import org.knime.base.node.meta.explain.util.TablePreparer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

import com.google.common.collect.Sets;

/**
 * Estimates Shapley Values based on KNIME's {@link BufferedDataTable}s.
 * This class contains the logic for the calculations of both the Shapley Values Loop Start and
 * Loop End nodes, and is passed between the two.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class KnimeShapleyValuesEstimator {

    private final ShapleyValues m_algorithm;

    private final TablePreparer m_featureTablePreparer;

    private final TablePreparer m_predictionTablePreparer;

    private final FeatureManager m_featureManager;

    private ShapleyValuesSettings m_settings;

    private RowTransformer m_rowTransformer;

    private BufferedDataContainer m_loopEndContainer;

    private int m_numIterations = -1;

    private int m_currentIteration = -1;

    /**
     * Creates a ShapleyValueEstimator with the provided settings
     *
     * @param settings
     */
    public KnimeShapleyValuesEstimator(final ShapleyValuesSettings settings) {
        m_algorithm = new ShapleyValues(settings.getIterationsPerFeature(), settings.getSeed());
        m_settings = settings;
        m_featureTablePreparer = new TablePreparer(settings.getFeatureCols(), "feature");
        m_predictionTablePreparer = new TablePreparer(settings.getPredictionCols(), "prediction");
        m_featureManager =
            new FeatureManager(m_settings.isTreatAllColumnsAsSingleFeature(), m_settings.isDontUseElementNames());
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
        m_predictionTablePreparer.updateSpecs(roiSpec, settings.getPredictionCols());
        m_featureTablePreparer.checkSpec(samplingSpec);
        final DataTableSpec featureTableSpec = m_featureTablePreparer.getTableSpec();
        m_featureManager.updateWithSpec(featureTableSpec);
        checkPredictionAndFeaturesDisjunct();
        return featureTableSpec;
    }

    private void checkPredictionAndFeaturesDisjunct() throws InvalidSettingsException {
        final DataTableSpec featureTableSpec = m_featureTablePreparer.getTableSpec();
        final DataTableSpec predictionTableSpec = m_predictionTablePreparer.getTableSpec();
        final Set<String> featureColumns = createSet(featureTableSpec.getColumnNames());
        final Set<String> predictionColumns = createSet(predictionTableSpec.getColumnNames());
        final Set<String> intersection = Sets.intersection(featureColumns, predictionColumns);
        CheckUtils.checkSetting(intersection.isEmpty(),
            "The following feature columns are also set as prediction columns: %s", intersection);
    }

    private static <E> Set<E> createSet(final E[] array) {
        return Arrays.stream(array).collect(Collectors.toSet());
    }

    /**
     * @param roiTable table containing the rows for which to explain the predictions
     * @param samplingTable table to use for sampling
     * @param exec {@link ExecutionContext} for reporting progress and creating tables
     * @return a table containing perturbed rows
     * @throws Exception
     */
    public BufferedDataTable executeLoopStart(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec) throws Exception {
        checkForEmptyTables(roiTable, samplingTable);
        m_featureTablePreparer.checkSpec(samplingTable.getDataTableSpec());
        boolean isFirstIteration = false;
        if (m_rowTransformer == null) {
            isFirstIteration = true;
            m_numIterations = calculateNumberofIterations(roiTable);
            initializeRowTransformer(roiTable, samplingTable, exec);
        }
        m_currentIteration++;

        return m_rowTransformer.next(exec.createSubExecutionContext(isFirstIteration ? 0.9 : 1.0));

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
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException, Exception {
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

    /**
     * Makes sure that all resources are released
     */
    public void reset() {
        if (m_rowTransformer != null) {
            m_rowTransformer.close();
        }
        m_rowTransformer = null;
        if (m_loopEndContainer != null && m_loopEndContainer.isOpen()) {
            m_loopEndContainer.close();
        }
        m_loopEndContainer = null;
        m_currentIteration = -1;
        m_currentIteration = -1;
    }

    private static void checkForEmptyTables(final BufferedDataTable roiTable, final BufferedDataTable samplingTable) {
        CheckUtils.checkArgument(roiTable.size() > 0, "The ROI table must contain at least one row.");
        CheckUtils.checkArgument(samplingTable.size() > 0, "The sampling table must contain at least one row.");
    }

    private void updateFeatureManager(final DataTable roiFeatureTable, final DataTable samplingFeatureTable) {
        final DataRow roiRow = roiFeatureTable.iterator().next();
        // TODO figure out how to handle the case where configure and execution state don't match
        m_featureManager.updateWithRow(roiRow);
        final DataRow samplingRow = samplingFeatureTable.iterator().next();
        CheckUtils.checkState(m_featureManager.hasSameNumberOfFeatures(samplingRow),
            "The rows in the sampling table contain a different number of features than the rows in the ROI table.");
    }

    private PerturberFactory<DataRow, Set<Integer>, DataCell[]> createPerturberFactory(final DataTable samplingTable,
        final long numRows, final ExecutionMonitor prog) throws Exception {
        final DataRow[] samplingSet = createSamplingSet(samplingTable, numRows, prog);
        final RowSampler rowSampler = new DefaultRowSampler(samplingSet);
        if (m_featureManager.containsCollection()) {
            final int[] numFeaturesPerCol = m_featureManager.getNumberOfFeaturesPerColumn();
            return new VectorEnabledPerturberFactory(rowSampler, m_featureManager.getFactories(), numFeaturesPerCol);
        } else {
            return new SimpleReplacingPerturberFactory(rowSampler);
        }

    }

    /**
     * @param samplingTable
     * @return
     * @throws InvalidSettingsException
     */
    private static DataRow[] createSamplingSet(final DataTable samplingFeatureTable, final long numRows,
        final ExecutionMonitor prog) throws Exception {
        // TODO check if we can implement Sobol (quasi random) sampling
        // in an efficient way using BufferedDataTables or caching
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
     * @param inSpec {@link DataTableSpec} that must contain the prediction columns
     * @return null if we don't know the number of features, otherwise
     * @throws InvalidSettingsException
     */
    public DataTableSpec configureLoopEnd(final DataTableSpec inSpec) throws InvalidSettingsException {
        final DataTableSpec predictionTableSpec = m_predictionTablePreparer.getTableSpec();
        Optional<List<String>> optionalFeatureNames = m_featureManager.getFeatureNames();
        if (!optionalFeatureNames.isPresent()) {
            // without feature names, we can't configure
            // can only happen if any collection column has no element names assigned
            // and the loop start has not been executed, yet
            return null;
        }
        return createLoopEndSpec(optionalFeatureNames.get(), predictionTableSpec);
    }

    /**
     * Calculates the Shapley Values for the current chunk of rows
     *
     * @param predictionTable table containing the prediction for the perturbed rows
     * @param exec {@link ExecutionContext} for reporting progress and creating tables
     * @throws Exception if something goes wrong or the execution is cancelled
     */
    public void consumePredictions(final BufferedDataTable predictionTable, final ExecutionContext exec)
        throws Exception {
        exec.setMessage("Calculating Shapley Values.");
        final DataTable filteredTable = m_predictionTablePreparer.createTable(predictionTable, exec);
        final Predictor predictor = new Predictor(filteredTable.iterator(), m_algorithm,
            m_featureManager.getNumFeatures().orElseThrow(() -> new IllegalStateException(
                "The number of features must be known during the execution of the loop end. This is a coding error.")),
            m_predictionTablePreparer.getNumColumns());
        if (m_loopEndContainer == null) {
            final DataTableSpec outputSpec = configureLoopEnd(predictionTable.getDataTableSpec());
            m_loopEndContainer = exec.createDataContainer(outputSpec);
        }
        final long totalLong = getNumberOfInputRowsFromBatchSize(predictionTable.size());
        final double total = totalLong;
        int currentRow = 0;
        while (predictor.hasNext()) {
            exec.setProgress(currentRow / total, "Calculating Shapley Values for row " + currentRow + " of " + totalLong);
            final DataRow nextRow = predictor.next();
            m_loopEndContainer.addRowToTable(nextRow);
            currentRow++;
        }
    }

    private long getNumberOfInputRowsFromBatchSize(final long batchSize) {
        final long chunkSize = m_settings.getChunkSize();
        final long numFeatures = m_featureManager.getNumFeatures().orElseThrow(() -> new IllegalStateException("The number of features must be known in the loop end."));
        final long iterationsPerFeature = m_settings.getIterationsPerFeature();
        final long rowsPerBatch = 2 * chunkSize * numFeatures * iterationsPerFeature;
        return batchSize / rowsPerBatch;
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
        CheckUtils.checkState(!m_loopEndContainer.isClosed(),
            "The loop end container is already closed, this indicates a coding error.");
        m_loopEndContainer.close();
        return m_loopEndContainer.getTable();
    }

    private static DataTableSpec createLoopEndSpec(final List<String> featureNames,
        final DataTableSpec predictionTableSpec) {
        final DataTableSpecCreator dtsc = new DataTableSpecCreator();
        for (DataColumnSpec predictionColSpec : predictionTableSpec) {
            final String predictionColName = predictionColSpec.getName();
            for (final String featureName : featureNames) {
                // TODO make this configurable (similar to GroupBy node)
                final String name = featureName + " (" + predictionColName + ")";
                final DataColumnSpecCreator dcsc = new DataColumnSpecCreator(name, DoubleCell.TYPE);
                dtsc.addColumns(dcsc.createSpec());
            }
        }
        return dtsc.createSpec();
    }
}
