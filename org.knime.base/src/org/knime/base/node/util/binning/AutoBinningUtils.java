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
 *
 * History
 *   12.07.2010 (hofer): created
 */
package org.knime.base.node.util.binning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretizeTranslator;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretizeTranslator.Configuration.Interval;
import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.base.node.util.binning.AutoBinningSettings.ColumnOutputNamingSettings.AppendSuffix;
import org.knime.base.node.util.binning.AutoBinningSettings.DataBoundsSettings.BoundSetting.FixedBound;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

import com.google.common.math.Quantiles;

/**
 * Given various settings, creates bins. TODO more stuff
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 *
 * @since 5.5
 */
public class AutoBinningUtils {

    /**
     * This class provides the functionality to automatically bin numerical columns in a data table.
     */
    public static class AutoBinner {

        private final AutoBinningSettings m_settings;

        /**
         * @param settings The settings object.
         * @param spec the data table spec
         * @throws InvalidSettingsException when settings are not consistent
         */
        public AutoBinner(final AutoBinningSettings settings, final DataTableSpec spec)
            throws InvalidSettingsException {
            m_settings = settings;
        }

        /**
         * @return the settings
         */
        protected AutoBinningSettings getSettings() {
            return m_settings;
        }

        public static BufferedDataTable extractAndSortSingleColumn(final BufferedDataTable data,
            final ExecutionContext exec, final int columnIndex) throws CanceledExecutionException {

            if (columnIndex < 0 || columnIndex >= data.getDataTableSpec().getNumColumns()) {
                throw new IllegalArgumentException("Column index " + columnIndex + " is out of bounds.");
            }

            return extractAndSortSingleColumn(data, exec, data.getSpec().getColumnNames()[columnIndex]);
        }

        public static BufferedDataTable extractAndSortSingleColumn(final BufferedDataTable data,
            final ExecutionContext exec, final String columnName) throws CanceledExecutionException {

            var rearranger = new ColumnRearranger(data.getDataTableSpec());
            rearranger.keepOnly(columnName);

            var unsortedSingleColumnTable = exec.createColumnRearrangeTable(data, rearranger, exec);

            var sortedTable = new SortedTable(unsortedSingleColumnTable, Collections.singletonList(columnName),
                new boolean[]{true}, exec).getBufferedDataTable();

            return sortedTable;
        }

        public PortObject[] createNodeOutput(final BufferedDataTable data, final ExecutionContext exec)
            throws Exception {
            var spec = data.getDataTableSpec();

            Map<String, List<BinBoundary>> edgesMap = new HashMap<String, List<BinBoundary>>();
            if (m_settings.binning() instanceof AutoBinningSettings.BinningSettings.FixedWidth fw) {
                var inData = calcDomainBoundsIfNeccessary( //
                    data, //
                    exec.createSubExecutionContext(0.9), //
                    m_settings.columnNames() //
                );

                for (var target : m_settings.columnNames()) {
                    DataTableSpec inSpec = inData.getDataTableSpec();
                    DataColumnSpec targetCol = inSpec.getColumnSpec(target);

                    double min = m_settings.boundsSettings().getLowerBound()
                        .orElse(((DoubleValue)targetCol.getDomain().getLowerBound()).getDoubleValue());

                    double max = m_settings.boundsSettings().getUpperBound()
                        .orElse(((DoubleValue)targetCol.getDomain().getUpperBound()).getDoubleValue());

                    double[] edges = m_settings.integerBounds() //
                        ? toIntegerBoundaries(calculateBounds(fw.numBins(), min, max)) //
                        : calculateBounds(fw.numBins(), min, max);

                    var binBoundaries = IntStream.range(0, edges.length) //
                        .mapToObj(j -> new BinBoundary(edges[j], //
                            j == edges.length - 1 //
                                ? BinBoundaryExactMatchBehaviour.TO_LOWER_BIN //
                                : BinBoundaryExactMatchBehaviour.TO_UPPER_BIN)) //
                        .toList();

                    edgesMap.put(target, binBoundaries);
                }
            }

            var translator = createDiscretizeTranslator(edgesMap);
            var table = exec.createColumnRearrangeTable(data, createRearranger(translator, spec), exec);

            var outputPmmlSpec = (PMMLPortObjectSpec)createOutputSpec(spec)[1];
            var outputPmml = new PMMLPortObject(outputPmmlSpec);
            outputPmml.addGlobalTransformations(translator.exportToTransDict());

            return new PortObject[]{ //
                table, //
                outputPmml, //
            };
        }

