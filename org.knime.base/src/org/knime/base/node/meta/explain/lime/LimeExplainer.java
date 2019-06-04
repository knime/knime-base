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
 *   May 2, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.knime.base.node.meta.explain.CountingDataContainer;
import org.knime.base.node.meta.explain.DoubleVector;
import org.knime.base.node.meta.explain.Explanation;
import org.knime.base.node.meta.explain.ExplanationToDataCellsConverter;
import org.knime.base.node.meta.explain.ExplanationToMultiRowConverter;
import org.knime.base.node.meta.explain.KnimeDoubleVector;
import org.knime.base.node.meta.explain.feature.FeatureManager;
import org.knime.base.node.meta.explain.lime.node.LIMESettings;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings;
import org.knime.base.node.meta.explain.node.ExplainerLoopEndSettings.PredictionColumnSelectionMode;
import org.knime.base.node.meta.explain.node.ExplanationAggregator;
import org.knime.base.node.meta.explain.util.MissingColumnException;
import org.knime.base.node.meta.explain.util.TablePreparer;
import org.knime.base.node.meta.explain.util.TableSpecUtil;
import org.knime.base.node.meta.explain.util.iter.DoubleIterable;
import org.knime.base.node.meta.explain.util.iter.IterableUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.filter.CloseableDataRowIterable;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Explains predictions for individual rows using the LIME apporach (Linear Interpretable Model Explanations) proposed
 * in the paper '"Why Should I trust You?" Explaining the Predictions of Any Classifier'. The algorithm for tabular data
 * implemented here is not directly outlined in the paper but its Python code can be found here:
 * https://github.com/marcotcr/lime/blob/master/lime/lime_tabular.py
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class LimeExplainer implements ExplanationAggregator {

    private LimeSampler m_sampler;

    private final TablePreparer m_featureTablePreparer;

    private final FeatureManager m_featureManager;

    private LIMESettings m_settings;

    private String m_weightColumnName;

    private Iterator<DataRow> m_rowIterator;

    private int m_maxIterations;

    private int m_currentIteration;

    private ExplainerLoopEndSettings m_endSettings;

    private CountingDataContainer m_endContainer;

    private ExplanationToDataCellsConverter m_explanationConverter;

    private TablePreparer m_predictionTablePreparer;

    private String m_currentRoiKey;

    /**
     * @param settings the settings of the node
     */
    public LimeExplainer(final LIMESettings settings) {
        m_settings = settings;
        m_featureTablePreparer = new TablePreparer(settings.getFeatureCols(), "feature");
        // we can't sample vectors as a whole therefore all vectors are treated as collection of features
        m_featureManager = new FeatureManager(false, m_settings.isUseElementNames());
    }

    /**
     * @return the name of the weight column (typically it will be 'weight' but this name might already be occupied by a
     *         column in the table)
     */
    public String getWeightColumnName() {
        return m_weightColumnName;
    }

    /**
     * @return the maximal number of iteration of the LIME loop i.e. the number of rows in the ROI table
     */
    public int getMaxIterations() {
        return m_maxIterations;
    }

    /**
     * @return the current iteration of the LIME loop i.e. the index of the current row of interest
     */
    public int getCurrentIteration() {
        return m_currentIteration;
    }

    /**
     * Resets the LimeExplainer to its state right after creation.
     */
    public void reset() {
        m_maxIterations = 0;
        m_currentIteration = -1;
        m_rowIterator = null;
        m_sampler = null;
    }

    /**
     * Performs the configuration of the loop start. The first output spec corresponds to the table that has to be
     * predicted by the user model i.e. it contains only the feature columns. The second output spec is for the table
     * used to train the surrogate model. It differs from the first one depending on the column types:
     * <ul>
     * <li>Nominal columns are represented as double column where a 1.0 indicates that the value matches the value of
     * the row of interest</li>
     * <li>Bit/Byte vector columns are split up into one double column per element. The values are also doubles sampled
     * according to the mean and std of the numerical features the elements represent.</li>
     * </ul>
     * Note that we can't create the second spec if vector columns are contained because we have no guarantee about the
     * number of elements a vector has.
     *
     * @param roiSpec {@link DataTableSpec} of the ROI table
     * @param samplingSpec {@link DataTableSpec} of the sampling table
     * @param settings the node settings
     * @return the spec of the table that has to be predicted by the user model and the spec of the table used to learn
     *         the surrogate model
     * @throws InvalidSettingsException if any feature columns are missing from <b>samplingSpec</b>
     */
    public DataTableSpec[] configure(final DataTableSpec roiSpec, final DataTableSpec samplingSpec,
        final LIMESettings settings) throws InvalidSettingsException {
        m_settings = settings;
        m_featureTablePreparer.updateSpecs(roiSpec, settings.getFeatureCols());
        m_featureTablePreparer.checkSpec(samplingSpec);
        final DataTableSpec featureSpec = createInverseSpec();
        m_featureManager.updateWithSpec(featureSpec);
        final DataTableSpec[] specs = new DataTableSpec[2];
        specs[0] = featureSpec;
        if (!m_featureManager.containsCollection()) {
            // we can only create the spec during configure if no collections are contained
            // because there is no guarantee on the number of elements in a collection
            // in most cases the number of elements is likely to match the number of element names
            // in the column spec but it is possible that the number of elements doesn't match the
            // number of element names which would result in different specs during configuration and execution
            specs[1] = createDataSpec();
        }
        return specs;
    }

    private DataTableSpec createDataSpec() {
        final DataTableSpecCreator specCreator = new DataTableSpecCreator();
        final List<String> featureNames = m_featureManager.getFeatureNames().orElseThrow(
            () -> new IllegalStateException("The feature names are not known. This indicates an coding error."));
        for (String name : featureNames) {
            specCreator.addColumns(new DataColumnSpecCreator(name, DoubleCell.TYPE).createSpec());
        }
        final DataTableSpec featureNameSpec = specCreator.createSpec();
        m_weightColumnName = DataTableSpec.getUniqueColumnName(featureNameSpec, "weight");
        specCreator.addColumns(new DataColumnSpecCreator(m_weightColumnName, DoubleCell.TYPE).createSpec());
        return specCreator.createSpec();
    }

    private Kernel createKernel() {
        return new ExponentialKernel(EuclideanDistance.INSTANCE, calculateKernelWidth());
    }

    private double calculateKernelWidth() {
        if (m_settings.isUseManualKernelWidth()) {
            return m_settings.getKernelWidth();
        } else {
            final int numFeatures = m_featureManager.getNumFeatures()
                .orElseThrow(() -> new IllegalStateException("Number of features unknown during execution."));
            // magic formula adapted from the Python LIME implementation
            return Math.sqrt(numFeatures) * 0.75;
        }
    }

    private Iterable<FeatureGroup> createFeatureGroups(final BufferedDataTable samplingTable,
        final ExecutionContext exec) throws Exception {
        final BufferedDataTable table =
            m_featureTablePreparer.createTable(samplingTable, exec.createSubExecutionContext(0.5));
        return consumeSamplingTable(table, exec.createSubProgress(0.5));
    }

    private Iterable<FeatureGroup> consumeSamplingTable(final BufferedDataTable table, final ExecutionMonitor exec) {
        final FeatureGroupsBuilder builder =
            new FeatureGroupsBuilder(m_settings, m_featureTablePreparer.getTableSpec());
        final double total = table.size();
        int i = 0;
        for (DataRow row : table) {
            builder.consume(row);
            exec.setProgress(i / total);
        }
        return builder.build();
    }

    /**
     * @return true if the LIME loop has more iterations i.e. there are more rows to explain
     */
    @Override
    public boolean hasNextIteration() {
        return m_rowIterator.hasNext();
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
    public BufferedDataTable[] getNextTables(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec) throws Exception {
        boolean isFirstIteration = false;
        if (m_sampler == null) {
            isFirstIteration = true;
            initialize(roiTable, samplingTable, exec.createSubExecutionContext(0.2));
        }
        m_currentIteration++;
        return doNextIteration(exec.createSubExecutionContext(isFirstIteration ? 0.8 : 1.0));
    }

    private BufferedDataTable[] doNextIteration(final ExecutionContext exec) {
        CheckUtils.checkState(m_rowIterator.hasNext(),
            "This method must not be called if there are no more rows left to process.");
        final DataRow roi = m_rowIterator.next();
        m_currentRoiKey = roi.getKey().getString();
        final Iterator<LimeSample> sampleIterator = m_sampler.createSampleSet(m_rowIterator.next());
        final BufferedDataContainer inverseContainer = exec.createDataContainer(createInverseSpec());
        final BufferedDataContainer dataContainer = exec.createDataContainer(createDataSpec());
        final long total = m_settings.getExplanationSetSize();
        final double progTotal = total;
        for (long i = 0; sampleIterator.hasNext(); i++) {
            final LimeSample sample = sampleIterator.next();
            final RowKey key = RowKey.createRowKey(i);
            inverseContainer.addRowToTable(new DefaultRow(key, Lists.newArrayList(sample.createInverseCells())));
            final List<DataCell> dataCells = Lists.newLinkedList(sample.createDataCells());
            dataCells.add(new DoubleCell(sample.getWeight()));
            dataContainer.addRowToTable(new DefaultRow(key, dataCells));
            exec.setProgress(i / progTotal, "Created sample " + i + " of " + total);
        }
        inverseContainer.close();
        dataContainer.close();
        return new BufferedDataTable[]{inverseContainer.getTable(), dataContainer.getTable()};
    }

    private void initialize(final BufferedDataTable roiTable, final BufferedDataTable samplingTable,
        final ExecutionContext exec) throws Exception {
        m_maxIterations = (int)roiTable.size();
        m_currentIteration = 0;
        final BufferedDataTable featureTable =
            m_featureTablePreparer.createTable(roiTable, exec.createSilentSubExecutionContext(0));
        final Iterable<FeatureGroup> featureGroups =
            createFeatureGroups(samplingTable, exec.createSubExecutionContext(0.2));
        m_sampler = new LimeSampler(featureGroups, createKernel(), m_settings.getExplanationSetSize());
        m_rowIterator = featureTable.iterator();
    }

    private DataTableSpec createInverseSpec() {
        return m_featureTablePreparer.getTableSpec();
    }

    @Override
    public DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final ExplainerLoopEndSettings settings) throws InvalidSettingsException {
        checkSpecs(inSpecs[0], inSpecs[1]);
        updateWithLoopEndSettings(settings, inSpecs[0]);
        return new DataTableSpec[] {createLoopEndSpec().orElse(null)};
    }

    private void checkSpecs(final DataTableSpec predictionSpec, final DataTableSpec explanationSetSpec) throws InvalidSettingsException {
        // TODO
    }

    private Optional<DataTableSpec> createLoopEndSpec() {
        final Optional<List<String>> featureNames = m_featureManager.getFeatureNames();
        return featureNames.map(m_explanationConverter::createSpec);
    }

    private void updateWithLoopEndSettings(final ExplainerLoopEndSettings settings,
        final DataTableSpec predictionSpec) {
        m_endSettings = settings;
        m_predictionTablePreparer = createPredictionTablePreparer(settings.getPredictionColumnSelectionMode(),
            settings.getPredictionCols(), predictionSpec, m_featureTablePreparer.getTableSpec());
        m_explanationConverter =
            new ExplanationToMultiRowConverter(m_predictionTablePreparer.getTableSpec().getColumnNames(), false);
    }

    private static TablePreparer createPredictionTablePreparer(final PredictionColumnSelectionMode mode,
        final DataColumnSpecFilterConfiguration predictionFilter, final DataTableSpec predictionSpec,
        final DataTableSpec featureSpec) {
        switch (mode) {
            case AUTOMATIC:
                final DataTableSpec filtered =
                    TableSpecUtil.keepOnly(TableSpecUtil.difference(predictionSpec, featureSpec),
                        c -> c.getType().isCompatible(DoubleValue.class));
                return new TablePreparer(filtered, "prediction");
            case MANUAL:
                return new TablePreparer(predictionFilter, "prediction");
            default:
                throw new IllegalArgumentException("Unknown PredictionColumnSelectionMode '" + mode + "'.");
        }
    }

    @Override
    public void consumePredictions(final BufferedDataTable[] inData,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        final BufferedDataTable predictionTable = inData[0];
        final BufferedDataTable featureTable = inData[1];
        CheckUtils.checkArgument(predictionTable.size() == featureTable.size(),
            "Prediction and feature table have varying number of rows.");
        if (m_endContainer == null) {
            // We are in the execution of the loop end if this method is called so we should have
            // all the feature names since the loop start must have been executed at least once
            m_endContainer = new CountingDataContainer(
                exec.createDataContainer(createLoopEndSpec().orElseThrow(IllegalStateException::new)));
        }
        final int numRows = (int)predictionTable.size();
        final int numFeatures = m_featureManager.getNumFeatures().orElseThrow(IllegalStateException::new);
        final LimePredictionConsumer consumer = new LimePredictionConsumer(numRows, numFeatures,
            m_predictionTablePreparer.getNumColumns(), m_endSettings.isRegularizeExplanations(),
            m_endSettings.getMaxActiveFeatures(), m_endSettings.getAlpha());
        final DoubleIterable weights = createWeightIterable(featureTable);
        final DoubleIterable[] predictions =
            createPredictionIterables(predictionTable, exec.createSilentSubExecutionContext(0));
        final Iterable<DoubleVector> features = createFeatureVectors(featureTable);
        final Explanation explanation = consumer.evaluatePredictions(predictions, features, weights, m_currentRoiKey);
        m_explanationConverter.convertAndWrite(explanation, m_endContainer::accept);
    }

    private Iterable<DoubleVector> createFeatureVectors(final BufferedDataTable featureTable) {
        final List<String> featureNames = m_featureManager.getFeatureNames().orElseThrow(IllegalStateException::new);
        final DataTableSpec spec = featureTable.getDataTableSpec();
        final int[] featureIdxs = spec.columnsToIndices(featureNames.toArray(new String[featureNames.size()]));
        final CloseableDataRowIterable rowIterable = featureTable.filter(TableFilter.materializeCols(featureIdxs));
        return Iterables.transform(rowIterable,
            r -> new KnimeDoubleVector(r, "feature", i -> featureIdxs[i], featureIdxs.length));
    }

    private DoubleIterable createWeightIterable(final BufferedDataTable featureTable) {
        final DataTableSpec tableSpec = featureTable.getDataTableSpec();
        final int weightColIdx = tableSpec.findColumnIndex(m_weightColumnName);
        return createDoubleIterable(featureTable, weightColIdx);
    }

    private DoubleIterable[] createPredictionIterables(final BufferedDataTable predictionTable,
        final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException, MissingColumnException {
        final BufferedDataTable filtered = m_predictionTablePreparer.createTable(predictionTable, exec);
        final DoubleIterable[] iterables = new DoubleIterable[m_predictionTablePreparer.getNumColumns()];
        for (int i = 0; i < iterables.length; i++) {
            iterables[i] = createDoubleIterable(filtered, i);
        }
        return iterables;
    }

    private static DoubleIterable createDoubleIterable(final BufferedDataTable table, final int colIdx) {
        return IterableUtils.toDoubleIterable(table.filter(TableFilter.materializeCols(colIdx)),
            r -> getAsDouble(r.getCell(colIdx)));
    }

    private static double getAsDouble(final DataCell cell) {
        if (cell.isMissing()) {
            throw new MissingValueException((MissingValue)cell, "Missing values are not allowed.");
        }
        CheckUtils.checkArgument(cell instanceof DoubleValue, "Weights must be numeric");
        return ((DoubleValue)cell).getDoubleValue();
    }

    @Override
    public BufferedDataTable[] getTables() {
        return new BufferedDataTable[] {m_endContainer.getTable()};
    }

}
