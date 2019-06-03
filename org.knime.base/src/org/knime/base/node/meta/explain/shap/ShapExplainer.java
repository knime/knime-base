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
 *   May 10, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.meta.explain.feature.FeatureManager;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings;
import org.knime.base.node.meta.explain.shap.node.ShapLoopStartSettings;
import org.knime.base.node.meta.explain.util.DefaultRandomDataGeneratorFactory;
import org.knime.base.node.meta.explain.util.MissingColumnException;
import org.knime.base.node.meta.explain.util.RandomDataGeneratorFactory;
import org.knime.base.node.meta.explain.util.TablePreparer;
import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.knime.base.node.meta.explain.util.iter.IterableUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ShapExplainer {

    private static final RowKey ROI_KEY = new RowKey("roi");

    private final TablePreparer m_featureTablePreparer;

    private final FeatureManager m_featureManager;

    private ShapLoopStartSettings m_settings;

    private ShapSampler m_sampler;

    private CloseableRowIterator m_rowIterator;

    private int m_maxIterations;

    private int m_currentIteration;

    private static final SampleToRow<ShapSample, RowKey> SAMPLE_TO_ROW = ShapPredictableSampleToRow.INSTANCE;

    private final ShapPredictionConsumer m_predictionConsumer = new ShapPredictionConsumer();

    private ShapIteration m_currentShapIteration;

    /**
     * @param settings
     */
    public ShapExplainer(final ShapLoopStartSettings settings) {
        m_settings = settings;
        m_featureTablePreparer = new TablePreparer(settings.getFeatureCols(), "feature");
        m_featureManager = new FeatureManager(settings.isTreatAllColumnsAsSingleFeature(), true);
    }

    /**
     * @return the maximal number of iteration of the SHAP loop i.e. the number of rows in the ROI table
     */
    public int getMaxIterations() {
        return m_maxIterations;
    }

    /**
     * @return the current iteration of the SHAP loop i.e. the index of the current row of interest
     */
    public int getCurrentIteration() {
        return m_currentIteration;
    }

    /**
     * @return true if the SHAP loop has more iterations i.e. there are more rows to explain
     */
    public boolean hasNextIteration() {
        return m_rowIterator.hasNext();
    }

    /**
     * Resets the LimeExplainer to its state right after creation.
     */
    public void reset() {
        m_maxIterations = 0;
        m_currentIteration = -1;
        if (m_rowIterator != null) {
            m_rowIterator.close();
        }
        m_rowIterator = null;
        m_sampler = null;
        m_predictionConsumer.reset();
    }

    /**
     * Performs the configuration of the loop start.
     *
     * @param roiSpec {@link DataTableSpec} of the ROI table
     * @param samplingSpec {@link DataTableSpec} of the sampling table
     * @param settings the node settings
     * @return the spec of the table that has to be predicted by the user's model
     * @throws InvalidSettingsException if any feature columns are missing from <b>samplingSpec</b>
     */
    public DataTableSpec configureLoopStart(final DataTableSpec roiSpec, final DataTableSpec samplingSpec,
        final ShapLoopStartSettings settings) throws InvalidSettingsException {
        updateLoopStartSettings(roiSpec, settings);
        m_featureTablePreparer.checkSpec(samplingSpec);
        final DataTableSpec featureSpec = m_featureTablePreparer.getTableSpec();
        m_featureManager.updateWithSpec(featureSpec);
        return featureSpec;
    }

    /**
     * Returns the tables for the next row of interest.
     *
     * @param roiTable the table containing the rows to explain (aka rows of interest)
     * @param samplingTable the table used for sampling (here we use it to extract feature statistics)
     * @param exec the execution context of the LIME loop start node
     * @return the table to predict with the user's model and the table for training the surrogate model
     * @throws Exception
     */
    public BufferedDataTable executeLoopStart(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec) throws Exception {
        if (roiTable.size() == 0 || samplingTable.size() == 0) {
            throw new InvalidSettingsException("Neither the roi nor the sampling table may be empty.");
        }
        m_currentIteration++;
        if (m_sampler == null) {
            initialize(roiTable, samplingTable, exec.createSubExecutionContext(0.2));
            // the first loop iteration predicts the sampling set to calculate the mean prediction nullFx
            return m_featureTablePreparer.createTable(samplingTable, exec.createSubExecutionContext(0.8));
        }
        return doNextIteration(exec);
    }

    private BufferedDataTable doNextIteration(final ExecutionContext exec) {
        CheckUtils.checkState(m_rowIterator.hasNext(),
            "This method must not be called if there are no more rows left to process.");
        final DataRow roi = m_rowIterator.next();
        final Iterator<ShapSample> sampleIterator = m_sampler.createSamples(roi);
        final BufferedDataContainer container = exec.createDataContainer(m_featureTablePreparer.getTableSpec());
        // the first row in each iteration is the roi
        container.addRowToTable(new DefaultRow(ROI_KEY, roi));
        final long total = m_settings.getExplanationSetSize();
        final List<ShapSample> currentShapSamples = new ArrayList<>();
        final double progTotal = total;
        for (long i = 0; sampleIterator.hasNext(); i++) {
            final ShapSample sample = sampleIterator.next();
            final RowKey key = RowKey.createRowKey(i);
            SAMPLE_TO_ROW.write(sample, key, container::addRowToTable);
            currentShapSamples.add(sample);
            exec.setProgress(i / progTotal, "Created sample " + i + " of " + total);
        }
        container.close();
        m_currentShapIteration = new ShapIteration(currentShapSamples, roi.getKey().getString());
        return container.getTable();
    }

    private void initialize(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        // plus one because the first iteration predicts the sampling table
        m_maxIterations = (int)roiTable.size() + 1;
        m_currentIteration = 0;
        m_rowIterator = m_featureTablePreparer.createIterator(roiTable, exec.createSilentSubExecutionContext(0));
        updateFeatureManager(samplingTable, exec);
        final SubsetReplacer subsetReplacer = new SubsetReplacer(
            m_featureTablePreparer.createTable(samplingTable, exec.createSilentSubExecutionContext(0)),
            m_featureManager.createRowHandler());
        final int numFeatures = m_featureManager.getNumFeatures()
            .orElseThrow(() -> new IllegalStateException("The number of features must be known during execution."));
        final RandomDataGeneratorFactory rdgFactory = new DefaultRandomDataGeneratorFactory(m_settings.getSeed());
        m_sampler = new ShapSampler(subsetReplacer, new DefaultMaskFactory(numFeatures),
            m_settings.getExplanationSetSize(), numFeatures, rdgFactory);
        m_predictionConsumer.setSamplingWeights(createSamplingWeights(samplingTable));
    }

    private DoubleIterable createSamplingWeights(final BufferedDataTable samplingTable)
        throws InvalidSettingsException {
        final Optional<String> weightColumn = m_settings.getWeightColumn();
        if (!weightColumn.isPresent()) {
            long size = samplingTable.size();
            return IterableUtils.constantDoubleIterable(1.0 / size, size);
        }
        return readWeightsFromTable(samplingTable);
    }

    private DoubleIterable readWeightsFromTable(final BufferedDataTable samplingTable) throws InvalidSettingsException {
        final DataTableSpec tableSpec = samplingTable.getDataTableSpec();
        // the exception should never be thrown because the caller already checked that weightColumn is present
        final String weightColumn = m_settings.getWeightColumn().orElseThrow(IllegalStateException::new);
        final int idx = tableSpec.findColumnIndex(weightColumn);
        CheckUtils.checkSetting(tableSpec.getColumnSpec(idx).getType().isCompatible(DoubleValue.class),
            "The sampling weight column '%s' is not numeric.", weightColumn);
        final double[] samplingWeights = new double[(int)samplingTable.size()];
        double weightSum = 0.0;
        try (CloseableRowIterator iter = samplingTable.filter(TableFilter.materializeCols(idx)).iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                final DataRow row = iter.next();
                final DataCell cell = row.getCell(idx);
                if (cell.isMissing()) {
                    throw new MissingValueException((MissingValue)cell,
                        "Missing value in the weight column of row " + row.getKey() + " detected.");
                }
                // we ensured above that the weight column is numeric
                final double weight = ((DoubleValue)cell).getDoubleValue();
                CheckUtils.checkArgument(weight > 0, "The weight for row %s of the sampling table is not positive.");
                samplingWeights[i] = weight;
                weightSum += weight;
            }
        }
        assert weightSum > 0 : "The sum of all weights must be greater than 0";
        for (int i = 0; i < samplingWeights.length; i++) {
            samplingWeights[i] /= weightSum;
        }
        return IterableUtils.arrayDoubleIterable(samplingWeights, false);
    }

    private void updateFeatureManager(final BufferedDataTable table, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        try (final CloseableRowIterator iter =
            m_featureTablePreparer.createIterator(table, exec.createSilentSubExecutionContext(0))) {
            m_featureManager.updateWithRow(iter.next());
        }
    }

    /**
     * @param roiSpec
     * @param settings
     */
    private void updateLoopStartSettings(final DataTableSpec roiSpec, final ShapLoopStartSettings settings) {
        m_settings = settings;
        m_featureTablePreparer.updateSpecs(roiSpec, settings.getFeatureCols());
    }

    /**
     * Performs configuration of the SHAP Loop End node.
     *
     * @param predSpec {@link DataTableSpec} of the table containing the predictions of the user's model
     * @param settings
     * @return the {@link DataTableSpec} of the Loop End's output table
     */
    public DataTableSpec configureLoopEnd(final DataTableSpec predSpec, final ExplainerLoopEndSettings settings) {
        updateLoopEndSettings(predSpec, settings);
        return m_predictionConsumer.getExplanationSpec();
    }

    /**
     * @param predictedTable the table containing the predictions of the user's model
     * @param exec {@link ExecutionContext} of the SHAP Loop End node
     * @throws Exception
     */
    public void consumePredictions(final BufferedDataTable predictedTable, final ExecutionContext exec)
        throws Exception {
        m_predictionConsumer.consumePredictions(predictedTable, m_currentShapIteration, exec);
    }

    /**
     * @return the table containing the Shapley Values
     */
    public BufferedDataTable getLoopEndTable() {
        return m_predictionConsumer.getExplanationTable();
    }

    private void updateLoopEndSettings(final DataTableSpec predictionSpec, final ExplainerLoopEndSettings settings) {
        m_featureManager.setUseElementNames(settings.isUseElementNames());
        m_predictionConsumer.updateSettings(predictionSpec, settings, m_featureManager.getFeatureNames().orElse(null),
            m_featureTablePreparer.getTableSpec());
    }

}