        private double[] findEdgesForEqualCount(final List<Double> values, final int binCount) {
            Collections.sort(values);
            int countPerBin = (int)(Math.round(values.size() / (double)binCount));
            double[] edges = new double[binCount + 1];
            edges[0] = m_settings.boundsSettings().getLowerBound().orElse(m_settings.integerBounds() //
                ? Math.floor(values.get(0)) //
                : values.get(0));
            edges[edges.length - 1] =
                m_settings.boundsSettings().getUpperBound().orElse(optionalCeil(values.get(values.size() - 1)));
            int startIndex = 0;
            int index = countPerBin - 1;
            for (int i = 1; i < edges.length - 1; i++) {
                if (index < values.size()) {
                    double edge = optionalCeil(values.get(index));
                    // get lower index
                    int lowerIndex = index;
                    while (lowerIndex >= startIndex && !(edge > optionalCeil(values.get(lowerIndex)))) {
                        lowerIndex--;
                    }
                    // get higher index
                    int higherIndex = index;
                    while (higherIndex < values.size() - 1 && !(optionalCeil(values.get(higherIndex + 1)) > edge)) {
                        higherIndex++;
                    }
                    int lowerDiff = -1 * (lowerIndex - startIndex + 1 - countPerBin);
                    int higherDiff = higherIndex - startIndex + 1 - countPerBin;
                    if (!(lowerIndex < startIndex) && lowerDiff <= higherDiff) {
                        index = lowerIndex;
                    } else {
                        index = higherIndex;
                    }
                    edges[i] = optionalCeil(values.get(index));
                    startIndex = index + 1;
                    index += countPerBin;
                } else {
                    edges[i] = edges[i - 1];
                }
            }
            return edges;
        }

        private double optionalCeil(final double value) {
            return m_settings.integerBounds() ? Math.ceil(value) : value;
        }

        protected PMMLPreprocDiscretizeTranslator
            createDiscretizeTranslator(final Map<String, List<BinBoundary>> edgesMap) {
            var config = new PMMLPreprocDiscretizeTranslator.Configuration(createBins(edgesMap));
            return new PMMLPreprocDiscretizeTranslator(config);
        }

        private Map<String, List<PMMLPreprocDiscretizeTranslator.Configuration.Bin>>
            createBins(final Map<String, List<BinBoundary>> edgesMap) {

            var discretizationsByColumnName =
                new HashMap<String, List<PMMLPreprocDiscretizeTranslator.Configuration.Bin>>();

            for (var targetColumn : edgesMap.keySet()) {
                var edges = edgesMap.get(targetColumn);

                var bins = new ArrayList<PMMLPreprocDiscretizeTranslator.Configuration.Bin>();

                for (int i = 0; i < edges.size() - 1; ++i) {
                    var leftEdge = edges.get(i);
                    var rightEdge = edges.get(i + 1);

                    var closure = PMMLPreprocDiscretizeTranslator.Configuration.ClosureStyle.from( //
                        leftEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN || i == 0, //
                        rightEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN
                            || i == edges.size() - 1 //
                    );
                    var binInterval = new PMMLPreprocDiscretizeTranslator.Configuration.Interval( //
                        leftEdge.value(), rightEdge.value(), closure //
                    );

                    var binName = m_settings.binNaming().computedName(i, leftEdge, rightEdge);

                    bins.add(new PMMLPreprocDiscretizeTranslator.Configuration.Bin( //
                        binName, //
                        Collections.singletonList(binInterval) //
                    ));

                    discretizationsByColumnName.put(targetColumn, bins);
                }
            }

            return discretizationsByColumnName;
        }

