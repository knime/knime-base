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

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.autobinner.pmml.DisretizeConfiguration;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretize;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizeBin;
import org.knime.base.node.preproc.autobinner.pmml.PMMLDiscretizePreprocPortObjectSpec;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval;
import org.knime.base.node.preproc.autobinner.pmml.PMMLInterval.Closure;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretize;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocPortObjectSpec;
import org.knime.core.util.binning.auto.BinNaming;
import org.knime.core.util.binning.auto.BinningMethod;
import org.knime.core.util.binning.auto.EqualityMethod;

/**
 * Given various settings, creates bins in the format of a {@link PMMLPreprocDiscretize}..
 *
 * Moved in v5.5 from a single node-specific package to this utils package.
 *
 * @author Heiko Hofer
 *
 * @since 5.5
 */
public class AutoBinningUtils {

    private AutoBinningSettings m_settings;

    private PMMLPreprocPortObjectSpec m_pmmlOutSpec;

    private DataTableSpec m_tableOutSpec;

    private String[] m_included;

    /**
     * @param settings The settings object.
     * @param spec the data table spec
     * @throws InvalidSettingsException when settings are not consistent
     */
    public AutoBinningUtils(final AutoBinningSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        m_settings = settings;
        m_included = settings.getFilterConfiguration().applyTo(spec).getIncludes();
    }

    /**
     * @return the settings
     */
    protected AutoBinningSettings getSettings() {
        return m_settings;
    }

    /**
     * Determine bins.
     *
     * @param data the input data
     * @param exec the execution context
     * @return the operation with the discretisation information
     * @throws Exception ...
     */
    public PMMLPreprocDiscretize execute(final BufferedDataTable data, final ExecutionContext exec) throws Exception {
        final DataTableSpec spec = data.getDataTableSpec();
        // determine intervals
        if (m_settings.getMethod() == BinningMethod.FIXED_NUMBER) {
            if (m_settings.getEqualityMethod() == EqualityMethod.WIDTH) {
                BufferedDataTable inData =
                    calcDomainBoundsIfNeccessary(data, exec.createSubExecutionContext(0.9), Arrays.asList(m_included));
                init(inData.getDataTableSpec());
                Map<String, double[]> edgesMap = new HashMap<String, double[]>();
                for (String target : m_included) {
                    DataTableSpec inSpec = inData.getDataTableSpec();
                    DataColumnSpec targetCol = inSpec.getColumnSpec(target);

                    // bounds of the domain
                    double min = m_settings.getFixedLowerBound()
                        .orElse(((DoubleValue)targetCol.getDomain().getLowerBound()).getDoubleValue());
                    double max = m_settings.getFixedUpperBound()
                        .orElse(((DoubleValue)targetCol.getDomain().getUpperBound()).getDoubleValue());

                    double[] edges = calculateBounds(m_settings.getBinCount(), min, max);

                    if (m_settings.getIntegerBounds()) {
                        edges = toIntegerBoundaries(edges);
                    }

                    edgesMap.put(target, edges);
                }
                return createDisretizeOp(edgesMap);
            } else { // EqualityMethod.equalCount
                Map<String, double[]> edgesMap = new HashMap<String, double[]>();
                for (String target : m_included) {
                    int colIndex = data.getDataTableSpec().findColumnIndex(target);
                    List<Double> values = new ArrayList<Double>();
                    for (DataRow row : data) {
                        if (!row.getCell(colIndex).isMissing()) {
                            values.add(((DoubleValue)row.getCell(colIndex)).getDoubleValue());
                        }
                    }
                    edgesMap.put(target, findEdgesForEqualCount(values, m_settings.getBinCount()));
                }
                return createDisretizeOp(edgesMap);
            }
        } else if (m_settings.getMethod() == BinningMethod.SAMPLE_QUANTILES) {
            init(spec);
            Map<String, double[]> edgesMap = new LinkedHashMap<String, double[]>();
            final int colCount = m_included.length;
            // contains all numeric columns if include all is set!
            for (String target : m_included) {
                exec.setMessage("Calculating quantiles (column \"" + target + "\")");
                ExecutionContext colSortContext = exec.createSubExecutionContext(0.7 / colCount);
                ExecutionContext colCalcContext = exec.createSubExecutionContext(0.3 / colCount);
                ColumnRearranger singleRearranger = new ColumnRearranger(spec);
                singleRearranger.keepOnly(target);
                BufferedDataTable singleColSorted =
                    colSortContext.createColumnRearrangeTable(data, singleRearranger, colSortContext);
                SortedTable sorted = new SortedTable(singleColSorted, Collections.singletonList(target),
                    new boolean[]{true}, colSortContext);
                colSortContext.setProgress(1.0);
                double[] edges = createEdgesFromQuantiles(sorted.getBufferedDataTable(), colCalcContext,
                    m_settings.getSampleQuantiles());
                colCalcContext.setProgress(1.0);
                exec.clearTable(singleColSorted);
                if (m_settings.getIntegerBounds()) {
                    edges = toIntegerBoundaries(edges);
                }
                edgesMap.put(target, edges);
            }
            return createDisretizeOp(edgesMap);
        } else {
            throw new IllegalStateException("Unknown binning method.");
        }
    }

