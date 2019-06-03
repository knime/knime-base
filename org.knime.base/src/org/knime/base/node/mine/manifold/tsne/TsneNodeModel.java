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
 *   10.05.2019 (Adrian): created
 */
package org.knime.base.node.mine.manifold.tsne;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.util.UniqueNameGenerator;

import smile.manifold.TSNE;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TsneNodeModel extends NodeModel {

    private static final String OUTPUT_PREFIX = "t-SNE dimension ";

    private static final int DATA_IN_PORT = 0;

    private static final boolean SINGLE_SMILE_THREAD = isSingleSmileThread();

    private static final String SMILE_THREADS_PROPERTY = "smile.threads";

    static SettingsModelDoubleBounded createLearningRateModel() {
        return new SettingsModelDoubleBounded("learningRate", 200, 1e-5, Double.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createOutputDimensionsModel() {
        return new SettingsModelIntegerBounded("outputDimensions", 2, 1, Integer.MAX_VALUE);
    }

    static SettingsModelDoubleBounded createPerplexityModel() {
        return new SettingsModelDoubleBounded("perplexity", 20, 1e-5, Double.MAX_VALUE);
    }

    static SettingsModelIntegerBounded createIterationsModel() {
        return new SettingsModelIntegerBounded("iterations", 1000, 1, Integer.MAX_VALUE);
    }

    static SettingsModelSeed createSeedModel() {
        return new SettingsModelSeed("seed", System.currentTimeMillis(), false);
    }

    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createFeaturesModel() {
        // TODO support more data types
        return new SettingsModelColumnFilter2("features", DoubleValue.class);
    }

    static SettingsModelBoolean createRemoveOriginalColumnsModel() {
        return new SettingsModelBoolean("removeOriginalColumns", false);
    }

    static SettingsModelBoolean createFailOnMissingValuesModel() {
        return new SettingsModelBoolean("failOnMissingValues", false);
    }

    private static boolean isSingleSmileThread() {
        try {
            final String smileThreads = System.getProperty(SMILE_THREADS_PROPERTY);
            if (smileThreads == null) {
                System.setProperty(SMILE_THREADS_PROPERTY, "1");
                // the property was not set, and we set it by hand
                return true;
            }
            return Integer.valueOf(smileThreads) == 1;
        } catch (Exception e) {
            // something went wrong in which case we have to assume that smile.threads != 1
            return false;
        }
    }

    private final SettingsModelDoubleBounded m_learningRate = createLearningRateModel();

    private final SettingsModelIntegerBounded m_outputDimensions = createOutputDimensionsModel();

    private final SettingsModelDoubleBounded m_perplexity = createPerplexityModel();

    private final SettingsModelIntegerBounded m_iterations = createIterationsModel();

    private final SettingsModelColumnFilter2 m_features = createFeaturesModel();

    private final SettingsModelBoolean m_removeOriginalColumns = createRemoveOriginalColumnsModel();

    private final SettingsModelBoolean m_failOnMissingValues = createFailOnMissingValuesModel();

    private final SettingsModelSeed m_seed = createSeedModel();

    TsneNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // TODO support distance matrices
        return new DataTableSpec[]{
            createColumnRearranger(inSpecs[DATA_IN_PORT], Collections.emptyList(), null).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec inputSpec, final List<Integer> missingIndices,
        final double[][] embedding) {
        final ColumnRearranger cr = new ColumnRearranger(inputSpec);
        if (m_removeOriginalColumns.getBooleanValue()) {
            cr.remove(m_features.applyTo(inputSpec).getIncludes());
        }
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(cr.createSpec());
        cr.append(new EmbeddingCellFactory(embedding, createSpecs(nameGen), missingIndices));
        return cr;
    }

    private DataColumnSpec[] createSpecs(final UniqueNameGenerator nameGen) {
        final int outDims = m_outputDimensions.getIntValue();
        final DataColumnSpec[] specs = new DataColumnSpec[outDims];
        for (int i = 0; i < outDims; i++) {
            specs[i] = nameGen.newColumn(OUTPUT_PREFIX + i, DoubleCell.TYPE);
        }
        return specs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        final BufferedDataTable table = inData[DATA_IN_PORT];
        if (table.size() == 0) {
            setWarningMessage("The input table was empty.");
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(table,
                createColumnRearranger(table.getDataTableSpec(), Collections.emptyList(), null), exec)};
        }
        final DataTableSpec tableSpec = table.getDataTableSpec();
        final BufferedDataTable filtered = filterTable(table, exec.createSilentSubExecutionContext(0));
        checkMemory(filtered.size(), filtered.getDataTableSpec().getNumColumns());
        try {
            final TsneData data = new TsneData(filtered, m_failOnMissingValues.getBooleanValue());
            final List<Integer> missingIndices = data.getMissingIndices();
            if (!missingIndices.isEmpty()) {
                setWarningMessage(missingIndices.size() + " rows were ignored because they contained missing values.");
            }
            final double[][] embedding = learnEmbedding(data.getData(), exec);
            final ColumnRearranger cr = createColumnRearranger(tableSpec, missingIndices, embedding);
            return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(table, cr, exec.createSilentSubProgress(0.0))};
        } catch (OutOfMemoryError oome) {
            throw new OutOfMemoryError("Couldn't calculate t-SNE because not enough memory is available.");
        }
    }

    private BufferedDataTable filterTable(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        final DataTableSpec tableSpec = table.getDataTableSpec();
        final ColumnRearranger cr = new ColumnRearranger(tableSpec);
        cr.keepOnly(m_features.applyTo(tableSpec).getIncludes());
        return exec.createColumnRearrangeTable(table, cr, exec);
    }

    private double[][] learnEmbedding(final double[][] data, final ExecutionMonitor monitor)
        throws CanceledExecutionException {
        final int iterations = m_iterations.getIntValue();
        monitor.checkCanceled();
        monitor.setMessage("Start learning");
        final int n = data.length;
        // TSNE will reuse this matrix internally (or create it if we don't provide it)
        final double[][] distances = new double[n][n];
        // TODO add support for distance matrices and figure out if we can create the distances
        // in a more memory efficient manner
        smile.math.Math.pdist(data, distances, true, false);
        setSeed();
        final TSNE tsne = new TSNE(distances, m_outputDimensions.getIntValue(), m_perplexity.getDoubleValue(),
            m_learningRate.getDoubleValue(), 1);
        final double progressStep = 1.0 / iterations;
        for (int i = 1; i < iterations; i++) {
            monitor.checkCanceled();
            monitor.setProgress(progressStep * i, "Finished iteration " + i);
            tsne.learn(1);
        }
        return tsne.getCoordinates();
    }

    private void checkMemory(final long rows, final int features) {
        final int outputdims = m_outputDimensions.getIntValue();
        final long requiredMemory = calculateMemoryRequirements(rows, features, outputdims);
        final long freeMemory = Runtime.getRuntime().freeMemory();
        if (requiredMemory > freeMemory) {
            throw new IllegalStateException("Not enough memory available to perform t-SNE. "
                + "Use a smaller table or increase the amount of memory KNIME Analytics Platform can use.");
        }
    }

    private static long calculateMemoryRequirements(final long rows, final int features, final int outputdims) {
        final long dataSize = rows * features;
        final long distanceSize = rows * rows;
        final long outputSize = rows * outputdims;
        // dataSize is allocated by us and the rest is allocated by Smile (all arrays are double)
        return (dataSize + 2 * distanceSize + 2 * outputSize) * 8;
    }

    private void setSeed() {
        if (m_seed.getIsActive()) {
            if (!SINGLE_SMILE_THREAD) {
                setWarningMessage(
                    "The VM argument smile.threads is not 1 in which case results of SMILE are not reproducible.");
            }
            final long seed = m_seed.getIsActive() ? m_seed.getLongValue() : System.currentTimeMillis();
            smile.math.Math.setSeed(seed);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_iterations.saveSettingsTo(settings);
        m_perplexity.saveSettingsTo(settings);
        m_learningRate.saveSettingsTo(settings);
        m_outputDimensions.saveSettingsTo(settings);
        m_features.saveSettingsTo(settings);
        m_removeOriginalColumns.saveSettingsTo(settings);
        m_failOnMissingValues.saveSettingsTo(settings);
        m_seed.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_iterations.validateSettings(settings);
        m_perplexity.validateSettings(settings);
        m_learningRate.validateSettings(settings);
        m_outputDimensions.validateSettings(settings);
        m_features.validateSettings(settings);
        m_removeOriginalColumns.validateSettings(settings);
        m_failOnMissingValues.validateSettings(settings);
        m_seed.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_iterations.loadSettingsFrom(settings);
        m_perplexity.loadSettingsFrom(settings);
        m_learningRate.loadSettingsFrom(settings);
        m_outputDimensions.loadSettingsFrom(settings);
        m_features.loadSettingsFrom(settings);
        m_removeOriginalColumns.loadSettingsFrom(settings);
        m_failOnMissingValues.loadSettingsFrom(settings);
        m_seed.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to reset
    }

    private static class EmbeddingCellFactory extends AbstractCellFactory {

        private final double[][] m_embedding;

        private final int m_nCols;

        private final DataCell[] m_missingCells;

        private final Iterator<Integer> m_missingIterator;

        private int m_nextMissingIdx;

        private int m_embeddingIdx = 0;

        private int m_rowIdx = -1;

        EmbeddingCellFactory(final double[][] embedding, final DataColumnSpec[] specs,
            final Iterable<Integer> missings) {
            super(false, specs);
            m_embedding = embedding;
            m_nCols = specs.length;
            final MissingCell missingCell = new MissingCell("Missing value in the input of t-SNE.");
            m_missingCells = new DataCell[m_nCols];
            Arrays.fill(m_missingCells, missingCell);
            m_missingIterator = missings.iterator();
            advanceMissingPointer();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell[] getCells(final DataRow row) {
            m_rowIdx++;
            if (m_rowIdx == m_nextMissingIdx) {
                advanceMissingPointer();
                return m_missingCells;
            }
            final DataCell[] cells = new DataCell[m_nCols];
            for (int i = 0; i < m_nCols; i++) {
                cells[i] = new DoubleCell(m_embedding[m_embeddingIdx][i]);
            }
            m_embeddingIdx++;
            return cells;
        }

        private void advanceMissingPointer() {
            m_nextMissingIdx = m_missingIterator.hasNext() ? m_missingIterator.next() : -1;
        }

    }

}