        //        private Map<String, List<PMMLDiscretizeBin>> createBinsLegacy(final Map<String, double[]> edgesMap) {
        //            var formatter = new BinnerNumberFormat(m_settings);
        //            var binMap = new HashMap<String, List<PMMLDiscretizeBin>>();
        //            for (String target : m_included) {
        //                if (null != edgesMap && null != edgesMap.get(target) && edgesMap.get(target).length > 1) {
        //                    double[] edges = edgesMap.get(target);
        //                    // Names of the bins
        //                    var binNames = new String[edges.length - 1];
        //
        //                    if (m_settings.getBinNaming() == BinNaming.NUMBERED) {
        //                        for (int i = 0; i < binNames.length; i++) {
        //                            binNames[i] = "Bin " + (i + 1);
        //                        }
        //                    } else if (m_settings.getBinNaming() == BinNaming.EDGES) {
        //                        binNames[0] = "[" + formatter.format(edges[0]) + "," + formatter.format(edges[1]) + "]";
        //                        for (int i = 1; i < binNames.length; i++) {
        //                            binNames[i] = "(" + formatter.format(edges[i]) + "," + formatter.format(edges[i + 1]) + "]";
        //                        }
        //                    } else { // BinNaming.midpoints
        //                        binNames[0] = formatter.format((edges[1] - edges[0]) / 2 + edges[0]);
        //                        for (int i = 1; i < binNames.length; i++) {
        //                            binNames[i] = formatter.format((edges[i + 1] - edges[i]) / 2 + edges[i]);
        //                        }
        //                    }
        //                    List<PMMLDiscretizeBin> bins = new ArrayList<PMMLDiscretizeBin>();
        //                    bins.add(new PMMLDiscretizeBin(binNames[0],
        //                        Arrays.asList(new PMMLInterval(edges[0], edges[1], Closure.closedClosed))));
        //                    for (int i = 1; i < binNames.length; i++) {
        //                        bins.add(new PMMLDiscretizeBin(binNames[i],
        //                            Arrays.asList(new PMMLInterval(edges[i], edges[i + 1], Closure.openClosed))));
        //                    }
        //                    binMap.put(target, bins);
        //                } else {
        //                    binMap.put(target, new ArrayList<PMMLDiscretizeBin>());
        //                }
        //            }
        //            return binMap;
        //        }

        public PortObjectSpec[] createOutputSpec(final DataTableSpec inSpec) throws InvalidSettingsException {
            var translator = createDiscretizeTranslator(Map.of());
            var tableOutSpec = createRearranger(translator, inSpec).createSpec();

            var specCreator = new PMMLPortObjectSpecCreator(inSpec);
            specCreator.setPreprocColNames(new ArrayList<>(translator.getConfig().getTargetColumnNames()));
            var spec = specCreator.createSpec();

            return new PortObjectSpec[]{tableOutSpec, spec};
        }

        public ColumnRearranger createRearranger(final PMMLPreprocDiscretizeTranslator translator,
            final DataTableSpec dataSpec) throws InvalidSettingsException {
            // Check if columns to discretize exist
            for (String name : translator.getConfig().getTargetColumnNames()) {
                if (!dataSpec.containsName(name)) {
                    throw new InvalidSettingsException(
                        "Column " + "\"" + name + "\"" + "is missing in the input table.");
                }
            }
            ColumnRearranger rearranger = new ColumnRearranger(dataSpec);
            for (String inputColumnName : translator.getConfig().getTargetColumnNames()) {
                int colIdx = dataSpec.findColumnIndex(inputColumnName);

                var binNameForBelow = m_settings.boundsSettings().lowerBound() instanceof FixedBound fb //
                    ? fb.binNameForValuesOutsideBound() //
                    : null;

                var binNameForAbove = m_settings.boundsSettings().upperBound() instanceof FixedBound fb //
                    ? fb.binNameForValuesOutsideBound() //
                    : null;

                var outputColumnName = m_settings.columnOutputNaming() instanceof AppendSuffix as //
                    ? inputColumnName + as.suffix() //
                    : inputColumnName;

                var binningFactory = new BinningCellFactory( //
                    outputColumnName, //
                    colIdx, //
                    translator.getConfig().getDiscretizations().get(inputColumnName), //
                    binNameForBelow, //
                    binNameForAbove //
                );

                if (m_settings.columnOutputNaming() instanceof AppendSuffix) {
                    rearranger.append(binningFactory);
                } else {
                    rearranger.replace(binningFactory, inputColumnName);
                }
            }

            return rearranger;
        }
    }

