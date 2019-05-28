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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.util.CheckUtils;
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
            String SMILE_THREADS_PROPERTY = "smile.threads";
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

    private final List<Integer> m_missingIndices = new ArrayList<>();

    TsneNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // TODO support distance matrices
        return new DataTableSpec[]{createColumnRearranger(inSpecs[DATA_IN_PORT], null).createSpec()};
    }

    /**
     *
     * @param inputSpec the spec of the input table
     * @param embedding the embedding which may be null during configuration
     * @return a ColumnRearranger that can be used to create the output table
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec inputSpec, final double[][] embedding) {
        final ColumnRearranger cr = new ColumnRearranger(inputSpec);
        if (m_removeOriginalColumns.getBooleanValue()) {
            cr.remove(m_features.applyTo(inputSpec).getIncludes());
        }
        final UniqueNameGenerator nameGen = new UniqueNameGenerator(cr.createSpec());
        cr.append(new EmbeddingCellFactory(embedding, createSpecs(nameGen), m_missingIndices));
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
            return new BufferedDataTable[]{
                exec.createColumnRearrangeTable(table, createColumnRearranger(table.getDataTableSpec(), null), exec)};
        }
        final DataTableSpec tableSpec = table.getDataTableSpec();
        final BufferedDataTable filtered = filterTable(table, exec.createSilentSubExecutionContext(0));
        final double[][] data = readIntoDoubleArray(filtered);
        final double[][] embedding = learnEmbedding(data, exec);
        final ColumnRearranger cr = createColumnRearranger(tableSpec, embedding);
        return new BufferedDataTable[]{exec.createColumnRearrangeTable(table, cr, exec.createSilentSubProgress(0.0))};
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
        setSeed();
        final TSNE tsne = new TSNE(data, m_outputDimensions.getIntValue(), m_perplexity.getDoubleValue(),
            m_learningRate.getDoubleValue(), 1);
        final double progressStep = 1.0 / iterations;
        for (int i = 1; i < iterations; i++) {
            monitor.checkCanceled();
            monitor.setProgress(progressStep * i, "Finished iteration " + i);
            tsne.learn(1);
        }
        return tsne.getCoordinates();
    }

    private void setSeed() {
        if (m_seed.getIsActive()) {
            if (!SINGLE_SMILE_THREAD) {
                setWarningMessage(
                    "The VM argument smile.threads is not 1 in which case results of SMILE are not reproducible.");
            }
            smile.math.Math.setSeed(m_seed.getLongValue());
        }
    }

    private double[][] readIntoDoubleArray(final BufferedDataTable features) {
        m_missingIndices.clear();
        final int nCols = features.getDataTableSpec().getNumColumns();
        CheckUtils.checkArgument(features.size() <= Integer.MAX_VALUE,
            "The input table can only have up to Integer.MAX_VALUE rows.");
        final int nRows = (int)features.size();
        final List<double[]> data = new ArrayList<>();
        try (final CloseableRowIterator rowIter = features.iterator()) {
            for (int i = 0; i < nRows; i++) {
                final DataRow row = rowIter.next();
                final double[] vector = readRow(nCols, i, row);
                if (vector != null) {
                    data.add(vector);
                }
            }
            assert !rowIter.hasNext();
        }
        if (data.size() == nCols) {
            // if the data is square, Smile interprets it as distances, we prevent this by adding a dummy row
            data.add(new double[nCols]);
        }
        return data.toArray(new double[data.size()][]);
    }

    private double[] readRow(final int nCols, final int i, final DataRow row) {
        final double[] vector = new double[nCols];
        for (int j = 0; j < nCols; j++) {
            final DataCell cell = row.getCell(j);
            if (cell.isMissing()) {
                handleMissingValue(row, i, j);
                // we can't read the row so continue we return null to indicate that something went wrong
                return null;
            }
            // we currently only support DoubleValues as features, hence this cast should be safe
            vector[j] = ((DoubleValue)row.getCell(j)).getDoubleValue();
        }
        return vector;
    }

    private void handleMissingValue(final DataRow row, final int rowIdx, final int colIdx) {
        if (m_failOnMissingValues.getBooleanValue()) {
            throw new MissingValueException((MissingValue)row.getCell(colIdx), "Missing value detected in row " + row);
        } else {
            // remember the missing idx
            m_missingIndices.add(rowIdx);
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
