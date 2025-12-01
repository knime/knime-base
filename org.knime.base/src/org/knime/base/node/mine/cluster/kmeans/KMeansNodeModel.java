/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.knime.base.node.mine.cluster.PMMLClusterTranslator;
import org.knime.base.node.mine.cluster.PMMLClusterTranslator.ComparisonMeasure;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Generate a clustering using a fixed number of cluster centers and the k-means algorithm. Right now this works only on
 * {@link DataTable}s holding {@link org.knime.core.data.def.DoubleCell}s (or derivatives thereof).
 *
 * @author Michael Berthold, University of Konstanz
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.10
 */
@SuppressWarnings("restriction")
public final class KMeansNodeModel extends WebUINodeModel<KMeansNodeParameters> {

    /** Constant for the RowKey generation and identification in the view. */
    public static final String CLUSTER = "cluster_";

    private int m_dimension; // dimension of input space

    private int m_nrIgnoredColumns;

    private boolean[] m_ignoreColumn;

    // mapping from cluster to covering data point
    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    private KMeansViewData m_viewData;

    /**
     * Constructor of the k-Means node model.
     */
    KMeansNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE, PMMLPortObject.TYPE},
            KMeansNodeParameters.class);
    }

    /**
     * @return cluster centers' hilite handler
     */
    HiLiteHandler getHiLiteHandler() {
        return m_translator.getFromHiLiteHandler();
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex == 1) {
            return m_translator.getFromHiLiteHandler();
        } else {
            return super.getOutHiLiteHandler(outIndex);
        }
    }

    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hiLiteHdl);
    }

    private static final String INTERNALS_HILITE_MAPPING_FILE = "hilite_mapping.xml.gz";

    private static final String INTERNALS_VIEW_DATA_FILE = "view_data.xml.gz";

    private static final String CFG_DIMENSION = "dimensions";

    private static final String CFG_IGNORED_COLS = "ignoredColumns";

    private static final String CFG_COVERAGE = "clusterCoverage";

    private static final String CFG_CLUSTER = "kMeansCluster";

    private static final String CFG_FEATURE_NAMES = "FeatureNames";

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (getSettings().map(s -> s.m_enableHilite).orElse(false)) {
            final var file = new File(nodeInternDir, INTERNALS_HILITE_MAPPING_FILE);
            try (final var is = new FileInputStream(file)) {
                final var config = NodeSettings.loadFromXML(is);
                m_translator.setMapper(DefaultHiLiteMapper.load(config));
            } catch (InvalidSettingsException e) {
                throw new IOException("Couldn't load hilite mapping", e);
            }
        }

        // Load view data
        final var viewDataFile = new File(nodeInternDir, INTERNALS_VIEW_DATA_FILE);
        if (viewDataFile.exists()) {
            try (final var is = new FileInputStream(viewDataFile)) {
                final var settings = NodeSettings.loadFromXML(is);
                m_dimension = settings.getInt(CFG_DIMENSION);
                m_nrIgnoredColumns = settings.getInt(CFG_IGNORED_COLS);
                int[] clusterCoverage = settings.getIntArray(CFG_COVERAGE);
                int nrOfClusters = clusterCoverage.length;
                double[][] clusters = new double[nrOfClusters][];
                for (int i = 0; i < nrOfClusters; i++) {
                    clusters[i] = settings.getDoubleArray(CFG_CLUSTER + i);
                }
                String[] featureNames = settings.getStringArray(CFG_FEATURE_NAMES);
                m_viewData =
                    new KMeansViewData(clusters, clusterCoverage, m_dimension - m_nrIgnoredColumns, featureNames);
            } catch (InvalidSettingsException e) {
                throw new IOException("Couldn't load view data", e);
            }
        }
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (getSettings().map(s -> s.m_enableHilite).orElse(false)) {
            final var file = new File(nodeInternDir, INTERNALS_HILITE_MAPPING_FILE);
            final var settings = new NodeSettings("hilite_mapping");
            final var mapper = m_translator.getMapper();
            if (mapper instanceof DefaultHiLiteMapper dmapper) {
                dmapper.save(settings);
            }
            try (final var os = new FileOutputStream(file)) {
                settings.saveToXML(os);
            }
        }

        // Save view data
        if (m_viewData != null) {
            final var viewDataFile = new File(nodeInternDir, INTERNALS_VIEW_DATA_FILE);
            final var internalSettings = new NodeSettings("kMeans");
            internalSettings.addInt(CFG_DIMENSION, m_dimension);
            internalSettings.addInt(CFG_IGNORED_COLS, m_nrIgnoredColumns);
            internalSettings.addIntArray(CFG_COVERAGE, m_viewData.clusterCoverage());
            for (int i = 0; i < m_viewData.clusterCoverage().length; i++) {
                internalSettings.addDoubleArray(CFG_CLUSTER + i, m_viewData.clusters()[i]);
            }
            internalSettings.addStringArray(CFG_FEATURE_NAMES, m_viewData.featureNames());
            try (final var os = new FileOutputStream(viewDataFile)) {
                internalSettings.saveToXML(os);
            }
        }
    }

    /**
     * Generate new clustering based on InputDataTable and specified number of clusters. Currently the objective
     * function only looks for cluster centers that are extremely similar to the first n patterns...
     *
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] data, final ExecutionContext exec,
        final KMeansNodeParameters modelSettings) throws Exception {
        // FIXME actually do something useful with missing values!
        BufferedDataTable inData = (BufferedDataTable)data[0];
        DataTableSpec spec = inData.getDataTableSpec();
        // get dimension of feature space
        m_dimension = inData.getDataTableSpec().getNumColumns();
        HashMap<RowKey, Set<RowKey>> mapping = new HashMap<RowKey, Set<RowKey>>();
        addExcludeColumnsToIgnoreList(modelSettings, spec);
        double[][] clusters = initializeClusters(modelSettings, inData);

        // also keep counts of how many patterns fall in a specific cluster
        int[] clusterCoverage = new int[modelSettings.m_nrOfClusters];

        // --------- create clusters --------------
        // reserve space for cluster center updates (do batch update!)
        double[][] delta = new double[modelSettings.m_nrOfClusters][];
        for (int c = 0; c < modelSettings.m_nrOfClusters; c++) {
            delta[c] = new double[m_dimension - m_nrIgnoredColumns];
        }

        // main loop - until clusters stop changing or maxNrIterations reached
        int currentIteration = 0;
        boolean finished = false;
        while ((!finished) && (currentIteration < modelSettings.m_maxIterations)) {
            exec.checkCanceled();
            exec.setProgress(currentIteration / (double)modelSettings.m_maxIterations, "Iteration " + currentIteration);
            // initialize counts and cluster-deltas
            for (int c = 0; c < modelSettings.m_nrOfClusters; c++) {
                clusterCoverage[c] = 0;
                delta[c] = new double[m_dimension - m_nrIgnoredColumns];
                int deltaPos = 0;
                for (int i = 0; i < m_dimension; i++) {
                    if (!m_ignoreColumn[i]) {
                        delta[c][deltaPos++] = 0.0;
                    }
                }
            }
            // assume that we are done (i.e. clusters have stopped changing)
            finished = true;
            RowIterator rowIt = inData.iterator(); // first training example
            while (rowIt.hasNext()) {
                DataRow currentRow = rowIt.next();
                int winner = findClosestPrototypeFor(modelSettings, currentRow, clusters);
                if (winner >= 0) {
                    // update winning cluster centers delta
                    int deltaPos = 0;
                    for (int i = 0; i < m_dimension; i++) {
                        DataCell currentCell = currentRow.getCell(i);
                        if (!m_ignoreColumn[i]) {
                            if (!currentCell.isMissing()) {
                                delta[winner][deltaPos] += ((DoubleValue)(currentCell)).getDoubleValue();
                            } else {
                                throw new Exception("Missing Values not (yet) allowed in k-Means.");
                            }
                            deltaPos++;
                        }
                    }
                    clusterCoverage[winner]++;
                } else {
                    // we didn't find any winner - very odd
                    assert (winner >= 0); // let's report this during
                    // debugging!
                    // otherwise just don't reproduce result
                    throw new IllegalStateException("No winner found: " + winner);
                }
            }
            // update cluster centers
            finished = updateClusterCenters(modelSettings, clusterCoverage, clusters, delta);
            currentIteration++;
        } // while(!finished & nrIt<maxNrIt)
          // create list of feature names
        int k = 0; // index of not-ignored columns
        int j = 0; // index of column
        String[] featureNames = new String[m_dimension];
        do {
            if (!m_ignoreColumn[j]) {
                featureNames[k] = spec.getColumnSpec(j).getName();
                k++;
            }
            j++;
        } while (j < m_dimension);
        // create output container and also mapping for HiLiteing
        BufferedDataContainer labeledInput = exec.createDataContainer(createAppendedSpec(modelSettings, spec));
        for (DataRow row : inData) {
            int winner = findClosestPrototypeFor(modelSettings, row, clusters);
            DataCell cell = new StringCell(CLUSTER + winner);
            labeledInput.addRowToTable(new AppendedColumnRow(row, cell));
            if (modelSettings.m_enableHilite) {
                RowKey key = new RowKey(CLUSTER + winner);
                if (mapping.get(key) == null) {
                    Set<RowKey> set = new HashSet<RowKey>();
                    set.add(row.getKey());
                    mapping.put(key, set);
                } else {
                    mapping.get(key).add(row.getKey());
                }
            }
        }
        labeledInput.close();
        if (modelSettings.m_enableHilite) {
            m_translator.setMapper(new DefaultHiLiteMapper(mapping));
        }
        BufferedDataTable outData = labeledInput.getTable();

        // handle the PMML output port
        PMMLPortObjectSpec pmmlOutSpec = createPMMLSpec(modelSettings, null, spec);
        PMMLPortObject outPMMLPort = new PMMLPortObject(pmmlOutSpec, null, spec);
        Set<String> columns = new LinkedHashSet<String>();
        for (String s : pmmlOutSpec.getLearningFields()) {
            columns.add(s);
        }
        outPMMLPort.addModelTranslater(new PMMLClusterTranslator(ComparisonMeasure.squaredEuclidean,
            modelSettings.m_nrOfClusters, clusters, clusterCoverage, columns));
        m_viewData = new KMeansViewData(clusters, clusterCoverage, m_dimension - m_nrIgnoredColumns, featureNames);

        DataContainer clusterCenterContainer = exec.createDataContainer(createClusterCentersSpec(spec));
        int i = 0;
        for (double[] cluster : clusters) {
            List<DataCell> cells = new ArrayList<>();
            for (double d : cluster) {
                cells.add(new DoubleCell(d));
            }
            clusterCenterContainer
                .addRowToTable(new DefaultRow(new RowKey(PMMLClusterTranslator.CLUSTER_NAME_PREFIX + i++), cells));
        }
        clusterCenterContainer.close();
        return new PortObject[]{outData, (BufferedDataTable)clusterCenterContainer.getTable(), outPMMLPort};
    }

    private boolean updateClusterCenters(final KMeansNodeParameters modelSettings, final int[] clusterCoverage,
        final double[][] clusters, final double[][] delta) {
        boolean finished = true;
        for (int c = 0; c < modelSettings.m_nrOfClusters; c++) {
            if (clusterCoverage[c] > 0) {
                // only update clusters who do cover some pattern:
                int pos = 0;
                for (int i = 0; i < m_dimension; i++) {
                    if (m_ignoreColumn[i]) {
                        continue;
                    }
                    // normalize delta by nr of covered patterns
                    double newValue = delta[c][pos] / clusterCoverage[c];
                    // compare before assigning the value to make sure we
                    // don't stop if things have changed substantially
                    if (Math.abs(clusters[c][pos] - newValue) > 1e-10) {
                        finished = false;
                    }
                    clusters[c][pos] = newValue;
                    pos++;
                }
            }
        }
        return finished;
    }

    private double[][] initializeClusters(final KMeansNodeParameters modelSettings, final BufferedDataTable input) {
        // initialize matrix of double (nr clusters * input dimension)
        double[][] clusters = new double[modelSettings.m_nrOfClusters][];
        for (int c = 0; c < modelSettings.m_nrOfClusters; c++) {
            clusters[c] = new double[m_dimension - m_nrIgnoredColumns];
        }
        //based on user selection to use first rows or random initialization, it returns the initial centroids.
        if (isFirstRowsInitialized(modelSettings)) {
            return firstRowsClusterInitialization(modelSettings, input, clusters);
        } else {
            return randomClusterInitialization(modelSettings, input, clusters);
        }
    }

    private static boolean isFirstRowsInitialized(final KMeansNodeParameters modelSettings) {
        return modelSettings.m_centroidInitialization == KMeansNodeParameters.CentroidInitialization.FIRST_ROWS;
    }

    private double[][] firstRowsClusterInitialization(final KMeansNodeParameters modelSettings, final DataTable input,
        final double[][] clusters) {
        // initialize cluster centers with values of first rows in table
        int c = 0;
        final RowIterator rowIt = input.iterator();
        while (rowIt.hasNext() && c < modelSettings.m_nrOfClusters) {
            DataRow currentRow = rowIt.next();
            assignCluster(currentRow, clusters, c);
            c++;
        }
        return clusters;
    }

    private double[][] randomClusterInitialization(final KMeansNodeParameters modelSettings,
        final BufferedDataTable input, final double[][] clusters) {
        //initialize random centroids
        final long nrOfRows = input.size();
        final Set<Long> randomInitialization = randomCentroidsSetCreation(modelSettings, nrOfRows);
        try (final CloseableRowIterator rowIt = input.iterator()) {
            int c = 0;
            for (long rowTrack = 0; rowIt.hasNext(); rowTrack++) {
                DataRow currentRow = rowIt.next();
                if (randomInitialization.contains(rowTrack)) {
                    assignCluster(currentRow, clusters, c);
                    c++;
                }
            }
            return clusters;
        }
    }

    private Set<Long> randomCentroidsSetCreation(final KMeansNodeParameters modelSettings, final long nrOfRows) {
        final Set<Long> randomInitialization = new HashSet<>();
        final RandomDataGenerator rdg = new RandomDataGenerator();
        rdg.reSeed(getSeedOrRandom(modelSettings));
        while (randomInitialization.size() < modelSettings.m_nrOfClusters) {
            randomInitialization.add(rdg.nextLong(0L, nrOfRows - 1));
        }
        return randomInitialization;
    }

    private void assignCluster(final DataRow currentRow, final double[][] clusters, final int c) {
        //stores a centroid in clusters and handles missing values.
        int pos = 0;
        for (int i = 0; i < currentRow.getNumCells(); i++) {
            if (!m_ignoreColumn[i]) {
                if (currentRow.getCell(i).isMissing()) {
                    clusters[c][pos] = 0;
                } else {
                    assert currentRow.getCell(i).getType().isCompatible(DoubleValue.class);
                    DoubleValue currentValue = (DoubleValue)currentRow.getCell(i);
                    clusters[c][pos] = currentValue.getDoubleValue();
                }
                pos++;
            }
        }
    }

    private int findClosestPrototypeFor(final KMeansNodeParameters modelSettings, final DataRow row,
        final double[][] clusters) {
        // find closest cluster center
        int winner = -1; // closest cluster so far
        double winnerDistance = Double.MAX_VALUE; // best distance
        for (int c = 0; c < modelSettings.m_nrOfClusters; c++) {
            double distance = 0.0;
            int pos = 0;
            for (int i = 0; i < m_dimension; i++) {
                DataCell currentCell = row.getCell(i);
                if (!m_ignoreColumn[i]) {
                    if (!currentCell.isMissing()) {
                        assert currentCell.getType().isCompatible(DoubleValue.class);
                        double d = (clusters[c][pos] - ((DoubleValue)(currentCell)).getDoubleValue());
                        if (!Double.isNaN(d)) {
                            distance += d * d;
                        }
                    } else {
                        distance += 0.0; // missing
                    }
                    pos++;
                }
            }
            if (distance < winnerDistance) { // found closer cluster
                winner = c; // make it new winner
                winnerDistance = distance;
            }
        } // for all clusters (find closest one)
        return winner;
    }

    /**
     * Clears the model.
     *
     * @see NodeModel#reset()
     */
    @Override
    protected void reset() {
        // remove the clusters
        m_viewData = null;
        if (m_translator != null) {
            m_translator.setMapper(null);
        }
    }

    /**
     * Returns <code>true</code> always and passes the current input spec to the output spec which is identical to the
     * input specification - after all, we are building cluster centers in the original feature space.
     *
     * @param inSpecs the specifications of the input port(s) - should be one
     * @return the copied input spec
     * @throws InvalidSettingsException if PMML incompatible type was found
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final KMeansNodeParameters modelSettings)
        throws InvalidSettingsException {
        DataTableSpec spec = (DataTableSpec)inSpecs[0];
        // input is output spec with all double compatible values set to
        // Double.
        m_dimension = spec.getNumColumns();
        // Find out which columns we can use (must be Double compatible)
        // Note that, for simplicity, we still use the entire dimensionality
        // for cluster prototypes below and simply ignore useless columns.
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;

        final var doubleColumns = ColumnSelectionUtil.getDoubleColumns(spec);
        final var includedColumnNames = Arrays.asList(modelSettings.m_columnFilter.filter(doubleColumns));

        // check if some columns are included
        if (includedColumnNames.size() <= 0) {
            setWarningMessage("No column in include list! Produces one huge cluster");
        }

        addExcludeColumnsToIgnoreList(modelSettings, spec);
        DataTableSpec appendedSpec = createAppendedSpec(modelSettings, spec);
        // return spec for data and model outport!
        PMMLPortObjectSpec pmmlSpec = new PMMLPortObjectSpecCreator(spec).createSpec();
        return new PortObjectSpec[]{appendedSpec, createClusterCentersSpec(spec),
            createPMMLSpec(modelSettings, pmmlSpec, spec)};
    }

    private DataTableSpec createClusterCentersSpec(final DataTableSpec spec) {
        // Create spec for cluster center table
        DataTableSpecCreator clusterCenterSpecCreator = new DataTableSpecCreator();
        for (int i = 0; i < m_dimension; i++) {
            if (!m_ignoreColumn[i]) {
                clusterCenterSpecCreator.addColumns(
                    new DataColumnSpecCreator(spec.getColumnSpec(i).getName(), DoubleCell.TYPE).createSpec());
            }
        }
        clusterCenterSpecCreator.dropAllDomains();
        return clusterCenterSpecCreator.createSpec();
    }

    private static DataTableSpec createAppendedSpec(final KMeansNodeParameters modelSettings,
        final DataTableSpec originalSpec) {
        // determine the possible values of the appended column
        DataCell[] possibleValues = new DataCell[modelSettings.m_nrOfClusters];
        for (int i = 0; i < modelSettings.m_nrOfClusters; i++) {
            DataCell key = new StringCell(CLUSTER + i);
            possibleValues[i] = key;
        }
        // create the domain
        final var colNameGuess = new UniqueNameGenerator(originalSpec).newName("Cluster");
        DataColumnDomainCreator domainCreator = new DataColumnDomainCreator(possibleValues);
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colNameGuess, StringCell.TYPE);
        creator.setDomain(domainCreator.createDomain());
        // create the appended column spec
        DataColumnSpec labelColSpec = creator.createSpec();
        return new DataTableSpec(originalSpec, new DataTableSpec(labelColSpec));
    }

    private void addExcludeColumnsToIgnoreList(final KMeansNodeParameters modelSettings,
        final DataTableSpec originalSpec) {
        // add all excluded columns to the ignore list
        var doubleColumns = ColumnSelectionUtil.getDoubleColumns(originalSpec);
        var includedColumnNames = Arrays.asList(modelSettings.m_columnFilter.filter(doubleColumns));
        final var includeSet = new HashSet<>(includedColumnNames);
        m_ignoreColumn = new boolean[m_dimension];
        m_nrIgnoredColumns = 0;
        for (int i = 0; i < m_dimension; i++) {
            String colName = originalSpec.getColumnSpec(i).getName();
            final var ignore = !includeSet.contains(colName);
            m_ignoreColumn[i] = ignore;
            if (ignore) {
                m_nrIgnoredColumns++;
            }
        }
    }

    private static PMMLPortObjectSpec createPMMLSpec(final KMeansNodeParameters modelSettings,
        final PMMLPortObjectSpec pmmlSpec, final DataTableSpec originalSpec) throws InvalidSettingsException {
        final var doubleColumns = ColumnSelectionUtil.getDoubleColumns(originalSpec);
        final var includes = Arrays.asList(modelSettings.m_columnFilter.filter(doubleColumns));
        PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(pmmlSpec, originalSpec);
        creator.setLearningColsNames(includes);
        return creator.createSpec();
    }

    KMeansViewData getViewData() {
        return m_viewData;
    }

    /**
     * @param modelSettings model settings
     * @return if the boolean is active, the stored seed is returned otherwise a random long value.
     */
    public static long getSeedOrRandom(final KMeansNodeParameters modelSettings) {
        if (modelSettings.m_useStaticRandomSeed) {
            return Long.parseLong(modelSettings.m_seedValue);
        }
        final long l1 = Double.doubleToLongBits(Math.random());
        final long l2 = Double.doubleToLongBits(Math.random());
        return ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
    }
}