    /**
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
     * Converts double boundaries to integer boundaries. The resulting array may be shorter if some boundaries map to
     * the same integer value.
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

    private double[] findEdgesForEqualCount(final List<Double> values, final int binCount) {
        Collections.sort(values);
        int countPerBin = (int)(Math.round(values.size() / (double)binCount));
        double[] edges = new double[binCount + 1];
        edges[0] = m_settings.getFixedLowerBound()
            .orElse(m_settings.getIntegerBounds() ? Math.floor(values.get(0)) : values.get(0));
        edges[edges.length - 1] = m_settings.getFixedUpperBound().orElse(optionalCeil(values.get(values.size() - 1)));
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
        return m_settings.getIntegerBounds() ? Math.ceil(value) : value;
    }

    @SuppressWarnings("null")
    private static double[] createEdgesFromQuantiles(final BufferedDataTable data, final ExecutionContext exec,
        final double[] sampleQuantiles) throws CanceledExecutionException {
        double[] edges = new double[sampleQuantiles.length];
        long n = data.size();
        long c = 0;
        int cc = 0;
        try (var iter = data.iterator()) {
            DataRow rowQ = null;
            DataRow rowQ1 = null;
            if (iter.hasNext()) {
                rowQ1 = iter.next();
                rowQ = rowQ1;
            }

            for (double p : sampleQuantiles) {
                double h = (n - 1) * p + 1;
                int q = (int)Math.floor(h);
                while ((1.0 == p || c < q) && iter.hasNext()) {
                    rowQ = rowQ1;
                    rowQ1 = iter.next();
                    c++;
                    exec.setProgress(c / (double)n);
                    exec.checkCanceled();
                }
                rowQ = 1.0 != p ? rowQ : rowQ1;
                final DataCell xqCell = rowQ.getCell(0);
                final DataCell xq1Cell = rowQ1.getCell(0);
                // TODO should be able to handle missing values (need to filter
                // data first?)
                if (xqCell.isMissing() || xq1Cell.isMissing()) {
                    throw new RuntimeException("Missing values not support for "
                        + "quantile calculation (error in row \"" + rowQ1.getKey() + "\")");
                }
                // for quantile calculation see also
                // http://en.wikipedia.org/wiki/
                //                Quantile#Estimating_the_quantiles_of_a_population.
                // this implements R-7
                double xq = ((DoubleValue)xqCell).getDoubleValue();
                double xq1 = ((DoubleValue)xq1Cell).getDoubleValue();
                double quantile = xq + (h - q) * (xq1 - xq);
                edges[cc] = quantile;
                cc++;
            }
            return edges;
        }
    }

    /**
     * @param edgesMap the boundary map
     * @return the {@link PMMLPreprocDiscretize} model
     */
    protected PMMLPreprocDiscretize createDisretizeOp(final Map<String, double[]> edgesMap) {
        Map<String, List<PMMLDiscretizeBin>> binMap = createBins(edgesMap);

        List<String> names = new ArrayList<String>();
        Map<String, PMMLDiscretize> discretize = new HashMap<String, PMMLDiscretize>();
        for (String target : m_included) {
            String binnedCol = m_settings.getReplaceColumn() ? target : target + " [Binned]";
            names.add(binnedCol);
            discretize.put(binnedCol, new PMMLDiscretize(target, binMap.get(target)));
        }

        DisretizeConfiguration config = new DisretizeConfiguration(names, discretize);

        PMMLPreprocDiscretize op = new PMMLPreprocDiscretize(config);
        return op;
    }