    /**
     * Calculates the bin boundaries for a histogram with the given number of equally-spaced bins, minimum and maximum
     * values.
     *
     * @param binCount number of bins
     * @param min minimum value
     * @param max maximum value
     * @return the boundaries
     */
    public static double[] calculateBounds(final int binCount, final double min, final double max) {
        double[] edges = new double[binCount + 1];
        edges[0] = min;
        edges[edges.length - 1] = max;
        for (int i = 1; i < edges.length - 1; i++) {
            edges[i] = min + i / (double)binCount * (max - min);
        }
        return edges;
    }

    /**
     * Converts double boundaries to integer boundaries (the data type is still double, but they are now doubles
     * representing integers). The resulting array may be shorter than the input if some boundaries map to the same
     * integer value.
     *
     * @param boundaries a sorted array of boundaries
     * @return the new boundaries, all integer values
     * @since 3.1
     */
    public static double[] toIntegerBoundaries(final double[] boundaries) {
        Set<Double> intBoundaries = new TreeSet<Double>();
        intBoundaries.add(Math.floor(boundaries[0]));
        for (int i = 1; i < boundaries.length; i++) {
            intBoundaries.add(Math.ceil(boundaries[i]));
        }
        double[] newEdges = new double[intBoundaries.size()];
        int i = 0;
        for (Double edge : intBoundaries) {
            newEdges[i++] = edge;
        }
        return newEdges;
    }

    /**
     * This method is based on the 7th quantile algorithm in R, equivalent to the scipy version with (a,b)=(1,1). See <a
     * href="https://en.wikipedia.org/wiki/Quantile#Estimating_quantiles_from_a_sample"}>WP:Quantile</a>, which also
     * defines all the symbols used in this function. We use the same nomenclature here.
     *
     * <p>
     * Note: it is assumed that the input data has only a single column which is sorted ascending.
     * </p>
     *
     * @param dataColumn
     * @param exec
     * @param sampleBoundaries
     * @return
     * @throws CanceledExecutionException
     */
    private static List<BinBoundary> createEdgesFromQuantiles(final BufferedDataTable dataColumn,
        final ExecutionContext exec, final List<BinBoundary> sampleBoundaries) throws CanceledExecutionException {

        double[] dataValues = new double[Math.toIntExact(dataColumn.size())];
        try (var iter = dataColumn.iterator()) {
            for (int i = 0; iter.hasNext(); i++) {
                var cell = iter.next().getCell(0);
                if (cell.isMissing()) {
                    throw new RuntimeException(
                        "Missing values not supported for quantile calculation (error in row \"" + i + "\")");
                }
                dataValues[i] = ((DoubleValue)cell).getDoubleValue();
            }
        }

        // TODO: this is great, but using this method limits the table size. Implement your own
        var quantiles = Quantiles.scale(sampleBoundaries.size()) //
            .indexes(IntStream.range(0, sampleBoundaries.size()).toArray()) //
            .compute(dataValues);

        return IntStream.range(0, quantiles.size()) //
            .mapToObj(i -> new BinBoundary(quantiles.get(i), sampleBoundaries.get(i).exactMatchBehaviour())) //
            .toList();

        //        long N = dataColumn.size();
        //        long c = 0;
        //        int cc = 0;
        //        try (var iter = dataColumn.iterator()) {
        //            DataRow rowQ = null;
        //            DataRow rowQ1 = null;
        //            if (iter.hasNext()) {
        //                rowQ1 = iter.next();
        //                rowQ = rowQ1;
        //            }
        //
        //            for (var p : sampleBoundaries) {
        //                double h = (N - 1) * p.value() + 1;
        //                int h_floor = (int)Math.floor(h);
        //
        //                while ((1.0 == p.value() || c < h_floor) && iter.hasNext()) {
        //                    rowQ = rowQ1;
        //                    rowQ1 = iter.next();
        //                    c++;
        //                    exec.setProgress(c / (double)N);
        //                    exec.checkCanceled();
        //                }
        //                rowQ = 1.0 != p.value() ? rowQ : rowQ1;
        //                final DataCell xqCell = rowQ.getCell(0);
        //                final DataCell xq1Cell = rowQ1.getCell(0);
        //                // TODO should be able to handle missing values (need to filter
        //                // data first?)
        //                if (xqCell.isMissing() || xq1Cell.isMissing()) {
        //                    throw new RuntimeException("Missing values not support for "
        //                        + "quantile calculation (error in row \"" + rowQ1.getKey() + "\")");
        //                }
        //                // for quantile calculation see also
        //                // http://en.wikipedia.org/wiki/
        //                //                Quantile#Estimating_the_quantiles_of_a_population.
        //                // this implements R-7
        //                double xq = ((DoubleValue)xqCell).getDoubleValue();
        //                double xq1 = ((DoubleValue)xq1Cell).getDoubleValue();
        //                double quantile = xq + (h - h_floor) * (xq1 - xq);
        //                edges[cc] = quantile;
        //                cc++;
        //            }
        //            return edges;
        //        }
    }

