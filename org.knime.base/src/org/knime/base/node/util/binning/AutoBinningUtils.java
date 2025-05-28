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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretizeTranslator;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretizeTranslator.Configuration;
import org.knime.base.node.preproc.autobinner.pmml.PMMLPreprocDiscretizeTranslator.Configuration.Interval;
import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundary;
import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.base.node.util.binning.AutoBinningSettings.BinningSettings;
import org.knime.base.node.util.binning.AutoBinningSettings.ColumnOutputNamingSettings.AppendSuffix;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * Given various settings, creates bins.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.5
 */
public class AutoBinningUtils {

    /**
     * This class provides the functionality to automatically bin numerical columns in a data table.
     */
    public static class AutoBinner {

        private final AutoBinningSettings m_settings;

        /**
         * Constructs an AutoBinner with the specified settings and data table specification.
         *
         * @param settings The settings object.
         * @param spec the data table spec
         * @throws InvalidSettingsException when settings are not consistent
         */
        public AutoBinner(final AutoBinningSettings settings, final DataTableSpec spec)
            throws InvalidSettingsException {
            m_settings = settings;
        }

        /**
         * Extracts a single column from the given data table, sorts it, and returns a new table with that single column
         * sorted in ascending order.
         *
         * @param data the data table to extract the column from
         * @param exec the execution context to report progress and handle cancellation
         * @param columnName the name of the column to extract and sort
         * @return a new data table containing only the specified column, sorted in ascending order
         * @throws CanceledExecutionException if the execution is canceled during the extraction or sorting
         */
        static BufferedDataTable extractAndSortSingleColumn(final BufferedDataTable data, final ExecutionContext exec,
            final String columnName) throws CanceledExecutionException {

            var rearranger = new ColumnRearranger(data.getDataTableSpec());
            rearranger.keepOnly(columnName);

            var unsortedSingleColumnTable = exec.createColumnRearrangeTable(data, rearranger, exec);

            var sortedTable = new SortedTable(unsortedSingleColumnTable, Collections.singletonList(columnName),
                new boolean[]{true}, exec).getBufferedDataTable();

            return sortedTable;
        }