    private Map<String, List<PMMLDiscretizeBin>> createBins(final Map<String, double[]> edgesMap) {
        BinnerNumberFormat formatter = new BinnerNumberFormat();
        Map<String, List<PMMLDiscretizeBin>> binMap = new HashMap<String, List<PMMLDiscretizeBin>>();
        for (String target : m_included) {
            if (null != edgesMap && null != edgesMap.get(target) && edgesMap.get(target).length > 1) {
                double[] edges = edgesMap.get(target);
                // Names of the bins
                String[] binNames = new String[edges.length - 1];
                if (m_settings.getBinNaming() == BinNaming.NUMBERED) {
                    for (int i = 0; i < binNames.length; i++) {
                        binNames[i] = "Bin " + (i + 1);
                    }
                } else if (m_settings.getBinNaming() == BinNaming.EDGES) {
                    binNames[0] = "[" + formatter.format(edges[0]) + "," + formatter.format(edges[1]) + "]";
                    for (int i = 1; i < binNames.length; i++) {
                        binNames[i] = "(" + formatter.format(edges[i]) + "," + formatter.format(edges[i + 1]) + "]";
                    }
                } else { // BinNaming.midpoints
                    binNames[0] = formatter.format((edges[1] - edges[0]) / 2 + edges[0]);
                    for (int i = 1; i < binNames.length; i++) {
                        binNames[i] = formatter.format((edges[i + 1] - edges[i]) / 2 + edges[i]);
                    }
                }
                List<PMMLDiscretizeBin> bins = new ArrayList<PMMLDiscretizeBin>();
                bins.add(new PMMLDiscretizeBin(binNames[0],
                    Arrays.asList(new PMMLInterval(edges[0], edges[1], Closure.closedClosed))));
                for (int i = 1; i < binNames.length; i++) {
                    bins.add(new PMMLDiscretizeBin(binNames[i],
                        Arrays.asList(new PMMLInterval(edges[i], edges[i + 1], Closure.openClosed))));
                }
                binMap.put(target, bins);
            } else {
                binMap.put(target, new ArrayList<PMMLDiscretizeBin>());
            }
        }
        return binMap;
    }

    /**
     * @param spec The <code>DataTableSpec</code> of the input table.
     * @return The spec of the output.
     * @throws InvalidSettingsException If settings and spec given in the constructor are invalid.
     */
    public PortObjectSpec[] getOutputSpec(final DataTableSpec spec) throws InvalidSettingsException {
        init(spec);
        return new PortObjectSpec[]{m_tableOutSpec, m_pmmlOutSpec};
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec) throws InvalidSettingsException {
        PMMLPreprocDiscretize op = createDisretizeOp(null);

        m_tableOutSpec = AutoBinnerApply.getOutputSpec(op, inSpec);
        m_pmmlOutSpec = new PMMLDiscretizePreprocPortObjectSpec(op);
    }

    /**
     * This formatted should not be changed, since it may result in a different output of the binning labels.
     */
    protected class BinnerNumberFormat {
        /** Constructor. */
        protected BinnerNumberFormat() {
            // no op
        }

        /** for numbers less than 0.0001. */
        private final DecimalFormat m_smallFormat = new DecimalFormat("0.00E0", new DecimalFormatSymbols(Locale.US));

        /** in all other cases, use the default Java formatter. */
        private final NumberFormat m_defaultFormat = NumberFormat.getNumberInstance(Locale.US);

        /**
         * Formats the double to a string. It will use the following either the format <code>0.00E0</code> for numbers
         * less than 0.0001 or the default NumberFormat.
         *
         * @param d the double to format
         * @return the string representation of <code>d</code>
         */
        public String format(final double d) {
            if (m_settings.getAdvancedFormatting()) {
                return advancedFormat(d);
            } else {
                if (d == 0.0) {
                    return "0";
                }
                if (Double.isInfinite(d) || Double.isNaN(d)) {
                    return Double.toString(d);
                }
                NumberFormat format;
                double abs = Math.abs(d);
                if (abs < 0.0001) {
                    format = m_smallFormat;
                } else {
                    format = m_defaultFormat;
                }
                synchronized (format) {
                    return format.format(d);
                }
            }
        }

        /**
         * @param d the double to format
         * @return the formated value
         */
        public String advancedFormat(final double d) {
            BigDecimal bd = new BigDecimal(d);
            switch (m_settings.getPrecisionMode()) {
                case DECIMAL:
                    bd = bd.setScale(m_settings.getPrecision(), m_settings.getRoundingMode());
                    break;
                case SIGNIFICANT:
                    bd = bd.round(new MathContext(m_settings.getPrecision(), m_settings.getRoundingMode()));
                    break;
            }
            switch (m_settings.getOutputFormat()) {
                case STANDARD:
                    return bd.toString();
                case PLAIN:
                    return bd.toPlainString();
                case ENGINEERING:
                    return bd.toEngineeringString();
                default:
                    return Double.toString(bd.doubleValue());
            }
        }

        /**
         * @return the smallFormat
         */
        public DecimalFormat getSmallFormat() {
            return m_smallFormat;
        }

        /**
         * @return the default format
         */
        public NumberFormat getDefaultFormat() {
            return m_defaultFormat;
        }
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
}