    /**
     * Calculates the domain bounds for the specified columns in the data table. It will try to extract them directly
     * from the domain of the table if it has one, otherwise it will calculate the bounds by iterating through the rows
     * of the table.
     *
     * I have lifted this method with very few changes from its original location in {@link AutoBinningUtils}, but I
     * have fixed one bug: when given a data table with some columns that have a domain and some that do not, the ones
     * with a domain will end up with a domain of [-infinity, +infinity], which is not correct.
     *
     * @param data the data table to calculate the bounds for
     * @param exec the execution context to report progress and handle cancellation
     * @param colNamesToRecalcValuesForIfNecessary the list of column names for which the domain bounds should be
     *            recalculated.
     * @return a new data table with updated domain bounds for the specified columns. If no recalculation is needed,
     *         returns the original data table. Otherwise it will be a spec replaced table with the updated domains.
     * @throws InvalidSettingsException if any of the specified columns is not numeric
     * @throws CanceledExecutionException if the execution is canceled during the calculation
     */
    public static BufferedDataTable calcDomainBoundsIfNeccessary( //
        final BufferedDataTable data, //
        final ExecutionContext exec, //
        final List<String> colNamesToRecalcValuesForIfNecessary //
    ) throws InvalidSettingsException, CanceledExecutionException {

        // If we have no columns to recalculate, just return the original data.
        if (null == colNamesToRecalcValuesForIfNecessary || colNamesToRecalcValuesForIfNecessary.isEmpty()) {
            return data;
        }

        // Find all numeric columns that are missing a domain, and extract their indices.
        // We filter out any columns that have a domain already, since we don't need to
        // recalculate those.
        var columnIndicesThatMustBeRecalculated = new ArrayList<Integer>();
        for (String colName : colNamesToRecalcValuesForIfNecessary) {
            DataColumnSpec colSpec = data.getDataTableSpec().getColumnSpec(colName);
            if (!colSpec.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException(
                    "Can only process numeric " + "data. The column \"" + colSpec.getName() + "\" is not numeric.");
            }
            if (!colSpec.getDomain().hasBounds()) {
                columnIndicesThatMustBeRecalculated.add(data.getDataTableSpec().findColumnIndex(colName));
            }
        }
        if (columnIndicesThatMustBeRecalculated.isEmpty()) {
            return data;
        }

        // Let's compute the new min/max values for the columns that need recalculation. Note that we are
        // not including any columns that already have a domain with bounds.
        var minValuesByColIdx = new HashMap<Integer, Double>();
        var maxValuesByColIdx = new HashMap<Integer, Double>();
        for (int colIdx : columnIndicesThatMustBeRecalculated) {
            minValuesByColIdx.put(colIdx, Double.MAX_VALUE);
            maxValuesByColIdx.put(colIdx, Double.MIN_VALUE);
        }
        int rowIdx = 0;
        for (DataRow row : data) {
            rowIdx++;
            exec.checkCanceled();
            exec.setProgress(rowIdx / (double)data.size());
            for (int col : columnIndicesThatMustBeRecalculated) {
                double val = ((DoubleValue)row.getCell(col)).getDoubleValue();
                if (minValuesByColIdx.get(col) > val) {
                    minValuesByColIdx.put(col, val);
                }
                if (maxValuesByColIdx.get(col) < val) {
                    minValuesByColIdx.put(col, val);
                }
            }
        }

        // Having got a new set of min/max values, we can now create a new column spec for
        // each column that needs recalculation, then create a new data table spec out of those
        // and return a spec replaced table with the new column specs.
        var newColSpecList = new ArrayList<DataColumnSpec>();
        int colIdx = 0;
        for (var columnSpec : data.getDataTableSpec()) {
            if (columnIndicesThatMustBeRecalculated.contains(colIdx)) {
                // if we are here, this means that we have recalculated the min/max values for this column,
                // so we need to create a new column spec with the new domain bounds.
                var specCreator = new DataColumnSpecCreator(columnSpec);
                var newDomain = new DataColumnDomainCreator( //
                    new DoubleCell(minValuesByColIdx.get(colIdx)), //
                    new DoubleCell(maxValuesByColIdx.get(colIdx)) //
                ).createDomain();
                specCreator.setDomain(newDomain);
                newColSpecList.add(specCreator.createSpec());
            } else {
                newColSpecList.add(columnSpec);
            }
            colIdx++;
        }
        var spec = new DataTableSpec(newColSpecList.toArray(new DataColumnSpec[0]));
        return exec.createSpecReplacerTable(data, spec);
    }