        /**
         * Creates the node output for the given data table based on the specified settings. The returned object is an
         * array of {@link PortObject} where the first element is a data table containing the binned values, and the
         * second element is a PMML port object containing the binning configuration. The return result can be directly
         * returned from the {@link NodeModel} execute method.
         *
         * @param data the data table to bin
         * @param exec the execution context to report progress and handle cancellation
         * @return an array of {@link PortObject} where the first element is a data table containing the binned values,
         *         and the second element is a PMML port object containing the binning configuration.
         * @throws Exception if an error occurs during the binning process, such as invalid settings or cancellation.
         */
        public PortObject[] createNodeOutput(final BufferedDataTable data, final ExecutionContext exec)
            throws Exception {
            var spec = data.getDataTableSpec();

            Map<String, List<BinBoundary>> edgesMap = new HashMap<String, List<BinBoundary>>();

            if (m_settings.binning() instanceof BinningSettings.EqualWidth fw) {
                var inData = calcDomainBoundsIfNeccessary( //
                    data, //
                    exec.createSubExecutionContext(0.9), //
                    m_settings.columnNames() //
                );

                for (var target : m_settings.columnNames()) {
                    var edges = createEdgesForEqualWidth( //
                        inData.getDataTableSpec().getColumnSpec(target), //
                        m_settings.boundsSettings().getLowerBound(), //
                        m_settings.boundsSettings().getUpperBound(), //
                        m_settings.integerBounds(), //
                        fw.numBins() //
                    );

                    edgesMap.put(target, edges);
                }
            } else if (m_settings.binning() instanceof BinningSettings.EqualCount ff) {
                for (var target : m_settings.columnNames()) {
                    var sortedColumn = extractAndSortSingleColumn(data, exec.createSubExecutionContext(0.9), target);

                    var edges = createEdgesForEqualCount( //
                        sortedColumn, //
                        exec, //
                        ff.numBins(), //
                        m_settings.boundsSettings().getLowerBound(), //
                        m_settings.boundsSettings().getUpperBound(), //
                        m_settings.integerBounds() //
                    );
                    edgesMap.put(target, edges);
                }
            } else if (m_settings.binning() instanceof BinningSettings.FixedQuantiles cq) {
                for (var target : m_settings.columnNames()) {
                    var sortedColumn = extractAndSortSingleColumn(data, exec.createSubExecutionContext(0.9), target);

                    var edges = createEdgesFromQuantiles(sortedColumn, exec, Arrays.asList(cq.quantiles()));
                    edgesMap.put(target, edges);
                }
            } else if (m_settings.binning() instanceof BinningSettings.FixedBoundaries fb) {
                for (var target : m_settings.columnNames()) {
                    edgesMap.put(target, Arrays.asList(fb.boundaries()));
                }
            } else {
                throw new InvalidSettingsException("Unknown binning settings: " + m_settings.binning());
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

        private PMMLPreprocDiscretizeTranslator
            createDiscretizeTranslator(final Map<String, List<BinBoundary>> edgesMap) throws InvalidSettingsException {
            var config = new Configuration(createBins(edgesMap));
            return new PMMLPreprocDiscretizeTranslator(config);
        }

        Map<String, List<Configuration.Bin>> createBins(final Map<String, List<BinBoundary>> edgesMap)
            throws InvalidSettingsException {
            var discretizationsByColumnName = new HashMap<String, List<Configuration.Bin>>();

            for (var targetColumn : edgesMap.keySet()) {
                var edges = edgesMap.get(targetColumn);

                var bins = new ArrayList<Configuration.Bin>();

                if (edgesMap.size() < 2) {
                    // with less than 2 edges we can't make any bins
                    throw new InvalidSettingsException( //
                        "Cannot create bins for column \"" + targetColumn + "\" with less than 2 edges.");
                }

                for (int i = 0; i < edges.size() - 1; ++i) {
                    var leftEdge = edges.get(i);
                    var rightEdge = edges.get(i + 1);

                    // we override the uppermost and lowermost bin boundaries to make them closed. Is that what
                    // we want to do?
                    var closure = Configuration.ClosureStyle.from( //
                        leftEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN || i == 0, //
                        rightEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN
                            || i == edges.size() - 1 //
                    );
                    var binInterval = new Configuration.Interval( //
                        leftEdge.value(), rightEdge.value(), closure //
                    );

                    var binName = m_settings.binNaming().computedName(i, leftEdge, rightEdge);

                    bins.add(new Configuration.Bin( //
                        binName, //
                        Collections.singletonList(binInterval) //
                    ));
                }

                discretizationsByColumnName.put(targetColumn, bins);
            }

            return discretizationsByColumnName;
        }

        /**
         * The {@link NodeModel} configure method returns an array of output specifications. This method is a shorthand
         * that provides a spec that can be directly returned from the configure method.
         *
         * @param inSpec the input data table spec
         * @return an array of output specifications, where the first element is the data table containing the bins, and
         *         the second is a PMML specification for the binning operation.
         * @throws InvalidSettingsException if the settings are not valid for the input spec
         */
        public PortObjectSpec[] createOutputSpec(final DataTableSpec inSpec) throws InvalidSettingsException {
            final List<String> targetColumns = m_settings.columnNames();

            // create some dummy boundaries that won't give any warnings or errors.
            var dummyBounds = List.of( //
                new BinBoundary(Double.NEGATIVE_INFINITY, BinBoundaryExactMatchBehaviour.TO_UPPER_BIN), //
                new BinBoundary(Double.POSITIVE_INFINITY, BinBoundaryExactMatchBehaviour.TO_LOWER_BIN) //
            );

            var translator = createDiscretizeTranslator( //
                targetColumns.stream().collect(Collectors.toMap(Function.identity(), colName -> dummyBounds)) //
            );

            var tableOutSpec = createRearranger(translator, inSpec).createSpec();

            var specCreator = new PMMLPortObjectSpecCreator(inSpec);
            specCreator.setPreprocColNames(new ArrayList<>(translator.getConfig().getTargetColumnNames()));
            var spec = specCreator.createSpec();

            return new PortObjectSpec[]{tableOutSpec, spec};
        }

        /**
         * Creates a {@link ColumnRearranger} that will rearrange the input data table according to the
         * PMMLPreprocDiscretizeTranslator configuration.
         *
         * @param translator the translator that contains the configuration for the discretization
         * @param inSpec the data table specification to rearrange
         * @return a ColumnRearranger that will rearrange the input data table according to the
         *         PMMLPreprocDiscretizeTranslator configuration
         * @throws InvalidSettingsException if any of the target columns specified in the translator is not present in
         *             the input data table specification.
         */
        public ColumnRearranger createRearranger(final PMMLPreprocDiscretizeTranslator translator,
            final DataTableSpec inSpec) throws InvalidSettingsException {
            // Check if columns to discretize exist
            for (String name : translator.getConfig().getTargetColumnNames()) {
                if (!inSpec.containsName(name)) {
                    throw new InvalidSettingsException(
                        "Column " + "\"" + name + "\"" + "is missing in the input table.");
                }
            }
            var rearranger = new ColumnRearranger(inSpec);
            for (String inputColumnName : translator.getConfig().getTargetColumnNames()) {
                int colIdx = inSpec.findColumnIndex(inputColumnName);

                var binNameForBelow = m_settings.boundsSettings().binNameForValuesOutsideLowerBound();
                var binNameForAbove = m_settings.boundsSettings().binNameForValuesOutsideUpperBound();

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
     * Calculates the bounds for a fixed-width binning. If integer bounds are requested, the bounds will be rounded to
     * integer values (specifically, the first bin boundary will be floored, and all others will be ceiled).
     *
     * @param singleColumnSpec the column spec for which to calculate the bounds. It should have a domain with bounds.
     * @param lowerBound the lower bound to use for the first bin boundary; if not present, the minimum value from the
     *            column domain
     * @param upperBound the upper bound to use for the last bin boundary; if not present, the maximum value from the
     *            column domain
     * @param integerBounds if true, the bin boundaries will be rounded to integer values (the data type is still
     *            double)
     * @param numBins the number of bins to create
     * @return a list of bin boundaries, each with an exact match behaviour describing how to handle values that exactly
     *         match the boundary value.
     */
    static List<BinBoundary> createEdgesForEqualWidth(final DataColumnSpec singleColumnSpec,
        final OptionalDouble lowerBound, final OptionalDouble upperBound, final boolean integerBounds,
        final int numBins) {

        double min = lowerBound.orElse(((DoubleValue)singleColumnSpec.getDomain().getLowerBound()).getDoubleValue());
        double max = upperBound.orElse(((DoubleValue)singleColumnSpec.getDomain().getUpperBound()).getDoubleValue());

        // naive equally spaced boundaries
        var unroundedEdges = DoubleStream.concat( //
            IntStream.range(0, numBins).mapToDouble(i -> min + i / (double)numBins * (max - min)), //
            DoubleStream.of(max) //
        ).boxed().toList();

        List<Double> edges = integerBounds //
            ? IntStream.range(0, unroundedEdges.size()) //
                .mapToObj(i -> i == 0 //
                    ? Math.floor(unroundedEdges.get(i)) //
                    : Math.ceil(unroundedEdges.get(i))) //
                .toList() //
            : unroundedEdges;

        return IntStream.range(0, edges.size()) //
            .mapToObj(j -> new BinBoundary(edges.get(j), //
                j == edges.size() - 1 //
                    ? BinBoundaryExactMatchBehaviour.TO_LOWER_BIN //
                    : BinBoundaryExactMatchBehaviour.TO_UPPER_BIN)) //
            .toList();
    }

    /**
     * This method is based on the 7th quantile algorithm in R, equivalent to the scipy <code>quantiles</code> method
     * with (a,b)=(1,1). See <a
     * href="https://en.wikipedia.org/wiki/Quantile#Estimating_quantiles_from_a_sample"}>WP:Quantile</a>.
     *
     * This method doesn't support integer boundaries. Should it?
     *
     * @param sortedColumn the sorted single-column data table to find the edges for, sorted ascending
     * @param exec the execution context to report progress and handle cancellation
     * @param sampleBoundaries the list of sample quantile bin boundaries to create edges for
     * @return
     * @throws CanceledExecutionException
     */
    static List<BinBoundary> createEdgesFromQuantiles(final BufferedDataTable sortedColumn, final ExecutionContext exec,
        final List<BinBoundary> sampleBoundaries) throws CanceledExecutionException {

        var edges = new ArrayList<Double>(sampleBoundaries.size());

        long numRows = sortedColumn.size();
        long rowIndex = 0;
        int edgeIndex = 0;
        try (var iter = sortedColumn.iterator()) {
            DataRow rowQ = null;
            DataRow rowQ1 = null;
            if (iter.hasNext()) {
                rowQ1 = iter.next();
                rowQ = rowQ1;
            }

            for (var quantileBound : sampleBoundaries) {
                // Named h and h_floor to follow the notation in the WP article.
                double h = (numRows - 1) * quantileBound.value() + 1;
                int h_floor = (int)Math.floor(h);

                while ((1.0 == quantileBound.value() || rowIndex < h_floor) && iter.hasNext()) {
                    rowQ = rowQ1;
                    rowQ1 = iter.next();
                    rowIndex++;
                    exec.setProgress(rowIndex / (double)numRows);
                    exec.checkCanceled();
                }
                rowQ = 1.0 != quantileBound.value() ? rowQ : rowQ1;
                final DataCell xqCell = rowQ.getCell(0);
                final DataCell xq1Cell = rowQ1.getCell(0);
                // TODO should be able to handle missing values (need to filter
                // data first?)
                if (xqCell.isMissing() || xq1Cell.isMissing()) {
                    throw new RuntimeException("Missing values not supported for "
                        + "quantile calculation (error in row \"" + rowQ1.getKey() + "\")");
                }
                // for quantile calculation see also
                // http://en.wikipedia.org/wiki/
                //                Quantile#Estimating_the_quantiles_of_a_population.
                // this implements R-7
                double xq = ((DoubleValue)xqCell).getDoubleValue();
                double xq1 = ((DoubleValue)xq1Cell).getDoubleValue();
                double quantile = xq + (h - h_floor) * (xq1 - xq);
                edges.set(edgeIndex, quantile);
                edgeIndex++;
            }

            return IntStream.range(0, edges.size()) //
                .mapToObj(i -> new BinBoundary(edges.get(i), sampleBoundaries.get(i).exactMatchBehaviour())) //
                .toList();
        }
    }

    /**
     * Finds the bin boundaries for a given number of bins, where each bin contains approximately the same number of
     * values. Note that the amounts of values in the bins may differ slightly, depending on the data - for example, the
     * method tries to avoid splitting identical values across different bins, and if integer cutoffs are enforced, the
     * number of values in the bins may vary even more.
     *
     * @param sortedSingleColumn the single-column data table to find the edges for. Should be sorted ascending.
     * @param exec the execution context to report progress and handle cancellation
     * @param binCount the number of bins to create
     * @param minValue if present, the minimum value to use for the first bin boundary; if not present, the minimum is
     *            calculated from the data.
     * @param maxValue if present, the maximum value to use for the last bin boundary; if not present, the maximum is
     *            calculated from the data.
     * @param integerBounds if true, the bin boundaries will be rounded to integer values (the data type is still
     *            double, but they will represent integers).
     * @return the list of bin boundaries, each with an exact match behaviour describing how to handle values that
     *         exactly match the boundary value.
     * @throws CanceledExecutionException if the execution is canceled during the calculation
     */
    static List<BinBoundary> createEdgesForEqualCount( //
        final BufferedDataTable sortedSingleColumn, //
        final ExecutionContext exec, //
        final int binCount, //
        final OptionalDouble minValue, //
        final OptionalDouble maxValue, //
        final boolean integerBounds //
    ) throws CanceledExecutionException {

        List<Double> tableValues = new ArrayList<>();
        try (var it = sortedSingleColumn.iterator()) {
            while (it.hasNext()) {
                tableValues.add(((DoubleValue)it.next().getCell(0)).getDoubleValue());
                exec.checkCanceled();
            }
        }

        DoubleUnaryOperator maybeCeil = integerBounds //
            ? Math::ceil //
            : DoubleUnaryOperator.identity();

        DoubleUnaryOperator maybeFloor = integerBounds //
            ? Math::floor //
            : DoubleUnaryOperator.identity();

        // this next bit of code does the actual bin calculation and was 'borrowed' from the old binner nodes.
        // It tries to find bin boundaries that give equal counts of values in each bin, but it prioritises not
        // splitting identical values across different bins. This can result in a fewer finite-width bins than
        // expected (and a bunch of zero-width bins at the end to keep the amount of boundaries as expected).
        var countPerBin = (int)(Math.round(sortedSingleColumn.size() / (double)binCount));
        var edges = new double[binCount + 1];
        edges[0] = minValue.orElse(maybeFloor.applyAsDouble(tableValues.get(0)));
        edges[edges.length - 1] = maxValue.orElse(maybeCeil.applyAsDouble(tableValues.get(tableValues.size() - 1)));
        var startIndex = 0;
        var index = countPerBin - 1;
        for (int i = 1; i < edges.length - 1; i++) {
            exec.checkCanceled();
            if (index < tableValues.size()) {
                double edge = maybeCeil.applyAsDouble(tableValues.get(index));
                // get lower index
                int lowerIndex = index;
                while (lowerIndex >= startIndex && !(edge > maybeCeil.applyAsDouble(tableValues.get(lowerIndex)))) {
                    lowerIndex--;
                }
                // get higher index
                int higherIndex = index;
                while (higherIndex < tableValues.size() - 1
                    && !(maybeCeil.applyAsDouble(tableValues.get(higherIndex + 1)) > edge)) {
                    higherIndex++;
                }
                int lowerDiff = -1 * (lowerIndex - startIndex + 1 - countPerBin);
                int higherDiff = higherIndex - startIndex + 1 - countPerBin;
                if (!(lowerIndex < startIndex) && lowerDiff <= higherDiff) {
                    index = lowerIndex;
                } else {
                    index = higherIndex;
                }
                edges[i] = maybeCeil.applyAsDouble(tableValues.get(index));
                startIndex = index + 1;
                index += countPerBin;
            } else {
                edges[i] = edges[i - 1];
            }
        }

        // assign sensible match behaviour to the edges and return
        return IntStream.range(0, edges.length) //
            .mapToObj(i -> new BinBoundary(edges[i], //
                i == edges.length - 1 //
                    ? BinBoundaryExactMatchBehaviour.TO_LOWER_BIN //
                    : BinBoundaryExactMatchBehaviour.TO_UPPER_BIN)) //
            .toList();
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
    static BufferedDataTable calcDomainBoundsIfNeccessary( //
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

    /**
     * A {@link SingleCellFactory} that creates a cell for each row in the data table, containing the bin for each row
     * in the specified target column.
     */
    static class BinningCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final List<PMMLPreprocDiscretizeTranslator.Configuration.Bin> m_bins;

        private final String m_binNameForBelow;

        private final String m_binNameForAbove;

        BinningCellFactory( //
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

    /**
     * Checks if the given column specification is compatible with binning. Currently, only columns that are compatible
     * with {@link DoubleValue} are considered compatible for binning.
     *
     * @param colSpec the column specification to check for binning compatibility
     * @return true if the column can be binned, false otherwise
     */
    public static boolean columnCanBeBinned(final DataColumnSpec colSpec) {
        return colSpec.getType().isCompatible(DoubleValue.class);
    }
}