    public record BinBoundary( //
        double value, //
        BinBoundaryExactMatchBehaviour exactMatchBehaviour //
    ) {
    }

    public static class BinningCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final List<PMMLPreprocDiscretizeTranslator.Configuration.Bin> m_bins;

        private final String m_binNameForBelow;

        private final String m_binNameForAbove;

        public BinningCellFactory( //
            final String newColName, //
            final int targetColumnIndex, //
            final List<PMMLPreprocDiscretizeTranslator.Configuration.Bin> bins, //
            final String binNameForBelow, //
            final String binNameForAbove //
        ) {
            super(new DataColumnSpecCreator(newColName, StringCell.TYPE).createSpec());

            m_targetColumnIndex = targetColumnIndex;
            m_bins = bins;

            m_binNameForBelow = binNameForBelow;
            m_binNameForAbove = binNameForAbove;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var value = ((DoubleValue)row.getCell(m_targetColumnIndex)).getDoubleValue();

            var matchingBins = m_bins.stream() //
                .filter(bin -> bin.covers(value)) //
                .toList();

            if (matchingBins.isEmpty()) {
                // find min left boundary of all bins, and max right boundary of all bins. Value should be outside
                // one or the other.

                var minLeft = m_bins.stream() //
                    .mapToDouble(b -> b.intervals().stream().mapToDouble(Interval::leftMargin).min().orElseThrow()) //
                    .min() //
                    .orElseThrow();

                var maxRight = m_bins.stream() //
                    .mapToDouble(b -> b.intervals().stream().mapToDouble(Interval::rightMargin).max().orElseThrow()) //
                    .max() //
                    .orElseThrow();

                if (value < minLeft) {
                    return StringCellFactory.create(m_binNameForBelow);
                } else if (value > maxRight) {
                    return StringCellFactory.create(m_binNameForAbove);
                } else {
                    throw new IllegalStateException("No bin found for value " + value + " in column "
                        + m_targetColumnIndex + ". " + "This is an implementation bug.");
                }

            } else if (matchingBins.size() > 1) {
                throw new IllegalStateException("Multiple bins found for value " + value + " in column "
                    + m_targetColumnIndex + ". " + "This is an implementation bug.");
            } else {
                var bin = matchingBins.get(0);
                return StringCellFactory.create(bin.binValue());
            }
        }
    }

    public static boolean columnCanBeBinned(final DataColumnSpec colSpec) {
        return colSpec.getType().isCompatible(DoubleValue.class);
    }
}
